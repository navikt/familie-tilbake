package no.nav.familie.tilbake.config

import no.nav.familie.tilbake.behandling.domain.Fagsystem
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties("familie.tilbake")
@ConstructorBinding
class RolleConfig(rolle: Map<Fagsystem, Map<Behandlerrolle, String>>) {

    val rolleMap =
            rolle.map {
                it.value.entries.map { rolleMap ->
                    rolleMap.value to (it.key to rolleMap.key)
                }
            }.flatten().toMap()
}
