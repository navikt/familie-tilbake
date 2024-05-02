package no.nav.familie.tilbake.api.forvaltning

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.tilbake.forvaltning.ForvaltningPreprodService
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.HenteParam
import no.nav.familie.tilbake.sikkerhet.Rolletilgangssjekk
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// NB! Kun preprod Denne kontrollen inneholder tjenester som kun brukes av forvaltningsteam via swagger. Frontend bør ikke kalle disse tjenestene.

@RestController
@RequestMapping("/api/forvaltning")
@ProtectedWithClaims(issuer = "azuread")
@Validated
@Profile("!prod")
class ForvaltningPreprodController(
    private val environment: Environment,
    private val forvaltningPreprodService: ForvaltningPreprodService,
) {
    @Operation(summary = "Legg inn test-kravgrunnlag - KUN PREPROD/DEV!")
    @PostMapping(
        path = ["/behandling/{behandlingId}/kravgrunnlag/testkravgrunnlag"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @Rolletilgangssjekk(
        Behandlerrolle.FORVALTER,
        "Legg inn testkravgrunnlag - preprod",
        AuditLoggerEvent.NONE,
        HenteParam.BEHANDLING_ID,
    )
    fun simulerMottakAvKravgrunnlag(
        @RequestBody kravgrunnlag: String,
    ): Ressurs<String> {
        if (environment.activeProfiles.contains("prod")){
                throw IllegalStateException("Kan ikke kjøre denne tjenesten i prod")
            }
        forvaltningPreprodService.leggInnTestKravgrunnlag(kravgrunnlag)
        return Ressurs.success("OK")
    }
}
