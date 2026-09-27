package com.example.pingapi

import com.example.pingapi.DAO.AccessLogDAO
import com.example.pingapi.DAO.PersonasDAO
import com.example.pingapi.DAO.entity.AccessLog
import com.example.pingapi.DAO.entity.PersonaTable
import com.example.pingapi.FractalEngine.Bounds
import com.example.pingapi.FractalEngine.FractalKind
import com.example.pingapi.FractalEngine.FractalPoint

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.SpringBootVersion
import java.sql.SQLException

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
        return ResponseEntity.noContent().build()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SYSTEM VERSION
// ─────────────────────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/system")
class SystemController {

    @GetMapping("/language-version")
    fun getKotlinVersion(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(
            mapOf(
                "language" to "Kotlin",
                "version" to KotlinVersion.CURRENT.toString()
            )
        )
    }

    @GetMapping("/server-version")
    fun getSpringBootVersion(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(
            mapOf(
                "server" to "Spring Boot",
                "version" to (SpringBootVersion.getVersion() ?: "unknown")
            )
        )
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

        // Configures appropriate default bounds depending on the requested fractal kind
        val defaultBounds = when (fractalKind) {
            FractalKind.MANDELBROT   -> Bounds(-2.0, 1.0, -1.2, 1.2)
            FractalKind.GOLDEN_RATIO -> Bounds(-2.0, 1.0, -1.2, 1.2)
            else                     -> Bounds(-1.5, 1.5, -1.5, 1.5)
        }

        val bounds = if (xMin != null && xMax != null && yMin != null && yMax != null)
            Bounds(xMin, xMax, yMin, yMax)
        else
            defaultBounds

        val iterations = maxIterations ?: 500

        val points = fractalEngine.getFractal(fractalKind, bounds, iterations)
        return ResponseEntity.ok(points)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DIJKSTRA
// ─────────────────────────────────────────────────────────────────────────────

@RestController
class AlgorithmController {

    @GetMapping("/GenerateRandomVertex_SpringBoot")
    fun generateRandomVertex(): String {
        val vertexSize   = 9
        val sampleSize   = 23
        val sourcePoint  = 0
        return try {
            AlgorithmManager.generateRandomPoints(vertexSize, sampleSize, sourcePoint)
        } catch (e: Exception) {
            e.message ?: "Unknown error"
        }
    }
}

@RestController
@RequestMapping("/api/data")
class DataController(
    private val accessLogDAO: AccessLogDAO,
    private val personasDAO: PersonasDAO
) {

    @GetMapping("/getAllLogs")
    fun getAllLogs(): ResponseEntity<List<AccessLog>> {
        return try {
            ResponseEntity.ok(accessLogDAO.getAllLogs())
        } catch (e: SQLException) {
            e.printStackTrace()
            ResponseEntity.status(500).body(null)
        }
    }

    @GetMapping("/getAllPersons")
    fun getPersons(): ResponseEntity<List<PersonaTable>> {
        return try {
            ResponseEntity.ok(personasDAO.getAllPersons())
        } catch (e: SQLException) {
            e.printStackTrace()
            ResponseEntity.status(500).body(null)
        }
    }
}
