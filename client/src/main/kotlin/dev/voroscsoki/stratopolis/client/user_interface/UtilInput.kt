package dev.voroscsoki.stratopolis.client.user_interface

import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import dev.voroscsoki.stratopolis.client.graphics.MainScene

class UtilInput(private val scene: MainScene) : InputAdapter() {
    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if(button == Input.Buttons.LEFT) {
            scene.pickBuildingRay(screenX, screenY)?.let {
                scene.showPopup(screenX, screenY, it.second)
            }
        }
        return super.touchDown(screenX, screenY, pointer, button)
    }
}