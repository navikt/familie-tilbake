package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.*

data class Behandlingsresultat(@Id
                               val id: UUID = UUID.randomUUID(),
                               val behandlingId: UUID,
                               val versjon: Int = 0,
                               val type: Behandlingsresultatstype = Behandlingsresultatstype.IKKE_FASTSATT,
                               @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                               val sporbar: Sporbar = Sporbar())

enum class Behandlingsresultatstype(val navn: String) {
    IKKE_FASTSATT("Ikke fastsatt"),
    FASTSATT("Resultatet er fastsatt"),  //Ikke bruk denne BehandlingResultatType.Blir fjernes senere
    HENLAGT_FEILOPPRETTET("Henlagt, søknaden er feilopprettet"),
    HENLAGT_FEILOPPRETTET_MED_BREV("Feilaktig opprettet - med henleggelsesbrev"),
    HENLAGT_FEILOPPRETTET_UTEN_BREV("Feilaktig opprettet - uten henleggelsesbrev"),
    HENLAGT_KRAVGRUNNLAG_NULLSTILT("Kravgrunnlaget er nullstilt"),
    HENLAGT_TEKNISK_VEDLIKEHOLD("Teknisk vedlikehold"),
    HENLAGT("Henlagt"),  // kun brukes i frontend
    INGEN_TILBAKEBETALING("Ingen tilbakebetaling"),
    DELVIS_TILBAKEBETALING("Delvis tilbakebetaling"),
    FULL_TILBAKEBETALING("Tilbakebetaling");
}
