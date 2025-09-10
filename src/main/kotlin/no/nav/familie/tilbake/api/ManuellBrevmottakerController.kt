package no.nav.familie.tilbake.api

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.ManuellBrevmottakerMapper
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.ManuellBrevmottakerService
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.TilgangskontrollService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.api.v1.dto.ManuellBrevmottakerRequestDto
import no.nav.tilbakekreving.api.v1.dto.ManuellBrevmottakerResponsDto
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/brevmottaker/manuell")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class ManuellBrevmottakerController(
    private val manuellBrevmottakerService: ManuellBrevmottakerService,
    private val tilgangskontrollService: TilgangskontrollService,
    private val tilbakekrevingService: TilbakekrevingService,
) {
    @Operation(summary = "Legger til brevmottaker manuelt")
    @PostMapping(
        path = ["/{behandlingId}"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun leggTilBrevmottaker(
        @PathVariable behandlingId: UUID,
        @Valid @RequestBody
        manuellBrevmottakerRequestDto: ManuellBrevmottakerRequestDto,
    ): Ressurs<UUID> {
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(behandlingId)
        if (tilbakekreving != null) {
            val logContext = SecureLog.Context.fra(tilbakekreving)
            val saksbehandler = ContextService.hentBehandler(logContext)
            tilgangskontrollService.validerTilgangBehandlingID(
                behandlingId = behandlingId,
                minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                auditLoggerEvent = AuditLoggerEvent.UPDATE,
                handling = "Legger til brevmottaker manuelt",
            )
            val id = UUID.randomUUID()
            tilbakekrevingService.behandleBrevmottaker(saksbehandler, tilbakekreving, manuellBrevmottakerRequestDto, id)
            return Ressurs.success(id, melding = "Manuell brevmottaker er lagt til.")
        }

        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
            auditLoggerEvent = AuditLoggerEvent.CREATE,
            handling = "Legger til brevmottaker manuelt",
        )
        val id = manuellBrevmottakerService.leggTilBrevmottaker(behandlingId, manuellBrevmottakerRequestDto)
        return Ressurs.success(id, melding = "Manuell brevmottaker er lagt til.")
    }

    @Operation(summary = "Henter manuell brevmottakere")
    @GetMapping(
        path = ["/{behandlingId}"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentManuellBrevmottakere(
        @PathVariable behandlingId: UUID,
    ): Ressurs<List<ManuellBrevmottakerResponsDto>> {
        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
            auditLoggerEvent = AuditLoggerEvent.ACCESS,
            handling = "Henter manuelle brevmottakere",
        )
        return Ressurs
            .success(
                manuellBrevmottakerService
                    .hentBrevmottakere(behandlingId)
                    .map { ManuellBrevmottakerMapper.tilRespons(it) },
            )
    }

    @Operation(summary = "Oppdaterer manuell brevmottaker")
    @PutMapping(
        path = ["/{behandlingId}/{manuellBrevmottakerId}"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun oppdaterManuellBrevmottaker(
        @PathVariable behandlingId: UUID,
        @PathVariable manuellBrevmottakerId: UUID,
        @Valid @RequestBody
        manuellBrevmottakerRequestDto: ManuellBrevmottakerRequestDto,
    ): Ressurs<String> {
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(behandlingId)
        if (tilbakekreving != null) {
            val logContext = SecureLog.Context.fra(tilbakekreving)
            val saksbehandler = ContextService.hentBehandler(logContext)
            tilgangskontrollService.validerTilgangBehandlingID(
                behandlingId = behandlingId,
                minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                auditLoggerEvent = AuditLoggerEvent.UPDATE,
                handling = "Oppdaterer manuell brevmottaker",
            )
            tilbakekrevingService.behandleBrevmottaker(saksbehandler, tilbakekreving, manuellBrevmottakerRequestDto, manuellBrevmottakerId)
            return Ressurs.success("", melding = "Manuell brevmottaker er oppdatert")
        }

        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
            auditLoggerEvent = AuditLoggerEvent.UPDATE,
            handling = "Oppdaterer manuell brevmottaker",
        )
        manuellBrevmottakerService.oppdaterBrevmottaker(behandlingId, manuellBrevmottakerId, manuellBrevmottakerRequestDto)
        return Ressurs.success("", melding = "Manuell brevmottaker er oppdatert")
    }

    @Operation(summary = "Fjerner manuell brevmottaker")
    @DeleteMapping(path = ["/{behandlingId}/{manuellBrevmottakerId}"])
    fun fjernManuellBrevmottaker(
        @PathVariable behandlingId: UUID,
        @PathVariable manuellBrevmottakerId: UUID,
    ): Ressurs<String> {
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(behandlingId)
        if (tilbakekreving != null) {
            val logContext = SecureLog.Context.fra(tilbakekreving)
            val saksbehandler = ContextService.hentBehandler(logContext)
            tilgangskontrollService.validerTilgangBehandlingID(
                behandlingId = behandlingId,
                minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                auditLoggerEvent = AuditLoggerEvent.UPDATE,
                handling = "Fjerner manuell brevmottaker",
            )
            tilbakekrevingService.fjernManuelBrevmottaker(saksbehandler, tilbakekreving, manuellBrevmottakerId)
            return Ressurs.success("", melding = "Manuell brevmottaker er oppdatert")
        }

        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
            auditLoggerEvent = AuditLoggerEvent.UPDATE,
            handling = "Fjerner manuell brevmottaker",
        )
        manuellBrevmottakerService.fjernBrevmottaker(behandlingId, manuellBrevmottakerId)
        return Ressurs.success("", melding = "Manuell brevmottaker er fjernet")
    }

    @Operation(summary = "Opprett og aktiver brevmottaker-steg på behandling")
    @PostMapping(path = ["/{behandlingId}/aktiver"])
    fun opprettBrevmottakerSteg(
        @PathVariable("behandlingId") behandlingId: UUID,
    ): Ressurs<String> {
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(behandlingId)
        if (tilbakekreving != null) {
            tilgangskontrollService.validerTilgangBehandlingID(
                behandlingId = behandlingId,
                minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                auditLoggerEvent = AuditLoggerEvent.UPDATE,
                handling = "Oppretter brevmottaker-steg på behandling",
            )
            tilbakekrevingService.aktiverBrevmottakerSteg(tilbakekreving)
            return Ressurs.success("OK")
        }

        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
            auditLoggerEvent = AuditLoggerEvent.CREATE,
            handling = "Oppretter brevmottaker-steg på behandling",
        )
        manuellBrevmottakerService.opprettBrevmottakerSteg(behandlingId)
        return Ressurs.success("OK")
    }

    @Operation(summary = "Fjern manuelle brevmottakere og deaktiver steg")
    @PutMapping(path = ["/{behandlingId}/deaktiver"])
    fun fjernBrevmottakerSteg(
        @PathVariable("behandlingId") behandlingId: UUID,
    ): Ressurs<String> {
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(behandlingId)
        if (tilbakekreving != null) {
            tilgangskontrollService.validerTilgangBehandlingID(
                behandlingId = behandlingId,
                minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                auditLoggerEvent = AuditLoggerEvent.UPDATE,
                handling = "Fjern ev. manuelt registrerte brevmottakere og deaktiver steg.",
            )
            tilbakekrevingService.fjernBrevmottakerSteg(tilbakekreving)
            return Ressurs.success("OK")
        }

        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
            auditLoggerEvent = AuditLoggerEvent.UPDATE,
            handling = "Fjern ev. manuelt registrerte brevmottakere og deaktiver steg.",
        )
        manuellBrevmottakerService.fjernManuelleBrevmottakereOgTilbakeførSteg(behandlingId)
        return Ressurs.success("OK")
    }
}
