package no.nav.familie.tilbake.api.dto

import no.nav.familie.tilbake.service.dokumentbestilling.brevmaler.Dokumentmalstype
import java.util.UUID
import javax.validation.constraints.Size

class BestillBrevDto(val behandlingId: UUID,
                     val brevmalkode: Dokumentmalstype,
                     @Size(min = 1, max = 3000)
                     val fritekst: String)
