package com.cheacher.app.data

import com.cheacher.app.chess.Color
import com.cheacher.app.domain.Repertoire
import com.cheacher.app.domain.repertoire

/**
 * The 1. d4 shelf. Three books: the Queen's Gambit against 1...d5, the Indian defences
 * against 1...Nf6, and the system openings for players who would rather learn a
 * structure than a hundred variations.
 */

/** 1. d4 d5 2. c4 — the oldest and best-tested opening in chess. */
val queensGambit: Repertoire = repertoire(
    id = "queens-gambit",
    title = "The Queen's Gambit",
    perspective = Color.WHITE,
    subtitle = "A pawn offered, never really given",
) {
    move("d4", "Queen's Pawn Opening", "Takes the centre with the pawn the queen already defends.") {
        move("d5", "Closed Game", "Black meets the centre head-on and the game will be a slow, structural one.") {
            move("c4", "Queen's Gambit", "Offers a wing pawn for the centre — Black cannot hold it, so it is a gambit in name only.") {
                move("e6", "Queen's Gambit Declined", "The most respectable decline: keep d5 defended and accept a passive bishop.") {
                    move("Nc3", "QGD, Main Line", "Adds a third attacker to d5 before Black can finish developing.") {
                        move("Nf6", "QGD, Normal Position", "Develops, defends d5 again, and prepares to castle.") {
                            move("Bg5", "QGD, Classical Variation", "Pins the knight so the pressure on d5 becomes real.") {
                                move("Be7", "QGD, Orthodox Defence", "Breaks the pin the solid way; nothing fancy, nothing loose.") {
                                    move("e3", "QGD, Orthodox Main Line", "A humble move that frees the bishop and makes the centre bulletproof.") {
                                        move("O-O", "QGD, Castling", "King to safety before the position opens up.") {
                                            move("Nf3", "QGD, Development", "The last minor piece comes out and White is fully mobilised.") {
                                                move("h6", "QGD, Tartakower Set-up", "Puts the question to the bishop before committing to a plan.") {
                                                    move("Bh4", "QGD, Bishop Retreat", "Keeps the pin; taking on f6 would help Black's development.") {
                                                        move("b6", "Tartakower Defence", "Solves the problem bishop by fianchettoing it — the modern main line.")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    move("Nf3", "QGD, Catalan Move Order", "Delays Nc3 so the bishop can be fianchettoed instead.") {
                        move("Nf6", "Catalan, Normal Position", "Black develops naturally and awaits White's set-up.") {
                            move("g3", "Catalan Opening", "The Catalan bishop: it will pressure d5 and c6 down the long diagonal all game.") {
                                move("Be7", "Catalan, Closed Set-up", "Prepares to castle rather than grab the c4 pawn immediately.") {
                                    move("Bg2", "Catalan, Fianchetto", "Completes the idea; the bishop is worth the tempo spent building it.") {
                                        move("O-O", "Catalan, Castling", "Both sides finish the opening before the pawn question is settled.") {
                                            move("O-O", "Catalan, Main Line", "White castles too and leaves c4 hanging as a long-term investment.") {
                                                move("dxc4", "Open Catalan", "Black finally takes; holding the pawn will cost time White will use.")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                move("c6", "Slav Defence", "Defends d5 with the c-pawn so the light-squared bishop stays free.") {
                    move("Nf3", "Slav, Main Line", "Develops and stops ...e5 before it can be considered.") {
                        move("Nf6", "Slav, Normal Position", "Develops and adds another defender to the d5 pawn.") {
                            move("Nc3", "Slav, Three Knights", "Piles onto d5; Black must now decide whether to take on c4.") {
                                move("dxc4", "Slav Accepted", "Takes, planning ...b5 to hold the pawn — the whole point of ...c6.") {
                                    move("a4", "Slav, Main Line", "Stops ...b5 for good, at the cost of a permanent hole on b4.") {
                                        move("Bf5", "Slav, Classical Tabiya", "The bishop gets out before ...e6 shuts it in — Black's entire opening argument.") {
                                            move("e3", "Slav, Main Tabiya", "Solid: regain the pawn with Bxc4 and play a good structural game.")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                move("dxc4", "Queen's Gambit Accepted", "Takes the pawn to trade the centre for time — Black will not hold it.") {
                    move("Nf3", "QGA, Main Line", "Stops ...e5 before regaining the pawn; move order matters here.") {
                        move("Nf6", "QGA, Normal Position", "Develops and prevents e4 for one more move.") {
                            move("e3", "QGA, Classical Variation", "Prepares Bxc4 and a healthy centre; the pawn comes back with interest.") {
                                move("e6", "QGA, Classical Defence", "Opens the bishop and prepares the freeing break ...c5.") {
                                    move("Bxc4", "QGA, Recapture", "The pawn returns and the bishop lands on its best diagonal.") {
                                        move("c5", "QGA, Tabiya", "The freeing break; Black accepts an isolated pawn for free piece play.") {
                                            move("O-O", "QGA, Main Tabiya", "King safe, centre flexible, and the d4-d5 break waiting in reserve.")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                move("e5", "Albin Counter-Gambit", "Answers a gambit with a gambit: a pawn for a wedge on d4.") {
                    move("dxe5", "Albin, Accepted", "Takes; declining would let Black have the centre for free.") {
                        move("d4", "Albin, Main Line", "The point: the pawn on d4 cramps White and takes c3 from the knight.") {
                            move("Nf3", "Albin, Development", "Develops and attacks the advanced pawn rather than trying to win it at once.") {
                                move("Nc6", "Albin, Tabiya", "Defends d4 and hits e5 — Black has real activity for the pawn.") {
                                    move("g3", "Albin, Main Tabiya", "Fianchettoes to pressure d4 from behind; the safest way to keep the extra pawn.")
                                }
                            }
                        }
                    }
                }
                move("Nf6", "Marshall Defence", "Ignores the attack on d5 and simply develops — dubious but sharp.") {
                    move("cxd5", "Marshall, Accepted", "Wins the pawn immediately; the whole line stands or falls on this.") {
                        move("Nxd5", "Marshall, Recapture", "Takes back with the knight, which will now be chased around the board.") {
                            move("e4", "Marshall, Main Line", "Kicks the knight and takes the whole centre in one move.") {
                                move("Nf6", "Marshall, Retreat", "Back home; Black has lost time and White has a dream centre.") {
                                    move("Nc3", "Marshall, Tabiya", "Develops and defends e4; White is close to winning by development alone.")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 1. d4 Nf6 2. c4 — the Indian defences, where Black lets White build and then attacks it. */
val indianDefences: Repertoire = repertoire(
    id = "indian-defences",
    title = "The Indian Defences",
    perspective = Color.WHITE,
    subtitle = "Nimzo, King's Indian, Grünfeld, Benoni",
) {
    move("d4", "Queen's Pawn Opening", "Takes the centre with the pawn the queen already defends.") {
        move("Nf6", "Indian Defence", "Stops e4 with a piece instead of a pawn — the modern way to meet 1. d4.") {
            move("c4", "Indian Game, Normal Position", "Fights for d5 and prepares Nc3, the standard queen's-pawn build.") {
                move("e6", "Indian Game, East Indian Set-up", "Opens the bishop's diagonal and keeps both ...d5 and ...Bb4 available.") {
                    move("Nc3", "Indian Game, Nimzo Move Order", "Develops toward the centre and invites the pin that follows.") {
                        move("Bb4", "Nimzo-Indian Defence", "Pins the knight that guards e4 — the purest expression of hypermodern chess.") {
                            move("e3", "Nimzo-Indian, Rubinstein Variation", "Quiet and flexible: develop the kingside first and settle the pin later.") {
                                move("O-O", "Rubinstein, Castling", "King safety first; Black's plan depends on what White does next.") {
                                    move("Bd3", "Rubinstein, Main Line", "Aims at h7 and prepares Nge2 or Nf3 with a big centre to come.") {
                                        move("d5", "Rubinstein, Classical Tabiya", "Stakes out the centre before White can play e4 in comfort.") {
                                            move("Nf3", "Rubinstein, Development", "The natural square, keeping e4 as a long-term ambition.") {
                                                move("c5", "Rubinstein, Main Tabiya", "The second break; the centre is fully contested and the game is balanced.")
                                            }
                                        }
                                    }
                                }
                            }
                            move("Qc2", "Nimzo-Indian, Classical Variation", "Defends c3 in advance so the bishop pair survives the pin.") {
                                move("O-O", "Classical Nimzo, Castling", "Black castles and waits for White to spend a move on a3.") {
                                    move("a3", "Classical Nimzo, Main Line", "Puts the question now, because Qc2 means the recapture is with the queen.") {
                                        move("Bxc3+", "Classical Nimzo, Trade", "Takes; the alternative is a retreat that admits the pin was pointless.") {
                                            move("Qxc3", "Classical Nimzo, Tabiya", "Recaptures with the queen: no doubled pawns, and the bishop pair is White's.")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    move("Nf3", "Indian Game, Queen's Indian Move Order", "Sidesteps the Nimzo pin by developing the other knight first.") {
                        move("b6", "Queen's Indian Defence", "Fianchettoes to fight for e4 from a distance — solid and elastic.") {
                            move("g3", "Queen's Indian, Fianchetto Variation", "Meets a fianchetto with a fianchetto; the long diagonal is the battleground.") {
                                move("Bb7", "Queen's Indian, Main Line", "Completes the plan; e4 is now guarded twice from far away.") {
                                    move("Bg2", "Queen's Indian, Bishop Duel", "The bishops stare at each other and neither side can force the issue yet.") {
                                        move("Be7", "Queen's Indian, Tabiya", "Modest and correct: castle, then challenge the centre with ...d5 or ...c5.") {
                                            move("O-O", "Queen's Indian, Main Tabiya", "Both sides are developed; the game will be decided by the central breaks.")
                                        }
                                    }
                                }
                            }
                        }
                        move("Bb4+", "Bogo-Indian Defence", "A check that develops, and a cheap way to avoid a mountain of theory.") {
                            move("Bd2", "Bogo-Indian, Main Line", "Blocks with the bishop and offers a trade that suits White's space edge.")
                        }
                    }
                }
                move("g6", "King's Indian Set-up", "Announces the fianchetto: Black will give up the centre and attack it later.") {
                    move("Nc3", "Indian Game, Normal Development", "The knight belongs on c3, whichever Indian defence Black chooses.") {
                        move("Bg7", "King's Indian Defence", "The bishop takes the long diagonal it will fight on for the whole game.") {
                            move("e4", "King's Indian, Classical Variation", "Takes the whole centre — exactly what Black invited, and still strong.") {
                                move("d6", "King's Indian, Main Line", "Stops e5 and prepares the ...e5 break that defines Black's counterplay.") {
                                    move("Nf3", "King's Indian, Classical Development", "The most natural square, guarding e5 and preparing to castle.") {
                                        move("O-O", "King's Indian, Castling", "The king gets safe before the pawn storms begin on both wings.") {
                                            move("Be2", "King's Indian, Classical Main Line", "A modest square that keeps the bishop safe from ...Ng4 tricks.") {
                                                move("e5", "King's Indian, Classical Tabiya", "The thematic break; the centre locks and both sides attack on opposite wings.") {
                                                    move("O-O", "King's Indian, Main Tabiya", "White castles into the storm, trusting the queenside play to be faster.") {
                                                        move("Nc6", "Mar del Plata Variation", "Provokes d5, because a closed centre is what Black's kingside attack needs.") {
                                                            move("d5", "Mar del Plata, Main Line", "Closes the centre and starts the race: c5 and b4 against f5 and g5.")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        move("d5", "Grünfeld Defence", "Strikes at the centre before the bishop is even out — hypermodernism at its sharpest.") {
                            move("cxd5", "Grünfeld, Exchange Variation", "Takes, because the alternative lets Black settle comfortably on d5.") {
                                move("Nxd5", "Grünfeld, Recapture", "The knight lands in the centre, where it will be chased for a tempo.") {
                                    move("e4", "Grünfeld, Main Line", "Kicks the knight and builds the giant centre the Grünfeld exists to attack.") {
                                        move("Nxc3", "Grünfeld, Knight Trade", "Trades before retreating, damaging the pawns that hold the centre together.") {
                                            move("bxc3", "Grünfeld, Doubled Pawns", "Doubled but strong: c3 and d4 form the broadest centre in opening theory.") {
                                                move("Bg7", "Grünfeld, Tabiya", "Now the fianchetto makes sense — the bishop points straight at d4.") {
                                                    move("Nf3", "Grünfeld, Development", "Defends d4 a second time before Black's pressure arrives.") {
                                                        move("c5", "Grünfeld, Main Tabiya", "The break that tests everything: the centre must hold or the game is Black's.")
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
                move("c5", "Benoni Set-up", "Attacks d4 from the wing at once and asks White to commit the centre.") {
                    move("d5", "Benoni, Advance", "Pushes past; the space edge is real but the d5 pawn will need care.") {
                        move("e6", "Modern Benoni", "Undermines the head of the chain instead of blockading it.") {
                            move("Nc3", "Modern Benoni, Main Line", "Develops and defends d5 before resolving the tension on e6.") {
                                move("exd5", "Modern Benoni, Exchange", "Takes, giving Black the half-open e-file and a queenside pawn majority.") {
                                    move("cxd5", "Modern Benoni, Recapture", "The correct recapture: it keeps the protected passer and the space.") {
                                        move("d6", "Modern Benoni, Tabiya", "Builds the classic Benoni structure and prepares the fianchetto.") {
                                            move("e4", "Modern Benoni, Classical Centre", "Takes the rest of the centre; White has space, Black has the ...b5 break.") {
                                                move("g6", "Modern Benoni, Fianchetto", "The bishop belongs on g7, staring down the long diagonal at White's centre.") {
                                                    move("Nf3", "Modern Benoni, Main Tabiya", "Completes development; the game is a classic space-versus-activity fight.")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        move("b5", "Benko Gambit", "A whole pawn for two open files and permanent queenside pressure.") {
                            move("cxb5", "Benko Gambit, Accepted", "Takes; the pawn is real and White should not be bluffed out of it.") {
                                move("a6", "Benko Gambit, Main Line", "Offers the second pawn to blast open the a- and b-files.") {
                                    move("bxa6", "Benko, Fully Accepted", "Takes everything, accepting a long defensive task for two extra pawns.") {
                                        move("Bxa6", "Benko, Tabiya", "The compensation: two open files, a great bishop, and no risk of losing quickly.")
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

/** Systems, not variations: the same set-up against almost anything Black does. */
val queensPawnSystems: Repertoire = repertoire(
    id = "d4-systems",
    title = "Queen's Pawn Systems",
    perspective = Color.WHITE,
    subtitle = "London, Colle, Trompowsky, Jobava",
) {
    move("d4", "Queen's Pawn Opening", "Takes the centre with the pawn the queen already defends.") {
        move("d5", "Closed Game", "Black meets the centre head-on and the game will be a slow, structural one.") {
            move("Bf4", "London System", "The bishop leaves the pawn chain before ...e6 can ever trap it — the system's one rule.") {
                move("Nf6", "London System, Main Line", "Black develops naturally; the London is set up against almost anything.") {
                    move("e3", "London, Pawn Wall", "Builds the c3-d4-e3 wall that makes the London so hard to attack.") {
                        move("e6", "London, Classical Defence", "A solid reply, at the cost of shutting in the light-squared bishop.") {
                            move("Nf3", "London, Development", "Defends d4 a second time and prepares the standard c3 and Nbd2 set-up.") {
                                move("c5", "London, Tabiya", "The critical break; d4 is the only square in White's structure worth attacking.") {
                                    move("c3", "London, Main Tabiya", "Holds d4 for good; White will finish with Nbd2, Bd3 and castle.")
                                }
                            }
                        }
                    }
                }
            }
            move("Nf3", "Queen's Pawn Game, Zukertort Set-up", "Develops first and keeps London, Colle and Catalan all on the table.") {
                move("Nf6", "Queen's Pawn Game, Symmetrical", "Black mirrors and waits for White to reveal the system.") {
                    move("e3", "Colle System", "A modest wall with one idea: build up, then break with e3-e4.") {
                        move("e6", "Colle, Classical Defence", "Solid and symmetrical; both sides are building the same wall.") {
                            move("Bd3", "Colle, Main Line", "The bishop takes the diagonal it needs for the e4 break and the h7 attack.") {
                                move("c5", "Colle, Tabiya", "Hits d4 to provoke a resolution before White's pieces are all in place.") {
                                    move("c3", "Colle, Main Tabiya", "Holds the centre; Nbd2, O-O and e4 follow in that order, every game.")
                                }
                            }
                        }
                    }
                }
            }
            move("e4", "Blackmar-Diemer Gambit", "A pawn for open lines and a violent attack — unsound, and terrifying over the board.") {
                move("dxe4", "Blackmar-Diemer, Accepted", "Takes; declining lets White have the perfect centre for free.") {
                    move("Nc3", "Blackmar-Diemer, Main Line", "Develops with a hit on the pawn and prepares the f3 break.") {
                        move("Nf6", "Blackmar-Diemer, Normal Defence", "Defends the pawn the only sensible way — with a developing move.") {
                            move("f3", "Blackmar-Diemer, Gambit Tabiya", "The whole idea: open the f-file toward f7 whether Black takes or not.")
                        }
                    }
                }
            }
        }
        move("Nf6", "Indian Defence", "Stops e4 with a piece instead of a pawn — the modern way to meet 1. d4.") {
            move("Bg5", "Trompowsky Attack", "Pins the knight on move two and dodges every Indian defence at once.") {
                move("Ne4", "Trompowsky, Main Line", "The critical reply: hit the bishop and gain time before it settles.") {
                    move("Bf4", "Trompowsky, Bishop Retreat", "Keeps the bishop active on the diagonal that matters after ...d5.") {
                        move("d5", "Trompowsky, Classical Defence", "Stakes out the centre and supports the advanced knight.") {
                            move("e3", "Trompowsky, Development", "Solid: build the wall, then challenge the e4 knight with Nd2 or Bd3.") {
                                move("c5", "Trompowsky, Tabiya", "The standard break; Black plays for the centre while White has the bishop pair.")
                            }
                        }
                    }
                }
            }
            move("Nc3", "Jobava London Move Order", "The modern hybrid: the knight comes out first so Bf4 hits with tempo.") {
                move("d5", "Jobava London, Main Line", "The principled centre grab, walking straight into White's set-up.") {
                    move("Bf4", "Jobava London Attack", "The bishop lands with ideas of Nb5 and e4 — far sharper than the classic London.") {
                        move("e6", "Jobava London, Tabiya", "Solid, but it shuts in the bishop and White is already eyeing Nb5 and Ne5.") {
                            move("e3", "Jobava London, Main Tabiya", "Completes the wall; Bd3, Nf3 and a kingside attack follow.")
                        }
                    }
                }
            }
        }
    }
}
