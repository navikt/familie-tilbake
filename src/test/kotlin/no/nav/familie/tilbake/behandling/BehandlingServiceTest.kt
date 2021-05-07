package no.nav.familie.tilbake.behandling

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockkObject
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingstype
import no.nav.familie.kontrakter.felles.tilbakekreving.Faktainfo
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Periode
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.kontrakter.felles.tilbakekreving.Varsel
import no.nav.familie.kontrakter.felles.tilbakekreving.Verge
import no.nav.familie.kontrakter.felles.tilbakekreving.Vergetype
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype.BARNETRYGD
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.api.dto.BehandlingDto
import no.nav.familie.tilbake.api.dto.BehandlingPåVentDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegsinfoDto
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandling.domain.Saksbehandlingstype
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.repository.Sporbar
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.service.dokumentbestilling.felles.domain.Brevsporing
import no.nav.familie.tilbake.service.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.service.dokumentbestilling.henleggelse.SendHenleggelsesbrevTask
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.InnloggetBrukertilgang
import no.nav.familie.tilbake.sikkerhet.Tilgangskontrollsfagsystem
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class BehandlingServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var brevsporingRepository: BrevsporingRepository

    @Autowired
    private lateinit var taskRepository: TaskRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var behandlingskontrollService: BehandlingskontrollService

    private final val fom: LocalDate = LocalDate.now().minusMonths(1)
    private final val tom: LocalDate = LocalDate.now()

    @BeforeEach
    fun init() {
        mockkObject(ContextService)
        every { ContextService.hentSaksbehandler() }.returns("Z0000")
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any(), any()) }.returns(
                InnloggetBrukertilgang(tilganger = mapOf(Tilgangskontrollsfagsystem.SYSTEM_TILGANG to Behandlerrolle.SYSTEM)))
    }

    @AfterEach
    fun tearDown() {
        clearMocks(ContextService)
    }

    @Test
    fun `opprettBehandlingAutomatisk skal opprette automatisk behandling uten verge`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = false,
                                                finnesVarsel = true,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL)

        val behandling = behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest)

        assertBehandling(behandling, opprettTilbakekrevingRequest)
        assertFagsak(behandling, opprettTilbakekrevingRequest)
        assertFagsystemsbehandling(behandling, opprettTilbakekrevingRequest)
        assertVarselData(behandling, opprettTilbakekrevingRequest)
        assertTrue { behandling.verger.isEmpty() }
    }

    @Test
    fun `opprettBehandlingAutomatisk skal opprette automatisk behandling med verge`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = true,
                                                finnesVarsel = true,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL)

        val behandling = behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest)

        assertBehandling(behandling, opprettTilbakekrevingRequest)
        assertFagsak(behandling, opprettTilbakekrevingRequest)
        assertFagsystemsbehandling(behandling, opprettTilbakekrevingRequest)
        assertVarselData(behandling, opprettTilbakekrevingRequest)
        assertVerge(behandling, opprettTilbakekrevingRequest)
    }

    @Test
    fun `opprettBehandlingAutomatisk skal opprette automatisk behandling uten varsel`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = false,
                                                finnesVarsel = false,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL)

        val behandling = behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest)

        assertBehandling(behandling, opprettTilbakekrevingRequest)
        assertFagsak(behandling, opprettTilbakekrevingRequest)
        assertFagsystemsbehandling(behandling, opprettTilbakekrevingRequest)
        assertTrue { behandling.varsler.isEmpty() }
        assertTrue { behandling.verger.isEmpty() }
    }

    @Test
    fun `opprettBehandlingAutomatisk oppretter ikke behandling når det finnes åpen tilbakekreving for samme eksternFagsakId`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = true,
                                                finnesVarsel = true,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL)
        behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest)

        val exception = assertFailsWith<RuntimeException>(block = {
            behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest)
        })
        assertEquals("Det finnes allerede en åpen behandling for ytelsestype="
                     + opprettTilbakekrevingRequest.ytelsestype +
                     " og eksternFagsakId=${opprettTilbakekrevingRequest.eksternFagsakId}, " +
                     "kan ikke opprette en ny.", exception.message)
    }

    @Test
    fun `opprettBehandlingAutomatisk skal ikke opprette automatisk behandling når siste tilbakekreving er ikke henlagt`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = true,
                                                finnesVarsel = true,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL)

        val behandling = behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest)
        val lagretBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(lagretBehandling.copy(status = Behandlingsstatus.AVSLUTTET))

        val exception = assertFailsWith<RuntimeException>(block = {
            behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest)
        })
        assertEquals("Det finnes allerede en avsluttet behandling for ytelsestype="
                     + opprettTilbakekrevingRequest.ytelsestype +
                     " og eksternFagsakId=${opprettTilbakekrevingRequest.eksternFagsakId} " +
                     "som ikke er henlagt, kan ikke opprette en ny.", exception.message)
    }

    @Test
    fun `hentBehandling skal hente behandling som opprettet uten varsel`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = true,
                                                finnesVarsel = true,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL)
        val behandling = behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest)
        val behandlingDto = behandlingService.hentBehandling(behandling.id)

        assertFellesBehandlingRespons(behandlingDto, behandling)
        assertFalse { behandlingDto.kanHenleggeBehandling }
        assertTrue { behandlingDto.harVerge }
        assertTrue { behandlingDto.erBehandlingPåVent }
        assertTrue { behandlingDto.kanEndres }
        assertBehandlingsstegsinfo(behandlingDto = behandlingDto,
                                   behandling = behandling,
                                   behandlingssteg = Behandlingssteg.GRUNNLAG,
                                   behandlingsstegstatus = Behandlingsstegstatus.VENTER,
                                   venteårsak = Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG)
    }

    @Test
    fun `hentBehandling skal hente behandling som ikke kan henlegges med verge`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = true,
                                                finnesVarsel = true,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL)
        val behandling = behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest)
        val behandlingDto = behandlingService.hentBehandling(behandling.id)

        assertFellesBehandlingRespons(behandlingDto, behandling)
        assertFalse { behandlingDto.kanHenleggeBehandling }
        assertTrue { behandlingDto.harVerge }
        assertTrue { behandlingDto.erBehandlingPåVent }
        assertTrue { behandlingDto.kanEndres }
        assertBehandlingsstegsinfo(behandlingDto = behandlingDto,
                                   behandling = behandling,
                                   behandlingssteg = Behandlingssteg.VARSEL,
                                   behandlingsstegstatus = Behandlingsstegstatus.VENTER,
                                   venteårsak = Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING)
    }

    @Test
    fun `hentBehandling skal hente behandling som kan henlegges uten verge`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = false,
                                                finnesVarsel = true,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL)
        val behandling = behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest)
        val lagretBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        val sporbar = behandling.sporbar.copy(opprettetTid = LocalDate.now().minusDays(10).atStartOfDay())
        val oppdatertBehandling = lagretBehandling.copy(sporbar = sporbar)
        behandlingRepository.update(oppdatertBehandling)

        val behandlingDto = behandlingService.hentBehandling(behandling.id)

        assertFellesBehandlingRespons(behandlingDto, oppdatertBehandling)
        assertTrue { behandlingDto.kanHenleggeBehandling }
        assertFalse { behandlingDto.harVerge }
        assertTrue { behandlingDto.erBehandlingPåVent }
        assertTrue { behandlingDto.kanEndres }
        assertBehandlingsstegsinfo(behandlingDto = behandlingDto,
                                   behandling = behandling,
                                   behandlingssteg = Behandlingssteg.VARSEL,
                                   behandlingsstegstatus = Behandlingsstegstatus.VENTER,
                                   venteårsak = Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING)
    }

    @Test
    fun `hentBehandling skal hente behandling når behandling er avsluttet`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = false,
                                                finnesVarsel = true,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL)
        val behandling = behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest)
        val lagretBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(lagretBehandling.copy(status = Behandlingsstatus.AVSLUTTET))

        val behandlingDto = behandlingService.hentBehandling(behandling.id)

        assertFalse { behandlingDto.kanEndres }
        assertFalse { behandlingDto.kanHenleggeBehandling }
    }

    @Test
    fun `hentBehandling skal ikke endre behandling av veileder`() {
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any(), any()) }.returns(
                InnloggetBrukertilgang(tilganger = mapOf(Tilgangskontrollsfagsystem.BARNETRYGD to Behandlerrolle.VEILEDER)))

        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = false,
                                                finnesVarsel = true,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL)
        val behandling = behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest)

        val behandlingDto = behandlingService.hentBehandling(behandling.id)

        assertFalse { behandlingDto.kanEndres }
    }

    @Test
    fun `hentBehandling skal ikke endre behandling av saksbehandler når behandling er på fattevedtak steg`() {
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any(), any()) }.returns(
                InnloggetBrukertilgang(tilganger = mapOf(Tilgangskontrollsfagsystem.BARNETRYGD to Behandlerrolle.SAKSBEHANDLER)))

        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = false,
                                                finnesVarsel = true,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL)
        val behandling = behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest)
        val lagretBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(lagretBehandling.copy(status = Behandlingsstatus.FATTER_VEDTAK))

        val behandlingDto = behandlingService.hentBehandling(behandling.id)

        assertFalse { behandlingDto.kanEndres }
    }

    @Test
    fun `hentBehandling skal endre behandling av beslutter når behandling er på fattevedtak steg`() {
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any(), any()) }.returns(
                InnloggetBrukertilgang(tilganger = mapOf(Tilgangskontrollsfagsystem.BARNETRYGD to Behandlerrolle.BESLUTTER)))

        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = false,
                                                finnesVarsel = true,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL)
        val behandling = behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest)
        val lagretBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(lagretBehandling.copy(status = Behandlingsstatus.FATTER_VEDTAK,
                                                          ansvarligSaksbehandler = "VL"))

        val behandlingDto = behandlingService.hentBehandling(behandling.id)

        assertTrue { behandlingDto.kanEndres }
    }

    @Test
    fun `hentBehandling skal ikke endre behandling med fattevedtak steg og beslutter er samme som saksbehandler`() {
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any(), any()) }.returns(
                InnloggetBrukertilgang(tilganger = mapOf(Tilgangskontrollsfagsystem.BARNETRYGD to Behandlerrolle.BESLUTTER)))

        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = false,
                                                finnesVarsel = true,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL)
        val behandling = behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest)
        val lagretBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(lagretBehandling.copy(status = Behandlingsstatus.FATTER_VEDTAK,
                                                          ansvarligSaksbehandler = "Z0000"))

        val behandlingDto = behandlingService.hentBehandling(behandling.id)

        assertFalse { behandlingDto.kanEndres }
    }

    @Test
    fun `settBehandlingPåVent skal ikke sett behandling på vent hvis fristdato er mindre enn i dag`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = true,
                                                finnesVarsel = true,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL)
        val behandling = behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest)

        val exception = assertFailsWith<RuntimeException>(block = {
            behandlingService.settBehandlingPåVent(behandling.id,
                                                   BehandlingPåVentDto(venteårsak = Venteårsak.ENDRE_TILKJENT_YTELSE,
                                                                       tidsfrist = LocalDate.now().minusDays(4)))
        })
        assertEquals("Fristen må være større enn dagens dato for behandling ${behandling.id}", exception.message)
    }

    @Test
    fun `settBehandlingPåVent skal ikke sett behandling på vent hvis fristdato er i dag`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = true,
                                                finnesVarsel = true,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL)
        val behandling = behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest)

        val exception = assertFailsWith<RuntimeException>(block = {
            behandlingService.settBehandlingPåVent(behandling.id,
                                                   BehandlingPåVentDto(venteårsak = Venteårsak.ENDRE_TILKJENT_YTELSE,
                                                                       tidsfrist = LocalDate.now()))
        })
        assertEquals("Fristen må være større enn dagens dato for behandling ${behandling.id}", exception.message)
    }

    @Test
    fun `settBehandlingPåVent skal sette behandling på vent hvis fristdato er større enn i dag`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = true,
                                                finnesVarsel = true,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL)
        val behandling = behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest)

        assertDoesNotThrow {
            behandlingService.settBehandlingPåVent(behandling.id,
                                                   BehandlingPåVentDto(venteårsak = Venteårsak.ENDRE_TILKJENT_YTELSE,
                                                                       tidsfrist = LocalDate.now().plusDays(1)))
        }
        assertTrue { behandlingskontrollService.erBehandlingPåVent(behandling.id) }
        assertAnsvarligSaksbehandler(behandling)
    }

    @Test
    fun `taBehandlingAvvent skal ikke gjenoppta når behandling er avsluttet`() {
        fagsakRepository.insert(Testdata.fagsak)
        val behandling = behandlingRepository.insert(Testdata.behandling)
        val lagretBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(lagretBehandling.copy(status = Behandlingsstatus.AVSLUTTET))

        val exception = assertFailsWith<RuntimeException>(block = { behandlingService.taBehandlingAvvent(lagretBehandling.id) })
        assertEquals("Behandling med id=${lagretBehandling.id} er allerede ferdig behandlet.", exception.message)
    }

    @Test
    fun `taBehandlingAvvent skal ikke gjenoppta når behandling er ikke på vent`() {
        fagsakRepository.insert(Testdata.fagsak)
        val behandling = behandlingRepository.insert(Testdata.behandling)

        val exception = assertFailsWith<RuntimeException>(block = { behandlingService.taBehandlingAvvent(behandling.id) })
        assertEquals("Behandling ${behandling.id} er ikke på vent, kan ike gjenoppta", exception.message)
    }

    @Test
    fun `taBehandlingAvvent skal gjenoppta når behandling er på vent`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = true,
                                                finnesVarsel = false,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL)
        val behandling = behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest)
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431.copy(behandlingId = behandling.id))

        behandlingService.settBehandlingPåVent(behandlingId = behandling.id,
                                               behandlingPåVentDto = BehandlingPåVentDto(Venteårsak.AVVENTER_DOKUMENTASJON,
                                                                                         LocalDate.now().plusDays(2)))

        assertTrue { behandlingskontrollService.erBehandlingPåVent(behandling.id) }

        behandlingService.taBehandlingAvvent(behandling.id)

        assertFalse { behandlingskontrollService.erBehandlingPåVent(behandling.id) }
        assertAnsvarligSaksbehandler(behandling)
    }

    @Test
    fun `henleggBehandling skal henlegge behandling og sende henleggelsesbrev`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = false,
                                                finnesVarsel = true,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL)
        var behandling = behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest)
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        //oppdaterer opprettettidspunkt slik at behandlingen kan henlegges
        behandlingRepository.update(behandling.copy(sporbar = Sporbar(opprettetAv = "VL",
                                                                      opprettetTid = LocalDateTime.now().minusDays(10))))
        // sender varselsbrev
        brevsporingRepository.insert(Brevsporing(behandlingId = behandling.id,
                                                 journalpostId = "testverdi",
                                                 dokumentId = "testverdi",
                                                 brevtype = Brevtype.VARSEL))
        behandlingService.taBehandlingAvvent(behandlingId = behandling.id)

        behandlingService.henleggBehandling(behandlingId = behandling.id,
                                            behandlingsresultatstype = Behandlingsresultatstype.HENLAGT_TEKNISK_VEDLIKEHOLD)

        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        assertEquals(Behandlingsstatus.AVSLUTTET, behandling.status)
        assertEquals(LocalDate.now(), behandling.avsluttetDato)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertEquals(2, behandlingsstegstilstand.size)
        assertEquals(Behandlingssteg.VARSEL, behandlingsstegstilstand[0].behandlingssteg)
        assertEquals(Behandlingsstegstatus.UTFØRT, behandlingsstegstilstand[0].behandlingsstegsstatus)
        assertEquals(Behandlingssteg.GRUNNLAG, behandlingsstegstilstand[1].behandlingssteg)
        assertEquals(Behandlingsstegstatus.AVBRUTT, behandlingsstegstilstand[1].behandlingsstegsstatus)

        val behandlingssresultat = behandling.sisteResultat
        assertNotNull(behandlingssresultat)
        assertEquals(Behandlingsresultatstype.HENLAGT_TEKNISK_VEDLIKEHOLD, behandlingssresultat.type)

        val task = taskRepository.findByStatus(Status.UBEHANDLET)
        assertEquals(3, task.count())
        assertEquals(SendHenleggelsesbrevTask.TYPE, task[1].type)
    }

    @Test
    fun `henleggBehandling skal henlegge behandling uten henleggelsesbrev`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = false,
                                                finnesVarsel = false,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL)
        var behandling = behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest)
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        //oppdaterer opprettettidspunkt slik at behandlingen kan henlegges
        behandlingRepository.update(behandling.copy(sporbar = Sporbar(opprettetAv = "VL",
                                                                      opprettetTid = LocalDateTime.now().minusDays(10))))

        behandlingService.henleggBehandling(behandlingId = behandling.id,
                                            behandlingsresultatstype = Behandlingsresultatstype.HENLAGT_TEKNISK_VEDLIKEHOLD)

        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        assertEquals(Behandlingsstatus.AVSLUTTET, behandling.status)
        assertEquals(LocalDate.now(), behandling.avsluttetDato)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertEquals(1, behandlingsstegstilstand.size)
        assertEquals(Behandlingssteg.GRUNNLAG, behandlingsstegstilstand[0].behandlingssteg)
        assertEquals(Behandlingsstegstatus.AVBRUTT, behandlingsstegstilstand[0].behandlingsstegsstatus)

        val behandlingssresultat = behandling.sisteResultat
        assertNotNull(behandlingssresultat)
        assertEquals(Behandlingsresultatstype.HENLAGT_TEKNISK_VEDLIKEHOLD, behandlingssresultat.type)


        assertNull(taskRepository.findByStatus(Status.UBEHANDLET).find { task -> task.type == SendHenleggelsesbrevTask.TYPE })
    }

    @Test
    fun `henleggBehandling skal ikke henlegge behandling som opprettet nå`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = false,
                                                finnesVarsel = false,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL)
        val behandling = behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest)

        val exception = assertFailsWith<RuntimeException> {
            behandlingService.henleggBehandling(behandlingId = behandling.id,
                                                behandlingsresultatstype =
                                                Behandlingsresultatstype.HENLAGT_TEKNISK_VEDLIKEHOLD)
        }
        assertEquals("Behandling med behandlingId=${behandling.id} kan ikke henlegges.", exception.message)
    }

    @Test
    fun `henleggBehandling skal ikke henlegge behandling som har aktivt kravgrunnlag`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = false,
                                                finnesVarsel = false,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL)
        val behandling = behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest)
        val kravgrunnlag = Testdata.kravgrunnlag431
        kravgrunnlagRepository.insert(kravgrunnlag.copy(behandlingId = behandling.id))

        val exception = assertFailsWith<RuntimeException> {
            behandlingService.henleggBehandling(behandlingId = behandling.id,
                                                behandlingsresultatstype =
                                                Behandlingsresultatstype.HENLAGT_TEKNISK_VEDLIKEHOLD)
        }
        assertEquals("Behandling med behandlingId=${behandling.id} kan ikke henlegges.", exception.message)
    }

    @Test
    fun `henleggBehandling skal ikke henlegge behandling som er allerede avsluttet`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = false,
                                                finnesVarsel = false,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL)
        var behandling = behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest)
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(behandling.copy(status = Behandlingsstatus.AVSLUTTET))

        val exception = assertFailsWith<RuntimeException> {
            behandlingService.henleggBehandling(behandlingId = behandling.id,
                                                behandlingsresultatstype =
                                                Behandlingsresultatstype.HENLAGT_TEKNISK_VEDLIKEHOLD)
        }
        assertEquals("Behandling med id=${behandling.id} er allerede ferdig behandlet.", exception.message)
    }

    private fun assertFellesBehandlingRespons(behandlingDto: BehandlingDto,
                                              behandling: Behandling) {
        assertEquals(behandling.eksternBrukId, behandlingDto.eksternBrukId)
        assertFalse { behandlingDto.erBehandlingHenlagt }
        assertEquals(Behandlingstype.TILBAKEKREVING.name, behandlingDto.type.name)
        assertEquals(Behandlingsstatus.UTREDES, behandlingDto.status)
        assertEquals(behandling.opprettetDato, behandlingDto.opprettetDato)
        assertNull(behandlingDto.avsluttetDato)
        assertNull(behandlingDto.vedtaksdato)
        assertEquals("8020", behandlingDto.enhetskode)
        assertEquals("Oslo", behandlingDto.enhetsnavn)
        assertNull(behandlingDto.resultatstype)
        assertEquals("Z0000", behandlingDto.ansvarligSaksbehandler)
        assertNull(behandlingDto.ansvarligBeslutter)
    }

    private fun assertBehandlingsstegsinfo(behandlingDto: BehandlingDto,
                                           behandling: Behandling,
                                           behandlingssteg: Behandlingssteg,
                                           behandlingsstegstatus: Behandlingsstegstatus,
                                           venteårsak: Venteårsak) {
        val behandlingsstegsinfo: List<BehandlingsstegsinfoDto> = behandlingDto.behandlingsstegsinfo
        assertEquals(1, behandlingsstegsinfo.size)
        assertEquals(behandlingssteg, behandlingsstegsinfo[0].behandlingssteg)
        assertEquals(behandlingsstegstatus, behandlingsstegsinfo[0].behandlingsstegstatus)
        assertEquals(venteårsak, behandlingsstegsinfo[0].venteårsak)
        assertEquals(behandling.opprettetDato.plusWeeks(venteårsak.defaultVenteTidIUker),
                     behandlingsstegsinfo[0].tidsfrist)
    }

    private fun assertFagsak(behandling: Behandling,
                             opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest) {
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        assertEquals(opprettTilbakekrevingRequest.eksternFagsakId, fagsak.eksternFagsakId)
        assertEquals(opprettTilbakekrevingRequest.ytelsestype.name, fagsak.ytelsestype.name)
        assertEquals(opprettTilbakekrevingRequest.fagsystem, fagsak.fagsystem)
        assertEquals(opprettTilbakekrevingRequest.språkkode, fagsak.bruker.språkkode)
        assertEquals(opprettTilbakekrevingRequest.personIdent, fagsak.bruker.ident)
    }

    private fun assertBehandling(behandling: Behandling,
                                 opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest) {
        assertEquals(Behandlingstype.TILBAKEKREVING.name, behandling.type.name)
        assertEquals(Behandlingsstatus.OPPRETTET.name, behandling.status.name)
        assertEquals(false, behandling.manueltOpprettet)
        assertEquals(opprettTilbakekrevingRequest.enhetId, behandling.behandlendeEnhet)
        assertEquals(opprettTilbakekrevingRequest.enhetsnavn, behandling.behandlendeEnhetsNavn)
        assertEquals(Saksbehandlingstype.ORDINÆR.name, behandling.saksbehandlingstype.name)
        assertEquals(LocalDate.now(), behandling.opprettetDato)
    }

    private fun assertFagsystemsbehandling(behandling: Behandling,
                                           opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest) {
        val fagsystemsbehandlinger = behandling.fagsystemsbehandling
        assertEquals(1, fagsystemsbehandlinger.size)
        val fagsystemsbehandling = fagsystemsbehandlinger.toList().first()
        assertEquals(true, fagsystemsbehandling.aktiv)
        assertEquals(opprettTilbakekrevingRequest.eksternId, fagsystemsbehandling.eksternId)
        assertEquals(opprettTilbakekrevingRequest.faktainfo.tilbakekrevingsvalg, fagsystemsbehandling.tilbakekrevingsvalg)
        assertEquals(opprettTilbakekrevingRequest.revurderingsvedtaksdato, fagsystemsbehandling.revurderingsvedtaksdato)
        assertEquals("testresultat", fagsystemsbehandling.resultat)
        assertEquals("testverdi", fagsystemsbehandling.årsak)
        assertTrue { fagsystemsbehandling.konsekvenser.isEmpty() }
    }

    private fun assertVarselData(behandling: Behandling,
                                 opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest) {
        val varsler = behandling.varsler
        assertEquals(1, varsler.size)
        val varsel = varsler.toList().first()
        opprettTilbakekrevingRequest.varsel?.let {
            assertEquals(it.varseltekst, varsel.varseltekst)
            assertEquals(it.sumFeilutbetaling, varsel.varselbeløp.toBigDecimal())
            assertEquals(it.perioder.size, varsel.perioder.size)
            assertEquals(it.perioder.first().fom, varsel.perioder.toList().first().fom)
            assertEquals(it.perioder.first().tom, varsel.perioder.toList().first().tom)
        }
    }

    private fun assertVerge(behandling: Behandling,
                            opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest) {
        assertTrue { behandling.verger.isNotEmpty() }
        assertEquals(1, behandling.verger.size)
        val verge = behandling.verger.toList().first()
        assertEquals(opprettTilbakekrevingRequest.verge?.vergetype?.navn, verge.type.navn)
        assertEquals(opprettTilbakekrevingRequest.verge?.gyldigFom, verge.gyldigFom)
        assertEquals(opprettTilbakekrevingRequest.verge?.gyldigTom, verge.gyldigTom)
        assertEquals(opprettTilbakekrevingRequest.verge?.navn, verge.navn)
        assertEquals(opprettTilbakekrevingRequest.verge?.organisasjonsnummer, verge.orgNr)
        assertEquals(opprettTilbakekrevingRequest.verge?.personIdent, verge.ident)
    }

    private fun lagOpprettTilbakekrevingRequest(finnesVerge: Boolean,
                                                finnesVarsel: Boolean,
                                                manueltOpprettet: Boolean,
                                                tilbakekrevingsvalg: Tilbakekrevingsvalg): OpprettTilbakekrevingRequest {
        val varsel = if (finnesVarsel) Varsel(varseltekst = "testverdi",
                                              sumFeilutbetaling = BigDecimal.valueOf(1500L),
                                              perioder = listOf(Periode(fom, tom))) else null
        val verge = if (finnesVerge) Verge(vergetype = Vergetype.VERGE_FOR_BARN,
                                           gyldigFom = fom,
                                           gyldigTom = tom.plusDays(100),
                                           navn = "Andy",
                                           personIdent = "321321321") else null

        val faktainfo = Faktainfo(revurderingsårsak = "testverdi",
                                  revurderingsresultat = "testresultat",
                                  tilbakekrevingsvalg = tilbakekrevingsvalg)

        return OpprettTilbakekrevingRequest(ytelsestype = BARNETRYGD,
                                            fagsystem = Fagsystem.BA,
                                            eksternFagsakId = "1234567",
                                            personIdent = "321321322",
                                            eksternId = UUID.randomUUID().toString(),
                                            manueltOpprettet = manueltOpprettet,
                                            språkkode = Språkkode.NN,
                                            enhetId = "8020",
                                            enhetsnavn = "Oslo",
                                            varsel = varsel,
                                            revurderingsvedtaksdato = fom,
                                            verge = verge,
                                            faktainfo = faktainfo)
    }

    private fun assertAnsvarligSaksbehandler(behandling: Behandling) {
        val lagretBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        assertEquals("Z0000", lagretBehandling.ansvarligSaksbehandler)
        assertNull(lagretBehandling.ansvarligBeslutter)
    }
}
