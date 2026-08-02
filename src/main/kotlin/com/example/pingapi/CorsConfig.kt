package com.example.pingapi

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@Configuration
class CorsConfig {

    @Bean
    fun corsFilter(): CorsFilter {
        val source = UrlBasedCorsConfigurationSource()
        val config = CorsConfiguration().apply {
            // Use setAllowedOriginPatterns instead of setAllowedOrigins if using credentials
            setAllowedOriginPatterns(listOf("*")) // Change to specific domains in production (e.g., listOf("https://your-frontend.com"))
            setAllowedMethods(listOf("GET", "POST", "PUT", "DELETE", "OPTIONS"))
            setAllowedHeaders(listOf("*"))
            setAllowCredentials(true)
        }
        source.registerCorsConfiguration("/**", config)
        return CorsFilter(source)
    }
}