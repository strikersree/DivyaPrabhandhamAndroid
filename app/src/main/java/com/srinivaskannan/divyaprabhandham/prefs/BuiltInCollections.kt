package com.srinivaskannan.divyaprabhandham.prefs

/**
 * The app's two permanent, undeletable recitation collections (Saththumurai)
 * — seeded via [AppState.seedOrSyncBuiltInCollection], called once per launch
 * from MainActivity. Stable ids so re-seeding on a later launch finds and
 * merges into the same collection rather than creating a duplicate.
 *
 * PRABHANDHA_SAARAM is deliberately NOT seeded yet: only a 27-key delta (the
 * additions that took it from 62 to its current composition) was available
 * when this was built, not the full baseline list, and two of those 27 keys
 * (the Thirumadals' dotted sub-unit identifiers, 2673.40/2674.78) can't
 * resolve at all until that splitting work lands in this corpus too — see
 * the delivery notes for both gaps. Seeding a collection called "the
 * complete essence of the whole Prabandham" with an admittedly incomplete
 * list felt like the wrong trade; better to leave it unseeded and correct
 * once than seeded and quietly wrong.
 */
object BuiltInCollections {

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
