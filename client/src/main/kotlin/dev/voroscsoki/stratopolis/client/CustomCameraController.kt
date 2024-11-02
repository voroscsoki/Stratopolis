package dev.voroscsoki.stratopolis.client

import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.Vector3
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CustomCameraController(val cam: PerspectiveCamera) : InputAdapter() {
    val invertedZoom = false
    var ctrlModifier = false
    var shiftModifier = false
    val scope = CoroutineScope(Dispatchers.IO)
    private val mutex = Mutex()
    val zoomHandler = SmoothMoveHandler(cam) { cam, amount ->
        scope.launch {
            mutex.withLock {
                cam.translate(cam.direction.cpy().nor().scl(amount))
                cam.update()
            }
        }
    }
    val rotateHandler = SmoothMoveHandler(cam) { cam, amount ->
        scope.launch {
            mutex.withLock {
                cam.rotateAround(Vector3(0f, 0f, 0f), Vector3(0f, 1f, 0f), amount)
                cam.update()
            }
        }
    }
    //val zoomHandler = SmoothMoveHandler(cam) { cam, amount -> cam.translate(cam.direction.cpy().nor().scl(amount)) }

    override fun keyDown(p0: Int): Boolean {
        if(p0 == 129) {
            ctrlModifier = true
        }
        //E: rotate right
        if(p0 == 33) {
            rotateHandler.requestMove(10f)
        }
        //Q: rotate left
        if(p0 == 45) {
            rotateHandler.requestMove(-10f)
        }
        return super.keyDown(p0)
    }

    override fun keyUp(p0: Int): Boolean {
        if(p0 == 129) {
            ctrlModifier = false
        }
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
                    * if (ctrlModifier) 0.1f else 1f)
        }
        return super.scrolled(amountX, amountY)
    }

}
