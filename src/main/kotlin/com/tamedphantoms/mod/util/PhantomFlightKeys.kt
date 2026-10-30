package com.tamedphantoms.mod.util

/**
 * Двойные нажатия верхом: «вперёд» — круиз, «вверх» — петля,
 * «влево» или «вправо» — бочка. Круиз гаснет от любой новой клавиши движения.
 * Окно двойного нажатия то же, что у ванильного рывка: 7 тиков.
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

    fun tick(now: Int, keys: Keys, flying: Boolean): Result {
        val pressedForward = keys.forward && !previous.forward
        val pressedBack = keys.back && !previous.back
        val pressedLeft = keys.left && !previous.left
        val pressedRight = keys.right && !previous.right
        val pressedUp = keys.up && !previous.up
        val pressedDown = keys.down && !previous.down
        val pressedSneak = keys.sneak && !previous.sneak
        previous = keys
        val otherPressed = pressedBack || pressedLeft || pressedRight || pressedUp || pressedDown || pressedSneak

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
        val loop = tap(UP, pressedUp, now) && flying
        val left = tap(LEFT, pressedLeft, now) && flying
        val right = tap(RIGHT, pressedRight, now) && flying
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
