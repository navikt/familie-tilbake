package no.nav.familie.tilbake.micrometer.domain

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import java.time.LocalDate

class VedtakPerDato(val dato: LocalDate,
                    val ytelsestype: Ytelsestype,
                    val vedtakstype: Behandlingsresultatstype,
                    val antall: Int)
