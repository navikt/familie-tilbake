package no.nav.familie.tilbake.api

import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.historikkinnslag.tilDto
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.TilgangskontrollService
import no.nav.familie.tilbake.sikkerhet.ValideringContext
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.api.v1.dto.HistorikkinnslagDto
import no.nav.tilbakekreving.behandlingslogg.EkstraInfo
import no.nav.tilbakekreving.kontrakter.historikk.Historikkinnslagstype
import no.nav.tilbakekreving.repository.TilbakekrevingFilter
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/behandlinger")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class HistorikkController(
    private val historikkService: HistorikkService,
    private val tilgangskontrollService: TilgangskontrollService,
    private val tilbakekrevingService: TilbakekrevingService,
) {
    @GetMapping(
        "/{behandlingId}/historikk",
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentHistorikkinnslag(
        @PathVariable("behandlingId") behandlingId: UUID,
    ): Ressurs<List<HistorikkinnslagDto?>> {
        val tilbakekreving = tilbakekrevingService.lesTilbakekreving(TilbakekrevingFilter.behandling(behandlingId), ValideringContext.HentHistorikk)
        if (tilbakekreving != null) {
            // Mapper til gammel Dto inntil frontend er klar til å bruke ny endepunkt.
            return Ressurs.success(
                tilbakekrevingService
                    .hentHistorikk(tilbakekreving.id)
                    .map { innslag ->
                        val ekstraInfo = innslag.ekstraInfo as Map<*, *>
                        HistorikkinnslagDto(
                            behandlingId = innslag.behandlingId,
                            type = Historikkinnslagstype.valueOf(innslag.type),
                            aktør = HistorikkinnslagDto.AktørDto.valueOf(innslag.aktør),
                            aktørIdent = innslag.aktørIdent,
                            tittel = innslag.tittel,
                            tekst = innslag.tekst,
                            steg = innslag.steg,
                            journalpostId = ekstraInfo[EkstraInfo.JOURNALPOST_ID] as String?,
                            dokumentId = ekstraInfo[EkstraInfo.DOKUMENTINFO_ID] as String?,
                            opprettetTid = innslag.opprettetTid.toLocalDateTime(),
                            nyFrist = (ekstraInfo[EkstraInfo.NY_FRIST_FOR_UTTALELSE] as? String)
                                ?.let { LocalDate.parse(it) },
                            begrunnelseForUtsattFrist = ekstraInfo[EkstraInfo.BEGRUNNELSE_FOR_UTSATT_FRIST] as String?,
                        )
                    },
            )
        }
        tilgangskontrollService.validerTilgangBehandlingID(
            behandlingId = behandlingId,
            minimumBehandlerrolle = Behandlerrolle.VEILEDER,
            auditLoggerEvent = AuditLoggerEvent.ACCESS,
            handling = "Henter historikkinnslag",
        )
        val historikkInnslagDtoSortertEtterOpprettetTidspunkt =
            historikkService
                .hentHistorikkinnslag(behandlingId)
                .map { it.tilDto() }
                .sortedBy { it.opprettetTid }

        return Ressurs.success(historikkInnslagDtoSortertEtterOpprettetTidspunkt)
    }
}
