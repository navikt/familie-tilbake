package no.nav.tilbakekreving.brev

import no.nav.tilbakekreving.aktør.Brukerinfo
import no.nav.tilbakekreving.behandling.Forhåndsvarselinfo
import no.nav.tilbakekreving.breeeev.standardtekster.HjemmelForTilbakekreving
import no.nav.tilbakekreving.kontrakter.ytelse.YtelsestypeDTO
import java.time.LocalDate
import java.util.UUID

data class VarselbrevInfo(
    val id: UUID,
    val brukerinfo: Brukerinfo,
    val forhåndsvarselinfo: Forhåndsvarselinfo,
    val eksternFagsakId: String,
    val ytelseType: YtelsestypeDTO,
    val hjemlerForTilbakekreving: List<HjemmelForTilbakekreving>,
    val varsletDato: LocalDate,
    val opprinneligUttalelsesfrist: LocalDate,
    val tekstFraSaksbehandler: String,
)
