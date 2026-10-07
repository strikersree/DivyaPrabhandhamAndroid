package com.srinivaskannan.divyaprabhandham.prefs

/**
 * The app's permanent, undeletable collections (the Saththumurai recitations, Podhu
 * Thaniyangal, Nithyanusanthanam and Kovil Thiruvaaymozhi)
 * — seeded via [AppState.seedOrSyncBuiltInCollection], called once per launch
 * from MainActivity. Stable ids so re-seeding on a later launch finds and
 * merges into the same collection rather than creating a duplicate.
 *
 * Both lists now match the iOS build entry for entry. PRABHANDHA_SAARAM
 * carries all 89 of its identifiers; it previously held 62, missing the
 * whole Mudhalaayiram opening block (116–937) and 2674.78, and had the
 * shared tail re-sorted numerically rather than kept in recitation order.
 * 2674.78 is Periya Thirumadal's dotted sub-unit identifier, which could not
 * resolve until that Thirumadal was split into numbered sub-units the way
 * Siriya Thirumadal was; the split landed with the corpus and StanzaParser
 * reads the dotted form, so both 2673.40 and 2674.78 resolve.
 *
 * Seed-or-sync picks new entries up on the next launch for anyone who
 * already has either collection, so these additions reach existing installs
 * without a migration.
 */
object BuiltInCollections {

    const val PRABHANDHA_SAARAM_ID = "builtin-prabhandha-saaram"
    const val PODHU_THANIYANGAL_ID = "builtin-podhu-thaniyangal"
    const val NITHYANUSANTHANAM_ID = "builtin-nithyanusanthanam"
    const val KOVIL_THIRUVAAYMOZHI_ID = "builtin-kovil-thiruvaaymozhi"

    /**
     * The five guru-vandana thaniyans recited before starting the Prabandham,
     * as their own division (d6, podhu_thaniyangal.json) — Vadakalai's,
     * Thenkalai's, then three shared across both sects, in that exact order
     * per explicit instruction. (Originally Thenkalai then Vadakalai; the
     * printed thaniyan numbers 1 and 2 were swapped in the corpus itself
     * along with the re-ordering, so this array is unchanged -- only what
     * each number now names is.) Six keys for five thaniyans: Kurathazhwan's
     * is two verses. All verified to resolve against the actual corpus.
     */
    val podhuThaniyangalKeys: List<String> = listOf(
        "d6w1s1#1", // Vadakalai -- Brahmatantra Swatantra Swami
        "d6w1s1#2", // Thenkalai -- Azhagiya Manavala Perumal Nayanar
        "d6w1s1#3", "d6w1s1#4", // shared -- Kurathazhwan, a two-verse thaniyan
        "d6w1s1#5", // shared -- Aalavandhar
        "d6w1s1#6", // shared -- Parasarabattar
    )

    /**
     * All 89 entries, in recitation order as given — not re-sorted
     * numerically. The Mudhalaayiram tail (935, 936, 947, 946, 937) is not
     * ascending and that is deliberate: this is a curated recitation
     * sequence, not a read-through of the source. Keys are the iOS build's
     * identifier list resolved against this corpus; all 89 verified to
     * resolve to a real stanza.
     */
    val prabhandhaSaaramKeys: List<String> = listOf(
        "w2s9#116", "w2s9#117", "w2s19#213", "w2s19#221", "w2s19#222", "w2s29#326", "w2s29#327", "w2s39#431", "w2s39#432", "w2s43#472", "w2s43#473", "w3s2#502", "w3s2#503",
        "w4s15#645", "w4s15#646", "w5s11#750", "w5s11#751", "w6s2#870", "w6s2#871", "w7s2#915", "w7s2#916", "w9s2#935", "w9s2#936", "w10s2#947", "w10s2#946", "w10s2#937",
        "b2w1s7#1006", "b2w1s7#1007", "b2w1s8#1016", "b2w1s8#1017", "b2w1s11#1046", "b2w1s11#1047", "b2w1s13#1061", "b2w1s13#1067", "b2w1s21#1146", "b2w1s21#1147", "b2w1s31#1246", "b2w1s31#1247",
        "b2w1s41#1346", "b2w1s41#1347", "b2w1s51#1446", "b2w1s51#1447", "b2w1s61#1546", "b2w1s61#1547", "b2w1s71#1646", "b2w1s71#1647", "b2w1s81#1746", "b2w1s81#1747", "b2w1s91#1846", "b2w1s91#1847",
        "b2w1s101#1950", "b2w1s101#1951", "b2w1s108#2020", "b2w1s108#2021", "b2w1s109#2030", "b2w1s109#2031", "b2w2s1#2050", "b2w2s1#2051", "b2w3s1#2080", "b2w3s1#2081", "b3w1s2#2180", "b3w1s2#2181",
        "b3w9s2#2673.40", "b3w10s2#2674.78", "b4w1s11#2783", "b4w1s11#2784", "b4w1s21#2895", "b4w1s21#2896", "b4w1s31#3005", "b4w1s31#3006", "b4w1s41#3115", "b4w1s41#3116",
        "b4w1s51#3225", "b4w1s51#3226", "b4w1s61#3335", "b4w1s61#3336", "b4w1s71#3445", "b4w1s71#3446", "b4w1s81#3555", "b4w1s81#3556", "b4w1s91#3665", "b4w1s91#3666",
        "b4w1s101#3775", "b4w1s101#3776", "b4w1s2#2675", "b4w2s2#3882", "b4w2s2#3883", "b4w2s2#3884", "b4w2s2#3777",
    )

    /**
     * நித்யானுசந்தானம் — the daily recitation, 104 pasurams in this order. Same list as the
     * iOS build's `nithyanusanthanamRanges`, written out as section-id keys: Thiruppallaandu,
     * Thiruppalliyezhuchi, Thiruppaavai, three decads of Periyaazhvaar Thirumozhi, then
     * Amalanaadhipiraan and Kanninun Siruthaambu.
     */
    val nithyanusanthanamKeys: List<String> = listOf(
        // Thiruppallaandu (1-12)
        "w1s2#1", "w1s2#2", "w1s2#3", "w1s2#4", "w1s2#5", "w1s2#6", "w1s2#7", "w1s2#8", "w1s2#9", "w1s2#10", "w1s2#11", "w1s2#12",
        // Thiruppalliyezhuchi (917-926)
        "w8s2#917", "w8s2#918", "w8s2#919", "w8s2#920", "w8s2#921", "w8s2#922", "w8s2#923", "w8s2#924", "w8s2#925", "w8s2#926",
        // Thiruppaavai (474-503)
        "w3s2#474", "w3s2#475", "w3s2#476", "w3s2#477", "w3s2#478", "w3s2#479", "w3s2#480", "w3s2#481", "w3s2#482", "w3s2#483", "w3s2#484", "w3s2#485", "w3s2#486", "w3s2#487", "w3s2#488", "w3s2#489", "w3s2#490", "w3s2#491", "w3s2#492", "w3s2#493", "w3s2#494", "w3s2#495", "w3s2#496", "w3s2#497", "w3s2#498", "w3s2#499", "w3s2#500", "w3s2#501", "w3s2#502", "w3s2#503",
        // Poochoodal -- Periyaazhvaar Thirumozhi 2-7 (182-191)
        "w2s16#182", "w2s16#183", "w2s16#184", "w2s16#185", "w2s16#186", "w2s16#187", "w2s16#188", "w2s16#189", "w2s16#190", "w2s16#191",
        // Kaappidal -- Periyaazhvaar Thirumozhi 2-8 (192-201)
        "w2s17#192", "w2s17#193", "w2s17#194", "w2s17#195", "w2s17#196", "w2s17#197", "w2s17#198", "w2s17#199", "w2s17#200", "w2s17#201",
        // Senniyongu -- Periyaazhvaar Thirumozhi 5-4, the last decad of the fifth pathu (463-473)
        "w2s43#463", "w2s43#464", "w2s43#465", "w2s43#466", "w2s43#467", "w2s43#468", "w2s43#469", "w2s43#470", "w2s43#471", "w2s43#472", "w2s43#473",
        // Amalanaadhipiraan (927-936)
        "w9s2#927", "w9s2#928", "w9s2#929", "w9s2#930", "w9s2#931", "w9s2#932", "w9s2#933", "w9s2#934", "w9s2#935", "w9s2#936",
        // Kanninun Siruthaambu (937-947)
        "w10s2#937", "w10s2#938", "w10s2#939", "w10s2#940", "w10s2#941", "w10s2#942", "w10s2#943", "w10s2#944", "w10s2#945", "w10s2#946", "w10s2#947",
    )

    /**
     * கோயில் திருவாய்மொழி — ten Thiruvaaymozhi decads, whole (ten pasurams and the phala sruti
     * each, 110 in all): 1-1, 1-2, 2-10, 3-3, 4-1, 4-10, 5-5, 7-2, 8-10, 10-10. Same as the iOS
     * build's `kovilThiruvaaymozhiRanges`.
     */
    val kovilThiruvaaymozhiKeys: List<String> = listOf(
        // Thiruvaaymozhi 1-1
        "b4w1s2#2675", "b4w1s2#2676", "b4w1s2#2677", "b4w1s2#2678", "b4w1s2#2679", "b4w1s2#2680", "b4w1s2#2681", "b4w1s2#2682", "b4w1s2#2683", "b4w1s2#2684", "b4w1s2#2685",
        // 1-2
        "b4w1s3#2686", "b4w1s3#2687", "b4w1s3#2688", "b4w1s3#2689", "b4w1s3#2690", "b4w1s3#2691", "b4w1s3#2692", "b4w1s3#2693", "b4w1s3#2694", "b4w1s3#2695", "b4w1s3#2696",
        // 2-10
        "b4w1s21#2886", "b4w1s21#2887", "b4w1s21#2888", "b4w1s21#2889", "b4w1s21#2890", "b4w1s21#2891", "b4w1s21#2892", "b4w1s21#2893", "b4w1s21#2894", "b4w1s21#2895", "b4w1s21#2896",
        // 3-3
        "b4w1s24#2919", "b4w1s24#2920", "b4w1s24#2921", "b4w1s24#2922", "b4w1s24#2923", "b4w1s24#2924", "b4w1s24#2925", "b4w1s24#2926", "b4w1s24#2927", "b4w1s24#2928", "b4w1s24#2929",
        // 4-1
        "b4w1s32#3007", "b4w1s32#3008", "b4w1s32#3009", "b4w1s32#3010", "b4w1s32#3011", "b4w1s32#3012", "b4w1s32#3013", "b4w1s32#3014", "b4w1s32#3015", "b4w1s32#3016", "b4w1s32#3017",
        // 4-10
        "b4w1s41#3106", "b4w1s41#3107", "b4w1s41#3108", "b4w1s41#3109", "b4w1s41#3110", "b4w1s41#3111", "b4w1s41#3112", "b4w1s41#3113", "b4w1s41#3114", "b4w1s41#3115", "b4w1s41#3116",
        // 5-5
        "b4w1s46#3161", "b4w1s46#3162", "b4w1s46#3163", "b4w1s46#3164", "b4w1s46#3165", "b4w1s46#3166", "b4w1s46#3167", "b4w1s46#3168", "b4w1s46#3169", "b4w1s46#3170", "b4w1s46#3171",
        // 7-2
        "b4w1s63#3348", "b4w1s63#3349", "b4w1s63#3350", "b4w1s63#3351", "b4w1s63#3352", "b4w1s63#3353", "b4w1s63#3354", "b4w1s63#3355", "b4w1s63#3356", "b4w1s63#3357", "b4w1s63#3358",
        // 8-10
        "b4w1s81#3546", "b4w1s81#3547", "b4w1s81#3548", "b4w1s81#3549", "b4w1s81#3550", "b4w1s81#3551", "b4w1s81#3552", "b4w1s81#3553", "b4w1s81#3554", "b4w1s81#3555", "b4w1s81#3556",
        // 10-10
        "b4w1s101#3766", "b4w1s101#3767", "b4w1s101#3768", "b4w1s101#3769", "b4w1s101#3770", "b4w1s101#3771", "b4w1s101#3772", "b4w1s101#3773", "b4w1s101#3774", "b4w1s101#3775", "b4w1s101#3776",
    )

    const val DESIKA_PRABHANDHA_SAATHTHUMURAI_ID = "builtin-desika-prabhandha-saaththumurai"

    /**
     * Keys an earlier build seeded into the Desika Saaththumurai that are
     * now wrong, not merely absent. They were resolved against an older
     * desika_prabandham.json whose verse boundaries differed in two works
     * (b5w15s1 parsed 23 verses where it now has 21, b5w17s1 nine where it
     * now has ten), so each still resolves — to the wrong verse. Named here
     * so seed-or-sync drops them from devices that already have them.
     */
    val desikaRetiredKeys: List<String> = listOf(
        "b5w15s1#22", "b5w15s1#23", "b5w17s1#8",
    )

    /**
     * The closing verses (phala sruti) of each of the 19 Desika Prabandham
     * minor works (Thiruvaimozhi and Ramanuja Nootrandhadhi are outside this
     * collection's scope). Taken from the iOS build entry for entry, now that
     * desika_prabandham.json matches it: the previous 39 keys were resolved
     * against an older copy of that file whose verse boundaries differed in
     * two works, so three of them pointed at the wrong verse. Verified
     * against the actual corpus: all 40 resolve to a real stanza.
     */
    val desikaPrabhandhaSaaththumuraiKeys: List<String> = listOf(
        "b5w1s1#38", "b5w1s1#39",
        "b5w2s1#55", "b5w2s1#56",
        "b5w3s1#35", "b5w3s1#37",
        "b5w4s1#20", "b5w4s1#21",
        "b5w5s1#53", "b5w5s1#54",
        "b5w6s1#27", "b5w6s1#28", "b5w6s1#29",
        "b5w7s1#10", "b5w7s1#11",
        "b5w8s1#10", "b5w8s1#11",
        "b5w9s1#9", "b5w9s1#10",
        "b5w10s1#10", "b5w10s1#11",
        "b5w11s1#12", "b5w11s1#13",
        "b5w12s1#9", "b5w12s1#10",
        "b5w13s1#11", "b5w13s1#12",
        "b5w14s1#10", "b5w14s1#11",
        "b5w15s1#20", "b5w15s1#21",
        "b5w16s1#9", "b5w16s1#10",
        "b5w17s1#9", "b5w17s1#10",
        "b5w18s1#17", "b5w18s1#18",
        "b5w20s1#18", "b5w20s1#19", "b5w20s1#20",
    )

    /**
     * The names of the app-curated collections, in every script.
     *
     * Held here rather than in the stored collection because the name is
     * written once, when the collection is first seeded, and the reader may
     * change script long after. A built-in cannot be renamed, so there is
     * nothing of the person's to preserve and the script's form can simply
     * win; a collection the person made keeps its own name whatever the
     * script, which is the point of having made it.
     *
     * The two romanised forms are the app's established spellings, the same
     * ones the division titles use -- not a mechanical transliteration,
     * which would give "pirabandha chaaththumurai".
     */
    fun displayName(id: String, script: ScriptChoice): String? {
        val forms = NAMES[id] ?: return null
        return when (script) {
            ScriptChoice.TAMIL -> forms[0]
            ScriptChoice.READABLE -> forms[1]
            ScriptChoice.SCHOLARLY -> forms[2]
            ScriptChoice.TELUGU -> forms[3]
            ScriptChoice.MALAYALAM -> forms[4]
            ScriptChoice.DEVANAGARI -> forms[5]
            ScriptChoice.KANNADA -> forms[6]
        }
    }

    private val NAMES: Map<String, List<String>> = mapOf(
        PRABHANDHA_SAARAM_ID to listOf(
            "பிரபந்த சாத்துமுறை", "Prabhandha Saaththumurai", "Pirapanta Cāttumuṟai",
            "పిరబంద శాత్తుముఱై", "പിരബന്ദ ശാത്തുമുറൈ", "पिरबन्द शात्तुमुऱै",
            "ಪಿರಬಂದ ಶಾತ್ತುಮುಱೈ",
        ),
        DESIKA_PRABHANDHA_SAATHTHUMURAI_ID to listOf(
            "தேசிக பிரபந்த சாத்துமுறை", "Desika Prabhandha Saaththumurai", "Tēcika Pirapanta Cāttumuṟai",
            "తేశిగ పిరబంద శాత్తుముఱై", "തേശിഗ പിരബന്ദ ശാത്തുമുറൈ", "तेशिग पिरबन्द शात्तुमुऱै",
            "ತೇಶಿಗ ಪಿರಬಂದ ಶಾತ್ತುಮುಱೈ",
        ),
        PODHU_THANIYANGAL_ID to listOf(
            "பொது தனியன்கள்", "Podhu Thaniyangal", "Potu Taṉiyaṉkaḷ",
            "పొదు తనియన్గళ్", "പൊദു തനിയൻഗൾ", "पॊदु तनियन्गळ्",
            "ಪೊದು ತನಿಯನ್ಗಳ್",
        ),
        // The Sanskrit-origin word is spelled as Sanskrit in the Indic scripts (నిత్య, not the
        // Tamil-phonetic నిద్య a mechanical conversion would give). Same forms as the iOS build.
        NITHYANUSANTHANAM_ID to listOf(
            "நித்யானுசந்தானம்", "Nithyaanusandhaanam", "Nityānusandhānam",
            "నిత్యానుసంధానం", "നിത്യാനുസന്ധാനം", "नित्यानुसन्धानम्",
            "ನಿತ್ಯಾನುಸಂಧಾನಂ",
        ),
        KOVIL_THIRUVAAYMOZHI_ID to listOf(
            "கோயில் திருவாய்மொழி", "Kovil Thiruvaaymozhi", "Kōyil Tiruvāymoḻi",
            "కోయిల్ తిరువాయ్మొఴి", "കോയിൽ തിരുവായ്മൊഴി", "कोयिल् तिरुवाय्मॊऴि",
            "ಕೋಯಿಲ್ ತಿರುವಾಯ್ಮೊೞಿ",
        ),
    )
}
