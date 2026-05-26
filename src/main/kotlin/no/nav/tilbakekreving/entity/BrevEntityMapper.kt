package no.nav.tilbakekreving.entity

import no.nav.tilbakekreving.entities.BrevEntity
import no.nav.tilbakekreving.entities.Brevtype
import no.nav.tilbakekreving.entities.HistorikkReferanseEntity
import no.nav.tilbakekreving.entities.VarselbrevEntity
import no.nav.tilbakekreving.entities.VedtaksbrevEntity
import java.sql.ResultSet
import java.util.UUID

object BrevEntityMapper : Entity<BrevEntity, UUID, UUID>(
    "tilbakekreving_brev",
    BrevEntity::id,
    FieldConverter.UUIDConverter.required(),
) {
    val tilbakekrevingRef = field(
        "tilbakekreving_ref",
        { it.tilbakekrevingRef!! },
        FieldConverter.NumericId,
    )

    val brevType = field(
        "brevtype",
        BrevEntity::brevtype,
        FieldConverter.EnumConverter.of<Brevtype>().required(),
    )

    fun map(
        resultSet: ResultSet,
        varselbrevEntity: VarselbrevEntity?,
        vedtaksbrevEntity: VedtaksbrevEntity?,
    ): BrevEntity {
        return BrevEntity(
            id = resultSet[id],
            tilbakekrevingRef = resultSet[tilbakekrevingRef],
            brevtype = resultSet[brevType],
            varselbrevEntity = varselbrevEntity,
            vedtaksbrevEntity = vedtaksbrevEntity,
        )
    }

    object VarselbrevEntityMapper : Entity<VarselbrevEntity, UUID, UUID>(
        "tilbakekreving_varselbrev",
        VarselbrevEntity::id,
        FieldConverter.UUIDConverter.required(),
    ) {
        val kravgrunnlagRef = field(
            "kravgrunnlag_ref",
            { it.kravgrunnlagRef.id },
            FieldConverter.UUIDConverter.required(),
        )

        val sendtTid = field(
            "sendt_tid",
            VarselbrevEntity::sendtTid,
            FieldConverter.LocalDateConverter.required(),
        )

        val fristForUttalelse = field(
            "frist_for_uttalelse",
            VarselbrevEntity::fristForUttalelse,
            FieldConverter.LocalDateConverter,
        )

        val journalpostId = field(
            "journalpost_id",
            VarselbrevEntity::journalpostId,
            FieldConverter.StringConverter,
        )

        val dokumentInfoId = field(
            "dokument_info_id",
            VarselbrevEntity::dokumentInfoId,
            FieldConverter.StringConverter,
        )

        val ansvarligSaksbehandlerIdent = field(
            "ansvarlig_saksbehandler_ident",
            VarselbrevEntity::ansvarligSaksbehandlerIdent,
            FieldConverter.StringConverter.required(),
        )

        val tekstFraSaksbehandler = field(
            "tekst_fra_saksbehandler",
            VarselbrevEntity::tekstFraSaksbehandler,
            FieldConverter.StringConverter.required(),
        )

        fun map(resultSet: ResultSet): VarselbrevEntity {
            return VarselbrevEntity(
                id = resultSet[id],
                kravgrunnlagRef = HistorikkReferanseEntity(resultSet[kravgrunnlagRef]),
                journalpostId = resultSet[journalpostId],
                dokumentInfoId = resultSet[VedtaksbrevEntityMapper.dokumentInfoId],
                sendtTid = resultSet[sendtTid],
                ansvarligSaksbehandlerIdent = resultSet[ansvarligSaksbehandlerIdent],
                fristForUttalelse = resultSet[fristForUttalelse],
                tekstFraSaksbehandler = resultSet[tekstFraSaksbehandler],
            )
        }
    }

    object VedtaksbrevEntityMapper : Entity<VedtaksbrevEntity, UUID, UUID>(
        "tilbakekreving_vedtaksbrev",
        VedtaksbrevEntity::id,
        FieldConverter.UUIDConverter.required(),
    ) {
        val sendtTid = field(
            "sendt_tid",
            VedtaksbrevEntity::sendtTid,
            FieldConverter.LocalDateConverter,
        )
        val journalpostId = field(
            "journalpost_id",
            VedtaksbrevEntity::journalpostId,
            FieldConverter.StringConverter,
        )

        val dokumentInfoId = field(
            "dokument_info_id",
            VedtaksbrevEntity::dokumentInfoId,
            FieldConverter.StringConverter,
        )

        fun map(resultSet: ResultSet): VedtaksbrevEntity {
            return VedtaksbrevEntity(
                id = resultSet[id],
                journalpostId = resultSet[journalpostId],
                dokumentInfoId = resultSet[dokumentInfoId],
                sendtTid = resultSet[sendtTid]!!,
            )
        }
    }
}
