package no.nav.tilbakekreving.entity

import no.nav.tilbakekreving.behandlingslogg.Behandlingsloggstype
import no.nav.tilbakekreving.behandlingslogg.Rolle
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

    val rolle = field(
        "rolle",
        { it.rolle },
        FieldConverter.EnumConverter.of<Rolle>().required(),
    )

    val behandlerIdent = field(
        "behandler_ident",
        { it.behandlerIdent },
        FieldConverter.StringConverter.required(),
    )

    val opprettetTid = field(
        "opprettet_tid",
        { it.opprettetTid },
        FieldConverter.LocalDateTimeConverter.required(),
    )

    val sistOppdatert = field(
        "sist_oppdatert",
        { it.sistOppdatert },
        FieldConverter.LocalDateTimeConverter,
    )

    val ekstraInfo = field(
        "ekstra_info",
        { it.ekstraInfo },
        FieldConverter.MapToJsonStringConverter,
    )

    fun map(
        resultSet: ResultSet,
    ): LoggInnlagEntity {
        val behandlingsloggstype = when (val type = resultSet[behandlingsloggstype]) {
            Behandlingsloggstype.VARSELBREV_SENDT -> Behandlingsloggstype.FORHÅNDSVARSEL_SENDT
            else -> type
        }
        return LoggInnlagEntity(
            id = resultSet[BrevEntityMapper.id],
            tilbakekrevingRef = resultSet[tilbakekrevingRef],
            behandlingId = resultSet[behandlingId],
            rolle = resultSet[rolle],
            behandlerIdent = resultSet[behandlerIdent],
            opprettetTid = resultSet[opprettetTid],
            sistOppdatert = resultSet[sistOppdatert],
            behandlingsloggstype = behandlingsloggstype,
            ekstraInfo = resultSet[ekstraInfo],
        )
    }
}
