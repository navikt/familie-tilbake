package no.nav.familie.tilbake.api

import no.nav.familie.tilbake.service.dokumentbestilling.brevmaler.Dokumentmalstype
import no.nav.familie.tilbake.service.dokumentbestilling.varsel.VarselbrevService
import no.nav.familie.tilbake.service.dokumentbestilling.varsel.manuelt.ManueltVarselbrevService
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.Rolletilgangssjekk
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/dokument")
@ProtectedWithClaims(issuer = "azuread")
class DokumentRestService(val varselbrevService: VarselbrevService,
                          val manueltVarselbrevService: ManueltVarselbrevService) {

    @GetMapping("/forhandsvis-varselbrev",
                produces = [MediaType.APPLICATION_PDF_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER, handling = "Forhåndsviser brev")
    fun hentForhåndsvisningVarselbrev(behandlingId: UUID, malType: Dokumentmalstype, fritekst: String): ByteArray {
        return manueltVarselbrevService.hentForhåndsvisningManueltVarselbrev(behandlingId, malType, fritekst)
    }
}
