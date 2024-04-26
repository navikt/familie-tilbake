package no.nav.familie.tilbake.dokumentbestilling.vedtak

import no.nav.familie.tilbake.behandling.domain.Saksbehandlingstype
import no.nav.familie.tilbake.dokumentbestilling.handlebars.FellesTekstformaterer
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevsdata
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.Vedtaksbrevstype

internal object TekstformatererVedtaksbrev {
    fun lagVedtaksbrevsfritekst(vedtaksbrevsdata: HbVedtaksbrevsdata, saksbehandlingstype: Saksbehandlingstype? = null): String {
        if (saksbehandlingstype == Saksbehandlingstype.AUTOMATISK_IKKE_INNKREVING_UNDER_4X_RETTSGEBYR) {
            return lagVedtaksbrev("vedtak/vedtak_feilutbetaling_under_4x_rettsgebyr", vedtaksbrevsdata)
        }
        return when (vedtaksbrevsdata.felles.vedtaksbrevstype) {
            Vedtaksbrevstype.FRITEKST_FEILUTBETALING_BORTFALT ->
                lagVedtaksbrev("vedtak/fritekstFeilutbetalingBortfalt/fritekstFeilutbetalingBortfalt", vedtaksbrevsdata)
            Vedtaksbrevstype.ORDINÆR -> lagVedtaksbrev("vedtak/vedtak", vedtaksbrevsdata)
        }
    }

    private fun lagVedtaksbrev(
        mal: String,
        vedtaksbrevsdata: HbVedtaksbrevsdata,
    ): String {
        return FellesTekstformaterer.lagBrevtekst(vedtaksbrevsdata, mal)
    }

    fun lagVedtaksbrevsvedleggHtml(vedtaksbrevsdata: HbVedtaksbrevsdata): String {
        return FellesTekstformaterer.lagBrevtekst(vedtaksbrevsdata, "vedtak/vedlegg")
    }

    fun lagVedtaksbrevsoverskrift(vedtaksbrevsdata: HbVedtaksbrevsdata): String {
        return FellesTekstformaterer.lagBrevtekst(vedtaksbrevsdata, "vedtak/vedtak_overskrift")
    }
}
