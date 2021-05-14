package no.nav.familie.tilbake.service.dokumentbestilling.vedtak.handlebars.dto

import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsesundertype
import no.nav.familie.tilbake.service.dokumentbestilling.vedtak.handlebars.dto.periode.HbVedtaksbrevsperiode
import java.time.LocalDate

class HbVedtaksbrevDatoer(val opphørsdatoDødSøker: LocalDate? = null,
                          val opphørsdatoDødtBarn: LocalDate? = null,
                          val opphørsdatoIkkeOmsorg: LocalDate? = null) {

    constructor(perioder: List<HbVedtaksbrevsperiode>) : this(getFørsteDagForHendelsesundertype(perioder,
                                                                                                Hendelsesundertype.MOTTAKER_DØD),
                                                              getFørsteDagForHendelsesundertype(perioder,
                                                                                                Hendelsesundertype.BARN_DØD))

    companion object {

        private fun getFørsteDagForHendelsesundertype(perioder: List<HbVedtaksbrevsperiode>,
                                                      vararg hendelsesundertyper: Hendelsesundertype): LocalDate? {
            return perioder.firstOrNull {
                hendelsesundertyper.contains(it.fakta.hendelsesundertype)
            }?.periode?.fom
        }

    }
}
