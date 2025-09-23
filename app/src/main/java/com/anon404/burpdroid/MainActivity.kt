package com.anon404.burpdroid

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import android.widget.Button
import android.widget.TextView

public class MainActivity : AppCompatActivity() {

    private lateinit var startButton: Button
    private lateinit var logsButton: Button
    private lateinit var statusText: TextView
    private var isIntercepting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startButton = findViewById(R.id.startButton)
        logsButton = findViewById(R.id.logsButton)
        statusText = findViewById(R.id.statusText)

        startButton.setOnClickListener {
            isIntercepting = !isIntercepting
            toggleProxyService()
            updateUI()
        }

        logsButton.setOnClickListener {
            val intent = Intent(this, LogsActivity::class.java)
            startActivity(intent)
        }

        updateUI()
    }

    private fun updateUI() {
        if (isIntercepting) {
            startButton.text = "Stop Intercept"
            statusText.text = "Status: Running"
        } else {
            startButton.text = "Start Intercept"
            statusText.text = "Status: Idle"
        }
    }

    private fun toggleProxyService() {
        val intent = Intent(this, ProxyService::class.java)
        if (isIntercepting) {
            startService(intent)
        } else {
            stopService(intent)
        }
    }
}
