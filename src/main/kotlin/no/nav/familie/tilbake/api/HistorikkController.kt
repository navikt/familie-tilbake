package no.nav.familie.tilbake.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.tilbake.api.dto.HistorikkinnslagDto
import no.nav.familie.tilbake.api.dto.tilDto
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/behandlinger")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class HistorikkController(private val historikkService: HistorikkService) {
    @GetMapping(
        "/{behandlingId}/historikk",
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun hentHistorikkinnslag(
        @PathVariable("behandlingId") behandlingId: UUID,
    ): Ressurs<List<HistorikkinnslagDto?>> {
        val historikkInnslagDtoSortertEtterOpprettetTidspunkt =
            historikkService.hentHistorikkinnslag(behandlingId)
                .map { it.tilDto() }
                .sortedBy { it.opprettetTid }

        return Ressurs.success(historikkInnslagDtoSortertEtterOpprettetTidspunkt)
    }
}
