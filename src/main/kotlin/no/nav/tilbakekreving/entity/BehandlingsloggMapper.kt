package no.nav.tilbakekreving.entity

import no.nav.tilbakekreving.behandlingslogg.Utfører
import no.nav.tilbakekreving.entities.LoggInnlagEntity
import no.nav.tilbakekreving.kontrakter.historikk.Historikkinnslagstype
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
        FieldConverter.UUIDConverter.required(),
    )

    val type = field(
        "type",
        { it.type },
        FieldConverter.EnumConverter.of<Historikkinnslagstype>().required(),
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

    val tittel = field(
        "tittel",
        { it.tittel },
        FieldConverter.StringConverter.required(),
    )

    val tekst = field(
        "tekst",
        { it.tekst },
        FieldConverter.StringConverter,
    )

    val steg = field(
        "steg",
        { it.steg },
        FieldConverter.StringConverter,
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
            type = resultSet[type],
            utfører = resultSet[utfører],
            utførerIdent = resultSet[utførerIdent],
            tittel = resultSet[tittel],
            tekst = resultSet[tekst],
            steg = resultSet[steg],
            opprettetTid = resultSet[opprettetTid],
        )
    }
}
