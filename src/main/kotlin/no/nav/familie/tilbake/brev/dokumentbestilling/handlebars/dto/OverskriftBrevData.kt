package no.nav.familie.tilbake.brev.dokumentbestilling.handlebars.dto

import no.nav.familie.tilbake.brev.dokumentbestilling.felles.BrevMetadata

data class OverskriftBrevData(val brevMetadata: BrevMetadata): BaseDokument(brevMetadata.ytelsestype,
                                                                       brevMetadata.språkkode,
                                                                       brevMetadata.behandlendeEnhetNavn,
                                                                       brevMetadata.ansvarligSaksbehandler)
