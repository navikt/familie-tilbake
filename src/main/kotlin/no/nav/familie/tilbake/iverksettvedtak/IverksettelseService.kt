package no.nav.familie.tilbake.iverksettvedtak

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingsvedtakService
import no.nav.familie.tilbake.behandling.domain.Iverksettingsstatus
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.integration.økonomi.ØkonomiClient
import no.nav.familie.tilbake.iverksettvedtak.domain.Tilbakekrevingsbeløp
import no.nav.familie.tilbake.iverksettvedtak.domain.Tilbakekrevingsperiode
import no.nav.familie.tilbake.iverksettvedtak.domain.ØkonomiXmlSendt
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.familie.tilbake.kravgrunnlag.domain.KodeAksjon
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakRequest
import no.nav.tilbakekreving.tilbakekrevingsvedtak.vedtak.v1.TilbakekrevingsbelopDto
import no.nav.tilbakekreving.tilbakekrevingsvedtak.vedtak.v1.TilbakekrevingsperiodeDto
import no.nav.tilbakekreving.tilbakekrevingsvedtak.vedtak.v1.TilbakekrevingsvedtakDto
import no.nav.tilbakekreving.typer.v1.PeriodeDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class IverksettelseService(private val behandlingRepository: BehandlingRepository,
                           private val kravgrunnlagRepository: KravgrunnlagRepository,
                           private val økonomiXmlSendtRepository: ØkonomiXmlSendtRepository,
                           private val tilbakekrevingsvedtakBeregningService: TilbakekrevingsvedtakBeregningService,
                           private val behandlingVedtakService: BehandlingsvedtakService,
                           private val økonomiClient: ØkonomiClient) {

    @Transactional
    fun sendIverksettVedtak(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)

        val beregnetPerioder = tilbakekrevingsvedtakBeregningService.beregnVedtaksperioder(behandlingId, kravgrunnlag)
        val request = lagIveksettelseRequest(behandling.ansvarligSaksbehandler, kravgrunnlag, beregnetPerioder)

        // lagre request i en separat transaksjon slik at det lagrer selv om tasken feiler
        val requestXml = TilbakekrevingsvedtakMarshaller.marshall(behandlingId, request)
        var økonomiXmlSendt = lagreIverksettelsesvedtakRequest(behandlingId, requestXml)

        // Send request til økonomi
        val respons = økonomiClient.iverksettVedtak(behandlingId, request)

        //oppdater respons
        økonomiXmlSendt = økonomiXmlSendtRepository.findByIdOrThrow(økonomiXmlSendt.id)
        økonomiXmlSendtRepository.update(økonomiXmlSendt.copy(kvittering = objectMapper.writeValueAsString(respons.mmel)))

        behandlingVedtakService.oppdaterBehandlingsvedtak(behandlingId, Iverksettingsstatus.IVERKSATT)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun lagreIverksettelsesvedtakRequest(behandlingId: UUID, requestXml: String): ØkonomiXmlSendt {
        return økonomiXmlSendtRepository.insert(ØkonomiXmlSendt(behandlingId = behandlingId, melding = requestXml))
    }

    private fun lagIveksettelseRequest(ansvarligSaksbehandler: String,
                                       kravgrunnlag: Kravgrunnlag431,
                                       beregnetPerioder: List<Tilbakekrevingsperiode>): TilbakekrevingsvedtakRequest {
        val request = TilbakekrevingsvedtakRequest()
        val vedtak = TilbakekrevingsvedtakDto()
        vedtak.apply {
            vedtakId = kravgrunnlag.vedtakId
            kodeAksjon = KodeAksjon.FATTE_VEDTAK.kode
            kodeHjemmel = "22-15" // fast verdi
            datoVedtakFagsystem = kravgrunnlag.fagsystemVedtaksdato ?: LocalDate.now()
            enhetAnsvarlig = kravgrunnlag.ansvarligEnhet
            kontrollfelt = kravgrunnlag.kontrollfelt
            saksbehId = ansvarligSaksbehandler
            tilbakekrevingsperiode.addAll(lagVedtaksperiode(beregnetPerioder))
        }
        return request.apply { tilbakekrevingsvedtak = vedtak }
    }

    private fun lagVedtaksperiode(beregnetPerioder: List<Tilbakekrevingsperiode>): List<TilbakekrevingsperiodeDto> {
        return beregnetPerioder.map {
            val tilbakekrevingsperiode = TilbakekrevingsperiodeDto()
            tilbakekrevingsperiode.apply {
                val periode = PeriodeDto()
                periode.fom = it.periode.fom.atDay(1)
                periode.tom = it.periode.tom.atEndOfMonth()
                this.periode = periode
                belopRenter = it.renter
                tilbakekrevingsbelop.addAll(lagVedtaksbeløp(it.beløp))
            }
        }
    }

    private fun lagVedtaksbeløp(beregnetBeløper: List<Tilbakekrevingsbeløp>): List<TilbakekrevingsbelopDto> {
        return beregnetBeløper.map {
            val tilbakekrevingsbeløp = TilbakekrevingsbelopDto()
            tilbakekrevingsbeløp.apply {
                kodeKlasse = it.klassekode.name
                belopNy = it.nyttBeløp
                belopOpprUtbet = it.utbetaltBeløp
                belopTilbakekreves = it.tilbakekrevesBeløp
                belopUinnkrevd = it.uinnkrevdBeløp
                belopSkatt = it.skattBeløp
                if (Klassetype.YTEL == it.klassetype) {
                    kodeResultat = it.kodeResultat.kode
                    kodeAarsak = "ANNET" // fast verdi
                    kodeSkyld = "IKKE_FORDELT" // fast verdi
                }
            }
        }
    }
}
