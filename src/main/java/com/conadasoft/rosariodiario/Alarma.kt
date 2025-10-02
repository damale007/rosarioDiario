package com.conadasoft.rosariodiario

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat

class Alarma : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val sharedPreferences = context.getSharedPreferences("mis_preferencias", Context.MODE_PRIVATE)

        if (sharedPreferences.getBoolean("tarea_programada", true)) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val intentCB = Intent(context, MainActivity::class.java)
            intentCB.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            val flags = if (Build.VERSION.SDK_INT >= 30) {
                PendingIntent.FLAG_IMMUTABLE
            } else PendingIntent.FLAG_UPDATE_CURRENT

            val contentIntent = PendingIntent.getActivity(context, 0, intentCB, flags)

            val channelId = "RosarioDiario_Channel"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val mChannel = NotificationChannel(channelId, "RosarioDiario Channel", importance)
            mChannel.enableLights(true)
            mChannel.description = "Canal de Notificaciones de RosarioDiario"
            mChannel.lightColor = Color.BLUE
            mChannel.enableVibration(true)
            mChannel.vibrationPattern = longArrayOf(1000, 500, 1000)
            notificationManager.createNotificationChannel(mChannel)

            val drawable = AppCompatResources.getDrawable(context, R.drawable.rosario)
            var bitmap: Bitmap? = null

            if (drawable is BitmapDrawable) {
                bitmap = drawable.bitmap
                // Ahora tienes el Bitmap 'bitmap'
            }

            val mBuilder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.notificacion)
                .setLargeIcon(bitmap)
                .setContentTitle("Rosario diario")
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setContentText("Es hora de rezar el Rosario Diario")

            notificationManager.notify((Math.random() * 3000 + 1).toInt(), mBuilder.build())
        }
    }
}