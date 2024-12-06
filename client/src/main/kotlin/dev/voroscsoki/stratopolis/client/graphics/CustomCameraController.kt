package dev.voroscsoki.stratopolis.client.graphics

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.math.Vector3
import dev.voroscsoki.stratopolis.common.elements.AgeGroup
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CustomCameraController(private val scene: MainScene) : InputAdapter() {
    private val invertedZoom = false
    private var ctrlModifier = false

    private val scope = CoroutineScope(Dispatchers.IO)
    private val mutex = Mutex()

    private val zoomHandler = SmoothMoveHandler(scene.cam) { cam, amount ->
        scope.launch {
            mutex.withLock {
                cam.translate(cam.direction.cpy().nor().scl(amount))
                //cam.update()
            }
        }
    }
    private val rotateHandler = SmoothMoveHandler(scene.cam) { cam, amount ->
        scope.launch {
            mutex.withLock {
                cam.rotateAround(cam.position, Vector3(0f, 1f, 0f), amount)
                cam.update()
            }
        }
    }
    private val linearHandler = SmoothMoveHandler(scene.cam) { cam, amount ->
        scope.launch {
            mutex.withLock {
                // positive amount should reasonably move the camera up
                // inversion necessary due to how translations work
                cam.translate(Vector3(cam.up.cpy().scl(-amount)))
                cam.update()
            }
        }
    }
    private val sidewaysHandler = SmoothMoveHandler(scene.cam) { cam, amount ->
        scope.launch {
            mutex.withLock {
                cam.translate(Vector3(cam.up.cpy().rotate(Vector3(0f,1f,0f), 90f).scl(-amount)))
                cam.update()
            }
        }
    }
    private var jobs = mutableMapOf<String, Job?>(
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
        if(!scene.isCameraMoveEnabled) return false

        val multiplier = if(ctrlModifier) 4f else 1f

        when (p0) {
            Keys.ESCAPE -> Gdx.app.exit()
            Keys.ALT_LEFT -> {
                scene.isAltPressed = true
                scene.forceHeatmap = true
            }
            Keys.CONTROL_LEFT -> {
                ctrlModifier = true
                jobs.forEach {
                    if(it.value?.isActive == true) cancelMove(it.key)
                }
            }

            Keys.E -> continuousMove(0.5f * multiplier, "rotate")
            Keys.Q -> continuousMove(-0.5f * multiplier, "rotate")
            Keys.A -> continuousMove(-4f * multiplier, "sideways")
            Keys.D -> continuousMove(4f * multiplier, "sideways")
            Keys.W -> continuousMove(-4f * multiplier, "linear")
            Keys.S -> continuousMove(4f * multiplier, "linear")
            Keys.NUM_1 -> scene.selectedAgeGroup = AgeGroup.CHILD
            Keys.NUM_2 -> scene.selectedAgeGroup = AgeGroup.TEEN
            Keys.NUM_3 -> scene.selectedAgeGroup = AgeGroup.ADULT
            Keys.NUM_4 -> scene.selectedAgeGroup = AgeGroup.SENIOR
            Keys.NUM_5 -> scene.selectedAgeGroup = null
            else -> return false
        }
        return super.keyDown(p0)
    }

    override fun keyUp(p0: Int): Boolean {
        if(p0 == Keys.CONTROL_LEFT) {
            ctrlModifier = false
            jobs.forEach {
                if(it.value?.isActive == true) cancelMove(it.key)
            }
        }
        if(p0 == Keys.ALT_LEFT) scene.isAltPressed = false
        if(p0 == Keys.Q || p0 == Keys.E) cancelMove("rotate")
        if(p0 == Keys.W || p0 == Keys.S) cancelMove("linear")
        if(p0 == Keys.A || p0 == Keys.D) cancelMove("sideways")
        return super.keyUp(p0)
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        CoroutineScope(Dispatchers.IO).launch {
            zoomHandler.requestMove(amountY * 5f
                    * if (invertedZoom) 1f else -1f
                    * if (ctrlModifier) 10f else 1f)
        }
        return super.scrolled(amountX, amountY)
    }

}
