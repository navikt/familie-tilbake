package no.nav.tilbakekreving.entity

import no.nav.tilbakekreving.entities.AktørEntity
import no.nav.tilbakekreving.entities.AktørType
import no.nav.tilbakekreving.entities.KravStatusmeldingEntity
import no.nav.tilbakekreving.hendelse.Kravstatuskode
import java.sql.ResultSet
import java.util.UUID

object KravStatusmeldingMapper : Entity<KravStatusmeldingEntity, UUID, UUID>(
    "tilbakekreving_krav_statusmelding",
    KravStatusmeldingEntity::id,
    FieldConverter.UUIDConverter.required(),
) {
    val tilbakekrevingId = field(
        "tilbakekreving_id",
        KravStatusmeldingEntity::tilbakekrevingId,
        FieldConverter.NumericId,
    )
    val vedtakId = field(
        "vedtak_id",
        KravStatusmeldingEntity::vedtakId,
        FieldConverter.BigIntConverter.required(),
    )
    val kravstatuskode = field(
        "kravstatuskode",
        KravStatusmeldingEntity::kravstatuskode,
        FieldConverter.EnumConverter.of<Kravstatuskode>().required(),
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
    val referanse = field(
        "referanse",
        KravStatusmeldingEntity::referanse,
        FieldConverter.StringConverter,
    )
    val kodeFagomraade = field(
        "kode_fagomraade",
        KravStatusmeldingEntity::kodeFagomraade,
        FieldConverter.StringConverter.required(),
    )
    val fagsystemId = field(
        "fagsystem_id",
        KravStatusmeldingEntity::fagsystemId,
        FieldConverter.StringConverter.required(),
    )
    val opprettet = field(
        "opprettet",
        KravStatusmeldingEntity::opprettet,
        FieldConverter.LocalDateTimeConverter.required(),
    )

    fun map(resultSet: ResultSet): KravStatusmeldingEntity {
        return KravStatusmeldingEntity(
            id = resultSet[id],
            tilbakekrevingId = resultSet[tilbakekrevingId],
            vedtakId = resultSet[vedtakId],
            kravstatuskode = resultSet[kravstatuskode],
            vedtakGjelder = AktørEntity(
                resultSet[vedtakGjelderType],
                resultSet[vedtakGjelderIdent],
            ),
            referanse = resultSet[referanse],
            kodeFagomraade = resultSet[kodeFagomraade],
            fagsystemId = resultSet[fagsystemId],
            opprettet = resultSet[opprettet],
        )
    }
}
