package com.cheacher.app.data

import com.cheacher.app.chess.Color
import com.cheacher.app.domain.Repertoire
import com.cheacher.app.domain.repertoire

/**
 * The 1. e4 e5 shelf — the oldest chess there is.
 *
 * [openGame] is the map of the whole territory: every serious second move for White and
 * the defences that answer them. [ruyLopez] is the one deep dive, because the Spanish
 * has more theory than the rest of the Open Game put together.
 */

/** Every mainstream way to meet 1...e5, one branch each, named to the tabiya. */
val openGame: Repertoire = repertoire(
    id = "open-game",
    title = "The Open Game",
    perspective = Color.WHITE,
    subtitle = "Five doors out of 1. e4 e5",
) {
    move("e4", "King's Pawn Opening", "Stakes the centre and frees the bishop and queen in one stroke.") {
        move("e5", "Open Game", "Black answers symmetrically and contests the centre head-on.") {
            move("Nf3", "King's Knight Opening", "Develops with tempo by attacking the e5 pawn.") {
                move("Nc6", "Normal Variation", "Defends e5 with a developing move rather than a pawn.") {
                    move("d4", "Scotch Game", "Blows the centre open immediately instead of building up behind it.") {
                        move("exd4", "Scotch, Exchange", "Black takes because ...d6 would concede the centre for nothing.") {
                            move("Nxd4", "Scotch Game, Main Line", "Recaptures with the knight and hands both sides open lines.") {
                                move("Bc5", "Scotch, Classical Variation", "Pins the knight to the idea of ...Qf6 and eyes f2.") {
                                    move("Be3", "Scotch, Classical Main Line", "Props up the d4 knight so the bishop on c5 has nothing to bite on.") {
                                        move("Qf6", "Scotch, Classical Queen Sortie", "Piles onto d4 before White can consolidate.") {
                                            move("c3", "Scotch, Classical Tabiya", "A third defender of d4 — the tension holds and White keeps a space edge.")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    move("Nc3", "Three Knights Game", "Develops the last minor piece toward the centre and keeps every option open.") {
                        move("Nf6", "Four Knights Game", "Symmetry taken to its logical end; solid and famously drawish.") {
                            move("Bb5", "Spanish Four Knights", "Borrows the Ruy Lopez idea inside a symmetrical position.") {
                                move("Bb4", "Four Knights, Symmetrical", "Copies the pin right back — the mirror holds one move longer.") {
                                    move("O-O", "Four Knights, Castling", "The first player to break symmetry is usually the one who benefits.") {
                                        move("O-O", "Four Knights, Double Castling", "Black keeps mirroring because there is nothing better yet.") {
                                            move("d3", "Four Knights, Main Line", "Shores up e4 and finally asks Black to find an independent plan.")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                move("Nf6", "Petrov's Defence", "Ignores the threat and counterattacks e4 — the great equaliser.") {
                    move("Nxe5", "Petrov, Main Line", "Grabs the pawn; the immediate recapture is the trap Black must avoid.") {
                        move("d6", "Petrov, Classical", "Kicks the knight first, because 2...Nxe4 straight away loses material to Qe2.") {
                            move("Nf3", "Petrov, Knight Retreat", "Steps back to safety, now that the e-file is open and both centres are gone.") {
                                move("Nxe4", "Petrov, Pawn Recapture", "Restores material equality on the square the whole line was about.") {
                                    move("d4", "Petrov, Centre Push", "Gains space and opens the c1 bishop before Black can consolidate.") {
                                        move("d5", "Petrov, Symmetrical Centre", "Props the e4 knight with a pawn instead of retreating it.") {
                                            move("Bd3", "Petrov, Classical Attack", "Points at h7 and prepares to challenge the outpost knight with c4 or Re1.")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                move("d6", "Philidor Defence", "Defends e5 with a pawn — cramped but genuinely hard to crack.") {
                    move("d4", "Philidor, Main Line", "Strikes at the pawn chain while Black's pieces are still at home.") {
                        move("exd4", "Philidor, Exchange", "Releases the tension rather than live with a permanently cramped centre.") {
                            move("Nxd4", "Philidor, Open Variation", "Recaptures in the centre with a lead in development.") {
                                move("Nf6", "Philidor, Knight Development", "Hits e4 and gets on with castling.") {
                                    move("Nc3", "Philidor, Classical Set-up", "Defends e4 and completes the standard White formation.") {
                                        move("Be7", "Philidor, Hanham Set-up", "A modest bishop move; Black's whole plan is to castle and untangle.")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            move("Nc3", "Vienna Game", "Develops before committing the f- or d-pawn, keeping f4 in reserve.") {
                move("Nf6", "Vienna Game, Falkbeer Variation", "The principled reply: hit e4 while it is still loose.") {
                    move("f4", "Vienna Gambit", "A King's Gambit with an extra developing move already banked.") {
                        move("d5", "Vienna Gambit, Main Line", "Counter in the centre — taking on f4 here hands White a free attack.") {
                            move("fxe5", "Vienna Gambit, Exchange", "Wins the e5 pawn and opens the f-file toward Black's king.") {
                                move("Nxe4", "Vienna Gambit, Knight Grab", "Restores the pawn and plants a knight in the middle of the board.") {
                                    move("Nf3", "Vienna Gambit, Development", "Ignores the knight and develops — the e5 pawn is worth more than the tempo.")
                                }
                            }
                        }
                    }
                }
                move("Nc6", "Vienna Game, Max Lange Defence", "Develops toward d4 and waits to see whether White commits to f4.") {
                    move("Bc4", "Vienna Game, Bishop Variation", "Takes aim at f7 and keeps the option of f4 for one more move.") {
                        move("Nf6", "Vienna, Bishop Main Line", "Finally challenges e4, inviting the sharp d3 and f4 build-up.") {
                            move("d3", "Vienna, Quiet Build-up", "Cements e4 so that f4 can come next with everything defended.")
                        }
                    }
                }
            }
            move("Bc4", "Bishop's Opening", "The Italian bishop without committing the king's knight — f4 stays available.") {
                move("Nf6", "Bishop's Opening, Berlin Defence", "Counterattacks e4 before White can build the ideal centre.") {
                    move("d3", "Bishop's Opening, Quiet Line", "Defends e4 modestly and steers toward a slow Italian-style squeeze.") {
                        move("c6", "Bishop's Opening, Philidor Counterattack", "Prepares ...d5 and takes b5 and d5 away from White's pieces.") {
                            move("Nf3", "Bishop's Opening, Main Line", "Completes development; the game becomes a Giuoco Pianissimo by another road.")
                        }
                    }
                }
            }
            move("f4", "King's Gambit", "Offers a pawn to rip open the f-file and own the centre — chess's oldest wager.") {
                move("exf4", "King's Gambit Accepted", "Takes the pawn; declining it politely is respectable but concedes the point.") {
                    move("Nf3", "King's Knight Gambit", "Stops ...Qh4+ before it starts and eyes the recapture on f4.") {
                        move("d6", "Fischer Defence", "Bobby Fischer's refutation attempt: hold f4 with ...g5 but keep e5 covered.") {
                            move("d4", "Fischer Defence, Main Line", "Grabs the full centre while Black spends moves clinging to the extra pawn.")
                        }
                    }
                }
                move("d5", "Falkbeer Counter-Gambit", "Refuses the pawn and offers one back to open lines while White's king is loose.") {
                    move("exd5", "Falkbeer, Accepted", "Takes, because declining leaves Black with a free hand in the centre.") {
                        move("e4", "Falkbeer, Main Line", "Cramps White by taking f3 away from the knight — that is the whole gambit.")
                    }
                }
            }
            move("d4", "Centre Game", "Opens the centre at once and accepts an early queen sortie as the price.") {
                move("exd4", "Centre Game, Accepted", "Black must take; anything else lets White keep both centre pawns.") {
                    move("Qxd4", "Centre Game, Main Line", "Recaptures with the queen, knowing it will be chased — but to a useful square.") {
                        move("Nc6", "Centre Game, Normal Variation", "Develops with tempo by kicking the queen.") {
                            move("Qe3", "Centre Game, Paulsen Attack", "Sidesteps to a square where the queen supports a queenside castle and a kingside storm.")
                        }
                    }
                }
            }
        }
    }
}

/** The Spanish: four defences, each carried to its recognisable tabiya. */
val ruyLopez: Repertoire = repertoire(
    id = "ruy-lopez",
    title = "The Ruy Lopez",
    perspective = Color.WHITE,
    subtitle = "The Spanish question, and four answers",
) {
    move("e4", "King's Pawn Opening", "Stakes the centre and frees the bishop and queen in one stroke.") {
        move("e5", "Open Game", "Black answers symmetrically and contests the centre head-on.") {
            move("Nf3", "King's Knight Opening", "Develops with tempo by attacking the e5 pawn.") {
                move("Nc6", "Normal Variation", "Defends e5 with a developing move rather than a pawn.") {
                    move("Bb5", "Ruy Lopez", "Pressures the knight that guards e5, asking Black a question that lasts all game.") {
                        move("a6", "Morphy Defence", "Puts the question back: retreat or trade, but decide now.") {
                            move("Ba4", "Ruy Lopez, Closed Set-up", "Keeps the bishop on the deadly diagonal; the pin is renewable at any moment.") {
                                move("Nf6", "Ruy Lopez, Main Line", "Develops and attacks e4 — Black's most testing move order.") {
                                    move("O-O", "Ruy Lopez, Castling", "Ignores e4 entirely: after ...Nxe4, d4 wins the pawn straight back.") {
                                        move("Be7", "Closed Ruy Lopez", "Breaks the pin, prepares to castle, and settles in for the long squeeze.") {
                                            move("Re1", "Closed Ruy, Rook Lift", "Finally defends e4 for real and puts the rook on the file that will open.") {
                                                move("b5", "Closed Ruy, Queenside Expansion", "Kicks the bishop before it can ever take on c6 with effect.") {
                                                    move("Bb3", "Closed Ruy, Bishop Retreat", "Retreats to the second-best diagonal, still glaring at f7.") {
                                                        move("d6", "Closed Ruy, Main Line", "Solidifies e5 and opens the c8 bishop's road out.") {
                                                            move("c3", "Closed Ruy, c3 Centre", "Prepares d4 — the whole Spanish plan in one modest pawn move.") {
                                                                move("O-O", "Closed Ruy, Tabiya", "Black completes development and the real manoeuvring game begins.") {
                                                                    move("h3", "Closed Ruy, Main Tabiya", "Takes g4 from the bishop for good before playing d4; the most-played position in chess.")
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        move("Nxe4", "Open Defence", "Takes the pawn and plays for piece activity instead of the slow squeeze.") {
                                            move("d4", "Open Defence, Centre Strike", "Opens the centre against the loose knight rather than regaining the pawn at once.") {
                                                move("b5", "Open Defence, Main Line", "Must be played — the bishop was about to trap the whole queenside.") {
                                                    move("Bb3", "Open Defence, Bishop Retreat", "Keeps the bishop pointed at f7 and at the coming d5 pawn.") {
                                                        move("d5", "Open Defence, Tabiya", "Props the e4 knight with a pawn and stakes out the centre.") {
                                                            move("dxe5", "Open Defence, Exchange", "Opens the d-file and leaves Black's knight on an outpost that needs constant care.") {
                                                                move("Be6", "Open Defence, Main Tabiya", "Shores up d5 and completes development; material is level, the structure is not.")
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            move("Bxc6", "Ruy Lopez, Exchange Variation", "Trades at once to wreck Black's pawn structure — a pure endgame bet.") {
                                move("dxc6", "Exchange Variation, Main Line", "Recaptures toward the centre and opens the queen and bishop.") {
                                    move("O-O", "Exchange Variation, Castling", "Declines to grab e5 (the ...Qd4 fork) and simply gets the king safe.") {
                                        move("f6", "Exchange Variation, Bronstein Defence", "Props up e5 permanently at the cost of a slightly airy king.") {
                                            move("d4", "Exchange Variation, Tabiya", "Opens the centre where White's healthy kingside majority will tell in the endgame.")
                                        }
                                    }
                                }
                            }
                        }
                        move("Nf6", "Berlin Defence", "Skips ...a6 and heads straight for the endgame Kramnik made famous.") {
                            move("O-O", "Berlin Defence, Main Line", "Offers the e4 pawn; the resulting endgame is the whole point of the line.") {
                                move("Nxe4", "Berlin, Open Variation", "Accepts the invitation — anything else transposes back to normal Spanish play.") {
                                    move("d4", "Berlin, Centre Strike", "Opens the centre before Black can hold the extra pawn with ...d5.") {
                                        move("Nd6", "Berlin, Knight Retreat", "Retreats while hitting the b5 bishop, forcing the exchange sequence.") {
                                            move("Bxc6", "Berlin, Exchange", "Takes first, because the bishop had nowhere better and c6 is now damaged.") {
                                                move("dxc6", "Berlin, Recapture", "Recaptures toward the centre, opening the queen's road to d1.") {
                                                    move("dxe5", "Berlin, Pawn Grab", "Restores material and kicks the knight one more time.") {
                                                        move("Nf5", "Berlin, Knight Regroup", "Heads for a square where it eyes d4 and can drop back to e7.") {
                                                            move("Qxd8+", "Berlin, Queen Trade", "Forces the trade Black invited: no queens, but no castling either.") {
                                                                move("Kxd8", "Berlin Wall Endgame", "The famous position: Black's king walks, the bishop pair compensates the broken pawns.")
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
                        move("d6", "Steinitz Defence", "Solid and passive: hold e5 with a pawn and break the pin later.") {
                            move("d4", "Steinitz, Centre Strike", "Punishes the passivity immediately by opening the centre.") {
                                move("Bd7", "Steinitz, Main Line", "Unpins by defending the knight so ...exd4 becomes possible next move.") {
                                    move("Nc3", "Steinitz, Classical Set-up", "Develops and overprotects e4 rather than resolving the centre early.") {
                                        move("Nf6", "Steinitz, Knight Development", "Gets on with development now that the pin has been neutralised.") {
                                            move("O-O", "Steinitz, Tabiya", "King to safety; White holds a lasting space edge with no risk at all.")
                                        }
                                    }
                                }
                            }
                        }
                        move("f5", "Schliemann Defence", "A wild pawn stab that ignores development to blow the centre open.") {
                            move("Nc3", "Schliemann, Main Line", "Adds a defender to e4 instead of grabbing material and getting attacked.") {
                                move("fxe4", "Schliemann, Accepted", "Opens the f-file toward White's king — the point of the whole gambit.") {
                                    move("Nxe4", "Schliemann, Knight Recapture", "Recaptures with a centralised knight and threatens to hop into d6 or g5.") {
                                        move("d5", "Schliemann, Tabiya", "Kicks the knight and opens the c8 bishop in one move.") {
                                            move("Nxe5", "Schliemann, Main Tabiya", "Grabs the loose e5 pawn; the position is sharp and both kings are still in the middle.")
                                        }
                                    }
                                }
                            }
                        }
                        move("Bc5", "Classical Defence", "Develops naturally and dares White to prove the pin means anything.") {
                            move("c3", "Classical Defence, c3 Line", "Prepares d4 to hit the bishop and the centre in one stroke.") {
                                move("Nf6", "Classical Defence, Main Line", "Develops with a hit on e4 before White gets d4 in.") {
                                    move("O-O", "Classical Defence, Tabiya", "Castles and leaves d4 hanging in the air as a permanent threat.")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
