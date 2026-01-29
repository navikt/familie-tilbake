package no.nav.tilbakekreving.entity

import no.nav.tilbakekreving.entities.AktørEntity
import no.nav.tilbakekreving.entities.AktørType
import no.nav.tilbakekreving.entities.BrukerEntity
import no.nav.tilbakekreving.kontrakter.bruker.Kjønn
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import java.sql.ResultSet
import java.util.UUID

object BrukerEntityMapper : Entity<BrukerEntity, UUID, UUID>(
    "tilbakekreving_bruker",
    { it.id!! },
    FieldConverter.UUIDConverter.required(),
) {
    val tilbakekrevingRef = field(
        "tilbakekreving_ref",
        { it.tilbakekrevingRef!! },
        FieldConverter.StringConverter.required(),
    )

    val ident = field(
        "ident",
        { it.aktørEntity.ident },
        FieldConverter.StringConverter.required(),
    )

    val aktørType = field(
        "aktør_type",
        { it.aktørEntity.aktørType },
        FieldConverter.EnumConverter.of<AktørType>().required(),
    )

    val språkkode = field(
        "språkkode",
        BrukerEntity::språkkode,
        FieldConverter.EnumConverter.of<Språkkode>(),
    )

    val navn = field(
        "navn",
        BrukerEntity::navn,
        FieldConverter.StringConverter,
    )

    val fødselsdato = field(
        "fødselsdato",
        BrukerEntity::fødselsdato,
        FieldConverter.LocalDateConverter,
    )

    val kjønn = field(
        "kjønn",
        BrukerEntity::kjønn,
        FieldConverter.EnumConverter.of<Kjønn>(),
    )

    val dødsdato = field(
        "dødsdato",
        BrukerEntity::dødsdato,
        FieldConverter.LocalDateConverter,
    )

    fun map(resultSet: ResultSet): BrukerEntity {
        return BrukerEntity(
            id = resultSet[id],
            tilbakekrevingRef = resultSet[tilbakekrevingRef],
            aktørEntity = AktørEntity(resultSet[aktørType], resultSet[ident]),
            språkkode = resultSet[språkkode],
            navn = resultSet[navn],
            fødselsdato = resultSet[fødselsdato],
            kjønn = resultSet[kjønn],
            dødsdato = resultSet[dødsdato],
        )
    }
}
