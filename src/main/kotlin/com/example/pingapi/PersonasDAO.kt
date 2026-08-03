package com.example.DAO

import com.example.entity.PersonaTable
import org.springframework.stereotype.Repository
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

@Repository
class PersonasDAO {
    private val jdbcURL = "jdbc:sqlserver://webapiangulardemo.mssql.somee.com:1433;databaseName=webapiangulardemo;encrypt=false"
    private val jdbcUsername = "aperezNWO_SQLLogin_1"
    private val jdbcPassword = "aperezNWO_SQLLogin_1"

    @Throws(SQLException::class)
    private fun getConnection(): Connection {
        return DriverManager.getConnection(jdbcURL, jdbcUsername, jdbcPassword)
    }

    @Throws(SQLException::class)
    fun getAllPersons(): List<PersonaTable> {
        val sql = """
            SELECT
                [Id_Column]            AS id_column
                ,[Ciudad]              AS ciudad
                ,[NombreCompleto]      AS nombreCompleto
            FROM
                [dbo].[Persona]
            ORDER BY
                Id_Column   asc
        """.trimIndent()

        val personas = mutableListOf<PersonaTable>()
        
        getConnection().use { connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.executeQuery().use { rs ->
                    while (rs.next()) {
                        val idColumn = rs.getLong("id_column")
                        val ciudad = rs.getString("ciudad")
                        val nombreCompleto = rs.getString("nombreCompleto")

                        personas.add(PersonaTable(idColumn, ciudad, nombreCompleto))
                    }
                }
            }
        }
        return personas
    }
}