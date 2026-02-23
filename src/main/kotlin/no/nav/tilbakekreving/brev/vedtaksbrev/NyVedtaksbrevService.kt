package no.nav.tilbakekreving.brev.vedtaksbrev

import no.nav.familie.tilbake.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.tilbakekreving.breeeev.Vedtaksbrev
import no.nav.tilbakekreving.kontrakter.frontend.models.HovedavsnittDto
import no.nav.tilbakekreving.kontrakter.frontend.models.RentekstElementDto
import no.nav.tilbakekreving.kontrakter.frontend.models.SignaturDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksbrevDataDto
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class NyVedtaksbrevService(
    private val vedtaksbrevDataRepository: VedtaksbrevDataRepository,
    private val eksterneDataForBrevService: EksterneDataForBrevService,
) {
    fun hentVedtaksbrevData(behandlingId: UUID, vedtaksbrev: Vedtaksbrev): VedtaksbrevDataDto {
        val lagredeData = vedtaksbrevDataRepository.hentVedtaksbrevData(behandlingId)

        return VedtaksbrevDataDto(
            hovedavsnitt = lagredeData?.hovedavsnitt ?: HovedavsnittDto(
                tittel = "Du må betale tilbake ${vedtaksbrev.ytelse.bestemtEntall}",
                underavsnitt = listOf(RentekstElementDto("")),
            ),
            avsnitt = lagredeData?.avsnitt ?: BrevFormatterer.lagAvsnitt(vedtaksbrev.perioder),
            brevGjelder = vedtaksbrev.brukerdata,
            ytelse = vedtaksbrev.ytelse,
            sendtDato = BrevFormatterer.norskDato(LocalDate.now()),
            sistOppdatert = lagredeData?.sistOppdatert ?: OffsetDateTime.now(),
            signatur = SignaturDto(
                vedtaksbrev.signatur.ansvarligEnhet,
                eksterneDataForBrevService.hentSaksbehandlernavn(vedtaksbrev.signatur.ansvarligSaksbehandlerIdent),
                vedtaksbrev.signatur.ansvarligBeslutterIdent?.let(eksterneDataForBrevService::hentSaksbehandlernavn),
            ),
        )
    }
}
