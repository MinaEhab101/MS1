package com.example.ai

import com.example.game.AIDifficulty
import com.example.game.BackgammonEngine
import com.example.game.BoardState
import com.example.game.GamePhase
import com.example.game.Move
import com.example.game.Player
import kotlin.math.abs
import kotlin.random.Random

object BackgammonAI {

    /**
     * Finds the next move for the AI to make.
     * Returns null if no moves are possible.
     */
    fun getBestMove(state: BoardState, difficulty: AIDifficulty): Move? {
        val legalMoves = BackgammonEngine.getAllLegalMoves(state)
        if (legalMoves.isEmpty()) return null

        return when (difficulty) {
            AIDifficulty.EASY -> chooseEasyMove(legalMoves, state)
            AIDifficulty.MEDIUM -> chooseMediumMove(legalMoves, state)
            AIDifficulty.HARD, AIDifficulty.EXPERT -> chooseAdvancedMove(state, difficulty)
        }
    }

    private fun chooseEasyMove(moves: List<Move>, state: BoardState): Move {
        // Mostly random, but slightly prefers hits or moves from bar
        if (state.barBlack > 0) {
            val fromBar = moves.filter { it.fromPoint == Move.BAR_POINT }
            if (fromBar.isNotEmpty()) return fromBar.random()
        }
        val hits = moves.filter { it.isHit }
        if (hits.isNotEmpty() && Random.nextFloat() < 0.6f) {
            return hits.random()
        }
        return moves.random()
    }

    private fun chooseMediumMove(moves: List<Move>, state: BoardState): Move {
        // Greedily score each single legal move
        return moves.maxByOrNull { move ->
            val simulated = BackgammonEngine.applyMove(state, move)
            evaluateBoard(simulated, Player.BLACK, AIDifficulty.MEDIUM)
        } ?: moves.first()
    }

    private fun chooseAdvancedMove(state: BoardState, difficulty: AIDifficulty): Move? {
        val sequences = BackgammonEngine.getAllLegalMoveSequences(state)
        if (sequences.isEmpty()) {
            return BackgammonEngine.getAllLegalMoves(state).firstOrNull()
        }

        val bestSequence = sequences.maxByOrNull { sequence ->
            var testState = state
            for (m in sequence) {
                testState = BackgammonEngine.applyMove(testState, m)
            }
            evaluateBoard(testState, Player.BLACK, difficulty)
        }

        return bestSequence?.firstOrNull()
    }

    /**
     * Positional heuristic evaluator from the perspective of [aiPlayer].
     */
    fun evaluateBoard(state: BoardState, aiPlayer: Player, difficulty: AIDifficulty): Double {
        if (state.winner == aiPlayer) return 100000.0
        if (state.winner == aiPlayer.opponent()) return -100000.0

        val oppPlayer = aiPlayer.opponent()
        var score = 0.0

        // 1. Borne off checkers (huge incentive)
        val aiOff = if (aiPlayer == Player.BLACK) state.offBlack else state.offWhite
        val oppOff = if (aiPlayer == Player.BLACK) state.offWhite else state.offBlack
        score += (aiOff * 120.0) - (oppOff * 120.0)

        // 2. Bar checkers
        val aiBar = if (aiPlayer == Player.BLACK) state.barBlack else state.barWhite
        val oppBar = if (aiPlayer == Player.BLACK) state.barWhite else state.barBlack
        score -= (aiBar * 200.0)
        score += (oppBar * 220.0) // Putting opponent on bar is great!

        // 3. Pip Count advantage
        val aiPip = state.pipCount(aiPlayer)
        val oppPip = state.pipCount(oppPlayer)
        score += (oppPip - aiPip) * 1.5

        // 4. Anchors & Points made (having 2 or more checkers)
        var consecutivePoints = 0
        var maxPrime = 0

        for (p in 1..24) {
            val v = state.points[p]
            val isAiPoint = (aiPlayer == Player.BLACK && v <= -2) || (aiPlayer == Player.WHITE && v >= 2)
            val isAiBlot = (aiPlayer == Player.BLACK && v == -1) || (aiPlayer == Player.WHITE && v == 1)
            val isOppBlot = (aiPlayer == Player.BLACK && v == 1) || (aiPlayer == Player.WHITE && v == -1)

            if (isAiPoint) {
                // Key points (Golden point: 5/20, Bar point: 7/18)
                val isGoldenPoint = if (aiPlayer == Player.BLACK) (p == 20 || p == 18) else (p == 5 || p == 7)
                score += if (isGoldenPoint) 45.0 else 30.0

                consecutivePoints++
                if (consecutivePoints > maxPrime) maxPrime = consecutivePoints
            } else {
                consecutivePoints = 0
            }

            // Blots: danger of being hit
            if (isAiBlot) {
                val blotDanger = calculateBlotRisk(state, p, aiPlayer)
                score -= (blotDanger * if (difficulty == AIDifficulty.EXPERT) 55.0 else 35.0)
            }

            if (isOppBlot) {
                // Exposed opponent blot
                score += 20.0
            }
        }

        // Prime bonus
        if (maxPrime >= 3) {
            score += when (maxPrime) {
                3 -> 40.0
                4 -> 100.0
                5 -> 220.0
                else -> 400.0 // 6-prime is almost an automatic win!
            }
        }

        return score
    }

    private fun calculateBlotRisk(state: BoardState, point: Int, aiPlayer: Player): Double {
        // Check how close opponent checkers are to this blot
        var risk = 1.0
        val oppPlayer = aiPlayer.opponent()

        if (aiPlayer == Player.BLACK) {
            // Black moves towards 24. White moves towards 1 (from 24 down to 1).
            // White hits Black by moving from point > point down to point.
            if (state.barWhite > 0) {
                // White can enter from bar at 25 - die (points 24..19). If point is in 24..19, very high risk!
                if (point in 19..24) risk += 2.0
            }
            for (d in 1..6) {
                val oppPoint = point + d
                if (oppPoint <= 24 && state.points[oppPoint] > 0) {
                    risk += 1.5 // Direct shot range 1..6
                }
            }
        } else {
            // White moves towards 1. Black moves towards 24 (from 1 up to 24).
            // Black hits White by moving from point < point up to point.
            if (state.barBlack > 0) {
                if (point in 1..6) risk += 2.0
            }
            for (d in 1..6) {
                val oppPoint = point - d
                if (oppPoint >= 1 && state.points[oppPoint] < 0) {
                    risk += 1.5
                }
            }
        }

        return risk
    }
}
