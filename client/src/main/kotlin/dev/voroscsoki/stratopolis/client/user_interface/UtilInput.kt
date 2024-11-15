package dev.voroscsoki.stratopolis.client.user_interface

import com.badlogic.gdx.InputAdapter
import dev.voroscsoki.stratopolis.client.Main
import dev.voroscsoki.stratopolis.common.networking.BuildingRequest
import dev.voroscsoki.stratopolis.common.networking.NodeRequest
import dev.voroscsoki.stratopolis.common.networking.SimulationStartRequest
import dev.voroscsoki.stratopolis.common.util.Vec3
import dev.voroscsoki.stratopolis.common.util.getMemoryUsage
import kotlinx.coroutines.runBlocking

class UtilInput : InputAdapter() {
    override fun keyDown(keycode: Int): Boolean {
        println("Key down: $keycode")
        //F1
        if (keycode == 131) {
            runBlocking {
                Main.socket.sendSocketMessage(NodeRequest(Vec3(47.4979, 0.0, 19.0402))) }
        }
        if (keycode == 132) {
            runBlocking {
                Main.socket.sendSocketMessage(BuildingRequest(Vec3(47.4979, 0.0, 19.0402))) }
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
}