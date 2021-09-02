package no.nav.familie.tilbake.behandling

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockkObject
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.tilbakekreving.Faktainfo
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettManueltTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Periode
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.kontrakter.felles.tilbakekreving.Varsel
import no.nav.familie.kontrakter.felles.tilbakekreving.Verge
import no.nav.familie.kontrakter.felles.tilbakekreving.Vergetype
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype.BARNETILSYN
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype.BARNETRYGD
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.api.dto.BehandlingDto
import no.nav.familie.tilbake.api.dto.BehandlingPåVentDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegsinfoDto
import no.nav.familie.tilbake.api.dto.ByttEnhetDto
import no.nav.familie.tilbake.api.dto.HenleggelsesbrevFritekstDto
import no.nav.familie.tilbake.api.dto.OpprettRevurderingDto
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import no.nav.familie.tilbake.behandling.domain.Behandlingsårsakstype
import no.nav.familie.tilbake.behandling.domain.Saksbehandlingstype
import no.nav.familie.tilbake.behandling.task.OpprettBehandlingManueltTask
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.repository.Sporbar
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevsporing
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.dokumentbestilling.henleggelse.SendHenleggelsesbrevTask
import no.nav.familie.tilbake.historikkinnslag.LagHistorikkinnslagTask
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.task.FinnKravgrunnlagTask
import no.nav.familie.tilbake.kravgrunnlag.task.HentKravgrunnlagTask
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattRepository
import no.nav.familie.tilbake.oppgave.FerdigstillOppgaveTask
import no.nav.familie.tilbake.oppgave.LagOppgaveTask
import no.nav.familie.tilbake.oppgave.OppdaterEnhetOppgaveTask
import no.nav.familie.tilbake.oppgave.OppdaterOppgaveTask
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
    private lateinit var økonomiXmlMottattRepository: ØkonomiXmlMottattRepository

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var behandlingskontrollService: BehandlingskontrollService

    private val fom: LocalDate = LocalDate.now().minusMonths(1)
    private val tom: LocalDate = LocalDate.now()

    @BeforeEach
    fun init() {
        mockkObject(ContextService)
        every { ContextService.hentSaksbehandler() }.returns("Z0000")
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any()) }
                .returns(InnloggetBrukertilgang(mapOf(Tilgangskontrollsfagsystem.SYSTEM_TILGANG to Behandlerrolle.SYSTEM)))
    }

    @AfterEach
    fun tearDown() {
        clearMocks(ContextService)
    }

    @Test
    fun `opprettBehandling skal opprette automatisk behandling uten verge`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = false,
                                                finnesVarsel = true,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL)

        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)

        assertBehandling(behandling, opprettTilbakekrevingRequest)
        assertFagsak(behandling, opprettTilbakekrevingRequest)
        assertFagsystemsbehandling(behandling, opprettTilbakekrevingRequest)
        assertVarselData(behandling, opprettTilbakekrevingRequest)
        assertTrue { behandling.verger.isEmpty() }
        assertHistorikkTask(behandling.id, TilbakekrevingHistorikkinnslagstype.BEHANDLING_OPPRETTET, Aktør.VEDTAKSLØSNING)
        assertFinnKravgrunnlagTask(behandling.id)
        assertOppgaveTask(behandling.id, LagOppgaveTask.TYPE)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingsstegstilstand(behandlingsstegstilstand,
                                       Behandlingssteg.VARSEL,
                                       Behandlingsstegstatus.VENTER,
                                       Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING)
    }

    @Test
    fun `opprettBehandling skal opprette automatisk behandling med verge`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = true,
                                                finnesVarsel = true,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL)

        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)

        assertBehandling(behandling, opprettTilbakekrevingRequest)
        assertFagsak(behandling, opprettTilbakekrevingRequest)
        assertFagsystemsbehandling(behandling, opprettTilbakekrevingRequest)
        assertVarselData(behandling, opprettTilbakekrevingRequest)
        assertVerge(behandling, opprettTilbakekrevingRequest)
        assertHistorikkTask(behandling.id, TilbakekrevingHistorikkinnslagstype.BEHANDLING_OPPRETTET, Aktør.VEDTAKSLØSNING)
        assertFinnKravgrunnlagTask(behandling.id)
        assertOppgaveTask(behandling.id, LagOppgaveTask.TYPE)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingsstegstilstand(behandlingsstegstilstand,
                                       Behandlingssteg.VARSEL,
                                       Behandlingsstegstatus.VENTER,
                                       Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING)
    }

    @Test
    fun `opprettBehandling skal opprette automatisk behandling uten varsel`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = false,
                                                finnesVarsel = false,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL)

        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)

        assertBehandling(behandling, opprettTilbakekrevingRequest)
        assertFagsak(behandling, opprettTilbakekrevingRequest)
        assertFagsystemsbehandling(behandling, opprettTilbakekrevingRequest)
        assertTrue { behandling.varsler.isEmpty() }
        assertTrue { behandling.verger.isEmpty() }
        assertHistorikkTask(behandling.id, TilbakekrevingHistorikkinnslagstype.BEHANDLING_OPPRETTET, Aktør.VEDTAKSLØSNING)
        assertFinnKravgrunnlagTask(behandling.id)
        assertOppgaveTask(behandling.id, LagOppgaveTask.TYPE)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingsstegstilstand(behandlingsstegstilstand,
                                       Behandlingssteg.GRUNNLAG,
                                       Behandlingsstegstatus.VENTER,
                                       Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG)
    }

    @Test
    fun `opprettBehandling oppretter ikke behandling når det finnes åpen tilbakekreving for samme eksternFagsakId`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = true,
                                                finnesVarsel = true,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL)
        behandlingService.opprettBehandling(opprettTilbakekrevingRequest)

        val exception = assertFailsWith<RuntimeException>(block = {
            behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        })
        assertEquals("Det finnes allerede en åpen behandling for ytelsestype="
                     + opprettTilbakekrevingRequest.ytelsestype +
                     " og eksternFagsakId=${opprettTilbakekrevingRequest.eksternFagsakId}, " +
                     "kan ikke opprette en ny.", exception.message)
    }

    @Test
    fun `opprettBehandling skal ikke opprette automatisk behandling når siste tilbakekreving er ikke henlagt`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = true,
                                                finnesVarsel = true,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL)

        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        val lagretBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(lagretBehandling.copy(status = Behandlingsstatus.AVSLUTTET))

        val exception = assertFailsWith<RuntimeException>(block = {
            behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        })
        assertEquals("Det finnes allerede en avsluttet behandling for ytelsestype="
                     + opprettTilbakekrevingRequest.ytelsestype +
                     " og eksternFagsakId=${opprettTilbakekrevingRequest.eksternFagsakId} " +
                     "som ikke er henlagt, kan ikke opprette en ny.", exception.message)
    }

    @Test
    fun `opprettBehandling skal opprette automatisk når det allerede finnes avsluttet behandling for samme fagsak`() {
        val forrigeOpprettTilbakekrevingRequest = lagOpprettTilbakekrevingRequest(finnesVerge = false,
                                                                                  finnesVarsel = true,
                                                                                  manueltOpprettet = false,
                                                                                  tilbakekrevingsvalg = Tilbakekrevingsvalg
                                                                                          .OPPRETT_TILBAKEKREVING_MED_VARSEL)
        val forrigeBehandling = behandlingService.opprettBehandling(forrigeOpprettTilbakekrevingRequest)

        val lagretBehandling = behandlingRepository.findByIdOrThrow(forrigeBehandling.id)
        behandlingRepository.update(lagretBehandling.copy(status = Behandlingsstatus.AVSLUTTET))

        //oppretter ny behandling for en annen eksternId
        val nyOpprettTilbakekrevingRequest = lagOpprettTilbakekrevingRequest(finnesVerge = false,
                                                                             finnesVarsel = true,
                                                                             manueltOpprettet = false,
                                                                             tilbakekrevingsvalg = Tilbakekrevingsvalg
                                                                                     .OPPRETT_TILBAKEKREVING_MED_VARSEL)
        val behandling = behandlingService.opprettBehandling(nyOpprettTilbakekrevingRequest)
        assertBehandling(behandling, nyOpprettTilbakekrevingRequest)
        assertFagsak(behandling, nyOpprettTilbakekrevingRequest)
        assertFagsystemsbehandling(behandling, nyOpprettTilbakekrevingRequest)
        assertVarselData(behandling, nyOpprettTilbakekrevingRequest)
        assertTrue { behandling.verger.isEmpty() }
        assertHistorikkTask(behandling.id, TilbakekrevingHistorikkinnslagstype.BEHANDLING_OPPRETTET, Aktør.VEDTAKSLØSNING)
        assertFinnKravgrunnlagTask(behandling.id)
        assertOppgaveTask(behandling.id, LagOppgaveTask.TYPE)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingsstegstilstand(behandlingsstegstilstand,
                                       Behandlingssteg.VARSEL,
                                       Behandlingsstegstatus.VENTER,
                                       Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING)
    }

    @Test
    fun `opprettBehandling skal ikke opprette manuelt når det ikke finnes kravgrunnlag for samme fagsak,ytelsestype,eksternId`() {
        val opprettTilbakekrevingRequest = lagOpprettTilbakekrevingRequest(finnesVerge = false,
                                                                           finnesVarsel = false,
                                                                           manueltOpprettet = true,
                                                                           tilbakekrevingsvalg = Tilbakekrevingsvalg
                                                                                   .OPPRETT_TILBAKEKREVING_UTEN_VARSEL)

        val exception = assertFailsWith<RuntimeException> { behandlingService.opprettBehandling(opprettTilbakekrevingRequest) }
        assertEquals("Det finnes intet kravgrunnlag for ytelsestype=${opprettTilbakekrevingRequest.ytelsestype}," +
                     "eksternFagsakId=${opprettTilbakekrevingRequest.eksternFagsakId} " +
                     "og eksternId=${opprettTilbakekrevingRequest.eksternId}. " +
                     "Tilbakekrevingsbehandling kan ikke opprettes manuelt.", exception.message)
    }

    @Test
    fun `opprettBehandling skal opprette manuelt når det finnes kravgrunnlag for samme fagsak,ytelsestype,eksternId`() {
        val opprettTilbakekrevingRequest = lagOpprettTilbakekrevingRequest(finnesVerge = false,
                                                                           finnesVarsel = false,
                                                                           manueltOpprettet = true,
                                                                           tilbakekrevingsvalg = Tilbakekrevingsvalg
                                                                                   .OPPRETT_TILBAKEKREVING_UTEN_VARSEL)
        val økonomiXmlMottatt = Testdata.økonomiXmlMottatt
        økonomiXmlMottattRepository.insert(økonomiXmlMottatt.copy(eksternFagsakId = opprettTilbakekrevingRequest.eksternFagsakId,
                                                                  ytelsestype = opprettTilbakekrevingRequest.ytelsestype,
                                                                  referanse = opprettTilbakekrevingRequest.eksternId))

        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)

        assertBehandling(behandling, opprettTilbakekrevingRequest, true)
        assertFagsak(behandling, opprettTilbakekrevingRequest)
        assertFagsystemsbehandling(behandling, opprettTilbakekrevingRequest)
        assertTrue { behandling.varsler.isEmpty() }
        assertTrue { behandling.verger.isEmpty() }
        assertHistorikkTask(behandling.id, TilbakekrevingHistorikkinnslagstype.BEHANDLING_OPPRETTET, Aktør.VEDTAKSLØSNING)
        assertFinnKravgrunnlagTask(behandling.id)
        assertOppgaveTask(behandling.id, LagOppgaveTask.TYPE)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingsstegstilstand(behandlingsstegstilstand,
                                       Behandlingssteg.GRUNNLAG,
                                       Behandlingsstegstatus.VENTER,
                                       Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG)
    }

    @Test
    fun `opprettBehandlingManuellTask skal opprette OpprettBehandlingManueltTask`() {
        behandlingService.opprettBehandlingManuellTask(OpprettManueltTilbakekrevingRequest(eksternFagsakId = "testverdi",
                                                                                           ytelsestype = BARNETRYGD,
                                                                                           eksternId = "testverdi"))

        val taskene = taskRepository.findByStatus(Status.UBEHANDLET)
        assertEquals(1, taskene.size)
        val task = taskene[0]
        assertEquals(OpprettBehandlingManueltTask.TYPE, task.type)
        assertEquals("testverdi", task.metadata["eksternFagsakId"])
        assertEquals(BARNETRYGD.name, task.metadata["ytelsestype"])
        assertEquals("testverdi", task.metadata["eksternId"])
        assertEquals("Z0000", task.metadata["ansvarligSaksbehandler"])
    }

    @Test
    fun `opprettRevurdering skal opprette revurdering for gitt avsluttet tilbakekrevingsbehandling`() {
        fagsakRepository.insert(Testdata.fagsak)
        var behandling = behandlingRepository.insert(Testdata.behandling)
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(behandling.copy(status = Behandlingsstatus.AVSLUTTET))

        var revurdering = behandlingService.opprettRevurdering(lagOpprettRevurderingDto(behandling.id))
        revurdering = behandlingRepository.findByIdOrThrow(revurdering.id)
        assertEquals(Behandlingstype.REVURDERING_TILBAKEKREVING, revurdering.type)
        assertEquals(Behandlingsårsakstype.REVURDERING_OPPLYSNINGER_OM_VILKÅR, revurdering.sisteÅrsak?.type)
        assertEquals(Behandlingsstatus.UTREDES, revurdering.status)
        assertEquals(behandling.behandlendeEnhet, revurdering.behandlendeEnhet)
        assertEquals(behandling.behandlendeEnhetsNavn, revurdering.behandlendeEnhetsNavn)
        assertFalse { behandling.manueltOpprettet }

        val aktivFagsystemsbehandling = revurdering.aktivFagsystemsbehandling
        assertEquals(behandling.aktivFagsystemsbehandling.tilbakekrevingsvalg, aktivFagsystemsbehandling.tilbakekrevingsvalg)
        assertEquals(behandling.aktivFagsystemsbehandling.revurderingsvedtaksdato,
                     aktivFagsystemsbehandling.revurderingsvedtaksdato)
        assertEquals(behandling.aktivFagsystemsbehandling.eksternId, aktivFagsystemsbehandling.eksternId)
        assertEquals(behandling.aktivFagsystemsbehandling.årsak, aktivFagsystemsbehandling.årsak)
        assertEquals(behandling.aktivFagsystemsbehandling.resultat, aktivFagsystemsbehandling.resultat)
        assertHistorikkTask(revurdering.id, TilbakekrevingHistorikkinnslagstype.BEHANDLING_OPPRETTET, Aktør.SAKSBEHANDLER)
        assertHistorikkTask(revurdering.id, TilbakekrevingHistorikkinnslagstype.BEHANDLING_PÅ_VENT, Aktør.VEDTAKSLØSNING,
                            "Venter på kravgrunnlag fra økonomi")
        assertTrue {
            taskRepository.findByStatus(Status.UBEHANDLET).any {
                HentKravgrunnlagTask.TYPE == it.type &&
                revurdering.id.toString() == it.payload
            }
        }
        val behandlingsstegstilstand = behandlingskontrollService.finnAktivStegstilstand(revurdering.id)
        assertNotNull(behandlingsstegstilstand)
        assertEquals(Behandlingssteg.GRUNNLAG, behandlingsstegstilstand.behandlingssteg)
        assertEquals(Behandlingsstegstatus.VENTER, behandlingsstegstilstand.behandlingsstegsstatus)
    }

    @Test
    fun `opprettRevurdering skal ikke opprette revurdering for tilbakekreving som er avsluttet uten kravgrunnlag`() {
        fagsakRepository.insert(Testdata.fagsak)
        var behandling = behandlingRepository.insert(Testdata.behandling)
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(behandling.copy(status = Behandlingsstatus.AVSLUTTET))

        val exception = assertFailsWith<RuntimeException> {
            behandlingService.opprettRevurdering(lagOpprettRevurderingDto(behandling.id))
        }
        assertEquals("Revurdering kan ikke opprettes for behandling ${behandling.id}. " +
                     "Enten behandlingen er ikke avsluttet med kravgrunnlag eller " +
                     "det finnes allerede en åpen revurdering", exception.message)
    }

    @Test
    fun `hentBehandling skal hente behandling som opprettet uten varsel`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = true,
                                                finnesVarsel = true,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL)
        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
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
        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
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
        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
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
        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        val lagretBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(lagretBehandling.copy(status = Behandlingsstatus.AVSLUTTET))

        val behandlingDto = behandlingService.hentBehandling(behandling.id)

        assertFalse { behandlingDto.kanEndres }
        assertFalse { behandlingDto.kanHenleggeBehandling }
    }

    @Test
    fun `hentBehandling skal ikke endre behandling av veileder`() {
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any()) }
                .returns(InnloggetBrukertilgang(mapOf(Tilgangskontrollsfagsystem.BARNETRYGD to Behandlerrolle.VEILEDER)))

        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = false,
                                                finnesVarsel = true,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL)
        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)

        val behandlingDto = behandlingService.hentBehandling(behandling.id)

        assertFalse { behandlingDto.kanEndres }
    }

    @Test
    fun `hentBehandling skal ikke endre behandling av saksbehandler når behandling er på fattevedtak steg`() {
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any()) }
                .returns(InnloggetBrukertilgang(mapOf(Tilgangskontrollsfagsystem.BARNETRYGD to Behandlerrolle.SAKSBEHANDLER)))

        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = false,
                                                finnesVarsel = true,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL)
        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        val lagretBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(lagretBehandling.copy(status = Behandlingsstatus.FATTER_VEDTAK))

        val behandlingDto = behandlingService.hentBehandling(behandling.id)

        assertFalse { behandlingDto.kanEndres }
    }

    @Test
    fun `hentBehandling skal endre behandling av beslutter når behandling er på fattevedtak steg`() {
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any()) }
                .returns(InnloggetBrukertilgang(mapOf(Tilgangskontrollsfagsystem.BARNETRYGD to Behandlerrolle.BESLUTTER)))

        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = false,
                                                finnesVarsel = true,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL)
        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        val lagretBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(lagretBehandling.copy(status = Behandlingsstatus.FATTER_VEDTAK,
                                                          ansvarligSaksbehandler = Constants.BRUKER_ID_VEDTAKSLØSNINGEN))

        val behandlingDto = behandlingService.hentBehandling(behandling.id)

        assertTrue { behandlingDto.kanEndres }
    }

    @Test
    fun `hentBehandling skal ikke endre behandling med fattevedtak steg og beslutter er samme som saksbehandler`() {
        every { ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(any(), any()) }
                .returns(InnloggetBrukertilgang(mapOf(Tilgangskontrollsfagsystem.BARNETRYGD to Behandlerrolle.BESLUTTER)))

        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = false,
                                                finnesVarsel = true,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL)
        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        val lagretBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(lagretBehandling.copy(status = Behandlingsstatus.FATTER_VEDTAK,
                                                          ansvarligSaksbehandler = "Z0000"))

        val behandlingDto = behandlingService.hentBehandling(behandling.id)

        assertFalse { behandlingDto.kanEndres }
    }

    @Test
    fun `hentBehandling kan ikke opprette revurdering når tilbakekreving ikke har kravgrunnlag`() {
        fagsakRepository.insert(Testdata.fagsak)
        var behandling = behandlingRepository.insert(Testdata.behandling)
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(behandling.copy(status = Behandlingsstatus.AVSLUTTET))

        val behandlingDto = behandlingService.hentBehandling(behandling.id)
        assertFalse { behandlingDto.kanRevurderingOpprettes }
    }

    @Test
    fun `hentBehandling kan opprette revurdering når tilbakekreving er avsluttet med kravgrunnlag`() {
        fagsakRepository.insert(Testdata.fagsak)
        var behandling = behandlingRepository.insert(Testdata.behandling)
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(behandling.copy(status = Behandlingsstatus.AVSLUTTET))

        val behandlingDto = behandlingService.hentBehandling(behandling.id)
        assertTrue { behandlingDto.kanRevurderingOpprettes }
    }

    @Test
    fun `hentBehandling kan ikke opprette revurdering når tilbakekreving har en åpen revurdering`() {
        fagsakRepository.insert(Testdata.fagsak)
        var behandling = behandlingRepository.insert(Testdata.behandling)
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(behandling.copy(status = Behandlingsstatus.AVSLUTTET))

        behandlingRepository.insert(Testdata.revurdering)

        val behandlingDto = behandlingService.hentBehandling(behandling.id)
        assertFalse { behandlingDto.kanRevurderingOpprettes }
    }

    @Test
    fun `hentBehandling kan opprette revurdering når tilbakekreving har en avsluttet revurdering`() {
        fagsakRepository.insert(Testdata.fagsak)
        var behandling = behandlingRepository.insert(Testdata.behandling)
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(behandling.copy(status = Behandlingsstatus.AVSLUTTET))

        var revurdering = behandlingRepository.insert(Testdata.revurdering)
        revurdering = behandlingRepository.findByIdOrThrow(revurdering.id)
        behandlingRepository.update(revurdering.copy(status = Behandlingsstatus.AVSLUTTET))

        val behandlingDto = behandlingService.hentBehandling(behandling.id)
        assertTrue { behandlingDto.kanRevurderingOpprettes }
    }

    @Test
    fun `settBehandlingPåVent skal ikke sett behandling på vent hvis fristdato er mindre enn i dag`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = true,
                                                finnesVarsel = true,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL)
        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)

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
        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)

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
        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)

        val behandlingPåVentDto = BehandlingPåVentDto(venteårsak = Venteårsak.ENDRE_TILKJENT_YTELSE,
                                                      tidsfrist = LocalDate.now().plusDays(1))
        assertDoesNotThrow {
            behandlingService.settBehandlingPåVent(behandling.id, behandlingPåVentDto)
        }
        assertTrue { behandlingskontrollService.erBehandlingPåVent(behandling.id) }
        assertAnsvarligSaksbehandler(behandling)
        assertHistorikkTask(behandling.id,
                            TilbakekrevingHistorikkinnslagstype.BEHANDLING_PÅ_VENT,
                            Aktør.SAKSBEHANDLER,
                            Venteårsak.ENDRE_TILKJENT_YTELSE.beskrivelse)
        assertOppgaveTask(behandling.id,
                          OppdaterOppgaveTask.TYPE,
                          "Frist er oppdatert av saksbehandler Z0000",
                          behandlingPåVentDto.tidsfrist)
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
        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431.copy(behandlingId = behandling.id))

        behandlingService.settBehandlingPåVent(behandlingId = behandling.id,
                                               behandlingPåVentDto = BehandlingPåVentDto(Venteårsak.AVVENTER_DOKUMENTASJON,
                                                                                         LocalDate.now().plusDays(2)))

        assertTrue { behandlingskontrollService.erBehandlingPåVent(behandling.id) }

        behandlingService.taBehandlingAvvent(behandling.id)

        assertFalse { behandlingskontrollService.erBehandlingPåVent(behandling.id) }
        assertAnsvarligSaksbehandler(behandling)
        assertHistorikkTask(behandling.id, TilbakekrevingHistorikkinnslagstype.BEHANDLING_GJENOPPTATT, Aktør.SAKSBEHANDLER)
        assertOppgaveTask(behandling.id,
                          OppdaterOppgaveTask.TYPE,
                          "Behandling er tatt av vent",
                          LocalDate.now())
    }

    @Test
    fun `taBehandlingAvvent skal gjenoppta behandling og hoppe til FAKTA steg når behandling venter på bruker med grunnlag`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = true,
                                                finnesVarsel = true,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL)
        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)

        var behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertTrue {
            behandlingsstegstilstand.any {
                it.behandlingssteg == Behandlingssteg.VARSEL &&
                it.behandlingsstegsstatus == Behandlingsstegstatus.VENTER &&
                it.venteårsak == Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING
            }
        }

        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431.copy(behandlingId = behandling.id))

        behandlingService.taBehandlingAvvent(behandling.id)

        behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertTrue {
            behandlingsstegstilstand.any {
                it.behandlingssteg == Behandlingssteg.VARSEL &&
                it.behandlingsstegsstatus == Behandlingsstegstatus.UTFØRT
            }
        }
        assertTrue {
            behandlingsstegstilstand.any {
                it.behandlingssteg == Behandlingssteg.FAKTA &&
                it.behandlingsstegsstatus == Behandlingsstegstatus.KLAR
            }
        }
        assertFalse {
            behandlingsstegstilstand.any { it.behandlingssteg == Behandlingssteg.GRUNNLAG }
        }

        assertFalse { behandlingskontrollService.erBehandlingPåVent(behandling.id) }
        assertAnsvarligSaksbehandler(behandling)
        assertHistorikkTask(behandling.id, TilbakekrevingHistorikkinnslagstype.BEHANDLING_GJENOPPTATT, Aktør.SAKSBEHANDLER)
        assertOppgaveTask(behandling.id,
                          OppdaterOppgaveTask.TYPE,
                          "Behandling er tatt av vent",
                          LocalDate.now())
    }

    @Test
    fun `henleggBehandling skal henlegge behandling og sende henleggelsesbrev`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = false,
                                                finnesVarsel = true,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL)
        var behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        //oppdaterer opprettettidspunkt slik at behandlingen kan henlegges
        behandlingRepository.update(behandling.copy(sporbar = Sporbar(opprettetAv = Constants.BRUKER_ID_VEDTAKSLØSNINGEN,
                                                                      opprettetTid = LocalDateTime.now().minusDays(10))))
        // sender varselsbrev
        brevsporingRepository.insert(Brevsporing(behandlingId = behandling.id,
                                                 journalpostId = "testverdi",
                                                 dokumentId = "testverdi",
                                                 brevtype = Brevtype.VARSEL))
        behandlingService.taBehandlingAvvent(behandlingId = behandling.id)

        behandlingService.henleggBehandling(behandlingId = behandling.id,
                                            henleggelsesbrevFritekstDto = HenleggelsesbrevFritekstDto(
                                                    behandlingsresultatstype = Behandlingsresultatstype.HENLAGT_FEILOPPRETTET,
                                                    begrunnelse = "testverdi"))

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
        assertEquals(Behandlingsresultatstype.HENLAGT_FEILOPPRETTET, behandlingssresultat.type)

        assertTrue { taskRepository.findByStatus(Status.UBEHANDLET).any { it.type == SendHenleggelsesbrevTask.TYPE } }
        assertHistorikkTask(behandling.id,
                            TilbakekrevingHistorikkinnslagstype.BEHANDLING_HENLAGT,
                            Aktør.SAKSBEHANDLER,
                            "testverdi")
        assertOppgaveTask(behandling.id, FerdigstillOppgaveTask.TYPE)
    }

    @Test
    fun `henleggBehandling skal henlegge behandling uten henleggelsesbrev`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = false,
                                                finnesVarsel = false,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL)
        var behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        //oppdaterer opprettettidspunkt slik at behandlingen kan henlegges
        behandlingRepository.update(behandling.copy(sporbar = Sporbar(opprettetAv = Constants.BRUKER_ID_VEDTAKSLØSNINGEN,
                                                                      opprettetTid = LocalDateTime.now().minusDays(10))))

        behandlingService
                .henleggBehandling(behandlingId = behandling.id,
                                   henleggelsesbrevFritekstDto = HenleggelsesbrevFritekstDto(
                                           behandlingsresultatstype = Behandlingsresultatstype.HENLAGT_TEKNISK_VEDLIKEHOLD,
                                           begrunnelse = "testverdi"))

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
        assertHistorikkTask(behandling.id,
                            TilbakekrevingHistorikkinnslagstype.BEHANDLING_HENLAGT,
                            Aktør.VEDTAKSLØSNING,
                            "testverdi")
        assertOppgaveTask(behandling.id, FerdigstillOppgaveTask.TYPE)
    }

    @Test
    fun `henleggBehandling skal ikke henlegge behandling som opprettet nå`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = false,
                                                finnesVarsel = false,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL)
        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)

        val exception = assertFailsWith<RuntimeException> {
            behandlingService
                    .henleggBehandling(behandlingId = behandling.id,
                                       henleggelsesbrevFritekstDto = HenleggelsesbrevFritekstDto(
                                               behandlingsresultatstype = Behandlingsresultatstype.HENLAGT_TEKNISK_VEDLIKEHOLD,
                                               begrunnelse = "testverdi"))
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
        val behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        val kravgrunnlag = Testdata.kravgrunnlag431
        kravgrunnlagRepository.insert(kravgrunnlag.copy(behandlingId = behandling.id))

        val exception = assertFailsWith<RuntimeException> {
            behandlingService
                    .henleggBehandling(behandlingId = behandling.id,
                                       henleggelsesbrevFritekstDto = HenleggelsesbrevFritekstDto(
                                               behandlingsresultatstype = Behandlingsresultatstype.HENLAGT_TEKNISK_VEDLIKEHOLD,
                                               begrunnelse = "testverdi"))
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
        var behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(behandling.copy(status = Behandlingsstatus.AVSLUTTET))

        val exception = assertFailsWith<RuntimeException> {
            behandlingService
                    .henleggBehandling(behandlingId = behandling.id,
                                       henleggelsesbrevFritekstDto = HenleggelsesbrevFritekstDto(
                                               behandlingsresultatstype = Behandlingsresultatstype.HENLAGT_TEKNISK_VEDLIKEHOLD,
                                               begrunnelse = "testverdi"))
        }
        assertEquals("Behandling med id=${behandling.id} er allerede ferdig behandlet.", exception.message)
    }

    @Test
    fun `byttBehandlendeEnhet skal bytte og oppdatere oppgave`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = false,
                                                finnesVarsel = false,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL)
        var behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)

        behandlingService.byttBehandlendeEnhet(behandling.id, ByttEnhetDto("4806",
                                                                           "bytter i unittest" +
                                                                           "\n\nmed linjeskift" +
                                                                           "\n\nto til og med"))

        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        assertEquals("4806", behandling.behandlendeEnhet)
        assertEquals("Mock NAV Drammen", behandling.behandlendeEnhetsNavn)

        assertTrue {
            taskRepository.findByStatus(Status.UBEHANDLET).any {
                it.type == OppdaterEnhetOppgaveTask.TYPE &&
                "Endret tildelt enhet: 4806" == it.metadata["beskrivelse"] &&
                "4806" == it.metadata["enhetId"]
            }
        }
        assertHistorikkTask(behandling.id,
                            TilbakekrevingHistorikkinnslagstype.ENDRET_ENHET,
                            Aktør.SAKSBEHANDLER,
                            "bytter i unittest  med linjeskift  to til og med")
    }

    @Test
    fun `byttBehandlendeEnhet skal ikke kunne bytte på behandling med fagsystem EF`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = false,
                                                finnesVarsel = false,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL)
                        .copy(fagsystem = Fagsystem.EF,
                              ytelsestype = BARNETILSYN)
        var behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)

        val exception = assertFailsWith<RuntimeException> {
            behandlingService.byttBehandlendeEnhet(behandling.id, ByttEnhetDto("4806", "bytter i unittest"))
        }
        assertEquals("Ikke implementert for fagsystem EF", exception.message)
    }

    @Test
    fun `byttBehandlendeEnhet skal ikke kunne bytte på avsluttet behandling`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = false,
                                                finnesVarsel = false,
                                                manueltOpprettet = false,
                                                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL)
        var behandling = behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(behandling.copy(status = Behandlingsstatus.AVSLUTTET))

        val exception = assertFailsWith<RuntimeException> {
            behandlingService.byttBehandlendeEnhet(behandling.id, ByttEnhetDto("4806", "bytter i unittest"))
        }
        assertEquals("Behandling med id=${behandling.id} er allerede ferdig behandlet.", exception.message)
    }

    private fun assertFellesBehandlingRespons(behandlingDto: BehandlingDto,
                                              behandling: Behandling) {
        assertEquals(behandling.eksternBrukId, behandlingDto.eksternBrukId)
        assertFalse { behandlingDto.erBehandlingHenlagt }
        assertEquals(Behandlingstype.TILBAKEKREVING, behandlingDto.type)
        assertEquals(Behandlingsstatus.UTREDES, behandlingDto.status)
        assertEquals(behandling.opprettetDato, behandlingDto.opprettetDato)
        assertNull(behandlingDto.avsluttetDato)
        assertNull(behandlingDto.vedtaksdato)
        assertEquals("8020", behandlingDto.enhetskode)
        assertEquals("Oslo", behandlingDto.enhetsnavn)
        assertNull(behandlingDto.resultatstype)
        assertEquals("bb1234", behandlingDto.ansvarligSaksbehandler)
        assertNull(behandlingDto.ansvarligBeslutter)
        assertFalse(behandlingDto.kanRevurderingOpprettes)
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

    private fun assertBehandlingsstegstilstand(behandlingsstegstilstand: List<Behandlingsstegstilstand>,
                                               behandlingssteg: Behandlingssteg,
                                               behandlingsstegstatus: Behandlingsstegstatus,
                                               venteårsak: Venteårsak? = null) {
        assertTrue {
            behandlingsstegstilstand.any {
                it.behandlingssteg == behandlingssteg &&
                it.behandlingsstegsstatus == behandlingsstegstatus
                it.venteårsak == venteårsak
            }
        }
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
                                 opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest,
                                 manueltOpprettet: Boolean? = false) {
        assertEquals(Behandlingstype.TILBAKEKREVING.name, behandling.type.name)
        assertEquals(Behandlingsstatus.OPPRETTET.name, behandling.status.name)
        assertEquals(manueltOpprettet, behandling.manueltOpprettet)
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
                                            faktainfo = faktainfo,
                                            saksbehandlerIdent = "Z0000")
    }

    private fun lagOpprettRevurderingDto(originalBehandlingId: UUID): OpprettRevurderingDto {
        return OpprettRevurderingDto(BARNETRYGD, originalBehandlingId, Behandlingsårsakstype.REVURDERING_OPPLYSNINGER_OM_VILKÅR)
    }

    private fun assertAnsvarligSaksbehandler(behandling: Behandling) {
        val lagretBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        assertEquals("Z0000", lagretBehandling.ansvarligSaksbehandler)
        assertNull(lagretBehandling.ansvarligBeslutter)
    }

    private fun assertHistorikkTask(behandlingId: UUID,
                                    historikkinnslagstype: TilbakekrevingHistorikkinnslagstype,
                                    aktør: Aktør,
                                    tekst: String? = null) {
        assertTrue {
            taskRepository.findByStatus(Status.UBEHANDLET).any {
                LagHistorikkinnslagTask.TYPE == it.type &&
                historikkinnslagstype.name == it.metadata["historikkinnslagstype"] &&
                aktør.name == it.metadata["aktor"] &&
                behandlingId.toString() == it.payload &&
                tekst == it.metadata["beskrivelse"]
            }
        }
    }

    private fun assertFinnKravgrunnlagTask(behandlingId: UUID) {
        assertTrue {
            taskRepository.findByStatus(Status.UBEHANDLET).any {
                FinnKravgrunnlagTask.TYPE == it.type && behandlingId.toString() == it.payload
            }
        }
    }

    private fun assertOppgaveTask(behandlingId: UUID,
                                  taskType: String,
                                  beskrivelse: String? = null,
                                  frist: LocalDate? = null) {
        assertTrue {
            taskRepository.findByStatus(Status.UBEHANDLET).any {
                it.type == taskType &&
                behandlingId.toString() == it.payload &&
                Oppgavetype.BehandleSak.value == it.metadata["oppgavetype"]
                beskrivelse == it.metadata["beskrivelse"] &&
                frist == it.metadata["frist"]?.let { dato -> LocalDate.parse(dato as CharSequence) }
            }
        }
    }
}
