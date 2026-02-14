package no.nav.tilbakekreving.api

import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.TilgangskontrollService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.brev.vedtaksbrev.BrevFormatterer
import no.nav.tilbakekreving.brev.vedtaksbrev.VedtaksbrevDataRepository
import no.nav.tilbakekreving.kontrakter.frontend.apis.BehandlingApi
import no.nav.tilbakekreving.kontrakter.frontend.models.FaktaOmFeilutbetalingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.HovedavsnittDto
import no.nav.tilbakekreving.kontrakter.frontend.models.OppdaterFaktaOmFeilutbetalingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.RentekstElementDto
import no.nav.tilbakekreving.kontrakter.frontend.models.SignaturDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksbrevDataDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksbrevRedigerbareDataDto
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

@Component
@ProtectedWithClaims(issuer = "azuread")
class BehandlingApiController(
    private val tilbakekrevingService: TilbakekrevingService,
    private val tilgangskontrollService: TilgangskontrollService,
    private val eksterneDataForBrevService: EksterneDataForBrevService,
    private val vedtaksbrevDataRepository: VedtaksbrevDataRepository,
) : BehandlingApi {
    override fun behandlingFakta(behandlingId: String): ResponseEntity<FaktaOmFeilutbetalingDto> {
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(UUID.fromString(behandlingId))
            ?: return ResponseEntity.notFound().build()

        tilgangskontrollService.validerTilgangTilbakekreving(
            tilbakekreving = tilbakekreving,
            behandlingId = UUID.fromString(behandlingId),
            minimumBehandlerrolle = Behandlerrolle.VEILEDER,
            auditLoggerEvent = AuditLoggerEvent.ACCESS,
            handling = "Henter fakta om feilutbetalingen",
        )
        return ResponseEntity.ok(tilbakekreving.tilFeilutbetalingFrontendDto())
    }

    override fun behandlingOppdaterFakta(behandlingId: String, oppdaterFaktaOmFeilutbetalingDto: OppdaterFaktaOmFeilutbetalingDto): ResponseEntity<FaktaOmFeilutbetalingDto> {
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(UUID.fromString(behandlingId))
            ?: return ResponseEntity.notFound().build()

        tilgangskontrollService.validerTilgangTilbakekreving(
            tilbakekreving = tilbakekreving,
            behandlingId = UUID.fromString(behandlingId),
            minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
            auditLoggerEvent = AuditLoggerEvent.UPDATE,
            handling = "Oppdaterer fakta om feilutbetalingen",
        )
        return tilbakekrevingService.hentTilbakekreving(UUID.fromString(behandlingId)) {
            val logContext = SecureLog.Context.fra(tilbakekreving)
            it.vurderFakta(
                behandlingId = UUID.fromString(behandlingId),
                behandler = ContextService.hentBehandler(logContext),
                oppdaget = oppdaterFaktaOmFeilutbetalingDto.vurdering?.oppdaget,
                årsak = oppdaterFaktaOmFeilutbetalingDto.vurdering?.årsak,
                perioder = oppdaterFaktaOmFeilutbetalingDto.perioder,
            )
            ResponseEntity.ok(it.tilFeilutbetalingFrontendDto())
        } ?: ResponseEntity.notFound().build()
    }

    override fun behandlingHentVedtaksbrev(behandlingId: String): ResponseEntity<VedtaksbrevDataDto> {
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(UUID.fromString(behandlingId))
            ?: return ResponseEntity.notFound().build()

        tilgangskontrollService.validerTilgangTilbakekreving(
            tilbakekreving = tilbakekreving,
            behandlingId = UUID.fromString(behandlingId),
            minimumBehandlerrolle = Behandlerrolle.VEILEDER,
            auditLoggerEvent = AuditLoggerEvent.ACCESS,
            handling = "Henter informasjon for bruk i brev",
        )

        val behandling = tilbakekreving.behandlingHistorikk.finn(UUID.fromString(behandlingId), tilbakekreving.sporingsinformasjon())
        val signatur = behandling.entry.brevSignatur()
        return ResponseEntity.ok(
            VedtaksbrevDataDto(
                hovedavsnitt = HovedavsnittDto(
                    tittel = "Du må betale tilbake",
                    underavsnitt = listOf(RentekstElementDto("")),
                ),
                avsnitt = BrevFormatterer.lagAvsnitt(behandling.entry.vurdertePerioderForBrev()),
                brevGjelder = tilbakekreving.bruker!!.brevmeta(),
                ytelse = tilbakekreving.eksternFagsak.brevmeta(),
                sendtDato = BrevFormatterer.norskDato(LocalDate.now()),
                signatur = SignaturDto(
                    signatur.ansvarligEnhet,
                    eksterneDataForBrevService.hentSaksbehandlernavn(signatur.ansvarligSaksbehandlerIdent),
                    signatur.ansvarligBeslutterIdent?.let(eksterneDataForBrevService::hentSaksbehandlernavn),
                ),
            ),
        )
    }

    override fun behandlingOppdaterVedtaksbrev(behandlingId: UUID, vedtaksbrevRedigerbareDataDto: VedtaksbrevRedigerbareDataDto): ResponseEntity<VedtaksbrevRedigerbareDataDto> {
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(behandlingId)
            ?: return ResponseEntity.notFound().build()

        tilgangskontrollService.validerTilgangTilbakekreving(
            tilbakekreving = tilbakekreving,
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.VEILEDER,
            auditLoggerEvent = AuditLoggerEvent.ACCESS,
            handling = "Henter informasjon for bruk i brev",
        )

        return ResponseEntity.ok(vedtaksbrevDataRepository.oppdaterVedtaksbrevData(behandlingId, vedtaksbrevRedigerbareDataDto))
    }
}
