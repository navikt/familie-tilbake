package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.*

data class Behandlingsstegstilstand(@Id
                                    val id: UUID = UUID.randomUUID(),
                                    val behandlingId: UUID,
                                    val behandlingsstegstype: Behandlingsstegstype,
                                    val behandlingsstegsstatus: Behandlingstegsstatus,
                                    val versjon: Int = 0,
                                    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                    val sporbar: Sporbar = Sporbar())

/**
 * Kode for status i intern håndtering av flyt på et steg
 *
 * Kommer kun til anvendelse dersom det oppstår aksjonspunkter eller noe må legges på vent i et steg. Hvis ikke
 * flyter et rett igjennom til UTFØRT.
 */
enum class Behandlingstegsstatus {

    INNGANG,

    /**
     * midlertidig intern tilstand når steget startes (etter inngang).
     */
    STARTET,
    VENTER,
    UTGANG,
    AVBRUTT,
    UTFØRT,
    FREMOVERFØRT,
    TILBAKEFØRT,

    /**
     * Kun for intern bruk.
     */
    UDEFINERT
}
