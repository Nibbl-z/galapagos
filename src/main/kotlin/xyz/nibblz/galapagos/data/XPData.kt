package xyz.nibblz.galapagos.data

import xyz.nibblz.galapagos.features.XPInfo

data class XPData(
    val basicStatisticTable: HashMap<String, Int> = hashMapOf(),
    val customStatisticTables: HashMap<String, HashMap<Int, Int>> = hashMapOf()
)

val XP_TABLE: HashMap<XPInfo.XPSource, XPData> = hashMapOf(
    XPInfo.XPSource.BATTLE_BOX_QUADS to XPData(
        basicStatisticTable = hashMapOf(
            "battle_box_quads_rounds_played" to 30,
            "battle_box_quads_team_rounds_won" to 45,
            "battle_box_quads_players_eliminated" to 10,
        )
    ),

    XPInfo.XPSource.BATTLE_BOX_ARENA to XPData(
        basicStatisticTable = hashMapOf(
            "battle_box_arena_rounds_played" to 30,
            "battle_box_arena_team_rounds_won" to 45,
            "battle_box_arena_players_eliminated" to 10,
        )
    ),

    XPInfo.XPSource.DYNABALL to XPData(
        basicStatisticTable = hashMapOf(
            "dynaball_wins" to 150
        ),
        customStatisticTables = hashMapOf(
            "players_stuck" to hashMapOf(
                1 to 5,
                2 to 10,
                4 to 20,
                6 to 30,
                8 to 50,
                10 to 75,
                15 to 100
            ),

            "survival" to hashMapOf(
                30 to 20,
                60 to 40,
                90 to 60,
                120 to 80,
                150 to 100,
                180 to 120,
                210 to 140,
                240 to 160,
                270 to 180,
                300 to 200,
                330 to 220,
                360 to 240,
                390 to 260,
                420 to 280,
                450 to 300
            )
        )
    ),

    XPInfo.XPSource.HOLE_IN_THE_WALL to XPData(
        customStatisticTables = hashMapOf(
            "placement" to hashMapOf(
                9 to 0,
                8 to 50,
                7 to 60,
                6 to 70,
                5 to 80,
                4 to 90,
                3 to 120,
                2 to 150,
                1 to 180
            ),
            "walls_survived" to hashMapOf(
                0 to 0,
                2 to 15,
                4 to 25,
                5 to 35,
                8 to 40,
                10 to 45,
                12 to 50,
                14 to 60,
                16 to 70,
                18 to 80,
                20 to 90,
                22 to 100,
                24 to 110,
                26 to 120,
                28 to 130,
                30 to 150
            )
        )
    ),

    XPInfo.XPSource.PW_SURVIVAL to XPData(
        basicStatisticTable = hashMapOf(
            "pw_survival_leap_champions" to 20,
            "pw_survival_final_duel_wins" to 350
        ),
        customStatisticTables = hashMapOf(
            "leap_reached" to hashMapOf(
                2 to 25,
                3 to 50,
                4 to 75,
                5 to 100,
                6 to 125,
                7 to 175,
                8 to 275
            )
        )
    ),

    XPInfo.XPSource.SKY_BATTLE_QUADS to XPData(
        customStatisticTables = hashMapOf(
            "survival" to hashMapOf(
                30 to 25,
                60 to 50,
                90 to 75,
                120 to 100,
                150 to 150,
                180 to 200,
                210 to 250
            ),
            "placement" to hashMapOf(
                12 to 75,
                10 to 100,
                7 to 125,
                4 to 175,
                3 to 200,
                2 to 250,
                1 to 300
            ),
            "eliminations" to hashMapOf(
                1 to 10,
                2 to 20,
                3 to 30,
                4 to 50,
                6 to 75,
                8 to 100,
                10 to 150,
                12 to 200
            )
        )
    ),

    XPInfo.XPSource.SKY_BATTLE_SOLOS to XPData(
        basicStatisticTable = hashMapOf(
            "sky_battle_solos_players_eliminated" to 15
        ),
        customStatisticTables = hashMapOf(
            "survival" to hashMapOf(
                30 to 25,
                60 to 50,
                90 to 75,
                120 to 100,
                150 to 150,
                180 to 200,
                210 to 250
            ),
            "placement" to hashMapOf(
                4 to 100,
                3 to 150,
                2 to 200,
                1 to 250
            )
        )
    ),

    XPInfo.XPSource.ROCKET_SPLEEF to XPData(
        customStatisticTables = hashMapOf(
            "survival" to hashMapOf(
                30 to 25,
                60 to 50,
                90 to 75,
                120 to 100,
                180 to 150,
                240 to 250
            ),
            "direct_hits" to hashMapOf(
                1 to 5,
                2 to 10,
                3 to 15,
                4 to 20,
                6 to 25,
                8 to 30,
                10 to 50,
                15 to 75,
                20 to 100
            )
        )
    ),

    XPInfo.XPSource.TGTTOS to XPData(
        customStatisticTables = hashMapOf(
            "round_placement" to hashMapOf(
                16 to 50,
                8 to 60,
                7 to 70,
                6 to 80,
                5 to 100,
                4 to 110,
                3 to 120,
                2 to 130,
                1 to 150
            )
        )
    )
)

/*
// Battle Box (Quads)


    // Battle Box Arena


    // Dynaball
 */