package com.example.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Singleton de diagnostico para o pipeline de notificacao push.
 * Registra eventos em tempo real para exibicao na tela (sem adb).
 *
 * Campos:
 * - Token FCM existe?
 * - Token registrado no servidor?
 * - Ultimo push recebido (quando)?
 * - Worker rodou (quando, quantas msgs)?
 * - Notificacao disparada (quando)?
 */
object PushDiagnostics {

    data class DiagState(
        val fcmTokenExists: Boolean = false,
        val fcmTokenPrefix: String = "",
        val tokenRegisteredOnServer: Boolean = false,
        val tokenRegistrationTime: String = "",
        val lastPushReceivedTime: String = "",
        val lastPushSenderId: String = "",
        val workerLastRunTime: String = "",
        val workerMsgCount: Int = 0,
        val lastNotificationTime: String = "",
        val lastNotificationSource: String = "",
        val pollingActive: Boolean = false,
        val pollingLastCycle: String = ""
    )

    private val _state = MutableStateFlow(DiagState())
    val state: StateFlow<DiagState> = _state

    fun updateFcmToken(token: String?) {
        _state.value = _state.value.copy(
            fcmTokenExists = !token.isNullOrBlank(),
            fcmTokenPrefix = token?.take(12)?.plus("...") ?: "(vazio)"
        )
    }

    fun markTokenRegistered() {
        _state.value = _state.value.copy(
            tokenRegisteredOnServer = true,
            tokenRegistrationTime = currentTimestamp()
        )
    }

    fun markPushReceived(senderId: String) {
        _state.value = _state.value.copy(
            lastPushReceivedTime = currentTimestamp(),
            lastPushSenderId = senderId
        )
    }

    fun markWorkerRun(msgCount: Int) {
        _state.value = _state.value.copy(
            workerLastRunTime = currentTimestamp(),
            workerMsgCount = msgCount
        )
    }

    fun markNotificationFired(source: String) {
        _state.value = _state.value.copy(
            lastNotificationTime = currentTimestamp(),
            lastNotificationSource = source
        )
    }

    fun markPollingCycle(active: Boolean) {
        _state.value = _state.value.copy(
            pollingActive = active,
            pollingLastCycle = currentTimestamp()
        )
    }

    private fun currentTimestamp(): String {
        val now = com.example.data.network.PlatformEnvironment.currentTimeMillis()
        val secs = (now / 1000) % 86400
        val h = (secs / 3600).toString().padStart(2, '0')
        val m = ((secs % 3600) / 60).toString().padStart(2, '0')
        val s = (secs % 60).toString().padStart(2, '0')
        return "$h:$m:$s"
    }
}
