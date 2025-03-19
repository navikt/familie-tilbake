package no.nav.tilbakekreving

import no.nav.tilbakekreving.api.v2.BrukerDto
import no.nav.tilbakekreving.api.v2.EksternFagsakDto
import no.nav.tilbakekreving.api.v2.OpprettTilbakekrevingEvent
import no.nav.tilbakekreving.api.v2.Opprettelsevalg
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.kontrakter.ytelse.Fagsystem
import no.nav.tilbakekreving.kontrakter.ytelse.Ytelsestype

fun eksternFagsak() =
    EksternFagsakDto(
        eksternId = "101010",
        ytelsestype = Ytelsestype.BARNETRYGD,
        fagsystem = Fagsystem.BA,
    )

fun bruker() =
    BrukerDto(
        ident = "31126900011",
        språkkode = Språkkode.NB,
    )

fun opprettTilbakekrevingEvent(
    eksternFagsak: EksternFagsakDto = eksternFagsak(),
    opprettelsevalg: Opprettelsevalg = Opprettelsevalg.OPPRETT_BEHANDLING_MED_VARSEL,
) = OpprettTilbakekrevingEvent(
    eksternFagsak = eksternFagsak,
    opprettelsesvalg = opprettelsevalg,
)
