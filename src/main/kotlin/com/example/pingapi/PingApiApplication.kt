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
// ENDPOINT REGISTRY (Dictionary)
// ─────────────────────────────────────────────────────────────────────────────

object EndpointRegistry {
    const val HEALTH             = "/health"
    const val PING               = "/ping"
    const val LANGUAGE_VERSION   = "/api/system/language-version"
    const val SERVER_VERSION     = "/api/system/server-version"
    const val APP_VERSION        = "/api/system/app-version"
    const val FRACTAL_GENERATE   = "/api/fractals/generate"
    const val GET_ALL_LOGS       = "/api/data/getAllLogs"
    const val GET_ALL_PERSONS    = "/api/data/getAllPersons"
    const val DIJKSTRA           = "/api/algorithm/GenerateRandomVertex_Kotlin"

    data class EndpointInfo(val key: String, val path: String, val description: String)

    val infoMap = mapOf(
        "HEALTH_ENDPOINT"           to EndpointInfo("HEALTH_ENDPOINT"          , HEALTH,           "Print all endpoints"),
        "PING_ENDPOINT"             to EndpointInfo("PING_ENDPOINT"            , PING,             "Ping endpoint"),
        "LANGUAGE_VERSION_ENDPOINT" to EndpointInfo("LANGUAGE_VERSION_ENDPOINT", LANGUAGE_VERSION, "Get Kotlin Version"),
        "SERVER_VERSION_ENDPOINT"   to EndpointInfo("SERVER_VERSION_ENDPOINT"  , SERVER_VERSION,   "Get Spring Boot Version"),
        "APP_VERSION_ENDPOINT"      to EndpointInfo("APP_VERSION_ENDPOINT"     , APP_VERSION,      "Get Spring Boot App Version"),
        "FRACTAL_ENDPOINT"          to EndpointInfo("FRACTAL_ENDPOINT"         , FRACTAL_GENERATE, "Fractal Generation Endpoint"),
        "GET_ALL_LOGS_ENDPOINT"     to EndpointInfo("GET_ALL_LOGS_ENDPOINT"    , GET_ALL_LOGS,     "Get All Access Logs"),
        "GET_ALL_PERSONS_ENDPOINT"  to EndpointInfo("GET_ALL_PERSONS_ENDPOINT" , GET_ALL_PERSONS,  "Get All Persons"),
        "DIJKSTRA_ENDPOINT"         to EndpointInfo("DIJKSTRA_ENDPOINT"        , DIJKSTRA,         "Generate Random Vertex via Dijkstra")
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// HEALTH / DIAGNOSTICS CONTROLLER
// ─────────────────────────────────────────────────────────────────────────────
@RestController
class HealthController {

    @GetMapping(EndpointRegistry.HEALTH)
    fun health(): ResponseEntity<Map<String, Any>> {
        val springVersion = SpringBootVersion.getVersion()
        val serverStatus = "Kotlin - Spring Boot Http Server - v[$springVersion] Working!"

        val endpointsList = EndpointRegistry.infoMap.values.map { info ->
            mapOf(
                "key" to info.key,
                "path" to info.path,
                "description" to info.description
            )
        }

        return ResponseEntity.ok(
            mapOf(
                "server" to serverStatus,
                "endpoints" to endpointsList
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PING
// ─────────────────────────────────────────────────────────────────────────────

@RestController
class PingController {

    @GetMapping(EndpointRegistry.PING)
    fun ping(): ResponseEntity<Void> {
        return ResponseEntity.noContent().build() // 204 — 0 bytes
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SYSTEM VERSION
// ─────────────────────────────────────────────────────────────────────────────

@RestController
class SystemController {

    @GetMapping(EndpointRegistry.LANGUAGE_VERSION)
    fun getKotlinVersion(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(
            mapOf(
                "language" to "Kotlin",
                "version" to KotlinVersion.CURRENT.toString()
            )
        )
    }

    @GetMapping(EndpointRegistry.SERVER_VERSION)
    fun getFrameworkServerVersion(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(
            mapOf(
                "server" to "Spring Boot",
                "version" to (SpringBootVersion.getVersion())
            )
        )
    }

    @GetMapping(EndpointRegistry.APP_VERSION)
    fun getApplicationVersion(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(
            mapOf(
                "appVersion" to "1.0.0"
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

    @GetMapping(EndpointRegistry.FRACTAL_GENERATE)
    fun getFractal(
        @RequestParam kind: Int,
        @RequestParam(required = false) xMin: Double?,
        @RequestParam(required = false) xMax: Double?,
        @RequestParam(required = false) yMin: Double?,
        @RequestParam(required = false) yMax: Double?,
        @RequestParam(required = false) maxIterations: Int?
    ): ResponseEntity<List<FractalPoint>> {

        val fractalKind = FractalKind.fromValue(kind)

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

// ─────────────────────────────────────────────────────────────────────────────
// DIJKSTRA
// ─────────────────────────────────────────────────────────────────────────────

@RestController
class AlgorithmController {

    @GetMapping(EndpointRegistry.DIJKSTRA)
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
class DataController(
    private val accessLogDAO: AccessLogDAO,
    private val personasDAO: PersonasDAO
) {

    @GetMapping(EndpointRegistry.GET_ALL_LOGS)
    fun getAllLogs(): ResponseEntity<List<AccessLog>> {
        return try {
            ResponseEntity.ok(accessLogDAO.getAllLogs())
        } catch (e: SQLException) {
            e.printStackTrace()
            ResponseEntity.status(500).body(null)
        }
    }

    @GetMapping(EndpointRegistry.GET_ALL_PERSONS)
    fun getPersons(): ResponseEntity<List<PersonaTable>> {
        return try {
            ResponseEntity.ok(personasDAO.getAllPersons())
        } catch (e: SQLException) {
            e.printStackTrace()
            ResponseEntity.status(500).body(null)
        }
    }
}
