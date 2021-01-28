package no.nav.familie.tilbake.domain.tbd

import no.nav.familie.tilbake.common.repository.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.util.*

@Table("vilkarsvurdering")
data class Vilkårsvurdering(@Id
                            val id: UUID = UUID.randomUUID(),
                            val behandlingId: UUID,
                            val aktiv: Boolean = true,
                            @MappedCollection(idColumn = "vilkarsvurdering_id")
                            val perioder: Set<Vilkårsvurderingsperiode> = setOf(),
                            @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                            val sporbar: Sporbar = Sporbar())

@Table("vilkarsvurderingsperiode")
data class Vilkårsvurderingsperiode(@Id
                                    val id: UUID = UUID.randomUUID(),
                                    val fom: LocalDate,
                                    val tom: LocalDate,
                                    val navoppfulgt: Navoppfulgt,
                                    @Column("vilkarsvurderingsresultat")
                                    val vilkårsvurderingsresultat: Vilkårsvurderingsresultat,
                                    val begrunnelse: String,
                                    @MappedCollection(idColumn = "vilkarsvurderingsperiode_id")
                                    val vilkårsvurderingAktsomheter: Set<VilkårsvurderingAktsomhet> = setOf(),
                                    @MappedCollection(idColumn = "vilkarsvurderingsperiode_id")
                                    val vilkårsvurderingGodTro: Set<VilkårsvurderingGodTro> = setOf(),
                                    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                    val sporbar: Sporbar = Sporbar())

@Table("vilkarsvurdering_god_tro")
data class VilkårsvurderingGodTro(@Id
                                  val id: UUID = UUID.randomUUID(),
                                  @Column("belop_er_i_behold")
                                  val beløpErIBehold: Boolean,
                                  @Column("belop_tilbakekreves")
                                  val beløpTilbakekreves: Long?,
                                  val begrunnelse: String,
                                  @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                  val sporbar: Sporbar = Sporbar())


@Table("vilkarsvurdering_aktsomhet")
data class VilkårsvurderingAktsomhet(@Id
                                     val id: UUID = UUID.randomUUID(),
                                     val aktsomhet: Aktsomhet,
                                     val ileggRenter: Boolean?,
                                     val andelTilbakekreves: Double?,
                                     @Column("manuelt_satt_belop")
                                     val manueltSattBeløp: Long?,
                                     val begrunnelse: String,
                                     @Column("serlige_grunner_til_reduksjon")
                                     val særligeGrunnerTilReduksjon: Boolean?,
                                     @Column("tilbakekrev_smabelop")
                                     val tilbakekrevSmabeløp: Boolean?,
                                     @MappedCollection(idColumn = "vilkarsvurdering_aktsomhet_id")
                                     val vilkårsvurderingSærligeGrunner: Set<VilkårsvurderingSærligGrunn> = setOf(),
                                     @Column("serlige_grunner_begrunnelse")
                                     val særligeGrunnerBegrunnelse: String?,
                                     @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                     val sporbar: Sporbar = Sporbar())

@Table("vilkarsvurdering_serlig_grunn")
data class VilkårsvurderingSærligGrunn(@Id
                                       val id: UUID = UUID.randomUUID(),
                                       @Column("serlig_grunn")
                                       val særligGrunn: SærligGrunn,
                                       val begrunnelse: String?,
                                       @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                       val sporbar: Sporbar = Sporbar())

enum class SærligGrunn(val navn: String) {
    GRAD_AV_UAKTSOMHET("Graden av uaktsomhet hos den kravet retter seg mot"),
    HELT_ELLER_DELVIS_NAVS_FEIL("Om feilen helt eller delvis kan tilskrives NAV"),
    STØRRELSE_BELØP("Størrelsen på feilutbetalt beløp"),
    TID_FRA_UTBETALING("Hvor lang tid siden utbetalingen fant sted"),
    ANNET("Annet");
}

enum class Aktsomhet(val navn: String) {
    FORSETT("Forsett"),
    GROV_UAKTSOMHET("Grov uaktsomhet"),
    SIMPEL_UAKTSOMHET("Simpel uaktsomhet");
}

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
    MANGELFULLE_OPPLYSNINGER_FRA_BRUKER("Ja, mottaker har forårsaket feilutbetalingen ved forsett " +
                                        "eller uaktsomt gitt mangelfulle opplysninger (1. ledd, 2 punkt)"),
    FEIL_OPPLYSNINGER_FRA_BRUKER("Ja, mottaker har forårsaket feilutbetalingen ved forsett eller " +
                                 "uaktsomt gitt feilaktige opplysninger (1. ledd, 2 punkt)"),
    GOD_TRO("Nei, mottaker har mottatt beløpet i god tro (1. ledd)"),
    UDEFINERT("Ikke Definert")
}

