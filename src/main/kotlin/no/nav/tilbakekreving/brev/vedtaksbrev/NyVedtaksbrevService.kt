package no.nav.tilbakekreving.brev.vedtaksbrev

import no.nav.familie.tilbake.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.tilbakekreving.breeeev.Vedtaksbrev
import no.nav.tilbakekreving.breeeev.begrunnelse.Forklaringstekster
import no.nav.tilbakekreving.breeeev.begrunnelse.VilkårsvurderingBegrunnelse
import no.nav.tilbakekreving.brev.vedtaksbrev.BrevFormatterer.tilDto
import no.nav.tilbakekreving.kontrakter.frontend.models.AvsnittDto
import no.nav.tilbakekreving.kontrakter.frontend.models.AvsnittUpdateItemDto
import no.nav.tilbakekreving.kontrakter.frontend.models.HovedavsnittDto
import no.nav.tilbakekreving.kontrakter.frontend.models.HovedavsnittUpdateDto
import no.nav.tilbakekreving.kontrakter.frontend.models.PakrevdBegrunnelseUpdateItemDto
import no.nav.tilbakekreving.kontrakter.frontend.models.RentekstElementDto
import no.nav.tilbakekreving.kontrakter.frontend.models.RotElementDto
import no.nav.tilbakekreving.kontrakter.frontend.models.RotElementUpdateItemDto
import no.nav.tilbakekreving.kontrakter.frontend.models.SignaturDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksbrevDataDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksbrevRedigerbareDataDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksbrevRedigerbareDataUpdateDto
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class NyVedtaksbrevService(
    private val vedtaksbrevDataRepository: VedtaksbrevDataRepository,
    private val eksterneDataForBrevService: EksterneDataForBrevService,
) {
    fun hentVedtaksbrevData(behandlingId: UUID, vedtaksbrev: Vedtaksbrev): VedtaksbrevDataDto {
        val signatur = SignaturDto(
            vedtaksbrev.signatur.ansvarligEnhet,
            eksterneDataForBrevService.hentSaksbehandlernavn(vedtaksbrev.signatur.ansvarligSaksbehandlerIdent),
            vedtaksbrev.signatur.ansvarligBeslutterIdent?.let(eksterneDataForBrevService::hentSaksbehandlernavn),
        )

        val (sistOppdatert, lagredeData) = vedtaksbrevDataRepository.hentVedtaksbrevData(behandlingId) ?: return VedtaksbrevDataDto(
            hovedavsnitt = HovedavsnittDto(
                tittel = "Du må betale tilbake ${vedtaksbrev.ytelse.bestemtEntall}",
                underavsnitt = listOf(RentekstElementDto("")),
            ),
            avsnitt = BrevFormatterer.lagAvsnitt(vedtaksbrev.perioder),
            sistOppdatert = OffsetDateTime.now(),
            brevGjelder = vedtaksbrev.brukerdata,
            sendtDato = BrevFormatterer.norskDato(LocalDate.now()),
            ytelse = vedtaksbrev.ytelse,
            signatur = signatur,
        )

        return VedtaksbrevDataDto(
            hovedavsnitt = lagredeData.hovedavsnitt.let(::mapHovedavsnitt),
            avsnitt = lagredeData.avsnitt.map(::mapAvsnitt),
            brevGjelder = vedtaksbrev.brukerdata,
            ytelse = vedtaksbrev.ytelse,
            sendtDato = BrevFormatterer.norskDato(LocalDate.now()),
            sistOppdatert = sistOppdatert.atOffset(ZoneOffset.UTC),
            signatur = signatur,
        )
    }

    fun oppdaterVedtaksbrevData(behandlingId: UUID, data: VedtaksbrevRedigerbareDataUpdateDto): VedtaksbrevRedigerbareDataDto {
        val (sistOppdatert, data) = vedtaksbrevDataRepository.oppdaterVedtaksbrevData(behandlingId, data)
        return VedtaksbrevRedigerbareDataDto(
            hovedavsnitt = mapHovedavsnitt(data.hovedavsnitt),
            avsnitt = data.avsnitt.map(::mapAvsnitt),
            sistOppdatert = sistOppdatert.atOffset(ZoneOffset.UTC),
        )
    }

    fun mapHovedavsnitt(hovedavsnitt: HovedavsnittUpdateDto): HovedavsnittDto {
        return HovedavsnittDto(
            tittel = hovedavsnitt.tittel,
            underavsnitt = hovedavsnitt.underavsnitt.map(::mapUnderavsnitt),
        )
    }

    fun mapAvsnitt(avsnitt: AvsnittUpdateItemDto): AvsnittDto {
        return AvsnittDto(
            tittel = avsnitt.tittel,
            forklaring = Forklaringstekster.PERIODE_AVSNITT,
            id = avsnitt.id,
            underavsnitt = avsnitt.underavsnitt.map(::mapUnderavsnitt),
        )
    }

    fun mapUnderavsnitt(avsnitt: RotElementUpdateItemDto): RotElementDto {
        return when (avsnitt) {
            is PakrevdBegrunnelseUpdateItemDto -> VilkårsvurderingBegrunnelse.valueOf(avsnitt.begrunnelseType).tilDto(avsnitt.underavsnitt)
            is RotElementDto -> avsnitt
        }
    }
}
