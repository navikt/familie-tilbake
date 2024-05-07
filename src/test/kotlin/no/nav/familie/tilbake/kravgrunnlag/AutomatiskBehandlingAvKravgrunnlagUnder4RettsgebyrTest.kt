package no.nav.familie.tilbake.kravgrunnlag

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.batch.AutomatiskSaksbehandlingTask
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.config.Constants.AUTOMATISK_SAKSBEHANDLING_UNDER_4X_RETTSGEBYR_VILKÅRSVURDERING_AKTSOMHET_BEGRUNNELSE
import no.nav.familie.tilbake.config.Constants.AUTOMATISK_SAKSBEHANDLING_UNDER_4X_RETTSGEBYR_VILKÅRSVURDERING_BEGRUNNELSE
import no.nav.familie.tilbake.config.FeatureToggleConfig
import no.nav.familie.tilbake.config.FeatureToggleService
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.felles.task.PubliserJournalpostTask
import no.nav.familie.tilbake.dokumentbestilling.vedtak.SendVedtaksbrevTask
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingRepository
import no.nav.familie.tilbake.foreldelse.ForeldelseService
import no.nav.familie.tilbake.iverksettvedtak.task.SendØkonomiTilbakekrevingsvedtakTask
import no.nav.familie.tilbake.kravgrunnlag.task.BehandleKravgrunnlagTask
import no.nav.familie.tilbake.oppgave.LagOppgaveTask
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.tilbake.vilkårsvurdering.domain.Aktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsresultat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class AutomatiskBehandlingAvKravgrunnlagUnder4RettsgebyrTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var behandleKravgrunnlagTask: BehandleKravgrunnlagTask

    @Autowired
    private lateinit var automatiskSaksbehandlingTask: AutomatiskSaksbehandlingTask

    @Autowired
    private lateinit var sendØkonomiTilbakekrevingsvedtakTask: SendØkonomiTilbakekrevingsvedtakTask

    @Autowired
    private lateinit var sendVedtaksbrevTask: SendVedtaksbrevTask

    @Autowired
    private lateinit var faktaFeilutbetalingRepository: FaktaFeilutbetalingRepository

    @Autowired
    private lateinit var foreldelseService: ForeldelseService

    @Autowired
    private lateinit var vilkårsvurderingService: VilkårsvurderingService

    @Autowired
    private lateinit var featureToggleService: FeatureToggleService

    private lateinit var behandlingId: UUID

    @BeforeEach
    fun init() {
        val fagsak = Testdata.fagsak
        val behandling = Testdata.lagBehandling()
        val copyFagsystemsbehandling = behandling.fagsystemsbehandling.first().copy(tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_AUTOMATISK)
        val automatiskBehandling = behandling.copy(fagsystemsbehandling = setOf(copyFagsystemsbehandling))
        val fagsakOvergangsstønad = fagsak.copy(ytelsestype = Ytelsestype.OVERGANGSSTØNAD)
        fagsakRepository.insert(fagsakOvergangsstønad)
        behandlingId = behandlingRepository.insert(automatiskBehandling).id

        every { featureToggleService.isEnabled(FeatureToggleConfig.AUTOMATISK_BEHANDLE_TILBAKEKREVING_UNDER_4X_RETTSGEBYR) } returns true
    }

    @Test
    fun `Skal knytte kravgrunnlag til åpen behandling under 4x rettsgebyr og behandles automatisk`() {
        lagGrunnlagssteg()

        val kravgrunnlagXml = readXml("/kravgrunnlagxml/kravgrunnlag_EF_under_4x_rettsgebyr.xml")
        val task = opprettTask(kravgrunnlagXml)
        behandleKravgrunnlagTask.doTask(task)

        val automatiskSaksbehandlingTasks = taskService.finnAlleTaskerMedPayloadOgType(behandlingId.toString(), AutomatiskSaksbehandlingTask.TYPE)
        automatiskSaksbehandlingTasks.size shouldBe 1
        automatiskSaksbehandlingTask.doTask(automatiskSaksbehandlingTasks.first())

        assertFakta()
        foreldelseService.hentAktivVurdertForeldelse(behandlingId)?.foreldelsesperioder shouldBe null

        assertVilkårsvurdering()
        sendVedtaksbrev()
        assertAtVedtaksbrevTaskErFullført()
    }

    @Test
    fun `Skal ikke behandle feilutbetalinger over 4x rettsgebyr automatisk`() {
        lagGrunnlagssteg()

        val kravgrunnlagXml = readXml("/kravgrunnlagxml/kravgrunnlag_EF_over_4x_rettsgebyr.xml")

        val task = opprettTask(kravgrunnlagXml)
        behandleKravgrunnlagTask.doTask(task)

        val automatiskSaksbehandlingTasks = taskService.finnAlleTaskerMedPayloadOgType(behandlingId.toString(), AutomatiskSaksbehandlingTask.TYPE)
        automatiskSaksbehandlingTasks.size shouldBe 0
        val lagOppgaveTask = taskService.finnAlleTaskerMedPayloadOgType(behandlingId.toString(), LagOppgaveTask.TYPE)
        lagOppgaveTask.first().payload shouldBe behandlingId.toString()
    }

    @Test
    fun `Skal behandle automatisk selv om det finnes foreldet periode i kravgrunnlag`() {
        lagGrunnlagssteg()

        val kravgrunnlagXml = readXml("/kravgrunnlagxml/kravgrunnlag_EF_under_4x_rettsgebyr_foreldet.xml")
        val task = opprettTask(kravgrunnlagXml)
        behandleKravgrunnlagTask.doTask(task)
        val automatiskSaksbehandlingTasks = taskService.finnAlleTaskerMedPayloadOgType(behandlingId.toString(), AutomatiskSaksbehandlingTask.TYPE)

        automatiskSaksbehandlingTask.doTask(automatiskSaksbehandlingTasks.first())
        val vurdertForeldelse = foreldelseService.hentAktivVurdertForeldelse(behandlingId)
        vurdertForeldelse?.foreldelsesperioder?.size shouldBe 1
        vurdertForeldelse?.foreldelsesperioder?.first()?.begrunnelse shouldBe Constants.AUTOMATISK_SAKSBEHANDLING_UNDER_4X_RETTSGEBYR_FORELDELSE_BEGRUNNELSE
    }

    private fun lagGrunnlagssteg() {
        behandlingsstegstilstandRepository
            .insert(
                Behandlingsstegstilstand(
                    behandlingId = behandlingId,
                    behandlingssteg = Behandlingssteg.GRUNNLAG,
                    behandlingsstegsstatus = Behandlingsstegstatus.VENTER,
                    venteårsak = Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG,
                    tidsfrist = LocalDate.now().plusWeeks(4),
                ),
            )
    }

    private fun opprettTask(kravgrunnlagXml: String): Task {
        return taskService.save(
            Task(
                type = BehandleKravgrunnlagTask.TYPE,
                payload = kravgrunnlagXml,
            ),
        )
    }

    private fun assertFakta() {
        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        behandlingsstegstilstand.any { it.behandlingssteg == Behandlingssteg.IVERKSETT_VEDTAK } shouldBe true
        val fakta = faktaFeilutbetalingRepository.findFaktaFeilutbetalingByBehandlingIdAndAktivIsTrue(behandlingId)
        fakta.begrunnelse shouldBe Constants.AUTOMATISK_SAKSBEHANDLING_UNDER_4X_RETTSGEBYR_FAKTA_BEGRUNNELSE
    }

    private fun sendVedtaksbrev() {
        val sendØkonomiTilbakekrevingsvedtak = taskService.finnAlleTaskerMedPayloadOgType(behandlingId.toString(), SendØkonomiTilbakekrevingsvedtakTask.TYPE).first()
        sendØkonomiTilbakekrevingsvedtakTask.onCompletion(sendØkonomiTilbakekrevingsvedtak)
        val sendBrevTask = taskService.finnAlleTaskerMedPayloadOgType(behandlingId.toString(), SendVedtaksbrevTask.TYPE).first()
        sendVedtaksbrevTask.doTask(sendBrevTask)
    }

    private fun assertVilkårsvurdering() {
        val vilkårsvurdering = vilkårsvurderingService.hentVilkårsvurdering(behandlingId)
        vilkårsvurdering.perioder.size shouldBe 1
        vilkårsvurdering.perioder.first().foreldet shouldBe false // Vil alltid være satt til ikke foreldet ved automatisk behandling
        vilkårsvurdering.perioder.first().begrunnelse shouldBe AUTOMATISK_SAKSBEHANDLING_UNDER_4X_RETTSGEBYR_VILKÅRSVURDERING_BEGRUNNELSE
        vilkårsvurdering.perioder.first().vilkårsvurderingsresultatInfo?.vilkårsvurderingsresultat shouldBe Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT
        vilkårsvurdering.perioder.first().vilkårsvurderingsresultatInfo?.aktsomhet?.tilbakekrevSmåbeløp shouldBe false
        vilkårsvurdering.perioder.first().vilkårsvurderingsresultatInfo?.aktsomhet?.aktsomhet shouldBe Aktsomhet.SIMPEL_UAKTSOMHET
        vilkårsvurdering.perioder.first().vilkårsvurderingsresultatInfo?.aktsomhet?.begrunnelse shouldBe AUTOMATISK_SAKSBEHANDLING_UNDER_4X_RETTSGEBYR_VILKÅRSVURDERING_AKTSOMHET_BEGRUNNELSE
    }

    private fun assertAtVedtaksbrevTaskErFullført() {
        val alleTasker = taskService.findAll()
        val publiserTask = alleTasker.first { it.type == PubliserJournalpostTask.TYPE }
        publiserTask shouldNotBe null // Vil sjekke at brevet er sendt, da publiserJournalpostTask opprettes når det er gjort
    }
}
