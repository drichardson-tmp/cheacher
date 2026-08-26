package com.cheacher.app.engine

import kotlin.random.Random

/**
 * iOS plays PocketFish for now. Real Stockfish here means compiling it into the app
 * (no processes to spawn on iOS) — the [SparringEngine] seam is exactly the socket a
 * compiled engine plugs into later, and at learner ratings the two are near-identical
 * opponents anyway.
 */
actual fun createSparringEngine(random: Random): SparringEngine = PocketFish(random)
