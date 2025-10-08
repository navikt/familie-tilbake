package no.nav.tilbakekreving.entity

import no.nav.tilbakekreving.entities.EksternFagsakBehandlingEntity
import no.nav.tilbakekreving.entities.EksternFagsakEntity
import no.nav.tilbakekreving.entities.YtelseEntity
import no.nav.tilbakekreving.fagsystem.Ytelsestype
import java.sql.ResultSet
import java.util.UUID

object EksternFagsakMapper : Entity<EksternFagsakEntity, UUID, UUID>(
    "tilbakekreving_ekstern_fagsak",
    EksternFagsakEntity::id,
    FieldConverter.UUIDConverter.required(),
) {
    val tilbakekrevingRef = field(
        "tilbakekreving_ref",
        EksternFagsakEntity::tilbakekrevingRef,
        FieldConverter.NumericId,
    )

    val eksternId = field(
        "ekstern_id",
        EksternFagsakEntity::eksternId,
        FieldConverter.StringConverter.required(),
    )
    val ytelse = field(
        "ytelse",
        { it.ytelseEntity.type },
        FieldConverter.EnumConverter.of<Ytelsestype>().required(),
    )

    fun map(resultSet: ResultSet, behandlinger: List<EksternFagsakBehandlingEntity>): EksternFagsakEntity {
        return EksternFagsakEntity(
            id = resultSet[id],
            tilbakekrevingRef = resultSet[tilbakekrevingRef],
            eksternId = resultSet[eksternId],
            ytelseEntity = YtelseEntity(
                type = resultSet[ytelse],
            ),
            behandlinger = behandlinger,
        )
    }
}
