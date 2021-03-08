package no.nav.familie.tilbake.api

import no.nav.familie.tilbake.api.dto.ForhåndsvisVarselbrevRequest
import no.nav.familie.tilbake.service.dokumentbestilling.brevmaler.Dokumentmalstype
import no.nav.familie.tilbake.service.dokumentbestilling.innhentdokumentasjon.InnhentDokumentasjonbrevService
import no.nav.familie.tilbake.service.dokumentbestilling.varsel.VarselbrevService
import no.nav.familie.tilbake.service.dokumentbestilling.varsel.manuelt.ManueltVarselbrevService
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.Rolletilgangssjekk
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/dokument")
@ProtectedWithClaims(issuer = "azuread")
class DokumentController(private val varselbrevService: VarselbrevService,
                         private val manueltVarselbrevService: ManueltVarselbrevService,
                         private val innhentDokumentasjonbrevService: InnhentDokumentasjonbrevService) {

    @GetMapping("/forhandsvis-manueltVarselbrev/{behandlingId}",
                produces = [MediaType.APPLICATION_PDF_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                        handling = "Forhåndsviser brev",
                        henteParam = "behandlingId")
    fun hentForhåndsvisningManueltVarselbrev(@PathVariable behandlingId: UUID,
                                             malType: Dokumentmalstype,
                                             fritekst: String): ByteArray {
        return manueltVarselbrevService.hentForhåndsvisningManueltVarselbrev(behandlingId, malType, fritekst)
    }

    @PostMapping("/forhandsvis-varselbrev",
                 produces = [MediaType.APPLICATION_PDF_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER, handling = "Forhåndsviser brev")
    fun hentForhåndsvisningVarselbrev(@RequestBody forhåndsvisVarselbrevRequest: ForhåndsvisVarselbrevRequest): ByteArray {
        return varselbrevService.hentForhåndsvisningVarselbrev(forhåndsvisVarselbrevRequest)
    }

    @GetMapping("/forhandsvis-innhentbrev/{behandlingId}",
                 produces = [MediaType.APPLICATION_PDF_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                        handling = "Forhåndsviser brev",
                        henteParam = "behandlingId")
    fun hentForhåndsvisningInnhentDokumentasjonsbrev(@PathVariable behandlingId: UUID,
                                                     fritekst: String): ByteArray {
        return innhentDokumentasjonbrevService.hentForhåndsvisningInnhentDokumentasjonBrev(behandlingId, fritekst)
    }

}
