package no.nav.tilbakekreving.entity

import no.nav.tilbakekreving.entities.AddresseEntity
import java.sql.ResultSet
import java.util.UUID

object BrevmottakerEntityMapper : Entity<AddresseEntity, UUID, UUID>(
    "tilbakekreving_brevmottaker",
    AddresseEntity::id,
    FieldConverter.UUIDConverter.required(),
) {
    private val tilbakekrevingRef = field(
        "tilbakekreving_ref",
        AddresseEntity::tilbakekrevingRef,
        FieldConverter.StringConverter.required(),
    )

    private val type = field(
        "type",
        AddresseEntity::type,
        FieldConverter.EnumConverter.of<AddresseEntity.Type>(),
    )

    private val tilleggsmottakerType = field(
        "tilleggsmottaker_type",
        AddresseEntity::tilleggsmottakerType,
        FieldConverter.EnumConverter.of<AddresseEntity.TilleggsmottakerType>(),
    )

    private val land = field(
        "land",
        AddresseEntity::land,
        FieldConverter.StringConverter,
    )

    private val addresselinje = field(
        "addresselinje",
        AddresseEntity::addresselinje,
        FieldConverter.StringConverter,
    )

    private val postnummer = field(
        "postnummer",
        AddresseEntity::postnummer,
        FieldConverter.StringConverter,
    )

    private val navn = field(
        "navn",
        AddresseEntity::navn,
        FieldConverter.StringConverter,
    )

    private val mottakerIdent = field(
        "mottaker_ident",
        AddresseEntity::mottakerIdent,
        FieldConverter.StringConverter,
    )

    fun map(resultSet: ResultSet): AddresseEntity {
        return AddresseEntity(
            id = resultSet[id],
            tilbakekrevingRef = resultSet[tilbakekrevingRef],
            type = resultSet[type],
            tilleggsmottakerType = resultSet[tilleggsmottakerType],
            land = resultSet[land],
            addresselinje = resultSet[addresselinje],
            postnummer = resultSet[postnummer],
            navn = resultSet[navn],
            mottakerIdent = resultSet[mottakerIdent],
        )
    }
}
