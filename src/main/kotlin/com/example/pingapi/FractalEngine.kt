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
    // GOLDEN RATIO / NAUTILUS — field-scan version
    //
    // Instead of tracing a spiral, we scan every screen pixel and evaluate an
    // implicit "shell field" that combines:
    //
    //   • a logarithmic-spiral phase   φ = θ - ln(r)/b
    //   • chamber bands drawn IN THAT PHASE (so they curve along the spiral,
    //     not radially)
    //   • a tube envelope that peaks near the spiral arms
    //   • a radial fade
    //
    // This is the same trick fractal-art tools use for shell-shaped formulas,
    // and it produces chambers, ribs, and an eye — not concentric rings.
    // ─────────────────────────────────────────────────────────────────────────

    fun generateGoldenRatio(bounds: Bounds, maxIterations: Int): List<FractalPoint> {
        val points = mutableListOf<FractalPoint>()

        val xRange  = bounds.xMax - bounds.xMin
        val yRange  = bounds.yMax - bounds.yMin
        val centerX = (bounds.xMin + bounds.xMax) / 2.0
        val centerY = (bounds.yMin + bounds.yMax) / 2.0
        val viewportScale = kotlin.math.min(xRange, yRange)

        // ── Nautilus constants ───────────────────────────────────────────────
        val PHI        = (1.0 + kotlin.math.sqrt(5.0)) / 2.0
        val growthRate = kotlin.math.ln(PHI) / (2.0 * Math.PI)   // ≈ 0.0766

        val maxRadius = viewportScale * 0.47
        val r0        = maxRadius / Math.pow(PHI, 4.0)           // ~4 turns visible

        // Chamber band count per whorl — 30 chambers per whorl, real nautilus.
        // Because bands are curved along the spiral, we can afford 30 without
        // the aliasing that plagued the "emit radial hairs" approach.
        val chambersPerWhorl = 30.0

        // The number of spiral ARMS. Real nautilus is 1 continuous tube, but
        // visually the shell reads as several overlapping turns of one tube.
        // We use ONE arm and rely on the phase term to produce the whorls.
        // (Higher = more arms = busier shell.)

        // Rib thickness as a fraction of the whorl pitch. Controls whether
        // the chambers look "wide" (small value) or "thin" (large value).
        val ribSharpness = 3.0

        // Radial falloff exponent
        val fadeStrength = 0.55

        // ── Pixel scan ───────────────────────────────────────────────────────
        // Two-pixel stride in each axis is enough for a smooth result and 4× faster.
        // Set to 1 for maximum detail if you don't mind 480k evaluations.
        val stride = 1

        for (py in 0 until CANVAS_HEIGHT step stride) {
            for (px in 0 until CANVAS_WIDTH step stride) {

                // Screen → world
                val wx = bounds.xMin + (px.toDouble() / CANVAS_WIDTH) * xRange
                val wy = bounds.yMin + (py.toDouble() / CANVAS_HEIGHT) * yRange

                // World → polar about the shell eye
                val dx = wx - centerX
                val dy = wy - centerY
                val r  = kotlin.math.sqrt(dx * dx + dy * dy)
                if (r < 1e-6 || r > maxRadius) continue

                val theta = kotlin.math.atan2(dy, dx)     // -π .. +π

                // ── Logarithmic-spiral phase ─────────────────────────────────
                // φ = θ - ln(r)/b
                // Points on the SAME spiral arm share the same φ (mod 2π).
                // This is what makes chamber bands curve along the spiral.
                val phi = theta - kotlin.math.ln(r / r0) / growthRate

                // ── Chamber bands, drawn in φ ────────────────────────────────
                // sin(chambersPerWhorl * φ) peaks at each chamber wall.
                // Use |sin| for symmetric ribs, or sin for one-sided blades.
                val chamberPhase = chambersPerWhorl * phi
                // We use a sharpened raised cosine so the walls read as
                // distinct ridges rather than a smooth wave.
                val chamber = Math.pow(
                    0.5 + 0.5 * kotlin.math.cos(chamberPhase),
                    ribSharpness
                )

                // ── Tube envelope ────────────────────────────────────────────
                // The tube is thickest at radius r where the spiral arm sits.
                // Because arms are the locus φ ≡ const, and a pixel is on an arm
                // when φ is a multiple of 2π/nTurns... actually every φ is on
                // SOME arm — the envelope here is the radial fade, since the
                // arm spacing already grows with r in the log-spiral.
                //
                // The visible "tube thickness" emerges from the fact that at
                // each pixel, the strength of its nearest chamber ridge is
                // controlled by how close φ is to a multiple of 2π/n.
                // That gives us a natural wall thickness without extra params.

                // ── Radial fade ──────────────────────────────────────────────
                val tNorm = (r / maxRadius).coerceIn(0.0, 1.0)
                val radialFade = 1.0 - fadeStrength * tNorm * tNorm

                // ── Combine into intensity ───────────────────────────────────
                // Body intensity: the shell's overall brightness at this radius
                val bodyI = 40.0 + 180.0 * radialFade

                // Ridge intensity: chamber walls brighten the pixel
                val ridgeI = bodyI * (0.35 + 0.65 * chamber)

                // The eye of the shell gets a small brightness boost so it
                // reads as a "hot spot" like in the reference.
                val eyeBoost = if (r < r0 * 1.4) {
                    60.0 * (1.0 - r / (r0 * 1.4))
                } else 0.0

                val intensity = (ridgeI + eyeBoost).toInt().coerceIn(0, 235)
                if (intensity < 10) continue       // skip near-black pixels

                points.add(FractalPoint(px.toDouble(), py.toDouble(), intensity))
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
