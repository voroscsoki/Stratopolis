package dev.voroscsoki.stratopolis.client

import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.Vector3
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

class CustomCameraController(val cam: PerspectiveCamera) : InputAdapter() {
    val invertedZoom = false
    var ctrlModifier = false
    var shiftModifier = false

    private suspend fun debouncedZoom(target: Float, debouncingFactor: Float = 0.08f) {
        var zoomTarget = target
        val tempVec = Vector3()
        withContext(Dispatchers.IO) {
            while (zoomTarget.absoluteValue > 0) {
                val zoomStep = if(zoomTarget.absoluteValue > 0.005f) zoomTarget * debouncingFactor else zoomTarget
                cam.translate(tempVec.set(cam.direction).scl(zoomStep))
                println("Scrolling $zoomStep")
                zoomTarget -= zoomStep
                cam.update()
                Thread.sleep(8)
            }
        }
        println(cam.position)
    }

    override fun keyDown(p0: Int): Boolean {
        if(p0 == 129) {
            ctrlModifier = true
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
            debouncedZoom(amountY * 5f
                    * if (invertedZoom) 1f else -1f
                    * if (ctrlModifier) 0.1f else 1f
            , if(shiftModifier) 1f else 0.08f)
        }
        return super.scrolled(amountX, amountY)
    }

}
