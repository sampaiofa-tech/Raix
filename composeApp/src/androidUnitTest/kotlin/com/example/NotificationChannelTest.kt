package com.example

import com.example.util.NotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Testa que o channel ID do FCM declarado no Manifest corresponde
 * ao channel ID criado em runtime pelo NotificationHelper.
 *
 * Causa raiz v1.9.0: mismatch entre Manifest e codigo causava
 * descarte silencioso de notificacoes em background.
 */
class NotificationChannelTest {

    @Test
    fun `channel ID constante deve corresponder ao valor esperado no Manifest`() {
        // O AndroidManifest declara este valor em
        // meta-data com.google.firebase.messaging.default_notification_channel_id
        val manifestChannelId = "pmsg_high_priority_messages_channel_v2"
        assertEquals(
            "Channel ID do NotificationHelper deve corresponder ao declarado no AndroidManifest.xml",
            manifestChannelId,
            NotificationHelper.CHANNEL_NEW_CONVERSATIONS_ID
        )
    }
}
