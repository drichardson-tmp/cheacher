package com.cheacher.app.data

import com.cheacher.app.chess.Color
import com.cheacher.app.domain.Repertoire
import com.cheacher.app.domain.repertoire

/**
 * Every legal first move — all twenty of them — and the name each one carries.
 *
 * There is no other position in chess where the whole move list fits on one page, and
 * every branch of it has a name. Ordered best to worst, so the progression ladder walks
 * from the four moves that win world championships down to the ones that lose material
 * by move three. Two or three plies each: this is a vocabulary book, not a theory book.
 */
val twentyFirstMoves: Repertoire = repertoire(
    id = "first-moves",
    title = "Twenty First Moves",
    perspective = Color.WHITE,
    subtitle = "Every legal opening move, and its name",
) {
    move("e4", "King's Pawn Opening", "Stakes the centre and frees the bishop and queen in one stroke.") {
        move("c5", "Sicilian Defence", "The most popular reply in chess: fight for d4 without conceding symmetry.") {
            move("Nf3", "Open Sicilian, Preparation", "Develops and prepares d4, the break that defines the Open Sicilian.")
        }
    }
    move("d4", "Queen's Pawn Opening", "Takes the centre with the pawn the queen already defends — the safest big centre.") {
        move("Nf6", "Indian Defence", "Stops e4 with a piece rather than a pawn; the modern main road.") {
            move("c4", "Indian Game", "Fights for d5 and builds the standard queen's-pawn formation.")
        }
    }
    move("Nf3", "Zukertort Opening", "The most flexible move in chess: controls e5 and commits to absolutely nothing.") {
        move("d5", "Zukertort, Symmetrical Variation", "Black takes the centre, since White declined to.") {
            move("c4", "Réti Opening", "Undermines d5 from the wing instead of meeting it head-on.")
        }
    }
    move("c4", "English Opening", "Fights for d5 from the flank — a Sicilian with an extra tempo.") {
        move("e5", "Reversed Sicilian", "Black takes the centre and the colours are effectively swapped.") {
            move("Nc3", "English, King's Knight Variation", "Develops toward d5, the square this whole opening is about.")
        }
    }
    move("g3", "King's Fianchetto Opening", "Prepares the strongest bishop in chess before deciding anything else.") {
        move("d5", "Benko Opening, Main Line", "Black takes the centre while White builds on the wing.") {
            move("Bg2", "Benko Opening, Fianchetto", "The bishop lands on the long diagonal and eyes d5 from a distance.")
        }
    }
    move("b3", "Nimzo-Larsen Attack", "The queenside fianchetto: e5 and d4 will be attacked from the corner.") {
        move("e5", "Nimzo-Larsen, Classical Variation", "Occupies the square White's bishop is about to stare at.") {
            move("Bb2", "Nimzo-Larsen, Main Line", "The point of b3 — immediate pressure on e5 down the long diagonal.")
        }
    }
    move("f4", "Bird's Opening", "Grabs e5 with a pawn, accepting a slightly loose king for real central control.") {
        move("d5", "Bird's Opening, Dutch Variation", "A Dutch Defence with colours reversed; solid and testing.") {
            move("Nf3", "Bird's Opening, Main Line", "Stops ...e5 for good and prepares the classic Bird set-up with e3 and b3.")
        }
    }
    move("Nc3", "Dunst Opening", "Develops toward the centre but blocks the c-pawn — playable, and rarely seen.") {
        move("d5", "Dunst Opening, Main Line", "The most testing reply: take the centre the knight cannot easily attack.") {
            move("e4", "Dunst Opening, Gambit Line", "Offers a pawn to open lines, since a quiet game favours Black's better structure.")
        }
    }
    move("b4", "Sokolsky Opening", "The Polish: a wing pawn thrown forward to fianchetto with tempo on e5.") {
        move("e5", "Sokolsky, Main Line", "Takes the centre and dares the b-pawn to keep advancing.") {
            move("Bb2", "Sokolsky, Fianchetto", "Hits e5 immediately; the point is that ...Bxb4 is met by Bxe5.")
        }
    }
    move("d3", "Mieses Opening", "A quiet, non-committal move that usually transposes into a King's Indian Attack.") {
        move("e5", "Mieses Opening, Main Line", "Black takes the full centre, which White has politely declined.") {
            move("Nf3", "Mieses Opening, Development", "Attacks e5 and steers toward the King's Indian Attack formation.")
        }
    }
    move("e3", "Van 't Kruijs Opening", "Modest and slow: it frees the bishop and transposes into almost anything.") {
        move("d5", "Van 't Kruijs, Main Line", "Occupies the centre unopposed, which is why this move order is rare.") {
            move("d4", "Van 't Kruijs, Transposition", "Takes the centre after all, reaching a normal queen's-pawn game a tempo down.")
        }
    }
    move("c3", "Saragossa Opening", "Prepares d4 the slow way — sound, but a tempo behind the direct move.") {
        move("d5", "Saragossa, Main Line", "The natural centre grab; Black is already comfortable.") {
            move("d4", "Saragossa, Transposition", "Builds the intended centre and transposes to a Queen's Pawn Game.")
        }
    }
    move("a3", "Anderssen's Opening", "A waiting move: White plays a reversed defence and lets Black commit first.") {
        move("d5", "Anderssen's Opening, Main Line", "Black takes the centre, which is exactly what this move invites.") {
            move("d4", "Anderssen's Opening, Transposition", "Now the centre; a3 will prove useful in some queenside lines and wasted in others.")
        }
    }
    move("g4", "Grob's Attack", "The most aggressive bad move in chess: it weakens everything and threatens nothing.") {
        move("d5", "Grob's Attack, Main Line", "Occupies the centre and hits the g4 pawn's only justification.") {
            move("Bg2", "Grob's Attack, Fianchetto", "The idea behind g4: the bishop hits d5 and the g-pawn supports an early h3 and Bxg4 trap.")
        }
    }
    move("h3", "Clemenz Opening", "Takes g4 from Black's pieces and does nothing else at all.") {
        move("d5", "Clemenz Opening, Main Line", "Black takes the centre for free and is already better placed.") {
            move("d4", "Clemenz Opening, Transposition", "Returns to normal chess; h3 is occasionally useful, rarely worth a tempo.")
        }
    }
    move("a4", "Ware Opening", "A rook's pawn on move one: it grabs b5 and hands Black the centre.") {
        move("e5", "Ware Opening, Main Line", "Takes the centre unopposed — the standard refutation is simply good play.") {
            move("h4", "Ware Opening, Crab Variation", "Both rook's pawns forward: the Crab, which is exactly as bad as it sounds.")
        }
    }
    move("h4", "Desprez Opening", "The kingside rook's pawn — it opens a file that only helps Black.") {
        move("d5", "Desprez Opening, Main Line", "Black takes the centre and can already claim an advantage.") {
            move("d4", "Desprez Opening, Transposition", "Back to normal chess a tempo down, with a permanently loose kingside.")
        }
    }
    move("f3", "Barnes Opening", "The worst first move on the board: it blocks the knight and opens the king's diagonal.") {
        move("e5", "Barnes Opening, Main Line", "Takes the centre and prepares ...Qh4+ ideas the very next move.") {
            move("Kf2", "Barnes Defence", "The infamous king move — an opening curiosity, not a recommendation.")
        }
    }
    move("Na3", "Durkin Opening", "The Sodium Attack: a knight to the rim, where it has fewer squares than at home.") {
        move("d5", "Durkin Opening, Main Line", "Black takes the centre while the knight sits on the edge of the board.") {
            move("c4", "Durkin Opening, Gambit Line", "Hits d5 and finally gives the knight something to do: b5 and c2 both open up.")
        }
    }
    move("Nh3", "Amar Opening", "Also called the Ammonia Attack — the knight heads for the rim on purpose.") {
        move("d5", "Amar Opening, Main Line", "Black takes the centre unchallenged.") {
            move("g3", "Amar Gambit Set-up", "The fianchetto justifies the knight a little: Bg2 and f4 give it a job on f2.")
        }
    }
}
