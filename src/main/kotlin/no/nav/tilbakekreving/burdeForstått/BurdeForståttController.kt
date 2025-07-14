package no.nav.tilbakekreving.burdeForstått

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/burde-forstaatt")
@ProtectedWithClaims(issuer = "azuread")
class BurdeForståttController(
    private val burdeForståttService: BurdeForståttService,
) {
    @Operation(summary = "Legge til kravgrunnlag fra Burde-forstaatt i KravgrunnlagBufferRepository.")
    @PostMapping(
        path = ["/v1"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun leggTilKravgrunnlag(
        @RequestBody
        kravgrunnlagDto: DetaljertKravgrunnlagDto,
    ): Ressurs<String> {
        val behandlingId = burdeForståttService.leggTilKravgrunnlag(kravgrunnlagDto)
        return Ressurs.success(behandlingId.toString(), melding = "Kravgrunnlag ble lagret")
    }
}
