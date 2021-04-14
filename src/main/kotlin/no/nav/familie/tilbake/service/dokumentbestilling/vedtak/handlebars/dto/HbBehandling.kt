package no.nav.familie.tilbake.service.dokumentbestilling.vedtak.handlebars.dto

import java.time.LocalDate

class HbBehandling(val erRevurdering: Boolean = false,
                   val originalBehandlingsdatoFagsakvedtak: LocalDate? = null,
                   val erRevurderingEtterKlage: Boolean = false) {

    init {
        if (erRevurdering) {
            requireNotNull(originalBehandlingsdatoFagsakvedtak) { "vedtaksdato for original behandling er ikke satt" }
        }
    }
}