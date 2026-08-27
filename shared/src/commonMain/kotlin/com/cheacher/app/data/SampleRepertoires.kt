package com.cheacher.app.data

import com.cheacher.app.chess.Color
import com.cheacher.app.domain.Repertoire
import com.cheacher.app.domain.repertoire

/**
 * The built-in study material. Hand-authored with the DSL so the source reads like a book:
 * indentation is the tree, every move carries its canonical name and one sentence of *why*.
 *
 * Every line is verified legal by the resolver at load time (and by a test), so a typo
 * here fails the build, not the learner.
 */
object SampleRepertoires {
    val all: List<Repertoire> by lazy {
        listOf(
            // The on-ramp, then the 1. e4 shelf...
            kingsPawn,
            italianGame,
            ruyLopez,
            openGame,
            sicilianCrossroads,
            frenchDefence,
            caroKann,
            kingsPawnSidelines,
            // The first repertoire from the other chair.
            frenchAsBlack,
            // ...the 1. d4 shelf...
            queensGambit,
            indianDefences,
            queensPawnSystems,
            // ...the flank openings, and the whole first-move vocabulary.
            englishOpening,
            retiOpening,
            twentyFirstMoves,
        )
    }

    fun byId(id: String): Repertoire = all.first { it.id == id }

    /**
     * The on-ramp: 1. e4 and the very first crossroads, kept deliberately shallow.
     * The main road runs through the Open Game to the Italian and Ruy Lopez first
     * moves; the Sicilian and French lines are one-fork stubs that point at the
     * deeper repertoires further down the shelf.
     */
    val kingsPawn: Repertoire = repertoire(
        id = "kings-pawn",
        title = "King's Pawn Openings",
        perspective = Color.WHITE,
        subtitle = "1. e4 and the first crossroads",
    ) {
        move("e4", "King's Pawn Opening", "Stakes the centre and frees the bishop and queen in one stroke.") {
            move("e5", "Open Game", "Black answers symmetrically and contests the centre head-on.") {
                move("Nf3", "King's Knight Opening", "Develops with tempo by attacking the e5 pawn.") {
                    move("Nc6", "Normal Variation", "Defends e5 with a developing move rather than a pawn.") {
                        move("Bc4", "Italian Game", "Aims the bishop at f7, the weakest square in Black's camp.")
                        move("Bb5", "Ruy Lopez", "Pressures the knight that guards e5, asking Black a question that lasts all game.")
                    }
                }
            }
            move("c5", "Sicilian Defence", "Fights for d4 from the wing, refusing the symmetry of 1...e5.") {
                move("Nf3", "Open Sicilian, Preparation", "Develops and prepares d4, the pawn break that defines the Open Sicilian.")
            }
            move("e6", "French Defence", "A modest pawn move that prepares ...d5 and a counterpunch at White's centre.") {
                move("d4", "French Defence, Normal Variation", "Takes the full centre while it is on offer; Black will strike back with ...d5.")
            }
        }
    }

    /**
     * A White repertoire around the Italian bishop: the quiet Pianissimo squeeze,
     * the sharp Two Knights lunge, and the Hungarian sidestep.
     */
    val italianGame: Repertoire = repertoire(
        id = "italian",
        title = "The Italian Game",
        perspective = Color.WHITE,
        subtitle = "Quiet development, eternal pressure on f7",
    ) {
        move("e4", "King's Pawn Opening", "Stakes the centre and frees the bishop and queen in one stroke.") {
            move("e5", "Open Game", "Black answers symmetrically and contests the centre head-on.") {
                move("Nf3", "King's Knight Opening", "Develops with tempo by attacking the e5 pawn.") {
                    move("Nc6", "Normal Variation", "Defends e5 with a developing move rather than a pawn.") {
                        move("Bc4", "Italian Game", "Aims the bishop at f7, the weakest square in Black's camp.") {
                            move("Bc5", "Giuoco Piano", "Black mirrors the diagonal and eyes f2 in return.") {
                                move("c3", "Giuoco Piano, c3 System", "Prepares d4 so the centre can expand with pawn support.") {
                                    move("Nf6", "Giuoco Piano, Main Line", "Counterattacks e4 before White's centre gets rolling.") {
                                        move("d3", "Giuoco Pianissimo", "Keeps the centre closed and saves the d4 break for a better moment.") {
                                            move("d6", "Giuoco Pianissimo, Main Line", "Black matches the slow build and shores up e5.") {
                                                move("O-O", "Giuoco Pianissimo, Castling Line", "Tucks the king away before any central pawn tension appears.")
                                            }
                                        }
                                    }
                                }
                            }
                            move("Nf6", "Two Knights Defence", "Black ignores f7 for a move and hits e4 immediately.") {
                                move("Ng5", "Knight Attack", "Only-move territory: two pieces now hit f7 and Black must react.") {
                                    move("d5", "Two Knights, Main Line", "Blocks the bishop's diagonal — the one sound answer to the double attack.") {
                                        move("exd5", "Knight Attack, Exchange", "Wins the pawn and keeps the f7 pressure alive.") {
                                            move("Na5", "Polerio Defence", "Chases the c4 bishop off the deadly diagonal at the cost of the rim.") {
                                                move("Bb5+", "Polerio Defence, Bishop Check", "Keeps the initiative with check instead of retreating meekly.") {
                                                    move("c6", "Polerio Defence, Main Line", "Blocks the check and undermines the d5 pawn at once.") {
                                                        move("dxc6", "Polerio, Pawn Grab", "Takes a second pawn while Black is still untangling.") {
                                                            move("bxc6", "Polerio, Recapture", "Recaptures and opens the b-file for the rook.") {
                                                                move("Be2", "Polerio, Main Retreat", "Steps out of the pawns' reach; White banks the extra pawn against Black's activity.")
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            move("Be7", "Hungarian Defence", "A modest sidestep that avoids all the sharp Italian theory.") {
                                move("d4", "Hungarian Defence, Centre Strike", "With no bishop on c5 to punish it, White grabs the centre at once.") {
                                    move("exd4", "Hungarian Defence, Exchange", "Concedes the centre rather than defend e5 awkwardly.") {
                                        move("Nxd4", "Hungarian Defence, Main Line", "Recaptures with a centralised knight and a lasting space edge.")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * The Sicilian from White's side of the table: the Open Sicilian with its two
     * biggest tabiyas, or the Closed Sicilian for players who refuse to open the box.
     */
    val sicilianCrossroads: Repertoire = repertoire(
        id = "sicilian",
        title = "Sicilian Crossroads",
        perspective = Color.WHITE,
        subtitle = "Open it or close it — but know both doors",
    ) {
        move("e4", "King's Pawn Opening", "Stakes the centre and frees the bishop and queen in one stroke.") {
            move("c5", "Sicilian Defence", "Fights for d4 from the wing, refusing the symmetry of 1...e5.") {
                move("Nf3", "Open Sicilian, Preparation", "Develops and prepares d4, the pawn break that defines the Open Sicilian.") {
                    move("d6", "Najdorf Set-up", "Restrains e5 and clears the path for the classical Najdorf structure.") {
                        move("d4", "Open Sicilian", "Trades the d-pawn for the c-pawn and an open, attacking game.") {
                            move("cxd4", "Open Sicilian, Exchange", "Black must take or the centre simply falls to White.") {
                                move("Nxd4", "Open Sicilian, Main Line", "Recaptures with the knight, keeping the queen's file half-open.") {
                                    move("Nf6", "Open Sicilian, Knight Development", "Develops with tempo against the undefended e4 pawn.") {
                                        move("Nc3", "Open Sicilian, Classical Centre", "Defends e4 and completes the classic Open Sicilian skeleton.") {
                                            move("a6", "Najdorf Variation", "A quiet rook-pawn move that takes b5 away from both white knights forever.")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    move("Nc6", "Old Sicilian", "Develops toward d4 and keeps every pawn structure open.") {
                        move("d4", "Open Sicilian", "Trades the d-pawn for the c-pawn and an open, attacking game.") {
                            move("cxd4", "Open Sicilian, Exchange", "Black must take or the centre simply falls to White.") {
                                move("Nxd4", "Open Sicilian, Main Line", "Recaptures with the knight, keeping the queen's file half-open.") {
                                    move("Nf6", "Sveshnikov Preparation", "Hits e4 and provokes Nc3 before revealing Black's pawn plan.") {
                                        move("Nc3", "Open Sicilian, Classical Centre", "Defends e4 and completes the classic Open Sicilian skeleton.") {
                                            move("e5", "Sveshnikov Variation", "Boldly punts the d4 knight backwards, accepting a d5 hole for free piece play.") {
                                                move("Ndb5", "Sveshnikov, Knight Raid", "Heads for d6 — the hole on d6 is the whole argument against ...e5.")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    move("e6", "French Sicilian", "Keeps the d5 break in reserve and opens the queen's diagonal.") {
                        move("d4", "Open Sicilian", "Trades the d-pawn for the c-pawn and an open, attacking game.") {
                            move("cxd4", "Open Sicilian, Exchange", "Black must take or the centre simply falls to White.") {
                                move("Nxd4", "Open Sicilian, Main Line", "Recaptures with the knight, keeping the queen's file half-open.") {
                                    move("a6", "Kan Variation", "Flexibility above all: b5 is stopped, and every black piece keeps its options.")
                                }
                            }
                        }
                    }
                }
                move("Nc3", "Closed Sicilian", "Declines the open fight — White will build slowly behind the pawn chain.") {
                    move("Nc6", "Closed Sicilian, Traditional", "Develops normally; the queenside pawns will do Black's talking later.") {
                        move("g3", "Closed Sicilian, Fianchetto", "Points the bishop at the long diagonal where the position will stay closed.") {
                            move("g6", "Closed Sicilian, Double Fianchetto", "Mirrors the plan — both kings will live behind fianchettoed walls.") {
                                move("Bg2", "Closed Sicilian, Main Line", "Completes the fianchetto; e4 is overprotected and f4 comes next.") {
                                    move("Bg7", "Closed Sicilian, Symmetry", "Finishes Black's mirror; the fight will be decided by the pawn breaks f4 and b5.") {
                                        move("d3", "Closed Sicilian, Botvinnik Set-up", "A modest pawn move that frees the c1 bishop and cements e4 for good.")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
