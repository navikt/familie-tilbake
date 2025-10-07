package no.nav.tilbakekreving.entities.mapper

import no.nav.tilbakekreving.entities.ManuellAdresseInfoEntity
import no.nav.tilbakekreving.kontrakter.brev.ManuellAdresseInfo

fun tilManuellAdresseInfoEntity(src: ManuellAdresseInfo): ManuellAdresseInfoEntity =
    ManuellAdresseInfoEntity(
        adresselinje1 = src.adresselinje1,
        adresselinje2 = src.adresselinje2,
        postnummer = src.postnummer,
        poststed = src.poststed,
        landkode = src.landkode,
    )

fun fraManuellAdresseInfoEntity(src: ManuellAdresseInfoEntity): ManuellAdresseInfo =
    ManuellAdresseInfo(
        adresselinje1 = src.adresselinje1,
        adresselinje2 = src.adresselinje2,
        postnummer = src.postnummer,
        poststed = src.poststed,
        landkode = src.landkode,
    )
