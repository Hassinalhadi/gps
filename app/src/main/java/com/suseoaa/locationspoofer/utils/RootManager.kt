package com.suseoaa.locationspoofer.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class RootManager {

    companion object {
        const val CATCH_LOG_FILE = "/data/local/tmp/locationspoofer_catch_log.jsonl"
    }

    suspend fun checkRootAccess(): Boolean = withContext(Dispatchers.IO) {
        executeCommand("id").contains("uid=0(root)")
    }

    suspend fun grantMockLocation(): Boolean = withContext(Dispatchers.IO) {
        val result =
            executeCommand("appops set com.suseoaa.locationspoofer android:mock_location allow")
        result != "ERROR"
    }

    suspend fun revokeMockLocation(): Boolean = withContext(Dispatchers.IO) {
        val result =
            executeCommand("appops set com.suseoaa.locationspoofer android:mock_location deny")
        result != "ERROR"
    }

    suspend fun initCatchLogFile(): Boolean = withContext(Dispatchers.IO) {
        val cmd = """
            touch $CATCH_LOG_FILE
            chmod 666 $CATCH_LOG_FILE
            chcon u:object_r:shell_data_file:s0 $CATCH_LOG_FILE
        """.trimIndent()
        executeCommand(cmd) != "ERROR"
    }

    suspend fun readCatchLogsRaw(): String = withContext(Dispatchers.IO) {
        val cmd = """
            if [ -f $CATCH_LOG_FILE ]; then
                tail -n 300 $CATCH_LOG_FILE
            fi
            logcat -d -s HS_CatchLog:I | grep -F '{"id":' | sed 's/.*HS_CatchLog[^:]*: //' | tail -n 200
        """.trimIndent()
        val res = executeCommand(cmd)
        if (res == "ERROR" || res == "SUCCESS") "" else res
    }

    suspend fun clearCatchLogs(): Boolean = withContext(Dispatchers.IO) {
        val cmd = """
            echo -n "" > $CATCH_LOG_FILE
            chmod 666 $CATCH_LOG_FILE
            chcon u:object_r:shell_data_file:s0 $CATCH_LOG_FILE
            logcat -c 2>/dev/null || true
        """.trimIndent()
        executeCommand(cmd) != "ERROR"
    }

    fun executeCommand(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            process.waitFor()
            output.ifEmpty { "SUCCESS" }
        } catch (e: Exception) {
            "ERROR"
        }
    }
}
