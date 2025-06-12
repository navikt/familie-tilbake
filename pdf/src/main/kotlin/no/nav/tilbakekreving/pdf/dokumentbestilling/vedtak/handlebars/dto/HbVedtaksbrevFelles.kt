package no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto

import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.HarBrukerUttaltSeg
import no.nav.tilbakekreving.kontrakter.ytelse.YtelsestypeDTO
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmetadata
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.getAnnenMottagersNavn
import no.nav.tilbakekreving.pdf.handlebars.dto.BaseDokument
import java.math.BigDecimal
import java.time.LocalDate

data class HbVedtaksbrevFelles(
    val brevmetadata: Brevmetadata,
    val søker: HbPerson,
    val fagsaksvedtaksdato: LocalDate,
    val varsel: HbVarsel? = null,
    val totalresultat: HbTotalresultat,
    val hjemmel: HbHjemmel,
    val konfigurasjon: HbKonfigurasjon,
    val fritekstoppsummering: String? = null,
    val vedtaksbrevstype: Vedtaksbrevstype,
    val ansvarligBeslutter: String? = null,
    val behandling: HbBehandling,
    val erFeilutbetaltBeløpKorrigertNed: Boolean = false,
    val totaltFeilutbetaltBeløp: BigDecimal,
    val datoer: HbVedtaksbrevDatoer? = null,
    val harBrukerUttaltSeg: HarBrukerUttaltSeg,
) : BaseDokument(
        brevmetadata.ytelsestype,
        brevmetadata.språkkode,
        brevmetadata.behandlendeEnhetsNavn,
        brevmetadata.ansvarligSaksbehandler,
        brevmetadata.gjelderDødsfall,
        brevmetadata.institusjon,
    ) {
    @Suppress("unused") // Handlebars
    val opphørsdatoDødSøker = datoer?.opphørsdatoDødSøker

    @Suppress("unused") // Handlebars
    val opphørsdatoDødtBarn = datoer?.opphørsdatoDødtBarn

    @Suppress("unused") // Handlebars
    val opphørsdatoIkkeOmsorg = datoer?.opphørsdatoIkkeOmsorg

    val annenMottagersNavn: String? = getAnnenMottagersNavn(brevmetadata)

    @Suppress("unused") // Handlebars
    val skattepliktig = YtelsestypeDTO.OVERGANGSSTØNAD == brevmetadata.ytelsestype

    @Suppress("unused") // Handlebars
    val isSkalIkkeViseSkatt = YtelsestypeDTO.OVERGANGSSTØNAD != brevmetadata.ytelsestype || !totalresultat.harSkattetrekk

    val harVedlegg = vedtaksbrevstype == Vedtaksbrevstype.ORDINÆR
    val hovedresultat = totalresultat.hovedresultat
}
