package com.comd1324.chaguestserver

import android.annotation.SuppressLint
import android.content.*
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var logView: TextView
    private lateinit var exitButton: Button
    private val logReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val log = intent?.getStringExtra("log") ?: return
            runOnUiThread {
                logView.append("$log\n")
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        logView = findViewById(R.id.log_view)
        exitButton = findViewById(R.id.exit_button)

        startService(Intent(this, TcpServerService::class.java))
        logView.append("서버 시작 중...\n")

        exitButton.setOnClickListener {
            stopService(Intent(this, TcpServerService::class.java))
            finishAffinity()
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(logReceiver, IntentFilter("TCP_LOG"), Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(logReceiver, IntentFilter("TCP_LOG"))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(logReceiver)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("logText", logView.text.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        logView.text = savedInstanceState.getString("logText", "")
    }
}

