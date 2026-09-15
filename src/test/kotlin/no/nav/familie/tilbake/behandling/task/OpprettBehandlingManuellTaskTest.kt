package no.nav.familie.tilbake.behandling.task

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.Fagsystem
import no.nav.familie.tilbake.behandling.HentFagsystemsbehandlingRequestSendtRepository
import no.nav.familie.tilbake.behandling.Ytelsestype
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.kontrakter.objectMapper
import no.nav.familie.tilbake.kravgrunnlag.task.FinnKravgrunnlagTask
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattRepository
import no.nav.tilbakekreving.FagsystemUtil
import no.nav.tilbakekreving.kontrakter.Faktainfo
import no.nav.tilbakekreving.kontrakter.HentFagsystemsbehandling
import no.nav.tilbakekreving.kontrakter.HentFagsystemsbehandlingRespons
import no.nav.tilbakekreving.kontrakter.Institusjon
import no.nav.tilbakekreving.kontrakter.Tilbakekrevingsvalg
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.test.FellesTestdata.SAKSBEHANDLER_IDENT
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.Properties
import java.util.UUID

internal class OpprettBehandlingManuellTaskTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var requestSendtRepository: HentFagsystemsbehandlingRequestSendtRepository

    @Autowired
    private lateinit var økonomiXmlMottattRepository: ØkonomiXmlMottattRepository

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var opprettBehandlingManueltTask: OpprettBehandlingManueltTask

    private val ytelsestype = Ytelsestype.BARNETRYGD

    @Test
    fun `preCondition skal sende hentFagsystemsbehandling request`() {
        val eksternFagsakId = UUID.randomUUID().toString()
        val eksternId = UUID.randomUUID().toString()
        opprettBehandlingManueltTask.preCondition(lagTask(eksternFagsakId, eksternId))

        val requestSendt =
            requestSendtRepository
                .findByEksternFagsakIdAndYtelsestypeAndEksternId(
                    eksternFagsakId,
                    ytelsestype,
                    eksternId,
                )
        requestSendt.shouldNotBeNull()
        requestSendt.eksternFagsakId shouldBe eksternFagsakId
        requestSendt.ytelsestype shouldBe ytelsestype
        requestSendt.eksternId shouldBe eksternId
        requestSendt.respons.shouldBeNull()
    }

    @Test
    fun `doTask skal ikke opprette behandling når responsen ikke har mottatt fra fagsystem`() {
        val eksternFagsakId = UUID.randomUUID().toString()
        val eksternId = UUID.randomUUID().toString()
        opprettBehandlingManueltTask.preCondition(lagTask(eksternFagsakId, eksternId))

        val exception = shouldThrow<RuntimeException> { opprettBehandlingManueltTask.doTask(lagTask(eksternFagsakId, eksternId)) }
        exception.message shouldBe "HentFagsystemsbehandling respons-en har ikke mottatt fra fagsystem for " +
            "eksternFagsakId=$eksternFagsakId,ytelsestype=$ytelsestype," +
            "eksternId=$eksternId." +
            "Task-en kan kjøre på nytt manuelt når respons-en er mottatt"

        val requestSendt =
            requestSendtRepository
                .findByEksternFagsakIdAndYtelsestypeAndEksternId(
                    eksternFagsakId,
                    ytelsestype,
                    eksternId,
                )
        requestSendt.shouldNotBeNull()
    }

    @Test
    fun `doTask skal ikke opprette behandling når responsen har mottatt fra fagsystem men finnes ikke kravgrunnlag`() {
        val eksternFagsakId = UUID.randomUUID().toString()
        val eksternId = UUID.randomUUID().toString()
        opprettBehandlingManueltTask.preCondition(lagTask(eksternFagsakId, eksternId))

        val requestSendt =
            requestSendtRepository
                .findByEksternFagsakIdAndYtelsestypeAndEksternId(
                    eksternFagsakId,
                    ytelsestype,
                    eksternId,
                )
        val respons = lagHentFagsystemsbehandlingRespons(eksternFagsakId, eksternId)
        requestSendt?.let { requestSendtRepository.update(it.copy(respons = objectMapper.writeValueAsString(respons))) }

        val exception = shouldThrow<RuntimeException> { opprettBehandlingManueltTask.doTask(lagTask(eksternFagsakId, eksternId)) }
        exception.message shouldBe "Det finnes intet kravgrunnlag for ytelsestype=$ytelsestype,eksternFagsakId=$eksternFagsakId " +
            "og eksternId=$eksternId. Tilbakekrevingsbehandling kan ikke opprettes manuelt."
    }

    @Test
    fun `doTask skal opprette behandling når responsen har mottatt fra fagsystem og finnes kravgrunnlag`() {
        val eksternFagsakId = UUID.randomUUID().toString()
        val eksternId = UUID.randomUUID().toString()
        opprettBehandlingManueltTask.preCondition(lagTask(eksternFagsakId, eksternId))

        val requestSendt = requestSendtRepository.findByEksternFagsakIdAndYtelsestypeAndEksternId(
            eksternFagsakId,
            ytelsestype,
            eksternId,
        )
        val respons = lagHentFagsystemsbehandlingRespons(eksternFagsakId, eksternId)
        requestSendt?.let { requestSendtRepository.update(it.copy(respons = objectMapper.writeValueAsString(respons))) }

        val økonomiXmlMottatt = Testdata.getøkonomiXmlMottatt()
        økonomiXmlMottattRepository.insert(økonomiXmlMottatt.copy(eksternFagsakId = eksternFagsakId, referanse = eksternId))

        opprettBehandlingManueltTask.doTask(lagTask(eksternFagsakId, eksternId))

        taskService.findAll().any { FinnKravgrunnlagTask.TYPE == it.type }.shouldBeTrue()

        val behandling = behandlingRepository.finnÅpenTilbakekrevingsbehandling(ytelsestype, eksternFagsakId)
        behandling.shouldNotBeNull()
        behandling.manueltOpprettet.shouldBeTrue()
        behandling.aktivtVarsel.shouldBeNull()
        behandling.aktivVerge.shouldBeNull()
        behandling.aktivFagsystemsbehandling.eksternId shouldBe eksternId

        val fagsystemsbehandling = respons.hentFagsystemsbehandling
        fagsystemsbehandling.shouldNotBeNull()
        behandling.aktivFagsystemsbehandling.resultat shouldBe fagsystemsbehandling.faktainfo.revurderingsresultat
        behandling.aktivFagsystemsbehandling.årsak shouldBe fagsystemsbehandling.faktainfo.revurderingsårsak
        behandling.behandlendeEnhet shouldBe fagsystemsbehandling.enhetId
        behandling.behandlendeEnhetsNavn shouldBe fagsystemsbehandling.enhetsnavn
        behandling.ansvarligSaksbehandler shouldBe "bb1234"
        behandling.ansvarligBeslutter.shouldBeNull()
        behandling.status shouldBe Behandlingsstatus.UTREDES

        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        fagsak.bruker.språkkode shouldBe fagsystemsbehandling.språkkode
        fagsak.fagsystem shouldBe Fagsystem.forDTO(FagsystemUtil.hentFagsystemFraYtelsestype(fagsystemsbehandling.ytelsestype))
        fagsak.institusjon shouldBe null
    }

    @Test
    fun `doTask skal opprette behandling for institusjon når responsen har mottatt fra fagsystem og finnes kravgrunnlag`() {
        val eksternFagsakId = UUID.randomUUID().toString()
        val eksternId = UUID.randomUUID().toString()
        opprettBehandlingManueltTask.preCondition(lagTask(eksternFagsakId, eksternId))

        val requestSendt =
            requestSendtRepository
                .findByEksternFagsakIdAndYtelsestypeAndEksternId(
                    eksternFagsakId,
                    ytelsestype,
                    eksternId,
                )
        val respons = lagHentFagsystemsbehandlingRespons(eksternFagsakId, eksternId, erInstitusjon = true)
        requestSendt?.let { requestSendtRepository.update(it.copy(respons = objectMapper.writeValueAsString(respons))) }

        val økonomiXmlMottatt = Testdata.getøkonomiXmlMottatt()
        økonomiXmlMottattRepository.insert(økonomiXmlMottatt.copy(eksternFagsakId = eksternFagsakId, referanse = eksternId))

        opprettBehandlingManueltTask.doTask(lagTask(eksternFagsakId, eksternId))

        taskService.findAll().any { FinnKravgrunnlagTask.TYPE == it.type }.shouldBeTrue()

        val behandling = behandlingRepository.finnÅpenTilbakekrevingsbehandling(ytelsestype, eksternFagsakId)
        behandling.shouldNotBeNull()
        behandling.manueltOpprettet.shouldBeTrue()
        behandling.aktivtVarsel.shouldBeNull()
        behandling.aktivVerge.shouldBeNull()
        behandling.aktivFagsystemsbehandling.eksternId shouldBe eksternId

        val fagsystemsbehandling = respons.hentFagsystemsbehandling
        fagsystemsbehandling.shouldNotBeNull()
        behandling.aktivFagsystemsbehandling.resultat shouldBe fagsystemsbehandling.faktainfo.revurderingsresultat
        behandling.aktivFagsystemsbehandling.årsak shouldBe fagsystemsbehandling.faktainfo.revurderingsårsak
        behandling.behandlendeEnhet shouldBe fagsystemsbehandling.enhetId
        behandling.behandlendeEnhetsNavn shouldBe fagsystemsbehandling.enhetsnavn
        behandling.ansvarligSaksbehandler shouldBe "bb1234"
        behandling.ansvarligBeslutter.shouldBeNull()
        behandling.status shouldBe Behandlingsstatus.UTREDES

        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        fagsak.bruker.språkkode shouldBe fagsystemsbehandling.språkkode
        fagsak.fagsystem shouldBe Fagsystem.forDTO(FagsystemUtil.hentFagsystemFraYtelsestype(fagsystemsbehandling.ytelsestype))
        fagsak.institusjon shouldNotBe null
        fagsak.institusjon!!.organisasjonsnummer shouldBe "987654321"
    }

    private fun lagTask(eksternFagsakId: String, eksternId: String): Task =
        Task(
            type = OpprettBehandlingManueltTask.TYPE,
            payload = "",
            properties =
                Properties().apply {
                    setProperty("eksternFagsakId", eksternFagsakId)
                    setProperty("ytelsestype", ytelsestype.name)
                    setProperty("eksternId", eksternId)
                    setProperty("ansvarligSaksbehandler", SAKSBEHANDLER_IDENT)
                },
        )

    private fun lagHentFagsystemsbehandlingRespons(
        eksternFagsakId: String,
        eksternId: String,
        erInstitusjon: Boolean = false,
        feilmelding: String? = null,
    ): HentFagsystemsbehandlingRespons {
        val institusjon = if (erInstitusjon) Institusjon(organisasjonsnummer = "987654321") else null
        val fagsystemsbehandling =
            HentFagsystemsbehandling(
                eksternFagsakId = eksternFagsakId,
                ytelsestype = ytelsestype.tilDTO(),
                eksternId = eksternId,
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
                institusjon = institusjon,
            )
        return HentFagsystemsbehandlingRespons(
            hentFagsystemsbehandling = fagsystemsbehandling,
            feilMelding = feilmelding,
        )
    }
}
