package com.hs2t.tinhtinh

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationListener : NotificationListenerService(){
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate() {
        super.onCreate()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // 1. Lọc thông báo từ MyVIB (com.vib.vibmplus hoặc com.vib.myvib2)
        val targetPackages = listOf("com.vib.vibmplus", "com.vib.myvib2", "com.mbmobile")
        
        if (targetPackages.contains(sbn.packageName)) {
            val extras = sbn.notification.extras
            val title = extras.getString("android.title")
            val text = extras.getString("android.text")

            // Kiểm tra tiêu đề hoặc nội dung thông báo VIB (Thường chứa biến động số dư)
            if(text != null){
                Log.d("TINHTINH", "Nhận thông báo: $text")

                // 2. Regex mới cho VIB: Bắt số tiền sau dấu + và nội dung sau chữ ND:
                // Cấu trúc VIB: GD +100,000VND ... ND: Noi dung chuyen tien
                val regex = Regex("""\+([\d,.]+)VND.*ND:\s*(.*)""", RegexOption.IGNORE_CASE)
                val matchResult = regex.find(text)

                if (matchResult != null) {
                    // 3. Làm sạch số tiền (Xóa dấu phẩy/chấm) để Home Assistant hiểu là con số
                    val rawAmount = matchResult.groupValues[1]
                    val amount = rawAmount.replace(",", "").replace(".", "")
                    
                    val datetime = "" // VIB thường không để datetime cố định như MB, để trống hoặc lấy giờ hệ thống
                    val memo = matchResult.groupValues[2].trim()

                    Log.d("TINHTINH", "Sửa thành công: Số tiền $amount - Nội dung: $memo")

                    // Gửi thông báo qua Broadcast
                    val intent = Intent("com.hs2t.tinhtinh.NOTIFICATION_POSTED")
                    intent.putExtra("amount", amount)
                    intent.putExtra("datetime", datetime)
                    intent.putExtra("memo", memo)
                    sendBroadcast(intent)
                    
                    playRingtone()
                } else {
                    Log.e("TINHTINH", "Không tìm thấy cấu trúc tiền VIB trong: $text")
                }
            }
        }
    }

    private fun playRingtone() {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                0
            )

            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(this, R.raw.bel)
                mediaPlayer?.setOnCompletionListener {
                    stopRingtone()
                }
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            Log.e("MyNotificationListener", "Error playing ringtone: ${e.message}")
        }
    }

    private fun stopRingtone() {
        if (mediaPlayer != null) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRingtone()
    }
}
