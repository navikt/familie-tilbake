package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.api.v1.dto.FaktaFeilutbetalingDto
import no.nav.tilbakekreving.api.v1.dto.FeilutbetalingsperiodeDto
import no.nav.tilbakekreving.api.v1.dto.VurderingAvBrukersUttalelseDto
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.brev.BrevHistorikk
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakRevurdering
import no.nav.tilbakekreving.entities.DatoperiodeEntity
import no.nav.tilbakekreving.entities.FaktastegEntity
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.kontrakter.Faktainfo
import no.nav.tilbakekreving.kontrakter.Tilbakekrevingsvalg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.HarBrukerUttaltSeg
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsesundertype
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.periode.til
import java.time.LocalDateTime

class Faktasteg(
    private val brevHistorikk: BrevHistorikk,
    private val tilbakekrevingOpprettet: LocalDateTime,
    private val opprettelsesvalg: Opprettelsesvalg,
    private var vurdering: Vurdering,
) : Saksbehandlingsteg {
    override val type: Behandlingssteg = Behandlingssteg.FAKTA

    override fun erFullstendig(): Boolean {
        return vurdering.erFullstendig()
    }

    override fun nullstill(
        kravgrunnlag: KravgrunnlagHendelse,
        eksternFagsakRevurdering: EksternFagsakRevurdering,
    ) {
        vurdering = tomVurdering(kravgrunnlag, eksternFagsakRevurdering)
    }

    internal fun vurder(vurdering: Vurdering) {
        this.vurdering = vurdering
    }

    fun tilFrontendDto(
        kravgrunnlag: KravgrunnlagHendelse,
        eksternFagsakRevurdering: EksternFagsakRevurdering,
    ): FaktaFeilutbetalingDto {
        return FaktaFeilutbetalingDto(
            varsletBeløp = brevHistorikk.sisteVarselbrev()?.varsletBeløp,
            totalFeilutbetaltPeriode = vurdering.perioder.minOf { it.periode.fom } til vurdering.perioder.maxOf { it.periode.tom },
            totaltFeilutbetaltBeløp = kravgrunnlag.feilutbetaltBeløpForAllePerioder(),
            feilutbetaltePerioder = vurdering.perioder.map {
                FeilutbetalingsperiodeDto(
                    periode = it.periode,
                    feilutbetaltBeløp = kravgrunnlag.totaltBeløpFor(it.periode),
                    hendelsestype = it.rettsligGrunnlag,
                    hendelsesundertype = it.rettsligGrunnlagUnderkategori,
                )
            },
            revurderingsvedtaksdato = eksternFagsakRevurdering.vedtaksdato,
            begrunnelse = vurdering.årsakTilFeilutbetaling,
            faktainfo = Faktainfo(
                revurderingsårsak = eksternFagsakRevurdering.revurderingsårsak.beskrivelse,
                revurderingsresultat = eksternFagsakRevurdering.årsakTilFeilutbetaling,
                tilbakekrevingsvalg =
                    when (opprettelsesvalg) {
                        Opprettelsesvalg.UTSETT_BEHANDLING_MED_VARSEL -> Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL
                        Opprettelsesvalg.UTSETT_BEHANDLING_UTEN_VARSEL -> Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL
                        Opprettelsesvalg.OPPRETT_BEHANDLING_MED_VARSEL -> Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_AUTOMATISK
                    },
                konsekvensForYtelser = emptySet(),
            ),
            kravgrunnlagReferanse = kravgrunnlag.referanse,
            vurderingAvBrukersUttalelse = VurderingAvBrukersUttalelseDto(
                harBrukerUttaltSeg = when (vurdering.uttalelse) {
                    is Uttalelse.Ja -> HarBrukerUttaltSeg.JA
                    is Uttalelse.Nei -> HarBrukerUttaltSeg.NEI
                    is Uttalelse.IkkeAktuelt -> HarBrukerUttaltSeg.IKKE_AKTUELT
                    is Uttalelse.IkkeVurdert -> HarBrukerUttaltSeg.IKKE_VURDERT
                },
                beskrivelse = (vurdering.uttalelse as? Uttalelse.Ja)?.begrunnelse,
            ),
            opprettetTid = tilbakekrevingOpprettet,
        )
    }

    fun tilEntity(): FaktastegEntity {
        return FaktastegEntity(
            tilbakekrevingOpprettet = tilbakekrevingOpprettet,
            opprettelsesvalg = opprettelsesvalg,
            perioder = vurdering.perioder.map { it.tilEntity() },
            uttalelse = when (vurdering.uttalelse) {
                is Uttalelse.Ja -> FaktastegEntity.Uttalelse.Ja
                is Uttalelse.Nei -> FaktastegEntity.Uttalelse.Nei
                is Uttalelse.IkkeAktuelt -> FaktastegEntity.Uttalelse.IkkeAktuelt
                is Uttalelse.IkkeVurdert -> FaktastegEntity.Uttalelse.IkkeVurdert
            },
            årsakTilFeilutbetaling = vurdering.årsakTilFeilutbetaling,
            vurderingAvBrukersUttalelse = (vurdering.uttalelse as? Uttalelse.Ja)?.begrunnelse,
        )
    }

    companion object {
        fun opprett(
            eksternFagsakRevurdering: EksternFagsakRevurdering,
            kravgrunnlag: KravgrunnlagHendelse,
            brevHistorikk: BrevHistorikk,
            tilbakekrevingOpprettet: LocalDateTime,
            opprettelsesvalg: Opprettelsesvalg,
        ): Faktasteg {
            return Faktasteg(
                brevHistorikk = brevHistorikk,
                tilbakekrevingOpprettet = tilbakekrevingOpprettet,
                opprettelsesvalg = opprettelsesvalg,
                vurdering = tomVurdering(kravgrunnlag, eksternFagsakRevurdering),
            )
        }

        private fun tomVurdering(kravgrunnlag: KravgrunnlagHendelse, eksternFagsakRevurdering: EksternFagsakRevurdering): Vurdering {
            return Vurdering(
                perioder = kravgrunnlag.datoperioder().map {
                    FaktaPeriode(
                        periode = eksternFagsakRevurdering.utvidPeriode(it),
                        rettsligGrunnlag = Hendelsestype.ANNET,
                        rettsligGrunnlagUnderkategori = Hendelsesundertype.ANNET_FRITEKST,
                    )
                },
                årsakTilFeilutbetaling = eksternFagsakRevurdering.årsakTilFeilutbetaling,
                uttalelse = Uttalelse.IkkeVurdert,
            )
        }
    }

    class Vurdering(
        val perioder: List<FaktaPeriode>,
        val årsakTilFeilutbetaling: String,
        val uttalelse: Uttalelse,
    ) {
        fun erFullstendig(): Boolean {
            return uttalelse.erFullstendig()
        }
    }

    class FaktaPeriode(
        val periode: Datoperiode,
        val rettsligGrunnlag: Hendelsestype,
        val rettsligGrunnlagUnderkategori: Hendelsesundertype,
    ) {
        fun tilEntity(): FaktastegEntity.FaktaPeriodeEntity {
            return FaktastegEntity.FaktaPeriodeEntity(
                periode = DatoperiodeEntity(fom = periode.fom, tom = periode.tom),
                rettsligGrunnlag = rettsligGrunnlag,
                rettsligGrunnlagUnderkategori = rettsligGrunnlagUnderkategori,
            )
        }
    }

    sealed interface Uttalelse {
        fun erFullstendig(): Boolean

        class Ja(val begrunnelse: String) : Uttalelse {
            override fun erFullstendig(): Boolean = begrunnelse.isNotBlank()
        }

        data object Nei : Uttalelse {
            override fun erFullstendig(): Boolean = true
        }

        data object IkkeAktuelt : Uttalelse {
            override fun erFullstendig(): Boolean = true
        }

        data object IkkeVurdert : Uttalelse {
            override fun erFullstendig(): Boolean = false
        }
    }
}
