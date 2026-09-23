package com.example.game

/**
 * Detailed outcome of validating a Backgammon checker move.
 */
sealed class MoveValidationResult {
    data class Valid(
        val move: Move,
        val isHit: Boolean,
        val isBearingOff: Boolean,
        val explanationAr: String,
        val explanationEn: String
    ) : MoveValidationResult()

    data class Invalid(
        val reason: InvalidReason,
        val messageAr: String,
        val messageEn: String
    ) : MoveValidationResult()
}

/**
 * Specific rule violation reasons for invalid Backgammon moves.
 */
enum class InvalidReason {
    NOT_YOUR_TURN,
    WRONG_PHASE,
    NO_DICE_AVAILABLE,
    NO_PIECE_AT_SOURCE,
    WRONG_PIECE_COLOR,
    MUST_ENTER_FROM_BAR,
    POINT_BLOCKED,
    CANNOT_BEAR_OFF_YET,
    HIGHER_CHECKER_MUST_MOVE,
    NO_MATCHING_DIE,
    LARGER_DIE_REQUIRED,
    INVALID_DIRECTION,
    OUT_OF_BOUNDS
}

/**
 * Dedicated Move Validation Service enforcing standard international Backgammon tournament rules:
 * - Checks player turns and active movement phase.
 * - Bar entry priority: if any checker is on the Bar, it MUST enter before any board moves.
 * - Point blocking: points occupied by 2 or more opponent checkers cannot be landed on.
 * - Blot hitting: points occupied by exactly 1 opponent checker are hit and sent to the Bar.
 * - Bearing off criteria: all remaining active checkers MUST be inside the Home Board (1..6 for White, 19..24 for Black).
 * - Bearing off over-die: bearing off with a die higher than the checker's point is permitted ONLY
 *   if no checkers reside on higher points in the Home Board.
 * - Maximal dice usage and larger die rule enforcement.
 */
object MoveValidationService {

    /**
     * Validates a candidate move from [fromPoint] to [toPoint] under the current [state].
     *
     * @param state The active board state.
     * @param fromPoint Starting position (Move.BAR_POINT = 0, or 1..24).
     * @param toPoint Destination position (1..24, or Move.OFF_POINT = -1).
     * @return [MoveValidationResult.Valid] if legal, or [MoveValidationResult.Invalid] with full Arabic/English explanation.
     */
    fun validateMove(
        state: BoardState,
        fromPoint: Int,
        toPoint: Int
    ): MoveValidationResult {
        val player = state.turn

        // 1. Phase Check
        if (state.phase != GamePhase.MOVE_CHECKERS) {
            return MoveValidationResult.Invalid(
                InvalidReason.WRONG_PHASE,
                "المرحلة الحالية ليست مرحلة تحريك القطع. يجب رمي النرد أولاً.",
                "Current phase is not checker movement. Roll the dice first."
            )
        }

        // 2. Dice Available Check
        if (state.dice.isEmpty()) {
            return MoveValidationResult.Invalid(
                InvalidReason.NO_DICE_AVAILABLE,
                "لا يوجد نرد متبقٍ لهذه الجولة.",
                "No dice available for this turn."
            )
        }

        // 3. Piece Ownership & Origin Check
        if (fromPoint == Move.BAR_POINT) {
            val barCount = if (player == Player.WHITE) state.barWhite else state.barBlack
            if (barCount <= 0) {
                return MoveValidationResult.Invalid(
                    InvalidReason.NO_PIECE_AT_SOURCE,
                    "لا توجد قطع لك على الحاجز (Bar).",
                    "You have no pieces on the Bar."
                )
            }
        } else {
            if (fromPoint !in 1..24) {
                return MoveValidationResult.Invalid(
                    InvalidReason.OUT_OF_BOUNDS,
                    "موقع البداية خارج حدود الرقعة ($fromPoint).",
                    "Source position is out of board bounds ($fromPoint)."
                )
            }
            val count = state.points[fromPoint]
            if (count == 0) {
                return MoveValidationResult.Invalid(
                    InvalidReason.NO_PIECE_AT_SOURCE,
                    "لا توجد قطع على النقطة $fromPoint.",
                    "No pieces on point $fromPoint."
                )
            }
            val ownsPiece = if (player == Player.WHITE) count > 0 else count < 0
            if (!ownsPiece) {
                return MoveValidationResult.Invalid(
                    InvalidReason.WRONG_PIECE_COLOR,
                    "هذه القطعة ملك للخصم ولا يمكنك تحريكها.",
                    "This piece belongs to the opponent."
                )
            }
        }

        // 4. Bar Priority Check: If player has checkers on bar, MUST move from bar first
        val hasCheckersOnBar = if (player == Player.WHITE) state.barWhite > 0 else state.barBlack > 0
        if (hasCheckersOnBar && fromPoint != Move.BAR_POINT) {
            return MoveValidationResult.Invalid(
                InvalidReason.MUST_ENTER_FROM_BAR,
                "يجب إدخال قطعك من الحاجز (Bar) أولاً قبل تحريك أي قطعة أخرى على الرقعة!",
                "You must enter your pieces from the Bar before making any board moves!"
            )
        }

        // 5. Calculate Required Distance
        val distance = calculateDistance(player, fromPoint, toPoint)
        if (distance == null || distance <= 0) {
            return MoveValidationResult.Invalid(
                InvalidReason.INVALID_DIRECTION,
                "اتجاه الحركة غير صحيح. تتحرك قطعك بالاتجاه المعاكس.",
                "Invalid move direction. Checkers move in the opposite direction."
            )
        }

        // 6. Bearing Off Specific Validation
        val isBearingOff = toPoint == Move.OFF_POINT
        if (isBearingOff) {
            if (!state.canBearOff(player)) {
                return MoveValidationResult.Invalid(
                    InvalidReason.CANNOT_BEAR_OFF_YET,
                    "لا يمكنك إخراج القطع حتى تجمع كافة قطعك الـ15 داخل بيتك!",
                    "You cannot bear off pieces until all 15 checkers are in your Home Board!"
                )
            }

            // Check if exact die or valid over-die
            val exactDieAvailable = state.dice.contains(distance)
            val higherDice = state.dice.filter { it > distance }

            if (!exactDieAvailable) {
                if (higherDice.isEmpty()) {
                    return MoveValidationResult.Invalid(
                        InvalidReason.NO_MATCHING_DIE,
                        "تحتاج نرد بقيمة $distance لإخراج هذه القطعة، ولا تملك هذا النرد.",
                        "You need a die of value $distance to bear off this piece."
                    )
                }

                // Over-die rule: allowed ONLY if no checkers exist on points higher than fromPoint in home board
                val higherCheckersExist = hasCheckersBehindInHomeBoard(state, player, fromPoint)
                if (higherCheckersExist) {
                    return MoveValidationResult.Invalid(
                        InvalidReason.HIGHER_CHECKER_MUST_MOVE,
                        "لا يمكنك استخدام نرد أكبر لإخراج هذه القطعة طالما توجد قطع أخرى في نقاط أبعد في بيتك!",
                        "Over-die bear off is only permitted if no checkers occupy higher points in your Home Board!"
                    )
                }
            }
        } else {
            // Normal or Bar-to-Board move
            if (toPoint !in 1..24) {
                return MoveValidationResult.Invalid(
                    InvalidReason.OUT_OF_BOUNDS,
                    "الهدف خارج حدود الرقعة ($toPoint).",
                    "Target point is out of board bounds ($toPoint)."
                )
            }

            // Target Point Blocking Check (2 or more opponent pieces)
            if (!state.isPointOpen(toPoint, player)) {
                val opponentCheckers = kotlin.math.abs(state.points[toPoint])
                return MoveValidationResult.Invalid(
                    InvalidReason.POINT_BLOCKED,
                    "النقطة $toPoint مغلقة ومحمية بجدار من $opponentCheckers قطع للخصم!",
                    "Point $toPoint is blocked by $opponentCheckers opponent checkers!"
                )
            }
        }

        // 7. Match against Tournament Legal Moves (incorporating larger die and maximum dice rules)
        val allLegalMoves = BackgammonEngine.getAllLegalMoves(state)
        val matchingLegalMove = allLegalMoves.firstOrNull {
            it.fromPoint == fromPoint && it.toPoint == toPoint
        }

        if (matchingLegalMove != null) {
            val isHit = state.isBlot(toPoint, player)
            val explanationAr = when {
                isBearingOff -> "حركة صحيحة: إخراج قطعة بنجاح إلى خارج الرقعة بالنرد ${matchingLegalMove.dieUsed}."
                isHit -> "حركة هجومية رائعة! صيد بلطة الخصم على النقطة $toPoint وإرسالها للحاجز بالنرد ${matchingLegalMove.dieUsed}!"
                fromPoint == Move.BAR_POINT -> "حركة صحيحة: إعادة إدخال القطعة من الحاجز إلى النقطة $toPoint بالنرد ${matchingLegalMove.dieUsed}."
                else -> "حركة صحيحة: تقدم من النقطة $fromPoint إلى $toPoint بالنرد ${matchingLegalMove.dieUsed}."
            }
            val explanationEn = when {
                isBearingOff -> "Valid move: Borne off checker using die ${matchingLegalMove.dieUsed}."
                isHit -> "Hit! Opponent blot hit on point $toPoint and sent to the Bar using die ${matchingLegalMove.dieUsed}!"
                fromPoint == Move.BAR_POINT -> "Valid move: Entered piece from Bar to point $toPoint using die ${matchingLegalMove.dieUsed}."
                else -> "Valid move: Advanced from point $fromPoint to $toPoint using die ${matchingLegalMove.dieUsed}."
            }

            return MoveValidationResult.Valid(
                move = matchingLegalMove,
                isHit = isHit,
                isBearingOff = isBearingOff,
                explanationAr = explanationAr,
                explanationEn = explanationEn
            )
        }

        // 8. If move was physically possible but filtered out by tournament maximal/larger die rules:
        val rawMoves = state.dice.distinct().flatMap { die ->
            BackgammonEngine.getRawLegalMoves(state, die)
        }
        val isRawPossible = rawMoves.any { it.fromPoint == fromPoint && it.toPoint == toPoint }

        return if (isRawPossible) {
            MoveValidationResult.Invalid(
                InvalidReason.LARGER_DIE_REQUIRED,
                "حسب قواعد الطاولة الدولية، إذا أمكن لعب أحد النردين فقط، فيجب إلزامياً لعب النرد ذي القيمة الأكبر!",
                "According to international tournament rules, if only one die can be played, you MUST play the higher die!"
            )
        } else {
            MoveValidationResult.Invalid(
                InvalidReason.NO_MATCHING_DIE,
                "لا يوجد نرد متاح يتيح الوصول بدقة من $fromPoint إلى $toPoint.",
                "No rolled die allows moving directly from $fromPoint to $toPoint."
            )
        }
    }

    /**
     * Returns all valid landing points for the given [fromPoint], along with their validation results.
     */
    fun getValidDestinations(state: BoardState, fromPoint: Int): List<Move> {
        val allLegal = BackgammonEngine.getAllLegalMoves(state)
        return allLegal.filter { it.fromPoint == fromPoint }
    }

    /**
     * Calculates the move distance required by Backgammon direction conventions.
     */
    fun calculateDistance(player: Player, fromPoint: Int, toPoint: Int): Int? {
        return if (player == Player.WHITE) {
            when {
                fromPoint == Move.BAR_POINT -> 25 - toPoint
                toPoint == Move.OFF_POINT -> fromPoint
                toPoint < fromPoint -> fromPoint - toPoint
                else -> null
            }
        } else {
            when {
                fromPoint == Move.BAR_POINT -> toPoint
                toPoint == Move.OFF_POINT -> 25 - fromPoint
                toPoint > fromPoint -> toPoint - fromPoint
                else -> null
            }
        }
    }

    private fun hasCheckersBehindInHomeBoard(state: BoardState, player: Player, point: Int): Boolean {
        return if (player == Player.WHITE) {
            // Points 6 down to (point + 1)
            (point + 1..6).any { state.points[it] > 0 }
        } else {
            // Points 19 up to (point - 1)
            (19 until point).any { state.points[it] < 0 }
        }
    }
}
