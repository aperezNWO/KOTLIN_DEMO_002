package com.example.pingapi.DAO

import com.example.pingapi.DAO.entity.AccessLog

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

import org.springframework.stereotype.Repository

@Repository
class AccessLogDAO {

    private val jdbcURL = "jdbc:sqlserver://webapiangulardemo.mssql.somee.com:1433;databaseName=webapiangulardemo;encrypt=false"
    private val jdbcUsername = "aperezNWO_SQLLogin_1"
    private val jdbcPassword = "aperezNWO_SQLLogin_1"

    @Throws(SQLException::class)
    private fun getConnection(): Connection {
        return DriverManager.getConnection(jdbcURL, jdbcUsername, jdbcPassword)
    }

    @Throws(SQLException::class)
    fun getAllLogs(): List<AccessLog> {
        val sql = """
            SELECT TOP 100
                   AL.[ID_column]     AS id_column
                 , AL.[PageName]      AS pageName
                 , AL.[AccessDate]    AS accessDate
                 , AL.[IpValue]       AS ipValue
            FROM
                dbo.accessLogs AL
            WHERE
                AL.[LogType] = 1
            AND
                (AL.PAGENAME LIKE '%DEMO%'
            and
                AL.PAGENAME LIKE '%PAGE%')
            AND
                AL.PAGENAME NOT LIKE '%ERROR%'
            AND
                AL.PAGENAME  NOT LIKE '%PAGE_DEMO_INDEX%'
            AND
                UPPER(AL.PAGENAME) NOT LIKE '%CACHE%'
            AND
                AL.IPVALUE <> '::1'
            order by
                AL.[ID_column] desc
        """.trimIndent()

        val accessLogs = mutableListOf<AccessLog>()
        
        getConnection().use { connection ->
            connection.prepareStatement(sql).use { preparedStatement ->
                preparedStatement.executeQuery().use { rs ->
                    while (rs.next()) {
                        val idColumn = rs.getLong("id_column")
                        val pageName = rs.getString("pageName")
                        val accessDate = rs.getString("accessDate")
                        val ipValue = rs.getString("ipValue")
                        
                        accessLogs.add(AccessLog(idColumn, pageName, accessDate, ipValue))
                    }
                }
            }
        }
        return accessLogs
    }
}
