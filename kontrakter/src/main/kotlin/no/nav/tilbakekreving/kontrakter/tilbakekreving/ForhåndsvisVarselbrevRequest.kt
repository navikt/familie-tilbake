package no.nav.tilbakekreving.kontrakter.tilbakekreving

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.validation.constraints.Size
import no.nav.tilbakekreving.kontrakter.Fagsystem
import no.nav.tilbakekreving.kontrakter.Språkkode
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class ForhåndsvisVarselbrevRequest(
    @Size(max = 1500, message = "Varseltekst er for lang")
    val varseltekst: String? = null,
    val ytelsestype: Ytelsestype,
    val behandlendeEnhetId: String? = null,
    val behandlendeEnhetsNavn: String,
    val saksbehandlerIdent: String? = null,
    val språkkode: Språkkode = Språkkode.NB,
    val vedtaksdato: LocalDate? = null,
    val feilutbetaltePerioderDto: FeilutbetaltePerioderDto,
    val fagsystem: Fagsystem,
    val eksternFagsakId: String,
    val ident: String,
    val verge: Verge? = null,
    val fagsystemsbehandlingId: String? = null,
    val institusjon: Institusjon? = null,
)

data class FeilutbetaltePerioderDto(
    val sumFeilutbetaling: Long,
    val perioder: List<Periode>,
)
