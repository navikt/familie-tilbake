package no.nav.familie.tilbake.api

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import no.nav.familie.tilbake.behandling.LagreUtkastVedtaksbrevService
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.dokumentbestilling.DokumentbehandlingService
import no.nav.familie.tilbake.dokumentbestilling.henleggelse.HenleggelsesbrevService
import no.nav.familie.tilbake.dokumentbestilling.varsel.VarselbrevService
import no.nav.familie.tilbake.dokumentbestilling.vedtak.VedtaksbrevService
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.TilgangskontrollService
import no.nav.familie.tilbake.sikkerhet.ValideringContext
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.api.v1.dto.BestillBrevDto
import no.nav.tilbakekreving.api.v1.dto.BrukeruttalelseDto
import no.nav.tilbakekreving.api.v1.dto.ForhåndsvarselDto
import no.nav.tilbakekreving.api.v1.dto.ForhåndsvarselUnntakDto
import no.nav.tilbakekreving.api.v1.dto.ForhåndsvisningHenleggelsesbrevDto
import no.nav.tilbakekreving.api.v1.dto.FristUtsettelseDto
import no.nav.tilbakekreving.api.v1.dto.FritekstavsnittDto
import no.nav.tilbakekreving.api.v1.dto.HentForhåndvisningVedtaksbrevPdfDto
import no.nav.tilbakekreving.api.v1.dto.VarslingsUnntak
import no.nav.tilbakekreving.behandling.BegrunnelseForUnntak
import no.nav.tilbakekreving.brev.varselbrev.ForhåndsvarselService
import no.nav.tilbakekreving.brev.varselbrev.Varselbrevtekst
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.kontrakter.ForhåndsvisVarselbrevRequest
import no.nav.tilbakekreving.kontrakter.brev.Dokumentmalstype
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.Avsnitt
import no.nav.tilbakekreving.repository.TilbakekrevingFilter
import org.springframework.http.HttpStatus
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
    private val forhåndsvarselService: ForhåndsvarselService,
) {
    @Operation(summary = "Bestill brevsending")
    @PostMapping("/bestill")
    fun bestillBrev(
        @RequestBody @Valid
        bestillBrevDto: BestillBrevDto,
    ): Ressurs<Nothing?> {
        val responseNyModell = tilbakekrevingService.endreTilbakekreving(TilbakekrevingFilter.behandling(bestillBrevDto.behandlingId), ValideringContext.BestillBrev) { tilbakekreving, context ->
            when (bestillBrevDto.brevmalkode) {
                Dokumentmalstype.VARSEL -> {
                    tilbakekreving.hentBehandling(bestillBrevDto.behandlingId).nullstillForhåndsvarselUnntakOgUttalelse()
                    tilbakekreving.sendVarselbrev(bestillBrevDto.behandlingId, bestillBrevDto.fritekst, context)
                }

                else -> throw Feil(
                    message = "Håndtering av ${bestillBrevDto.brevmalkode} støttes ikke enda",
                    httpStatus = HttpStatus.BAD_REQUEST,
                    logContext = SecureLog.Context.fra(tilbakekreving),
                )
            }
            Ressurs.success(null)
        }
        if (responseNyModell != null) {
            return responseNyModell
        }

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
    @PostMapping("/forhandsvis/behandling/{behandlingId}")
    fun forhåndsvisBrev(
        @PathVariable("behandlingId") behandlingId: UUID,
        @RequestBody @Valid
        bestillBrevDto: BestillBrevDto,
    ): Ressurs<ByteArray> {
        val tilbakekreving = tilbakekrevingService.lesTilbakekreving(TilbakekrevingFilter.behandling(bestillBrevDto.behandlingId), ValideringContext.ForhåndsvisBrev)
        if (tilbakekreving != null) {
            return Ressurs.success(forhåndsvarselService.forhåndsvisVarselbrev(tilbakekrevingService.lesecontext(), tilbakekreving, bestillBrevDto))
        }

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

    @Operation(summary = "Hent forhåndsvarselinformasjon")
    @GetMapping(
        path = ["/forhåndsvarsel/behandling/{behandlingId}/v1"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentForhåndsvarselinfo(
        @PathVariable("behandlingId") behandlingId: UUID,
    ): Ressurs<ForhåndsvarselDto> {
        val tilbakekreving = tilbakekrevingService.lesTilbakekreving(TilbakekrevingFilter.behandling(behandlingId), ValideringContext.HentForhåndsvarselinformasjon)
            ?: return Ressurs.failure("Fant ingen tilbakekreving til behandlingId $behandlingId")
        return Ressurs.success(tilbakekreving.hentForhåndsvarselFrontendDto(behandlingId))
    }

    @Operation(summary = "Henter varselbrevtekst")
    @GetMapping(
        "/varselbrevtekst/{behandlingId}",
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentForhåndsvarselTekst(
        @PathVariable("behandlingId") behandlingId: UUID,
    ): Ressurs<Varselbrevtekst> {
        val tilbakekreving = tilbakekrevingService.lesTilbakekreving(TilbakekrevingFilter.behandling(behandlingId), ValideringContext.HentForhåndsvarselTekster)
        if (tilbakekreving != null) {
            return Ressurs.success(forhåndsvarselService.hentVarselbrevTekster(tilbakekrevingService.lesecontext(), behandlingId, tilbakekreving))
        }
        return Ressurs.failure("Fant ingen tilbakekreving til behandlingId $behandlingId")
    }

    @Operation(summary = "Skal utsette uttalelse frist")
    @PostMapping(
        "/forhåndsvarsel/behandling/{behandlingId}/utsettelse",
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun utsettUttalelseFrist(
        @PathVariable behandlingId: UUID,
        @Valid @RequestBody
        dto: FristUtsettelseDto,
    ): Ressurs<Nothing?> {
        return tilbakekrevingService.endreTilbakekreving(TilbakekrevingFilter.behandling(behandlingId), ValideringContext.RegistrerUtsattFrist) { tilbakekreving, context ->
            tilbakekreving.gjørSaksbehandling(behandlingId, context) {
                lagreFristUtsettelse(dto.nyFrist!!, dto.begrunnelse!!, context)
            }
            Ressurs.success(null)
        } ?: Ressurs.failure("Fant ingen tilbakekreving til behandlingId $behandlingId")
    }

    @Operation(summary = "Skal ikke sendes forhåndsvarsel")
    @PostMapping(
        "/forhåndsvarsel/behandling/{behandlingId}/unntak",
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun forhåndsvarselUnntak(
        @PathVariable behandlingId: UUID,
        @Valid @RequestBody
        dto: ForhåndsvarselUnntakDto,
    ): Ressurs<Nothing?> {
        return tilbakekrevingService.endreTilbakekreving(TilbakekrevingFilter.behandling(behandlingId), ValideringContext.RegistrerForhåndsvarselUnntak) { tilbakekreving, context ->
            tilbakekreving.gjørSaksbehandling(behandlingId, context) {
                lagreForhåndsvarselUnntak(
                    begrunnelseForUnntak = when (dto.begrunnelseForUnntak) {
                        VarslingsUnntak.IKKE_PRAKTISK_MULIG -> BegrunnelseForUnntak.IKKE_PRAKTISK_MULIG
                        VarslingsUnntak.UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING -> BegrunnelseForUnntak.UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING
                        VarslingsUnntak.ÅPENBART_UNØDVENDIG -> BegrunnelseForUnntak.ÅPENBART_UNØDVENDIG
                    },
                    beskrivelse = dto.beskrivelse,
                    sideeffektContext = context,
                )
            }
            Ressurs.success(null)
        } ?: Ressurs.failure("Fant ingen tilbakekreving til behandlingId $behandlingId")
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
        val tilbakekreving = tilbakekrevingService.lesTilbakekreving(TilbakekrevingFilter.behandling(behandlingId), ValideringContext.HentVedtaksbrevTekster)
        if (tilbakekreving != null) {
            throw ModellFeil.UgyldigOperasjonException("Kan ikke hente vedtaksbrev-tekst for ny modell", tilbakekreving.sporingsinformasjon(behandlingId))
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

    @Operation(summary = "Lagrer brukerens uttalelse")
    @PostMapping(
        "/forhåndsvarsel/behandling/{behandlingId}/uttalelse",
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun lagreBrukeruttalelse(
        @PathVariable behandlingId: UUID,
        @RequestBody brukeruttalelse: BrukeruttalelseDto,
    ): Ressurs<Nothing?> {
        return tilbakekrevingService.endreTilbakekreving(TilbakekrevingFilter.behandling(behandlingId), ValideringContext.RegistrerBrukeruttalelse) { tilbakekreving, context ->
            forhåndsvarselService.lagreUttalelse(tilbakekreving, behandlingId, brukeruttalelse, context)
            Ressurs.success(null)
        } ?: Ressurs.failure("Fant ingen tilbakekreving til behandlingId $behandlingId")
    }
}
