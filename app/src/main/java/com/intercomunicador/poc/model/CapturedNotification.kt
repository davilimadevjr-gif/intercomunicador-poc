package com.intercomunicador.poc.model

import android.app.PendingIntent
import android.app.RemoteInput

/**
 * Modelo de dados representando uma notificação capturada do WhatsApp.
 */
data class CapturedNotification(
    val id: String,
    val packageName: String,
    val senderName: String,
    val messageText: String,
    val timestamp: Long,
    val replyAction: PendingIntent? = null,
    val remoteInput: RemoteInput? = null
)
