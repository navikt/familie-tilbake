package no.nav.familie.tilbake.api

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import no.nav.familie.tilbake.behandling.LagreUtkastVedtaksbrevService
import no.nav.familie.tilbake.dokumentbestilling.DokumentbehandlingService
import no.nav.familie.tilbake.dokumentbestilling.henleggelse.HenleggelsesbrevService
import no.nav.familie.tilbake.dokumentbestilling.varsel.VarselbrevService
import no.nav.familie.tilbake.dokumentbestilling.vedtak.Avsnitt
import no.nav.familie.tilbake.dokumentbestilling.vedtak.VedtaksbrevService
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.TilgangskontrollService
import no.nav.familie.tilbake.v2.TilbakekrevingService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilbakekreving.api.v1.dto.BestillBrevDto
import no.nav.tilbakekreving.api.v1.dto.ForhåndsvisningHenleggelsesbrevDto
import no.nav.tilbakekreving.api.v1.dto.FritekstavsnittDto
import no.nav.tilbakekreving.api.v1.dto.HentForhåndvisningVedtaksbrevPdfDto
import no.nav.tilbakekreving.kontrakter.ForhåndsvisVarselbrevRequest
import no.nav.tilbakekreving.kontrakter.brev.Dokumentmalstype
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/dokument")
@ProtectedWithClaims(issuer = "azuread")
class DokumentController(
    private val varselbrevService: VarselbrevService,
    private val dokumentbehandlingService: DokumentbehandlingService,
    private val henleggelsesbrevService: HenleggelsesbrevService,
    private val vedtaksbrevService: VedtaksbrevService,
    private val lagreUtkastVedtaksbrevService: LagreUtkastVedtaksbrevService,
    private val tilgangskontrollService: TilgangskontrollService,
    private val tilbakekrevingService: TilbakekrevingService,
) {
    @Operation(summary = "Bestill brevsending")
    @PostMapping("/bestill")
    fun bestillBrev(
        @RequestBody @Valid
        bestillBrevDto: BestillBrevDto,
    ): Ressurs<Nothing?> {
        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = bestillBrevDto.behandlingId,
            minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
            auditLoggerEvent = AuditLoggerEvent.CREATE,
            handling = "Sender brev",
        )
        val maltype: Dokumentmalstype = bestillBrevDto.brevmalkode
        dokumentbehandlingService.bestillBrev(bestillBrevDto.behandlingId, maltype, bestillBrevDto.fritekst)
        return Ressurs.success(null)
    }

    @Operation(summary = "Forhåndsvis brev")
    @PostMapping("/forhandsvis")
    fun forhåndsvisBrev(
        @RequestBody @Valid
        bestillBrevDto: BestillBrevDto,
    ): Ressurs<ByteArray> {
        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = bestillBrevDto.behandlingId,
            minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
            auditLoggerEvent = AuditLoggerEvent.ACCESS,
            handling = "Forhåndsviser brev",
        )
        val dokument: ByteArray =
            dokumentbehandlingService.forhåndsvisBrev(
                bestillBrevDto.behandlingId,
                bestillBrevDto.brevmalkode,
                bestillBrevDto.fritekst,
            )
        return Ressurs.success(dokument)
    }

    @Operation(summary = "Forhåndsvis varselbrev")
    @PostMapping(
        "/forhandsvis-varselbrev",
        produces = [MediaType.APPLICATION_PDF_VALUE],
    )
    fun hentForhåndsvisningVarselbrev(
        @Valid @RequestBody
        forhåndsvisVarselbrevRequest: ForhåndsvisVarselbrevRequest,
    ): ByteArray {
        tilgangskontrollService.validerTilgangFagsystemOgFagsakId(
            fagsystem = forhåndsvisVarselbrevRequest.fagsystem,
            eksternFagsakId = forhåndsvisVarselbrevRequest.eksternFagsakId,
            minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
            auditLoggerEvent = AuditLoggerEvent.ACCESS,
            handling = "Forhåndsviser brev",
        )
        return varselbrevService.hentForhåndsvisningVarselbrev(forhåndsvisVarselbrevRequest)
    }

    @Operation(summary = "Forhåndsvis henleggelsesbrev")
    @PostMapping(
        "/forhandsvis-henleggelsesbrev",
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentForhåndsvisningHenleggelsesbrev(
        @Valid @RequestBody
        dto: ForhåndsvisningHenleggelsesbrevDto,
    ): Ressurs<ByteArray> {
        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = dto.behandlingId,
            minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
            auditLoggerEvent = AuditLoggerEvent.ACCESS,
            handling = "Forhåndsviser henleggelsesbrev",
        )
        return Ressurs.success(henleggelsesbrevService.hentForhåndsvisningHenleggelsesbrev(dto.behandlingId, dto.fritekst))
    }

    @Operation(summary = "Forhåndsvis vedtaksbrev")
    @PostMapping(
        "/forhandsvis-vedtaksbrev",
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentForhåndsvisningVedtaksbrev(
        @Valid @RequestBody
        hentForhåndsvisningVedtaksbrevRequest: HentForhåndvisningVedtaksbrevPdfDto,
    ): Ressurs<ByteArray> {
        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = hentForhåndsvisningVedtaksbrevRequest.behandlingId,
            minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
            auditLoggerEvent = AuditLoggerEvent.ACCESS,
            handling = "Forhåndsviser brev",
        )
        return Ressurs.success(vedtaksbrevService.hentForhåndsvisningVedtaksbrevMedVedleggSomPdf(hentForhåndsvisningVedtaksbrevRequest))
    }

    @Operation(summary = "Hent vedtaksbrevtekst")
    @GetMapping(
        "/vedtaksbrevtekst/{behandlingId}",
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentVedtaksbrevtekst(
        @PathVariable behandlingId: UUID,
    ): Ressurs<List<Avsnitt>> {
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(behandlingId)
        if (tilbakekreving != null) {
            tilgangskontrollService.validerTilgangTilbakekreving(
                tilbakekreving = tilbakekreving,
                behandlingId = behandlingId,
                minimumBehandlerrolle = Behandlerrolle.VEILEDER,
                auditLoggerEvent = AuditLoggerEvent.ACCESS,
                handling = "Henter vedtaksbrevtekst",
            )
            return Ressurs.success(emptyList())
        }
        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.VEILEDER,
            auditLoggerEvent = AuditLoggerEvent.ACCESS,
            handling = "Henter vedtaksbrevtekst",
        )
        return Ressurs.success(vedtaksbrevService.hentVedtaksbrevSomTekst(behandlingId))
    }

    @Operation(summary = "Lagre utkast av vedtaksbrev")
    @PostMapping(
        "/vedtaksbrevtekst/{behandlingId}/utkast",
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun lagreUtkastVedtaksbrev(
        @PathVariable behandlingId: UUID,
        @RequestBody fritekstavsnitt: FritekstavsnittDto,
    ): Ressurs<String> {
        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
            auditLoggerEvent = AuditLoggerEvent.UPDATE,
            handling = "Lagrer utkast av vedtaksbrev",
        )
        lagreUtkastVedtaksbrevService.lagreUtkast(behandlingId, fritekstavsnitt)
        return Ressurs.success("OK")
    }
}
