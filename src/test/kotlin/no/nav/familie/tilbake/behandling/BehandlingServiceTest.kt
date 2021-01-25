package no.nav.familie.tilbake.behandling

import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.tilbakekreving.BehandlingType
import no.nav.familie.kontrakter.felles.tilbakekreving.FeilutbetaltePerioder
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Periode
import no.nav.familie.kontrakter.felles.tilbakekreving.Verge
import no.nav.familie.kontrakter.felles.tilbakekreving.VergeType
import no.nav.familie.tilbake.OppslagSpringRunnerTest
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
    fun `skal_opprette_automatisk_behandling_uten_verge`() {
        val opprettTilbakekrevingRequest = lagOpprettTilbakekrevingRequest(finnesVerge = false, manueltOpprettet = false)
        val behandling = behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest)

        assertBehandling(behandling, opprettTilbakekrevingRequest)
        assertFagsak(behandling, opprettTilbakekrevingRequest)
        assertEksternBehandling(behandling, opprettTilbakekrevingRequest)
        assertVarselData(behandling, opprettTilbakekrevingRequest)
        assertTrue { behandling.verger.isEmpty() }
    }

    @Test
    fun `skal_opprette_automatisk_behandling_med_verge`() {
        val opprettTilbakekrevingRequest = lagOpprettTilbakekrevingRequest(finnesVerge = true, manueltOpprettet = false)
        val behandling = behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest)

        assertBehandling(behandling, opprettTilbakekrevingRequest)
        assertFagsak(behandling, opprettTilbakekrevingRequest)
        assertEksternBehandling(behandling, opprettTilbakekrevingRequest)
        assertVarselData(behandling, opprettTilbakekrevingRequest)
        assertVerge(behandling, opprettTilbakekrevingRequest)
    }

    @Test
    fun `skal_ikke_opprette_automatisk_behandling_når_det_allerede_finnes_en_åpen_tilbakekreving_for_samme_eksternFagsakId`() {
        val opprettTilbakekrevingRequest = lagOpprettTilbakekrevingRequest(finnesVerge = true, manueltOpprettet = false)
        behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest)

        assertFailsWith<RuntimeException>(message = "Det finnes allerede en åpen behandling for fagsystem=" + opprettTilbakekrevingRequest.fagsystem +
                                                    " og eksternFagsakId=${opprettTilbakekrevingRequest.eksternFagsakId}, kan ikke opprettes en ny.",
                                          block = { behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest) })
    }

    @Test
    fun `skal_ikke_opprette_automatisk_behandling_når_siste_tilbakekreving_er_ikke_henlagt`() {
        val opprettTilbakekrevingRequest = lagOpprettTilbakekrevingRequest(finnesVerge = true, manueltOpprettet = false)
        val behandling = behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest)
        behandlingRepository.update(behandling.copy(status = Behandlingsstatus.AVSLUTTET))
        assertFailsWith<RuntimeException>(message = "Det finnes allerede en avsluttet behandling for fagsystem=" + opprettTilbakekrevingRequest.fagsystem +
                                                    " og eksternFagsakId=${opprettTilbakekrevingRequest.eksternFagsakId} som ikke er henlagt, kan ikke opprettes en ny.",
                                          block = { behandlingService.opprettBehandlingAutomatisk(opprettTilbakekrevingRequest) })
    }

    private fun assertFagsak(behandling: Behandling,
                             opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest) {
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        assertEquals(opprettTilbakekrevingRequest.eksternFagsakId, fagsak.eksternFagsakId)
        assertEquals(opprettTilbakekrevingRequest.fagsystem, fagsak.ytelsestype.name)
        assertEquals(Fagsystem.fraYtelsestype(fagsak.ytelsestype).name, fagsak.fagsystem.name)
        assertEquals(Fagsaksstatus.OPPRETTET, fagsak.status)
    }

    private fun assertBehandling(behandling: Behandling,
                                 opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest) {
        assertEquals(BehandlingType.TILBAKEKREVING.name, behandling.type.kode)
        assertEquals(Behandlingsstatus.OPPRETTET, behandling.status)
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
        val eksternBehandling = eksternBehandlinger.toList().get(0)
        assertEquals(true, eksternBehandling.aktiv)
        assertEquals(opprettTilbakekrevingRequest.eksternId, eksternBehandling.eksternId)
    }

    private fun assertVarselData(behandling: Behandling,
                                 opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest) {
        val varsler = behandling.varsler
        assertEquals(1, varsler.size)
        val varsel = varsler.toList().get(0)
        assertEquals(opprettTilbakekrevingRequest.revurderingVedtakDato, varsel.revurderingsvedtaksdato)
        assertEquals(opprettTilbakekrevingRequest.varselTekst, varsel.varseltekst)
        assertEquals(opprettTilbakekrevingRequest.feilutbetaltePerioder.sumFeilutbetaling, varsel.varselbeløp.toBigDecimal())
        assertEquals(opprettTilbakekrevingRequest.feilutbetaltePerioder.perioder.size, varsel.perioder.size)
        assertEquals(opprettTilbakekrevingRequest.feilutbetaltePerioder.perioder.get(0).fom, varsel.perioder.toList().get(0).fom)
        assertEquals(opprettTilbakekrevingRequest.feilutbetaltePerioder.perioder.get(0).tom, varsel.perioder.toList().get(0).tom)
    }

    private fun assertVerge(behandling: Behandling,
                            opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest) {
        assertTrue { behandling.verger.isNotEmpty() }
        assertEquals(1, behandling.verger.size)
        val verge = behandling.verger.toList().get(0)
        assertEquals(opprettTilbakekrevingRequest.verge?.vergeType?.navn, verge.type.navn)
        assertEquals(opprettTilbakekrevingRequest.verge?.gyldigFom, verge.gyldigFom)
        assertEquals(opprettTilbakekrevingRequest.verge?.gyldigTom, verge.gyldigTom)
        assertEquals(opprettTilbakekrevingRequest.verge?.navn, verge.navn)
        assertEquals(opprettTilbakekrevingRequest.verge?.organisasjonsnummer, verge.orgNr)
        assertEquals(opprettTilbakekrevingRequest.verge?.personIdent?.ident, verge.ident)
    }

    private fun lagOpprettTilbakekrevingRequest(finnesVerge: Boolean,
                                                manueltOpprettet: Boolean): OpprettTilbakekrevingRequest {
        val feilutbetaltePerioder = FeilutbetaltePerioder(BigDecimal.valueOf(1500L),
                                                          listOf(Periode(fom, tom)))
        val verge = if (finnesVerge) Verge(vergeType = VergeType.BARN,
                                           gyldigFom = fom,
                                           gyldigTom = tom.plusDays(100),
                                           navn = "Andy",
                                           personIdent = PersonIdent(ident = "321321321")) else null

        return OpprettTilbakekrevingRequest(fagsystem = "BA",
                                            eksternFagsakId = "1234567",
                                            personIdent = PersonIdent(ident = "321321322"),
                                            eksternId = UUID.randomUUID().toString(),
                                            manueltOpprettet = manueltOpprettet,
                                            enhetId = "8020",
                                            enhetsnavn = "Oslo",
                                            varselTekst = "testverdi",
                                            feilutbetaltePerioder = feilutbetaltePerioder,
                                            revurderingVedtakDato = fom,
                                            verge = verge
        )
    }
}
