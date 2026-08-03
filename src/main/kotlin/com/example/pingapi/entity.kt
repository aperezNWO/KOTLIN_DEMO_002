package com.example.entity

data class AccessLog(
    val id_column: Long,
    val pageName: String?,
    val accessDate: String?,
    val ipValue: String?
)

data class PersonaTable(
    val id_column: Long,
    val ciudad: String?,
    val nombreCompleto: String?
)