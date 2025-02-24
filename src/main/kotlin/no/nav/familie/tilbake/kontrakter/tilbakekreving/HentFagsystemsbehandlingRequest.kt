package no.nav.familie.tilbake.kontrakter.tilbakekreving

import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import no.nav.familie.tilbake.kontrakter.Regelverk
import no.nav.familie.tilbake.kontrakter.Språkkode
import java.time.LocalDate

data class HentFagsystemsbehandlingRequest(
    val eksternFagsakId: String,
    val ytelsestype: Ytelsestype,
    val eksternId: String,
)

data class HentFagsystemsbehandlingRespons(
    val feilMelding: String? = null,
    val hentFagsystemsbehandling: HentFagsystemsbehandling? = null,
)

data class HentFagsystemsbehandling(
    val eksternFagsakId: String,
    val ytelsestype: Ytelsestype,
    val regelverk: Regelverk? = null,
    val eksternId: String,
    val personIdent: String,
    val språkkode: Språkkode = Språkkode.NB,
    val enhetId: String,
    val enhetsnavn: String,
    @field:Pattern(regexp = "(^$|.{11})", message = "PersonIdent er ikke riktig")
    val revurderingsvedtaksdato: LocalDate,
    @field:Valid
    val verge: Verge? = null,
    @field:Valid
    val faktainfo: Faktainfo,
    @field:Valid
    val institusjon: Institusjon? = null,
)
