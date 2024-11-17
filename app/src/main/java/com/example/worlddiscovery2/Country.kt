package com.example.worlddiscovery2

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color


data class Country(var code: String, var name: String,var latitude: Float,var longitude: Float)

class ColoredPath internal constructor(val color: Color, private val points: FloatArray, val size: Int) {
    operator fun plus(position: Offset): ColoredPath {
        val newPoints = if (2 * (size + 1) >= points.size) {
            points.copyOf(points.size * 2)
        } else {
            points
        }
        newPoints[2 * size] = position.x
        newPoints[2 * size + 1] = position.y
        return ColoredPath(color, newPoints, size+1)
    }

    operator fun get(index: Int): Offset =
        Offset(points[2*index], points[2*index+1])

    companion object {
        fun create(color: Color) = ColoredPath(color, FloatArray(32), 0)
    }
}

data class Sketch(val id: Long, val paths: List<ColoredPath> = listOf()) {

    operator fun plus(color: Color) =
        Sketch(id, paths + ColoredPath.create(color))

    operator fun plus(position: Offset) =
        Sketch(id, paths.subList(0, paths.size-1) + (paths.last() + position))

    companion object {
        private var SERIAL: Long = 0L
        fun createEmpty() = Sketch(SERIAL++)
    }
}
