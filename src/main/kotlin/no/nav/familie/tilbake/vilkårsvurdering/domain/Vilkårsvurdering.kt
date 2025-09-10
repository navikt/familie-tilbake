package no.nav.familie.tilbake.vilkårsvurdering.domain

import no.nav.familie.tilbake.common.repository.Sporbar
import no.nav.tilbakekreving.kontrakter.periode.Månedsperiode
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunnType
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.util.UUID

@Table("vilkarsvurdering")
data class Vilkårsvurdering(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val aktiv: Boolean = true,
    @MappedCollection(idColumn = "vilkarsvurdering_id")
    val perioder: Set<Vilkårsvurderingsperiode> = setOf(),
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)

@Table("vilkarsvurderingsperiode")
data class Vilkårsvurderingsperiode(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val periode: Månedsperiode,
    @Column("vilkarsvurderingsresultat")
    val vilkårsvurderingsresultat: Vilkårsvurderingsresultat,
    val begrunnelse: String,
    @MappedCollection(idColumn = "vilkarsvurderingsperiode_id")
    val aktsomhet: VilkårsvurderingAktsomhet? = null,
    @MappedCollection(idColumn = "vilkarsvurderingsperiode_id")
    val godTro: VilkårsvurderingGodTro? = null,
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)

@Table("vilkarsvurdering_god_tro")
data class VilkårsvurderingGodTro(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column("belop_er_i_behold")
    val beløpErIBehold: Boolean,
    @Column("belop_tilbakekreves")
    val beløpTilbakekreves: BigDecimal? = null,
    val begrunnelse: String,
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
) {
    val beløpSomErIBehold get() = if (this.beløpErIBehold) beløpTilbakekreves else BigDecimal.ZERO

    fun erLik(vilkårsvurderingGodTro: VilkårsvurderingGodTro?) = beløpErIBehold == vilkårsvurderingGodTro?.beløpErIBehold && begrunnelse == vilkårsvurderingGodTro.begrunnelse
}

@Table("vilkarsvurdering_aktsomhet")
data class VilkårsvurderingAktsomhet(
    @Id
    val id: UUID = UUID.randomUUID(),
    val aktsomhet: Aktsomhet,
    val ileggRenter: Boolean? = null,
    val andelTilbakekreves: BigDecimal? = null,
    @Column("manuelt_satt_belop")
    val manueltSattBeløp: BigDecimal? = null,
    val begrunnelse: String,
    @Column("serlige_grunner_til_reduksjon")
    val særligeGrunnerTilReduksjon: Boolean = false,
    @Column("tilbakekrev_smabelop")
    val tilbakekrevSmåbeløp: Boolean = true,
    @MappedCollection(idColumn = "vilkarsvurdering_aktsomhet_id")
    val vilkårsvurderingSærligeGrunner: Set<VilkårsvurderingSærligGrunn> = setOf(),
    @Column("serlige_grunner_begrunnelse")
    val særligeGrunnerBegrunnelse: String? = null,
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
) {
    init {
        require(!(andelTilbakekreves != null && manueltSattBeløp != null)) { "Kan ikke sette både prosenterSomTilbakekreves og beløpSomTilbakekreves" }
        if (aktsomhet == Aktsomhet.FORSETT) {
            check(!særligeGrunnerTilReduksjon) { "Ved FORSETT skal ikke særligeGrunnerTilReduksjon settes her" }
            check(manueltSattBeløp == null) { "Ved FORSETT er beløp automatisk, og skal ikke settes her" }
            check(andelTilbakekreves == null) { "Ved FORSETT er andel automatisk, og skal ikke settes her" }
            check(tilbakekrevSmåbeløp) { "Dette er gyldig bare for Simpel uaktsom" }
        }
        if (aktsomhet == Aktsomhet.GROV_UAKTSOMHET) {
            check(tilbakekrevSmåbeløp) { "Dette er gyldig bare for Simpel uaktsom" }
        }
    }

    val skalHaSærligeGrunner
        get() = Aktsomhet.GROV_UAKTSOMHET == aktsomhet || Aktsomhet.SIMPEL_UAKTSOMHET == aktsomhet && this.tilbakekrevSmåbeløp

    val særligeGrunner get() = vilkårsvurderingSærligeGrunner.map(VilkårsvurderingSærligGrunn::særligGrunn)

    fun erLik(vilkårsvurderingAktsomhet: VilkårsvurderingAktsomhet?) =
        aktsomhet == vilkårsvurderingAktsomhet?.aktsomhet &&
            ileggRenter == vilkårsvurderingAktsomhet.ileggRenter &&
            manueltSattBeløp?.toInt() == vilkårsvurderingAktsomhet.manueltSattBeløp?.toInt() &&
            begrunnelse == vilkårsvurderingAktsomhet.begrunnelse &&
            særligeGrunnerTilReduksjon == vilkårsvurderingAktsomhet.særligeGrunnerTilReduksjon &&
            tilbakekrevSmåbeløp == vilkårsvurderingAktsomhet.tilbakekrevSmåbeløp &&
            andelTilbakekreves == vilkårsvurderingAktsomhet.andelTilbakekreves &&
            særligeGrunnerTilReduksjonErLik(vilkårsvurderingSærligeGrunner, vilkårsvurderingAktsomhet.vilkårsvurderingSærligeGrunner)

    fun særligeGrunnerTilReduksjonErLik(
        gjeldeneVilkårsvurderingSærligeGrunner: Set<VilkårsvurderingSærligGrunn>,
        vilkårsvurderingSærligeGrunner: Set<VilkårsvurderingSærligGrunn>,
    ): Boolean =
        gjeldeneVilkårsvurderingSærligeGrunner.map { it.særligGrunn } == vilkårsvurderingSærligeGrunner.map { it.særligGrunn } &&
            gjeldeneVilkårsvurderingSærligeGrunner.map { it.begrunnelse } == vilkårsvurderingSærligeGrunner.map { it.begrunnelse }
}

@Table("vilkarsvurdering_serlig_grunn")
data class VilkårsvurderingSærligGrunn(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column("serlig_grunn")
    val særligGrunn: SærligGrunnType,
    val begrunnelse: String?,
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)
