package com.anon404.burpdroid

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

// NOTE: UI interaction code is commented out due to environment limitations.
public class MainActivity : AppCompatActivity() {

    private var isIntercepting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(R.layout.activity_main)

        // val startButton = findViewById(R.id.startButton)
        // val logsButton = findViewById(R.id.logsButton)

        // startButton.setOnClickListener {
        //     isIntercepting = !isIntercepting
        //     toggleProxyService()
        //     // updateUI()
        // }
        //
        // logsButton.setOnClickListener {
        //     val intent = Intent(this, LogsActivity::class.java)
        //     startActivity(intent)
        // }
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
