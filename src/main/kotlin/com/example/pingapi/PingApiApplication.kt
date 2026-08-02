package com.example.pingapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
class PingApiApplication

fun main(args: Array<String>) {
    runApplication<PingApiApplication>(*args)
}

@RestController
class PingController {
    @GetMapping("/ping")
    fun ping(): ResponseEntity<Void> {
        return ResponseEntity.noContent().build() // 204 - 0 bytes
    }
}