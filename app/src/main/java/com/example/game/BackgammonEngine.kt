package com.example.game

import kotlin.math.abs
import kotlin.random.Random

/**
 * Result of an opening roll to decide who plays first.
 */
data class OpeningRollResult(
    val whiteDie: Int,
    val blackDie: Int,
    val isTied: Boolean,
    val startingPlayer: Player?,
    val nextState: BoardState
)

/**
 * Core game engine for Backgammon implementing standard international tournament rules:
 * - Complete board representation (24 points, Bar, Off)
 * - Piece movement direction (White 24 -> 1, Black 1 -> 24)
 * - Bar entry enforcement before any board moves
 * - Blocked points (2+ opponent checkers) and blot hitting (1 opponent checker)
 * - Bearing off criteria (all active checkers in home board) and over-die bear off rules
 * - Maximum dice enforcement (must play both dice if possible, or all 4 of a double)
 * - Larger die enforcement (if either die can be played, but not both, must play the larger die)
 * - Victory detection with Single, Gammon, and Backgammon scoring
 * - Doubling cube support (offer, accept, decline)
 * - Turn-level move undo support
 *
 * This module is completely decoupled from Android and UI layers.
 */
object BackgammonEngine {

    /**
     * Creates standard Backgammon initial position:
     * - White: 2 on 24, 5 on 13, 3 on 8, 5 on 6 (moves towards 1)
     * - Black: 2 on 1, 5 on 12, 3 on 17, 5 on 19 (moves towards 24)
     */
    fun createInitialBoard(firstPlayer: Player = Player.WHITE): BoardState {
        val points = IntArray(25)

        // White pieces (positive numbers)
        points[24] = 2
        points[13] = 5
        points[8] = 3
        points[6] = 5

        // Black pieces (negative numbers)
        points[1] = -2
        points[12] = -5
        points[17] = -3
        points[19] = -5

        return BoardState(
            points = points,
            barWhite = 0,
            barBlack = 0,
            offWhite = 0,
            offBlack = 0,
            turn = firstPlayer,
            dice = emptyList(),
            initialDice = emptyList(),
            lastRoll = null,
            phase = GamePhase.ROLL_DICE,
            winner = null,
            winType = null,
            cubeValue = 1,
            cubeOwner = null,
            doubler = null,
            selectedPoint = null,
            legalMovesForSelected = emptyList(),
            moveHistory = emptyList(),
            turnMoves = emptyList(),
            turnStartState = null
        )
    }

    /**
     * Executes standard opening roll: each player rolls 1 die.
     * Higher die starts the game using both rolled numbers.
     * Tied roll requires a re-roll.
     */
    fun openingRoll(
        state: BoardState,
        forcedWhite: Int? = null,
        forcedBlack: Int? = null
    ): OpeningRollResult {
        val w = forcedWhite ?: (Random.nextInt(6) + 1)
        val b = forcedBlack ?: (Random.nextInt(6) + 1)

        if (w == b) {
            return OpeningRollResult(
                whiteDie = w,
                blackDie = b,
                isTied = true,
                startingPlayer = null,
                nextState = state.copy(lastRoll = Pair(w, b))
            )
        }

        val starter = if (w > b) Player.WHITE else Player.BLACK
        val diceList = listOf(w, b)

        val stateAfterRoll = state.copy(
            turn = starter,
            dice = diceList,
            initialDice = diceList,
            lastRoll = Pair(w, b),
            phase = GamePhase.MOVE_CHECKERS,
            selectedPoint = null,
            legalMovesForSelected = emptyList(),
            turnMoves = emptyList(),
            turnStartState = null
        )

        val readyState = stateAfterRoll.copy(turnStartState = stateAfterRoll)

        val legalMoves = getAllLegalMoves(readyState)
        val finalState = if (legalMoves.isEmpty()) {
            // In the rare event no moves are possible, turn passes
            readyState.copy(
                turn = starter.opponent(),
                dice = emptyList(),
                initialDice = emptyList(),
                phase = GamePhase.ROLL_DICE,
                turnStartState = null
            )
        } else {
            readyState
        }

        return OpeningRollResult(
            whiteDie = w,
            blackDie = b,
            isTied = false,
            startingPlayer = starter,
            nextState = finalState
        )
    }

    /**
     * Rolls two dice for the active player.
     * Doubles produce 4 usable moves of that die value.
     * Enforces automatic turn transfer if no legal moves exist (dead roll).
     */
    fun rollDice(state: BoardState, forcedD1: Int? = null, forcedD2: Int? = null): BoardState {
        if (state.phase != GamePhase.ROLL_DICE || state.winner != null) return state

        val d1 = forcedD1 ?: (Random.nextInt(6) + 1)
        val d2 = forcedD2 ?: (Random.nextInt(6) + 1)

        val diceList = if (d1 == d2) {
            listOf(d1, d1, d1, d1)
        } else {
            listOf(d1, d2)
        }

        val stateWithDice = state.copy(
            dice = diceList,
            initialDice = diceList,
            lastRoll = Pair(d1, d2),
            phase = GamePhase.MOVE_CHECKERS,
            selectedPoint = null,
            legalMovesForSelected = emptyList(),
            turnMoves = emptyList(),
            turnStartState = null
        )

        val stateWithTurnStart = stateWithDice.copy(turnStartState = stateWithDice)

        // Check if any legal moves exist under standard Backgammon rules
        val allLegal = getAllLegalMoves(stateWithTurnStart)
        return if (allLegal.isEmpty()) {
            // Dead roll: no moves possible, pass turn immediately
            stateWithTurnStart.copy(
                turn = stateWithTurnStart.turn.opponent(),
                dice = emptyList(),
                initialDice = emptyList(),
                phase = GamePhase.ROLL_DICE,
                turnStartState = null
            )
        } else {
            stateWithTurnStart
        }
    }

    /**
     * Checks if all active checkers of [player] are in their home board and bar is clear.
     */
    fun canBearOff(state: BoardState, player: Player): Boolean = state.canBearOff(player)

    /**
     * Generates all raw physically possible single moves using [die] from [state].
     * Enforces bar entry precedence and bearing off over-die rules.
     */
    fun getRawLegalMoves(state: BoardState, die: Int): List<Move> {
        val player = state.turn
        val moves = mutableListOf<Move>()

        if (player == Player.WHITE) {
            // White checker on the bar: MUST enter before any board moves
            if (state.barWhite > 0) {
                val target = 25 - die // Points 24 down to 19
                if (target in 1..24 && state.isPointOpen(target, Player.WHITE)) {
                    moves.add(
                        Move(
                            fromPoint = Move.BAR_POINT,
                            toPoint = target,
                            dieUsed = die,
                            isHit = state.isBlot(target, Player.WHITE)
                        )
                    )
                }
                return moves
            }

            val canBearOffWhite = state.canBearOff(Player.WHITE)

            for (p in 1..24) {
                if (state.points[p] <= 0) continue

                val target = p - die
                if (target >= 1) {
                    // Regular board move
                    if (state.isPointOpen(target, Player.WHITE)) {
                        moves.add(
                            Move(
                                fromPoint = p,
                                toPoint = target,
                                dieUsed = die,
                                isHit = state.isBlot(target, Player.WHITE)
                            )
                        )
                    }
                } else if (canBearOffWhite) {
                    // Bearing off
                    if (p == die) {
                        // Exact roll
                        moves.add(Move(fromPoint = p, toPoint = Move.OFF_POINT, dieUsed = die, isHit = false))
                    } else if (p < die) {
                        // Over-die: allowed ONLY if no checkers on points higher than p in home board
                        val higherCheckersExist = (p + 1..6).any { state.points[it] > 0 }
                        if (!higherCheckersExist) {
                            moves.add(Move(fromPoint = p, toPoint = Move.OFF_POINT, dieUsed = die, isHit = false))
                        }
                    }
                }
            }
        } else {
            // Player.BLACK
            // Black checker on the bar: MUST enter before any board moves
            if (state.barBlack > 0) {
                val target = die // Points 1 up to 6
                if (target in 1..24 && state.isPointOpen(target, Player.BLACK)) {
                    moves.add(
                        Move(
                            fromPoint = Move.BAR_POINT,
                            toPoint = target,
                            dieUsed = die,
                            isHit = state.isBlot(target, Player.BLACK)
                        )
                    )
                }
                return moves
            }

            val canBearOffBlack = state.canBearOff(Player.BLACK)

            for (p in 1..24) {
                if (state.points[p] >= 0) continue

                val target = p + die
                if (target <= 24) {
                    // Regular board move
                    if (state.isPointOpen(target, Player.BLACK)) {
                        moves.add(
                            Move(
                                fromPoint = p,
                                toPoint = target,
                                dieUsed = die,
                                isHit = state.isBlot(target, Player.BLACK)
                            )
                        )
                    }
                } else if (canBearOffBlack) {
                    // Bearing off past 24
                    val dist = 25 - p
                    if (dist == die) {
                        // Exact roll
                        moves.add(Move(fromPoint = p, toPoint = Move.OFF_POINT, dieUsed = die, isHit = false))
                    } else if (dist < die) {
                        // Over-die: allowed ONLY if no checkers on points further from 25 than p
                        // (points 19 up to p - 1)
                        val furtherCheckersExist = (19 until p).any { state.points[it] < 0 }
                        if (!furtherCheckersExist) {
                            moves.add(Move(fromPoint = p, toPoint = Move.OFF_POINT, dieUsed = die, isHit = false))
                        }
                    }
                }
            }
        }

        return moves
    }

    /**
     * Generates all complete legal move sequences for the current turn, enforcing:
     * 1. Maximum dice usage: players must use as many dice as legally possible.
     * 2. Larger die rule: if only one die out of two unequal dice can be played,
     *    the player MUST play the larger die.
     */
    fun getAllLegalMoveSequences(state: BoardState): List<List<Move>> {
        if (state.phase != GamePhase.MOVE_CHECKERS || state.dice.isEmpty() || state.winner != null) {
            return emptyList()
        }

        val allSequences = mutableListOf<List<Move>>()

        fun search(currentState: BoardState, currentPath: List<Move>) {
            val remainingDice = currentState.dice
            if (remainingDice.isEmpty() || currentState.winner != null) {
                if (currentPath.isNotEmpty()) allSequences.add(currentPath)
                return
            }

            val uniqueDice = remainingDice.distinct()
            var branchFound = false

            for (die in uniqueDice) {
                val rawMoves = getRawLegalMoves(currentState, die)
                for (m in rawMoves) {
                    branchFound = true
                    val nextState = applySingleMoveInternal(currentState, m)
                    search(nextState, currentPath + m)
                }
            }

            if (!branchFound && currentPath.isNotEmpty()) {
                allSequences.add(currentPath)
            }
        }

        search(state, emptyList())

        if (allSequences.isEmpty()) return emptyList()

        // 1. Enforce Maximum Dice Usage: only keep sequences with the maximum number of moves
        val maxLength = allSequences.maxOf { it.size }
        var maximalSequences = allSequences.filter { it.size == maxLength }

        // 2. Enforce Larger Die Rule:
        // If only 1 die can be played and the turn started with 2 unequal dice,
        // the player MUST play the higher number if possible.
        if (maxLength == 1 && state.initialDice.size == 2 && state.initialDice[0] != state.initialDice[1]) {
            val maxDie = maxOf(state.initialDice[0], state.initialDice[1])
            val sequencesWithMaxDie = maximalSequences.filter { it[0].dieUsed == maxDie }
            if (sequencesWithMaxDie.isNotEmpty()) {
                maximalSequences = sequencesWithMaxDie
            }
        }

        return maximalSequences
    }

    /**
     * Returns all legal single moves that can be played right now from [state].
     * Strictly enforces standard Backgammon rules by only permitting moves that belong
     * to a valid maximal turn sequence.
     */
    fun getAllLegalMoves(state: BoardState): List<Move> {
        val sequences = getAllLegalMoveSequences(state)
        return sequences.map { it.first() }.distinct()
    }

    /**
     * Alias for [getAllLegalMoves].
     */
    fun getLegalMoves(state: BoardState): List<Move> = getAllLegalMoves(state)

    /**
     * Returns legal moves originating from [fromPoint].
     * Use [Move.BAR_POINT] (0) to get legal moves entering from the bar.
     */
    fun getLegalMovesForPoint(state: BoardState, fromPoint: Int): List<Move> {
        return getAllLegalMoves(state).filter { it.fromPoint == fromPoint }
    }

    /**
     * Validates if a proposed move is legally playable in [state].
     */
    fun isValidMove(state: BoardState, move: Move): Boolean {
        return getAllLegalMoves(state).contains(move)
    }

    /**
     * Applies a single move to [state], consuming the die and checking for victory.
     * Transitions turn if all dice are consumed or if remaining dice cannot be played.
     */
    fun applyMove(state: BoardState, move: Move): BoardState {
        // Save turn start state if this is the first move of the turn
        val baseState = if (state.turnStartState == null) {
            state.copy(turnStartState = state.deepCopy())
        } else {
            state
        }

        val updatedState = applySingleMoveInternal(baseState, move)
        val newTurnMoves = baseState.turnMoves + move
        val newHistory = baseState.moveHistory + move

        // Check for victory
        if (updatedState.offWhite >= 15) {
            val winType = determineWinType(Player.WHITE, updatedState.offBlack, updatedState.barBlack, updatedState.points)
            return updatedState.copy(
                winner = Player.WHITE,
                winType = winType,
                phase = GamePhase.GAME_OVER,
                dice = emptyList(),
                selectedPoint = null,
                legalMovesForSelected = emptyList(),
                moveHistory = newHistory,
                turnMoves = newTurnMoves,
                turnStartState = null
            )
        }

        if (updatedState.offBlack >= 15) {
            val winType = determineWinType(Player.BLACK, updatedState.offWhite, updatedState.barWhite, updatedState.points)
            return updatedState.copy(
                winner = Player.BLACK,
                winType = winType,
                phase = GamePhase.GAME_OVER,
                dice = emptyList(),
                selectedPoint = null,
                legalMovesForSelected = emptyList(),
                moveHistory = newHistory,
                turnMoves = newTurnMoves,
                turnStartState = null
            )
        }

        val stateAfterMove = updatedState.copy(
            selectedPoint = null,
            legalMovesForSelected = emptyList(),
            moveHistory = newHistory,
            turnMoves = newTurnMoves
        )

        // Turn ends if all dice are used
        if (stateAfterMove.dice.isEmpty()) {
            return stateAfterMove.copy(
                turn = stateAfterMove.turn.opponent(),
                phase = GamePhase.ROLL_DICE,
                initialDice = emptyList(),
                turnMoves = emptyList(),
                turnStartState = null
            )
        }

        // Check if any legal moves remain with unconsumed dice
        val remainingLegal = getAllLegalMoves(stateAfterMove)
        return if (remainingLegal.isEmpty()) {
            // Cannot play remaining dice: turn passes to opponent
            stateAfterMove.copy(
                turn = stateAfterMove.turn.opponent(),
                phase = GamePhase.ROLL_DICE,
                dice = emptyList(),
                initialDice = emptyList(),
                turnMoves = emptyList(),
                turnStartState = null
            )
        } else {
            stateAfterMove.copy(phase = GamePhase.MOVE_CHECKERS)
        }
    }

    /**
     * Undoes the last move played in the current turn, restoring the unconsumed die.
     * Returns null if no moves have been made yet in this turn.
     */
    fun undoLastMove(state: BoardState): BoardState? {
        if (!state.canUndo()) return null
        val start = state.turnStartState ?: return null
        val movesToReplay = state.turnMoves.dropLast(1)

        var reconstructed = start
        for (m in movesToReplay) {
            reconstructed = applyMove(reconstructed, m)
        }
        return reconstructed
    }

    /**
     * Resets the active turn to its state prior to any checker moves, restoring all rolled dice.
     */
    fun resetTurn(state: BoardState): BoardState {
        return state.turnStartState ?: state
    }

    /**
     * Proposes a double using the Doubling Cube.
     */
    fun offerDouble(state: BoardState): BoardState {
        if (!state.canOfferDouble(state.turn)) return state
        return state.copy(
            phase = GamePhase.DOUBLING_OFFERED,
            doubler = state.turn
        )
    }

    /**
     * Opponent accepts the offered double.
     * Cube value doubles and cube ownership passes to the accepting player.
     */
    fun acceptDouble(state: BoardState): BoardState {
        if (state.phase != GamePhase.DOUBLING_OFFERED) return state
        val doubler = state.doubler ?: state.turn
        val acceptor = doubler.opponent()
        return state.copy(
            cubeValue = state.cubeValue * 2,
            cubeOwner = acceptor,
            doubler = null,
            phase = GamePhase.ROLL_DICE
        )
    }

    /**
     * Opponent declines the offered double and forfeits the game.
     * Active player wins with points equal to the current cube value.
     */
    fun declineDouble(state: BoardState): BoardState {
        if (state.phase != GamePhase.DOUBLING_OFFERED) return state
        val winner = state.doubler ?: state.turn
        return state.copy(
            winner = winner,
            winType = WinType.SINGLE,
            phase = GamePhase.GAME_OVER,
            doubler = null
        )
    }

    /**
     * Determines whether victory is Single (1x), Gammon (2x), or Backgammon (3x).
     * - Single: Loser has borne off at least 1 checker.
     * - Gammon: Loser has borne off 0 checkers.
     * - Backgammon: Loser has borne off 0 checkers AND has a checker on the bar or in the winner's home board.
     */
    fun determineWinType(
        winner: Player,
        loserOff: Int,
        loserBar: Int,
        points: IntArray
    ): WinType {
        if (loserOff > 0) return WinType.SINGLE

        val loserHasCheckersInWinnerHome = if (winner == Player.WHITE) {
            // White home board is 1..6; check for Black checkers (negative values)
            (1..6).any { points[it] < 0 }
        } else {
            // Black home board is 19..24; check for White checkers (positive values)
            (19..24).any { points[it] > 0 }
        }

        return if (loserBar > 0 || loserHasCheckersInWinnerHome) {
            WinType.BACKGAMMON
        } else {
            WinType.GAMMON
        }
    }

    /**
     * Internal helper to apply a single move without turn phase switching.
     */
    private fun applySingleMoveInternal(state: BoardState, move: Move): BoardState {
        val newPoints = state.points.copyOf()
        var newBarWhite = state.barWhite
        var newBarBlack = state.barBlack
        var newOffWhite = state.offWhite
        var newOffBlack = state.offBlack
        val player = state.turn

        // 1. Remove checker from source
        if (move.fromPoint == Move.BAR_POINT) {
            if (player == Player.WHITE) newBarWhite-- else newBarBlack--
        } else {
            if (player == Player.WHITE) {
                newPoints[move.fromPoint]--
            } else {
                newPoints[move.fromPoint]++
            }
        }

        // 2. Add checker to destination or bear off
        if (move.toPoint == Move.OFF_POINT) {
            if (player == Player.WHITE) newOffWhite++ else newOffBlack++
        } else {
            if (move.isHit) {
                if (player == Player.WHITE) {
                    newPoints[move.toPoint] = 1 // 1 White checker lands
                    newBarBlack++ // Black blot sent to bar
                } else {
                    newPoints[move.toPoint] = -1 // 1 Black checker lands
                    newBarWhite++ // White blot sent to bar
                }
            } else {
                if (player == Player.WHITE) {
                    newPoints[move.toPoint]++
                } else {
                    newPoints[move.toPoint]--
                }
            }
        }

        // 3. Consume die used
        val newDice = state.dice.toMutableList()
        newDice.remove(move.dieUsed)

        return state.copy(
            points = newPoints,
            barWhite = newBarWhite,
            barBlack = newBarBlack,
            offWhite = newOffWhite,
            offBlack = newOffBlack,
            dice = newDice
        )
    }
}
