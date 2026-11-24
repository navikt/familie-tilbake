package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.breeeev.mottaker.Addresse
import no.nav.tilbakekreving.breeeev.mottaker.ManuellAddresse
import no.nav.tilbakekreving.breeeev.mottaker.Tilleggsmottaker
import java.util.UUID

data class AddresseEntity(
    val id: UUID,
    val tilbakekrevingRef: String,
    val type: Type?,
    val tilleggsmottakerType: TilleggsmottakerType?,
    val land: String?,
    val addresselinje: String?,
    val postnummer: String?,
    val navn: String?,
    val mottakerIdent: String?,
) {
    internal fun fraEntity(): ManuellAddresse = when (requireNotNull(type)) {
        Type.NorskAddresse -> Addresse.Norsk(
            id = id,
            addresselinje = addresselinje!!,
            postnummer = postnummer!!,
        )
        Type.UtenlandskAddresse -> Addresse.Utenlandsk(
            id = id,
            land = land!!,
            addresselinje = addresselinje!!,
        )
    }

    internal fun fraTilleggsmottakerEntity(): Tilleggsmottaker = when (requireNotNull(tilleggsmottakerType)) {
        TilleggsmottakerType.Verge -> when (type) {
            null -> Tilleggsmottaker.VergeMedFolkeregistrertAddresse(id, mottakerIdent!!)
            else -> Tilleggsmottaker.VergeMedManuellAddresse(navn!!, fraEntity())
        }
        TilleggsmottakerType.Fullmektig -> when (type) {
            null -> when {
                mottakerIdent!!.length == 9 -> Tilleggsmottaker.FullmektigMedOrganisasjonsnummer(id, navn!!, mottakerIdent)
                else -> Tilleggsmottaker.FullmektigPerson(id, mottakerIdent)
            }
            else -> Tilleggsmottaker.FullmektigMedManuellAddresse(navn!!, fraEntity())
        }
    }

    enum class TilleggsmottakerType {
        Verge,
        Fullmektig,
    }

    enum class Type {
        NorskAddresse,
        UtenlandskAddresse,
    }
}
