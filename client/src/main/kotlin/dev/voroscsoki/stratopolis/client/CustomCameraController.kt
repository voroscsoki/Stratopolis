package dev.voroscsoki.stratopolis.client

import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.Vector3
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CustomCameraController(val cam: PerspectiveCamera) : InputAdapter() {
    val invertedZoom = false
    var ctrlModifier = false
    var shiftModifier = false

    private val scope = CoroutineScope(Dispatchers.IO)
    private val mutex = Mutex()

    private val zoomHandler = SmoothMoveHandler(cam) { cam, amount ->
        scope.launch {
            mutex.withLock {
                cam.translate(cam.direction.cpy().nor().scl(amount))
                cam.update()
            }
        }
    }
    private val rotateHandler = SmoothMoveHandler(cam) { cam, amount ->
        scope.launch {
            mutex.withLock {
                cam.rotateAround(Vector3(0f, 0f, 0f), Vector3(0f, 1f, 0f), amount)
                cam.update()
            }
        }
    }
    private val linearHandler = SmoothMoveHandler(cam) { cam, amount ->
        scope.launch {
            mutex.withLock {
                cam.translate(Vector3(0f, 0f, amount))
                cam.update()
            }
        }
    }
    private val sidewaysHandler = SmoothMoveHandler(cam) { cam, amount ->
        scope.launch {
            mutex.withLock {
                cam.translate(Vector3(amount, 0f, 0f))
                cam.update()
            }
        }
    }
    var jobs = mutableMapOf<String, Job?>(
        "linear" to null,
        "rotate" to null,
        "sideways" to null
    )

    private fun continuousMove(amount: Float, jobType: String) {
        jobs[jobType]?.cancel()
        jobs[jobType] = scope.launch {
            while(isActive) {
                when(jobType) {
                    "linear" -> linearHandler.requestMove(amount)
                    "rotate" -> rotateHandler.requestMove(amount)
                    "sideways" -> sidewaysHandler.requestMove(amount)
                }
                delay(16)
            }
        }
    }
    private fun cancelMove(jobType: String) {
        jobs[jobType]?.cancel()
        jobs[jobType] = null
    }

    //val zoomHandler = SmoothMoveHandler(cam) { cam, amount -> cam.translate(cam.direction.cpy().nor().scl(amount)) }

    override fun keyDown(p0: Int): Boolean {
        if(p0 == Keys.CONTROL_LEFT) {
            ctrlModifier = true
        }
        if(p0 == Keys.E) {
            continuousMove(5f, "rotate")
        }
        if(p0 == Keys.Q) {
            continuousMove(-5f, "rotate")
        }
        if(p0 == Keys.A) {
            continuousMove(-0.5f, "sideways")
        }
        if(p0 == Keys.D) {
            continuousMove(0.5f, "sideways")
        }
        if (p0 == Keys.W) {
            continuousMove(-0.5f, "linear")
        }
        if (p0 == Keys.S) {
            continuousMove(0.5f, "linear")
        }
        return super.keyDown(p0)
    }

    override fun keyUp(p0: Int): Boolean {
        if(p0 == Keys.CONTROL_LEFT) {
            ctrlModifier = false
        }
        if(p0 == Keys.Q || p0 == Keys.E) cancelMove("rotate")
        if(p0 == Keys.W || p0 == Keys.S) cancelMove("linear")
        if(p0 == Keys.A || p0 == Keys.D) cancelMove("sideways")
        return super.keyUp(p0)
    }

    override fun keyTyped(p0: Char): Boolean {
        return super.keyTyped(p0)
    }

    override fun touchDown(p0: Int, p1: Int, p2: Int, p3: Int): Boolean {
        return super.touchDown(p0, p1, p2, p3)
    }

    override fun touchUp(p0: Int, p1: Int, p2: Int, p3: Int): Boolean {
        return super.touchUp(p0, p1, p2, p3)
    }

    override fun touchCancelled(p0: Int, p1: Int, p2: Int, p3: Int): Boolean {
        return super.touchCancelled(p0, p1, p2, p3)
    }

    override fun touchDragged(p0: Int, p1: Int, p2: Int): Boolean {
        return super.touchDragged(p0, p1, p2)
    }

    override fun mouseMoved(p0: Int, p1: Int): Boolean {
        return super.mouseMoved(p0, p1)
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        CoroutineScope(Dispatchers.IO).launch {
            zoomHandler.requestMove(amountY * 5f
                    * if (invertedZoom) 1f else -1f
                    * if (ctrlModifier) 4f else 1f)
        }
        return super.scrolled(amountX, amountY)
    }

}
