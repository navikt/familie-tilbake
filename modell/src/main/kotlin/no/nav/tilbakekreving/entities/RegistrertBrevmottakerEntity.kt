package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.RegistrertBrevmottaker
import no.nav.tilbakekreving.kontrakter.brev.ManuellAdresseInfo
import no.nav.tilbakekreving.kontrakter.verge.Vergetype
import java.util.UUID

data class RegistrertBrevmottakerEntity(
    val mottakerType: MottakerType,
    val id: UUID,
    val navn: String?,
    val personIdent: String?,
    val organisasjonsnummer: String?,
    val vergetype: Vergetype?,
    val manuellAdresseInfoEntity: ManuellAdresseInfoEntity?,
    val defaultMottaker: RegistrertBrevmottakerEntity?,
    val utenlandskAdresse: RegistrertBrevmottakerEntity?,
    val verge: RegistrertBrevmottakerEntity?,
    val fullmektig: RegistrertBrevmottakerEntity?,
) {
    fun fraEntity(): RegistrertBrevmottaker = when (mottakerType) {
        MottakerType.DEFAULT_MOTTAKER ->
            RegistrertBrevmottaker.DefaultMottaker(
                id = id,
                navn = requireNotNull(navn) { "navn kreves for DEFAULT_MOTTAKER" },
                personIdent = personIdent,
            )

        MottakerType.UTENLANDSK_ADRESSE_MOTTAKER ->
            RegistrertBrevmottaker.UtenlandskAdresseMottaker(
                id = id,
                navn = requireNotNull(navn) { "navn kreves for UTENLANDSK_ADRESSE_MOTTAKER" },
                manuellAdresseInfo = manuellAdresseInfoEntity?.fraEntity(),
            )

        MottakerType.FULLMEKTIG_MOTTAKER ->
            RegistrertBrevmottaker.FullmektigMottaker(
                id = id,
                navn = requireNotNull(navn) { "navn kreves for FULLMEKTIG_MOTTAKER" },
                organisasjonsnummer = organisasjonsnummer,
                personIdent = personIdent,
                vergeType = requireNotNull(vergetype) { "vergetype kreves for FULLMEKTIG_MOTTAKER" },
                manuellAdresseInfo = manuellAdresseInfoEntity?.fraEntity(),
            )

        MottakerType.VERGE_MOTTAKER ->
            RegistrertBrevmottaker.VergeMottaker(
                id = id,
                navn = requireNotNull(navn) { "navn kreves for VERGE_MOTTAKER" },
                vergetype = requireNotNull(vergetype) { "vergetype kreves for VERGE_MOTTAKER" },
                personIdent = personIdent,
                manuellAdresseInfo = manuellAdresseInfoEntity?.fraEntity(),
            )

        MottakerType.DODSBO_MOTTAKER ->
            RegistrertBrevmottaker.DødsboMottaker(
                id = id,
                navn = requireNotNull(navn) { "navn kreves for DODSBO_MOTTAKER" },
                manuellAdresseInfo = manuellAdresseInfoEntity?.fraEntity(),
            )

        MottakerType.UTENLANDSK_ADRESSE_OG_VERGE_MOTTAKER ->
            RegistrertBrevmottaker.UtenlandskAdresseOgVergeMottaker(
                id = id,
                utenlandskAdresse = requireNotNull(utenlandskAdresse) { "utenlandskAdresse kreves" }.fraEntity() as RegistrertBrevmottaker.UtenlandskAdresseMottaker,
                verge = requireNotNull(verge) { "verge kreves" }.fraEntity() as RegistrertBrevmottaker.VergeMottaker,
            )

        MottakerType.UTENLANDSK_ADRESSE_OG_FULLMEKTIG_MOTTAKER ->
            RegistrertBrevmottaker.UtenlandskAdresseOgFullmektigMottaker(
                id = id,
                utenlandskAdresse = requireNotNull(utenlandskAdresse) { "utenlandskAdresse kreves" }.fraEntity() as RegistrertBrevmottaker.UtenlandskAdresseMottaker,
                fullmektig = requireNotNull(fullmektig) { "fullmektig kreves" }.fraEntity() as RegistrertBrevmottaker.FullmektigMottaker,
            )

        MottakerType.DEFAULT_BRUKER_ADRESSE_OG_VERGE_MOTTAKER ->
            RegistrertBrevmottaker.DefaultBrukerAdresseOgVergeMottaker(
                id = id,
                defaultMottaker = requireNotNull(defaultMottaker) { "defaultMottaker kreves" }.fraEntity() as RegistrertBrevmottaker.DefaultMottaker,
                verge = requireNotNull(verge) { "verge kreves" }.fraEntity() as RegistrertBrevmottaker.VergeMottaker,
            )

        MottakerType.DEFAULT_BRUKER_ADRESSE_OG_FULLMEKTIG_MOTTAKER ->
            RegistrertBrevmottaker.DefaultBrukerAdresseOgFullmektigMottaker(
                id = id,
                defaultMottaker = requireNotNull(defaultMottaker) { "defaultMottaker kreves" }.fraEntity() as RegistrertBrevmottaker.DefaultMottaker,
                fullmektig = requireNotNull(fullmektig) { "fullmektig kreves" }.fraEntity() as RegistrertBrevmottaker.FullmektigMottaker,
            )
    }
}

data class ManuellAdresseInfoEntity(
    val adresselinje1: String,
    val adresselinje2: String?,
    val postnummer: String,
    val poststed: String,
    val landkode: String,
) {
    fun fraEntity(): ManuellAdresseInfo = ManuellAdresseInfo(
        adresselinje1 = adresselinje1,
        adresselinje2 = adresselinje2,
        postnummer = postnummer,
        poststed = poststed,
        landkode = landkode,
    )
}

enum class MottakerType {
    DEFAULT_MOTTAKER,
    UTENLANDSK_ADRESSE_MOTTAKER,
    FULLMEKTIG_MOTTAKER,
    VERGE_MOTTAKER,
    DODSBO_MOTTAKER,
    UTENLANDSK_ADRESSE_OG_VERGE_MOTTAKER,
    UTENLANDSK_ADRESSE_OG_FULLMEKTIG_MOTTAKER,
    DEFAULT_BRUKER_ADRESSE_OG_VERGE_MOTTAKER,
    DEFAULT_BRUKER_ADRESSE_OG_FULLMEKTIG_MOTTAKER,
}
