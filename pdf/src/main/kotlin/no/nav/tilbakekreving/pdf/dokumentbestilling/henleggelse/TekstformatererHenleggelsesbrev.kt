package no.nav.tilbakekreving.pdf.dokumentbestilling.henleggelse

import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmetadata
import no.nav.tilbakekreving.pdf.dokumentbestilling.henleggelse.handlebars.dto.Henleggelsesbrevsdokument
import no.nav.tilbakekreving.pdf.handlebars.FellesTekstformaterer
import no.nav.tilbakekreving.pdf.handlebars.dto.Brevoverskriftsdata

object TekstformatererHenleggelsesbrev {
    fun lagFritekst(dokument: Henleggelsesbrevsdokument): String = FellesTekstformaterer.lagBrevtekst(dokument, "henleggelse/henleggelse")

    fun lagOverskrift(
        brevmetadata: Brevmetadata,
    ): String = FellesTekstformaterer.lagBrevtekst(Brevoverskriftsdata(brevmetadata), "henleggelse/henleggelse_overskrift")

    fun lagRevurderingsfritekst(dokument: Henleggelsesbrevsdokument): String = FellesTekstformaterer.lagBrevtekst(dokument, "henleggelse/henleggelse_revurdering")

    fun lagRevurderingsoverskrift(brevmetadata: Brevmetadata): String = FellesTekstformaterer.lagBrevtekst(
        Brevoverskriftsdata(brevmetadata),
        "henleggelse/henleggelse_revurdering_overskrift",
    )
}
