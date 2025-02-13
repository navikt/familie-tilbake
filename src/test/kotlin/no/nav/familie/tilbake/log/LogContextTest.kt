package no.nav.familie.tilbake.log

import no.nav.familie.prosessering.config.ProsesseringInfoProvider
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.task.TracableTaskService
import no.nav.familie.tilbake.config.DatabaseConfig
import no.nav.familie.tilbake.database.DbContainerInitializer
import no.nav.familie.tilbake.log.SecureLog.Context.Companion.logContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.Properties
import java.util.UUID

@Profile("local")
@ExtendWith(SpringExtension::class)
@ContextConfiguration(
    initializers = [DbContainerInitializer::class],
    classes = [
        LogContextTest.Companion.TestConfig::class,
        DatabaseConfig::class,
        TracableTaskService::class,
        DataSourceAutoConfiguration::class,
        TransactionAutoConfiguration::class,
        FlywayAutoConfiguration::class,
    ],
)
class LogContextTest {
    @Autowired
    private lateinit var taskService: TracableTaskService
    private val testLogger = TracedLogger.getLogger<LogContextTest>()

    @Test
    fun `kopierer context fra forrige task`() {
        val task =
            Task(
                type = "test-task",
                payload = "unused",
                properties =
                    Properties().apply {
                        putAll(
                            arrayOf(
                                "logContext.fagsystemId" to "unik",
                                "logContext.behandlingId" to "unik",
                            ),
                        )
                    },
            )
        assertEquals(SecureLog.Context.medBehandling("unik", "unik"), task.logContext())
    }

    @Test
    fun `log context ligger på lagret task`() {
        val logContext =
            SecureLog.Context.medBehandling(
                fagsystemId = UUID.randomUUID().toString(),
                behandlingId = UUID.randomUUID().toString(),
            )
        val persistedId =
            taskService
                .save(
                    Task(
                        payload = "",
                        type = "test_task",
                    ),
                    logContext,
                ).id
        assertEquals(logContext, taskService.findById(persistedId).logContext())
    }

    @Test
    fun `log context blir overført til MDC`() {
        val logContext =
            SecureLog.Context.medBehandling(
                fagsystemId = UUID.randomUUID().toString(),
                behandlingId = UUID.randomUUID().toString(),
            )

        var fagsystemId: String? = null
        var behandlingId: String? = null
        testLogger.medContext(logContext) {
            fagsystemId = MDC.get("fagsystemId")
            behandlingId = MDC.get("behandlingId")
        }

        assertEquals(fagsystemId, logContext.fagsystemId)
        assertEquals(behandlingId, logContext.behandlingId)
    }

    companion object {
        @Configuration
        @ComponentScan(basePackages = ["no.nav.familie.prosessering"])
        class TestConfig {
            @Bean
            fun prosesseringInfoProvider() =
                object : ProsesseringInfoProvider {
                    override fun harTilgang(): Boolean = true

                    override fun hentBrukernavn(): String = "SYSTEM"
                }
        }
    }
}
