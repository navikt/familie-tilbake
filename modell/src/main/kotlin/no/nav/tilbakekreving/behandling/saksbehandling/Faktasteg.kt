package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.api.v1.dto.FaktaFeilutbetalingDto
import no.nav.tilbakekreving.api.v1.dto.VurderingAvBrukersUttalelseDto
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandling
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import no.nav.tilbakekreving.kontrakter.Faktainfo
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.HarBrukerUttaltSeg
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class Faktasteg(
    private val eksternFagsakBehandling: HistorikkReferanse<UUID, EksternFagsakBehandling>,
    private val faktainfo: Faktainfo,
    private val kravgrunnlag: HistorikkReferanse<UUID, KravgrunnlagHendelse>,
) : Saksbehandlingsteg<FaktaFeilutbetalingDto> {
    override val type: Behandlingssteg = Behandlingssteg.FAKTA

    override fun erFullstending(): Boolean {
        return true
    }

    override fun tilFrontendDto(): FaktaFeilutbetalingDto {
        return FaktaFeilutbetalingDto(
            varsletBeløp = faktainfo.varsletBeløp,
            totalFeilutbetaltPeriode = Datoperiode(LocalDate.now().minusDays(14), LocalDate.now()),
            feilutbetaltePerioder = emptyList(),
            totaltFeilutbetaltBeløp = BigDecimal("0.0"),
            revurderingsvedtaksdato = LocalDate.now(),
            begrunnelse = faktainfo.revurderingsårsak,
            faktainfo = faktainfo,
            kravgrunnlagReferanse = kravgrunnlag.entry.referanse,
            vurderingAvBrukersUttalelse =
                VurderingAvBrukersUttalelseDto(
                    harBrukerUttaltSeg = HarBrukerUttaltSeg.NEI,
                    beskrivelse = null,
                ),
            opprettetTid = LocalDateTime.now(),
        )
    }
}
