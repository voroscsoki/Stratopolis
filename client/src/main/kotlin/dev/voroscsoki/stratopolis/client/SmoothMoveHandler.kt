package dev.voroscsoki.stratopolis.client

import com.badlogic.gdx.graphics.PerspectiveCamera
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.absoluteValue
import kotlin.math.sign

class SmoothMoveHandler(val camera: PerspectiveCamera, private val translateFun: (PerspectiveCamera, Float) -> Unit) {
    data class MoveState(
        val isMoving: Boolean = false,
        val remainingAmount: Float = 0f,
        val direction: Int = 0 // -1: in, 1: out, 0: none
    )

    private val _state = MutableStateFlow(MoveState())
    private var moveJob: Job? = null

    fun requestMove(amount: Float, debouncingFactor: Float = 0.04f) {
        val currentState = _state.value
        val newDirection = amount.sign.toInt()
        if (currentState.isMoving && currentState.direction == newDirection) {
            _state.value = currentState.copy(
                remainingAmount = currentState.remainingAmount + amount
            )
            return
        }

        moveJob?.cancel()
        moveJob = CoroutineScope(Dispatchers.IO).launch {
            _state.value = MoveState(
                isMoving = true,
                remainingAmount = amount,
                direction = newDirection
            )
            var remaining = amount
            while (remaining.absoluteValue > 0.005f && isActive) {
                remaining = _state.value.remainingAmount
                val step =
                    remaining * debouncingFactor

                translateFun(camera, step)
                camera.update()
                remaining -= step
                _state.value = _state.value.copy(remainingAmount = remaining)
                delay(8)
            }
        }
    }

    fun cancel() {
        moveJob?.cancel()
        moveJob = null
        _state.value = MoveState()
    }
}