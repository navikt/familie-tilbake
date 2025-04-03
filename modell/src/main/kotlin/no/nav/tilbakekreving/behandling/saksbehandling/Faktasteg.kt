package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandling
import no.nav.tilbakekreving.faktainfo.Faktainfo
import no.nav.tilbakekreving.faktainfo.FaktainfoDto
import no.nav.tilbakekreving.faktainfo.FeilutbetalingsperiodeDto
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.hendelse.VarselbrevSendtHendelse
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class Faktasteg(
    private val eksternFagsakBehandling: HistorikkReferanse<UUID, EksternFagsakBehandling>,
    private val faktainfo: Faktainfo,
    private val kravgrunnlag: HistorikkReferanse<UUID, KravgrunnlagHendelse>,
    private val brev: HistorikkReferanse<UUID, VarselbrevSendtHendelse>,
) : Saksbehandlingsteg<FaktainfoDto> {
    override val type: Behandlingssteg = Behandlingssteg.FAKTA

    override fun erFullstending(): Boolean {
        return true
    }

    override fun tilFrontendDto(): FaktainfoDto {
        return FaktainfoDto(
            varsletBeløp = brev.entry.varsletBeløp,
            totalFeilutbetaltPeriode = Datoperiode(LocalDate.now().minusDays(14), LocalDate.now()),
            feilutbetaltePerioder =
                kravgrunnlag.entry.perioder.map { periode ->
                    FeilutbetalingsperiodeDto(
                        periode = periode.periode,
                        feilutbetaltBeløp = periode.totaltBeløp(),
                        hendelsestype = faktainfo.perioder.first().hendelsestype,
                        hendelsesundertype = faktainfo.perioder.first().hendelsesundertype,
                    )
                },
            totaltFeilutbetaltBeløp = kravgrunnlag.entry.totalFeilutbetalBeløpForAllePerioder(),
            revurderingsvedtaksdato = LocalDate.now(),
            begrunnelse = faktainfo.begrunnelse,
            faktainfo = faktainfo,
            kravgrunnlagReferanse = kravgrunnlag.entry.referanse,
            vurderingAvBrukersUttalelse = faktainfo.vurderingAvBrukersUttalelse,
            opprettetTid = LocalDateTime.now(),
        )
    }

    companion object {
        fun opprett(
            faktainfo: Faktainfo,
            eksternFagsakBehandling: HistorikkReferanse<UUID, EksternFagsakBehandling>,
            kravgrunnlag: HistorikkReferanse<UUID, KravgrunnlagHendelse>,
            brev: HistorikkReferanse<UUID, VarselbrevSendtHendelse>,
        ): Faktasteg {
            return Faktasteg(
                eksternFagsakBehandling = eksternFagsakBehandling,
                faktainfo = faktainfo,
                kravgrunnlag = kravgrunnlag,
                brev = brev,
            )
        }
    }
}
