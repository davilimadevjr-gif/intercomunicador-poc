package com.intercomunicador.poc.service

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.intercomunicador.poc.model.CapturedNotification
import java.util.concurrent.CopyOnWriteArrayList

class WhatsAppNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "WA_Listener_PoC"
        
        // Lista em memória das últimas notificações capturadas para exibição na UI
        val capturedNotifications = CopyOnWriteArrayList<CapturedNotification>()
        
        // Listener para notificar a UI em tempo real
        var onNotificationReceivedListener: ((CapturedNotification) -> Unit)? = null

        /**
         * Método estático para enviar uma resposta direta via RemoteInput sem abrir o WhatsApp.
         * 
         * @param context Contexto da aplicação
         * @param pendingIntent PendingIntent de resposta extraído da notificação
         * @param remoteInput RemoteInput extraído da ação de notificação
         * @param replyMessage Texto digitado ou ditado por voz para enviar
         * @return Boolean indicando se o disparo do PendingIntent ocorreu com sucesso
         */
        fun sendDirectReply(
            context: Context,
            pendingIntent: PendingIntent,
            remoteInput: RemoteInput,
            replyMessage: String
        ): Boolean {
            return try {
                val intent = Intent()
                val bundle = Bundle()
                
                // Preenche a chave do RemoteInput com a mensagem transcrita
                bundle.putCharSequence(remoteInput.resultKey, replyMessage)
                
                // Anexa os resultados do RemoteInput ao Intent de resposta
                RemoteInput.addResultsToIntent(arrayOf(remoteInput), intent, bundle)
                
                // Dispara o PendingIntent diretamente no sistema Android
                pendingIntent.send(context, 0, intent)
                Log.d(TAG, "Resposta via RemoteInput enviada com sucesso: '$replyMessage'")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao enviar resposta via RemoteInput", e)
                false
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        
        // Filtrar apenas notificações vindas do WhatsApp (Pessoal ou Business)
        if (packageName != "com.whatsapp" && packageName != "com.whatsapp.w4b") {
            return
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // Extrair remetente e mensagem
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "Desconhecido"
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        // Ignorar notificações vazias ou administrativas (ex: "Procurando novas mensagens")
        if (text.isEmpty() || text.contains("novas mensagens", ignoreCase = true)) {
            return
        }

        Log.d(TAG, "Nova notificação capturada de [$title]: $text")

        // Buscar a ação de "Responder" (RemoteInput) dentro da notificação
        var foundReplyAction: PendingIntent? = null
        var foundRemoteInput: RemoteInput? = null

        val actions = notification.actions
        if (actions != null) {
            for (action in actions) {
                val remoteInputs = action.remoteInputs
                if (remoteInputs != null && remoteInputs.isNotEmpty()) {
                    for (ri in remoteInputs) {
                        // Verifica se o RemoteInput aceita entrada de texto
                        if (ri.allowFreeFormInput) {
                            foundReplyAction = action.actionIntent
                            foundRemoteInput = ri
                            break
                        }
                    }
                }
                if (foundReplyAction != null) break
            }
        }

        val captured = CapturedNotification(
            id = sbn.key,
            packageName = packageName,
            senderName = title,
            messageText = text,
            timestamp = sbn.postTime,
            replyAction = foundReplyAction,
            remoteInput = foundRemoteInput
        )

        capturedNotifications.add(0, captured)
        
        // Manter apenas as últimas 20 notificações em memória
        if (capturedNotifications.size > 20) {
            capturedNotifications.removeAt(capturedNotifications.size - 1)
        }

        // Notificar listener da UI
        onNotificationReceivedListener?.invoke(captured)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
