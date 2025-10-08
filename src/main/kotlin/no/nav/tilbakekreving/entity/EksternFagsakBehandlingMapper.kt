package no.nav.tilbakekreving.entity

import no.nav.tilbakekreving.entities.DatoperiodeEntity
import no.nav.tilbakekreving.entities.EksternFagsakBehandlingEntity
import no.nav.tilbakekreving.entities.EksternFagsakBehandlingType
import no.nav.tilbakekreving.entities.UtvidetPeriodeEntity
import java.sql.ResultSet
import java.util.UUID

object EksternFagsakBehandlingMapper : Entity<EksternFagsakBehandlingEntity, UUID, UUID>(
    "tilbakekreving_ekstern_fagsak_behandling",
    EksternFagsakBehandlingEntity::id,
    FieldConverter.UUIDConverter.required(),
) {
    val eksternFagsakRef = field(
        "ekstern_fagsak_ref",
        EksternFagsakBehandlingEntity::eksternFagsakRef,
        FieldConverter.UUIDConverter.required(),
    )
    val type = field(
        "type",
        EksternFagsakBehandlingEntity::type,
        FieldConverter.EnumConverter.of<EksternFagsakBehandlingType>().required(),
    )
    val eksternId = field(
        "ekstern_id",
        EksternFagsakBehandlingEntity::eksternId,
        FieldConverter.StringConverter.required(),
    )

    val revurderingsårsak = field(
        "revurderingsårsak",
        EksternFagsakBehandlingEntity::revurderingsårsak,
        FieldConverter.EnumConverter.of(),
    )
    val årsakTilFeilutbetaling = field(
        "årsak_til_feilutbetaling",
        EksternFagsakBehandlingEntity::årsakTilFeilutbetaling,
        FieldConverter.StringConverter,
    )
    val vedtaksdato = field(
        "vedtaksdato",
        EksternFagsakBehandlingEntity::vedtaksdato,
        FieldConverter.LocalDateConverter,
    )

    fun map(resultSet: ResultSet, utvidetPerioder: List<UtvidetPeriodeEntity>): EksternFagsakBehandlingEntity {
        return EksternFagsakBehandlingEntity(
            id = resultSet[id],
            eksternFagsakRef = resultSet[eksternFagsakRef],
            type = resultSet[type],
            eksternId = resultSet[eksternId],
            revurderingsårsak = resultSet[revurderingsårsak],
            årsakTilFeilutbetaling = resultSet[årsakTilFeilutbetaling],
            vedtaksdato = resultSet[vedtaksdato],
            utvidedePerioder = utvidetPerioder,
        )
    }

    object UtvidetPeriodeMapper : Entity<UtvidetPeriodeEntity, UUID, UUID>(
        "tilbakekreving_ekstern_fagsak_behandling_utvidet_periode",
        UtvidetPeriodeEntity::id,
        FieldConverter.UUIDConverter.required(),
    ) {
        val eksternFagsakBehandlingRef = field(
            "ekstern_fagsak_behandling_ref",
            UtvidetPeriodeEntity::eksternFagsakBehandlingRef,
            FieldConverter.UUIDConverter.required(),
        )
        val kravgrunnlagPeriodeFom = field(
            "kravgrunnlag_periode_fom",
            { it.kravgrunnlagPeriode.fom },
            FieldConverter.LocalDateConverter.required(),
        )
        val kravgrunnlagPeriodeTom = field(
            "kravgrunnlag_periode_tom",
            { it.kravgrunnlagPeriode.tom },
            FieldConverter.LocalDateConverter.required(),
        )
        val vedtaksperiodeFom = field(
            "vedtaksperiode_fom",
            { it.vedtaksperiode.fom },
            FieldConverter.LocalDateConverter.required(),
        )
        val vedtaksperiodeTom = field(
            "vedtaksperiode_tom",
            { it.vedtaksperiode.tom },
            FieldConverter.LocalDateConverter.required(),
        )

        fun map(resultSet: ResultSet): UtvidetPeriodeEntity {
            return UtvidetPeriodeEntity(
                id = resultSet[id],
                eksternFagsakBehandlingRef = resultSet[eksternFagsakBehandlingRef],
                kravgrunnlagPeriode = DatoperiodeEntity(
                    fom = resultSet[kravgrunnlagPeriodeFom],
                    tom = resultSet[kravgrunnlagPeriodeTom],
                ),
                vedtaksperiode = DatoperiodeEntity(
                    fom = resultSet[vedtaksperiodeFom],
                    tom = resultSet[vedtaksperiodeTom],
                ),
            )
        }
    }
}
