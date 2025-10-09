package no.nav.tilbakekreving.entity

import no.nav.tilbakekreving.entities.DatoperiodeEntity
import no.nav.tilbakekreving.entities.FaktastegEntity
import no.nav.tilbakekreving.entities.FaktastegEntity.FaktaPeriodeEntity
import no.nav.tilbakekreving.entities.FaktastegEntity.Uttalelse
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsesundertype
import java.sql.ResultSet
import java.util.UUID

object FaktavurderingEntityMapper : Entity<FaktastegEntity, UUID, UUID>(
    "tilbakekreving_faktavurdering",
    FaktastegEntity::id,
    FieldConverter.UUIDConverter.required(),
) {
    val behandlingRef = field(
        "behandling_ref",
        FaktastegEntity::behandlingRef,
        FieldConverter.UUIDConverter.required(),
    )
    val årsakTilFeilutbetaling = field(
        "årsak_til_feilutbetaling",
        FaktastegEntity::årsakTilFeilutbetaling,
        FieldConverter.StringConverter.required(),
    )
    val uttalelse = field(
        "uttalelse",
        FaktastegEntity::uttalelse,
        FieldConverter.EnumConverter.of<Uttalelse>().required(),
    )
    val vurderingAvBrukersUttalelse = field(
        "vurdering_av_brukers_uttalelse",
        FaktastegEntity::vurderingAvBrukersUttalelse,
        FieldConverter.StringConverter,
    )

    fun map(
        resultSet: ResultSet,
        perioder: List<FaktaPeriodeEntity>,
    ): FaktastegEntity {
        return FaktastegEntity(
            id = resultSet[id],
            behandlingRef = resultSet[behandlingRef],
            perioder = perioder,
            årsakTilFeilutbetaling = resultSet[årsakTilFeilutbetaling],
            uttalelse = resultSet[uttalelse],
            vurderingAvBrukersUttalelse = resultSet[vurderingAvBrukersUttalelse],
        )
    }

    object FaktavurderingPeriodeEntityMapper : Entity<FaktaPeriodeEntity, UUID, UUID>(
        "tilbakekreving_faktavurdering_periode",
        FaktaPeriodeEntity::id,
        FieldConverter.UUIDConverter.required(),
    ) {
        val faktavurderingRef = field(
            "faktavurdering_ref",
            FaktaPeriodeEntity::faktavurderingRef,
            FieldConverter.UUIDConverter.required(),
        )
        val periodeFom = field(
            "periode_fom",
            { it.periode.fom },
            FieldConverter.LocalDateConverter.required(),
        )
        val periodeTom = field(
            "periode_tom",
            { it.periode.tom },
            FieldConverter.LocalDateConverter.required(),
        )
        val rettsligGrunnlag = field(
            "rettslig_grunnlag",
            FaktaPeriodeEntity::rettsligGrunnlag,
            FieldConverter.EnumConverter.of<Hendelsestype>().required(),
        )
        val rettsligGrunnlagUnderkategori = field(
            "rettslig_grunnlag_underkategori",
            FaktaPeriodeEntity::rettsligGrunnlagUnderkategori,
            FieldConverter.EnumConverter.of<Hendelsesundertype>().required(),
        )

        fun map(resultSet: ResultSet): FaktaPeriodeEntity {
            return FaktaPeriodeEntity(
                id = resultSet[id],
                faktavurderingRef = resultSet[faktavurderingRef],
                periode = DatoperiodeEntity(
                    fom = resultSet[periodeFom],
                    tom = resultSet[periodeTom],
                ),
                rettsligGrunnlag = resultSet[rettsligGrunnlag],
                rettsligGrunnlagUnderkategori = resultSet[rettsligGrunnlagUnderkategori],
            )
        }
    }
}
