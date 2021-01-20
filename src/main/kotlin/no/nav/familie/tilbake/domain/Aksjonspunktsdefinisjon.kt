package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.*

data class Aksjonspunktsdefinisjon(@Id
                                   val id: UUID = UUID.randomUUID(),
                                   val vurderingspunktsdefinisjonId: UUID,
                                   val kode: String,
                                   val navn: String,
                                   val beskrivelse: String?,
                                   val totrinnsbehandlingDefault: Boolean,
                                   val fristperiode: String?,
                                   val skjermlenketype: Skjermlenketype = Skjermlenketype.UDEFINERT,
                                   val aksjonspunktstype: Aksjonspunktstype = Aksjonspunktstype.UDEFINERT,
                                   val tilbakehoppVedGjenopptakelse: Boolean = false,
                                   val lagUtenHistorikk: Boolean = false,
                                   @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                   val sporbar: Sporbar = Sporbar())

enum class Aksjonspunktstype(val navn: String) {
    MANUELL("Manuell"),
    AUTOPUNKT("Autopunkt"),
    OVERSTYRING("Overstyring"),
    SAKSBEHANDLEROVERSTYRING("Saksbehandleroverstyring"),
    UDEFINERT("Ikke Definert");
}

enum class Skjermlenketype(val navn: String) {
    FAKTA_OM_FEILUTBETALING("Fakta om feilutbetaling"),
    TILBAKEKREVING("Tilbakekreving"),
    FORELDELSE("Foreldelse"),
    VEDTAK("Vedtak"),
    FAKTA_OM_VERGE("Fakta om verge/fullmektig"),
    UDEFINERT("Ikke Definert")
}
