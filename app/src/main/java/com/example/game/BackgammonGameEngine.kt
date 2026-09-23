package com.example.game

import com.example.data.Board
import com.example.data.Match
import com.example.data.Piece
import com.example.data.Player as MatchPlayer

/**
 * State manager and rules orchestrator for Backgammon matches.
 *
 * Manages:
 * - Board state representation and transitions
 * - Individual piece positions and stack indices
 * - Dice rolling and doubles expansion
 * - Standard Backgammon move validation (Bar entry, point blocking, blot hitting, bearing off)
 * - Maximal dice enforcement and larger-die rule enforcement
 * - Doubling cube state transitions
 * - Move history and turn-level undo
 * - Firestore model conversions (Board, Piece, Player, Match)
 *
 * This class is completely decoupled from any UI, Android framework, or Compose code.
 */
class BackgammonGameEngine(
    initialState: BoardState = BackgammonEngine.createInitialBoard()
) {
    /** The current active board state */
    var currentState: BoardState = initialState
        private set

    /** Shorthand accessor for board state */
    val boardState: BoardState get() = currentState

    /** Currently active player turn (WHITE or BLACK) */
    val activePlayer: Player get() = currentState.turn

    /** Current game phase */
    val phase: GamePhase get() = currentState.phase

    /** Available dice for the current turn */
    val dice: List<Int> get() = currentState.dice

    /** Initial dice rolled for the current turn */
    val initialDice: List<Int> get() = currentState.initialDice

    /** Winner if the game is over, otherwise null */
    val winner: Player? get() = currentState.winner

    /** Type of victory if game over (Single, Gammon, Backgammon) */
    val winType: WinType? get() = currentState.winType

    /** Current doubling cube value */
    val cubeValue: Int get() = currentState.cubeValue

    /** Player who owns the doubling cube, or null if centered */
    val cubeOwner: Player? get() = currentState.cubeOwner

    /**
     * Resets the engine to a new game with the standard initial layout.
     */
    fun resetGame(firstPlayer: Player = Player.WHITE): BoardState {
        currentState = BackgammonEngine.createInitialBoard(firstPlayer)
        return currentState
    }

    /**
     * Loads the engine state directly from an existing [BoardState].
     */
    fun loadState(state: BoardState) {
        currentState = state
    }

    /**
     * Loads the engine state from a Firestore-compatible [Board] entity.
     */
    fun loadFromBoard(board: Board) {
        currentState = board.toBoardState()
    }

    /**
     * Loads the engine state from a Firestore-compatible [Match] entity.
     */
    fun loadFromMatch(match: Match) {
        currentState = match.board.toBoardState()
    }

    /**
     * Returns a Firestore-compatible [Board] snapshot of the current state.
     */
    fun getBoard(): Board = Board.fromBoardState(currentState)

    /**
     * Generates a complete list of all 30 checkers currently on the board, bar, or bear-off tray.
     * Each piece includes its owner color, point position (0 = bar, 1..24 = point, -1 = off),
     * and its vertical stack index.
     */
    fun getPieces(): List<Piece> = getBoard().pieces

    /**
     * Returns a map of board positions to pieces occupying that position.
     * Key 0 represents the Bar, 1..24 represent points, -1 represents borne-off pieces.
     */
    fun getPiecePositions(): Map<Int, List<Piece>> {
        return getPieces().groupBy { it.point }
    }

    /**
     * Returns the signed checker count on a given point (1..24).
     * Positive indicates White checkers, negative indicates Black checkers, 0 indicates empty.
     */
    fun getCheckersAt(point: Int): Int {
        if (point !in 1..24) return 0
        return currentState.points[point]
    }

    /**
     * Returns the [Player] controlling the given point, or null if the point is open/empty.
     */
    fun getPlayerAt(point: Int): Player? {
        val count = getCheckersAt(point)
        return when {
            count > 0 -> Player.WHITE
            count < 0 -> Player.BLACK
            else -> null
        }
    }

    /**
     * Checks if the given point is open (available to land on) for [player].
     */
    fun isPointOpen(point: Int, player: Player = activePlayer): Boolean {
        return currentState.isPointOpen(point, player)
    }

    /**
     * Checks if the given point holds an opponent blot (single vulnerable checker).
     */
    fun isBlot(point: Int, player: Player = activePlayer): Boolean {
        return currentState.isBlot(point, player)
    }

    /**
     * Executes the opening roll for both players to determine who starts.
     * Tied rolls require a re-roll.
     */
    fun openingRoll(forcedWhite: Int? = null, forcedBlack: Int? = null): OpeningRollResult {
        val result = BackgammonEngine.openingRoll(currentState, forcedWhite, forcedBlack)
        currentState = result.nextState
        return result
    }

    /**
     * Rolls two dice for the active player.
     * Expands doubles into 4 moves. If no legal moves are possible, automatically advances the turn.
     */
    fun rollDice(forcedD1: Int? = null, forcedD2: Int? = null): List<Int> {
        currentState = BackgammonEngine.rollDice(currentState, forcedD1, forcedD2)
        return currentState.dice
    }

    /**
     * Checks if all active checkers for [player] are in their home board and bar is empty.
     */
    fun canBearOff(player: Player = activePlayer): Boolean {
        return BackgammonEngine.canBearOff(currentState, player)
    }

    /**
     * Returns all legal single moves that can currently be played by the active player,
     * fully enforcing tournament maximal-dice and larger-die rules.
     */
    fun getLegalMoves(): List<Move> {
        return BackgammonEngine.getAllLegalMoves(currentState)
    }

    /**
     * Returns legal moves originating specifically from [fromPoint].
     * Use [Move.BAR_POINT] (0) to query re-entry moves from the bar.
     */
    fun getLegalMovesForPoint(fromPoint: Int): List<Move> {
        return BackgammonEngine.getLegalMovesForPoint(currentState, fromPoint)
    }

    /**
     * Validates whether a specific [Move] is legally playable right now.
     */
    fun isValidMove(move: Move): Boolean {
        return BackgammonEngine.isValidMove(currentState, move)
    }

    /**
     * Validates whether a move from [fromPoint] to [toPoint] is legally playable.
     */
    fun isValidMove(fromPoint: Int, toPoint: Int): Boolean {
        return getLegalMovesForPoint(fromPoint).any { it.toPoint == toPoint }
    }

    /**
     * Finds and applies the legal move matching [fromPoint] and [toPoint].
     * Returns true if a legal move was executed, false otherwise.
     */
    fun makeMove(fromPoint: Int, toPoint: Int): Boolean {
        val matchingMove = getLegalMovesForPoint(fromPoint).firstOrNull { it.toPoint == toPoint }
            ?: return false
        return applyMove(matchingMove)
    }

    /**
     * Applies a validated [Move] to the board state.
     * Consumes the used die, updates pieces, checks for hits and bearing off, and handles win conditions.
     * Returns true on success, false if the move was invalid.
     */
    fun applyMove(move: Move): Boolean {
        if (!isValidMove(move)) return false
        currentState = BackgammonEngine.applyMove(currentState, move)
        return true
    }

    /**
     * Undoes the most recent move in the current turn.
     * Returns true if a move was undone, false if no moves were made this turn.
     */
    fun undoLastMove(): Boolean {
        val previous = BackgammonEngine.undoLastMove(currentState) ?: return false
        currentState = previous
        return true
    }

    /**
     * Resets all moves made during the active player's turn, restoring all original dice.
     */
    fun resetTurn(): BoardState {
        currentState = BackgammonEngine.resetTurn(currentState)
        return currentState
    }

    /**
     * Checks if [player] is eligible to offer a double.
     */
    fun canOfferDouble(player: Player = activePlayer): Boolean {
        return currentState.canOfferDouble(player)
    }

    /**
     * Offers to double the game stakes.
     */
    fun offerDouble(): Boolean {
        if (!canOfferDouble()) return false
        currentState = BackgammonEngine.offerDouble(currentState)
        return true
    }

    /**
     * Opponent accepts the offered double.
     */
    fun acceptDouble(): Boolean {
        if (currentState.phase != GamePhase.DOUBLING_OFFERED) return false
        currentState = BackgammonEngine.acceptDouble(currentState)
        return true
    }

    /**
     * Opponent declines the offered double and forfeits.
     */
    fun declineDouble(): Boolean {
        if (currentState.phase != GamePhase.DOUBLING_OFFERED) return false
        currentState = BackgammonEngine.declineDouble(currentState)
        return true
    }

    /**
     * Checks if the game has ended in victory or resignation.
     */
    fun isGameOver(): Boolean = currentState.winner != null

    /**
     * Calculates the Pip count (distance to bear off all checkers) for [player].
     */
    fun getPipCount(player: Player): Int = currentState.pipCount(player)

    /**
     * Creates a Firestore-compatible [Match] representation with host and guest players.
     */
    fun createMatch(
        matchId: String,
        host: MatchPlayer,
        guest: MatchPlayer,
        mode: String = "ONLINE"
    ): Match {
        return Match(
            matchId = matchId,
            hostPlayer = host.copy(color = "WHITE", pipCount = getPipCount(Player.WHITE)),
            guestPlayer = guest.copy(color = "BLACK", pipCount = getPipCount(Player.BLACK)),
            board = getBoard(),
            status = if (isGameOver()) "FINISHED" else "PLAYING",
            mode = mode,
            winnerId = when (winner) {
                Player.WHITE -> host.id
                Player.BLACK -> guest.id
                null -> null
            },
            winnerColor = winner?.name,
            winType = winType?.name,
            pointsAwarded = (winType?.pointsMultiplier ?: 1) * cubeValue,
            createdAt = System.currentTimeMillis(),
            lastMoveAt = System.currentTimeMillis()
        )
    }

    /**
     * Updates an existing [Match] entity with the current engine state.
     */
    fun updateMatch(existingMatch: Match): Match {
        return existingMatch.copy(
            board = getBoard(),
            status = if (isGameOver()) "FINISHED" else "PLAYING",
            winnerId = when (winner) {
                Player.WHITE -> existingMatch.hostPlayer.id
                Player.BLACK -> existingMatch.guestPlayer.id
                null -> null
            },
            winnerColor = winner?.name,
            winType = winType?.name,
            pointsAwarded = (winType?.pointsMultiplier ?: 1) * cubeValue,
            lastMoveAt = System.currentTimeMillis()
        )
    }
}
