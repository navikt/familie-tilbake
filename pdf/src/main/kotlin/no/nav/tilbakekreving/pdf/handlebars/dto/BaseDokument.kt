package no.nav.tilbakekreving.pdf.handlebars.dto

import no.nav.tilbakekreving.FagsystemUtil
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.kontrakter.ytelse.Fagsystem
import no.nav.tilbakekreving.kontrakter.ytelse.Ytelsestype
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.header.Institusjon

private const val EF_URL = "nav.no/alene-med-barn"

open class BaseDokument(
    val ytelsestype: Ytelsestype,
    override val språkkode: Språkkode,
    val behandlendeEnhetsNavn: String,
    val ansvarligSaksbehandler: String,
    val gjelderDødsfall: Boolean,
    val institusjon: Institusjon? = null,
) : Språkstøtte {
    val avsenderenhet =
        if (FagsystemUtil.hentFagsystemFraYtelsestype(ytelsestype) == Fagsystem.EF) {
            "Nav Arbeid og ytelser"
        } else {
            behandlendeEnhetsNavn
        }
    private val infoMap =
        mapOf(
            Ytelsestype.BARNETRYGD to
                Ytelsesinfo(
                    "nav.no/barnetrygd",
                    mapOf(
                        Språkkode.NB to
                            Ytelsesnavn(
                                "barnetrygd",
                                "barnetrygden",
                                "barnetrygden din",
                            ),
                        Språkkode.NN to
                            Ytelsesnavn(
                                "barnetrygd",
                                "barnetrygda",
                                "barnetrygda di",
                            ),
                    ),
                ),
            Ytelsestype.OVERGANGSSTØNAD to
                Ytelsesinfo(
                    EF_URL,
                    mapOf(
                        Språkkode.NB to
                            Ytelsesnavn(
                                "overgangsstønad",
                                "overgangsstønaden",
                                "overgangsstønaden din",
                            ),
                        Språkkode.NN to
                            Ytelsesnavn(
                                "overgangsstønad",
                                "overgangsstønaden",
                                "overgangsstønaden din",
                            ),
                    ),
                ),
            Ytelsestype.BARNETILSYN to
                Ytelsesinfo(
                    EF_URL,
                    mapOf(
                        Språkkode.NB to
                            Ytelsesnavn(
                                "stønad til barnetilsyn",
                                "stønaden til barnetilsyn",
                                "stønaden din til barnetilsyn",
                            ),
                        Språkkode.NN to
                            Ytelsesnavn(
                                "stønad til barnetilsyn",
                                "stønaden til barnetilsyn",
                                "stønaden din til barnetilsyn",
                            ),
                    ),
                ),
            Ytelsestype.SKOLEPENGER to
                Ytelsesinfo(
                    EF_URL,
                    mapOf(
                        Språkkode.NB to
                            Ytelsesnavn(
                                "stønad til skolepenger",
                                "stønaden til skolepenger",
                                "stønaden din til skolepenger",
                            ),
                        Språkkode.NN to
                            Ytelsesnavn(
                                "stønad til skulepengar",
                                "stønaden til skulepengar",
                                "stønaden din til skulepengar",
                            ),
                    ),
                ),
            Ytelsestype.KONTANTSTØTTE to
                Ytelsesinfo(
                    "nav.no/kontantstotte",
                    mapOf(
                        Språkkode.NB to
                            Ytelsesnavn(
                                "kontantstøtte",
                                "kontantstøtten",
                                "kontantstøtten din",
                            ),
                        Språkkode.NN to
                            Ytelsesnavn(
                                "kontantstøtte",
                                "kontantstøtta",
                                "kontantstøtta di",
                            ),
                    ),
                ),
        )

    private val ytelsesinfo
        get() =
            infoMap[ytelsestype]
                ?: error("Dokument forsøkt generert for ugyldig ytelsestype: $ytelsestype ")

    private val ytelsesnavn
        get() =
            ytelsesinfo.navn[språkkode]
                ?: error("Dokument forsøkt generert for ugyldig språkkode: $språkkode ytelse: $ytelsestype")

    @Suppress("unused") // Handlebars
    val ytelsesnavnUbestemt = ytelsesnavn.ubestemt

    @Suppress("unused")
    open // Handlebars
    val ytelsesnavnBestemt = ytelsesnavn.bestemt

    @Suppress("unused") // Handlebars
    val ytelsesnavnEiendomsform = ytelsesnavn.eiendomsform

    @Suppress("unused") // Handlebars
    val ytelseUrl = ytelsesinfo.url

    private class Ytelsesinfo(
        val url: String,
        val navn: Map<Språkkode, Ytelsesnavn>,
    )

    private class Ytelsesnavn(
        val ubestemt: String,
        val bestemt: String,
        val eiendomsform: String,
    )
}
