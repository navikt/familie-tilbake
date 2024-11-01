package no.nav.familie.tilbake.dokumentbestilling.vedtak

import no.nav.familie.tilbake.dokumentbestilling.handlebars.FellesTekstformaterer
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevsdata
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.Vedtaksbrevstype

internal object TekstformatererVedtaksbrev {
    fun lagVedtaksbrevsfritekst(
        vedtaksbrevsdata: HbVedtaksbrevsdata,
        erPerioderSammenslått: Boolean = false,
    ): String =
        when (vedtaksbrevsdata.felles.vedtaksbrevstype) {
            Vedtaksbrevstype.FRITEKST_FEILUTBETALING_BORTFALT ->
                lagVedtaksbrev("vedtak/fritekstFeilutbetalingBortfalt/fritekstFeilutbetalingBortfalt", vedtaksbrevsdata)
            Vedtaksbrevstype.ORDINÆR ->
                if (erPerioderSammenslått) {
                    lagVedtaksbrev("vedtak/vedtak_sammenslå_perioder", vedtaksbrevsdata)
                } else {
                    lagVedtaksbrev("vedtak/vedtak", vedtaksbrevsdata)
                }
            Vedtaksbrevstype.AUTOMATISK_4X_RETTSGEBYR -> lagVedtaksbrev("vedtak/vedtak_feilutbetaling_under_4x_rettsgebyr", vedtaksbrevsdata)
        }

    private fun lagVedtaksbrev(
        mal: String,
        vedtaksbrevsdata: HbVedtaksbrevsdata,
    ): String = FellesTekstformaterer.lagBrevtekst(vedtaksbrevsdata, mal)

    fun lagVedtaksbrevsvedleggHtml(vedtaksbrevsdata: HbVedtaksbrevsdata): String = FellesTekstformaterer.lagBrevtekst(vedtaksbrevsdata, "vedtak/vedlegg")

    fun lagVedtaksbrevsoverskrift(vedtaksbrevsdata: HbVedtaksbrevsdata): String = FellesTekstformaterer.lagBrevtekst(vedtaksbrevsdata, "vedtak/vedtak_overskrift")
}
