package no.nav.familie.tilbake.domain.tbd

import no.nav.familie.tilbake.common.repository.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table("behandlingsarsak")
data class Behandlingsårsak(@Id
                            val id: UUID = UUID.randomUUID(),
                            val behandlingId: UUID,
                            val originalBehandlingId: UUID?,
                            val type: Behandlingsårsakstype,
                            val versjon: Int = 0,
                            @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                            val sporbar: Sporbar = Sporbar())

enum class Behandlingsårsakstype(val navn: String) {
    REVURDERING_KLAGE_NFP("Revurdering NFP omgjør vedtak basert på klage"),
    REVURDERING_KLAGE_KA("Revurdering etter KA-behandlet klage"),
    REVURDERING_OPPLYSNINGER_OM_VILKÅR("Nye opplysninger om vilkårsvurdering"),
    REVURDERING_OPPLYSNINGER_OM_FORELDELSE("Nye opplysninger om foreldelse"),
    REVURDERING_FEILUTBETALT_BELØP_HELT_ELLER_DELVIS_BORTFALT("Feilutbetalt beløp helt eller delvis bortfalt"),
    UDEFINERT("Ikke Definert")
}
