package no.nav.tilbakekreving.entity

import no.nav.tilbakekreving.entities.DatoperiodeEntity
import no.nav.tilbakekreving.entities.ForskjellEntity
import java.sql.ResultSet
import java.util.UUID

object ForskjellEntityMapper : Entity<ForskjellEntity, UUID, UUID>(
    "tilbakekreving_kravgrunnlag_forskjell",
    ForskjellEntity::id,
    FieldConverter.UUIDConverter.required(),
) {
    val faktavurderingPeriodeRef = field(
        "faktavurdering_periode_ref",
        ForskjellEntity::faktavurderingPeriodeRef,
        FieldConverter.UUIDConverter,
    )
    val vilkårsvurderingPeriodeRef = field(
        "vilkårsvurdering_periode_ref",
        ForskjellEntity::vilkårsvurderingPeriodeRef,
        FieldConverter.UUIDConverter,
    )
    val originalPeriodeFom = field(
        "original_periode_fom",
        { it.originalPeriode.fom },
        FieldConverter.LocalDateConverter.required(),
    )
    val originalPeriodeTom = field(
        "original_periode_tom",
        { it.originalPeriode.tom },
        FieldConverter.LocalDateConverter.required(),
    )
    val endringIBeløp = field(
        "endring_i_beløp",
        ForskjellEntity::endringIBeløp,
        FieldConverter.BigDecimalConverter.required(),
    )

    fun map(resultSet: ResultSet): ForskjellEntity {
        return ForskjellEntity(
            id = resultSet[id],
            faktavurderingPeriodeRef = resultSet[faktavurderingPeriodeRef],
            vilkårsvurderingPeriodeRef = resultSet[vilkårsvurderingPeriodeRef],
            originalPeriode = DatoperiodeEntity(
                fom = resultSet[originalPeriodeFom],
                tom = resultSet[originalPeriodeTom],
            ),
            endringIBeløp = resultSet[endringIBeløp],
        )
    }
}
