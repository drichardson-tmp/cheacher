package com.cheacher.app.data

import com.cheacher.app.chess.Color
import com.cheacher.app.domain.Repertoire
import com.cheacher.app.domain.repertoire

/** A first book from Black's chair: one coherent French repertoire against White's main tries. */
val frenchAsBlack: Repertoire = repertoire(
    id = "french-black",
    title = "The French, as Black",
    perspective = Color.BLACK,
    subtitle = "Meet 1. e4 with one durable pawn chain",
) {
    move("e4", "King's Pawn Opening", "White takes the centre; Black will challenge its base rather than mirror it.") {
        move("e6", "French Defence", "Prepares ...d5 while leaving the king's knight free to choose its best square.") {
            move("d4", "French, Main Centre", "White builds the classical two-pawn centre that gives the defence its target.") {
                move("d5", "French, Main Position", "Locks onto e4 and makes d4 the base Black will attack with ...c5.") {
                    move("Nc3", "French, Classical Development", "White protects e4 and keeps both the Winawer and Classical on the table.") {
                        move("Nf6", "French, Classical Variation", "Attacks e4 immediately and develops without surrendering the dark bishop.") {
                            move("e5", "French, Steinitz Centre", "White gains space and asks the knight where it belongs.") {
                                move("Nfd7", "French, Steinitz Retreat", "The knight reroutes toward c5 and frees the f-pawn to challenge the centre.") {
                                    move("f4", "Steinitz, Space Chain", "White reinforces e5 and signals a kingside expansion.") {
                                        move("c5", "Steinitz, Base Strike", "Hits d4, the base of White's chain — the French plan in its purest form.") {
                                            move("Nf3", "Steinitz, Development", "White develops while holding d4 with a piece.") {
                                                move("Nc6", "Steinitz, Main Tabiya", "Adds a second attacker to d4 and completes Black's queenside pressure.")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    move("e5", "French, Advance Variation", "White closes the centre and claims space before developing.") {
                        move("c5", "Advance, Immediate Strike", "Attacks d4 before White can make the pawn chain comfortable.") {
                            move("c3", "Advance, Chain Support", "White reinforces d4 and prepares a compact centre.") {
                                move("Nc6", "Advance, Development", "Develops another attacker against d4 and keeps ...Qb6 ready.") {
                                    move("Nf3", "Advance, Main Set-up", "White develops the king's knight behind the chain.") {
                                        move("Qb6", "Advance, Double Pressure", "Hits d4 and b2 at once, forcing White to solve two weaknesses.")
                                    }
                                }
                            }
                        }
                    }
                    move("exd5", "French, Exchange Variation", "White releases the tension and trades space for open development.") {
                        move("exd5", "Exchange, Symmetrical Centre", "Recaptures with the e-pawn so Black's pieces receive open central lines.") {
                            move("Bd3", "Exchange, Active Bishop", "White points at h7 before the kings are safe.") {
                                move("Bd6", "Exchange, Matching Development", "Meets the bishop symmetrically and prepares a calm castle.") {
                                    move("Nf3", "Exchange, Natural Development", "White brings the king's knight toward the centre.") {
                                        move("Nf6", "Exchange, Main Tabiya", "Black completes development with equality and no bad bishop.")
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
