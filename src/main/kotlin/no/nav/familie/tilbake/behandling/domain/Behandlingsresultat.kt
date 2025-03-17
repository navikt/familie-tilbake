package no.nav.familie.tilbake.behandling.domain

import no.nav.familie.tilbake.common.repository.Sporbar
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsresultatstype
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import java.time.LocalDate
import java.util.UUID

data class Behandlingsresultat(
    @Id
    val id: UUID = UUID.randomUUID(),
    val type: Behandlingsresultatstype = Behandlingsresultatstype.IKKE_FASTSATT,
    @MappedCollection(idColumn = "behandlingsresultat_id")
    val behandlingsvedtak: Behandlingsvedtak? = null,
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
) {
    companion object {
        val ALLE_HENLEGGELSESKODER: Set<Behandlingsresultatstype> =
            setOf(
                Behandlingsresultatstype.HENLAGT_KRAVGRUNNLAG_NULLSTILT,
                Behandlingsresultatstype.HENLAGT_FEILOPPRETTET,
                Behandlingsresultatstype.HENLAGT_TEKNISK_VEDLIKEHOLD,
                Behandlingsresultatstype.HENLAGT_FEILOPPRETTET_MED_BREV,
                Behandlingsresultatstype.HENLAGT_FEILOPPRETTET_UTEN_BREV,
                Behandlingsresultatstype.HENLAGT_MANGLENDE_KRAVGRUNNLAG,
            )

        val ALLE_FASTSATTKODER =
            setOf(
                Behandlingsresultatstype.INGEN_TILBAKEBETALING,
                Behandlingsresultatstype.DELVIS_TILBAKEBETALING,
                Behandlingsresultatstype.FULL_TILBAKEBETALING,
            )
    }

    fun erBehandlingHenlagt(): Boolean = ALLE_HENLEGGELSESKODER.contains(type)

    fun erBehandlingFastsatt(): Boolean = ALLE_FASTSATTKODER.contains(type)

    fun resultatstypeTilFrontend(): Behandlingsresultatstype {
        if (erBehandlingHenlagt()) {
            return Behandlingsresultatstype.HENLAGT
        }
        return type
    }
}

data class Behandlingsvedtak(
    @Id
    val id: UUID = UUID.randomUUID(),
    val vedtaksdato: LocalDate,
    val iverksettingsstatus: Iverksettingsstatus = Iverksettingsstatus.IKKE_IVERKSATT,
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)

enum class Iverksettingsstatus {
    IKKE_IVERKSATT,
    UNDER_IVERKSETTING,
    IVERKSATT,
}
