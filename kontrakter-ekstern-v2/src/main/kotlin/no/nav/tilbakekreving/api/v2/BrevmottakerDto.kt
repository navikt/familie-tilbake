package no.nav.tilbakekreving.api.v2

import no.nav.tilbakekreving.kontrakter.brev.ManuellAdresseInfo
import no.nav.tilbakekreving.kontrakter.brev.MottakerType
import no.nav.tilbakekreving.kontrakter.verge.Vergetype

data class BrevmottakerDto(
    val type: MottakerType,
    val vergetype: Vergetype? = null,
    val navn: String,
    val organisasjonsnummer: String? = null,
    val personIdent: String? = null,
    val manuellAdresseInfo: ManuellAdresseInfo? = null,
)
