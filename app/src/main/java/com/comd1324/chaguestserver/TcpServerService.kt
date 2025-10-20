package com.comd1324.chaguestserver

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.*
import java.net.ServerSocket
import java.net.Socket

class TcpServerService : Service() {

    private var serverThread: Thread? = null
    private var running = true

    override fun onCreate() {
        super.onCreate()
        startForeground(1, createNotification("TCP 서버 실행 중"))
        startServer(1324)
    }

    private fun createNotification(content: String): Notification {
        val channelId = "tcp_server_channel"
        val channelName = "TCP Server"

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Android 8.0 (API 26) 이상에서만 NotificationChannel 사용
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // 이미 채널이 있는지 확인 후 없으면 새로 생성
            if (notificationManager.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "TCP 서버 실행 상태를 표시합니다."
                    setShowBadge(false)
                    enableLights(false)
                    enableVibration(false)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }

        val builder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationCompat.Builder(this, channelId)
        } else {
            NotificationCompat.Builder(this)
        }

        return builder
            .setContentTitle("TCP 서버")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW) // 하위 버전에서 IMPORTANCE_LOW 대체
            .build()
    }


    private fun log(msg: String) {
        val intent = Intent("TCP_LOG").putExtra("log", msg)
        sendBroadcast(intent)
    }

    private fun startServer(port: Int) {
        serverThread = Thread {
            try {
                val serverSocket = ServerSocket(port)
                log("서버 시작됨 (포트 $port)")

                while (running) {
                    val client = serverSocket.accept()
                    handleClient(client)
                }

                serverSocket.close()
                log("서버 종료됨")

            } catch (e: Exception) {
                log("서버 오류: ${e.message}")
            }
        }
        serverThread?.start()
    }

    private fun handleClient(client: Socket) {
        Thread {
            val ip = client.inetAddress.hostAddress
            log("클라이언트 연결: $ip")

            val input = BufferedReader(InputStreamReader(client.getInputStream()))
            val outputStream = client.getOutputStream()

            // HTTP 요청 첫 줄 읽기
            val requestLine = input.readLine() ?: ""
            log("요청 수신: $requestLine")

            // 경로만 추출 (예: POST /service/inspection/check HTTP/1.1 -> /service/inspection/check)
            val path = requestLine.split(" ").getOrNull(1) ?: ""


            //서비스 상태 체크
            val responseBytes = when (path) {
                "/service/inspection/check" -> {
                    // JSON 한 줄로 생성
                    val resJson = """{"success":true}"""
                    val bodyBytes = resJson.toByteArray(Charsets.UTF_8)

                    // HTTP 200 OK 헤더 + 바디
                    val httpResponse = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: application/json; charset=utf-8\r\n" +
                            "Content-Length: ${bodyBytes.size}\r\n" +
                            "\r\n" +
                            resJson
                    httpResponse.toByteArray(Charsets.UTF_8)
                }
                //공지사항 체크
                "/service/notice/get" -> {
                    // 현재 서버 시간 (KitKat 호환)
                    val calendar = java.util.Calendar.getInstance()
                    val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                    val noticeTime = formatter.format(calendar.time)

                    val resJson = """{"success":true,"notice":"$noticeTime","noticeUrl":"https://google.com"}"""
                    val bodyBytes = resJson.toByteArray(Charsets.UTF_8)

                    val httpResponse = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: application/json; charset=utf-8\r\n" +
                            "Content-Length: ${bodyBytes.size}\r\n" +
                            "\r\n" +
                            resJson
                    httpResponse.toByteArray(Charsets.UTF_8)
                }
                "/user/info/get" -> {
                    // UserInfoDTO JSON 생성
                    val resJson = """{"success":true,"token":123456789,"info":{"tireCnt":1000000,"tireRemainSecs":0,"canPresent":false,"gold":1000000,"trophyCnt":1000000,"characterNo":2,"carNo":1,"carSeq":0,"carClass":"S","carAccel":0,"carSpeed":0,"carFuleCost":0,"maxDistance":0,"maxPoint":0,"maxCombo":0,"maxGold":0,"maxScore":0,"maxSpeed":0,"goldAmt":0,"goldUse":0,"playCount":0,"crashCount":0,"jumpCount":0,"item1Use":0,"item2Use":0,"item3Use":0,"item4Use":0,"item5Use":0,"item6Use":0,"item7Use":0,"totalDistance":0,"totalCombo":0,"maxSpeedCrashCount":0,"totalCarDestroyCount":0,"dormancyTireSendCnt":123,"friendInviteCnt":0,"missionsCount":0,"missions":[]}}"""
                    val bodyBytes = resJson.toByteArray(Charsets.UTF_8)

                    val httpResponse = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: application/json; charset=utf-8\r\n" +
                            "Content-Length: ${bodyBytes.size}\r\n" +
                            "\r\n" +
                            resJson
                    httpResponse.toByteArray(Charsets.UTF_8)
                }
                "ping" -> "pong".toByteArray(Charsets.UTF_8)
                else -> "알 수 없는 요청".toByteArray(Charsets.UTF_8)
            }

            // 응답 전송
            outputStream.write(responseBytes)
            outputStream.flush()
            log("응답 전송: ${String(responseBytes)}")

            client.close()
        }.start()
    }


    override fun onDestroy() {
        running = false
        serverThread?.interrupt()
        log("서비스 종료")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
