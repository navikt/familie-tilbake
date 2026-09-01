package no.nav.tilbakekreving.entity

import no.nav.tilbakekreving.behandling.saksbehandling.ÅrsakTilTilbakeføring
import no.nav.tilbakekreving.entities.AktsomhetType
import no.nav.tilbakekreving.entities.AktsomhetsvurderingEntity
import no.nav.tilbakekreving.entities.BeholdType
import no.nav.tilbakekreving.entities.DatoperiodeEntity
import no.nav.tilbakekreving.entities.FeilaktigEllerMangelfullType
import no.nav.tilbakekreving.entities.ForskjellEntity
import no.nav.tilbakekreving.entities.Forståelsesgrad
import no.nav.tilbakekreving.entities.GodTroEntity
import no.nav.tilbakekreving.entities.KanUnnlatesEntity
import no.nav.tilbakekreving.entities.MottakersForståelseEntity
import no.nav.tilbakekreving.entities.ReduksjonMomenterEntity
import no.nav.tilbakekreving.entities.SkalReduseresEntity
import no.nav.tilbakekreving.entities.SkalReduseresType
import no.nav.tilbakekreving.entities.VilkårsvurderingsperiodeEntity
import no.nav.tilbakekreving.entities.VilkårsvurderingstegEntity
import no.nav.tilbakekreving.entities.VurderingType
import no.nav.tilbakekreving.entities.VurdertAktsomhetEntity
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

    val tilbakeført = field(
        "tilbakeført",
        VilkårsvurderingstegEntity::tilbakeført,
        FieldConverter.EnumConverter.of<ÅrsakTilTilbakeføring>(),
    )

    fun map(resultSet: ResultSet, vurdertePerioder: List<VilkårsvurderingsperiodeEntity>): VilkårsvurderingstegEntity {
        return VilkårsvurderingstegEntity(
            id = resultSet[id],
            behandlingRef = resultSet[behandlingRef],
            vurderinger = vurdertePerioder,
            tilbakeført = resultSet[tilbakeført],
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

        val forrigePeriodeId = field(
            "forrige_periode_id",
            { it.vurdering.forrigePeriodeId },
            FieldConverter.UUIDConverter,
        )

        val unnlates = field(
            "unnlates",
            { it.vurdering.kanUnnlates },
            FieldConverter.EnumConverter.of<KanUnnlatesEntity>(),
        )

        val begrunnelseForUnnlatelse = field(
            "begrunnelse_for_unnlatelse",
            { it.vurdering.begrunnelseForUnnlatelse },
            FieldConverter.StringConverter,
        )

        fun map(
            resultSet: ResultSet,
            godTro: GodTroEntity?,
            aktsomhet: VurdertAktsomhetEntity?,
            reduksjonMomenter: ReduksjonMomenterEntity?,
            mottakersForståelse: MottakersForståelseEntity?,
            endringIKravgrunnnlag: ForskjellEntity?,
        ): VilkårsvurderingsperiodeEntity {
            return VilkårsvurderingsperiodeEntity(
                id = resultSet[id],
                vurderingRef = resultSet[vurderingRef],
                periode = DatoperiodeEntity(resultSet[periodeFom], resultSet[periodeTom]),
                begrunnelseForTilbakekreving = resultSet[vilkårForTilbakekreving],
                vurdering = AktsomhetsvurderingEntity(
                    vurderingType = resultSet[vurderingType],
                    mottakersForståelse = mottakersForståelse,
                    begrunnelse = resultSet[vilkårForTilbakekreving],
                    beløpIBehold = godTro,
                    aktsomhet = aktsomhet,
                    kanUnnlates = resultSet[unnlates],
                    reduksjonMomenterEntity = reduksjonMomenter,
                    feilaktigEllerMangelfull = resultSet[feilaktigEllerMangelfull],
                    forrigePeriodeId = resultSet[forrigePeriodeId],
                    begrunnelseForUnnlatelse = resultSet[begrunnelseForUnnlatelse],
                ),
                endringIKravgrunnlag = endringIKravgrunnnlag,
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
            GodTroEntity::beløpIBehold,
            FieldConverter.BigDecimalConverter,
        )

        fun map(resultSet: ResultSet): GodTroEntity {
            return GodTroEntity(
                periodeRef = resultSet[id],
                begrunnelse = resultSet[begrunnelse],
                beholdType = resultSet[beløpIBehold],
                beløpIBehold = resultSet[beløp],
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

        fun map(
            resultSet: ResultSet,
        ): VurdertAktsomhetEntity {
            return VurdertAktsomhetEntity(
                periodeRef = resultSet[id],
                aktsomhetType = resultSet[aktsomhetType],
                begrunnelse = resultSet[begrunnelse],
                skalIleggesRenter = null,
            )
        }
    }

    object MottakersForståelseMapper : Entity<MottakersForståelseEntity, UUID, UUID>(
        "tilbakekreving_vilkårsvurdering_periode_mottakers_forståelse",
        MottakersForståelseEntity::periodeRef,
        FieldConverter.UUIDConverter.required(),
    ) {
        val mottakersForståelse = field(
            "mottakers_forståelse",
            MottakersForståelseEntity::mottakersForståelse,
            FieldConverter.EnumConverter.of<Forståelsesgrad>().required(),
        )

        val begrunnelse = field(
            "begrunnelse",
            MottakersForståelseEntity::begrunnelse,
            FieldConverter.StringConverter.required(),
        )

        fun map(
            resultSet: ResultSet,
        ): MottakersForståelseEntity {
            return MottakersForståelseEntity(
                periodeRef = resultSet[id],
                mottakersForståelse = resultSet[mottakersForståelse],
                begrunnelse = resultSet[begrunnelse],
            )
        }
    }

    object ReduksjonMomenterMapper : Entity<ReduksjonMomenterEntity, UUID, UUID>(
        "tilbakekreving_vilkårsvurdering_periode_særlige_grunner",
        { it.periodeRef!! },
        FieldConverter.UUIDConverter.required(),
    ) {
        val begrunnelse = field(
            "begrunnelse",
            ReduksjonMomenterEntity::begrunnelse,
            FieldConverter.StringConverter.required(),
        )
        val annetBegrunnelse = field(
            "annet_begrunnelse",
            ReduksjonMomenterEntity::annetBegrunnelse,
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

        val reduksjonMomenter = field(
            "reduksjon_momenter",
            ReduksjonMomenterEntity::grunner,
            FieldConverter.StringArrayConverter,
        )

        fun map(
            resultSet: ResultSet,
        ): ReduksjonMomenterEntity {
            val grunner = resultSet[reduksjonMomenter]
            return ReduksjonMomenterEntity(
                periodeRef = resultSet[id],
                begrunnelse = resultSet[begrunnelse],
                grunner = grunner,
                skalReduseres = SkalReduseresEntity(
                    type = resultSet[skalReduseres],
                    prosentdel = resultSet[reduksjonProsent],
                ),
                annetBegrunnelse = resultSet[annetBegrunnelse],
            )
        }
    }
}
