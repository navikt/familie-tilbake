package no.nav.familie.tilbake.api

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.tilbake.api.dto.BestillBrevDto
import no.nav.familie.tilbake.service.dokumentbestilling.DokumentbehandlingService
import no.nav.familie.tilbake.service.dokumentbestilling.brevmaler.Dokumentmalstype
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RequestMapping("/api/brev")
@ProtectedWithClaims(issuer = "azuread")
@RestController
class BrevController(private val dokumentbehandlingService: DokumentbehandlingService) {

    @PostMapping("/bestill")
    fun bestillBrev(@RequestBody @Valid bestillBrevDto: BestillBrevDto): Ressurs<Any?> {
        val maltype: Dokumentmalstype = bestillBrevDto.brevmalkode
        dokumentbehandlingService.bestillBrev(bestillBrevDto.behandlingId, maltype, bestillBrevDto.fritekst)
        return Ressurs.success(null)
    }

    @PostMapping("/forhandsvis")
    fun forhåndsvisBrev(@RequestBody @Valid bestillBrevDto: BestillBrevDto): Ressurs<ByteArray> {
        val dokument: ByteArray = dokumentbehandlingService.forhåndsvisBrev(bestillBrevDto.behandlingId,
                                                                            bestillBrevDto.brevmalkode,
                                                                            bestillBrevDto.fritekst)
        return Ressurs.success(dokument)
    }

}