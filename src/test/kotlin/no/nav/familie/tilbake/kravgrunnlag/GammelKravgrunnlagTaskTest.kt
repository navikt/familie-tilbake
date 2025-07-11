package no.nav.familie.tilbake.kravgrunnlag

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingService
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.HentFagsystemsbehandlingRequestSendtRepository
import no.nav.familie.tilbake.behandling.HentFagsystemsbehandlingService
import no.nav.familie.tilbake.behandling.domain.HentFagsystemsbehandlingRequestSendt
import no.nav.familie.tilbake.behandling.steg.StegService
import no.nav.familie.tilbake.behandling.task.TracableTaskService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.common.exceptionhandler.IntegrasjonException
import no.nav.familie.tilbake.common.exceptionhandler.KravgrunnlagIkkeFunnetFeil
import no.nav.familie.tilbake.common.exceptionhandler.SperretKravgrunnlagFeil
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.historikkinnslag.HistorikkinnslagRepository
import no.nav.familie.tilbake.integration.kafka.KafkaProducer
import no.nav.familie.tilbake.kontrakter.objectMapper
import no.nav.familie.tilbake.kravgrunnlag.batch.GammelKravgrunnlagService
import no.nav.familie.tilbake.kravgrunnlag.batch.GammelKravgrunnlagTask
import no.nav.familie.tilbake.kravgrunnlag.domain.ØkonomiXmlMottatt
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.kontrakter.Faktainfo
import no.nav.tilbakekreving.kontrakter.HentFagsystemsbehandling
import no.nav.tilbakekreving.kontrakter.HentFagsystemsbehandlingRespons
import no.nav.tilbakekreving.kontrakter.Tilbakekrevingsvalg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate
import java.util.UUID

internal class GammelKravgrunnlagTaskTest : OppslagSpringRunnerTest() {
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
    private lateinit var kravgrunnlagService: KravgrunnlagService

    @Autowired
    private lateinit var taskService: TracableTaskService

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

    @Autowired
    private lateinit var historikkinnslagRepository: HistorikkinnslagRepository

    private val mockHentKravgrunnlagService: HentKravgrunnlagService = mockk()

    private lateinit var historikkService: HistorikkService
    private lateinit var gammelKravgrunnlagService: GammelKravgrunnlagService
    private lateinit var hentFagsystemsbehandlingService: HentFagsystemsbehandlingService
    private lateinit var gammelKravgrunnlagTask: GammelKravgrunnlagTask

    private var xmlMottatt: ØkonomiXmlMottatt = Testdata.getøkonomiXmlMottatt()
    private lateinit var mottattXMl: String
    private lateinit var mottattXmlId: UUID

    @BeforeEach
    fun init() {
        mottattXMl = readKravgrunnlagXmlMedIkkeForeldetDato("/kravgrunnlagxml/kravgrunnlag_BA_riktig_eksternfagsakId_ytelsestype.xml")
        xmlMottatt = xmlMottattRepository.insert(Testdata.getøkonomiXmlMottatt().copy(melding = mottattXMl))
        mottattXmlId = xmlMottatt.id

        val kafkaProducer: KafkaProducer = mockk()
        historikkService = HistorikkService(behandlingRepository, brevSporingRepository, historikkinnslagRepository)
        gammelKravgrunnlagService =
            GammelKravgrunnlagService(
                behandlingRepository,
                kravgrunnlagRepository,
                behandlingService,
                behandlingskontrollService,
                økonomiXmlMottattService,
                mockHentKravgrunnlagService,
                kravgrunnlagService,
                stegService,
                historikkService,
            )
        hentFagsystemsbehandlingService = spyk(HentFagsystemsbehandlingService(requestSendtRepository, kafkaProducer))
        gammelKravgrunnlagTask =
            GammelKravgrunnlagTask(gammelKravgrunnlagService, hentFagsystemsbehandlingService)

        every { kafkaProducer.sendHentFagsystemsbehandlingRequest(any(), any(), any()) } returns Unit
    }

    @AfterEach
    fun tearDown() {
        requestSendtRepository.deleteAll()
    }

    @Test
    fun `doTask skal kaste exception når fagsystemsbehandling ikke finnes i fagsystem`() {
        requestSendtRepository.insert(
            HentFagsystemsbehandlingRequestSendt(
                eksternFagsakId = xmlMottatt.eksternFagsakId,
                ytelsestype = xmlMottatt.ytelsestype,
                eksternId = xmlMottatt.referanse,
            ),
        )
        val exception = shouldThrow<RuntimeException> { gammelKravgrunnlagTask.doTask(lagTask()) }
        exception.message shouldBe "HentFagsystemsbehandling respons-en har ikke mottatt fra fagsystem for " +
            "eksternFagsakId=${xmlMottatt.eksternFagsakId},ytelsestype=${xmlMottatt.ytelsestype}," +
            "eksternId=${xmlMottatt.referanse}.Task-en kan kjøre på nytt manuelt når respons-en er mottatt."
    }

    @Test
    fun `doTask skal opprette en behandling og koble kravgrunnlag med behandlingen`() {
        requestSendtRepository
            .insert(
                HentFagsystemsbehandlingRequestSendt(
                    eksternFagsakId = xmlMottatt.eksternFagsakId,
                    ytelsestype = xmlMottatt.ytelsestype,
                    eksternId = xmlMottatt.referanse,
                    respons = lagHentFagsystemsbehandlingRespons(xmlMottatt),
                ),
            )

        val hentetKravgrunnlag = KravgrunnlagUtil.unmarshalKravgrunnlag(mottattXMl)

        every { mockHentKravgrunnlagService.hentKravgrunnlagFraØkonomi(any(), any(), any()) } returns hentetKravgrunnlag

        gammelKravgrunnlagTask.doTask(lagTask())

        val behandling =
            behandlingRepository.finnÅpenTilbakekrevingsbehandling(
                xmlMottatt.ytelsestype,
                hentetKravgrunnlag.fagsystemId,
            )
        behandling.shouldNotBeNull()
        kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(behandling.id).shouldBeTrue()

        val behandlingsstegstilstand = behandlingstilstandRepository.findByBehandlingId(behandling.id)
        assertSteg(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertSteg(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)

        xmlMottattRepository.findByIdOrNull(mottattXmlId).shouldBeNull()
        xmlMottattArkivRepository
            .findByEksternFagsakIdAndYtelsestype(
                xmlMottatt.eksternFagsakId,
                xmlMottatt.ytelsestype,
            ).shouldNotBeNull()
    }

    @Test
    fun `doTask skal ikke feile hvis det finnes en åpen behandling`() {
        requestSendtRepository
            .insert(
                HentFagsystemsbehandlingRequestSendt(
                    eksternFagsakId = xmlMottatt.eksternFagsakId,
                    ytelsestype = xmlMottatt.ytelsestype,
                    eksternId = xmlMottatt.referanse,
                    respons = lagHentFagsystemsbehandlingRespons(xmlMottatt),
                ),
            )

        val hentetKravgrunnlag = KravgrunnlagUtil.unmarshalKravgrunnlag(mottattXMl)

        every { mockHentKravgrunnlagService.hentKravgrunnlagFraØkonomi(any(), any(), any()) } returns hentetKravgrunnlag

        fagsakRepository.insert(Testdata.fagsak.copy(eksternFagsakId = xmlMottatt.eksternFagsakId))
        val lagretBehandling = behandlingRepository.insert(Testdata.lagBehandling())
        behandlingskontrollService.fortsettBehandling(lagretBehandling.id, SecureLog.Context.tom())
        stegService.håndterSteg(lagretBehandling.id, SecureLog.Context.tom())

        gammelKravgrunnlagTask.doTask(lagTask())

        val behandling =
            behandlingRepository.finnÅpenTilbakekrevingsbehandling(
                xmlMottatt.ytelsestype,
                hentetKravgrunnlag.fagsystemId,
            )
        behandling.shouldNotBeNull()
        kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(behandling.id).shouldBeTrue()

        xmlMottattRepository.findByIdOrNull(mottattXmlId).shouldBeNull()
        xmlMottattArkivRepository
            .findByEksternFagsakIdAndYtelsestype(
                xmlMottatt.eksternFagsakId,
                xmlMottatt.ytelsestype,
            ).shouldNotBeNull()
    }

    @Test
    fun `doTask skal opprette en behandling og venter på kravgrunnlag når hentet kravgrunnlag er sperret hos økonomi`() {
        requestSendtRepository
            .insert(
                HentFagsystemsbehandlingRequestSendt(
                    eksternFagsakId = xmlMottatt.eksternFagsakId,
                    ytelsestype = xmlMottatt.ytelsestype,
                    eksternId = xmlMottatt.referanse,
                    respons = lagHentFagsystemsbehandlingRespons(xmlMottatt),
                ),
            )

        val hentetKravgrunnlag = KravgrunnlagUtil.unmarshalKravgrunnlag(mottattXMl)

        every { mockHentKravgrunnlagService.hentKravgrunnlagFraØkonomi(any(), any(), any()) } throws
            SperretKravgrunnlagFeil("Hentet kravgrunnlag er sperret", logContext = SecureLog.Context.tom())

        gammelKravgrunnlagTask.doTask(lagTask())
        val behandling =
            behandlingRepository.finnÅpenTilbakekrevingsbehandling(
                xmlMottatt.ytelsestype,
                hentetKravgrunnlag.fagsystemId,
            )
        behandling.shouldNotBeNull()
        kravgrunnlagRepository.existsByBehandlingIdAndAktivTrueAndSperretTrue(behandling.id).shouldBeTrue()

        val behandlingsstegstilstand = behandlingstilstandRepository.findByBehandlingId(behandling.id)
        assertSteg(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.VENTER)

        xmlMottattRepository.findByIdOrNull(mottattXmlId).shouldBeNull()
        xmlMottattArkivRepository
            .findByEksternFagsakIdAndYtelsestype(
                xmlMottatt.eksternFagsakId,
                xmlMottatt.ytelsestype,
            ).shouldNotBeNull()
    }

    @Test
    fun `doTask skal kaste exception når kravgrunnlag ikke finnes hos økonomi`() {
        requestSendtRepository
            .insert(
                HentFagsystemsbehandlingRequestSendt(
                    eksternFagsakId = xmlMottatt.eksternFagsakId,
                    ytelsestype = xmlMottatt.ytelsestype,
                    eksternId = xmlMottatt.referanse,
                    respons = lagHentFagsystemsbehandlingRespons(xmlMottatt),
                ),
            )

        every { mockHentKravgrunnlagService.hentKravgrunnlagFraØkonomi(any(), any(), any()) } throws
            IntegrasjonException("Kravgrunnlag finnes ikke i økonomi", logContext = SecureLog.Context.tom())

        val exception = shouldThrow<RuntimeException> { gammelKravgrunnlagTask.doTask(lagTask()) }
        exception.message shouldBe "Kravgrunnlag finnes ikke i økonomi"
    }

    @Test
    fun `doTask skal arkivere kravgrunnlag som ikke finnes hos økonomi dersom det finnes nyere duplikat med kravstatus AVSL`() {
        requestSendtRepository
            .insert(
                HentFagsystemsbehandlingRequestSendt(
                    eksternFagsakId = xmlMottatt.eksternFagsakId,
                    ytelsestype = xmlMottatt.ytelsestype,
                    eksternId = xmlMottatt.referanse,
                    respons = lagHentFagsystemsbehandlingRespons(xmlMottatt),
                ),
            )

        økonomiXmlMottattService.arkiverMottattXml(
            mottattXmlId = null,
            mottattXml =
                mottattXMl
                    .replace("<urn:vedtakId>", "<urn:vedtakId>2")
                    .replace("<urn:kravgrunnlagId>", "<urn:kravgrunnlagId>2")
                    .replace("<urn:kontrollfelt>", "<urn:kontrollfelt>2"),
            fagsystemId = xmlMottatt.eksternFagsakId,
            ytelsestype = xmlMottatt.ytelsestype,
        )

        økonomiXmlMottattService.arkiverMottattXml(
            mottattXmlId = null,
            mottattXml =
                readXml("/kravvedtakstatusxml/statusmelding_AVSL_BA.xml")
                    .replace("<urn:vedtakId>", "<urn:vedtakId>2"),
            fagsystemId = xmlMottatt.eksternFagsakId,
            ytelsestype = xmlMottatt.ytelsestype,
        )

        every { mockHentKravgrunnlagService.hentKravgrunnlagFraØkonomi(any(), any(), any()) } throws
            KravgrunnlagIkkeFunnetFeil(melding = "Noe gikk galt", logContext = SecureLog.Context.tom())

        val task = lagTask()
        gammelKravgrunnlagTask.doTask(task)

        val arkiverteKravgrunnlag =
            økonomiXmlMottattService.hentArkiverteMottattXml(xmlMottatt.eksternFagsakId, xmlMottatt.ytelsestype)

        arkiverteKravgrunnlag.size shouldBe 3
        arkiverteKravgrunnlag.shouldHaveSingleElement { it.melding == mottattXMl }
    }

    private fun lagTask(): Task = taskService.save(Task(type = GammelKravgrunnlagTask.TYPE, payload = mottattXmlId.toString()), SecureLog.Context.tom())

    private fun lagHentFagsystemsbehandlingRespons(xmlMottatt: ØkonomiXmlMottatt): String {
        val fagsystemsbehandling =
            HentFagsystemsbehandling(
                eksternFagsakId = xmlMottatt.eksternFagsakId,
                ytelsestype = xmlMottatt.ytelsestype.tilDTO(),
                eksternId = xmlMottatt.referanse,
                personIdent = "testverdi",
                språkkode = Språkkode.NB,
                enhetId = "8020",
                enhetsnavn = "testverdi",
                revurderingsvedtaksdato = LocalDate.now(),
                faktainfo =
                    Faktainfo(
                        revurderingsårsak = "testverdi",
                        revurderingsresultat = "OPPHØR",
                        tilbakekrevingsvalg =
                            Tilbakekrevingsvalg
                                .IGNORER_TILBAKEKREVING,
                    ),
            )

        return objectMapper.writeValueAsString(
            HentFagsystemsbehandlingRespons(
                hentFagsystemsbehandling = fagsystemsbehandling,
            ),
        )
    }

    private fun assertSteg(
        behandlingsstegstilstand: List<Behandlingsstegstilstand>,
        behandlingssteg: Behandlingssteg,
        behandlingsstegstatus: Behandlingsstegstatus,
    ) {
        behandlingsstegstilstand.shouldHaveSingleElement {
            it.behandlingssteg == behandlingssteg &&
                behandlingsstegstatus == it.behandlingsstegsstatus
        }
    }
}
