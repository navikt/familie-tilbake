package no.nav.tilbakekreving.kontrakter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.validation.constraints.Size
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.kontrakter.verge.Verge
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.kontrakter.ytelse.YtelsestypeDTO
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class ForhåndsvisVarselbrevRequest(
    @Size(max = 1500, message = "Varseltekst er for lang")
    val varseltekst: String? = null,
    val ytelsestype: YtelsestypeDTO,
    val behandlendeEnhetId: String? = null,
    val behandlendeEnhetsNavn: String,
    val saksbehandlerIdent: String? = null,
    val språkkode: Språkkode = Språkkode.NB,
    val vedtaksdato: LocalDate? = null,
    val feilutbetaltePerioderDto: FeilutbetaltePerioderDto,
    val fagsystem: FagsystemDTO,
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
