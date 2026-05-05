package no.nav.tilbakekreving.api

import jakarta.validation.Valid
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.TilgangskontrollService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.brev.varselbrev.ForhåndsvarselService
import no.nav.tilbakekreving.brev.vedtaksbrev.NyVedtaksbrevService
import no.nav.tilbakekreving.kontrakter.frontend.apis.BehandlingApi
import no.nav.tilbakekreving.kontrakter.frontend.models.BeregningsresultatDto
import no.nav.tilbakekreving.kontrakter.frontend.models.FaktaOmFeilutbetalingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.ForhaandsvarselResponseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.ForhaandsvarselUnntakDto
import no.nav.tilbakekreving.kontrakter.frontend.models.LogginnslagDto
import no.nav.tilbakekreving.kontrakter.frontend.models.OppdaterFaktaOmFeilutbetalingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.SendForhaandsvarselDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UpdateUttalelsesfristDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UttalelseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UttalelsesfristDto
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
    private val forhåndsvarselService: ForhåndsvarselService,
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
        val logContext = SecureLog.Context.fra(tilbakekreving)
        val beslutter = ContextService.hentBehandler(logContext)
        tilgangskontrollService.validerTilgangTilbakekreving(
            tilbakekreving = tilbakekreving,
            behandlingId = UUID.fromString(behandlingId),
            minimumBehandlerrolle = Behandlerrolle.VEILEDER,
            auditLoggerEvent = AuditLoggerEvent.ACCESS,
            handling = "Henter informasjon for bruk i brev",
        )

        return ResponseEntity.ok(
            nyVedtaksbrevService.hentVedtaksbrevData(
                UUID.fromString(behandlingId),
                tilbakekreving.hentVedtaksbrevInfo(UUID.fromString(behandlingId)),
                beslutter,
            ),
        )
    }

    override fun behandlingForeslaaVedtak(behandlingId: UUID): ResponseEntity<Unit> {
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

        return ResponseEntity.ok(
            nyVedtaksbrevService.oppdaterVedtaksbrevData(
                behandlingId = behandlingId,
                data = vedtaksbrevRedigerbareDataUpdateDto,
                info = tilbakekreving.hentVedtaksbrevInfo(behandlingId),
            ),
        )
    }

    override fun behandlingHentVedtaksresultat(behandlingId: UUID): ResponseEntity<BeregningsresultatDto> {
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(behandlingId)
            ?: return ResponseEntity.notFound().build()

        tilgangskontrollService.validerTilgangTilbakekreving(
            tilbakekreving = tilbakekreving,
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.VEILEDER,
            auditLoggerEvent = AuditLoggerEvent.ACCESS,
            handling = "Henter vedtaksresultat",
        )

        return ResponseEntity.ok(tilbakekreving.hentVedtaksresultatForFrontend())
    }

    override fun behandlingBehandlingslogg(behandlingId: UUID): ResponseEntity<List<LogginnslagDto>> {
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(behandlingId)
            ?: return ResponseEntity.notFound().build()

        tilgangskontrollService.validerTilgangTilbakekreving(
            tilbakekreving = tilbakekreving,
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.VEILEDER,
            auditLoggerEvent = AuditLoggerEvent.ACCESS,
            handling = "Henter informasjon for bruk i brev",
        )
        return ResponseEntity.ok(tilbakekrevingService.hentHistorikk(tilbakekreving))
    }

    override fun behandlingForhandsvarsel(behandlingId: UUID): ResponseEntity<ForhaandsvarselResponseDto> {
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(behandlingId)
            ?: return ResponseEntity.notFound().build()

        tilgangskontrollService.validerTilgangTilbakekreving(
            tilbakekreving = tilbakekreving,
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
            auditLoggerEvent = AuditLoggerEvent.ACCESS,
            handling = "Henter informasjon for forhåndsvarsel",
        )
        return ResponseEntity.ok(forhåndsvarselService.nyHentForhåndsvarselinfo(tilbakekreving))
    }

    override fun behandlingSendVarselbrev(behandlingId: UUID, sendForhaandsvarselDto: SendForhaandsvarselDto): ResponseEntity<Unit> {
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(behandlingId)
            ?: return ResponseEntity.notFound().build()

        tilgangskontrollService.validerTilgangTilbakekreving(
            tilbakekreving = tilbakekreving,
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
            auditLoggerEvent = AuditLoggerEvent.UPDATE,
            handling = "Sender forhåndsvarsel brev",
        )
        return tilbakekrevingService.hentTilbakekreving(behandlingId) {
            ResponseEntity.ok(it.sendVarselbrev(sendForhaandsvarselDto.tekstFraSaksbehandler))
        } ?: ResponseEntity.notFound().build()
    }

    override fun behandlingLagreBrukersuttalelse(behandlingId: UUID, uttalelseDto: UttalelseDto): ResponseEntity<Unit> {
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(behandlingId)
            ?: return ResponseEntity.notFound().build()
        val logContext = SecureLog.Context.fra(tilbakekreving)
        val saksbehandler = ContextService.hentBehandler(logContext)
        tilgangskontrollService.validerTilgangTilbakekreving(
            tilbakekreving = tilbakekreving,
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
            auditLoggerEvent = AuditLoggerEvent.UPDATE,
            handling = "Lagrer brukeruttalelse",
        )
        return tilbakekrevingService.hentTilbakekreving(behandlingId) {
            ResponseEntity.ok(forhåndsvarselService.nyLagreUttalelse(it, uttalelseDto, saksbehandler))
        } ?: ResponseEntity.notFound().build()
    }

    override fun behandlingUtsettUttalelsesfrist(behandlingId: UUID, utsettFristDto: UpdateUttalelsesfristDto): ResponseEntity<UttalelsesfristDto> {
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(behandlingId)
            ?: return ResponseEntity.notFound().build()
        val logContext = SecureLog.Context.fra(tilbakekreving)
        val saksbehandler = ContextService.hentBehandler(logContext)
        tilgangskontrollService.validerTilgangTilbakekreving(
            tilbakekreving = tilbakekreving,
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
            auditLoggerEvent = AuditLoggerEvent.UPDATE,
            handling = "Utsetter uttalelsesfrist",
        )
        return tilbakekrevingService.hentTilbakekreving(behandlingId) {
            ResponseEntity.ok(forhåndsvarselService.nyUtsettUttalelsesfrist(it, utsettFristDto, saksbehandler))
        } ?: ResponseEntity.notFound().build()
    }

    override fun behandlingLagreForhaandsvarselUnntak(behandlingId: UUID, unntakDto: ForhaandsvarselUnntakDto): ResponseEntity<Unit> {
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(behandlingId)
            ?: return ResponseEntity.notFound().build()
        val logContext = SecureLog.Context.fra(tilbakekreving)
        val saksbehandler = ContextService.hentBehandler(logContext)
        tilgangskontrollService.validerTilgangTilbakekreving(
            tilbakekreving = tilbakekreving,
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
            auditLoggerEvent = AuditLoggerEvent.UPDATE,
            handling = "Lagrer unntak for forhåndsvarsel",
        )

        return tilbakekrevingService.hentTilbakekreving(behandlingId) {
            ResponseEntity.ok(
                forhåndsvarselService.nyLagreForhåndsvarselUnntak(it, unntakDto, saksbehandler),
            )
        } ?: ResponseEntity.notFound().build()
    }
}
