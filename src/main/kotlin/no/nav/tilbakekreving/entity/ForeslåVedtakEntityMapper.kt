package no.nav.tilbakekreving.entity

import no.nav.tilbakekreving.entities.ForeslåVedtakStegEntity
import java.sql.ResultSet
import java.util.UUID

object ForeslåVedtakEntityMapper : Entity<ForeslåVedtakStegEntity, UUID, UUID>(
    "tilbakekreving_foreslåvedtak",
    ForeslåVedtakStegEntity::id,
    FieldConverter.UUIDConverter.required(),
) {
    val behandlingRef = field(
        "behandling_ref",
        { it.behandlingRef!! },
        FieldConverter.UUIDConverter.required(),
    )

    val vurdert = field(
        "vurdert",
        ForeslåVedtakStegEntity::vurdert,
        FieldConverter.BooleanConverter.required(),
    )

    val trengerNyVurdering = field(
        "trenger_ny_vurdering",
        ForeslåVedtakStegEntity::trengerNyVurdering,
        FieldConverter.BooleanConverter.required(),
    )

    fun map(
        resultSet: ResultSet,
    ): ForeslåVedtakStegEntity {
        return ForeslåVedtakStegEntity(
            id = resultSet[id],
            behandlingRef = resultSet[behandlingRef],
            vurdert = resultSet[vurdert],
            trengerNyVurdering = resultSet[trengerNyVurdering],
        )
    }
}
