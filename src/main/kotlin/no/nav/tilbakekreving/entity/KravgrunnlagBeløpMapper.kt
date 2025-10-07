package no.nav.tilbakekreving.entity

import no.nav.tilbakekreving.entities.BeløpEntity
import java.sql.ResultSet
import java.util.UUID

object KravgrunnlagBeløpMapper : Entity<BeløpEntity, UUID, UUID>(
    "tilbakekreving_kravgrunnlag_beløp",
    BeløpEntity::id,
    FieldConverter.UUIDConverter.required(),
) {
    val kravgrunnlagPeriodeId = field(
        "kravgrunnlag_periode_id",
        BeløpEntity::kravgrunnlagPeriodeId,
        FieldConverter.UUIDConverter.required(),
    )
    val klassekode = field(
        "klassekode",
        BeløpEntity::klassekode,
        FieldConverter.StringConverter.required(),
    )
    val klassetype = field(
        "klassetype",
        BeløpEntity::klassetype,
        FieldConverter.StringConverter.required(),
    )
    val opprinneligUtbetalingsbeløp = field(
        "opprinnelig_utbetalingsbeløp",
        BeløpEntity::opprinneligUtbetalingsbeløp,
        FieldConverter.BigDecimalConverter.required(),
    )
    val nyttBeløp = field(
        "nytt_beløp",
        BeløpEntity::nyttBeløp,
        FieldConverter.BigDecimalConverter.required(),
    )
    val tilbakekrevesBeløp = field(
        "tilbakekreves_beløp",
        BeløpEntity::tilbakekrevesBeløp,
        FieldConverter.BigDecimalConverter.required(),
    )
    val skatteprosent = field(
        "skatteprosent",
        BeløpEntity::skatteprosent,
        FieldConverter.BigDecimalConverter.required(),
    )

    fun map(resultSet: ResultSet): BeløpEntity {
        return BeløpEntity(
            id = resultSet[id],
            kravgrunnlagPeriodeId = resultSet[kravgrunnlagPeriodeId],
            klassekode = resultSet[klassekode],
            klassetype = resultSet[klassetype],
            opprinneligUtbetalingsbeløp = resultSet[opprinneligUtbetalingsbeløp],
            nyttBeløp = resultSet[nyttBeløp],
            tilbakekrevesBeløp = resultSet[tilbakekrevesBeløp],
            skatteprosent = resultSet[skatteprosent],
        )
    }
}
