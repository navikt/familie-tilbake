package no.nav.tilbakekreving.kontrakter.tilbakekreving.v1

import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import no.nav.tilbakekreving.kontrakter.Fagsystem
import no.nav.tilbakekreving.kontrakter.Regelverk
import no.nav.tilbakekreving.kontrakter.Språkkode
import java.time.LocalDate
import no.nav.tilbakekreving.kontrakter.tilbakekreving.Behandlingstype
import no.nav.tilbakekreving.kontrakter.tilbakekreving.Brevmottaker
import no.nav.tilbakekreving.kontrakter.tilbakekreving.Faktainfo
import no.nav.tilbakekreving.kontrakter.tilbakekreving.Institusjon
import no.nav.tilbakekreving.kontrakter.tilbakekreving.Varsel
import no.nav.tilbakekreving.kontrakter.tilbakekreving.Verge
import no.nav.tilbakekreving.kontrakter.tilbakekreving.Ytelsestype

data class OpprettTilbakekrevingRequest(
    val fagsystem: Fagsystem,
    val regelverk: Regelverk? = null,
    val ytelsestype: Ytelsestype,
    val eksternFagsakId: String,
    @field:Pattern(regexp = "(^$|.{11})", message = "PersonIdent er ikke riktig")
    val personIdent: String,
    // Fagsystemreferanse til behandlingen, må være samme id som brukes mot datavarehus og økonomi
    val eksternId: String,
    val behandlingstype: Behandlingstype? = Behandlingstype.TILBAKEKREVING,
    val manueltOpprettet: Boolean,
    val språkkode: Språkkode = Språkkode.NB,
    val enhetId: String,
    val enhetsnavn: String,
    val saksbehandlerIdent: String,
    @field:Valid
    val varsel: Varsel?,
    val revurderingsvedtaksdato: LocalDate,
    @field:Valid
    val verge: Verge? = null,
    @field:Valid
    val faktainfo: Faktainfo,
    @field:Valid
    val institusjon: Institusjon? = null,
    @field:Valid
    val manuelleBrevmottakere: Set<Brevmottaker> = emptySet(),
    val begrunnelseForTilbakekreving: String?,
) {
    init {
        if (manueltOpprettet) {
            require(varsel == null) { "Kan ikke opprette manuelt behandling med varsel" }
        }
    }
}
