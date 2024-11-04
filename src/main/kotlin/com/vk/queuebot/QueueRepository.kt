package com.vk.queuebot

// import com.vk.queuebot.domain.db.tables.Queue.QUEUE
// import com.vk.queuebot.domain.db.tables.records.QueueRecord
// import jakarta.annotation.PostConstruct
// import com.vk.queuebot.domain.db.tables.Queue
// import com.vk.queuebot.domai// n.HEHE
// import db.tables.Queue
import com.vk.queuebot.domain.db.tables.Queue.QUEUE
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Repository

@Repository
class QueueRepository(private val dsl: DSLContext) {
    fun createQueue(name: String, fromId: Long, createdTs: Long): Int {
        return dsl.insertInto(QUEUE)
            .set(QUEUE.NAME, name)
            .set(QUEUE.FROM_ID, fromId)
            .set(QUEUE.CREATED_TS, createdTs)
            .returning(QUEUE.ID)
            .fetchOne()
            ?.value1() ?: throw RuntimeException("Failed to insert queue")
    }

    fun findAllQueues(): List<Record> {
        return dsl.selectFrom(QUEUE).fetch().toList()

    }
}