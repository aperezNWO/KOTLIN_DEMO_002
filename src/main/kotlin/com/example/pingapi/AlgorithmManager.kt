package com.example.pingapi

import java.util.Random
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

object AlgorithmManager {

    // ─────────────────────────────────────────────────────────────────────────
    // ENTRY POINT
    // ─────────────────────────────────────────────────────────────────────────

    fun generateRandomPoints(vertexSize: Int, sampleSizeRaw: Int, sourcePoint: Int): String {

        val sampleSize = sampleSizeRaw - 2          // REMOVER EXTREMOS DE COORDENADAS

        val graph = Array(vertexSize) { IntArray(vertexSize) }

        val currentTimeMillis = System.currentTimeMillis()
        val randX = Random(currentTimeMillis / 2)
        val randY = Random(currentTimeMillis * 2)

        val vertexX = fisherYates(sampleSize, randX)
        val vertexY = fisherYates(sampleSize, randY)

        val vertexArray = mutableListOf<String>()
        for (index in 0 until vertexSize) {
            val separator = if (index < vertexSize - 1) "|" else ""
            vertexArray.add("[${vertexX[index]},${vertexY[index]}]$separator")
        }

        val vertexArrayString = vertexArray.joinToString("")

        val separator2       = "■"
        val vertexMatrix     = generateRandomMatrix(vertexArray, graph, vertexSize)
        val vertexList       = dijkstra(vertexArray, graph, vertexSize, sampleSize, sourcePoint)

        val sortedListEncoded = vertexList.replace(",", "<br/>").replace("\t", "&nbsp;")

        return "$vertexArrayString$separator2$vertexMatrix$separator2$sortedListEncoded"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RANDOM ADJACENCY MATRIX
    // ─────────────────────────────────────────────────────────────────────────

    fun generateRandomMatrix(vertexString: List<String>, graph: Array<IntArray>, vertexSize: Int): String {

        // Diagonal = 0
        for (index in 0 until vertexSize) {
            graph[index][index] = 0
        }

        val rnd = Random(System.currentTimeMillis() % 1000)

        // Assign random edges (upper triangle, mirrored)
        for (indexX in 0 until vertexSize) {
            for (indexY in (indexX + 1) until vertexSize) {
                var randomValue = rnd.nextInt(2).toDouble()
                if (randomValue == 1.0) {
                    randomValue = getHipotemuza(vertexString, indexX, indexY)
                }
                graph[indexX][indexY] = randomValue.toInt()
                graph[indexY][indexX] = randomValue.toInt()
            }
        }

        // Guarantee connectivity — if a vertex has no edges at all, connect
        // its last zero-neighbour with the Euclidean distance weight
        for (indexX in 0 until vertexSize) {
            var zeroCount = 0
            for (indexY in 0 until vertexSize) {
                if (indexX != indexY && graph[indexX][indexY] == 0) {
                    zeroCount++
                    if (zeroCount == vertexSize - 1) {
                        val hipotemuza = getHipotemuza(vertexString, indexX, indexY).toInt()
                        graph[indexX][indexY] = hipotemuza
                        graph[indexY][indexX] = hipotemuza
                    }
                }
            }
        }

        // Serialise matrix to string: {a,b,c}|{d,e,f}|…
        val sb = StringBuilder()
        for (indexX in 0 until vertexSize) {
            val separator1  = if (indexX < vertexSize - 1) "|" else ""
            val rowValues   = (0 until vertexSize).joinToString(",") { indexY ->
                "${graph[indexX][indexY]}"
            }
            sb.append("{$rowValues}$separator1")
        }
        return sb.toString()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EUCLIDEAN DISTANCE (hypotenuse)
    // ─────────────────────────────────────────────────────────────────────────

    private fun getHipotemuza(vertexString: List<String>, indexX: Int, indexY: Int): Double {
        val coordSource = vertexString[indexY].replace(Regex("[|\\[\\]]"), "").split(",")
        val coordDest   = vertexString[indexX].replace(Regex("[|\\[\\]]"), "").split(",")

        val sourceX = coordSource[0].toDouble()
        val sourceY = coordSource[1].toDouble()
        val destX   = coordDest[0].toDouble()
        val destY   = coordDest[1].toDouble()

        return pythagorean(abs(destX - sourceX), abs(destY - sourceY))
    }

    private fun pythagorean(coordX: Double, coordY: Double): Double =
        sqrt(coordX.pow(2) + coordY.pow(2))

    // ─────────────────────────────────────────────────────────────────────────
    // FISHER-YATES SHUFFLE
    // ─────────────────────────────────────────────────────────────────────────

    fun fisherYates(count: Int, rand: Random): IntArray {
        val deck = IntArray(count) { it + 1 }           // [1, 2, … count]

        // First pass (forward)
        for (i in 0..count - 2) {
            val j = rand.nextInt(count - i)
            if (j > 0) {
                val tmp  = deck[i]
                deck[i]  = deck[i + j]
                deck[i + j] = tmp
            }
        }

        // Second pass (backward)
        for (i in count - 1 downTo 1) {
            val j = rand.nextInt(i + 1)
            if (j != i) {
                val tmp  = deck[i]
                deck[i]  = deck[j]
                deck[j]  = tmp
            }
        }

        return deck
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DIJKSTRA RUNNER
    // ─────────────────────────────────────────────────────────────────────────

    fun dijkstra(
        vertex      : List<String>,
        graph       : Array<IntArray>,
        vertexSize  : Int,
        sampleSize  : Int,
        sourcePoint : Int
    ): String {
        val gfg = Gfg()
        gfg.dijkstra(graph, sourcePoint, vertexSize)

        val sb = StringBuilder()
        for (index in gfg.dist.indices) {
            // Clamp unreachable nodes (Integer.MAX_VALUE) to 0
            if (gfg.dist[index] >= Int.MAX_VALUE) gfg.dist[index] = 0

            // After (Kotlin correct)
            val separator = if (index < gfg.dist.size - 1) "," else ""

            sb.append(
                "%02d<%s>-%02d-%s%s".format(
                    index,                                                   // 1. 01
                    vertex[index].replace(",", ";").replace("|", ""),        // 2. [6;8]
                    gfg.dist[index],                                         // 3. 41
                    gfg.path[index].replace(",", ";"),                       // 4. [0;3]≡[3;7]≡…
                    separator                                                 // 5. ,
                )
            )

            /*  Expected format:
                00<[5;19]>-00-<br/>
                01<[6;8]>-41-[0;3]≡[3;7]≡[7;6]≡[6;2]≡[2;1]≡<br/>
                …
            */
        }
        return sb.toString()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GFG — Dijkstra core (translated from the Java GFG helper class)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Self-contained Dijkstra implementation.
     * After calling [dijkstra]:
     *   [dist] — shortest distance from source to each vertex (Int.MAX_VALUE = unreachable)
     *   [path] — human-readable path string per vertex, e.g. "[0;3]≡[3;7]≡[7;6]≡"
     */
    private class Gfg {

        lateinit var dist: IntArray
        lateinit var path: MutableList<String>

        fun dijkstra(graph: Array<IntArray>, src: Int, vertexSize: Int) {

            dist = IntArray(vertexSize) { Int.MAX_VALUE }
            path = MutableList(vertexSize) { "" }

            val visited  = BooleanArray(vertexSize)
            val previous = IntArray(vertexSize) { -1 }

            dist[src] = 0

            repeat(vertexSize) {

                // Pick the unvisited vertex with the smallest known distance
                val u = (0 until vertexSize)
                    .filter { !visited[it] }
                    .minByOrNull { dist[it] } ?: return@repeat

                visited[u] = true

                // Relax neighbours
                for (v in 0 until vertexSize) {
                    val weight = graph[u][v]
                    if (!visited[v] && weight > 0 && dist[u] != Int.MAX_VALUE) {
                        val newDist = dist[u] + weight
                        if (newDist < dist[v]) {
                            dist[v]     = newDist
                            previous[v] = u
                        }
                    }
                }
            }

            // Reconstruct path strings
            for (v in 0 until vertexSize) {
                path[v] = buildPathString(previous, src, v)
            }
        }

        /**
         * Walks [previous] back from [dest] to [src] and builds the
         * "≡"-separated path string that matches the Java output format.
         * Example: "[0;3]≡[3;7]≡[7;6]≡"
         */
        private fun buildPathString(previous: IntArray, src: Int, dest: Int): String {
            if (dest == src) return ""

            val steps = mutableListOf<Int>()
            var cur   = dest
            while (cur != -1) {
                steps.add(cur)
                cur = previous[cur]
            }
            steps.reverse()                           // src → … → dest

            if (steps.first() != src) return ""      // unreachable

            val sb = StringBuilder()
            for (i in 0 until steps.size - 1) {
                sb.append("[${steps[i]};${steps[i + 1]}]≡")
            }
            return sb.toString()
        }
    }
}
