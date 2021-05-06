package no.nav.familie.tilbake.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.tilbakekreving.ForhåndsvisVarselbrevRequest
import no.nav.familie.tilbake.api.dto.ForhåndsvisningBrevDto
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
import javax.validation.Valid

@RestController
@RequestMapping("/api/dokument")
@ProtectedWithClaims(issuer = "azuread")
class DokumentController(private val varselbrevService: VarselbrevService,
                         private val manueltVarselbrevService: ManueltVarselbrevService,
                         private val innhentDokumentasjonbrevService: InnhentDokumentasjonbrevService,
                         private val henleggelsesbrevService: HenleggelsesbrevService,
                         private val vedtaksbrevService: VedtaksbrevService) {

    @PostMapping("/forhandsvis/{behandlingId}",
                 produces = [MediaType.APPLICATION_JSON_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                        handling = "Forhåndsviser brev",
                        henteParam = "behandlingId")
    fun hentForhåndsvisningManueltVarselbrev(@PathVariable behandlingId: UUID,
                                             @RequestBody dto: ForhåndsvisningBrevDto): Ressurs<ByteArray> {
        if (dto.malType == Dokumentmalstype.INNHENT_DOKUMENTASJON) {
            return Ressurs.success(innhentDokumentasjonbrevService.hentForhåndsvisningInnhentDokumentasjonBrev(behandlingId,
                                                                                                               dto.fritekst))
        } else if (dto.malType == Dokumentmalstype.VARSEL || dto.malType == Dokumentmalstype.KORRIGERT_VARSEL) {
            return Ressurs.success(manueltVarselbrevService.hentForhåndsvisningManueltVarselbrev(behandlingId,
                                                                                                 dto.malType,
                                                                                                 dto.fritekst))
        } else {
            return Ressurs.funksjonellFeil(melding = "Dokumentmal $dto.malType er ikke støttet",
                                           frontendFeilmelding = "Dokumentmal $dto.malType er ikke støttet");
        }
    }

    @PostMapping("/forhandsvis-varselbrev",
                 produces = [MediaType.APPLICATION_PDF_VALUE])
    @Rolletilgangssjekk(minimumBehandlerrolle = Behandlerrolle.SAKSBEHANDLER, handling = "Forhåndsviser brev")
    fun hentForhåndsvisningVarselbrev(@Valid @RequestBody forhåndsvisVarselbrevRequest: ForhåndsvisVarselbrevRequest): ByteArray {
        return varselbrevService.hentForhåndsvisningVarselbrev(forhåndsvisVarselbrevRequest)
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
    fun hentForhåndsvisningVedtaksbrev(@Valid @RequestBody dto: HentForhåndvisningVedtaksbrevPdfDto): Ressurs<ByteArray> {
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
