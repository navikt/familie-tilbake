package no.nav.tilbakekreving.entity

import no.nav.tilbakekreving.entities.UttalelsesfristEntity
import java.sql.ResultSet
import java.util.UUID

object FristUtsettelseEntityMapper : Entity<UttalelsesfristEntity, UUID, UUID>(
    "tilbakekreving_uttalelsesfrist",
    UttalelsesfristEntity::id,
    FieldConverter.UUIDConverter.required(),
) {
    val behandlingRef = field(
        "behandling_ref",
        UttalelsesfristEntity::behandlingRef,
        FieldConverter.UUIDConverter.required(),
    )

    val opprinneligFrist = field(
        "opprinnelig_frist",
        UttalelsesfristEntity::opprinneligFrist,
        FieldConverter.LocalDateConverter.required(),
    )

    val nyFrist = field(
        "ny_frist",
        UttalelsesfristEntity::nyFrist,
        FieldConverter.LocalDateConverter,
    )

    val begrunnelse = field(
        "begrunnelse",
        UttalelsesfristEntity::begrunnelse,
        FieldConverter.StringConverter,
    )

    fun map(resultSet: ResultSet): UttalelsesfristEntity {
        return UttalelsesfristEntity(
            id = resultSet[id],
            behandlingRef = resultSet[behandlingRef],
            opprinneligFrist = resultSet[opprinneligFrist],
            nyFrist = resultSet[nyFrist],
            begrunnelse = resultSet[begrunnelse],
        )
    }
}
