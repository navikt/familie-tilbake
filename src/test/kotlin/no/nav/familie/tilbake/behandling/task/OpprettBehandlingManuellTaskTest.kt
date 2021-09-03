package no.nav.familie.tilbake.behandling.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.tilbakekreving.Faktainfo
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandling
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandlingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandlingRespons
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingManuellOpprettelseService
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingService
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.FagsystemUtil
import no.nav.familie.tilbake.behandling.HentFagsystemsbehandlingRequestSendtRepository
import no.nav.familie.tilbake.behandling.HentFagsystemsbehandlingService
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.integration.kafka.DefaultKafkaProducer
import no.nav.familie.tilbake.integration.kafka.KafkaProducer
import no.nav.familie.tilbake.kravgrunnlag.task.FinnKravgrunnlagTask
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattRepository
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.util.concurrent.SettableListenableFuture
import java.time.LocalDate
import java.util.Properties
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class OpprettBehandlingManuellTaskTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var requestSendtRepository: HentFagsystemsbehandlingRequestSendtRepository

    @Autowired
    private lateinit var økonomiXmlMottattRepository: ØkonomiXmlMottattRepository

    @Autowired
    private lateinit var taskRepository: TaskRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingService: BehandlingService

    private val mockKafkaTemplate: KafkaTemplate<String, String> = mockk()
    private lateinit var spyKafkaProducer: KafkaProducer

    private lateinit var hentFagsystemsbehandlingService: HentFagsystemsbehandlingService
    private lateinit var behandlingManuellOpprettelseService: BehandlingManuellOpprettelseService
    private lateinit var opprettBehandlingManueltTask: OpprettBehandlingManueltTask

    private val requestIdSlot = slot<UUID>()
    private val hentFagsystemsbehandlingRequestSlot = slot<HentFagsystemsbehandlingRequest>()

    private val eksternFagsakId = "testverdi"
    private val ytelsestype = Ytelsestype.BARNETRYGD
    private val eksternId = "testverdi"
    private val ansvarligSaksbehandler = "Z0000"

    @BeforeEach
    fun init() {
        spyKafkaProducer = spyk(DefaultKafkaProducer(mockKafkaTemplate))
        hentFagsystemsbehandlingService = HentFagsystemsbehandlingService(requestSendtRepository, spyKafkaProducer)
        behandlingManuellOpprettelseService = BehandlingManuellOpprettelseService(behandlingService)
        opprettBehandlingManueltTask = OpprettBehandlingManueltTask(hentFagsystemsbehandlingService,
                                                                    behandlingManuellOpprettelseService)

        val future = SettableListenableFuture<SendResult<String, String>>()
        every { mockKafkaTemplate.send(any<ProducerRecord<String, String>>()) }.returns(future)
    }

    @AfterEach
    fun tearDown() {
        requestSendtRepository.deleteAll()
    }

    @Test
    fun `preCondition skal sende hentFagsystemsbehandling request`() {
        opprettBehandlingManueltTask.preCondition(lagTask())

        verify {
            spyKafkaProducer.sendHentFagsystemsbehandlingRequest(capture(requestIdSlot),
                                                                 capture(hentFagsystemsbehandlingRequestSlot))
        }
        val requestId = requestIdSlot.captured
        val requestSendt = requestSendtRepository
                .findByEksternFagsakIdAndYtelsestypeAndEksternId(eksternFagsakId,
                                                                 ytelsestype,
                                                                 eksternId)
        assertNotNull(requestSendt)
        assertEquals(requestId, requestSendt.id)
        assertEquals(eksternFagsakId, requestSendt.eksternFagsakId)
        assertEquals(ytelsestype, requestSendt.ytelsestype)
        assertEquals(eksternId, requestSendt.eksternId)
        assertNull(requestSendt.respons)
    }

    @Test
    fun `doTask skal ikke opprette behandling når responsen ikke har mottatt fra fagsystem`() {
        opprettBehandlingManueltTask.preCondition(lagTask())

        val exception = assertFailsWith<RuntimeException> { opprettBehandlingManueltTask.doTask(lagTask()) }
        assertEquals("HentFagsystemsbehandling respons-en har ikke mottatt fra fagsystem for " +
                     "eksternFagsakId=$eksternFagsakId,ytelsestype=$ytelsestype," +
                     "eksternId=$eksternId." +
                     "Task-en kan kjøre på nytt manuelt når respons-en er mottatt", exception.message)

        val requestSendt = requestSendtRepository
                .findByEksternFagsakIdAndYtelsestypeAndEksternId(eksternFagsakId,
                                                                 ytelsestype,
                                                                 eksternId)
        assertNotNull(requestSendt)
    }

    @Test
    fun `doTask skal ikke opprette behandling når responsen har mottatt fra fagsystem men finnes ikke kravgrunnlag`() {
        opprettBehandlingManueltTask.preCondition(lagTask())

        val requestSendt = requestSendtRepository
                .findByEksternFagsakIdAndYtelsestypeAndEksternId(eksternFagsakId,
                                                                 ytelsestype,
                                                                 eksternId)
        val respons = lagHentFagsystemsbehandlingRespons()
        requestSendt?.let { requestSendtRepository.update(it.copy(respons = objectMapper.writeValueAsString(respons))) }

        val exception = assertFailsWith<RuntimeException> { opprettBehandlingManueltTask.doTask(lagTask()) }
        assertEquals("Det finnes intet kravgrunnlag for ytelsestype=$ytelsestype,eksternFagsakId=$eksternFagsakId " +
                     "og eksternId=$eksternId. Tilbakekrevingsbehandling kan ikke opprettes manuelt.", exception.message)
    }

    @Test
    fun `doTask skal opprette behandling når responsen har mottatt fra fagsystem og finnes kravgrunnlag`() {
        opprettBehandlingManueltTask.preCondition(lagTask())

        val requestSendt = requestSendtRepository
                .findByEksternFagsakIdAndYtelsestypeAndEksternId(eksternFagsakId,
                                                                 ytelsestype,
                                                                 eksternId)
        val respons = lagHentFagsystemsbehandlingRespons()
        requestSendt?.let { requestSendtRepository.update(it.copy(respons = objectMapper.writeValueAsString(respons))) }

        val økonomiXmlMottatt = Testdata.økonomiXmlMottatt
        økonomiXmlMottattRepository.insert(økonomiXmlMottatt.copy(eksternFagsakId = eksternFagsakId, referanse = eksternId))

        assertDoesNotThrow { opprettBehandlingManueltTask.doTask(lagTask()) }

        assertTrue { taskRepository.findAll().any { FinnKravgrunnlagTask.TYPE == it.type } }

        val behandling = behandlingRepository.finnÅpenTilbakekrevingsbehandling(ytelsestype, eksternFagsakId)
        assertNotNull(behandling)
        assertTrue { behandling.manueltOpprettet }
        assertNull(behandling.aktivtVarsel)
        assertNull(behandling.aktivVerge)
        assertEquals(eksternId, behandling.aktivFagsystemsbehandling.eksternId)

        val fagsystemsbehandling = respons.hentFagsystemsbehandling
        assertNotNull(fagsystemsbehandling)
        assertEquals(fagsystemsbehandling.faktainfo.revurderingsresultat, behandling.aktivFagsystemsbehandling.resultat)
        assertEquals(fagsystemsbehandling.faktainfo.revurderingsårsak, behandling.aktivFagsystemsbehandling.årsak)
        assertEquals(fagsystemsbehandling.enhetId, behandling.behandlendeEnhet)
        assertEquals(fagsystemsbehandling.enhetsnavn, behandling.behandlendeEnhetsNavn)
        assertEquals("bb1234", behandling.ansvarligSaksbehandler)
        assertNull(behandling.ansvarligBeslutter)
        assertEquals(Behandlingsstatus.UTREDES, behandling.status)

        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        assertEquals(fagsystemsbehandling.språkkode, fagsak.bruker.språkkode)
        assertEquals(FagsystemUtil.hentFagsystemFraYtelsestype(fagsystemsbehandling.ytelsestype), fagsak.fagsystem)

    }

    private fun lagTask(): Task {
        return Task(type = OpprettBehandlingManueltTask.TYPE,
                    payload = "",
                    properties = Properties().apply {
                        setProperty("eksternFagsakId", eksternFagsakId)
                        setProperty("ytelsestype", ytelsestype.name)
                        setProperty("eksternId", eksternId)
                        setProperty("ansvarligSaksbehandler", ansvarligSaksbehandler)
                    })
    }

    private fun lagHentFagsystemsbehandlingRespons(): HentFagsystemsbehandlingRespons {
        val fagsystemsbehandling = HentFagsystemsbehandling(eksternFagsakId = eksternFagsakId,
                                                            ytelsestype = ytelsestype,
                                                            eksternId = eksternId,
                                                            personIdent = "testverdi",
                                                            språkkode = Språkkode.NB,
                                                            enhetId = "8020",
                                                            enhetsnavn = "testverdi",
                                                            revurderingsvedtaksdato = LocalDate.now(),
                                                            faktainfo = Faktainfo(revurderingsårsak = "testverdi",
                                                                                  revurderingsresultat = "OPPHØR",
                                                                                  tilbakekrevingsvalg = Tilbakekrevingsvalg
                                                                                          .IGNORER_TILBAKEKREVING))
        return HentFagsystemsbehandlingRespons(hentFagsystemsbehandling = fagsystemsbehandling)
    }
}
