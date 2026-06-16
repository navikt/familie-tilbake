package no.nav.tilbakekreving.e2e
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.nondeterministic.eventuallyConfig
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import jakarta.jms.ConnectionFactory
import jakarta.jms.JMSException
import kotlinx.coroutines.runBlocking
import no.nav.familie.tilbake.api.BehandlingController
import no.nav.familie.tilbake.api.VilkårsvurderingController
import no.nav.familie.tilbake.config.OppdragClientMock
import no.nav.familie.tilbake.config.PdlClientMock
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.tilbakekreving.SystemKlokke
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.api.BehandlingApiController
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegVilkårsvurderingDto
import no.nav.tilbakekreving.api.v1.dto.VilkårsvurderingsperiodeDto
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.behandling.UttalelseInfo
import no.nav.tilbakekreving.behandling.UttalelseVurdering
import no.nav.tilbakekreving.kontrakter.frontend.models.FaktaPeriodeDto
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagBufferRepository
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagMediator
import no.nav.tilbakekreving.repository.NyBehandlingRepository
import no.nav.tilbakekreving.repository.TilbakekrevingFilter
import no.nav.tilbakekreving.test.FellesTestdata
import no.nav.tilbakekreving.test.FellesTestdata.ANSVARLIG_SAKSBEHANDLER
import no.nav.tilbakekreving.test.FellesTestdata.SAKSBEHANDLER_IDENT
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

@ActiveProfiles("ny-modell")
open class TilbakekrevingE2EBase : E2EBase() {
    @Autowired
    protected lateinit var behandlingRepository: NyBehandlingRepository

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

    @Autowired
    protected lateinit var behandlingApiController: BehandlingApiController

    @Autowired
    protected lateinit var vilkårsvurderingController: VilkårsvurderingController

    @AfterEach
    fun reset() {
        pdlClient.reset()
    }

    fun sendMessage(
        queueName: String,
        text: String,
    ) {
        repeat(5) {
            try {
                connectionFactory.createConnection().use {
                    it.createSession().use { session ->
                        val message = session.createTextMessage(text)
                        val queue = session.createQueue(queueName)
                        session.createProducer(queue).use {
                            it.send(message)
                        }
                    }
                }
                return
            } catch (e: JMSException) {
                e.printStackTrace()
                Thread.sleep(500)
            }
        }
    }

    fun sendKravgrunnlag(
        queueName: String,
        kravgrunnlag: String,
    ) {
        sendMessage(queueName, kravgrunnlag)

        avventAntallUlesteKravgrunnlag(1)
    }

    fun sendKravgrunnlagOgAvventLesing(
        queueName: String,
        kravgrunnlag: String,
    ) {
        sendKravgrunnlag(queueName, kravgrunnlag)
        kravgrunnlagMediator.lesKravgrunnlag()

        avventAntallUlesteKravgrunnlag(0)
    }

    fun behandlingIdFor(
        fagsystem: FagsystemDTO,
        fagsystemId: String,
    ): UUID? {
        return tilbakekreving(fagsystem, fagsystemId)?.nåværendeBehandlingId()
    }

    fun lagreUttalelse(
        behandlingId: UUID,
        uttalelse: String? = "",
    ) {
        val tilbakekrevingId = tilbakekreving(behandlingId).id
        tilbakekrevingService.hentOgLagreTilbakekreving(TilbakekrevingFilter.tilbakekreving(tilbakekrevingId)) { tilbakekreving, context ->
            tilbakekreving.gjørSaksbehandling(behandlingId, context(ANSVARLIG_SAKSBEHANDLER)) {
                if (uttalelse == null) {
                    lagreUttalelse(UttalelseVurdering.NEI_ETTER_FORHÅNDSVARSEL, null, "")
                } else {
                    lagreUttalelse(
                        UttalelseVurdering.JA_ETTER_FORHÅNDSVARSEL,
                        UttalelseInfo(UUID.randomUUID(), LocalDate.now(), "Reddit", uttalelse),
                        null,
                    )
                }
            }
        }
    }

    fun tilbakekreving(behandlingId: UUID): Tilbakekreving =
        tilbakekrevingService.hentTilbakekreving(TilbakekrevingFilter.behandling(behandlingId)).shouldNotBeNull()

    fun tilbakekreving(fagsystem: FagsystemDTO, fagsystemId: String): Tilbakekreving? =
        tilbakekrevingService.hentTilbakekreving(TilbakekrevingFilter.fagsak(fagsystemId, fagsystem))

    fun behandling(behandlingId: UUID): Behandling {
        return tilbakekreving(behandlingId).hentBehandling(behandlingId)
    }

    fun allePeriodeIder(behandlingId: UUID): List<UUID> = tilbakekreving(behandlingId)
        .shouldNotBeNull()
        .tilFeilutbetalingFrontendDto(behandlingId, SystemKlokke)
        .perioder
        .map(FaktaPeriodeDto::id)
        .map(UUID::fromString)

    fun <T> somSaksbehandler(
        ident: String,
        callback: () -> T,
    ): T = ContextServiceHelpers.somSaksbehandler(ident, callback)

    fun utførSteg(
        behandlingId: UUID,
        stegData: BehandlingsstegDto,
        ident: String = SAKSBEHANDLER_IDENT,
    ) {
        somSaksbehandler(ident) {
            behandlingController.utførBehandlingssteg(behandlingId, stegData).status shouldBe Ressurs.Status.SUKSESS
        }
    }

    fun håndterVilkårsvurdering(
        ident: String,
        behandlingId: UUID,
        vararg vurderinger: VilkårsvurderingsperiodeDto,
    ) {
        utførSteg(behandlingId, BehandlingsstegVilkårsvurderingDto(vurderinger.toList()), ident)
    }

    fun tilbakekrevVedtak(
        behandlingId: UUID,
        perioder: List<Datoperiode>,
    ) {
        somSaksbehandler(FellesTestdata.SAKSBEHANDLER_IDENT) {
            behandlingApiController.behandlingOppdaterFakta(
                behandlingId.toString(),
                BehandlingsstegGenerator.lagFaktastegVurderingFritekst(allePeriodeIder(behandlingId)),
            )
        }

        utførSteg(
            behandlingId,
            BehandlingsstegGenerator.lagIkkeForeldetVurdering(
                *perioder.toTypedArray(),
            ),
        )
        utførSteg(
            behandlingId,
            BehandlingsstegGenerator.lagVilkårsvurderingFullTilbakekreving(*perioder.toTypedArray()),
        )
        utførSteg(behandlingId, BehandlingsstegGenerator.lagForeslåVedtakVurdering())
        utførSteg(
            ident = FellesTestdata.BESLUTTER_IDENT,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagGodkjennVedtakVurdering(),
        )
    }

    fun avventAntallUlesteKravgrunnlag(antall: Int) {
        runBlocking {
            eventually(
                eventuallyConfig {
                    duration = 2000.milliseconds
                    interval = 10.milliseconds
                },
            ) {
                tellUlesteKravgrunnlag() shouldBe antall
            }
        }
    }

    private fun tellUlesteKravgrunnlag(): Int {
        return jdbcTemplate.query("SELECT count(1) AS antall FROM kravgrunnlag_buffer WHERE lest=false AND utenfor_scope=false;") { rs, _ ->
            rs.getInt("antall")
        }.single()
    }

    companion object {
        fun Tilbakekreving.nåværendeBehandlingId() = hentBehandlingsinformasjon().behandlingId
    }
}
