package no.nav.tilbakekreving.entity

import no.nav.tilbakekreving.entities.BrevEntity
import no.nav.tilbakekreving.entities.Brevtype
import no.nav.tilbakekreving.entities.HistorikkReferanseEntity
import no.nav.tilbakekreving.entities.VarselbrevEntity
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
        BrevEntity::brevType,
        FieldConverter.EnumConverter.of<Brevtype>().required(),
    )

    val kravgrunnlagRef = field(
        "kravgrunnlag_ref",
        { it.kravgrunnlagRef.id },
        FieldConverter.UUIDConverter.required(),
    )

    fun map(resultSet: ResultSet, varselbrevEntity: VarselbrevEntity?): BrevEntity {
        return BrevEntity(
            id = resultSet[id],
            tilbakekrevingRef = resultSet[tilbakekrevingRef],
            brevType = resultSet[brevType],
            kravgrunnlagRef = HistorikkReferanseEntity(resultSet[kravgrunnlagRef]),
            varselbrevEntity = varselbrevEntity,
        )
    }

    object VarselbrevEntityMapper : Entity<VarselbrevEntity, UUID, UUID>(
        "tilbakekreving_varselbrev",
        VarselbrevEntity::id,
        FieldConverter.UUIDConverter.required(),
    ) {
        val brevRef = field(
            "brev_ref",
            VarselbrevEntity::brevRef,
            FieldConverter.UUIDConverter.required(),
        )

        val sendtTid = field(
            "sendt_tid",
            VarselbrevEntity::sendtTid,
            FieldConverter.LocalDateConverter,
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

        val ansvarligSaksbehandlerIdent = field(
            "ansvarlig_saksbehandler_ident",
            VarselbrevEntity::ansvarligSaksbehandlerIdent,
            FieldConverter.StringConverter.required(),
        )

        val tekstFraSaksbehandler = field(
            "tekst_fra_saksbehandler",
            VarselbrevEntity::tekstFraSaksbehandler,
            FieldConverter.StringConverter,
        )

        fun map(resultSet: ResultSet): VarselbrevEntity {
            return VarselbrevEntity(
                id = resultSet[id],
                brevRef = resultSet[brevRef],
                journalpostId = resultSet[journalpostId],
                sendtTid = resultSet[sendtTid],
                ansvarligSaksbehandlerIdent = resultSet[ansvarligSaksbehandlerIdent],
                fristForUttalelse = resultSet[fristForUttalelse],
                tekstFraSaksbehandler = resultSet[tekstFraSaksbehandler],
            )
        }
    }
}
