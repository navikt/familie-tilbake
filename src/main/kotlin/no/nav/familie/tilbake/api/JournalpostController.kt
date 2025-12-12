package no.nav.familie.tilbake.api

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.JournalføringService
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.familie.tilbake.kontrakter.journalpost.Journalpost
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.TilgangskontrollService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.dokumentHåndtering.saf.SafService
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/behandling")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class JournalpostController(
    private val journalføringService: JournalføringService,
    private val tilgangskontrollService: TilgangskontrollService,
    private val tilbakekrevingService: TilbakekrevingService,
    private val safService: SafService,
) {
    @Operation(summary = "Hent dokument fra journalføring")
    @GetMapping("/{behandlingId}/journalpost/{journalpostId}/dokument/{dokumentInfoId}")
    fun hentDokument(
        @PathVariable behandlingId: UUID,
        @PathVariable journalpostId: String,
        @PathVariable dokumentInfoId: String,
    ): Ressurs<ByteArray> {
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(behandlingId)
        if (tilbakekreving != null) {
            tilgangskontrollService.validerTilgangTilbakekreving(
                tilbakekreving = tilbakekreving,
                behandlingId = behandlingId,
                minimumBehandlerrolle = Behandlerrolle.VEILEDER,
                auditLoggerEvent = AuditLoggerEvent.ACCESS,
                handling = "Henter vilkårsvurdering for en gitt behandling",
            )
            return Ressurs.success(
                safService.hentDokument(
                    behandlingId = behandlingId,
                    journalpostId = journalpostId,
                    dokumentInfoId = dokumentInfoId,
                    tilbakekreving.eksternFagsak.eksternId,
                ),
                "OK",
            )
        }
        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.VEILEDER,
            auditLoggerEvent = AuditLoggerEvent.ACCESS,
            handling = "Henter journalført dokument",
        )
        return Ressurs.success(journalføringService.hentDokument(journalpostId, dokumentInfoId, behandlingId), "OK")
    }

    @Operation(summary = "Hent journalpost informasjon")
    @GetMapping("/{behandlingId}/journalposter")
    fun hentJournalposter(
        @PathVariable behandlingId: UUID,
    ): Ressurs<List<Journalpost>> {
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(behandlingId)
        if (tilbakekreving != null) {
            tilgangskontrollService.validerTilgangTilbakekreving(
                tilbakekreving = tilbakekreving,
                behandlingId = behandlingId,
                minimumBehandlerrolle = Behandlerrolle.VEILEDER,
                auditLoggerEvent = AuditLoggerEvent.ACCESS,
                handling = "Henter vilkårsvurdering for en gitt behandling",
            )
            return Ressurs.success(safService.hentJournalposter(tilbakekreving, null, null))
        }
        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.VEILEDER,
            auditLoggerEvent = AuditLoggerEvent.ACCESS,
            handling = "Henter journalført dokument",
        )
        return Ressurs.success(journalføringService.hentJournalposter(behandlingId))
    }
}
