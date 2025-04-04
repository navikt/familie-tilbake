package no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker

import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.domene.ManuellBrevmottaker
import no.nav.tilbakekreving.api.v1.dto.ManuellBrevmottakerRequestDto
import no.nav.tilbakekreving.api.v1.dto.ManuellBrevmottakerResponsDto
import no.nav.tilbakekreving.kontrakter.brev.Brevmottaker
import no.nav.tilbakekreving.kontrakter.brev.ManuellAdresseInfo
import java.util.UUID

object ManuellBrevmottakerMapper {
    fun tilDomene(
        behandlingId: UUID,
        manuellBrevmottakerRequestDto: ManuellBrevmottakerRequestDto,
        navnFraRegister: String?,
    ) = ManuellBrevmottaker(
        behandlingId = behandlingId,
        type = manuellBrevmottakerRequestDto.type,
        navn = navnFraRegister ?: manuellBrevmottakerRequestDto.navn,
        ident = manuellBrevmottakerRequestDto.personIdent,
        orgNr = manuellBrevmottakerRequestDto.organisasjonsnummer,
        adresselinje1 = manuellBrevmottakerRequestDto.manuellAdresseInfo?.adresselinje1,
        adresselinje2 = manuellBrevmottakerRequestDto.manuellAdresseInfo?.adresselinje2,
        postnummer = manuellBrevmottakerRequestDto.manuellAdresseInfo?.postnummer?.trim(),
        poststed = manuellBrevmottakerRequestDto.manuellAdresseInfo?.poststed?.trim(),
        landkode = manuellBrevmottakerRequestDto.manuellAdresseInfo?.landkode,
        vergetype = manuellBrevmottakerRequestDto.vergetype,
    )

    fun tilDomene(
        brevmottaker: Brevmottaker,
        behandlingId: UUID,
    ) = ManuellBrevmottaker(
        behandlingId = behandlingId,
        type = brevmottaker.type,
        ident = brevmottaker.personIdent,
        orgNr = brevmottaker.organisasjonsnummer,
        adresselinje1 = brevmottaker.manuellAdresseInfo?.adresselinje1,
        adresselinje2 = brevmottaker.manuellAdresseInfo?.adresselinje2,
        postnummer = brevmottaker.manuellAdresseInfo?.postnummer,
        poststed = brevmottaker.manuellAdresseInfo?.poststed,
        landkode = brevmottaker.manuellAdresseInfo?.landkode,
        navn = brevmottaker.navn,
        vergetype = brevmottaker.vergetype,
    )

    fun tilRespons(manuellBrevmottaker: ManuellBrevmottaker) =
        ManuellBrevmottakerResponsDto(
            id = manuellBrevmottaker.id,
            brevmottaker =
                Brevmottaker(
                    type = manuellBrevmottaker.type,
                    navn = manuellBrevmottaker.navn,
                    personIdent = manuellBrevmottaker.ident,
                    organisasjonsnummer = manuellBrevmottaker.orgNr,
                    manuellAdresseInfo =
                        if (manuellBrevmottaker.hasManuellAdresse()) {
                            ManuellAdresseInfo(
                                adresselinje1 = manuellBrevmottaker.adresselinje1!!,
                                adresselinje2 = manuellBrevmottaker.adresselinje2,
                                postnummer = manuellBrevmottaker.postnummer!!,
                                poststed = manuellBrevmottaker.poststed!!,
                                landkode = manuellBrevmottaker.landkode!!,
                            )
                        } else {
                            null
                        },
                    vergetype = manuellBrevmottaker.vergetype,
                ),
        )
}
