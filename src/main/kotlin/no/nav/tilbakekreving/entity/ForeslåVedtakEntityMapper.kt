package no.nav.tilbakekreving.entity

import no.nav.tilbakekreving.behandling.saksbehandling.ÅrsakTilTilbakeføring
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

    val tilbakeført = field(
        "tilbakeført",
        ForeslåVedtakStegEntity::tilbakeført,
        FieldConverter.EnumConverter.of<ÅrsakTilTilbakeføring>(),
    )

    fun map(
        resultSet: ResultSet,
    ): ForeslåVedtakStegEntity {
        return ForeslåVedtakStegEntity(
            id = resultSet[id],
            behandlingRef = resultSet[behandlingRef],
            vurdert = resultSet[vurdert],
            tilbakeført = resultSet[tilbakeført],
        )
    }
}
