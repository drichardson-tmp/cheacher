package com.cheacher.app.data

import com.cheacher.app.chess.Color
import com.cheacher.app.domain.Repertoire
import com.cheacher.app.domain.repertoire

/**
 * Everything Black plays against 1. e4 except 1...e5 and 1...c5 — the semi-open games.
 * One book each for the two big pawn-chain defences, and one catch-all for the rest.
 */

/** 1. e4 e6: the pawn chain, and White's four ways to treat it. */
val frenchDefence: Repertoire = repertoire(
    id = "french",
    title = "The French Defence",
    perspective = Color.WHITE,
    subtitle = "Four verdicts on one pawn chain",
) {
    move("e4", "King's Pawn Opening", "Stakes the centre and frees the bishop and queen in one stroke.") {
        move("e6", "French Defence", "A modest pawn move that prepares ...d5 and a counterpunch at White's centre.") {
            move("d4", "French Defence, Normal Variation", "Takes the full centre while it is on offer; Black will strike back with ...d5.") {
                move("d5", "French Defence, Main Line", "The counterpunch. White must now decide the whole character of the game.") {
                    move("Nc3", "French, Paulsen Variation", "Defends e4 with a piece and keeps every pawn option alive.") {
                        move("Bb4", "Winawer Variation", "Pins the defender of e4 and prepares to trade the bishop for structure.") {
                            move("e5", "Winawer, Advance", "Gains space and shuts out the f6 knight before it ever gets there.") {
                                move("c5", "Winawer, Main Line", "Attacks the base of the chain — the one break that matters here.") {
                                    move("a3", "Winawer, Main Tabiya", "Forces the bishop to decide: take on c3 or retreat and lose the argument.") {
                                        move("Bxc3+", "Winawer, Bishop Trade", "Takes, wrecking White's queenside pawns in exchange for the bishop pair.") {
                                            move("bxc3", "Winawer, Poisoned Structure", "Doubled pawns, but a huge centre and an open b-file — the classic imbalance.") {
                                                move("Ne7", "Winawer, Tabiya", "Routes the knight to f5 or g6 since f6 is permanently unavailable.")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        move("Nf6", "Classical French", "Develops and adds a second attacker to e4 without committing the bishop.") {
                            move("Bg5", "Classical, Main Line", "Pins the knight so that e5 comes with real force next move.") {
                                move("Be7", "Classical, Steinitz Set-up", "Breaks the pin the solid way and prepares to castle.") {
                                    move("e5", "Classical, Advance", "Now the push works: the pinned knight has to move and the bishop trade follows.") {
                                        move("Nfd7", "Classical, Knight Retreat", "Retreats to the only square that keeps the knight useful behind the chain.") {
                                            move("Bxe7", "Classical, Exchange", "Trades off Black's good defensive bishop before it finds a better job.") {
                                                move("Qxe7", "Classical, Recapture", "The queen takes and eyes the dark squares White just gave up.") {
                                                    move("f4", "Classical, Tabiya", "Buttresses e5 and starts the kingside space grab that defines the line.")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        move("dxe4", "Rubinstein Variation", "Releases the tension early for a solid, slightly passive game.") {
                            move("Nxe4", "Rubinstein, Main Line", "Recaptures with the piece and enjoys a free centre and easy development.") {
                                move("Nd7", "Rubinstein, Tabiya", "Prepares ...Ngf6 so the knight can be challenged without doubling pawns.") {
                                    move("Nf3", "Rubinstein, Development", "Simple and strong: finish developing and let the space edge do the work.")
                                }
                            }
                        }
                    }
                    move("e5", "French, Advance Variation", "Locks the centre at once and takes the whole kingside as territory.") {
                        move("c5", "Advance, Main Line", "Hits the base of the chain immediately — Black's standard plan of attack.") {
                            move("c3", "Advance, Chain Defence", "Props up d4 so the chain holds and White can develop behind it.") {
                                move("Nc6", "Advance, Development", "Adds a third attacker to d4 before doing anything else.") {
                                    move("Nf3", "Advance, Knight Development", "Defends d4 a second time and prepares to castle out of the fight.") {
                                        move("Qb6", "Advance, Queen Sortie", "The queen joins the assault on d4 and pins White's b-pawn to b2.") {
                                            move("a3", "Advance, Tabiya", "A patient move that prepares b4 and takes the sting out of ...Nb4.")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    move("Nd2", "Tarrasch Variation", "Defends e4 without allowing ...Bb4 — flexible, and structurally safe.") {
                        move("c5", "Tarrasch, Open Variation", "Strikes at d4 immediately, betting that the isolated pawn is worth the activity.") {
                            move("exd5", "Tarrasch, Exchange", "Opens the position while Black's king is still in the centre.") {
                                move("Qxd5", "Tarrasch, Queen Recapture", "The queen grabs a strong central square and dares White to chase it.") {
                                    move("Ngf3", "Tarrasch, Development", "Develops the other knight, defending d4 and preparing Bc4 with tempo.") {
                                        move("cxd4", "Tarrasch, Central Exchange", "Resolves the centre while the White queen is still stuck on d1.") {
                                            move("Bc4", "Tarrasch, Tabiya", "Hits the queen and wins the time White needs to recapture on d4.")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    move("exd5", "French, Exchange Variation", "Symmetry: dull-looking, and the fastest way to a level, open position.") {
                        move("exd5", "Exchange, Main Line", "The only recapture that keeps a healthy pawn structure.") {
                            move("Nf3", "Exchange, Development", "Nothing fancy — develop, castle, and play for the open e-file.") {
                                move("Nf6", "Exchange, Symmetrical", "Black mirrors; whoever finds the first asymmetric idea takes the initiative.") {
                                    move("Bd3", "Exchange, Tabiya", "Takes the best diagonal before Black can claim it with ...Bd6.")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 1. e4 c6: the solid defence, met three different ways. */
val caroKann: Repertoire = repertoire(
    id = "caro-kann",
    title = "The Caro-Kann",
    perspective = Color.WHITE,
    subtitle = "Solid as granite — three ways to chip it",
) {
    move("e4", "King's Pawn Opening", "Stakes the centre and frees the bishop and queen in one stroke.") {
        move("c6", "Caro-Kann Defence", "Prepares ...d5 with a pawn, so the light-squared bishop keeps its freedom.") {
            move("d4", "Caro-Kann, Main Line", "Takes the full centre and invites the challenge Black has prepared.") {
                move("d5", "Caro-Kann, Classical Centre", "The point of 1...c6: the centre is contested with no bad bishop left behind.") {
                    move("Nc3", "Caro-Kann, Classical Variation", "Defends e4 and offers the trade Black is nearly obliged to accept.") {
                        move("dxe4", "Classical, Exchange", "Takes, because leaving the tension lets White play e5 with a free hand.") {
                            move("Nxe4", "Classical, Recapture", "A centralised knight, an open position, and a lead in development.") {
                                move("Bf5", "Classical Caro-Kann", "The move that justifies the whole opening: the bishop gets out first.") {
                                    move("Ng3", "Classical, Knight Kick", "Attacks the bishop and gains time for the coming h-pawn advance.") {
                                        move("Bg6", "Classical, Bishop Retreat", "The only square that keeps the bishop on its good diagonal.") {
                                            move("h4", "Classical, Main Line", "Threatens h5 to trap or trade the bishop and gains kingside space for free.") {
                                                move("h6", "Classical, Tabiya", "Makes luft for the bishop on h7 before White's pawn arrives.") {
                                                    move("Nf3", "Classical, Main Tabiya", "Development resumes; White will castle long and play on the kingside.")
                                                }
                                            }
                                        }
                                    }
                                }
                                move("Nd7", "Karpov Variation", "Prepares ...Ngf6 so the recapture never doubles Black's pawns.") {
                                    move("Nf3", "Karpov, Main Line", "Develops and eyes e5 and g5 — Black's set-up is solid but passive.") {
                                        move("Ngf6", "Karpov, Knight Challenge", "Finally questions the strong knight, now that a recapture is available.") {
                                            move("Nxf6+", "Karpov, Exchange", "Trades on Black's terms — but the knight on f6 is the one Black wanted.") {
                                                move("Nxf6", "Karpov, Tabiya", "Recaptures with the knight and keeps the structure flawless.")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    move("e5", "Caro-Kann, Advance Variation", "Shuts the centre and takes space — the most popular try today.") {
                        move("Bf5", "Advance, Main Line", "The bishop escapes before ...e6 buries it; this is why Black plays 1...c6.") {
                            move("Nf3", "Advance, Short System", "Quiet and dangerous: develop, castle, and squeeze rather than lunge.") {
                                move("e6", "Advance, Classical Set-up", "Locks the chain and prepares ...c5 to hit its base.") {
                                    move("Be2", "Advance, Short Variation", "A modest square that keeps the knight's route to f4 or h4 open.") {
                                        move("c5", "Advance, Tabiya", "The standard break; the game turns on whether d4 holds.")
                                    }
                                }
                            }
                        }
                    }
                    move("exd5", "Caro-Kann, Exchange", "Clarifies the centre and heads for an isolated-pawn fight on White's terms.") {
                        move("cxd5", "Exchange, Recapture", "The only sensible recapture — but the c-file is now open for White.") {
                            move("c4", "Panov-Botvinnik Attack", "Hits d5 at once and converts a quiet defence into a sharp IQP battle.") {
                                move("Nf6", "Panov, Main Line", "Develops and defends d5 with the piece that belongs there.") {
                                    move("Nc3", "Panov, Development", "Piles onto d5 and keeps Black's structure under permanent question.") {
                                        move("e6", "Panov, Tabiya", "Solid: hold d5 with a pawn and accept a slightly passive but sound game.") {
                                            move("Nf3", "Panov, Main Tabiya", "Completes the standard IQP set-up; White plays for pieces, Black for the endgame.")
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
}

/** Scandinavian, Alekhine, Pirc, Modern, Nimzowitsch — the rest of the 1. e4 world. */
val kingsPawnSidelines: Repertoire = repertoire(
    id = "e4-sidelines",
    title = "Answering the Rest of 1. e4",
    perspective = Color.WHITE,
    subtitle = "Scandinavian, Alekhine, Pirc, Modern",
) {
    move("e4", "King's Pawn Opening", "Stakes the centre and frees the bishop and queen in one stroke.") {
        move("d5", "Scandinavian Defence", "Challenges e4 on move one and refuses to let White build a big centre.") {
            move("exd5", "Scandinavian, Accepted", "Takes the pawn; there is nothing better and nothing to fear.") {
                move("Qxd5", "Scandinavian, Main Line", "Recaptures with the queen, accepting that it will be chased for a tempo.") {
                    move("Nc3", "Scandinavian, Queen Chase", "Develops with tempo — the free move that pays for White's opening.") {
                        move("Qa5", "Scandinavian, Mieses-Kotroc Variation", "The classical retreat: the queen stays active and eyes the c3 knight.") {
                            move("d4", "Scandinavian, Centre Build", "Occupies the centre now that the queen has been pushed to the edge.") {
                                move("Nf6", "Scandinavian, Development", "Develops toward the centre and prepares ...Bf5 or ...Bg4.") {
                                    move("Nf3", "Scandinavian, Tabiya", "Finishes the natural set-up; White is simply a tempo ahead of a normal game.")
                                }
                            }
                        }
                    }
                }
                move("Nf6", "Modern Scandinavian", "Declines the queen recapture and plays a gambit for time instead.") {
                    move("d4", "Modern Scandinavian, Centre", "Holds the pawn one more move and grabs the centre while Black chases.") {
                        move("Nxd5", "Modern Scandinavian, Recapture", "Regains the pawn with the knight rather than the queen.") {
                            move("c4", "Modern Scandinavian, Kick", "Chases the knight and claims a broad pawn centre in the process.") {
                                move("Nb6", "Modern Scandinavian, Tabiya", "Retreats to the rim but keeps an eye on c4 and d5.") {
                                    move("Nf3", "Modern Scandinavian, Main Line", "Develops; White has space, Black has piece play against the big centre.")
                                }
                            }
                        }
                    }
                }
            }
        }
        move("Nf6", "Alekhine's Defence", "Invites White to chase the knight, betting the pawn centre becomes a target.") {
            move("e5", "Alekhine, Main Line", "Accepts the invitation — declining it concedes the whole argument.") {
                move("Nd5", "Alekhine, Knight Hop", "The knight settles on the one square where it is not immediately hit again.") {
                    move("d4", "Alekhine, Centre Build", "Builds the big centre Black is provoking, and dares Black to undermine it.") {
                        move("d6", "Alekhine, Main Variation", "Strikes at e5 before the centre gets any bigger.") {
                            move("Nf3", "Alekhine, Modern Variation", "Develops calmly rather than overextending with the c- and f-pawns.") {
                                move("Bg4", "Alekhine, Tabiya", "Pins the defender of d4 — Black's standard way of attacking the centre.") {
                                    move("Be2", "Alekhine, Main Tabiya", "Breaks the pin and completes development; the centre holds.")
                                }
                            }
                        }
                    }
                }
            }
        }
        move("d6", "Pirc Defence", "Concedes the centre on purpose, planning to hit it later from a fianchetto.") {
            move("d4", "Pirc, Main Line", "Takes what is offered; the full centre is the whole point.") {
                move("Nf6", "Pirc, Knight Development", "Attacks e4 and forces White to commit a defender.") {
                    move("Nc3", "Pirc, Classical Set-up", "The natural defender, and the knight belongs on c3 in every 1. e4 line.") {
                        move("g6", "Pirc, Main Variation", "The fianchetto: the bishop will pressure d4 from a distance all game.") {
                            move("f4", "Austrian Attack", "The critical try: a third centre pawn and a kingside pawn storm to come.") {
                                move("Bg7", "Austrian Attack, Main Line", "Completes the fianchetto and waits for White to overextend.") {
                                    move("Nf3", "Austrian Attack, Tabiya", "Develops behind the pawn wall; White will castle and push e5.")
                                }
                            }
                        }
                    }
                }
            }
        }
        move("g6", "Modern Defence", "The Pirc without ...Nf6 — maximum flexibility, minimum commitment.") {
            move("d4", "Modern, Main Line", "Occupies the centre unopposed while Black finishes the fianchetto.") {
                move("Bg7", "Modern, Fianchetto", "The bishop takes the long diagonal, which is Black's entire opening plan.") {
                    move("Nc3", "Modern, Classical Set-up", "Defends e4 and keeps Be3 and Qd2 available for a queenside castle.") {
                        move("d6", "Modern, Tabiya", "Stops e5 and finally gives the bishop on g7 something to look at.") {
                            move("Be3", "Modern, Main Tabiya", "Shores up d4 and starts the 150 Attack: Qd2, Bh6 and h4 next.")
                        }
                    }
                }
            }
        }
        move("Nc6", "Nimzowitsch Defence", "A provocative knight move that invites White to overextend in the centre.") {
            move("d4", "Nimzowitsch Defence, Main Line", "Grabs the centre and dares Black to prove the knight belongs on c6.") {
                move("d5", "Nimzowitsch, Scandinavian Variation", "Hits e4 immediately, before White can consolidate the two centre pawns.") {
                    move("Nc3", "Nimzowitsch, Tabiya", "Defends e4 with a piece and keeps the tension where Black must resolve it.")
                }
            }
        }
    }
}
