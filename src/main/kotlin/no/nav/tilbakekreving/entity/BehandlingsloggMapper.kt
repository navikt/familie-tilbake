package no.nav.tilbakekreving.entity

import no.nav.tilbakekreving.behandlingslogg.Behandlingsloggstype
import no.nav.tilbakekreving.behandlingslogg.Utfører
import no.nav.tilbakekreving.entities.LoggInnlagEntity
import java.sql.ResultSet
import java.util.UUID

object BehandlingsloggMapper : Entity<LoggInnlagEntity, UUID, UUID>(
    "tilbakekreving_behandlingslogg",
    LoggInnlagEntity::id,
    FieldConverter.UUIDConverter.required(),
) {
    val tilbakekrevingRef = field(
        "tilbakekreving_ref",
        { it.tilbakekrevingRef },
        FieldConverter.NumericId,
    )

    val behandlingId = field(
        "behandling_id",
        { it.behandlingId },
        FieldConverter.UUIDConverter,
    )

    val behandlingsloggstype = field(
        "behandlingsloggstype",
        { it.behandlingsloggstype },
        FieldConverter.EnumConverter.of<Behandlingsloggstype>().required(),
    )

    val utfører = field(
        "utfører",
        { it.utfører },
        FieldConverter.EnumConverter.of<Utfører>().required(),
    )

    val utførerIdent = field(
        "utfører_ident",
        { it.utførerIdent },
        FieldConverter.StringConverter.required(),
    )

    val opprettetTid = field(
        "opprettet_tid",
        { it.opprettetTid },
        FieldConverter.LocalDateTimeConverter.required(),
    )

    fun map(
        resultSet: ResultSet,
    ): LoggInnlagEntity {
        return LoggInnlagEntity(
            id = resultSet[BrevEntityMapper.id],
            tilbakekrevingRef = resultSet[tilbakekrevingRef],
            behandlingId = resultSet[behandlingId],
            utfører = resultSet[utfører],
            utførerIdent = resultSet[utførerIdent],
            opprettetTid = resultSet[opprettetTid],
            behandlingsloggstype = resultSet[behandlingsloggstype],
        )
    }
}
