package no.nav.tilbakekreving.entity

import no.nav.tilbakekreving.entities.DatoperiodeEntity
import no.nav.tilbakekreving.entities.ForeldelseperiodeEntity
import no.nav.tilbakekreving.entities.ForeldelsesstegEntity
import no.nav.tilbakekreving.entities.ForeldelsesvurderingEntity
import no.nav.tilbakekreving.entities.ForeldelsesvurderingType
import java.sql.ResultSet
import java.util.UUID

object ForeldelsesvurderingEntityMapper : Entity<ForeldelsesstegEntity, UUID, UUID>(
    "tilbakekreving_foreldelsesvurdering",
    ForeldelsesstegEntity::id,
    FieldConverter.UUIDConverter.required(),
) {
    val behandlingRef = field(
        "behandling_ref",
        ForeldelsesstegEntity::behandlingRef,
        FieldConverter.UUIDConverter.required(),
    )

    fun map(
        resultSet: ResultSet,
        vurdertePerioder: List<ForeldelseperiodeEntity>,
    ): ForeldelsesstegEntity {
        return ForeldelsesstegEntity(
            id = resultSet[id],
            behandlingRef = resultSet[behandlingRef],
            vurdertePerioder = vurdertePerioder,
        )
    }

    object VurdertPeriode : Entity<ForeldelseperiodeEntity, UUID, UUID>(
        "tilbakekreving_foreldelsesvurdering_periode",
        ForeldelseperiodeEntity::id,
        FieldConverter.UUIDConverter.required(),
    ) {
        val foreldelsesvurderingRef = field(
            "foreldelsesvurdering_ref",
            ForeldelseperiodeEntity::foreldelsesvurderingRef,
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

        val vurdering = field(
            "vurdering",
            { it.foreldelsesvurdering.type },
            FieldConverter.EnumConverter.of<ForeldelsesvurderingType>().required(),
        )

        val begrunnelse = field(
            "begrunnelse",
            { it.foreldelsesvurdering.begrunnelse },
            FieldConverter.StringConverter,
        )

        val frist = field(
            "frist",
            { it.foreldelsesvurdering.frist },
            FieldConverter.LocalDateConverter,
        )

        val oppdaget = field(
            "oppdaget",
            { it.foreldelsesvurdering.oppdaget },
            FieldConverter.LocalDateConverter,
        )

        fun map(resultSet: ResultSet): ForeldelseperiodeEntity {
            return ForeldelseperiodeEntity(
                id = resultSet[id],
                foreldelsesvurderingRef = resultSet[foreldelsesvurderingRef],
                periode = DatoperiodeEntity(
                    fom = resultSet[periodeFom],
                    tom = resultSet[periodeTom],
                ),
                foreldelsesvurdering = ForeldelsesvurderingEntity(
                    type = resultSet[vurdering],
                    begrunnelse = resultSet[begrunnelse],
                    frist = resultSet[frist],
                    oppdaget = resultSet[oppdaget],
                ),
            )
        }
    }
}
