package no.nav.tilbakekreving.e2e

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.nondeterministic.eventuallyConfig
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import jakarta.jms.ConnectionFactory
import kotlinx.coroutines.runBlocking
import no.nav.familie.tilbake.api.BehandlingController
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.config.OppdragClientMock
import no.nav.familie.tilbake.config.PdlClientMock
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.InnloggetBrukertilgang
import no.nav.familie.tilbake.sikkerhet.Tilgangskontrollsfagsystem
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegDto
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagBufferRepository
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagMediator
import no.nav.tilbakekreving.saksbehandler.Behandler
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

open class TilbakekrevingE2EBase : E2EBase() {
    @Autowired
    protected lateinit var kravgrunnlagBufferRepository: KravgrunnlagBufferRepository

    @Autowired
    private lateinit var kravgrunnlagMediator: KravgrunnlagMediator

    @Autowired
    protected lateinit var tilbakekrevingService: TilbakekrevingService

    @Autowired
    private lateinit var connectionFactory: ConnectionFactory

    @Autowired
    protected lateinit var pdlClient: PdlClientMock

    @Autowired
    protected lateinit var oppdragClient: OppdragClientMock

    @Autowired
    protected lateinit var behandlingController: BehandlingController

    @Autowired
    protected lateinit var jdbcTemplate: JdbcTemplate

    @AfterEach
    fun reset() {
        pdlClient.reset()
    }

    fun sendMessage(
        queueName: String,
        text: String,
    ) {
        val connection = connectionFactory.createConnection()
        connection.createSession().use { session ->
            val message = session.createTextMessage(text)
            val queue = session.createQueue(queueName)
            session.createProducer(queue).use {
                it.send(message)
            }
        }
    }

    fun sendKravgrunnlag(
        queueName: String,
        kravgrunnlag: String,
    ) {
        sendMessage(queueName, kravgrunnlag)

        runBlocking {
            eventually(
                eventuallyConfig {
                    duration = 1000.milliseconds
                    interval = 10.milliseconds
                },
            ) {
                tellUlesteKravgrunnlag() shouldBe 1
            }
        }
    }

    fun sendKravgrunnlagOgAvventLesing(
        queueName: String,
        kravgrunnlag: String,
    ) {
        sendKravgrunnlag(queueName, kravgrunnlag)
        kravgrunnlagMediator.lesKravgrunnlag()

        tellUlesteKravgrunnlag() shouldBe 0
    }

    fun behandlingIdFor(
        fagsystemId: String,
        fagsystem: FagsystemDTO,
    ): UUID? {
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(fagsystem, fagsystemId) ?: return null
        return tilbakekreving.behandlingHistorikk.nåværende().entry.tilFrontendDto(Behandler.Saksbehandler("A123456"), true).behandlingId
    }

    fun behandling(behandlingId: UUID): Behandling {
        return tilbakekrevingService.hentTilbakekreving(behandlingId).shouldNotBeNull().behandlingHistorikk.nåværende().entry
    }

    fun somSaksbehandler(
        ident: String,
        callback: () -> Unit,
    ) {
        mockkObject(ContextService)
        every { ContextService.hentPåloggetSaksbehandler(any(), any()) } returns ident
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any(), any()) } returns InnloggetBrukertilgang(
            Tilgangskontrollsfagsystem.entries.associateWith { Behandlerrolle.BESLUTTER },
        )
        callback()
        unmockkObject(ContextService)
    }

    fun utførSteg(
        ident: String,
        behandlingId: UUID,
        stegData: BehandlingsstegDto,
    ) {
        somSaksbehandler(ident) {
            behandlingController.utførBehandlingssteg(behandlingId, stegData).status shouldBe Ressurs.Status.SUKSESS
        }
    }

    private fun tellUlesteKravgrunnlag(): Int {
        return jdbcTemplate.query("SELECT count(1) AS antall FROM kravgrunnlag_buffer WHERE lest=false;") { rs, _ ->
            rs.getInt("antall")
        }.single()
    }
}
