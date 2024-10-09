package dev.voroscsoki.stratopolis.client.util

import com.badlogic.gdx.math.Vector3

class VertexSorter {
    companion object {
        fun sort(vertices: List<Vector3>, normal: Vector3): List<Vector3> {
            val centerPoint = pointAverage(vertices)
            val axis1 = normal.rotate(Vector3.Y, 90f)
            val axis2 = normal.crs(axis1)

            val vertexMap = vertices.map { it to it.cpy().sub(centerPoint).nor() }
            val sorted = vertexMap.sortedBy { it.second.dot(axis1) }.sortedBy { it.second.dot(axis2) }
            return sorted.map { it.first }
        }

        private fun pointAverage(vertices: List<Vector3>): Vector3 {
            val x = vertices.map { it.x }.average().toFloat()
            val y = vertices.map { it.y }.average().toFloat()
            val z = vertices.map { it.z }.average().toFloat()
            return Vector3(x,y,z)
        }
    }
}