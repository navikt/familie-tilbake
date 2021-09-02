package no.nav.familie.tilbake.kravgrunnlag

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.tilbakekreving.Faktainfo
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandlingRespons
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingService
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.HentFagsystemsbehandlingRequestSendtRepository
import no.nav.familie.tilbake.behandling.HentFagsystemsbehandlingService
import no.nav.familie.tilbake.behandling.domain.HentFagsystemsbehandlingRequestSendt
import no.nav.familie.tilbake.behandling.steg.StegService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.common.exceptionhandler.IntegrasjonException
import no.nav.familie.tilbake.common.exceptionhandler.SperretKravgrunnlagFeil
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.integration.kafka.KafkaProducer
import no.nav.familie.tilbake.kravgrunnlag.batch.HåndterGamleKravgrunnlagService
import no.nav.familie.tilbake.kravgrunnlag.batch.HåndterGammelKravgrunnlagTask
import no.nav.familie.tilbake.kravgrunnlag.domain.ØkonomiXmlMottatt
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class HåndterGammelKravgrunnlagTaskTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var brevSporingRepository: BrevsporingRepository

    @Autowired
    private lateinit var xmlMottattRepository: ØkonomiXmlMottattRepository

    @Autowired
    private lateinit var xmlMottattArkivRepository: ØkonomiXmlMottattArkivRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var taskRepository: TaskRepository

    @Autowired
    private lateinit var requestSendtRepository: HentFagsystemsbehandlingRequestSendtRepository

    @Autowired
    private lateinit var behandlingstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var behandlingskontrollService: BehandlingskontrollService

    @Autowired
    private lateinit var økonomiXmlMottattService: ØkonomiXmlMottattService

    @Autowired
    private lateinit var stegService: StegService

    private val mockHentKravgrunnlagService: HentKravgrunnlagService = mockk()

    private lateinit var historikkService: HistorikkService
    private lateinit var håndterGamleKravgrunnlagService: HåndterGamleKravgrunnlagService
    private lateinit var hentFagsystemsbehandlingService: HentFagsystemsbehandlingService
    private lateinit var håndterGammelKravgrunnlagTask: HåndterGammelKravgrunnlagTask

    private var xmlMottatt: ØkonomiXmlMottatt = Testdata.økonomiXmlMottatt
    private lateinit var mottattXMl: String
    private lateinit var mottattXmlId: UUID


    @BeforeEach
    fun init() {
        mottattXMl = readXml("/kravgrunnlagxml/kravgrunnlag_BA_riktig_eksternfagsakId_ytelsestype.xml")
        xmlMottatt = xmlMottattRepository.insert(Testdata.økonomiXmlMottatt.copy(melding = mottattXMl))
        mottattXmlId = xmlMottatt.id

        val kafkaProducer: KafkaProducer = mockk()
        historikkService = HistorikkService(behandlingRepository, fagsakRepository, brevSporingRepository, kafkaProducer)
        håndterGamleKravgrunnlagService = HåndterGamleKravgrunnlagService(behandlingRepository,
                                                                          kravgrunnlagRepository,
                                                                          behandlingService,
                                                                          behandlingskontrollService,
                                                                          økonomiXmlMottattService,
                                                                          mockHentKravgrunnlagService,
                                                                          stegService,
                                                                          historikkService)
        hentFagsystemsbehandlingService = spyk(HentFagsystemsbehandlingService(requestSendtRepository, kafkaProducer))
        håndterGammelKravgrunnlagTask =
                HåndterGammelKravgrunnlagTask(håndterGamleKravgrunnlagService, hentFagsystemsbehandlingService)

        every { kafkaProducer.sendHentFagsystemsbehandlingRequest(any(), any()) } returns Unit
        every { kafkaProducer.sendHistorikkinnslag(any(), any(), any()) } returns Unit
    }

    @AfterEach
    fun tearDown() {
        requestSendtRepository.deleteAll()
    }

    @Test
    fun `doTask skal kaste exception når fagsystemsbehandling ikke finnes i fagsystem`() {
        requestSendtRepository.insert(HentFagsystemsbehandlingRequestSendt(eksternFagsakId = xmlMottatt.eksternFagsakId,
                                                                           ytelsestype = xmlMottatt.ytelsestype,
                                                                           eksternId = xmlMottatt.referanse))
        val exception = assertThrows<RuntimeException> { håndterGammelKravgrunnlagTask.doTask(lagTask()) }
        assertEquals("HentFagsystemsbehandling respons-en har ikke mottatt fra fagsystem for " +
                     "eksternFagsakId=${xmlMottatt.eksternFagsakId},ytelsestype=${xmlMottatt.ytelsestype}," +
                     "eksternId=${xmlMottatt.referanse}.Task-en kan kjøre på nytt manuelt når respons-en er mottatt. " +
                     "Hvis data ikke finnes i fagsystem, " +
                     "må kravgrunnlaget arkiveres manuelt ved å bruke forvaltningsrutine etter feilundersøkelse.",
                     exception.message)
    }

    @Test
    fun `doTask skal opprette en behandling og koble kravgrunnlag med behandlingen`() {
        requestSendtRepository
                .insert(HentFagsystemsbehandlingRequestSendt(eksternFagsakId = xmlMottatt.eksternFagsakId,
                                                             ytelsestype = xmlMottatt.ytelsestype,
                                                             eksternId = xmlMottatt.referanse,
                                                             respons = lagHentFagsystemsbehandlingRespons(xmlMottatt)))


        val hentetKravgrunnlag = KravgrunnlagUtil.unmarshalKravgrunnlag(mottattXMl)

        every { mockHentKravgrunnlagService.hentKravgrunnlagFraØkonomi(any(), any()) } returns hentetKravgrunnlag

        assertDoesNotThrow { håndterGammelKravgrunnlagTask.doTask(lagTask()) }
        val behandling = behandlingRepository.finnÅpenTilbakekrevingsbehandling(xmlMottatt.ytelsestype,
                                                                                hentetKravgrunnlag.fagsystemId)
        assertNotNull(behandling)
        assertTrue { kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(behandling.id) }

        val behandlingsstegstilstand = behandlingstilstandRepository.findByBehandlingId(behandling.id)
        assertSteg(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertSteg(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)

        assertNull(xmlMottattRepository.findByIdOrNull(mottattXmlId))
        assertNotNull(xmlMottattArkivRepository.findByEksternFagsakIdAndYtelsestype(xmlMottatt.eksternFagsakId,
                                                                                    xmlMottatt.ytelsestype))
    }

    @Test
    fun `doTask skal opprette en behandling og venter på kravgrunnlag når hentet kravgrunnlag er sperret hos økonomi`() {
        requestSendtRepository
                .insert(HentFagsystemsbehandlingRequestSendt(eksternFagsakId = xmlMottatt.eksternFagsakId,
                                                             ytelsestype = xmlMottatt.ytelsestype,
                                                             eksternId = xmlMottatt.referanse,
                                                             respons = lagHentFagsystemsbehandlingRespons(xmlMottatt)))


        val hentetKravgrunnlag = KravgrunnlagUtil.unmarshalKravgrunnlag(mottattXMl)

        every { mockHentKravgrunnlagService.hentKravgrunnlagFraØkonomi(any(), any()) } throws
                SperretKravgrunnlagFeil("Hentet kravgrunnlag er sperret")

        assertDoesNotThrow { håndterGammelKravgrunnlagTask.doTask(lagTask()) }
        val behandling = behandlingRepository.finnÅpenTilbakekrevingsbehandling(xmlMottatt.ytelsestype,
                                                                                hentetKravgrunnlag.fagsystemId)
        assertNotNull(behandling)
        assertTrue { kravgrunnlagRepository.existsByBehandlingIdAndAktivTrueAndSperretTrue(behandling.id) }

        val behandlingsstegstilstand = behandlingstilstandRepository.findByBehandlingId(behandling.id)
        assertSteg(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.VENTER)

        assertNull(xmlMottattRepository.findByIdOrNull(mottattXmlId))
        assertNotNull(xmlMottattArkivRepository.findByEksternFagsakIdAndYtelsestype(xmlMottatt.eksternFagsakId,
                                                                                    xmlMottatt.ytelsestype))
    }

    @Test
    fun `doTask skal kaste exception når kravgrunnlag ikke finnes hos økonomi`() {
        requestSendtRepository
                .insert(HentFagsystemsbehandlingRequestSendt(eksternFagsakId = xmlMottatt.eksternFagsakId,
                                                             ytelsestype = xmlMottatt.ytelsestype,
                                                             eksternId = xmlMottatt.referanse,
                                                             respons = lagHentFagsystemsbehandlingRespons(xmlMottatt)))


        every { mockHentKravgrunnlagService.hentKravgrunnlagFraØkonomi(any(), any()) } throws
                IntegrasjonException("Kravgrunnlag finnes ikke i økonomi")

        val exception = assertFailsWith<RuntimeException> { håndterGammelKravgrunnlagTask.doTask(lagTask()) }
        assertEquals("Kravgrunnlag finnes ikke i økonomi", exception.message)
    }


    private fun lagTask(): Task {
        return taskRepository.save(Task(type = HåndterGammelKravgrunnlagTask.TYPE, payload = mottattXmlId.toString()))
    }

    private fun lagHentFagsystemsbehandlingRespons(xmlMottatt: ØkonomiXmlMottatt): String {
        val respons = HentFagsystemsbehandlingRespons(eksternFagsakId = xmlMottatt.eksternFagsakId,
                                                      ytelsestype = xmlMottatt.ytelsestype,
                                                      eksternId = xmlMottatt.referanse,
                                                      personIdent = "testverdi",
                                                      språkkode = Språkkode.NB,
                                                      enhetId = "8020",
                                                      enhetsnavn = "testverdi",
                                                      revurderingsvedtaksdato = LocalDate.now(),
                                                      faktainfo = Faktainfo(revurderingsårsak = "testverdi",
                                                                            revurderingsresultat = "OPPHØR",
                                                                            tilbakekrevingsvalg = Tilbakekrevingsvalg
                                                                                    .IGNORER_TILBAKEKREVING))

        return objectMapper.writeValueAsString(respons)
    }

    private fun assertSteg(behandlingsstegstilstand: List<Behandlingsstegstilstand>,
                           behandlingssteg: Behandlingssteg,
                           behandlingsstegstatus: Behandlingsstegstatus) {
        assertTrue {
            behandlingsstegstilstand.any {
                it.behandlingssteg == behandlingssteg &&
                behandlingsstegstatus == it.behandlingsstegsstatus
            }
        }
    }

}