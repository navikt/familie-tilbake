package no.nav.tilbakekreving.brev

import no.nav.tilbakekreving.aktør.Brukerinfo
import no.nav.tilbakekreving.behandling.Forhåndsvarselinfo
import no.nav.tilbakekreving.kontrakter.ytelse.YtelsestypeDTO

data class VarselbrevInfo(
    val brukerinfo: Brukerinfo,
    val forhåndsvarselinfo: Forhåndsvarselinfo,
    val eksternFagsakId: String,
    val ytelseType: YtelsestypeDTO,
)
