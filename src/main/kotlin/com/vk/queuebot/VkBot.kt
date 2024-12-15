package com.vk.queuebot

import api.longpoll.bots.LongPollBot
import api.longpoll.bots.exceptions.VkApiException
import api.longpoll.bots.methods.impl.messages.Send
import api.longpoll.bots.model.events.Update
import api.longpoll.bots.model.events.messages.MessageEvent
import api.longpoll.bots.model.events.messages.MessageNew
import api.longpoll.bots.model.objects.additional.Keyboard
import api.longpoll.bots.model.objects.additional.buttons.Button
import api.longpoll.bots.model.objects.additional.buttons.CallbackButton
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Service
class VkBot(
    @Value("\${admin.ids:332517068}")
    private val adminIds: Set<Int>
) : LongPollBot() {
    companion object {
        private val OLD_GOD = listOf("старый бог", "old god")
        private val START_TEXT = listOf("start queue", "s t", "st")
        private val LOG = LoggerFactory.getLogger(VkBot::class.java)
    }

    @PostConstruct
    fun t() {
        LOG.info("adminIds: $adminIds")
        executor.scheduleAtFixedRate(
            {
                GlobalScope.launch(Dispatchers.IO) {
                    task()
                }
            },
            0,
            1,
            TimeUnit.HOURS
        )
        this.startPolling()
    }

    suspend fun task() {
        val now = LocalDateTime.now()
        if (now.hour == 23) {
            mutex.withLock {
                queues.clear()
            }
            LOG.info("queue cleared at $now")
        }
    }

    private val queues = mutableMapOf<String, MutableList<Int>>()
    private val mutex = Mutex()
    private var muteIvan = true
    private val executor = Executors.newScheduledThreadPool(1)

    override fun onUnknownObject(unknownObject: Update.UnknownObject) {
        println(unknownObject)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onMessageEvent(messageEvent: MessageEvent) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val userId = messageEvent.userId
                val jsonPayload = JsonParser.parseString(messageEvent.payload.toString()).asJsonObject
                val action = jsonPayload.get("action").asString

                when (action) {
                    "join" -> updateQueue(userId, jsonPayload, true)
                    "leave" -> updateQueue(userId, jsonPayload, false)
                }
            } catch (e: VkApiException) {
                e.printStackTrace()
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onMessageNew(messageNew: MessageNew) {
        GlobalScope.launch {
            try {
                val message = messageNew.message
                if (message.hasText()) {
                    val messageDto = MessageDto(
                        text = message.text,
                        peerId = message.peerId,
                        fromId = message.fromId,
                        date = message.date,
                        id = message.conversationMessageId,
                    )
                    LOG.info("Received: $messageDto")
                    handleMessage(messageDto)
                }
            } catch (e: VkApiException) {
                e.printStackTrace()
            }
        }
    }

    data class MessageDto(
        val text: String,
        val peerId: Int,
        val fromId: Int,
        val date: Int,
        val id: Int,
    )

    private fun handleMessage(message: MessageDto) {
        val text = message.text.lowercase(Locale.getDefault()).trim()
        val currentStart = START_TEXT.filter { text.startsWith(it) }
        when {
            currentStart.isNotEmpty() -> {
                val queueName = text.substringAfter(currentStart[0]).trim()
                startQueue(queueName, message.peerId)
            }

            OLD_GOD.any { it in text } -> {
                vk.messages.send().setMessage("Старый бог!").setPeerId(message.peerId).execute()
            }

            message.fromId in adminIds && (text == "on" || text == "off") -> {
                muteIvan = text == "on"
                vk.messages.send().setMessage("Ivan is muted = $muteIvan").setPeerId(message.peerId).execute()
            }

            muteIvan && message.fromId == 176065807 -> {
                vk.messages.send().setMessage("Тебе слова не давали").setPeerId(message.peerId).execute()
            }
        }
    }

    // Received: MessageDto(text=Хуй, peerId=2000000001, fromId=176065807, date=1734270149)
    // 2024-12-15T16:42:35.592+03:00  INFO 85990 --- [queueBot] [atcher-worker-1] com.vk.queuebot.VkBot                    : Received: MessageDto(text=Old god, peerId=2000000001, fromId=176065807, date=1734270154)

    fun startQueue(queueName: String, peerId: Int) {
        if (!queues.containsKey(queueName)) {
            // Create buttons using a helper method
            val response2 = "Queue '$queueName' started! Click the buttons (one sec) to join or leave."
            val responseBody = vk.messages.send()
                .setPeerIds(peerId)
                .setMessage(response2)
                .execute()

            val responses = responseBody.response as List<Send.ResponseBody.Response>
            val response = responses[0]

            val peerId = response.peerId
            val conversationMessageId = response.conversationMessageId

            queues[queueName] = mutableListOf()
            val keyboard = createKeyboard(queueName, conversationMessageId, peerId)
            vk.messages.edit()
                .setPeerId(peerId)
                .setConversationMessageId(conversationMessageId)
                .setKeyboard(keyboard)
                .setMessage("Queue '$queueName' started!")
                .execute()
        } else {
            vk.messages.send()
                .setPeerId(peerId)
                .setMessage("Queue '$queueName' already exists.")
                .execute()
        }
    }

    private fun createKeyboard(queueName: String, messageId: Int, lastPeerId: Int): Keyboard {
        val joinButton = createButton("Join", "join", Button.Color.POSITIVE, queueName, messageId, lastPeerId)
        val leaveButton = createButton("Leave", "leave", Button.Color.NEGATIVE, queueName, messageId, lastPeerId)

        val row1 = listOf(joinButton, leaveButton)
        return Keyboard(listOf(row1)).setInline(true)
    }

    private fun createButton(
        label: String,
        action: String,
        color: Button.Color,
        queueName: String,
        messageId: Int,
        lastPeerId: Int
    ): Button {
        val payload = JsonObject().apply {
            addProperty("action", action)
            addProperty("queueName", queueName)
            addProperty("messageId", messageId)
            addProperty("peerId", lastPeerId)
        }
        return CallbackButton(color, CallbackButton.Action(label, payload))
    }

    private suspend fun updateQueue(userId: Int, jsonPayload: JsonObject, isJoining: Boolean) = mutex.withLock {
        val queueName = jsonPayload.get("queueName").asString
        val messageId = jsonPayload.get("messageId").asInt
        val peerId = jsonPayload.get("peerId").asInt
        val queue = queues[queueName] ?: return

        if (isJoining) {
            if (!queue.contains(userId)) {
                queue.add(userId)
            } else {
                return@withLock
            }
        } else {
            if (!queue.remove(userId)) {
                return@withLock
            }
        }
        sendUpdatedQueueMessage(messageId, peerId, queue, queueName)
    }

    private fun sendUpdatedQueueMessage(messageId: Int, peerId: Int, queue: List<Int>, queueName: String) {
        val users = vk.users
            .get()
            .setUserIds(queue.map { it.toString() })
            .execute()
            .response
            .map { UserInfo(it.firstName, it.lastName) }

        val keyboard = createKeyboard(queueName, messageId, peerId)

        val queueList =
            "Queue '$queueName' started!\n " + users.joinToString("\n") { it.toString() }
        vk.messages.edit()
            .setPeerId(peerId)
            .setMessage(queueList)
            .setConversationMessageId(messageId)
            .setKeyboard(keyboard)
            .execute()
    }

    override fun getAccessToken(): String {
        val env = System.getenv()
        val token = env["VK_TOKEN"]!!
        return token
    }
}

private data class UserInfo(
    val name: String,
    val lastName: String,
) {
    override fun toString(): String {
        return "$name $lastName"
    }
}
