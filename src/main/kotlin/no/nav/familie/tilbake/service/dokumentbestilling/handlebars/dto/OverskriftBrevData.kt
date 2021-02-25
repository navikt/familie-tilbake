package no.nav.familie.tilbake.service.dokumentbestilling.handlebars.dto

import no.nav.familie.kontrakter.felles.tilbakekreving.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevMetadata

data class OverskriftBrevData(val brevMetadata: BrevMetadata): BaseDokument(brevMetadata.ytelsestype,
                                                                       brevMetadata.språkkode,
                                                                       brevMetadata.behandlendeEnhetNavn,
                                                                       brevMetadata.ansvarligSaksbehandler)
