package no.nav.familie.tilbake.service.dokumentbestilling.handlebars.dto

import no.nav.familie.kontrakter.felles.tilbakekreving.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype

open class BaseDokument(open val ytelsestype: Ytelsestype,
                        open val språkkode: Språkkode,
                        open val behandlendeEnhetsNavn: String,
                        open val ansvarligSaksbehandler: String) {

    private val infoMap =
            mapOf(Ytelsestype.BARNETRYGD to Ytelsesinfo("nav.no/barnetrygd",
                                                        mapOf(Språkkode.NB to Ytelsesnavn("barnetrygd", "barnetrygden"),
                                                              Språkkode.NN to Ytelsesnavn("barnetrygd", "barnetrygda"))),
                  Ytelsestype.OVERGANGSSTØNAD to Ytelsesinfo("nav.no/overgangsstonad",
                                                             mapOf(Språkkode.NB to Ytelsesnavn("overgangsstønad",
                                                                                               "overgansgstønaden"),
                                                                   Språkkode.NN to Ytelsesnavn("overgangsstønad",
                                                                                               "overgangsstønaden"))),
                  Ytelsestype.BARNETILSYN to Ytelsesinfo("nav.no/familie/alene-med-barn/barnetilsyn",
                                                         mapOf(Språkkode.NB to Ytelsesnavn("stønad til barnetilsyn",
                                                                                           "stønaden til barnetilsyn"),
                                                               Språkkode.NN to Ytelsesnavn("stønad til barnetilsyn",
                                                                                           "stønaden til barnetilsyn"))),
                  Ytelsestype.SKOLEPENGER to Ytelsesinfo("nav.no/familie/alene-med-barn/skolepenger",
                                                         mapOf(Språkkode.NB to Ytelsesnavn("stønad til skolepenger",
                                                                                           "stønaden til skolepenger"),
                                                               Språkkode.NN to Ytelsesnavn("stønad til skulepengar",
                                                                                           "stønaden til skulepengar"))),
                  Ytelsestype.KONTANTSTØTTE to Ytelsesinfo("nav.no/kontantstotte",
                                                           mapOf(Språkkode.NB to Ytelsesnavn("kontantstøtte", "kontantstøtten"),
                                                                 Språkkode.NN to Ytelsesnavn("kontantstøtte", "kontantstøtta"))))

    private val ytelsesinfo
        get() = infoMap[ytelsestype]
                ?: error("Dokument forsøkt generert for ugyldig ytelsestype: $ytelsestype ")

    private val ytelsesnavn
        get() = ytelsesinfo.navn[språkkode]
                ?: error("Dokument forsøkt generert for ugyldig språkkode: $språkkode ytelse: $ytelsestype")

    @Suppress("unused") // Handlebars
    val ytelsesnavnUbestemt = ytelsesnavn.ubestemt

    @Suppress("unused") // Handlebars
    val ytelsesnavnBestemt = ytelsesnavn.bestemt

    @Suppress("unused") // Handlebars
    val ytelseUrl = ytelsesinfo.url


    private class Ytelsesinfo(val url: String, val navn: Map<Språkkode, Ytelsesnavn>)

    private class Ytelsesnavn(val ubestemt: String, val bestemt: String)

}
