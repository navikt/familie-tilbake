package no.nav.familie.tilbake.service.dokumentbestilling.felles

import no.nav.familie.kontrakter.felles.tilbakekreving.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.behandling.domain.Behandlingstype

data class BrevMetadata(var sakspartId: String,
                        val sakspartNavn: String,
                        val finnesVerge: Boolean = false,
                        val vergeNavn: String? = null,
                        val mottakerAdresse: Adresseinfo,
                        val behandlendeEnhetId: String? = null,
                        val behandlendeEnhetNavn: String,
                        val ansvarligSaksbehandler: String,
                        val saksnummer: String? = null,
                        val språkkode: Språkkode,
                        val ytelsestype: Ytelsestype,
                        val behandlingType: Behandlingstype? = null,
                        val tittel: String? = null)
