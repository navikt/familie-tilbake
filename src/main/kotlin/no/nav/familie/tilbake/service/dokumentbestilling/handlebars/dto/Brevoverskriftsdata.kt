package no.nav.familie.tilbake.service.dokumentbestilling.handlebars.dto

import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmetadata

data class Brevoverskriftsdata(val brevmetadata: Brevmetadata) : BaseDokument(brevmetadata.ytelsestype,
                                                                              brevmetadata.språkkode,
                                                                              brevmetadata.behandlendeEnhetsNavn,
                                                                              brevmetadata.ansvarligSaksbehandler)
