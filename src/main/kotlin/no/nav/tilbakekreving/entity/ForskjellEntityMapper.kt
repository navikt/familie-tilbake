package no.nav.tilbakekreving.entity

import no.nav.tilbakekreving.entities.DatoperiodeEntity
import no.nav.tilbakekreving.entities.ForskjellEntity
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagSammenligning
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
    val type = field(
        "type",
        ForskjellEntity::type,
        FieldConverter.EnumConverter.of<KravgrunnlagSammenligning.ForskjellType>().required(),
    )
    val foreldelsesvurderingPeriodeRef = field(
        "foreldelsesvurdering_periode_ref",
        ForskjellEntity::foreldelsesvurderingPeriodeRef,
        FieldConverter.UUIDConverter,
    )
    val originalPeriodeFom = field(
        "original_periode_fom",
        { it.originalPeriode?.fom },
        FieldConverter.LocalDateConverter,
    )
    val originalPeriodeTom = field(
        "original_periode_tom",
        { it.originalPeriode?.tom },
        FieldConverter.LocalDateConverter,
    )
    val nyPeriodeFom = field(
        "ny_periode_fom",
        { it.nyPeriode?.fom },
        FieldConverter.LocalDateConverter,
    )
    val nyPeriodeTom = field(
        "ny_periode_tom",
        { it.nyPeriode?.tom },
        FieldConverter.LocalDateConverter,
    )
    val gammeltBeløp = field(
        "gammelt_beløp",
        ForskjellEntity::gammeltBeløp,
        FieldConverter.BigDecimalConverter,
    )
    val nyttBeløp = field(
        "nytt_beløp",
        ForskjellEntity::nyttBeløp,
        FieldConverter.BigDecimalConverter,
    )

    fun map(resultSet: ResultSet): ForskjellEntity {
        return ForskjellEntity(
            id = resultSet[id],
            faktavurderingPeriodeRef = resultSet[faktavurderingPeriodeRef],
            vilkårsvurderingPeriodeRef = resultSet[vilkårsvurderingPeriodeRef],
            foreldelsesvurderingPeriodeRef = resultSet[foreldelsesvurderingPeriodeRef],
            type = resultSet[type],
            originalPeriode = resultSet[originalPeriodeFom]?.let {
                DatoperiodeEntity(
                    fom = it,
                    tom = resultSet[originalPeriodeTom]!!,
                )
            },
            nyPeriode = resultSet[nyPeriodeFom]?.let {
                DatoperiodeEntity(
                    fom = it,
                    tom = resultSet[nyPeriodeTom]!!,
                )
            },
            gammeltBeløp = resultSet[gammeltBeløp],
            nyttBeløp = resultSet[nyttBeløp],
        )
    }
}
