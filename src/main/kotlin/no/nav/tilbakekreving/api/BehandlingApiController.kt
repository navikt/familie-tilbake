package no.nav.tilbakekreving.api

import jakarta.validation.Valid
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.TilgangskontrollService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.brev.vedtaksbrev.NyVedtaksbrevService
import no.nav.tilbakekreving.kontrakter.frontend.apis.BehandlingApi
import no.nav.tilbakekreving.kontrakter.frontend.models.FaktaOmFeilutbetalingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.OppdaterFaktaOmFeilutbetalingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksbrevDataDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksbrevRedigerbareDataDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksbrevRedigerbareDataUpdateDto
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import java.util.UUID

@Component
@ProtectedWithClaims(issuer = "azuread")
class BehandlingApiController(
    private val tilbakekrevingService: TilbakekrevingService,
    private val tilgangskontrollService: TilgangskontrollService,
    private val nyVedtaksbrevService: NyVedtaksbrevService,
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

    @Validated
    override fun behandlingOppdaterFakta(
        behandlingId: String,
        @Valid oppdaterFaktaOmFeilutbetalingDto: OppdaterFaktaOmFeilutbetalingDto,
    ): ResponseEntity<FaktaOmFeilutbetalingDto> {
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

        return ResponseEntity.ok(nyVedtaksbrevService.hentVedtaksbrevData(UUID.fromString(behandlingId), tilbakekreving.hentVedtaksbrevInfo(UUID.fromString(behandlingId))))
    }

    override fun behandlingForeslVedtak(behandlingId: UUID): ResponseEntity<Unit> {
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(behandlingId)
            ?: return ResponseEntity.notFound().build()

        tilgangskontrollService.validerTilgangTilbakekreving(
            tilbakekreving = tilbakekreving,
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
            auditLoggerEvent = AuditLoggerEvent.UPDATE,
            handling = "Foreslår vedtak",
        )
        return tilbakekrevingService.hentTilbakekreving(behandlingId) {
            val logContext = SecureLog.Context.fra(it)
            it.håndterForeslåVedtak(ContextService.hentBehandler(logContext))
            ResponseEntity.ok(Unit)
        } ?: ResponseEntity.notFound().build()
    }

    override fun behandlingOppdaterVedtaksbrev(behandlingId: UUID, vedtaksbrevRedigerbareDataUpdateDto: VedtaksbrevRedigerbareDataUpdateDto): ResponseEntity<VedtaksbrevRedigerbareDataDto> {
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(behandlingId)
            ?: return ResponseEntity.notFound().build()

        tilgangskontrollService.validerTilgangTilbakekreving(
            tilbakekreving = tilbakekreving,
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.VEILEDER,
            auditLoggerEvent = AuditLoggerEvent.ACCESS,
            handling = "Henter informasjon for bruk i brev",
        )

        return ResponseEntity.ok(nyVedtaksbrevService.oppdaterVedtaksbrevData(behandlingId, vedtaksbrevRedigerbareDataUpdateDto))
    }
}
