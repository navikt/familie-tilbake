package no.nav.familie.tilbake.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.tilbake.config.FeatureToggleConfig
import no.nav.familie.tilbake.config.FeatureToggleService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/featuretoggle"], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
class FeatureToggleController(
    private val featureToggleService: FeatureToggleService,
) {
    private val funksjonsbrytere =
        setOf(
            FeatureToggleConfig.KAN_SE_HISTORISKE_VURDERINGER,
        )

    @GetMapping
    fun featureToggles(): Ressurs<Map<String, Boolean>> {
        return Ressurs.success(funksjonsbrytere.associateWith { featureToggleService.isEnabled(it) })
    }
}
