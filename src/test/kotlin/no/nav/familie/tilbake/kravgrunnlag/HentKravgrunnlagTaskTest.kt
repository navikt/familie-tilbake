package no.nav.familie.tilbake.kravgrunnlag

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.Applikasjon
import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.kontrakter.felles.historikkinnslag.Historikkinnslagstype
import no.nav.familie.kontrakter.felles.historikkinnslag.OpprettHistorikkinnslagRequest
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.ØkonomiConsumerLokalConfig
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.integration.kafka.KafkaProducer
import no.nav.familie.tilbake.integration.økonomi.ØkonomiConsumer
import no.nav.familie.tilbake.kravgrunnlag.task.HentKravgrunnlagTask
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class HentKravgrunnlagTaskTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var brevsporingRepository: BrevsporingRepository

    private lateinit var kafkaProducer: KafkaProducer
    private lateinit var historikkService: HistorikkService
    private lateinit var økonomiConsumer: ØkonomiConsumer
    private lateinit var hentKravgrunnlagService: HentKravgrunnlagService
    private lateinit var hentKravgrunnlagTask: HentKravgrunnlagTask

    private lateinit var fagsak: Fagsak
    private lateinit var behandling: Behandling

    private val behandlingSlot = slot<UUID>()
    private val historikkinnslagSlot = slot<OpprettHistorikkinnslagRequest>()

    @BeforeEach
    fun init() {
        fagsak = fagsakRepository.insert(Testdata.fagsak)
        behandling = behandlingRepository.insert(Testdata.behandling)
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)

        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(behandling.copy(status = Behandlingsstatus.AVSLUTTET))

        val kafkaTemplate: KafkaTemplate<String, String> = mockk()
        kafkaProducer = spyk(KafkaProducer(kafkaTemplate))
        historikkService = HistorikkService(behandlingRepository, fagsakRepository, brevsporingRepository, kafkaProducer)
        val økonomiService = ØkonomiConsumerLokalConfig.ØkonomiMockService(kravgrunnlagRepository)
        økonomiConsumer = ØkonomiConsumer(økonomiService)
        hentKravgrunnlagService = HentKravgrunnlagService(kravgrunnlagRepository, økonomiConsumer, historikkService)
        hentKravgrunnlagTask = HentKravgrunnlagTask(behandlingRepository, hentKravgrunnlagService)

        every { kafkaProducer.sendHistorikkinnslag(any(), any(), any()) } returns Unit
    }

    @Test
    fun `doTask skal hente kravgrunnlag for revurderingstilbakekreving`() {
        val revurdering = behandlingRepository.insert(Testdata.revurdering)

        assertDoesNotThrow { hentKravgrunnlagTask.doTask(lagTask(revurdering.id)) }
        assertTrue { kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(revurdering.id) }

        verify { kafkaProducer.sendHistorikkinnslag(capture(behandlingSlot), any(), capture(historikkinnslagSlot)) }
        assertEquals(revurdering.id, behandlingSlot.captured)

        val historikkinnslagRequest = historikkinnslagSlot.captured
        assertEquals(Historikkinnslagstype.HENDELSE, historikkinnslagRequest.type)
        assertEquals(revurdering.eksternBrukId.toString(), historikkinnslagRequest.behandlingId)
        assertEquals(fagsak.eksternFagsakId, historikkinnslagRequest.eksternFagsakId)
        assertEquals(Aktør.VEDTAKSLØSNING, historikkinnslagRequest.aktør)
        assertEquals("VL", historikkinnslagRequest.aktørIdent)
        assertEquals(Applikasjon.FAMILIE_TILBAKE, historikkinnslagRequest.applikasjon)
        assertEquals(TilbakekrevingHistorikkinnslagstype.KRAVGRUNNLAG_HENT.tittel, historikkinnslagRequest.tittel)
        assertEquals(LocalDate.now(), historikkinnslagRequest.opprettetTidspunkt.toLocalDate())
    }

    private fun lagTask(behandlingId: UUID): Task {
        return Task(type = HentKravgrunnlagTask.TYPE,
                    payload = behandlingId.toString())
    }
}