package no.nav.familie.tilbake.kravgrunnlag

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.steg.StegService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.vedtak.VedtaksbrevsoppsummeringRepository
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingRepository
import no.nav.familie.tilbake.foreldelse.VurdertForeldelseRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravstatuskode
import no.nav.familie.tilbake.kravgrunnlag.task.BehandleKravgrunnlagTask
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class AutomatiskBehandlingAvKravgrunnlagUnder4Rettsgebyr : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var mottattXmlRepository: ØkonomiXmlMottattRepository

    @Autowired
    private lateinit var mottattXmlArkivRepository: ØkonomiXmlMottattArkivRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var faktaFeilutbetalingRepository: FaktaFeilutbetalingRepository

    @Autowired
    private lateinit var foreldelseRepository: VurdertForeldelseRepository

    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @Autowired
    private lateinit var vedtaksbrevsoppsummeringRepository: VedtaksbrevsoppsummeringRepository

    @Autowired
    private lateinit var behandlingskontrollService: BehandlingskontrollService

    @Autowired
    private lateinit var stegService: StegService

    @Autowired
    private lateinit var behandleKravgrunnlagTask: BehandleKravgrunnlagTask

    private val fagsak = Testdata.fagsak
    private val behandling = Testdata.behandling

    @BeforeEach
    fun init() {
        val fagsakOvergangsstønad = fagsak.copy(ytelsestype = Ytelsestype.OVERGANGSSTØNAD)
        fagsakRepository.insert(fagsakOvergangsstønad)
        behandlingRepository.insert(behandling)
    }

    @Test
    fun `Skal knytte kravgrunnlag til åpen behandling under 4Rettsgebyr og spike`() {
        lagGrunnlagssteg()

        val kravgrunnlagXml = readXml("/kravgrunnlagxml/kravgrunnlag_EF_under_4r.xml")

        val task = opprettTask(kravgrunnlagXml)
        behandleKravgrunnlagTask.doTask(task)

        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
        kravgrunnlag.shouldNotBeNull()
        kravgrunnlag.kravstatuskode shouldBe Kravstatuskode.NYTT
        kravgrunnlag.fagsystemId shouldBe fagsak.eksternFagsakId
        KravgrunnlagUtil.tilYtelsestype(kravgrunnlag.fagområdekode.name) shouldBe Ytelsestype.OVERGANGSSTØNAD

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
    }

    private fun lagGrunnlagssteg() {
        behandlingsstegstilstandRepository
            .insert(
                Behandlingsstegstilstand(
                    behandlingId = behandling.id,
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
}
