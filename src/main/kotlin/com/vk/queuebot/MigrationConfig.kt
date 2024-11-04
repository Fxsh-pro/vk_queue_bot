package com.vk.queuebot

import org.jooq.ConnectionProvider
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.TransactionProvider
import org.jooq.conf.RenderNameStyle
import org.jooq.conf.Settings
import org.jooq.impl.DataSourceConnectionProvider
import org.jooq.impl.DefaultConfiguration
import org.jooq.impl.DefaultDSLContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.jooq.SpringTransactionProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

@Configuration
class MigrationConfig(
    @Value("\${jooq.sql.dialect:POSTGRES}")
    private var jooqDialect: SQLDialect? = null
) {
    @Bean
    fun connectionProvider(dataSource: DataSource): ConnectionProvider? {
        return DataSourceConnectionProvider(dataSource)
    }

    private fun jooqSettings(): Settings {
        val settings = Settings()
        settings.isRenderSchema = false
        settings.renderNameStyle = RenderNameStyle.LOWER
        return settings
    }

    private fun jooqConfiguration(
        connectionProvider: ConnectionProvider,
        transactionManager: PlatformTransactionManager
    ): org.jooq.Configuration {
        val transactionProvider: TransactionProvider = SpringTransactionProvider(transactionManager)
        val configuration = DefaultConfiguration()
        configuration.setSQLDialect(jooqDialect)
        configuration.setSettings(jooqSettings())
        configuration.setConnectionProvider(connectionProvider)
        configuration.setTransactionProvider(transactionProvider)
        return configuration
    }

    @Bean
    fun dslContext(
        connectionProvider: ConnectionProvider,
        transactionManager: PlatformTransactionManager
    ): DSLContext? {
        return DefaultDSLContext(jooqConfiguration(connectionProvider, transactionManager))
    }

}