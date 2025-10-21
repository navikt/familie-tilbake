package no.nav.tilbakekreving.entity

import no.nav.tilbakekreving.entities.BrevmottakerStegEntity
import no.nav.tilbakekreving.entities.ManuellAdresseInfoEntity
import no.nav.tilbakekreving.entities.MottakerType
import no.nav.tilbakekreving.entities.RegistrertBrevmottakerEntity
import no.nav.tilbakekreving.kontrakter.verge.Vergetype
import java.sql.ResultSet
import java.util.UUID

object BrevmottakerEntityMapper : Entity<BrevmottakerStegEntity, UUID, UUID>(
    "tilbakekreving_brevmottaker",
    BrevmottakerStegEntity::id,
    FieldConverter.UUIDConverter.required(),
) {
    val behandlingRef = field(
        "behandling_ref",
        { it.behandlingRef!! },
        FieldConverter.UUIDConverter.required(),
    )

    val aktivert = field(
        "aktivert",
        BrevmottakerStegEntity::aktivert,
        FieldConverter.BooleanConverter.required(),
    )

    fun map(
        resultSet: ResultSet,
        defaultMottakerEntity: RegistrertBrevmottakerEntity,
        registrertBrevmottakerEntity: RegistrertBrevmottakerEntity,
    ): BrevmottakerStegEntity {
        return BrevmottakerStegEntity(
            id = resultSet[id],
            behandlingRef = resultSet[behandlingRef],
            aktivert = resultSet[aktivert],
            defaultMottakerEntity = defaultMottakerEntity,
            registrertBrevmottakerEntity = registrertBrevmottakerEntity,
        )
    }

    object RegistrertBrevmottaker : Entity<RegistrertBrevmottakerEntity, UUID, UUID>(
        "tilbakekreving_registrert_brevmottaker",
        RegistrertBrevmottakerEntity::id,
        FieldConverter.UUIDConverter.required(),
    ) {
        val brevmottakerRef = field(
            "brevmottaker_ref",
            RegistrertBrevmottakerEntity::brevmottakerRef,
            FieldConverter.UUIDConverter,
        )

        val parentRef = field(
            "parent_ref",
            RegistrertBrevmottakerEntity::parentRef,
            FieldConverter.UUIDConverter,
        )

        val mottakerType = field(
            "mottaker_type",
            { it.mottakerType },
            FieldConverter.EnumConverter.of<MottakerType>().required(),
        )

        val navn = field(
            "navn",
            { it.navn },
            FieldConverter.StringConverter,
        )

        val personIdent = field(
            "person_ident",
            { it.personIdent },
            FieldConverter.StringConverter,
        )

        val organisasjonsnummer = field(
            "organisasjonsnummer",
            { it.organisasjonsnummer },
            FieldConverter.StringConverter,
        )

        val vergetype = field(
            "vergetype",
            { it.vergetype },
            FieldConverter.EnumConverter.of<Vergetype>(),
        )

        val adresselinje1 = field(
            "adresselinje1",
            { it.manuellAdresseInfoEntity?.adresselinje1 },
            FieldConverter.StringConverter,
        )

        val adresselinje2 = field(
            "adresselinje2",
            { it.manuellAdresseInfoEntity?.adresselinje2 },
            FieldConverter.StringConverter,
        )

        val postnummer = field(
            "postnummer",
            { it.manuellAdresseInfoEntity?.postnummer },
            FieldConverter.StringConverter,
        )

        val poststed = field(
            "poststed",
            { it.manuellAdresseInfoEntity?.poststed },
            FieldConverter.StringConverter,
        )

        val landkode = field(
            "landkode",
            { it.manuellAdresseInfoEntity?.landkode },
            FieldConverter.StringConverter,
        )

        fun map(
            resultSet: ResultSet,
            utenlandskAdresse: RegistrertBrevmottakerEntity?,
            vergeMottaker: RegistrertBrevmottakerEntity?,
            fullmektigMottaker: RegistrertBrevmottakerEntity?,
        ): RegistrertBrevmottakerEntity {
            return RegistrertBrevmottakerEntity(
                id = resultSet[id],
                brevmottakerRef = resultSet[brevmottakerRef],
                parentRef = resultSet[parentRef],
                mottakerType = resultSet[mottakerType],
                navn = resultSet[navn],
                personIdent = resultSet[personIdent],
                organisasjonsnummer = resultSet[organisasjonsnummer],
                vergetype = resultSet[vergetype],
                manuellAdresseInfoEntity = when {
                    resultSet[adresselinje1] != null -> ManuellAdresseInfoEntity(
                        adresselinje1 = resultSet[adresselinje1]!!,
                        adresselinje2 = resultSet[adresselinje2],
                        postnummer = resultSet[postnummer]!!,
                        poststed = resultSet[poststed]!!,
                        landkode = resultSet[landkode]!!,
                    )
                    else -> null
                },
                utenlandskAdresse = utenlandskAdresse,
                verge = vergeMottaker,
                fullmektig = fullmektigMottaker,
            )
        }
    }
}
