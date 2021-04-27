package no.nav.familie.tilbake.api.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.tilbakekreving.Periode
import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Verge
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import java.time.LocalDate
import javax.validation.constraints.Size

@JsonIgnoreProperties(ignoreUnknown = true)
class ForhåndsvisVarselbrevRequest(@Size(max = 1500, message = "Varseltekst er for lang")
                                   val varseltekst: String? = null,
                                   val ytelsestype: Ytelsestype,
                                   val behandlendeEnhetId: String? = null,
                                   val behandlendeEnhetsNavn: String,
                                   val språkkode: Språkkode = Språkkode.NB,
                                   val vedtaksdato: LocalDate? = null,
                                   val feilutbetaltePerioderDto: FeilutbetaltePerioderDto,
                                   val fagsystem: Fagsystem,
                                   val saksnummer: String,
                                   val ident: String,
                                   val verge: Verge? = null)

class FeilutbetaltePerioderDto(var sumFeilutbetaling: Long,
                               var perioder: List<Periode>)
