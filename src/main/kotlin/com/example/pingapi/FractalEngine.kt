package com.example.pingapi

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import kotlin.math.round
import kotlin.random.Random

class FractalEngine {

    // ─────────────────────────────────────────────────────────────────────────
    // FRACTAL KIND ENUM
    // ─────────────────────────────────────────────────────────────────────────

    enum class FractalKind(private val code: Int) {
        MANDELBROT(1),
        JULIA(2),
        LEAF(3),
        GOLDEN_RATIO(4);

        @JsonValue
        fun getValue(): Int = code

        companion object {
            @JvmStatic
            @JsonCreator
            fun fromValue(value: Int): FractalKind =
                entries.firstOrNull { it.code == value }
                    ?: throw IllegalArgumentException("Tipo de fractal inválido: $value")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SHARED TYPES
    // ─────────────────────────────────────────────────────────────────────────

    data class FractalPoint(val x: Double, val y: Double, val intensity: Int)

    data class Bounds(
        val xMin: Double,
        val xMax: Double,
        val yMin: Double,
        val yMax: Double
    )

    // ─────────────────────────────────────────────────────────────────────────
    // ROUTER
    // ─────────────────────────────────────────────────────────────────────────

    fun getFractal(
        fractalKind: FractalKind,
        bounds: Bounds,
        maxIterations: Int
    ): List<FractalPoint> = when (fractalKind) {
        FractalKind.MANDELBROT -> generateMandelbrot(bounds, maxIterations)
        FractalKind.JULIA -> generateJulia(bounds, maxIterations)
        FractalKind.LEAF -> generateLeaf()
        FractalKind.GOLDEN_RATIO -> generateGoldenRatio(bounds, maxIterations) // Routed here
    }

    private fun encodeIntensity(iter: Int, maxIterations: Int): Int =
        if (iter == maxIterations) 0 else (iter * 255 / maxIterations)

    // ─────────────────────────────────────────────────────────────────────────
    // MANDELBROT
    // ─────────────────────────────────────────────────────────────────────────

    fun generateMandelbrot(bounds: Bounds, maxIterations: Int): List<FractalPoint> {
        val points = mutableListOf<FractalPoint>()
        val xRange = bounds.xMax - bounds.xMin
        val yRange = bounds.yMax - bounds.yMin

        for (screenY in 0 until CANVAS_HEIGHT) {
            for (screenX in 0 until CANVAS_WIDTH) {

                val cRe = bounds.xMin + (screenX * xRange / CANVAS_WIDTH)
                val cIm = bounds.yMin + (screenY * yRange / CANVAS_HEIGHT)

                var zRe = 0.0
                var zIm = 0.0
                var iter = 0

                while (zRe * zRe + zIm * zIm <= 4.0 && iter < maxIterations) {
                    val nextRe = zRe * zRe - zIm * zIm + cRe
                    val nextIm = 2.0 * zRe * zIm + cIm
                    zRe = nextRe
                    zIm = nextIm
                    iter++
                }

                points.add(FractalPoint(screenX.toDouble(), screenY.toDouble(), encodeIntensity(iter, maxIterations)))
            }
        }

        return points
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JULIA
    // ─────────────────────────────────────────────────────────────────────────

    fun generateJulia(bounds: Bounds, maxIterations: Int): List<FractalPoint> {
        val points = mutableListOf<FractalPoint>()
        val xRange = bounds.xMax - bounds.xMin
        val yRange = bounds.yMax - bounds.yMin

        val cRe = -0.400
        val cIm = 0.600

        for (screenY in 0 until CANVAS_HEIGHT) {
            for (screenX in 0 until CANVAS_WIDTH) {

                var zRe = bounds.xMin + (screenX * xRange / CANVAS_WIDTH)
                var zIm = bounds.yMin + (screenY * yRange / CANVAS_HEIGHT)
                var iter = 0

                while (zRe * zRe + zIm * zIm <= 4.0 && iter < maxIterations) {
                    val nextRe = zRe * zRe - zIm * zIm + cRe
                    val nextIm = 2.0 * zRe * zIm + cIm
                    zRe = nextRe
                    zIm = nextIm
                    iter++
                }

                points.add(FractalPoint(screenX.toDouble(), screenY.toDouble(), encodeIntensity(iter, maxIterations)))
            }
        }

        return points
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GOLDEN RATIO FRACTAL
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generates the Golden Ratio Fractal on a CANVAS_WIDTH × CANVAS_HEIGHT grid.
     * Formula: z(n+1) = z(n)² + c - (1 / φ), where φ is the golden ratio.
     */
    fun generateGoldenRatio(bounds: Bounds, maxIterations: Int): List<FractalPoint> {
        val points = mutableListOf<FractalPoint>()
        val xRange = bounds.xMax - bounds.xMin
        val yRange = bounds.yMax - bounds.yMin

        // Golden ratio phi = (1 + sqrt(5)) / 2
        val phi = (1.0 + kotlin.math.sqrt(5.0)) / 2.0
        val phiOffset = 1.0 / phi

        for (screenY in 0 until CANVAS_HEIGHT) {
            for (screenX in 0 until CANVAS_WIDTH) {

                val cRe = bounds.xMin + (screenX * xRange / CANVAS_WIDTH)
                val cIm = bounds.yMin + (screenY * yRange / CANVAS_HEIGHT)

                var zRe = 0.0
                var zIm = 0.0
                var iter = 0

                while (zRe * zRe + zIm * zIm <= 4.0 && iter < maxIterations) {
                    val nextRe = zRe * zRe - zIm * zIm + cRe - phiOffset
                    val nextIm = 2.0 * zRe * zIm + cIm
                    zRe = nextRe
                    zIm = nextIm
                    iter++
                }

                points.add(FractalPoint(screenX.toDouble(), screenY.toDouble(), encodeIntensity(iter, maxIterations)))
            }
        }

        return points
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BARNSLEY FERN
    // ─────────────────────────────────────────────────────────────────────────

    fun generateLeaf(): List<FractalPoint> {
        val points = mutableListOf<FractalPoint>()
        val pixelGrid = Array(CANVAS_WIDTH) { IntArray(CANVAS_HEIGHT) }

        var x = 0.0
        var y = 0.0
        val rand = Random.Default
        val totalPoints = 150_000

        repeat(totalPoints) {
            val nextX: Double
            val nextY: Double
            val r = rand.nextInt(100)

            when {
                r < 1 -> {
                    nextX = 0.0; nextY = 0.16 * y
                }

                r < 86 -> {
                    nextX = 0.85 * x + 0.04 * y; nextY = -0.04 * x + 0.85 * y + 1.6
                }

                r < 93 -> {
                    nextX = 0.20 * x - 0.26 * y; nextY = 0.23 * x + 0.22 * y + 1.6
                }

                else -> {
                    nextX = -0.15 * x + 0.28 * y; nextY = 0.26 * x + 0.24 * y + 0.44
                }
            }

            x = nextX
            y = nextY

            val screenX = round((x + 2.182) * (CANVAS_WIDTH - 1) / (2.655 + 2.182)).toInt()
            val screenY = round((9.96 - y) * (CANVAS_HEIGHT - 1) / 9.96).toInt()

            if (screenX in 0 until CANVAS_WIDTH && screenY in 0 until CANVAS_HEIGHT) {
                pixelGrid[screenX][screenY] = 200
            }
        }

        for (px in 0 until CANVAS_WIDTH) {
            for (py in 0 until CANVAS_HEIGHT) {
                if (pixelGrid[px][py] > 0) {
                    points.add(FractalPoint(px.toDouble(), py.toDouble(), pixelGrid[px][py]))
                }
            }
        }

        return points
    }

    companion object {
        const val CANVAS_WIDTH = 800
        const val CANVAS_HEIGHT = 600
    }
}
