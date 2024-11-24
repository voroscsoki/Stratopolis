package dev.voroscsoki.stratopolis.client.user_interface

import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import dev.voroscsoki.stratopolis.client.Main
import dev.voroscsoki.stratopolis.client.graphics.MainScene
import dev.voroscsoki.stratopolis.common.networking.RoadRequest
import dev.voroscsoki.stratopolis.common.networking.SimulationStartRequest
import dev.voroscsoki.stratopolis.common.util.getMemoryUsage
import kotlinx.coroutines.runBlocking

class UtilInput(val scene: MainScene) : InputAdapter() {
    override fun keyDown(keycode: Int): Boolean {
        println("Key down: $keycode")
        //F2
        if (keycode == 132) {
            runBlocking {
                Main.socket.sendSocketMessage(RoadRequest(Main.instanceData.baselineCoord)) }
        }
        //F3
        if (keycode == 133) {
            runBlocking {
                Main.socket.sendSocketMessage(SimulationStartRequest) }
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