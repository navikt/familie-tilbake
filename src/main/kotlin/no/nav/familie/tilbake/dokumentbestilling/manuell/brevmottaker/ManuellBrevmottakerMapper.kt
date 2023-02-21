package no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker

import no.nav.familie.tilbake.api.dto.ManuellBrevmottakerDto
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.domene.ManuellBrevmottaker
import java.util.UUID

object ManuellBrevmottakerMapper {

    fun tilDomene(behandlingId: UUID, manuellBrevmottakerDto: ManuellBrevmottakerDto) =
        ManuellBrevmottaker(
            behandlingId = behandlingId,
            type = manuellBrevmottakerDto.type,
            navn = manuellBrevmottakerDto.navn,
            adresselinje1 = manuellBrevmottakerDto.adresselinje1,
            adresselinje2 = manuellBrevmottakerDto.adresselinje2,
            postnummer = manuellBrevmottakerDto.postnummer.trim(),
            poststed = manuellBrevmottakerDto.poststed.trim(),
            landkode = manuellBrevmottakerDto.landkode
        )

    fun tilRespons(manuellBrevmottaker: ManuellBrevmottaker) = ManuellBrevmottakerDto(
        id = manuellBrevmottaker.id,
        type = manuellBrevmottaker.type,
        navn = manuellBrevmottaker.navn,
        adresselinje1 = manuellBrevmottaker.adresselinje1,
        adresselinje2 = manuellBrevmottaker.adresselinje2,
        postnummer = manuellBrevmottaker.postnummer,
        poststed = manuellBrevmottaker.poststed,
        landkode = manuellBrevmottaker.landkode
    )
}
