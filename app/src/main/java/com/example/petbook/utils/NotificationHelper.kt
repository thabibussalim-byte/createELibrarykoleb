package com.example.petbook.utils

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.petbook.R
import com.example.petbook.data.local.datastore.SettingPreferences
import com.example.petbook.data.local.datastore.dataStore
import com.example.petbook.ui.main.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "petbook_notifications"
        const val CHANNEL_NAME = "Petbook Notifications"

        const val TARGET_HISTORY = "target_history"
        const val TARGET_CATALOG = "target_catalog"
    }

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi untuk aktivitas peminjaman buku"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(
        id: Int, 
        title: String, 
        message: String, 
        target: String? = null,
        extraId: Int = -1,
        imageUrl: String? = null
    ) {
        val pref = SettingPreferences.getInstance(context.dataStore)
        val isNotifEnabled = runBlocking { pref.getNotificationSetting().first() }
        
        if (!isNotifEnabled) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (target != null) {
                putExtra("target_page", target)
                putExtra("extra_id", extraId)
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            id, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val appLogo = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_logo_app_petbook1)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_logo_app_petbook_foreground)
            .setLargeIcon(appLogo)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(context)
                .asBitmap()
                .load(imageUrl)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        builder.setLargeIcon(resource) // Ganti icon besar jadi cover buku
                        builder.setStyle(NotificationCompat.BigPictureStyle()
                            .bigPicture(resource)
                            .bigLargeIcon(null as Bitmap?))
                        notificationManager.notify(id, builder.build())
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
        } else {
            notificationManager.notify(id, builder.build())
        }
    }
}
