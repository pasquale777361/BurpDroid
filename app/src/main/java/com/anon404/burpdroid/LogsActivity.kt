package com.anon404.burpdroid

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

public class LogsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logs)

        val recyclerView = findViewById<RecyclerView>(R.id.logsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val logs = RequestLogger.getLogs()
        val adapter = LogsAdapter(logs)
        recyclerView.adapter = adapter
    }
}
