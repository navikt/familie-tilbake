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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

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
    @Operation(
        description = "Legg inn behandlingId (intern id) fra tilbakekrevings-behandlingen (@id) (ikke eksternid fra url)\n" +
                "lim inn et kravgrunnlag du vil bruke:\n\n" +
                "Bytt ut MINIMUM:\n\n" +
                "<urn:fagsystemId> testverdi</urn:fagsystemId> (ekstern fagsak id - long )\n\n" +
                "<urn:referanse>1</urn:referanse> (og ekstern behandlingsid long)\n\n" +
                "Dette er eksternider fra vedtaksløsning!\n\n" +
                "Endre personident i kravgrunnlag også\n\n" +
                "Det vil ikke fungere å iverksette mot økonomi - men du kan bruke forvaltningsendepunkt: \"settIverksettingUtført()\" - da lages brev og behandlingen settes til AVSLUTTET. Data her kan finnes på task i prosessering (klar til plukk, eller feilet) . SendØkonomiVedtak",
        summary =
            "Legg inn test-kravgrunnlag - KUN PREPROD/DEV! ",
    )
    @PostMapping(
        path = ["/behandling/{behandlingId}/kravgrunnlag/testkravgrunnlag"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.TEXT_XML_VALUE],
    )
    @Rolletilgangssjekk(
        Behandlerrolle.FORVALTER,
        "Legg inn testkravgrunnlag - preprod",
        AuditLoggerEvent.NONE,
        HenteParam.BEHANDLING_ID,
    )
    fun simulerMottakAvKravgrunnlag(
        @PathVariable behandlingId: UUID,
        @RequestBody kravgrunnlag: String,
    ): Ressurs<String> {
        if (environment.activeProfiles.contains("prod")) {
            throw IllegalStateException("Kan ikke kjøre denne tjenesten i prod")
        }
        forvaltningPreprodService.validerKravgrunnlagOgBehandling(behandlingId, kravgrunnlag)
        forvaltningPreprodService.leggInnTestKravgrunnlag(kravgrunnlag)
        return Ressurs.success("OK")
    }
}
