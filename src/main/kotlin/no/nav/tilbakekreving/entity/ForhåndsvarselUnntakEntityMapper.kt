package no.nav.tilbakekreving.entity

import no.nav.tilbakekreving.behandling.BegrunnelseForUnntak
import no.nav.tilbakekreving.entities.ForhåndsvarselUnntakEntity
import java.sql.ResultSet
import java.util.UUID

object ForhåndsvarselUnntakEntityMapper : Entity<ForhåndsvarselUnntakEntity, UUID, UUID>(
    "tilbakekreving_forhåndsvarsel_unntak",
    ForhåndsvarselUnntakEntity::id,
    FieldConverter.UUIDConverter.required(),
) {
    val behandlingRef = field(
        "behandling_ref",
        { it.behandlingRef },
        FieldConverter.UUIDConverter.required(),
    )

    val begrunnelseForUnntak = field(
        "unntak_begrunnelse",
        { it.begrunnelseForUnntak },
        FieldConverter.EnumConverter.of<BegrunnelseForUnntak>().required(),
    )

    val beskrivelse = field(
        "beskrivelse",
        { it.beskrivelse },
        FieldConverter.StringConverter.required(),
    )

    fun map(
        resultSet: ResultSet,
    ): ForhåndsvarselUnntakEntity {
        return ForhåndsvarselUnntakEntity(
            id = resultSet[id],
            behandlingRef = resultSet[behandlingRef],
            beskrivelse = resultSet[beskrivelse],
            begrunnelseForUnntak = resultSet[begrunnelseForUnntak],
        )
    }
}
