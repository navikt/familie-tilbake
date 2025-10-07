package no.nav.tilbakekreving.entity

import no.nav.tilbakekreving.entities.BeløpEntity
import no.nav.tilbakekreving.entities.DatoperiodeEntity
import no.nav.tilbakekreving.entities.KravgrunnlagPeriodeEntity
import java.sql.ResultSet
import java.util.UUID

object KravgrunnlagPeriodeMapper : Entity<KravgrunnlagPeriodeEntity, UUID, UUID>(
    "tilbakekreving_kravgrunnlag_periode",
    KravgrunnlagPeriodeEntity::id,
    FieldConverter.UUIDConverter.required(),
) {
    val kravgrunnlagId = field(
        "kravgrunnlag_id",
        KravgrunnlagPeriodeEntity::kravgrunnlagId,
        FieldConverter.UUIDConverter.required(),
    )
    val fom = field(
        "fom",
        { it.periode.fom },
        FieldConverter.LocalDateConverter.required(),
    )
    val tom = field(
        "tom",
        { it.periode.tom },
        FieldConverter.LocalDateConverter.required(),
    )
    val månedligSkattebeløp = field(
        "månedlig_skattebeløp",
        KravgrunnlagPeriodeEntity::månedligSkattebeløp,
        FieldConverter.BigDecimalConverter.required(),
    )

    fun map(
        resultSet: ResultSet,
        beløp: List<BeløpEntity>,
    ): KravgrunnlagPeriodeEntity {
        return KravgrunnlagPeriodeEntity(
            id = resultSet[id],
            kravgrunnlagId = resultSet[kravgrunnlagId],
            periode = DatoperiodeEntity(
                fom = resultSet[fom],
                tom = resultSet[tom],
            ),
            månedligSkattebeløp = resultSet[månedligSkattebeløp],
            beløp = beløp,
        )
    }
}
