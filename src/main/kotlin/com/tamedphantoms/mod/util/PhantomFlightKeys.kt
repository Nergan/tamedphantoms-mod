package com.tamedphantoms.mod.util

/**
 * Двойные нажатия верхом: «вперёд» — круиз, клавиша петли — петля,
 * клавиши бочки — бочка. Если отдельная клавиша не задана, петля читает «вверх»,
 * а бочка — «влево» и «вправо». Круиз гаснет от любой новой клавиши движения
 * и от нажатия петли или бочки. Окно двойного нажатия то же, что у ванильного рывка: 7 тиков.
 */
class PhantomFlightKeys(private val window: Int = DOUBLE_TAP_TICKS) {

    data class Keys(
        val forward: Boolean = false,
        val back: Boolean = false,
        val left: Boolean = false,
        val right: Boolean = false,
        val up: Boolean = false,
        val down: Boolean = false,
        val sneak: Boolean = false,
        /** Отдельная клавиша петли. null — та же, что [up]. */
        val loop: Boolean? = null,
        /** Отдельные клавиши бочки. null — те же, что [left] и [right]. */
        val rollLeft: Boolean? = null,
        val rollRight: Boolean? = null,
    )

    enum class CruiseChange { NONE, STARTED, STOPPED }

    data class Result(
        val cruise: CruiseChange,
        val loop: Boolean,
        /** +1 — бочка влево, −1 — вправо. */
        val roll: Int,
    )

    var cruising: Boolean = false
        private set

    private var previous = Keys()
    private var cruiseArmed = false
    private val lastTap = IntArray(TAP_KINDS) { NEVER }

    fun stop(): Boolean {
        val was = cruising
        cruising = false
        return was
    }

    fun tick(now: Int, keys: Keys, flying: Boolean, figures: Boolean = flying): Result {
        val loopDown = keys.loop ?: keys.up
        val rollLeftDown = keys.rollLeft ?: keys.left
        val rollRightDown = keys.rollRight ?: keys.right
        val pressedForward = keys.forward && !previous.forward
        val pressedBack = keys.back && !previous.back
        val pressedLeft = keys.left && !previous.left
        val pressedRight = keys.right && !previous.right
        val pressedUp = keys.up && !previous.up
        val pressedDown = keys.down && !previous.down
        val pressedSneak = keys.sneak && !previous.sneak
        val pressedLoop = loopDown && !(previous.loop ?: previous.up)
        val pressedRollLeft = rollLeftDown && !(previous.rollLeft ?: previous.left)
        val pressedRollRight = rollRightDown && !(previous.rollRight ?: previous.right)
        previous = keys
        val otherPressed = pressedBack || pressedLeft || pressedRight || pressedUp || pressedDown || pressedSneak ||
            pressedLoop || pressedRollLeft || pressedRollRight

        var change = CruiseChange.NONE
        if (cruising) {
            if (!flying || otherPressed || (cruiseArmed && pressedForward)) {
                cruising = false
                change = CruiseChange.STOPPED
            } else if (!keys.forward) {
                cruiseArmed = true
            }
        }

        val forwardTwice = tap(FORWARD, pressedForward, now)
        if (!cruising && change == CruiseChange.NONE && forwardTwice && flying) {
            cruising = true
            cruiseArmed = false
            change = CruiseChange.STARTED
        }
        val loop = tap(UP, pressedLoop, now) && figures
        val left = tap(LEFT, pressedRollLeft, now) && figures
        val right = tap(RIGHT, pressedRollRight, now) && figures
        val roll = when {
            left && !right -> 1
            right && !left -> -1
            else -> 0
        }
        return Result(change, loop, roll)
    }

    private fun tap(kind: Int, pressed: Boolean, now: Int): Boolean {
        if (!pressed) return false
        val hit = now - lastTap[kind] <= window
        lastTap[kind] = if (hit) NEVER else now
        return hit
    }

    companion object {
        const val DOUBLE_TAP_TICKS = 7
        private const val NEVER = Int.MIN_VALUE / 2
        private const val TAP_KINDS = 4
        private const val FORWARD = 0
        private const val UP = 1
        private const val LEFT = 2
        private const val RIGHT = 3
    }
}
