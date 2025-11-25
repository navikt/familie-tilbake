package no.nav.tilbakekreving.entity

import no.nav.tilbakekreving.entities.FristUtsettelseEntity
import java.sql.ResultSet
import java.util.UUID

object FristUtsettelseEntityMapper : Entity<FristUtsettelseEntity, UUID, UUID>(
    "tilbakekreving_utsett_uttalelse",
    FristUtsettelseEntity::id,
    FieldConverter.UUIDConverter.required(),
) {
    val behandlingRef = field(
        "behandling_ref",
        FristUtsettelseEntity::behandlingRef,
        FieldConverter.UUIDConverter.required(),
    )

    val nyFrist = field(
        "ny_frist",
        FristUtsettelseEntity::nyFrist,
        FieldConverter.LocalDateConverter.required(),
    )

    val begrunnelse = field(
        "begrunnelse",
        FristUtsettelseEntity::begrunnelse,
        FieldConverter.StringConverter.required(),
    )

    fun map(resultSet: ResultSet): FristUtsettelseEntity {
        return FristUtsettelseEntity(
            id = resultSet[id],
            behandlingRef = resultSet[behandlingRef],
            nyFrist = resultSet[nyFrist],
            begrunnelse = resultSet[begrunnelse],
        )
    }
}
