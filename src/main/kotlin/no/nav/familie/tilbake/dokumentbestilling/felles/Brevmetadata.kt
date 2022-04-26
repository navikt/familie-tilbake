package no.nav.familie.tilbake.dokumentbestilling.felles

import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import no.nav.familie.tilbake.dokumentbestilling.handlebars.dto.Språkstøtte

data class Brevmetadata(val sakspartId: String,
                        val sakspartsnavn: String,
                        val finnesVerge: Boolean = false,
                        val vergenavn: String? = null,
                        val mottageradresse: Adresseinfo,
                        val behandlendeEnhetId: String? = null,
                        val behandlendeEnhetsNavn: String,
                        val ansvarligSaksbehandler: String,
                        val saksnummer: String,
                        override val språkkode: Språkkode,
                        val ytelsestype: Ytelsestype,
                        val behandlingstype: Behandlingstype? = null,
                        val tittel: String? = null) : Språkstøtte
