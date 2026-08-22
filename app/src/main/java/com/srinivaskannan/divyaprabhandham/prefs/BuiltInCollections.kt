package com.srinivaskannan.divyaprabhandham.prefs

/**
 * The app's two permanent, undeletable recitation collections (Saththumurai)
 * — seeded via [AppState.seedOrSyncBuiltInCollection], called once per launch
 * from MainActivity. Stable ids so re-seeding on a later launch finds and
 * merges into the same collection rather than creating a duplicate.
 *
 * PRABHANDHA_SAARAM is now seeded with 62 of its 63 confirmed entries. The
 * 63rd, 2674.78, is Periya Thirumadal's dotted sub-unit identifier — it
 * can't resolve until that Thirumadal is split into numbered sub-units the
 * same way Siriya Thirumadal was (see 2673.40 below, now resolvable).
 * Periya's book pages haven't been provided yet in this session; once they
 * are and the split lands, adding "b3w10s2#2674.78" to the list below is
 * the entire remaining step — seed-or-sync picks it up automatically on
 * the next launch for anyone who already has the collection.
 */
object BuiltInCollections {

    const val PRABHANDHA_SAARAM_ID = "builtin-prabhandha-saaram"

    /**
     * 62 of 63 confirmed entries (2674.78 pending, see class doc). Every key
     * verified against the actual corpus before being hardcoded here: all
     * resolve to a real stanza, cross-checked against the resolution
     * script's own output line-by-line.
     */
    val prabhandhaSaaramKeys: List<String> = listOf(
        "b2w1s7#1006", "b2w1s7#1007",
        "b2w1s8#1016", "b2w1s8#1017",
        "b2w1s11#1046", "b2w1s11#1047",
        "b2w1s13#1061", "b2w1s13#1067",
        "b2w1s21#1146", "b2w1s21#1147",
        "b2w1s31#1246", "b2w1s31#1247",
        "b2w1s41#1346", "b2w1s41#1347",
        "b2w1s51#1446", "b2w1s51#1447",
        "b2w1s61#1546", "b2w1s61#1547",
        "b2w1s71#1646", "b2w1s71#1647",
        "b2w1s81#1746", "b2w1s81#1747",
        "b2w1s91#1846", "b2w1s91#1847",
        "b2w1s101#1950", "b2w1s101#1951",
        "b2w1s108#2020", "b2w1s108#2021",
        "b2w1s109#2030", "b2w1s109#2031",
        "b2w2s1#2050", "b2w2s1#2051",
        "b2w3s1#2080", "b2w3s1#2081",
        "b3w1s2#2180", "b3w1s2#2181",
        "b3w9s2#2673.40",
        "b4w1s2#2675",
        "b4w1s11#2783", "b4w1s11#2784",
        "b4w1s21#2895", "b4w1s21#2896",
        "b4w1s31#3005", "b4w1s31#3006",
        "b4w1s41#3115", "b4w1s41#3116",
        "b4w1s51#3225", "b4w1s51#3226",
        "b4w1s61#3335", "b4w1s61#3336",
        "b4w1s71#3445", "b4w1s71#3446",
        "b4w1s81#3555", "b4w1s81#3556",
        "b4w1s91#3665", "b4w1s91#3666",
        "b4w1s101#3775", "b4w1s101#3776",
        "b4w2s2#3777",
        "b4w2s2#3882", "b4w2s2#3883", "b4w2s2#3884",
    )

    const val DESIKA_PRABHANDHA_SAATHTHUMURAI_ID = "builtin-desika-prabhandha-saaththumurai"

    /**
     * The closing verses (phala sruti) of each of the 19 Desika Prabandham
     * minor works (Thiruvaimozhi and Ramanuja Nootrandhadhi are outside this
     * collection's scope). Verified against the actual corpus: all 39 keys
     * resolve to a real stanza, none missing.
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
        "b5w15s1#22", "b5w15s1#23",
        "b5w16s1#9", "b5w16s1#10",
        "b5w17s1#8", "b5w17s1#9",
        "b5w18s1#17", "b5w18s1#18",
        "b5w20s1#18", "b5w20s1#20",
    )
}
