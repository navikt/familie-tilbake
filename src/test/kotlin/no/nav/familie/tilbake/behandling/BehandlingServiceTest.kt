package no.nav.familie.tilbake.behandling

import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingstype
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Periode
import no.nav.familie.kontrakter.felles.tilbakekreving.Varsel
import no.nav.familie.kontrakter.felles.tilbakekreving.Verge
import no.nav.familie.kontrakter.felles.tilbakekreving.Vergetype
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype.BARNETRYGD
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.api.dto.BehandlingDto
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandling.domain.Fagsaksstatus
import no.nav.familie.tilbake.behandling.domain.Fagsystem
import no.nav.familie.tilbake.behandling.domain.Saksbehandlingstype
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class BehandlingServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingService: BehandlingService

    private final val fom: LocalDate = LocalDate.now().minusMonths(1)
    private final val tom: LocalDate = LocalDate.now()


    @Test
    fun `opprettBehandlingAutomatisk skal opprette automatisk behandling uten verge`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = false, finnesVarsel = true, manueltOpprettet = false)

        val behandling = behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest)

        assertBehandling(behandling, opprettTilbakekrevingRequest)
        assertFagsak(behandling, opprettTilbakekrevingRequest)
        assertEksternBehandling(behandling, opprettTilbakekrevingRequest)
        assertVarselData(behandling, opprettTilbakekrevingRequest)
        assertTrue { behandling.verger.isEmpty() }
    }

    @Test
    fun `opprettBehandlingAutomatisk skal opprette automatisk behandling med verge`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = true, finnesVarsel = true, manueltOpprettet = false)

        val behandling = behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest)

        assertBehandling(behandling, opprettTilbakekrevingRequest)
        assertFagsak(behandling, opprettTilbakekrevingRequest)
        assertEksternBehandling(behandling, opprettTilbakekrevingRequest)
        assertVarselData(behandling, opprettTilbakekrevingRequest)
        assertVerge(behandling, opprettTilbakekrevingRequest)
    }

    @Test
    fun `opprettBehandlingAutomatisk skal opprette automatisk behandling uten varsel`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = false, finnesVarsel = false, manueltOpprettet = false)

        val behandling = behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest)

        assertBehandling(behandling, opprettTilbakekrevingRequest)
        assertFagsak(behandling, opprettTilbakekrevingRequest)
        assertEksternBehandling(behandling, opprettTilbakekrevingRequest)
        assertTrue { behandling.varsler.isEmpty() }
        assertTrue { behandling.verger.isEmpty() }
    }

    @Test
    fun `opprettBehandlingAutomatisk oppretter ikke behandling når det finnes åpen tilbakekreving for samme eksternFagsakId`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = true, finnesVarsel = true, manueltOpprettet = false)
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
                lagOpprettTilbakekrevingRequest(finnesVerge = true, finnesVarsel = true, manueltOpprettet = false)

        val behandling = behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest)
        behandlingRepository.update(behandling.copy(status = Behandlingsstatus.AVSLUTTET))

        val exception = assertFailsWith<RuntimeException>(block = {
            behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest)
        })
        assertEquals("Det finnes allerede en avsluttet behandling for ytelsestype="
                     + opprettTilbakekrevingRequest.ytelsestype +
                     " og eksternFagsakId=${opprettTilbakekrevingRequest.eksternFagsakId} " +
                     "som ikke er henlagt, kan ikke opprette en ny.", exception.message)
    }

    @Test
    fun `hentBehandling skal hente behandling som ikke kan henlegges med verge`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = true, finnesVarsel = true, manueltOpprettet = false)
        val behandling = behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest)
        val behandlingDto = behandlingService.hentBehandling(behandling.id)

        assertFellesBehandlingRespons(behandlingDto, behandling)
        assertFalse { behandlingDto.kanHenleggeBehandling }
        assertTrue { behandlingDto.harVerge }
    }

    @Test
    fun `hentBehandling skal hente behandling som kan henlegges uten verge`() {
        val opprettTilbakekrevingRequest =
                lagOpprettTilbakekrevingRequest(finnesVerge = false, finnesVarsel = true, manueltOpprettet = false)
        val behandling = behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest)
        val sporbar = behandling.sporbar.copy(opprettetTid = LocalDate.now().minusDays(10).atStartOfDay())
        val oppdatertBehandling = behandling.copy(sporbar = sporbar)
        behandlingRepository.update(oppdatertBehandling)

        val behandlingDto = behandlingService.hentBehandling(behandling.id)

        assertFellesBehandlingRespons(behandlingDto, oppdatertBehandling)
        assertTrue { behandlingDto.kanHenleggeBehandling }
        assertFalse { behandlingDto.harVerge }
    }

    @Test
    fun `hentBehandling skal ikke hente behandling når behandling ikke finnes`() {
        val behandlingId = UUID.randomUUID()
        val exception = assertFailsWith<RuntimeException>(block = { behandlingService.hentBehandling(behandlingId) })
        assertEquals("Behandling finnes ikke for behandlingId=$behandlingId", exception.message)
    }

    private fun assertFellesBehandlingRespons(behandlingDto: BehandlingDto,
                                              behandling: Behandling) {
        assertEquals(behandling.eksternBrukId, behandlingDto.eksternBrukId)
        assertFalse { behandlingDto.erBehandlingHenlagt }
        assertEquals(Behandlingstype.TILBAKEKREVING.name, behandlingDto.type.name)
        assertEquals(Behandlingsstatus.OPPRETTET, behandlingDto.status)
        assertEquals(behandling.opprettetDato, behandlingDto.opprettetDato)
        assertNull(behandlingDto.avsluttetDato)
        assertNull(behandlingDto.vedtaksdato)
        assertEquals("8020", behandlingDto.enhetskode)
        assertEquals("Oslo", behandlingDto.enhetsnavn)
        assertNull(behandlingDto.resultatstype)
        assertEquals("VL", behandlingDto.ansvarligSaksbehandler)
        assertNull(behandlingDto.ansvarligBeslutter)
        assertFalse { behandlingDto.erBehandlingPåVent }
    }

    private fun assertFagsak(behandling: Behandling,
                             opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest) {
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        assertEquals(opprettTilbakekrevingRequest.eksternFagsakId, fagsak.eksternFagsakId)
        assertEquals(opprettTilbakekrevingRequest.ytelsestype.name, fagsak.ytelsestype.name)
        assertEquals(Fagsystem.fraYtelsestype(fagsak.ytelsestype).name, fagsak.fagsystem.name)
        assertEquals(Fagsaksstatus.OPPRETTET, fagsak.status)
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

    private fun assertEksternBehandling(behandling: Behandling,
                                        opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest) {
        val eksternBehandlinger = behandling.eksternBehandling
        assertEquals(1, eksternBehandlinger.size)
        val eksternBehandling = eksternBehandlinger.toList().first()
        assertEquals(true, eksternBehandling.aktiv)
        assertEquals(opprettTilbakekrevingRequest.eksternId, eksternBehandling.eksternId)
    }

    private fun assertVarselData(behandling: Behandling,
                                 opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest) {
        val varsler = behandling.varsler
        assertEquals(1, varsler.size)
        val varsel = varsler.toList().first()
        assertEquals(opprettTilbakekrevingRequest.revurderingsvedtaksdato, varsel.revurderingsvedtaksdato)
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
        assertEquals(opprettTilbakekrevingRequest.verge?.personIdent?.ident, verge.ident)
    }

    private fun lagOpprettTilbakekrevingRequest(finnesVerge: Boolean,
                                                finnesVarsel: Boolean,
                                                manueltOpprettet: Boolean): OpprettTilbakekrevingRequest {
        val varsel = if (finnesVarsel) Varsel(varseltekst = "testverdi",
                                              sumFeilutbetaling = BigDecimal.valueOf(1500L),
                                              perioder = listOf(Periode(fom, tom))) else null
        val verge = if (finnesVerge) Verge(vergetype = Vergetype.VERGE_FOR_BARN,
                                           gyldigFom = fom,
                                           gyldigTom = tom.plusDays(100),
                                           navn = "Andy",
                                           personIdent = PersonIdent(ident = "321321321")) else null

        return OpprettTilbakekrevingRequest(ytelsestype = BARNETRYGD,
                                            eksternFagsakId = "1234567",
                                            personIdent = PersonIdent(ident = "321321322"),
                                            eksternId = UUID.randomUUID().toString(),
                                            manueltOpprettet = manueltOpprettet,
                                            enhetId = "8020",
                                            enhetsnavn = "Oslo",
                                            varsel = varsel,
                                            revurderingsvedtaksdato = fom,
                                            verge = verge
        )
    }
}
