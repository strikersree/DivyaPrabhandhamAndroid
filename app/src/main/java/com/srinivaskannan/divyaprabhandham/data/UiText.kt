package com.srinivaskannan.divyaprabhandham.data

import com.srinivaskannan.divyaprabhandham.prefs.ScriptChoice

/**
 * In-app localisation for UI chrome.
 *
 * Unlike Android's resource system, which is driven by the device locale,
 * this follows the in-app [ScriptChoice]: Tamil for the original script,
 * English for either transliteration mode. Someone reading on a phone set to
 * English can still keep the whole interface in Tamil, and vice versa — which
 * is the point, and the reason this is not a strings.xml.
 *
 * Devotional content (verses, titles, authors) is never translated here; it is
 * transliterated via the JSON `_r`/`_s` fields. This table is only for
 * functional interface words.
 *
 * Generated from the iOS build's Localization.swift so the two stay in step.
 */
enum class Ui {
    HOME,
    BOOK,
    SAVED,
    SETTINGS,
    SEARCH,
    APPEARANCE_HEADER,
    APPEARANCE_FOOTER,
    ACCENT_HEADER,
    ACCENT_FOOTER,
    ACCOUNT_HEADER,
    ACCOUNT_FOOTER,
    SCRIPT_HEADER,
    SCRIPT_FOOTER,
    SYNC_HEADER,
    SYNC_FOOTER,
    SYNC_TOGGLE,
    SYNC_ON,
    SIGN_OUT,
    FOUR_THOUSANDS,
    CONTINUE_READING,
    RECENTLY_VIEWED,
    BOOKMARKS,
    NO_BOOKMARKS,
    PASURAM,
    WORKS,
    SECTIONS,
    COMING_SOON,
    PREVIOUS_SECTION,
    NEXT_SECTION,
    ADD_BOOKMARK,
    REMOVE_BOOKMARK,
    GO_TO_BOOKMARK,
    SHARE,
    FONT_SIZE,
    TODAY_THIRUPPAVAI,
    TODAY_THIRUPPAVAI_HINT,
    PASURAMS,
    THEME_AUTO,
    THEME_LIGHT,
    THEME_DARK,
    THEME_HIGH_CONTRAST,
    THEME_SEPIA,
    THEME_NIGHT,
    THEME_LIGHT_READER,
    ACCENT_VERMILION,
    ACCENT_GOLD,
    ACCENT_CRIMSON,
    ACCENT_GREEN,
    ACCENT_PEACOCK,
    ACCENT_DYNAMIC,
    DIVYA_PRABANDHAM,
    SELECT_SECTION,
    SELECT_SECTION_HINT,
    NO_BOOKMARKS_HINT,
    MARGAZHI,
    DAY,
    GO_TO_PASURAM,
    VERSE,
    FULL_BOOK_NAME,
    BOOKMARKED,
    READER_THEME,
    CHANGE_THEME,
    COMPOSED_BY,
    FONT_HEADER,
    FONT_FOOTER,
    FONT_TRADITIONAL,
    FONT_MODERN,
    FONT_CLASSIC,
    FONT_SANS,
    EXPLAIN_DEFINE,
    ESSENCE_TITLE,
    ESSENCE_UNAVAILABLE,
    SYNC_TITLE,
    ACCOUNT_PROMPT,
    ACCOUNT_MANAGE,
    NOTIFICATIONS_HEADER,
    NOTIFICATIONS_FOOTER,
    DAILY_REMINDERS,
    REMINDER_MESSAGE,
    REMINDER_TIME,
    ADD_TIME,
    NOTIFICATIONS_DENIED,
    OPEN_SETTINGS,
    REMINDERS_OFF,
    NOW_PLAYING,
    PLAY_RECITATION,
    PINNED,
    PIN_TO_HOME,
    UNPIN_FROM_HOME,
    LOADING_RECITATION,
    READING_PROGRESS,
    WIDGET_HEADER,
    WIDGET_FOOTER,
    WIDGET_SOURCE,
    FOLLOW_APP_SETTING,
    ALL_AAYIRAMS,
    TIP_JAR,
    TIP_JAR_BLURB,
    TIP_THANKS,
    TIP_THANKS_BLURB,
    MAYBE_LATER,
    DONT_ASK_AGAIN,
    DIVYA_DESAMS,
    DIVYA_DESAMS_TAB,
    DESAM_VISITED,
    DESAM_CLEAR_VISIT,
    DESAM_PICK_YEAR,
    PILGRIMAGE_TITLE,
    PILGRIMAGE_TO_NEXT,
    PILGRIMAGE_TO_FIRST,
    PILGRIMAGE_CARDS_TITLE,
    PILGRIMAGE_CARDS_BODY,
    PILGRIMAGE_TEMPLES,
    DESAM_VERSES,
    PERUMAL,
    THAAYAR,
    DESAM_SEARCH_PROMPT,
    DESAM_ALL_REGIONS,
    ABOUT,
    ABOUT_BLURB,
    VERSION,
    DECAD_ESSENCE,
    CREDITS_RECITATION,
    CREDITS_SOURCES,
    CREDITS_GRATITUDE,
    CREDITS_TESTERS,
    TESTERS_NOTE,
    VERSE_RIGHTS_NOTE,
    SUPPORTER_SINCE,
    TIP_UNAVAILABLE,
    LEAVE_A_TIP,
    SYNC_NOW,
    SYNC_LAST,
    SYNC_NEVER,
    SYNC_SYNCING,
    SYNC_FAILED,
    SYNC_NEEDS_CONSENT,
    LISTEN,
    LISTEN_ON_YOUTUBE,
    LISTEN_UNAVAILABLE,
    PIN_LIMIT,
    LOADING,
    CLOSE,
    DONE,
    CANCEL,
    BACK,
    NO_RESULTS,
    ASK_PLACEHOLDER,
    ASK_INTRO_TITLE,
    ASK_INTRO_BODY,
    ASK_DISCLAIMER,
    ASK_LOCAL_MATCHES,
    ASK_THINKING,
    ASK_LISTENING,
    ASK_VOICE,
    ASK_HISTORY,
    ASK_HISTORY_EMPTY,
    ASK_HISTORY_CLEAR,
    ASK_VOICE_MODE,
    ASK_VOICE_STOP,
    ASK_ERR_OFFLINE,
    ASK_ERR_TIMEOUT,
    ASK_ERR_RATE,
    ASK_ERR_SERVER,
}

object UiText {
    /** Pick the Tamil or English string for the current script. */
    fun string(key: Ui, script: ScriptChoice): String {
        val pair = table.getValue(key)
        return if (script.usesEnglishUi) pair.second else pair.first
    }

    private val table: Map<Ui, Pair<String, String>> = mapOf(
        Ui.HOME to ("முகப்பு" to "Home"),
        Ui.BOOK to ("நூல்" to "Book"),
        Ui.SAVED to ("சேமித்தவை" to "Saved"),
        Ui.SETTINGS to ("அமைப்புகள்" to "Settings"),
        Ui.SEARCH to ("தேடுக" to "Search"),
        Ui.APPEARANCE_HEADER to ("தோற்றம்" to "Appearance"),
        Ui.APPEARANCE_FOOTER to (
            "தானியங்கி: வாசிப்புத் தோற்றத்தையே பின்பற்றும். உயர் மாறுபாடு: அதிக எழுத்துத் தெளிவுக்காக முழு வெள்ளை–கருப்பு வண்ணங்கள்." to
            "Auto follows the reading theme. High Contrast uses full black-and-white palettes for maximum legibility."
        ),
        Ui.ACCENT_HEADER to ("முதன்மை நிறம்" to "Accent Colour"),
        Ui.ACCENT_FOOTER to (
            "பட்டன்கள், தேர்வுகள் மற்றும் சிறப்பு அம்சங்களின் நிறம்." to
            "Colour of buttons, selections and highlights."
        ),
        Ui.ACCOUNT_HEADER to (
            "ஒத்திசைவு" to
            "Sync"
        ),
        Ui.ACCOUNT_FOOTER to (
            "உங்கள் வாசிப்புத் தரவு உங்கள் சொந்த Google Drive-இல் மட்டுமே சேமிக்கப்படுகிறது. எங்கள் சேவையகங்களுக்கு எந்தத் தரவும் அனுப்பப்படுவதில்லை." to
            "Your reading data lives in your own Google Drive and nowhere else. Nothing is sent to our servers."
        ),
        Ui.SCRIPT_HEADER to ("எழுத்து / மொழி" to "Script & Language"),
        Ui.SCRIPT_FOOTER to (
            "மூல தமிழ் அல்லது ஆங்கில ஒலிபெயர்ப்பு. ஆங்கிலம் தேர்ந்தால் செயலி முழுவதும் ஆங்கிலத்தில் இருக்கும்." to
            "Original Tamil, or English transliteration. Choosing English switches the whole app to English."
        ),
        Ui.SYNC_HEADER to ("" to ""),
        Ui.SYNC_FOOTER to (
            "நினைவுக்குறிகள், வாசிப்பு நிலை மற்றும் விருப்பங்கள் உங்கள் Google Drive-இன் மறைவான செயலிக் கோப்புறையில் சேமிக்கப்படும். உங்கள் சாதனங்களுக்கு இடையே ஒத்திசைக்கப்படும்; வேறு யாரும் அதைப் பார்க்க முடியாது." to
            "Bookmarks, reading position and preferences are kept in a hidden app folder in your own Google Drive, so they follow you between devices. Nobody else can see it, and it never touches a server of ours."
        ),
        Ui.SYNC_TOGGLE to (
            "Google ஒத்திசைவு" to
            "Sync with Google"
        ),
        Ui.SYNC_ON to (
            "ஒத்திசைவு இயக்கத்தில் உள்ளது" to
            "Syncing with your Google account"
        ),
        Ui.SIGN_OUT to (
            "ஒத்திசைவை நிறுத்து" to
            "Turn off sync"
        ),
        Ui.FOUR_THOUSANDS to ("நான்கு ஆயிரங்கள்" to "The Four Thousand"),
        Ui.CONTINUE_READING to ("தொடர்ந்து படிக்க" to "Continue Reading"),
        Ui.RECENTLY_VIEWED to ("சமீபத்தில் பார்த்தவை" to "Recently Viewed"),
        Ui.BOOKMARKS to ("நினைவுக்குறிகள்" to "Bookmarks"),
        Ui.NO_BOOKMARKS to ("சேமித்த பாசுரங்கள் இல்லை" to "No saved pasurams"),
        Ui.PASURAM to ("பாசுரம்" to "Pasuram"),
        Ui.WORKS to ("நூல்கள்" to "works"),
        Ui.SECTIONS to ("பகுதிகள்" to "sections"),
        Ui.COMING_SOON to ("விரைவில்" to "Coming soon"),
        Ui.PREVIOUS_SECTION to ("முந்தைய பகுதி" to "Previous section"),
        Ui.NEXT_SECTION to ("அடுத்த பகுதி" to "Next section"),
        Ui.ADD_BOOKMARK to ("நினைவுக்குறி சேர்" to "Add bookmark"),
        Ui.REMOVE_BOOKMARK to ("நினைவுக்குறியை நீக்கு" to "Remove bookmark"),
        Ui.GO_TO_BOOKMARK to ("நினைவுக்குறிக்குச் செல்" to "Go to bookmark"),
        Ui.SHARE to ("பாசுரத்தைப் பகிர்" to "Share pasuram"),
        Ui.FONT_SIZE to ("எழுத்து அளவு" to "Font size"),
        Ui.TODAY_THIRUPPAVAI to (
            "இன்றைய திருப்பாவை பாசுரத்தைப் படிக்க" to
            "Read today's Thiruppavai pasuram"
        ),
        Ui.TODAY_THIRUPPAVAI_HINT to (
            "இன்றைய திருப்பாவைப் பாசுரத்தைப் படிக்க தொடவும்" to
            "Tap to read today's Thiruppavai pasuram"
        ),
        Ui.PASURAMS to ("பாசுரங்கள்" to "pasurams"),
        Ui.THEME_AUTO to ("தானியங்கி" to "Auto"),
        Ui.THEME_LIGHT to ("வெளிர்" to "Light"),
        Ui.THEME_DARK to ("இருள்" to "Dark"),
        Ui.THEME_HIGH_CONTRAST to ("உயர் மாறுபாடு" to "High Contrast"),
        Ui.THEME_SEPIA to ("செபியா" to "Sepia"),
        Ui.THEME_NIGHT to ("இரவு" to "Night"),
        Ui.THEME_LIGHT_READER to ("வெளிர்" to "Light"),
        Ui.ACCENT_VERMILION to ("செந்நிறம்" to "Vermilion"),
        Ui.ACCENT_GOLD to ("தங்கம்" to "Gold"),
        Ui.ACCENT_CRIMSON to ("கருஞ்சிவப்பு" to "Crimson"),
        Ui.ACCENT_GREEN to ("இலைப் பச்சை" to "Leaf Green"),
        Ui.ACCENT_PEACOCK to ("மயில் நீலம்" to "Peacock Blue"),
        Ui.ACCENT_DYNAMIC to ("சாதன வண்ணம்" to "From wallpaper"),
        Ui.DIVYA_PRABANDHAM to ("திவ்ய பிரபந்தம்" to "Divya Prabandham"),
        Ui.SELECT_SECTION to ("பகுதியைத் தேர்ந்தெடுக்கவும்" to "Select a section"),
        Ui.SELECT_SECTION_HINT to (
            "பக்கப்பட்டியில் இருந்து ஒரு பகுதியைத் திறக்கவும்" to
            "Open a section from the sidebar"
        ),
        Ui.NO_BOOKMARKS_HINT to (
            "பாசுரத்தின் நட்சத்திரத்தைத் தொட்டு இங்கே சேமிக்கலாம்" to
            "Tap a pasuram's star to save it here"
        ),
        Ui.MARGAZHI to ("மார்கழி" to "Margazhi"),
        Ui.DAY to ("நாள்" to "Day"),
        Ui.GO_TO_PASURAM to ("க்குச் செல்" to "Go to pasuram"),
        Ui.VERSE to ("பாடல்" to "Verse"),
        Ui.FULL_BOOK_NAME to ("நாலாயிர திவ்ய பிரபந்தம்" to "Naalayira Divya Prabandham"),
        Ui.BOOKMARKED to ("நினைவுக்குறிக்கப்பட்டது" to "bookmarked"),
        Ui.READER_THEME to ("தோற்றம்" to "Theme"),
        Ui.CHANGE_THEME to ("வாசிப்பு தோற்றத்தை மாற்ற" to "Change reading theme"),
        Ui.COMPOSED_BY to ("அருளிச் செய்தவை" to "Composed by"),
        Ui.FONT_HEADER to ("எழுத்துரு" to "Reading Font"),
        Ui.FONT_FOOTER to (
            "பாசுர வாசிப்புக்கான எழுத்துரு. ஒவ்வொரு தேர்வும் தமிழுக்கும் ஆங்கிலத்திற்கும் ஏற்ற எழுத்துருக்களை இணைக்கிறது." to
            "Typeface for reading verses. Each choice pairs a Tamil and a Latin face."
        ),
        Ui.FONT_TRADITIONAL to ("பாரம்பரியம்" to "Traditional"),
        Ui.FONT_MODERN to ("நவீனம்" to "Modern"),
        Ui.FONT_CLASSIC to ("செம்மை" to "Classic"),
        Ui.FONT_SANS to ("சான்ஸ்" to "Sans"),
        Ui.EXPLAIN_DEFINE to ("சாரம் காண்க" to "Explain / Define"),
        Ui.ESSENCE_TITLE to ("சாரம்" to "Essence"),
        Ui.ESSENCE_UNAVAILABLE to (
            "இந்தப் பாசுரத்தின் சாரம் விரைவில் சேர்க்கப்படும்." to
            "An essence for this pasuram is coming soon."
        ),
        Ui.SYNC_TITLE to (
            "ஒத்திசைவு" to
            "Sync"
        ),
        Ui.ACCOUNT_PROMPT to (
            "சாதனம் மாறினாலும் உங்கள் வாசிப்பு தொடரும்" to
            "Keep your reading when you change phones"
        ),
        Ui.ACCOUNT_MANAGE to (
            "ஒத்திசைவு" to
            "Sync"
        ),
        Ui.NOTIFICATIONS_HEADER to ("அறிவிப்புகள்" to "Notifications"),
        Ui.NOTIFICATIONS_FOOTER to (
            "நீங்கள் தேர்ந்தெடுத்த நேரங்களில் தினமும் வாசிப்பைத் தொடர நினைவூட்டல் பெறுங்கள்." to
            "Get a daily reminder to keep reading at the times you choose."
        ),
        Ui.DAILY_REMINDERS to ("தினசரி நினைவூட்டல்கள்" to "Daily reminders"),
        Ui.REMINDER_MESSAGE to (
            "திவ்ய பிரபந்தம் தொடர்ந்து படியுங்கள்" to
            "Continue reading Divya Prabhandham"
        ),
        Ui.REMINDER_TIME to ("நினைவூட்டல் நேரம்" to "Reminder time"),
        Ui.ADD_TIME to ("நேரத்தைச் சேர்" to "Add a time"),
        Ui.NOTIFICATIONS_DENIED to (
            "அறிவிப்புகள் அமைப்புகளில் முடக்கப்பட்டுள்ளன. நினைவூட்டல்களைப் பெற அமைப்புகளில் இயக்கவும்." to
            "Notifications are turned off in Settings. Enable them there to get reminders."
        ),
        Ui.OPEN_SETTINGS to ("அமைப்புகளைத் திற" to "Open Settings"),
        Ui.REMINDERS_OFF to ("முடக்கம்" to "Off"),
        Ui.NOW_PLAYING to ("ஒலிக்கிறது" to "Now playing"),
        Ui.PLAY_RECITATION to ("பாராயணம் கேட்க" to "Play recitation"),
        Ui.PINNED to ("பொருத்தியவை" to "Pinned"),
        Ui.PIN_TO_HOME to ("முகப்பில் பொருத்து" to "Pin to Home"),
        Ui.UNPIN_FROM_HOME to ("முகப்பிலிருந்து அகற்று" to "Unpin from Home"),
        Ui.LOADING_RECITATION to ("ஏற்றுகிறது…" to "Loading…"),
        Ui.READING_PROGRESS to ("வாசிப்பு நிலை" to "Reading progress"),
        Ui.WIDGET_HEADER to ("விட்ஜெட்" to "Widget"),
        Ui.WIDGET_FOOTER to (
            "ஒவ்வொரு மணி நேரமும் ஒரு பாசுரம் விட்ஜெட்டில் தோன்றும். எந்தப் பகுதியிலிருந்து காட்ட வேண்டும் என்பதைத் தேர்ந்தெடுக்கலாம்." to
            "A pasuram appears in the widget every hour. Choose which part it is drawn from."
        ),
        Ui.WIDGET_SOURCE to ("பாசுரத் தொகுப்பு" to "Verse source"),
        Ui.FOLLOW_APP_SETTING to ("செயலியின் அமைப்பு" to "Follow app setting"),
        Ui.ALL_AAYIRAMS to ("அனைத்தும்" to "All"),
        Ui.TIP_JAR to ("ஆதரவு அளியுங்கள்" to "Support this app"),
        Ui.TIP_JAR_BLURB to (
            "இச்செயலியின் அனைத்து பாசுரங்களும் என்றும் இலவசம். விரும்பினால் மட்டும் ஒரு சிறு அன்பளிப்பு அளிக்கலாம்." to
            "Every verse in this app is free, and always will be. If it has been of value to you, a small gift is welcome."
        ),
        Ui.TIP_THANKS to ("நன்றி" to "Thank you"),
        Ui.TIP_THANKS_BLURB to (
            "உங்கள் ஆதரவுக்கு மிக்க நன்றி." to
            "Your kindness keeps this work going."
        ),
        Ui.MAYBE_LATER to ("பிறகு பார்க்கலாம்" to "Maybe later"),
        Ui.DONT_ASK_AGAIN to ("மீண்டும் கேட்க வேண்டாம்" to "Don't ask again"),
        Ui.DIVYA_DESAMS to ("திவ்ய தேசங்கள்" to "Divya Desams"),
        Ui.DIVYA_DESAMS_TAB to ("தேசங்கள்" to "Desams"),
        Ui.DESAM_VISITED to ("சென்றுள்ளேன்" to "Visited"),
        Ui.DESAM_CLEAR_VISIT to ("நீக்கு" to "Remove"),
        Ui.DESAM_PICK_YEAR to ("எந்த ஆண்டு சென்றீர்கள்?" to "Which year did you visit?"),
        Ui.PILGRIMAGE_TITLE to ("யாத்திரை" to "Pilgrimage"),
        Ui.PILGRIMAGE_TO_NEXT to ("மேலும் அடுத்த நிலைக்கு" to "more to next level"),
        Ui.PILGRIMAGE_TO_FIRST to ("கோயில்கள் முதல் நிலைக்கு" to "temples to Level 1"),
        Ui.PILGRIMAGE_CARDS_TITLE to ("சாதனை அட்டைகள்" to "Achievement cards"),
        Ui.PILGRIMAGE_CARDS_BODY to ("ஒவ்வொரு நிலையிலும் ஒரு அட்டை திறக்கும்." to "A card unlocks at each level."),
        Ui.PILGRIMAGE_TEMPLES to ("கோயில்கள்" to "temples"),
        Ui.DESAM_VERSES to ("பாசுரங்கள்" to "verses"),
        Ui.PERUMAL to ("பெருமாள்" to "Perumal"),
        Ui.THAAYAR to ("தாயார்" to "Thaayar"),
        Ui.DESAM_SEARCH_PROMPT to ("கோயில், ஊர் அல்லது பெருமாள் பெயர்" to "Temple, place or deity"),
        Ui.DESAM_ALL_REGIONS to ("அனைத்தும்" to "All"),
        Ui.ABOUT to ("இந்தச் செயலி பற்றி" to "About"),
        Ui.ABOUT_BLURB to (
            "நாலாயிர திவ்ய பிரபந்தமும் ஸ்ரீ தேசிகப் பிரபந்தமும் — ஆழ்வார்கள் அருளிச்செய்த பாசுரங்களை அமைதியாகப் படிக்க ஓர் இடம். இணையம் இல்லாமலும் இயங்கும்; விளம்பரங்கள் இல்லை." to
            "The Naalayira Divya Prabandham and Sri Desika Prabandham — a quiet place to read the verses of the Aazhwars. Works fully offline, with no ads and no tracking."
        ),
        Ui.VERSION to ("பதிப்பு" to "Version"),
        Ui.DECAD_ESSENCE to ("இப்பதிகத்தின் சாரம்" to "About this decad"),
        Ui.CREDITS_RECITATION to ("பாராயணம்" to "Recitation"),
        Ui.CREDITS_SOURCES to ("மூலங்களும் தொகுப்புகளும்" to "Sources & compilations"),
        Ui.CREDITS_GRATITUDE to ("நன்றியுடன்" to "With gratitude"),
        Ui.CREDITS_TESTERS to ("சோதனையாளர்கள்" to "Testers"),
        Ui.TESTERS_NOTE to (
            "இச்செயலியை முன்கூட்டியே பயன்படுத்திப் பிழைகளைச் சுட்டிக்காட்டிய அனைத்து நண்பர்களுக்கும் குடும்பத்தினருக்கும் நன்றி." to
            "Thank you to the friends and family who tested early builds and pointed out what needed fixing."
        ),
        Ui.VERSE_RIGHTS_NOTE to (
            "பாசுரங்கள் பொது உரிமையில் உள்ளவை. படித்தல் முழுவதும் இணையம் இல்லாமல் இயங்கும்; பாராயணங்கள் YouTube வழியாக இயக்கப்படுகின்றன, அவை இச்செயலியில் சேமிக்கப்படுவதில்லை." to
            "The verses are in the public domain. Reading works entirely offline; recitations are played through YouTube and are never stored in this app."
        ),
        Ui.SUPPORTER_SINCE to ("ஆதரவாளர்" to "Supporter since"),
        Ui.TIP_UNAVAILABLE to ("இப்போது கிடைக்கவில்லை" to "Not available right now"),
        Ui.LEAVE_A_TIP to ("அன்பளிப்பு" to "Leave a tip"),
        Ui.SYNC_NOW to ("இப்போது ஒத்திசை" to "Sync now"),
        Ui.SYNC_LAST to ("கடைசி ஒத்திசைவு" to "Last synced"),
        Ui.SYNC_NEVER to ("இதுவரை இல்லை" to "Never"),
        Ui.SYNC_SYNCING to ("ஒத்திசைக்கிறது…" to "Syncing…"),
        Ui.SYNC_FAILED to ("ஒத்திசைவு தோல்வி — மீண்டும் முயற்சிக்க தட்டவும்" to "Sync failed — tap to retry"),
        Ui.SYNC_NEEDS_CONSENT to ("அனுமதி தேவை" to "Permission needed"),
        Ui.LISTEN to ("பாராயணம் கேட்க" to "Listen"),
        Ui.LISTEN_ON_YOUTUBE to ("YouTube Music-இல் கேட்க" to "Listen on YouTube Music"),
        Ui.LISTEN_UNAVAILABLE to ("இந்நூலுக்குப் பாராயணம் இணைக்கப்படவில்லை" to "No recitation linked for this work yet"),
        Ui.PIN_LIMIT to ("ஆறு நூல்கள் வரை மட்டுமே பொருத்த முடியும்" to "You can pin up to six works"),
        Ui.LOADING to ("ஏற்றுகிறது…" to "Loading…"),
        Ui.CLOSE to ("மூடு" to "Close"),
        Ui.DONE to ("சரி" to "Done"),
        Ui.CANCEL to ("ரத்து" to "Cancel"),
        Ui.BACK to ("பின்" to "Back"),
        Ui.NO_RESULTS to ("முடிவுகள் இல்லை" to "No results"),
        Ui.ASK_PLACEHOLDER to ("பாசுரம், கோயில் அல்லது கேள்வி…" to "Ask about a pasuram, temple, or verse…"),
        Ui.ASK_INTRO_TITLE to ("கேளுங்கள்" to "Ask"),
        Ui.ASK_INTRO_BODY to ("பாசுரங்கள், ஆழ்வார்கள், திவ்ய தேசங்கள் குறித்து கேளுங்கள். எண்ணைத் தட்டச்சு செய்தால் நேரடியாகச் செல்லலாம்." to "Ask about pasurams, Azhwars, and Divya Desams. Type a number to jump straight there."),
        Ui.ASK_DISCLAIMER to ("AI-உருவாக்கியது — முக்கியமான விவரங்களைச் சரிபார்க்கவும்." to "AI-generated — please verify important details."),
        Ui.ASK_LOCAL_MATCHES to ("பாசுரத் தொகுப்பில் கிடைத்தவை:" to "Found in the corpus:"),
        Ui.ASK_THINKING to ("சிந்திக்கிறது…" to "Thinking…"),
        Ui.ASK_LISTENING to ("கேட்கிறது…" to "Listening…"),
        Ui.ASK_VOICE to ("குரல் மூலம் கேளுங்கள்" to "Ask by voice"),
        Ui.ASK_HISTORY to ("வரலாறு" to "History"),
        Ui.ASK_HISTORY_EMPTY to ("இதுவரை கேள்விகள் இல்லை." to "No questions yet."),
        Ui.ASK_HISTORY_CLEAR to ("அழி" to "Clear"),
        Ui.ASK_VOICE_MODE to ("குரல் பயன்முறை" to "Voice mode"),
        Ui.ASK_VOICE_STOP to ("நிறுத்து" to "Stop"),
        Ui.ASK_ERR_OFFLINE to ("இணைப்பு இல்லை. இணையத்தைச் சரிபார்க்கவும்." to "You're offline. Check your connection and try again."),
        Ui.ASK_ERR_TIMEOUT to ("பதில் தாமதமானது. மீண்டும் முயற்சிக்கவும்." to "That took too long. Please try again."),
        Ui.ASK_ERR_RATE to ("சற்று பொறுத்து மீண்டும் கேளுங்கள்." to "A lot of questions just now — please wait a moment and try again."),
        Ui.ASK_ERR_SERVER to ("பதிலளிக்க முடியவில்லை. பிறகு முயற்சிக்கவும்." to "Couldn't get an answer right now. Please try again later."),
    )
}
