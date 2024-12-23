/*
 * This file is generated by jOOQ.
*/
package com.vk.queuebot.domain.db;


import com.vk.queuebot.domain.db.tables.Queue;
import com.vk.queuebot.domain.db.tables.SchemaVersion;
import com.vk.queuebot.domain.db.tables.records.QueueRecord;
import com.vk.queuebot.domain.db.tables.records.SchemaVersionRecord;

import javax.annotation.Generated;

import org.jooq.Identity;
import org.jooq.UniqueKey;
import org.jooq.impl.Internal;


/**
 * A class modelling foreign key relationships and constraints of tables of 
 * the <code>PUBLIC</code> schema.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.10.7"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Keys {

    // -------------------------------------------------------------------------
    // IDENTITY definitions
    // -------------------------------------------------------------------------

    public static final Identity<QueueRecord, Integer> IDENTITY_QUEUE = Identities0.IDENTITY_QUEUE;

    // -------------------------------------------------------------------------
    // UNIQUE and PRIMARY KEY definitions
    // -------------------------------------------------------------------------

    public static final UniqueKey<QueueRecord> CONSTRAINT_4 = UniqueKeys0.CONSTRAINT_4;
    public static final UniqueKey<SchemaVersionRecord> SCHEMA_VERSION_PK = UniqueKeys0.SCHEMA_VERSION_PK;

    // -------------------------------------------------------------------------
    // FOREIGN KEY definitions
    // -------------------------------------------------------------------------


    // -------------------------------------------------------------------------
    // [#1459] distribute members to avoid static initialisers > 64kb
    // -------------------------------------------------------------------------

    private static class Identities0 {
        public static Identity<QueueRecord, Integer> IDENTITY_QUEUE = Internal.createIdentity(Queue.QUEUE, Queue.QUEUE.ID);
    }

    private static class UniqueKeys0 {
        public static final UniqueKey<QueueRecord> CONSTRAINT_4 = Internal.createUniqueKey(Queue.QUEUE, "CONSTRAINT_4", Queue.QUEUE.ID);
        public static final UniqueKey<SchemaVersionRecord> SCHEMA_VERSION_PK = Internal.createUniqueKey(SchemaVersion.SCHEMA_VERSION, "schema_version_pk", SchemaVersion.SCHEMA_VERSION.VERSION);
    }
}
