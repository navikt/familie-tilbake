package no.nav.familie.tilbake.kravgrunnlag

import io.kotest.inspectors.forOne
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.spyk
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.steg.StegService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.historikkinnslag.Aktør
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.historikkinnslag.HistorikkinnslagRepository
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.integration.kafka.DefaultKafkaProducer
import no.nav.familie.tilbake.integration.kafka.KafkaProducer
import no.nav.familie.tilbake.integration.kafka.KafkaProperties
import no.nav.familie.tilbake.integration.økonomi.MockOppdragClient
import no.nav.familie.tilbake.integration.økonomi.OppdragClient
import no.nav.familie.tilbake.kravgrunnlag.task.HentKravgrunnlagTask
import no.nav.familie.tilbake.log.LogService
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
import no.nav.tilbakekreving.kontrakter.historikk.Historikkinnslagstype
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import java.time.LocalDate
import java.util.UUID

internal class HentKravgrunnlagTaskTest : OppslagSpringRunnerTest() {
    override val tømDBEtterHverTest = false

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var mottattXmlRepository: ØkonomiXmlMottattRepository

    @Autowired
    private lateinit var brevsporingRepository: BrevsporingRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var stegService: StegService

    @Autowired
    private lateinit var historikkinnslagRepository: HistorikkinnslagRepository

    @Autowired
    private lateinit var logService: LogService

    private lateinit var kafkaProducer: KafkaProducer
    private lateinit var historikkService: HistorikkService
    private lateinit var oppdragClient: OppdragClient
    private lateinit var hentKravgrunnlagService: HentKravgrunnlagService
    private lateinit var hentKravgrunnlagTask: HentKravgrunnlagTask

    private lateinit var fagsak: Fagsak
    private lateinit var behandling: Behandling

    @BeforeEach
    fun init() {
        fagsak = fagsakRepository.insert(Testdata.fagsak())
        behandling = behandlingRepository.insert(Testdata.lagBehandling(fagsak.id))
        kravgrunnlagRepository.insert(Testdata.lagKravgrunnlag(behandling.id))

        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(behandling.copy(status = Behandlingsstatus.AVSLUTTET))

        val kafkaTemplate: KafkaTemplate<String, String> = mockk()
        kafkaProducer = spyk(DefaultKafkaProducer(kafkaTemplate, KafkaProperties(KafkaProperties.HentFagsystem("request", "response"))))
        historikkService = HistorikkService(behandlingRepository, brevsporingRepository, historikkinnslagRepository)
        oppdragClient = MockOppdragClient(kravgrunnlagRepository, mottattXmlRepository)
        hentKravgrunnlagService = HentKravgrunnlagService(kravgrunnlagRepository, oppdragClient, historikkService)
        hentKravgrunnlagTask = HentKravgrunnlagTask(behandlingRepository, hentKravgrunnlagService, stegService, logService)
    }

    @Test
    fun `doTask skal hente kravgrunnlag for revurderingstilbakekreving`() {
        val revurdering = behandlingRepository.insert(Testdata.lagRevurdering(behandling.id, fagsak.id))
        behandlingsstegstilstandRepository
            .insert(
                Behandlingsstegstilstand(
                    behandlingId = revurdering.id,
                    behandlingssteg = Behandlingssteg.GRUNNLAG,
                    behandlingsstegsstatus = Behandlingsstegstatus.VENTER,
                    venteårsak = Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG,
                    tidsfrist = LocalDate.now().plusWeeks(3),
                ),
            )

        hentKravgrunnlagTask.doTask(lagTask(revurdering.id))
        kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(revurdering.id).shouldBeTrue()

        historikkinnslagRepository.findByBehandlingId(revurdering.id).forOne {
            it.type shouldBe Historikkinnslagstype.HENDELSE
            it.tittel shouldBe TilbakekrevingHistorikkinnslagstype.KRAVGRUNNLAG_HENT.tittel
            it.tekst shouldBe TilbakekrevingHistorikkinnslagstype.KRAVGRUNNLAG_HENT.tekst
            it.aktør shouldBe Aktør.Vedtaksløsning.type
            it.opprettetAv shouldBe Constants.BRUKER_ID_VEDTAKSLØSNINGEN
        }

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(revurdering.id)
        behandlingsstegstilstand
            .any {
                Behandlingssteg.GRUNNLAG == it.behandlingssteg &&
                    Behandlingsstegstatus.UTFØRT == it.behandlingsstegsstatus
            }.shouldBeTrue()

        behandlingsstegstilstand
            .any {
                Behandlingssteg.FAKTA == it.behandlingssteg &&
                    Behandlingsstegstatus.KLAR == it.behandlingsstegsstatus
            }.shouldBeTrue()
    }

    private fun lagTask(behandlingId: UUID): Task =
        Task(
            type = HentKravgrunnlagTask.TYPE,
            payload = behandlingId.toString(),
        )
}
