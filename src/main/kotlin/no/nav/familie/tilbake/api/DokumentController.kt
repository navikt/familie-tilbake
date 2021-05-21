package no.nav.familie.tilbake.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.tilbakekreving.ForhåndsvisVarselbrevRequest
import no.nav.familie.tilbake.api.dto.BestillBrevDto
import no.nav.familie.tilbake.api.dto.ForhåndsvisningHenleggelsesbrevDto
import no.nav.familie.tilbake.api.dto.HentForhåndvisningVedtaksbrevPdfDto
import no.nav.familie.tilbake.service.dokumentbestilling.DokumentbehandlingService
import no.nav.familie.tilbake.service.dokumentbestilling.brevmaler.Dokumentmalstype
import no.nav.familie.tilbake.service.dokumentbestilling.henleggelse.HenleggelsesbrevService
import no.nav.familie.tilbake.service.dokumentbestilling.varsel.VarselbrevService
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
import javax.validation.Valid

@RestController
@RequestMapping("/api/dokument")
@ProtectedWithClaims(issuer = "azuread")
class DokumentController(private val varselbrevService: VarselbrevService,
                         private val dokumentbehandlingService: DokumentbehandlingService,
                         private val henleggelsesbrevService: HenleggelsesbrevService,
                         private val vedtaksbrevService: VedtaksbrevService) {

    @PostMapping("/bestill")
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER, handling = "Sender brev")
    fun bestillBrev(@RequestBody @Valid bestillBrevDto: BestillBrevDto): Ressurs<Any?> {
        val maltype: Dokumentmalstype = bestillBrevDto.brevmalkode
        dokumentbehandlingService.bestillBrev(bestillBrevDto.behandlingId, maltype, bestillBrevDto.fritekst)
        return Ressurs.success(null)
    }

    @PostMapping("/forhandsvis")
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER, handling = "Forhåndsviser brev")
    fun forhåndsvisBrev(@RequestBody @Valid bestillBrevDto: BestillBrevDto): Ressurs<ByteArray> {
        val dokument: ByteArray = dokumentbehandlingService.forhåndsvisBrev(bestillBrevDto.behandlingId,
                                                                            bestillBrevDto.brevmalkode,
                                                                            bestillBrevDto.fritekst)
        return Ressurs.success(dokument)
    }

    @PostMapping("/forhandsvis-varselbrev",
                 produces = [MediaType.APPLICATION_PDF_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER, handling = "Forhåndsviser brev")
    fun hentForhåndsvisningVarselbrev(@Valid @RequestBody forhåndsvisVarselbrevRequest: ForhåndsvisVarselbrevRequest): ByteArray {
        return varselbrevService.hentForhåndsvisningVarselbrev(forhåndsvisVarselbrevRequest)
    }

    @PostMapping("/forhandsvis-henleggelsesbrev",
                     produces = [MediaType.APPLICATION_JSON_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                        handling = "Forhåndsviser henleggelsesbrev",)
    fun hentForhåndsvisningHenleggelsesbrev(@Valid @RequestBody dto: ForhåndsvisningHenleggelsesbrevDto): Ressurs<ByteArray> {
        return Ressurs.success(henleggelsesbrevService.hentForhåndsvisningHenleggelsesbrev(dto.behandlingId, dto.fritekst))
    }

    @PostMapping("/forhandsvis-vedtaksbrev",
                 produces = [MediaType.APPLICATION_JSON_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER, handling = "Forhåndsviser brev")
    fun hentForhåndsvisningVedtaksbrev(@Valid @RequestBody dto: HentForhåndvisningVedtaksbrevPdfDto): Ressurs<ByteArray> {
        return Ressurs.success(vedtaksbrevService.hentForhåndsvisningVedtaksbrevMedVedleggSomPdf(dto))
    }

    @GetMapping("/vedtaksbrevtekst/{behandlingId}",
                produces = [MediaType.APPLICATION_JSON_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.VEILEDER,
                        handling = "Henter vedtaksbrevtekst",
                        henteParam = "behandlingId")
    fun hentVedtaksbrevtekst(@PathVariable behandlingId: UUID): Ressurs<List<Avsnitt>> {
        return Ressurs.success(vedtaksbrevService.hentVedtaksbrevSomTekst(behandlingId))
    }

}
