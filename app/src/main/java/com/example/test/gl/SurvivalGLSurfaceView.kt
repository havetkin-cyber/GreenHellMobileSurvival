package com.example.test.gl

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import com.example.test.game.SurvivalGame

class SurvivalGLSurfaceView @JvmOverloads constructor(
    context: Context,
    val game: SurvivalGame,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    val renderer: SurvivalRenderer

    init {
        setEGLContextClientVersion(3)
        renderer = SurvivalRenderer(context, game)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}
