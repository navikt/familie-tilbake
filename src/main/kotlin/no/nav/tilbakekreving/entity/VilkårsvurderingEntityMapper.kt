package no.nav.tilbakekreving.entity

import no.nav.tilbakekreving.entities.AktsomhetType
import no.nav.tilbakekreving.entities.AktsomhetsvurderingEntity
import no.nav.tilbakekreving.entities.BeholdType
import no.nav.tilbakekreving.entities.DatoperiodeEntity
import no.nav.tilbakekreving.entities.FeilaktigEllerMangelfullType
import no.nav.tilbakekreving.entities.GodTroEntity
import no.nav.tilbakekreving.entities.SkalReduseresEntity
import no.nav.tilbakekreving.entities.SkalReduseresType
import no.nav.tilbakekreving.entities.SærligGrunnEntity
import no.nav.tilbakekreving.entities.SærligeGrunnerEntity
import no.nav.tilbakekreving.entities.VilkårsvurderingsperiodeEntity
import no.nav.tilbakekreving.entities.VilkårsvurderingstegEntity
import no.nav.tilbakekreving.entities.VurderingType
import no.nav.tilbakekreving.entities.VurdertAktsomhetEntity
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunnType
import java.sql.ResultSet
import java.util.UUID

object VilkårsvurderingEntityMapper : Entity<VilkårsvurderingstegEntity, UUID, UUID>(
    tableName = "tilbakekreving_vilkårsvurdering",
    idGetter = VilkårsvurderingstegEntity::id,
    idConverter = FieldConverter.UUIDConverter.required(),
) {
    val behandlingRef = field(
        "behandling_ref",
        { it.behandlingRef!! },
        FieldConverter.UUIDConverter.required(),
    )

    fun map(resultSet: ResultSet, vurdertePerioder: List<VilkårsvurderingsperiodeEntity>): VilkårsvurderingstegEntity {
        return VilkårsvurderingstegEntity(
            id = resultSet[id],
            behandlingRef = resultSet[behandlingRef],
            vurderinger = vurdertePerioder,
        )
    }

    object VilkårsvurdertPeriodeEntityMapper : Entity<VilkårsvurderingsperiodeEntity, UUID, UUID>(
        tableName = "tilbakekreving_vilkårsvurdering_periode",
        idGetter = VilkårsvurderingsperiodeEntity::id,
        idConverter = FieldConverter.UUIDConverter.required(),
    ) {
        val vurderingRef = field(
            "vurdering_ref",
            { it.vurderingRef!! },
            FieldConverter.UUIDConverter.required(),
        )
        val periodeFom = field(
            "periode_fom",
            { it.periode.fom },
            FieldConverter.LocalDateConverter.required(),
        )
        val periodeTom = field(
            "periode_tom",
            { it.periode.tom },
            FieldConverter.LocalDateConverter.required(),
        )
        val vurderingType = field(
            "vurdering_type",
            { it.vurdering.vurderingType },
            FieldConverter.EnumConverter.of<VurderingType>().required(),
        )
        val vilkårForTilbakekreving = field(
            "vilkår_for_tilbakekreving",
            { it.vurdering.begrunnelse },
            FieldConverter.StringConverter,
        )

        // TODO: Flytte denne til AktsomhetMapper(og tabell) når vi er ferdig med å migrere fra JSON
        val feilaktigEllerMangelfull = field(
            "feilaktig_eller_mangelfull",
            { it.vurdering.feilaktigEllerMangelfull },
            FieldConverter.EnumConverter.of<FeilaktigEllerMangelfullType>(),
        )

        fun map(
            resultSet: ResultSet,
            godTro: GodTroEntity?,
            aktsomhet: VurdertAktsomhetEntity?,
        ): VilkårsvurderingsperiodeEntity {
            return VilkårsvurderingsperiodeEntity(
                id = resultSet[id],
                vurderingRef = resultSet[vurderingRef],
                periode = DatoperiodeEntity(resultSet[periodeFom], resultSet[periodeTom]),
                begrunnelseForTilbakekreving = resultSet[vilkårForTilbakekreving],
                vurdering = AktsomhetsvurderingEntity(
                    vurderingType = resultSet[vurderingType],
                    begrunnelse = resultSet[vilkårForTilbakekreving],
                    beløpIBehold = godTro,
                    aktsomhet = aktsomhet,
                    feilaktigEllerMangelfull = resultSet[feilaktigEllerMangelfull],
                ),
            )
        }
    }

    object GodTroEntityMapper : Entity<GodTroEntity, UUID, UUID>(
        "tilbakekreving_vilkårsvurdering_periode_god_tro",
        { it.periodeRef!! },
        FieldConverter.UUIDConverter.required(),
    ) {
        val begrunnelse = field(
            "begrunnelse",
            GodTroEntity::begrunnelse,
            FieldConverter.StringConverter.required(),
        )

        val beløpIBehold = field(
            "beløp_i_behold",
            GodTroEntity::beholdType,
            FieldConverter.EnumConverter.of<BeholdType>().required(),
        )

        val beløp = field(
            "beløp",
            GodTroEntity::beløp,
            FieldConverter.BigDecimalConverter,
        )

        fun map(resultSet: ResultSet): GodTroEntity {
            return GodTroEntity(
                periodeRef = resultSet[id],
                begrunnelse = resultSet[begrunnelse],
                beholdType = resultSet[beløpIBehold],
                beløp = resultSet[beløp],
            )
        }
    }

    object AktsomhetMapper : Entity<VurdertAktsomhetEntity, UUID, UUID>(
        "tilbakekreving_vilkårsvurdering_periode_aktsomhet",
        { it.periodeRef!! },
        FieldConverter.UUIDConverter.required(),
    ) {
        val aktsomhetType = field(
            "type",
            VurdertAktsomhetEntity::aktsomhetType,
            FieldConverter.EnumConverter.of<AktsomhetType>().required(),
        )

        val begrunnelse = field(
            "begrunnelse",
            VurdertAktsomhetEntity::begrunnelse,
            FieldConverter.StringConverter.required(),
        )

        val unnlates = field(
            "unnlates",
            VurdertAktsomhetEntity::kanUnnlates,
            FieldConverter.EnumConverter.of(),
        )

        fun map(
            resultSet: ResultSet,
            særligeGrunner: SærligeGrunnerEntity?,
        ): VurdertAktsomhetEntity {
            return VurdertAktsomhetEntity(
                periodeRef = resultSet[id],
                aktsomhetType = resultSet[aktsomhetType],
                begrunnelse = resultSet[begrunnelse],
                skalIleggesRenter = null,
                særligGrunner = særligeGrunner,
                kanUnnlates = resultSet[unnlates],
            )
        }
    }

    object SærligeGrunnerMapper : Entity<SærligeGrunnerEntity, UUID, UUID>(
        "tilbakekreving_vilkårsvurdering_periode_særlige_grunner",
        { it.periodeRef!! },
        FieldConverter.UUIDConverter.required(),
    ) {
        val begrunnelse = field(
            "begrunnelse",
            SærligeGrunnerEntity::begrunnelse,
            FieldConverter.StringConverter.required(),
        )
        val særligGrunnAnnetBegrunnelse = field(
            "annet_særlig_grunn_begrunnelse",
            { it.grunner.firstOrNull { grunn -> grunn.type == SærligGrunnType.ANNET }?.annetBegrunnelse },
            FieldConverter.StringConverter,
        )
        val skalReduseres = field(
            "skal_reduseres",
            { it.skalReduseres.type },
            FieldConverter.EnumConverter.of<SkalReduseresType>().required(),
        )
        val reduksjonProsent = field(
            "reduksjon_prosent",
            { it.skalReduseres.prosentdel },
            FieldConverter.IntConverter,
        )
        val særligeGrunner = field(
            "særlige_grunner",
            { it.grunner.map { grunn -> grunn.type } },
            FieldConverter.EnumArrayConverter.of<SærligGrunnType>(),
        )

        fun map(
            resultSet: ResultSet,
        ): SærligeGrunnerEntity {
            return SærligeGrunnerEntity(
                periodeRef = resultSet[id],
                begrunnelse = resultSet[begrunnelse],
                grunner = resultSet[særligeGrunner].map {
                    when (it) {
                        SærligGrunnType.ANNET -> SærligGrunnEntity(it, resultSet[særligGrunnAnnetBegrunnelse!!])
                        else -> SærligGrunnEntity(it, null)
                    }
                },
                skalReduseres = SkalReduseresEntity(
                    type = resultSet[skalReduseres],
                    prosentdel = resultSet[reduksjonProsent],
                ),
            )
        }
    }
}
