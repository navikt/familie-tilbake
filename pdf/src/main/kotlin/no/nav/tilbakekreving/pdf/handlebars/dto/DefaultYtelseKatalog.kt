package no.nav.tilbakekreving.pdf.handlebars.dto

import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.kontrakter.ytelse.YtelsestypeDTO

object DefaultYtelseKatalog : YtelseKatalog {
    private const val EF_URL = "nav.no/alene-med-barn"
    private val dataProviders: Map<YtelsestypeDTO, () -> Ytelsesinfo> =
        YtelsestypeDTO.entries.associateWith { yt ->
            {
                when (yt) {
                    YtelsestypeDTO.BARNETRYGD -> Ytelsesinfo(
                        url = "nav.no/barnetrygd",
                        navn = mapOf(
                            Språkkode.NB to Ytelsesnavn("barnetrygd", "barnetrygden", "barnetrygden din"),
                            Språkkode.NN to Ytelsesnavn("barnetrygd", "barnetrygda", "barnetrygda di"),
                        ),
                    )
                    YtelsestypeDTO.OVERGANGSSTØNAD -> Ytelsesinfo(
                        url = EF_URL,
                        navn = mapOf(
                            Språkkode.NB to Ytelsesnavn("overgangsstønad", "overgangsstønaden", "overgangsstønaden din"),
                            Språkkode.NN to Ytelsesnavn("overgangsstønad", "overgangsstønaden", "overgangsstønaden din"),
                        ),
                    )
                    YtelsestypeDTO.BARNETILSYN -> Ytelsesinfo(
                        url = EF_URL,
                        navn = mapOf(
                            Språkkode.NB to Ytelsesnavn("stønad til barnetilsyn", "stønaden til barnetilsyn", "stønaden din til barnetilsyn"),
                            Språkkode.NN to Ytelsesnavn("stønad til barnetilsyn", "stønaden til barnetilsyn", "stønaden din til barnetilsyn"),
                        ),
                    )
                    YtelsestypeDTO.SKOLEPENGER -> Ytelsesinfo(
                        url = EF_URL,
                        navn = mapOf(
                            Språkkode.NB to Ytelsesnavn("stønad til skolepenger", "stønaden til skolepenger", "stønaden din til skolepenger"),
                            Språkkode.NN to Ytelsesnavn("stønad til skulepengar", "stønaden til skulepengar", "stønaden din til skulepengar"),
                        ),
                    )
                    YtelsestypeDTO.KONTANTSTØTTE -> Ytelsesinfo(
                        url = "nav.no/kontantstotte",
                        navn = mapOf(
                            Språkkode.NB to Ytelsesnavn("kontantstøtte", "kontantstøtten", "kontantstøtten din"),
                            Språkkode.NN to Ytelsesnavn("kontantstøtte", "kontantstøtta", "kontantstøtta di"),
                        ),
                    )
                    YtelsestypeDTO.TILLEGGSSTØNAD -> Ytelsesinfo(
                        url = "nav.no/tilleggsstonad",
                        navn = mapOf(
                            Språkkode.NB to Ytelsesnavn("tilleggsstønad", "tilleggsstønad", "tilleggsstønaden din"),
                            Språkkode.NN to Ytelsesnavn("tilleggsstønad", "tilleggsstønad", "tilleggsstønaden din"),
                        ),
                    )
                    YtelsestypeDTO.ARBEIDSAVKLARINGSPENGER -> Ytelsesinfo(
                        url = "nav.no/arbeidsavklaringspenger",
                        navn = mapOf(
                            Språkkode.NB to Ytelsesnavn("arbeidsavklaringspenger", "arbeidsavklaringspengene", "arbeidsavklaringspengene dine"),
                            Språkkode.NN to Ytelsesnavn("arbeidsavklaringspengar", "arbeidsavklaringspengane", "arbeidsavklaringspengane dine"),
                        ),
                    )
                    YtelsestypeDTO.INFOTRYGD -> error("INFOTRYGD støttes ikke.")
                }
            }
        }

    override fun infoFor(type: YtelsestypeDTO): Ytelsesinfo =
        dataProviders.getValue(type).invoke()
}
