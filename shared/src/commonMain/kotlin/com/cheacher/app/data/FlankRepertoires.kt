package com.cheacher.app.data

import com.cheacher.app.chess.Color
import com.cheacher.app.domain.Repertoire
import com.cheacher.app.domain.repertoire

/**
 * The flank openings: control the centre from the wings, and transpose into whatever
 * looks best once Black has committed.
 */

/** 1. c4 — the English, and the three shapes it usually takes. */
val englishOpening: Repertoire = repertoire(
    id = "english",
    title = "The English Opening",
    perspective = Color.WHITE,
    subtitle = "The Sicilian, a move up",
) {
    move("c4", "English Opening", "Fights for d5 from the wing and keeps every central pawn in reserve.") {
        move("e5", "Reversed Sicilian", "Black takes the centre — this is a Sicilian with colours reversed and a tempo more.") {
            move("Nc3", "English, King's Knight Variation", "Develops toward d5, the square the whole opening is about.") {
                move("Nf6", "Reversed Sicilian, Four Knights", "Natural development; Black fights for d5 with pieces too.") {
                    move("Nf3", "English, Four Knights Variation", "Attacks e5 and keeps the game flexible and symmetrical for now.") {
                        move("Nc6", "English, Four Knights Main Line", "Defends e5 and completes the mirror.") {
                            move("g3", "English, Fianchetto Variation", "The signature English bishop: it will bear down on d5 all game.") {
                                move("d5", "English, Reversed Dragon", "Black breaks first — the extra tempo makes this genuinely comfortable.") {
                                    move("cxd5", "Reversed Dragon, Exchange", "Takes, so that d5 becomes a square rather than a pawn.") {
                                        move("Nxd5", "Reversed Dragon, Recapture", "The knight sits on d5, and the fight is about whether it can stay.") {
                                            move("Bg2", "Reversed Dragon, Tabiya", "Completes the fianchetto; the bishop and the knight on c3 both hit d5.")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        move("c5", "Symmetrical English", "Mirrors White's plan and keeps every option — including transposing to a Sicilian.") {
            move("Nf3", "Symmetrical English, Main Line", "Develops and prepares d4, which turns the game into an Open Sicilian a tempo up.") {
                move("Nf6", "Symmetrical, Normal Position", "The mirror continues; someone has to break it eventually.") {
                    move("Nc3", "Symmetrical, Four Knights", "Develops the last knight before committing the d-pawn.") {
                        move("Nc6", "Symmetrical, Four Knights Main Line", "Full symmetry — and White is still the one with the extra move.") {
                            move("d4", "Symmetrical, Open Variation", "Breaks the symmetry with the standard central strike.") {
                                move("cxd4", "Symmetrical, Exchange", "Black must take or lose the centre outright.") {
                                    move("Nxd4", "Symmetrical, Main Line", "A Sicilian structure with colours reversed and White a full tempo ahead.") {
                                        move("e6", "Symmetrical, Tabiya", "Solid: cover d5 with a pawn and finish developing.")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        move("Nf6", "Anglo-Indian Defence", "Keeps the transposition doors open — this could still become almost any 1. d4 opening.") {
            move("Nc3", "Anglo-Indian, Main Line", "Develops and fights for the e4 and d5 squares at the same time.") {
                move("e6", "Anglo-Indian, Nimzo Set-up", "Prepares ...Bb4 or ...d5 and invites White to transpose to a Nimzo-Indian.") {
                    move("e4", "Mikenas-Carls Variation", "Refuses the transposition and takes the centre with a bang.") {
                        move("d5", "Mikenas-Carls, Main Line", "Hits the centre immediately, before White consolidates the two big pawns.") {
                            move("e5", "Mikenas-Carls, Advance", "Pushes past with tempo and gains a big space advantage.") {
                                move("d4", "Mikenas-Carls, Counter-Strike", "Kicks the knight and hits back in the centre — the critical try.") {
                                    move("exf6", "Mikenas-Carls, Exchange", "Takes the knight; the pawn on c3 is falling but Black's kingside is wrecked.") {
                                        move("dxc3", "Mikenas-Carls, Recapture", "Takes the knight back and creates a passed pawn deep in White's camp.") {
                                            move("bxc3", "Mikenas-Carls, Tabiya", "Doubled pawns for a shattered black kingside — sharp and roughly balanced.")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                move("g6", "Anglo-Indian, King's Indian Set-up", "Heads for a King's Indian structure without committing the d-pawn yet.") {
                    move("e4", "Botvinnik System", "Takes the full centre; White plays a King's Indian Attack a tempo up.") {
                        move("Bg7", "Botvinnik System, Main Line", "The fianchetto is completed and the long diagonal is Black's main asset.") {
                            move("d4", "Botvinnik System, Tabiya", "The classical centre; the game transposes into a King's Indian on White's terms.")
                        }
                    }
                }
            }
        }
        move("e6", "English, Agincourt Defence", "The most flexible reply: ...d5 is coming and the game may become a QGD.") {
            move("Nc3", "Agincourt, Main Line", "Develops and fights for d5 before Black gets to play it.") {
                move("d5", "Agincourt, Classical Centre", "The break; from here the game usually transposes into queen's-pawn territory.") {
                    move("d4", "Agincourt, Transposition", "Takes the centre and converts the English into a Queen's Gambit Declined.")
                }
            }
        }
    }
}

/** 1. Nf3 — the most flexible first move in chess. */
val retiOpening: Repertoire = repertoire(
    id = "reti",
    title = "The Réti and the King's Indian Attack",
    perspective = Color.WHITE,
    subtitle = "Develop first, decide later",
) {
    move("Nf3", "Zukertort Opening", "Controls e5 and commits nothing — every queen's-pawn and flank opening is still available.") {
        move("d5", "Zukertort, Symmetrical Variation", "Black takes the centre; White will now attack it from the wing.") {
            move("c4", "Réti Opening", "The gambit that gives the opening its name: undermine d5 without occupying the centre.") {
                move("e6", "Réti, Classical Defence", "Defends d5 with a pawn and heads for a solid, closed structure.") {
                    move("g3", "Réti, Fianchetto Variation", "The bishop belongs on the long diagonal, aimed straight through d5.") {
                        move("Nf6", "Réti, Main Line", "Natural development; Black's set-up is as flexible as White's.") {
                            move("Bg2", "Réti, Double Fianchetto Set-up", "Completes the fianchetto; b3 and Bb2 often follow.") {
                                move("Be7", "Réti, Tabiya", "Modest and sound, preparing to castle and then challenge with ...c5.") {
                                    move("O-O", "Réti, Main Tabiya", "Both sides are ready; the game is decided by who breaks in the centre first.")
                                }
                            }
                        }
                    }
                }
                move("d4", "Réti, Advance Variation", "Pushes past instead of holding the tension — space at the cost of a target.") {
                    move("e3", "Réti, Undermining Line", "Attacks the advanced pawn at its base rather than blockading it.") {
                        move("Nc6", "Réti, Advance Main Line", "Defends d4 with a developing move — the only way to keep the wedge.") {
                            move("exd4", "Réti, Exchange", "Resolves the tension while Black's queenside is still undeveloped.") {
                                move("Nxd4", "Réti, Recapture", "Takes with the knight and reaches a comfortable, open position.") {
                                    move("Nxd4", "Réti, Knight Trade", "Trades off the centralised knight before it becomes annoying.") {
                                        move("Qxd4", "Réti, Tabiya", "The queen recaptures in the centre; White will chase it away with Nc3.")
                                    }
                                }
                            }
                        }
                    }
                }
                move("dxc4", "Réti Accepted", "Takes the pawn, which White never intended to hold anyway.") {
                    move("e3", "Réti Accepted, Main Line", "Prepares Bxc4 and a healthy centre — the pawn comes back next move.") {
                        move("Nf6", "Réti Accepted, Development", "Develops and prepares ...e6 to hold the extra pawn one move longer.") {
                            move("Bxc4", "Réti Accepted, Tabiya", "The pawn returns; White has free development and a comfortable game.")
                        }
                    }
                }
            }
            move("g3", "King's Indian Attack", "A King's Indian set-up with White's extra tempo — a system, not a variation.") {
                move("Nf6", "King's Indian Attack, Main Line", "Black develops naturally; the KIA plays out the same way against almost anything.") {
                    move("Bg2", "King's Indian Attack, Fianchetto", "The bishop takes the long diagonal it will fight on all game.") {
                        move("e6", "King's Indian Attack, Classical Defence", "Solid, and exactly the structure the KIA's e4-e5 plan is designed against.") {
                            move("O-O", "King's Indian Attack, Castling", "King safe, and now d3 and e4 come in that order every time.") {
                                move("Be7", "King's Indian Attack, Tabiya", "Black finishes the mirror; the plans diverge sharply from here.") {
                                    move("d3", "King's Indian Attack, Main Tabiya", "The modest pawn that makes e4 possible — the whole system in one move.")
                                }
                            }
                        }
                    }
                }
            }
        }
        move("Nf6", "Zukertort, Indian Set-up", "Black mirrors and keeps every Indian defence available.") {
            move("g3", "Zukertort, Double Fianchetto", "Heads for a symmetrical fianchetto battle where small edges matter.") {
                move("g6", "Zukertort, Symmetrical Fianchetto", "The mirror holds; both bishops will glare down the same two diagonals.") {
                    move("Bg2", "Double Fianchetto, Development", "The bishop lands and eyes the centre from the corner.") {
                        move("Bg7", "Double Fianchetto, Main Line", "Symmetry maintained — the first player to break it usually gains something.") {
                            move("O-O", "Double Fianchetto, Castling", "King safe; c4 and d4 will follow and the game becomes a normal English.")
                        }
                    }
                }
            }
        }
        move("c5", "Zukertort, Sicilian Invitation", "Black plays a Sicilian and dares White to enter it — or to stay on the flank.") {
            move("c4", "Zukertort, Symmetrical English Transposition", "Declines the Open Sicilian and steers into a Symmetrical English instead.") {
                move("Nc6", "Symmetrical English, Transposed", "The game is now an English by transposition, a full tempo in White's favour.") {
                    move("d4", "Symmetrical English, Open Variation", "Breaks in the centre; the extra tempo is what makes this a reversed Sicilian.")
                }
            }
        }
    }
}
