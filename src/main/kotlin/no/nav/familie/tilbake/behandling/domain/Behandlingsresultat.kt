package no.nav.familie.tilbake.behandling.domain

import no.nav.familie.tilbake.common.repository.Sporbar
import no.nav.familie.tilbake.domain.Behandlingsvedtak
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import java.util.*

data class Behandlingsresultat(@Id
                               val id: UUID = UUID.randomUUID(),
                               val type: Behandlingsresultatstype = Behandlingsresultatstype.IKKE_FASTSATT,
                               @MappedCollection(idColumn = "behandlingsresultat_id")
                               val behandlingsvedtak: Set<Behandlingsvedtak> = setOf(),
                               @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                               val sporbar: Sporbar = Sporbar()) {

    companion object {

        val ALLE_HENLEGGELSESKODER: Set<Behandlingsresultatstype> = setOf(Behandlingsresultatstype.HENLAGT_KRAVGRUNNLAG_NULLSTILT,
                                                                          Behandlingsresultatstype.HENLAGT_FEILOPPRETTET,
                                                                          Behandlingsresultatstype.HENLAGT_TEKNISK_VEDLIKEHOLD,
                                                                          Behandlingsresultatstype.HENLAGT_FEILOPPRETTET_MED_BREV,
                                                                          Behandlingsresultatstype.HENLAGT_FEILOPPRETTET_UTEN_BREV)
    }


    fun erBehandlingHenlagt(): Boolean {
        return ALLE_HENLEGGELSESKODER.contains(type)
    }
}

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
