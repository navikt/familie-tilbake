package no.nav.familie.tilbake.api.dto

import no.nav.familie.tilbake.behandling.domain.Fagsaksstatus
import no.nav.familie.tilbake.behandling.domain.Ytelsestype
import no.nav.familie.tilbake.integration.pdl.internal.Kjønn
import java.time.LocalDate

data class FagsakDto(val eksternFagsakId: String,
                     val status: Fagsaksstatus,
                     val ytelsestype: Ytelsestype,
                     val søkerFødselsnummer: String,
                     val språkkode: String,
                     val bruker: BrukerDto)

data class BrukerDto(val navn: String,
                     val fødselsdato: LocalDate,
                     val kjønn: Kjønn)
