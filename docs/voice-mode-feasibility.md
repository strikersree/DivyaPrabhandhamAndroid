# Voice conversation mode: feasibility

## The request

A switch (top-right of the Search/Ask page) that turns Ask into a spoken
conversation: the user talks, the AI answers, and the answer is **spoken back**
so it can be used hands-free / eyes-free.

We already have the input half — the inline `VoiceRecognizer` (speech->text).
Voice mode adds the output half (text->speech) and a continuous loop.

Verdict: **feasible with Android's built-in TextToSpeech, with honest caveats
about Tamil voice quality and the lack of true streaming.** Recommended as a
"speak the answer" mode built on the system TTS — not a low-latency
duplex assistant, which Android can't do natively.

## What it takes

1. **A mode switch** in the Ask top bar (this also becomes the natural home for
   the history button from the other task). When on: after each answer arrives,
   speak it; optionally auto-start listening again for a hands-free loop.
2. **TextToSpeech (android.speech.tts):** built into Android, free, on-device.
   `speak()` the answer text. Pair the TTS locale with the answer's script
   (Tamil answer -> ta-IN voice; English -> en).
3. **A speaking state + control:** a stop/mute affordance, and audio-focus
   handling so it ducks/pauses other audio. Barge-in (tapping the mic while it
   speaks) should stop TTS and start listening.

## Honest caveats (why this is "good", not "flawless")

### Tamil TTS quality and availability is device-dependent
Google's TTS engine supports Tamil, **but the voice data is per-language and
must be present on the device**; quality varies by engine and device, and some
devices ship without the Tamil voice until the user installs it. So on a given
phone Tamil playback may be: (a) present and decent, (b) present but robotic, or
(c) absent, in which case we must detect it and fall back (speak English, or
show a "install a Tamil voice" hint, or silently skip TTS for Tamil). The app
must query `isLanguageAvailable(Locale("ta","IN"))` and degrade gracefully — not
assume Tamil speech works everywhere.

### No true streaming / low latency
Android's native `TextToSpeech` **cannot stream** — it needs the complete text
before it speaks. So the flow is: speak (STT) -> proxy round-trip (a few
seconds) -> full answer -> then speak (TTS). That is a "walkie-talkie" cadence,
not the instant back-and-forth of a phone assistant. That is fine for a
devotional Q&A ("what is the meaning of pasuram 1?" ... pause ... spoken answer),
but it should not be sold as a real-time conversation. True low-latency duplex
voice would need a streaming cloud voice API (OpenAI/Gemini Live, ElevenLabs) —
new cost, new latency, new proxy work, and out of scope here.

### Reading sacred text aloud raises the accuracy bar again
A TTS mispronouncing a pasuram or divine name is more jarring than a text typo.
Two mitigations: (a) speak the AI's *answer/summary*, not raw verse text (verse
recitation is what the YouTube hand-off is for); (b) keep the "AI-generated,
verify" note visible even in voice mode.

### Permissions & lifecycle
Mic permission is already handled (voice input). TTS needs no permission but
needs careful lifecycle: init is async (`onInit`), must `shutdown()` on dispose,
and audio focus must be requested/abandoned. A continuous listen->answer->speak
loop must have a clear stop, or it feels like it will not let go.

## Options

### A. "Speak the answer" mode on system TTS — RECOMMENDED
The switch turns on: answers are spoken via Android TTS (locale matched to
script, graceful fallback when Tamil is unavailable), with a stop control and
optional auto-listen for hands-free use. All on-device, free, no new backend.
Honest walkie-talkie cadence. This is the right scope.

### B. Streaming cloud voice (Gemini Live / OpenAI Realtime) — OUT OF SCOPE
True real-time duplex voice. Much nicer feel, but: new streaming proxy, per-
minute audio cost, new privacy surface, and significant work. Revisit only if
voice becomes a headline feature.

### C. Bundle a Tamil TTS voice — NOT WORTH IT
Shipping a Tamil voice (e.g. an offline engine) would guarantee Tamil playback
but bloats the app massively and duplicates what Google TTS already does on most
devices. Better to rely on system TTS and fall back.

## Recommendation

Build **Option A**: a voice-mode switch top-right of Ask that speaks answers
through Android's TextToSpeech, script-matched with graceful fallback when a
Tamil voice is not installed, with a visible speaking/stop state and optional
auto-listen. Set expectations as "ask by voice, hear the answer" — a calm
walkie-talkie cadence — not a real-time assistant, which Android cannot do
natively. It reuses the existing VoiceRecognizer, adds a TtsSpeaker wrapper, and
needs no new backend or cost.

Decisions to confirm:
1. **Auto-loop** after speaking (hands-free: answer -> auto-listen again), or
   one-shot (speak answer, stop)?
2. **Tamil-unavailable fallback:** speak English, or stay silent with a small
   "no Tamil voice installed" hint?
