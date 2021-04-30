package no.nav.familie.tilbake.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.tilbakekreving.ForhåndsvisVarselbrevRequest
import no.nav.familie.tilbake.api.dto.HentForhåndvisningVedtaksbrevPdfDto
import no.nav.familie.tilbake.service.dokumentbestilling.brevmaler.Dokumentmalstype
import no.nav.familie.tilbake.service.dokumentbestilling.henleggelse.HenleggelsesbrevService
import no.nav.familie.tilbake.service.dokumentbestilling.innhentdokumentasjon.InnhentDokumentasjonbrevService
import no.nav.familie.tilbake.service.dokumentbestilling.varsel.VarselbrevService
import no.nav.familie.tilbake.service.dokumentbestilling.varsel.manuelt.ManueltVarselbrevService
import no.nav.familie.tilbake.service.dokumentbestilling.vedtak.Avsnitt
import no.nav.familie.tilbake.service.dokumentbestilling.vedtak.VedtaksbrevService
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
                         private val innhentDokumentasjonbrevService: InnhentDokumentasjonbrevService,
                         private val henleggelsesbrevService: HenleggelsesbrevService,
                         private val vedtaksbrevService: VedtaksbrevService) {

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

    @GetMapping("/forhandsvis-henleggelse/{behandlingId}",
                produces = [MediaType.APPLICATION_PDF_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                        handling = "Forhåndsviser brev",
                        henteParam = "behandlingId")
    fun hentForhåndsvisningHenleggelsesbrev(@PathVariable behandlingId: UUID,
                                            fritekst: String): ByteArray {
        return henleggelsesbrevService.hentForhåndsvisningHenleggelsesbrev(behandlingId, fritekst)
    }

    @PostMapping("/forhandsvis-vedtaksbrev",
                 produces = [MediaType.APPLICATION_JSON_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER, handling = "Forhåndsviser brev")
    fun hentForhåndsvisningVedtaksbrev(@RequestBody dto: HentForhåndvisningVedtaksbrevPdfDto): Ressurs<ByteArray> {
        return Ressurs.success(vedtaksbrevService.hentForhåndsvisningVedtaksbrevMedVedleggSomPdf (dto))
    }

    @GetMapping("/vedtaksbrevtekst/{behandlingId}",
                 produces = [MediaType.APPLICATION_JSON_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                        handling = "Forhåndsviser brev",
                        henteParam = "behandlingId")
    fun hentVedtaksbrevtekst(@PathVariable behandlingId: UUID): Ressurs<List<Avsnitt>> {
        return Ressurs.success(vedtaksbrevService.hentVedtaksbrevSomTekst (behandlingId))
    }

}
