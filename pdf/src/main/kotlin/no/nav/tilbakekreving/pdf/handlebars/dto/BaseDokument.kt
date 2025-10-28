package no.nav.tilbakekreving.pdf.handlebars.dto

import no.nav.tilbakekreving.FagsystemUtil
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.kontrakter.ytelse.YtelsestypeDTO
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.header.Institusjon

open class BaseDokument(
    val ytelsestype: YtelsestypeDTO,
    override val språkkode: Språkkode,
    val behandlendeEnhetsNavn: String,
    val ansvarligSaksbehandler: String,
    val gjelderDødsfall: Boolean,
    val institusjon: Institusjon? = null,
    private val katalog: YtelseKatalog = DefaultYtelseKatalog,
) : Språkstøtte {
    val avsenderenhet =
        if (FagsystemUtil.hentFagsystemFraYtelsestype(ytelsestype) == FagsystemDTO.EF) {
            "Nav Arbeid og ytelser"
        } else {
            behandlendeEnhetsNavn
        }

    private val ytelsesinfo: Ytelsesinfo
        get() = katalog.infoFor(ytelsestype)

    private val ytelsesnavn: Ytelsesnavn
        get() = ytelsesinfo.navnFor(språkkode)

    @Suppress("unused") // Handlebars
    val ytelsesnavnUbestemt = ytelsesnavn.ubestemt

    @Suppress("unused")
    open // Handlebars
    val ytelsesnavnBestemt = ytelsesnavn.bestemt

    @Suppress("unused") // Handlebars
    val ytelsesnavnEiendomsform = ytelsesnavn.eiendomsform

    @Suppress("unused") // Handlebars
    val ytelseUrl = ytelsesinfo.url
}
