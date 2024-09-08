package dev.voroscsoki.stratopolis.client

import com.badlogic.gdx.InputAdapter
import dev.voroscsoki.stratopolis.client.api.HttpAccessor
import kotlinx.coroutines.runBlocking

class MyInput : InputAdapter() {
    override fun keyDown(keycode: Int): Boolean {
        println("Key down: $keycode")
        if (keycode == 131) {
            runBlocking { HttpAccessor.testRequest().map {it.coords}.forEach { Main.appScene.addBuilding(it) } }
        }
        //print app memory usage
        println("Memory usage: ${(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024} MB")
        return super.keyDown(keycode)
    }

    override fun keyUp(keycode: Int): Boolean {
        return super.keyUp(keycode)
    }

    override fun keyTyped(character: Char): Boolean {
        return super.keyTyped(character)
    }
}