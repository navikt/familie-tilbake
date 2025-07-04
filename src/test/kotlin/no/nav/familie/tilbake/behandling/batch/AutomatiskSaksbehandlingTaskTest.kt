package no.nav.familie.tilbake.behandling.batch

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forOne
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.task.TracableTaskService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.vedtak.SendVedtaksbrevTask
import no.nav.familie.tilbake.dokumentbestilling.vedtak.VedtaksbrevsoppsummeringRepository
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingRepository
import no.nav.familie.tilbake.foreldelse.VurdertForeldelseRepository
import no.nav.familie.tilbake.iverksettvedtak.task.AvsluttBehandlingTask
import no.nav.familie.tilbake.iverksettvedtak.task.SendØkonomiTilbakekrevingsvedtakTask
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingRepository
import no.nav.tilbakekreving.kontrakter.Tilbakekrevingsvalg
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandling.Saksbehandlingstype
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsesundertype
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Properties

internal class AutomatiskSaksbehandlingTaskTest : OppslagSpringRunnerTest() {
    override val tømDBEtterHverTest = false

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var faktaFeilutbetalingRepository: FaktaFeilutbetalingRepository

    @Autowired
    private lateinit var foreldelsesRepository: VurdertForeldelseRepository

    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @Autowired
    private lateinit var vedtaksbrevsoppsummeringRepository: VedtaksbrevsoppsummeringRepository

    @Autowired
    private lateinit var sendØkonomiTilbakekrevingsvedtakTask: SendØkonomiTilbakekrevingsvedtakTask

    @Autowired
    private lateinit var sendVedtaksbrevTask: SendVedtaksbrevTask

    @Autowired
    private lateinit var avsluttBehandlingTask: AvsluttBehandlingTask

    private val taskService: TracableTaskService = mockk()

    @Autowired
    private lateinit var behandlingskontrollService: BehandlingskontrollService

    @Autowired
    private lateinit var automatiskSaksbehandlingTask: AutomatiskSaksbehandlingTask

    private lateinit var fagsak: Fagsak
    private lateinit var behandling: Behandling

    @BeforeEach
    fun init() {
        val fagsak = fagsakRepository.insert(Testdata.fagsak())
        behandling = Testdata.lagBehandling(fagsakId = fagsak.id)
        val fagsystemsbehandling =
            behandling.aktivFagsystemsbehandling.copy(
                tilbakekrevingsvalg =
                    Tilbakekrevingsvalg
                        .OPPRETT_TILBAKEKREVING_UTEN_VARSEL,
            )
        behandlingRepository.insert(
            behandling.copy(
                fagsystemsbehandling = setOf(fagsystemsbehandling),
                status = Behandlingsstatus.UTREDES,
            ),
        )
        val feilKravgrunnlagBeløp = Testdata.lagFeilKravgrunnlagsbeløp().copy(nyttBeløp = BigDecimal("100"))
        val ytelKravgrunnlagsbeløp433 =
            Testdata.lagYtelKravgrunnlagsbeløp().copy(
                opprinneligUtbetalingsbeløp = BigDecimal("100"),
                tilbakekrevesBeløp = BigDecimal("100"),
            )

        val kravgrunnlag = Testdata.lagKravgrunnlag(behandling.id).copy(
            kontrollfelt = "2019-11-22-19.09.31.458065",
            perioder = setOf(
                Testdata.lagKravgrunnlagsperiode().copy(
                    beløp = setOf(
                        feilKravgrunnlagBeløp,
                        ytelKravgrunnlagsbeløp433,
                    ),
                ),
            ),
        )

        kravgrunnlagRepository.insert(kravgrunnlag)
        behandlingsstegstilstandRepository.insert(
            lagBehandlingsstegstilstand(
                Behandlingssteg.GRUNNLAG,
                Behandlingsstegstatus.UTFØRT,
            ),
        )
        behandlingsstegstilstandRepository.insert(
            lagBehandlingsstegstilstand(
                Behandlingssteg.FAKTA,
                Behandlingsstegstatus.KLAR,
            ),
        )
    }

    @Test
    fun `doTask skal ikke behandle når behandling allerede er avsluttet`() {
        val behandling = behandlingRepository.findByIdOrThrow(behandling.id).copy(status = Behandlingsstatus.AVSLUTTET)
        behandlingRepository.update(behandling)

        val exception = shouldThrow<RuntimeException> { automatiskSaksbehandlingTask.doTask(lagTask()) }
        exception.message shouldBe "Behandling med id=${behandling.id} er allerede ferdig behandlet"
    }

    @Test
    fun `doTask skal ikke behandle når behandling er på vent`() {
        behandlingskontrollService.settBehandlingPåVent(
            behandling.id,
            Venteårsak.ENDRE_TILKJENT_YTELSE,
            LocalDate.now().plusWeeks(2),
            SecureLog.Context.tom(),
        )

        val exception = shouldThrow<RuntimeException> { automatiskSaksbehandlingTask.doTask(lagTask()) }
        exception.message shouldBe "Behandling med id=${behandling.id} er på vent, kan ikke behandle steg FAKTA"
    }

    @Test
    fun `doTask skal behandle behandling automatisk`() {
        automatiskSaksbehandlingTask.doTask(lagTask())
        mockTaskExecution()

        val behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandling.saksbehandlingstype shouldBe Saksbehandlingstype.AUTOMATISK_IKKE_INNKREVING_LAVT_BELØP
        behandling.ansvarligSaksbehandler shouldBe "VL"
        behandling.ansvarligBeslutter shouldBe "VL"
        behandling.status shouldBe Behandlingsstatus.AVSLUTTET

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.UTFØRT)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.FATTE_VEDTAK, Behandlingsstegstatus.UTFØRT)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.IVERKSETT_VEDTAK, Behandlingsstegstatus.UTFØRT)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.AVSLUTTET, Behandlingsstegstatus.UTFØRT)

        val faktaFeilutbetaling = faktaFeilutbetalingRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
        faktaFeilutbetaling.shouldNotBeNull()
        faktaFeilutbetaling.begrunnelse shouldBe Constants.AUTOMATISK_SAKSBEHANDLING_BEGRUNNELSE
        faktaFeilutbetaling.perioder.shouldHaveSingleElement {
            Hendelsestype.ANNET == it.hendelsestype &&
                Hendelsesundertype.ANNET_FRITEKST == it.hendelsesundertype
        }

        foreldelsesRepository.findByBehandlingIdAndAktivIsTrue(behandling.id).shouldBeEmpty()

        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
            .singleOrNull()
            .shouldNotBeNull()
        vilkårsvurdering.perioder.forOne {
            it.begrunnelse shouldBe Constants.AUTOMATISK_SAKSBEHANDLING_BEGRUNNELSE
            it.vilkårsvurderingsresultat shouldBe Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT
            it.aktsomhet.shouldNotBeNull()
            it.aktsomhet.aktsomhet shouldBe Aktsomhet.SIMPEL_UAKTSOMHET
            it.aktsomhet.tilbakekrevSmåbeløp shouldBe false
        }

        vedtaksbrevsoppsummeringRepository.findByBehandlingId(behandling.id).shouldBeNull()
    }

    private fun lagBehandlingsstegstilstand(
        behandlingssteg: Behandlingssteg,
        behandlingsstegstatus: Behandlingsstegstatus,
    ): Behandlingsstegstilstand =
        Behandlingsstegstilstand(
            behandlingId = behandling.id,
            behandlingssteg = behandlingssteg,
            behandlingsstegsstatus = behandlingsstegstatus,
        )

    private fun lagTask(): Task = Task(type = AutomatiskSaksbehandlingTask.TYPE, payload = behandling.id.toString())

    private fun assertBehandlingsstegstilstand(
        behandlingsstegstilstand: List<Behandlingsstegstilstand>,
        behandlingssteg: Behandlingssteg,
        behandlingsstegstatus: Behandlingsstegstatus,
    ) {
        behandlingsstegstilstand
            .any {
                behandlingssteg == it.behandlingssteg &&
                    behandlingsstegstatus == it.behandlingsstegsstatus
            }.shouldBeTrue()
    }

    private fun mockTaskExecution() {
        val sendVedtakTilØkonomiTask =
            Task(
                type = SendØkonomiTilbakekrevingsvedtakTask.TYPE,
                payload = behandling.id.toString(),
                properties =
                    Properties().apply {
                        setProperty(
                            "ansvarligSaksbehandler",
                            ContextService.hentSaksbehandler(SecureLog.Context.tom()),
                        )
                    },
            )
        every { taskService.save(sendVedtakTilØkonomiTask, SecureLog.Context.tom()) }.run {
            sendØkonomiTilbakekrevingsvedtakTask.doTask(sendVedtakTilØkonomiTask)
        }

        val vedtaksbrevTask = Task(type = SendVedtaksbrevTask.TYPE, payload = behandling.id.toString())
        every { taskService.save(vedtaksbrevTask, SecureLog.Context.tom()) }.run {
            sendVedtaksbrevTask.doTask(vedtaksbrevTask)
        }

        val avsluttTask = Task(type = AvsluttBehandlingTask.TYPE, payload = behandling.id.toString())
        every { taskService.save(avsluttTask, SecureLog.Context.tom()) }.run { avsluttBehandlingTask.doTask(avsluttTask) }
    }
}
