# Intercomunicador Inteligente — PoC Android (Kotlin)

Proof-of-Concept (PoC) desenvolvida para validar a arquitetura técnica de integração de voz com o WhatsApp via Android sem violar as diretrizes da Google Play Store nem correr risco de banimento de conta.

---

## 📌 Principais Recursos Validados nesta PoC

1. **Captura de Notificações em Tempo Real (`WhatsAppNotificationListener.kt`):**
   - Utiliza a permissão nativa `NotificationListenerService`.
   - Intercepta mensagens vindas de `com.whatsapp` e `com.whatsapp.w4b` (WhatsApp Business).
   - Extrai remetente, conteúdo e timestamp.

2. **Resposta Direta via API Oficial (`RemoteInput`):**
   - Localiza a ação nativa de "Responder" (`NotificationCompat.Action`) anexada na notificação do WhatsApp.
   - Preenche o `RemoteInput` com o texto da resposta (obtido por voz/ditado STT).
   - Envia a resposta através de `PendingIntent.send()`. **Zero clique na tela, zero abertura do app do WhatsApp, zero risco de banimento.**

3. **Gerenciador de Áudio e Bluetooth (`AudioIntercomManager.kt`):**
   - Leitura de mensagens por **Text-to-Speech (TTS)** nativo em Português do Brasil.
   - Solicitação de **AudioFocus** (`AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`) para pausar ou diminuir o volume da música (Spotify, YouTube Music) durante a leitura e restaurar em seguida.
   - Alternância para **Bluetooth SCO** (`startBluetoothSco()`) para capturar o áudio do microfone do intercomunicador do capacete.

---

## 🚀 Como Testar no Celular Android

### Passo 1: Abrir o projeto no Android Studio
1. Abra o Android Studio.
2. Selecione **Open Project** e navegue até a pasta `c:\Users\User\Desktop\new\android-poc`.
3. Aguarde o Gradle sincronizar as dependências (`Compose`, `Kotlin`, `Core-KTX`).

### Passo 2: Compilar e Instalar
1. Conecte um celular Android via cabo USB (com depuração USB ativa) ou inicie um emulador Android.
2. Clique no botão **Run (Shift + F10)** no Android Studio para instalar o app **Intercomunicador PoC**.

### Passo 3: Conceder Permissão de Notificação
1. Ao abrir o aplicativo, o card inicial mostrará: `⚠️ Permissão de Notificação NECESSÁRIA`.
2. Clique no botão **"Abrir Configurações do Android"**.
3. Na lista de apps do sistema, encontre **Leitor de Notificações WhatsApp PoC** (ou **Intercomunicador PoC**) e **ATIVE** a chave de acesso.

### Passo 4: Realizar o Teste de Comunicação
1. Peça para um amigo (ou use outro celular) enviar uma mensagem de WhatsApp para o seu celular.
2. Observe que a mensagem aparecerá instantaneamente no app **Intercomunicador PoC**.
3. Clique em **"🔊 Ouvir (TTS)"** para testar a síntese de voz e a pausa na música.
4. Digite ou mantenha a mensagem de teste no campo de texto e clique em **"💬 Responder (RemoteInput)"**.
5. Verifique no WhatsApp que a mensagem foi entregue para a outra pessoa como uma resposta direta normal!

---

## 📂 Estrutura de Arquivos

```
android-poc/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/intercomunicador/poc/
│           ├── MainActivity.kt               # Interface Jetpack Compose e teste interativo
│           ├── model/
│           │   └── CapturedNotification.kt  # Modelo de dados da notificação
│           ├── service/
│           │   └── WhatsAppNotificationListener.kt # NotificationListener & RemoteInput reply
│           └── audio/
│               └── AudioIntercomManager.kt   # TTS, AudioFocus e Bluetooth SCO
├── build.gradle.kts
└── settings.gradle.kts
```
