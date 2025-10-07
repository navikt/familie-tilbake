package no.nav.tilbakekreving.entity

import no.nav.tilbakekreving.entities.AktørEntity
import no.nav.tilbakekreving.entities.AktørType
import no.nav.tilbakekreving.entities.KravgrunnlagHendelseEntity
import no.nav.tilbakekreving.entities.KravgrunnlagPeriodeEntity
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse.Kravstatuskode
import java.sql.ResultSet
import java.util.UUID

object KravgrunnlagHendelseMapper : Entity<KravgrunnlagHendelseEntity, UUID, UUID>(
    "tilbakekreving_kravgrunnlag",
    KravgrunnlagHendelseEntity::id,
    FieldConverter.UUIDConverter.required(),
) {
    val tilbakekrevingId = field(
        "tilbakekreving_id",
        KravgrunnlagHendelseEntity::tilbakekrevingId,
        FieldConverter.NumericId,
    )
    val vedtakId = field(
        "vedtak_id",
        KravgrunnlagHendelseEntity::vedtakId,
        FieldConverter.BigIntConverter.required(),
    )
    val kravstatuskode = field(
        "kravstatuskode",
        KravgrunnlagHendelseEntity::kravstatuskode,
        FieldConverter.EnumConverter.of<Kravstatuskode>().required(),
    )
    val fagsystemVedtaksdato = field(
        "fagsystem_vedtaksdato",
        KravgrunnlagHendelseEntity::fagsystemVedtaksdato,
        FieldConverter.LocalDateConverter,
    )
    val vedtakGjelderType = field(
        "vedtak_gjelder_type",
        { it.vedtakGjelder.aktørType },
        FieldConverter.EnumConverter.of<AktørType>().required(),
    )
    val vedtakGjelderIdent = field(
        "vedtak_gjelder_ident",
        { it.vedtakGjelder.ident },
        FieldConverter.StringConverter.required(),
    )
    val utbetalesTilType = field(
        "utbetales_til_type",
        { it.utbetalesTil.aktørType },
        FieldConverter.EnumConverter.of<AktørType>().required(),
    )
    val utbetalesTilIdent = field(
        "utbetales_til_ident",
        { it.utbetalesTil.ident },
        FieldConverter.StringConverter.required(),
    )
    val skalBeregneRenter = field(
        "skal_beregne_renter",
        KravgrunnlagHendelseEntity::skalBeregneRenter,
        FieldConverter.BooleanConverter.required(),
    )
    val ansvarligEnhet = field(
        "ansvarlig_enhet",
        KravgrunnlagHendelseEntity::ansvarligEnhet,
        FieldConverter.StringConverter.required(),
    )
    val kontrollfelt = field(
        "kontrollfelt",
        KravgrunnlagHendelseEntity::kontrollfelt,
        FieldConverter.StringConverter.required(),
    )
    val kravgrunnlagId = field(
        "kravgrunnlag_id",
        KravgrunnlagHendelseEntity::kravgrunnlagId,
        FieldConverter.StringConverter.required(),
    )
    val referanse = field(
        "referanse",
        KravgrunnlagHendelseEntity::referanse,
        FieldConverter.StringConverter.required(),
    )

    fun map(
        resultSet: ResultSet,
        perioder: List<KravgrunnlagPeriodeEntity>,
    ): KravgrunnlagHendelseEntity {
        return KravgrunnlagHendelseEntity(
            id = resultSet[id],
            tilbakekrevingId = resultSet[tilbakekrevingId],
            vedtakId = resultSet[vedtakId],
            kravstatuskode = resultSet[kravstatuskode],
            fagsystemVedtaksdato = resultSet[fagsystemVedtaksdato],
            vedtakGjelder = AktørEntity(
                resultSet[vedtakGjelderType],
                resultSet[vedtakGjelderIdent],
            ),
            utbetalesTil = AktørEntity(
                resultSet[utbetalesTilType],
                resultSet[utbetalesTilIdent],
            ),
            skalBeregneRenter = resultSet[skalBeregneRenter],
            ansvarligEnhet = resultSet[ansvarligEnhet],
            kontrollfelt = resultSet[kontrollfelt],
            kravgrunnlagId = resultSet[kravgrunnlagId],
            referanse = resultSet[referanse],
            perioder = perioder,
        )
    }
}
