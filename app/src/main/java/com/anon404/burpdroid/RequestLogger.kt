package com.anon404.burpdroid

import java.util.Collections

object RequestLogger {
    val logs: MutableList<String> = Collections.synchronizedList(mutableListOf<String>())

    fun addLog(log: String) {
        logs.add(log)
    }

    fun getLogs(): List<String> {
        return logs.toList()
    }

    fun clearLogs() {
        logs.clear()
    }
}
