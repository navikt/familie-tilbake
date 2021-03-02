package no.nav.familie.tilbake.service.dokumentbestilling.felles

import no.nav.familie.kontrakter.felles.tilbakekreving.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.behandling.domain.Behandlingstype

data class Brevmetadata(var sakspartId: String,
                        val sakspartsnavn: String,
                        val finnesVerge: Boolean = false,
                        val vergenavn: String? = null,
                        val mottageradresse: Adresseinfo,
                        val behandlendeEnhetId: String? = null,
                        val behandlendeEnhetsNavn: String,
                        val ansvarligSaksbehandler: String,
                        val saksnummer: String? = null,
                        val språkkode: Språkkode,
                        val ytelsestype: Ytelsestype,
                        val behandlingstype: Behandlingstype? = null,
                        val tittel: String? = null)
