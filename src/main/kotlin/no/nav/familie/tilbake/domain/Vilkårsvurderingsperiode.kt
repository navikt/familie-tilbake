package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.util.*

@Table("vilkarsvurderingsperiode")
data class Vilkårsvurderingsperiode(@Id
                                    val id: UUID = UUID.randomUUID(),
                                    @Column("vilkarsvurdering_id")
                                    val vilkårsvurderingId: UUID,
                                    val fom: LocalDate,
                                    val tom: LocalDate,
                                    val navoppfulgt: Navoppfulgt,
                                    @Column("vilkarsvurderingsresultat")
                                    val vilkårsvurderingsresultat: Vilkårsvurderingsresultat,
                                    val begrunnelse: String,
                                    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                    val sporbar: Sporbar = Sporbar())

enum class Navoppfulgt {
    NAV_KAN_IKKE_LASTES,
    HAR_IKKE_FULGT_OPP,
    HAR_BENYTTET_FEIL,
    HAR_IKKE_SJEKKET,
    BEREGNINGSFEIL,
    HAR_UTFØRT_FEIL,
    HAR_SENDT_TIL_FEIL_MOTTAKER,
    UDEFINERT
}

enum class Vilkårsvurderingsresultat(val navn: String) {
    FORSTO_BURDE_FORSTÅTT("Ja, mottaker forsto eller burde forstått at utbetalingen skyldtes en feil (1. ledd, 1. punkt)"),
    MANGELFULLE_OPPLYSNINGER_FRA_BRUKER("Ja, mottaker har forårsaket feilutbetalingen ved forsett eller uaktsomt gitt mangelfulle opplysninger (1. ledd, 2 punkt)"),
    FEIL_OPPLYSNINGER_FRA_BRUKER("Ja, mottaker har forårsaket feilutbetalingen ved forsett eller uaktsomt gitt feilaktige opplysninger (1. ledd, 2 punkt)"),
    GOD_TRO("Nei, mottaker har mottatt beløpet i god tro (1. ledd)"),
    UDEFINERT("Ikke Definert")
}
