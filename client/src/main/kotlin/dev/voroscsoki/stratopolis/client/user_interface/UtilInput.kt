package dev.voroscsoki.stratopolis.client.user_interface

import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import dev.voroscsoki.stratopolis.client.Main
import dev.voroscsoki.stratopolis.client.graphics.MainScene
import dev.voroscsoki.stratopolis.client.graphics.toWorldCoords
import dev.voroscsoki.stratopolis.common.networking.BuildingRequest
import dev.voroscsoki.stratopolis.common.networking.NodeRequest
import dev.voroscsoki.stratopolis.common.networking.SimulationStartRequest
import dev.voroscsoki.stratopolis.common.util.Vec3
import dev.voroscsoki.stratopolis.common.util.getMemoryUsage
import kotlinx.coroutines.runBlocking

class UtilInput(val scene: MainScene) : InputAdapter() {
    override fun keyDown(keycode: Int): Boolean {
        println("Key down: $keycode")
        //F1
        if (keycode == 131) {
            runBlocking {
                Main.socket.sendSocketMessage(NodeRequest(Vec3(47.4979f, 0f, 19.0402f))) }
        }
        if (keycode == 132) {
            runBlocking {
                val source = scene.cam.position?.toWorldCoords(scene.baselineCoord)!!.copy(y = 0f)
                Main.socket.sendSocketMessage(BuildingRequest(source, 0.15f)) }
        }
        if (keycode == 133) {
            runBlocking {
                Main.socket.sendSocketMessage(SimulationStartRequest()) }
        }

        //print app memory usage
        println("Memory usage: ${getMemoryUsage()} MB")
        return super.keyDown(keycode)
    }

    override fun keyUp(keycode: Int): Boolean {
        return super.keyUp(keycode)
    }

    override fun keyTyped(character: Char): Boolean {
        return super.keyTyped(character)
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if(button == Input.Buttons.LEFT) {
            scene.pickBuildingRay(screenX, screenY)?.let {
                scene.showPopup(screenX, screenY, it.second)
            }
        }
        return super.touchDown(screenX, screenY, pointer, button)
    }
}