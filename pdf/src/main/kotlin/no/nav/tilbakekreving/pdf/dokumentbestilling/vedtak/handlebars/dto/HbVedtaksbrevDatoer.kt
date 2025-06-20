package no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto

import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsesundertype
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.periode.HbVedtaksbrevsperiode
import java.time.LocalDate

class HbVedtaksbrevDatoer(
    val opphørsdatoDødSøker: LocalDate? = null,
    val opphørsdatoDødtBarn: LocalDate? = null,
    val opphørsdatoIkkeOmsorg: LocalDate? = null,
) {
    constructor(perioder: List<HbVedtaksbrevsperiode>) : this(
        getFørsteDagForHendelsesundertype(
            perioder,
            Hendelsesundertype.BRUKER_DØD,
        ),
        getFørsteDagForHendelsesundertype(
            perioder,
            Hendelsesundertype.BARN_DØD,
        ),
    )

    companion object {
        private fun getFørsteDagForHendelsesundertype(
            perioder: List<HbVedtaksbrevsperiode>,
            vararg hendelsesundertyper: Hendelsesundertype,
        ): LocalDate? =
            perioder
                .firstOrNull {
                    hendelsesundertyper.contains(it.fakta.hendelsesundertype)
                }?.periode
                ?.fom
    }
}
