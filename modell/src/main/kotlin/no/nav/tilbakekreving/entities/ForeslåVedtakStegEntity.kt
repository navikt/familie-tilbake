package no.nav.tilbakekreving.entities

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.tilbakekreving.behandling.saksbehandling.ForeslåVedtakSteg
import java.util.UUID

@JsonIgnoreProperties(ignoreUnknown = true)
data class ForeslåVedtakStegEntity(
    val id: UUID = UUID.randomUUID(),
    val behandlingRef: UUID? = null,
    val vurdert: Boolean = false,
) {
    fun fraEntity(): ForeslåVedtakSteg {
        return ForeslåVedtakSteg(
            id = id,
            vurdert = vurdert,
        )
    }
}
