package no.nav.familie.tilbake.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.tilbake.service.dokumentbestilling.felles.pdf.JournalføringService
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.Rolletilgangssjekk
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/behandling")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class JournalpostController(private val journalføringService: JournalføringService) {

    @GetMapping("/{behandlingId}/journalpost/{journalpostId}/dokument/{dokumentInfoId}")
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.VEILEDER,
                        handling = "Henter journalført dokument",
                        henteParam = "behandlingId")
    fun hentDokument(@PathVariable behandlingId: UUID,
                     @PathVariable journalpostId: String,
                     @PathVariable dokumentInfoId: String)
            : Ressurs<ByteArray> {
        return Ressurs.success(journalføringService.hentDokument(journalpostId, dokumentInfoId), "OK")
    }
}
