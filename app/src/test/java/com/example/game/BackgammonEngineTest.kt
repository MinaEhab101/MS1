package com.example.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgammonEngineTest {

    @Test
    fun testInitialBoardSetup() {
        val board = BackgammonEngine.createInitialBoard()

        assertEquals(15, board.totalCheckers(Player.WHITE))
        assertEquals(15, board.totalCheckers(Player.BLACK))
        assertEquals(0, board.barWhite)
        assertEquals(0, board.barBlack)
        assertEquals(0, board.offWhite)
        assertEquals(0, board.offBlack)
        assertEquals(Player.WHITE, board.turn)
        assertEquals(GamePhase.ROLL_DICE, board.phase)

        // Check starting point values
        assertEquals(2, board.points[24]) // 2 White checkers
        assertEquals(5, board.points[13]) // 5 White checkers
        assertEquals(3, board.points[8])  // 3 White checkers
        assertEquals(5, board.points[6])  // 5 White checkers

        assertEquals(-2, board.points[1])  // 2 Black checkers
        assertEquals(-5, board.points[12]) // 5 Black checkers
        assertEquals(-3, board.points[17]) // 3 Black checkers
        assertEquals(-5, board.points[19]) // 5 Black checkers
    }

    @Test
    fun testPipCountsAtStart() {
        val board = BackgammonEngine.createInitialBoard()
        // Standard starting pip count for both players is 167
        // White: 2*24 + 5*13 + 3*8 + 5*6 = 48 + 65 + 24 + 30 = 167
        assertEquals(167, board.pipCount(Player.WHITE))
        // Black: 2*24 + 5*13 + 3*8 + 5*6 = 167
        assertEquals(167, board.pipCount(Player.BLACK))
    }

    @Test
    fun testDiceRollDoublesProvidesFourMoves() {
        val board = BackgammonEngine.createInitialBoard()
        val rolled = BackgammonEngine.rollDice(board, forcedD1 = 4, forcedD2 = 4)

        assertEquals(listOf(4, 4, 4, 4), rolled.dice)
        assertEquals(listOf(4, 4, 4, 4), rolled.initialDice)
        assertEquals(Pair(4, 4), rolled.lastRoll)
        assertEquals(GamePhase.MOVE_CHECKERS, rolled.phase)
    }

    @Test
    fun testBlockedPointEnforcement() {
        val board = BackgammonEngine.createInitialBoard()
        // White turn. Points with 2+ Black checkers: 1, 12, 17, 19
        // White has 5 checkers on 6. If White rolls 5: 6 - 5 = 1.
        // Point 1 has -2 (2 Black checkers), which is blocked!
        val rolled = BackgammonEngine.rollDice(board, forcedD1 = 5, forcedD2 = 2)

        val movesFrom6 = BackgammonEngine.getLegalMovesForPoint(rolled, 6)
        // Move to point 1 using 5 should be blocked!
        assertFalse(movesFrom6.any { it.toPoint == 1 })
        // Move to point 4 using 2 is open!
        assertTrue(movesFrom6.any { it.toPoint == 4 && it.dieUsed == 2 })
    }

    @Test
    fun testBlotHittingEnforcement() {
        val initial = BackgammonEngine.createInitialBoard()
        // Set up a Black blot on point 20 (1 checker = -1)
        val customPoints = initial.points.copyOf()
        customPoints[20] = -1 // single blot

        val state = initial.copy(points = customPoints)
        val rolled = BackgammonEngine.rollDice(state, forcedD1 = 4, forcedD2 = 2)

        // White on point 24 moving with 4: 24 - 4 = 20 (hits the blot!)
        val legalMoves = BackgammonEngine.getLegalMovesForPoint(rolled, 24)
        val hitMove = legalMoves.find { it.toPoint == 20 && it.dieUsed == 4 }
        assertNotNull(hitMove)
        assertTrue(hitMove!!.isHit)

        // Apply hit move
        val afterHit = BackgammonEngine.applyMove(rolled, hitMove)
        // Point 20 should now have 1 White checker (+1)
        assertEquals(1, afterHit.points[20])
        // Black should now have 1 checker on the bar!
        assertEquals(1, afterHit.barBlack)
    }

    @Test
    fun testForcedBarEntryEnforcement() {
        val initial = BackgammonEngine.createInitialBoard()
        // Put a White checker on the bar
        val customPoints = initial.points.copyOf()
        customPoints[24] = 1 // remove one from 24
        val state = initial.copy(points = customPoints, barWhite = 1)

        val rolled = BackgammonEngine.rollDice(state, forcedD1 = 3, forcedD2 = 2)
        val allLegal = BackgammonEngine.getAllLegalMoves(rolled)

        // Player MUST enter from bar: all first legal moves must have fromPoint == BAR_POINT
        assertTrue(allLegal.isNotEmpty())
        assertTrue(allLegal.all { it.fromPoint == Move.BAR_POINT })
    }

    @Test
    fun testBearingOffValidation() {
        // Create position where White has all checkers in home board (1..6)
        val homePoints = IntArray(25)
        homePoints[6] = 5
        homePoints[5] = 5
        homePoints[4] = 5

        val state = BoardState(
            points = homePoints,
            barWhite = 0,
            turn = Player.WHITE,
            phase = GamePhase.ROLL_DICE
        )

        assertTrue(state.canBearOff(Player.WHITE))

        val rolled = BackgammonEngine.rollDice(state, forcedD1 = 6, forcedD2 = 5)
        val movesFrom6 = BackgammonEngine.getLegalMovesForPoint(rolled, 6)
        val bearOffFrom6 = movesFrom6.find { it.toPoint == Move.OFF_POINT && it.dieUsed == 6 }
        assertNotNull(bearOffFrom6)
        assertTrue(bearOffFrom6!!.isBearingOff)
    }

    @Test
    fun testBearingOffOverDieRule() {
        // White only has checkers on points 3 and 2. None on 4, 5, 6.
        val homePoints = IntArray(25)
        homePoints[3] = 2
        homePoints[2] = 2

        val state = BoardState(
            points = homePoints,
            barWhite = 0,
            turn = Player.WHITE,
            phase = GamePhase.ROLL_DICE
        )

        // Roll 6 and 5 (both higher than highest checker 3)
        val rolled = BackgammonEngine.rollDice(state, forcedD1 = 6, forcedD2 = 5)

        // Die 6 can bear off from point 3 (highest occupied point)
        val movesFrom3 = BackgammonEngine.getLegalMovesForPoint(rolled, 3)
        assertTrue(movesFrom3.any { it.toPoint == Move.OFF_POINT && it.dieUsed == 6 })

        // Die 6 CANNOT bear off from point 2 directly, because point 3 is higher and occupied!
        val movesFrom2 = BackgammonEngine.getLegalMovesForPoint(rolled, 2)
        assertFalse(movesFrom2.any { it.toPoint == Move.OFF_POINT && it.dieUsed == 6 })
    }

    @Test
    fun testMaximumDiceRule() {
        // Standard rule: player must play both dice if legally possible.
        // If playing die A blocks playing die B, but playing die B allows playing die A,
        // then the player MUST start with die B so that both dice are played.
        val points = IntArray(25)
        // White checker on point 10
        points[10] = 1
        // Opponent blocks point 4 with 2 checkers
        points[4] = -2
        // All other points open
        val state = BoardState(
            points = points,
            turn = Player.WHITE,
            dice = listOf(6, 3),
            initialDice = listOf(6, 3),
            phase = GamePhase.MOVE_CHECKERS
        )

        // If White plays 6 first: 10 - 6 = 4 (BLOCKED by Black checkers on 4!).
        // If White plays 3 first: 10 - 3 = 7 (Open), and then 7 - 6 = 1 (Open!).
        // Thus, the only sequence of length 2 starts with die 3!
        val legalMoves = BackgammonEngine.getAllLegalMoves(state)
        assertEquals(1, legalMoves.size)
        assertEquals(3, legalMoves[0].dieUsed)
        assertEquals(7, legalMoves[0].toPoint)
    }

    @Test
    fun testLargerDieRule() {
        // Standard rule: if either die can be played, but not both,
        // the player MUST play the larger number.
        val points = IntArray(25)
        // White checker on point 10
        points[10] = 1
        // Black blocks point 4 (10 - 6) and point 7 (10 - 3)?
        // No: suppose point 7 (10 - 3) is open, but point 1 (7 - 6) is blocked.
        // And suppose point 5 (10 - 5) is open... Let's construct:
        // Checker on 10.
        // Die 6: target 4 is open. But from 4, target 4 - 2 = 2 is blocked (-2). So only 1 move.
        // Die 2: target 8 is open. But from 8, target 8 - 6 = 2 is blocked (-2). So only 1 move.
        // Both 6 and 2 can be played as a single move, but NOT together (both lead to blocked point 2).
        points[2] = -2 // blocks second move

        val state = BoardState(
            points = points,
            turn = Player.WHITE,
            dice = listOf(6, 2),
            initialDice = listOf(6, 2),
            phase = GamePhase.MOVE_CHECKERS
        )

        val sequences = BackgammonEngine.getAllLegalMoveSequences(state)
        // Max sequence length is 1
        assertTrue(sequences.all { it.size == 1 })
        // Must play the larger die (6)
        val legalMoves = BackgammonEngine.getAllLegalMoves(state)
        assertEquals(1, legalMoves.size)
        assertEquals(6, legalMoves[0].dieUsed)
        assertEquals(4, legalMoves[0].toPoint)
    }

    @Test
    fun testVictoryAndWinTypeGammon() {
        // White bears off all checkers, Black has borne off 0 checkers and has no checkers in White home
        val points = IntArray(25)
        points[19] = -15 // All 15 Black checkers in Black home board

        val state = BoardState(
            points = points,
            offWhite = 14,
            turn = Player.WHITE,
            phase = GamePhase.MOVE_CHECKERS,
            dice = listOf(1)
        )
        // Put 1 White checker on point 1
        val pointsWithOne = points.copyOf()
        pointsWithOne[1] = 1
        val stateWithChecker = state.copy(points = pointsWithOne)

        val winMove = Move(fromPoint = 1, toPoint = Move.OFF_POINT, dieUsed = 1)
        val finalState = BackgammonEngine.applyMove(stateWithChecker, winMove)

        assertEquals(Player.WHITE, finalState.winner)
        assertEquals(WinType.GAMMON, finalState.winType)
        assertEquals(GamePhase.GAME_OVER, finalState.phase)
    }

    @Test
    fun testVictoryAndWinTypeBackgammon() {
        // White bears off all checkers, Black has a checker on the bar!
        val points = IntArray(25)
        points[19] = -14

        val pointsWithOne = points.copyOf()
        pointsWithOne[1] = 1

        val state = BoardState(
            points = pointsWithOne,
            barBlack = 1,
            offWhite = 14,
            turn = Player.WHITE,
            phase = GamePhase.MOVE_CHECKERS,
            dice = listOf(1)
        )

        val winMove = Move(fromPoint = 1, toPoint = Move.OFF_POINT, dieUsed = 1)
        val finalState = BackgammonEngine.applyMove(state, winMove)

        assertEquals(Player.WHITE, finalState.winner)
        assertEquals(WinType.BACKGAMMON, finalState.winType)
        assertEquals(GamePhase.GAME_OVER, finalState.phase)
    }

    @Test
    fun testDoublingCubeFlow() {
        val board = BackgammonEngine.createInitialBoard()
        assertEquals(1, board.cubeValue)
        assertNull(board.cubeOwner)
        assertTrue(board.canOfferDouble(Player.WHITE))

        // White offers double
        val offered = BackgammonEngine.offerDouble(board)
        assertEquals(GamePhase.DOUBLING_OFFERED, offered.phase)
        assertEquals(Player.WHITE, offered.doubler)

        // Black accepts double
        val accepted = BackgammonEngine.acceptDouble(offered)
        assertEquals(2, accepted.cubeValue)
        assertEquals(Player.BLACK, accepted.cubeOwner)
        assertEquals(GamePhase.ROLL_DICE, accepted.phase)

        // Now Black owns the cube: White CANNOT double again!
        assertFalse(accepted.canOfferDouble(Player.WHITE))
        // But Black CAN double on their turn
        val blackTurn = accepted.copy(turn = Player.BLACK)
        assertTrue(blackTurn.canOfferDouble(Player.BLACK))
    }

    @Test
    fun testTurnMoveUndo() {
        val board = BackgammonEngine.createInitialBoard()
        val rolled = BackgammonEngine.rollDice(board, forcedD1 = 3, forcedD2 = 1)

        val legal = BackgammonEngine.getAllLegalMoves(rolled)
        val firstMove = legal.first()

        val afterMove = BackgammonEngine.applyMove(rolled, firstMove)
        assertTrue(afterMove.canUndo())

        val undone = BackgammonEngine.undoLastMove(afterMove)
        assertNotNull(undone)
        assertEquals(rolled.dice, undone!!.dice)
        assertEquals(rolled.points.toList(), undone.points.toList())
    }
}
