package com.example.pingapi

import com.example.pingapi.FractalEngine.Bounds
import com.example.pingapi.FractalEngine.FractalKind
import com.example.pingapi.FractalEngine.FractalPoint
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
class PingApiApplication

fun main(args: Array<String>) {
    runApplication<PingApiApplication>(*args)
}

// ─────────────────────────────────────────────────────────────────────────────
// PING
// ─────────────────────────────────────────────────────────────────────────────

@RestController
class PingController {

    @GetMapping("/ping")
    fun ping(): ResponseEntity<Void> {
        return ResponseEntity.noContent().build() // 204 — 0 bytes
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FRACTALS
// ─────────────────────────────────────────────────────────────────────────────

@RestController
class FractalController {

    private val fractalEngine = FractalEngine()

    /**
     * Generates a fractal and returns a JSON point array consumed by the
     * Angular _fetchAndRender pipeline.
     *
     * Parameters
     * ──────────
     * kind          — 1=Mandelbrot  2=Julia  3=Barnsley Fern
     * xMin/xMax     — complex-plane Re bounds of the current view window.
     * yMin/yMax     — complex-plane Im bounds of the current view window.
     *                 Sent directly by Angular's applyZoomToBounds(). Falls
     *                 back to each fractal's default view when omitted.
     * maxIterations — escape-time iteration ceiling (default 500).
     *                 Ignored for Barnsley Fern (kind=3).
     *
     * Examples
     * ────────
     * Default unzoomed Mandelbrot:
     *   /api/fractals/generate?kind=1
     *
     * Zoomed Julia view:
     *   /api/fractals/generate?kind=2&xMin=-0.7&xMax=0.3&yMin=-0.2&yMax=0.8&maxIterations=500
     */
    @GetMapping("/api/fractals/generate")
    fun getFractal(
        @RequestParam kind: Int,
        @RequestParam(required = false) xMin: Double?,
        @RequestParam(required = false) xMax: Double?,
        @RequestParam(required = false) yMin: Double?,
        @RequestParam(required = false) yMax: Double?,
        @RequestParam(required = false) maxIterations: Int?
    ): ResponseEntity<List<FractalPoint>> {

        val fractalKind = FractalKind.fromValue(kind)

        // Default bounds per fractal type — must match Angular's
        // DEFAULT_BOUNDS_MANDELBROT / DEFAULT_BOUNDS_JULIA constants.
        val defaultBounds = if (fractalKind == FractalKind.MANDELBROT)
            Bounds(-2.0, 1.0, -1.2, 1.2)
        else
            Bounds(-1.5, 1.5, -1.5, 1.5)

        val bounds = if (xMin != null && xMax != null && yMin != null && yMax != null)
            Bounds(xMin, xMax, yMin, yMax)
        else
            defaultBounds

        val iterations = maxIterations ?: 500

        val points = fractalEngine.getFractal(fractalKind, bounds, iterations)
        return ResponseEntity.ok(points)
    }
}
