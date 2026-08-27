package com.intercomunicador.poc

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intercomunicador.poc.audio.AudioIntercomManager
import com.intercomunicador.poc.model.CapturedNotification
import com.intercomunicador.poc.service.WhatsAppNotificationListener

class MainActivity : ComponentActivity() {

    private lateinit var audioManager: AudioIntercomManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        audioManager = AudioIntercomManager(this)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(audioManager = audioManager)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioManager.shutdown()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(audioManager: AudioIntercomManager) {
    val context = LocalContext.current
    var isPermissionGranted by remember { mutableStateOf(isNotificationServiceEnabled(context)) }
    var notificationsList by remember { mutableStateOf(WhatsAppNotificationListener.capturedNotifications.toList()) }
    var customReplyText by remember { mutableStateOf("OK! Resposta enviada pelo Intercomunicador Inteligente.") }

    // Registrar callback em tempo real
    DisposableEffect(Unit) {
        WhatsAppNotificationListener.onNotificationReceivedListener = { notification ->
            notificationsList = WhatsAppNotificationListener.capturedNotifications.toList()
        }
        onDispose {
            WhatsAppNotificationListener.onNotificationReceivedListener = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Intercomunicador PoC — WhatsApp", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Card de Status de Permissões
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isPermissionGranted) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isPermissionGranted) "✅ Permissão de Notificação ATIVA" else "⚠️ Permissão de Notificação NECESSÁRIA",
                        fontWeight = FontWeight.Bold,
                        color = if (isPermissionGranted) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isPermissionGranted)
                            "O serviço está escutando notificações do WhatsApp em tempo real."
                        else
                            "É necessário conceder acesso às notificações para capturar as mensagens do WhatsApp.",
                        fontSize = 13.sp
                    )
                    if (!isPermissionGranted) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                isPermissionGranted = isNotificationServiceEnabled(context)
                            }
                        ) {
                            Text("Abrir Configurações do Android")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Campo para testar texto de resposta
            OutlinedTextField(
                value = customReplyText,
                onValueChange = { customReplyText = it },
                label = { Text("Texto da Resposta de Teste (Simulação de Voz/STT)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Mensagens Capturadas (${notificationsList.size}):",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (notificationsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nenhuma mensagem do WhatsApp capturada ainda.\nEnvie uma mensagem de outro celular para testar.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notificationsList) { item ->
                        NotificationCard(
                            item = item,
                            replyText = customReplyText,
                            onSpeakClick = {
                                audioManager.speakMessage(item.senderName, item.messageText)
                            },
                            onReplyClick = {
                                if (item.replyAction != null && item.remoteInput != null) {
                                    val success = WhatsAppNotificationListener.sendDirectReply(
                                        context = context,
                                        pendingIntent = item.replyAction,
                                        remoteInput = item.remoteInput,
                                        replyMessage = customReplyText
                                    )
                                    if (success) {
                                        Toast.makeText(context, "Resposta enviada via RemoteInput!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Falha ao disparar resposta via RemoteInput", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Esta notificação não possui botão de resposta direta", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    item: CapturedNotification,
    replyText: String,
    onSpeakClick: () -> Unit,
    onReplyClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = item.senderName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    text = if (item.packageName == "com.whatsapp.w4b") "WA Business" else "WhatsApp",
                    fontSize = 11.sp,
                    color = Color(0xFF075E54)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = item.messageText, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSpeakClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("🔊 Ouvir (TTS)", fontSize = 12.sp)
                }

                Button(
                    onClick = onReplyClick,
                    enabled = item.replyAction != null && item.remoteInput != null,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                ) {
                    Text("💬 Responder (RemoteInput)", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}

/**
 * Utilitário para verificar se a permissão NotificationListenerService foi concedida pelo usuário.
 */
fun isNotificationServiceEnabled(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    if (!TextUtils.isEmpty(flat)) {
        val names = flat.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (name in names) {
            val componentName = ComponentName.unflattenFromString(name)
            if (componentName != null && TextUtils.equals(pkgName, componentName.packageName)) {
                return true
            }
        }
    }
    return false
}
