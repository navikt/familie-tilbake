package no.nav.tilbakekreving.entity

import no.nav.tilbakekreving.entities.PåVentEntity
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
import java.sql.ResultSet
import java.util.UUID

object PåventEntityMapper : Entity<PåVentEntity, UUID, UUID>(
    "tilbakekreving_påvent",
    PåVentEntity::id,
    FieldConverter.UUIDConverter.required(),
) {
    val behandlingRef = field(
        "behandling_ref",
        { it.behandlingRef!! },
        FieldConverter.UUIDConverter.required(),
    )

    val årsak = field(
        "årsak",
        PåVentEntity::årsak,
        FieldConverter.EnumConverter.of<Venteårsak>().required(),
    )

    val utløpsdato = field(
        "utløpsdato",
        PåVentEntity::utløpsdato,
        FieldConverter.LocalDateConverter.required(),
    )

    val begrunnelse = field(
        "begrunnelse",
        { it.begrunnelse!! },
        FieldConverter.StringConverter.required(),
    )

    fun map(resultSet: ResultSet): PåVentEntity {
        return PåVentEntity(
            id = resultSet[id],
            behandlingRef = resultSet[behandlingRef],
            årsak = resultSet[årsak],
            utløpsdato = resultSet[utløpsdato],
            begrunnelse = resultSet[begrunnelse],
        )
    }
}
