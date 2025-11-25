package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.kontrakter.frontend.models.BestemmelseEllerGrunnlagDto
import no.nav.kontrakter.frontend.models.FaktaDto
import no.nav.kontrakter.frontend.models.FaktaPeriodeDto
import no.nav.kontrakter.frontend.models.RettsligGrunnlagDto
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
import java.util.UUID

class Faktasteg(
    private val id: UUID,
    private val brevHistorikk: BrevHistorikk,
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

    fun nyTilFrontendDto(kravgrunnlag: KravgrunnlagHendelse): FaktaDto {
        return FaktaDto(
            perioder = vurdering.perioder.map {
                it.tilFrontendDto(
                    kravgrunnlag = kravgrunnlag,
                )
            },
        )
    }

    fun tilFrontendDto(
        kravgrunnlag: KravgrunnlagHendelse,
        eksternFagsakRevurdering: EksternFagsakRevurdering,
        opprettelsesvalg: Opprettelsesvalg,
        tilbakekrevingOpprettet: LocalDateTime,
    ): FaktaFeilutbetalingDto {
        return FaktaFeilutbetalingDto(
            varsletBeløp = brevHistorikk.sisteVarselbrev()?.hentVarsletBeløp(),
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
                tilbakekrevingsvalg = when (opprettelsesvalg) {
                    Opprettelsesvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL -> Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL
                },
                konsekvensForYtelser = emptySet(),
            ),
            kravgrunnlagReferanse = kravgrunnlag.referanse,
            vurderingAvBrukersUttalelse = vurderingAvBrukersUttalelse(),
            opprettetTid = tilbakekrevingOpprettet,
        )
    }

    fun vurderingAvBrukersUttalelse(): VurderingAvBrukersUttalelseDto {
        return VurderingAvBrukersUttalelseDto(
            harBrukerUttaltSeg = when (vurdering.uttalelse) {
                is Uttalelse.Ja -> HarBrukerUttaltSeg.JA
                is Uttalelse.Nei -> HarBrukerUttaltSeg.NEI
                is Uttalelse.IkkeAktuelt -> HarBrukerUttaltSeg.IKKE_AKTUELT
                is Uttalelse.IkkeVurdert -> HarBrukerUttaltSeg.IKKE_VURDERT
            },
            beskrivelse = (vurdering.uttalelse as? Uttalelse.Ja)?.begrunnelse,
        )
    }

    fun tilEntity(behandlingRef: UUID): FaktastegEntity {
        return FaktastegEntity(
            id = id,
            behandlingRef = behandlingRef,
            perioder = vurdering.perioder.map { it.tilEntity(id) },
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
        ): Faktasteg {
            return Faktasteg(
                id = UUID.randomUUID(),
                brevHistorikk = brevHistorikk,
                vurdering = tomVurdering(kravgrunnlag, eksternFagsakRevurdering),
            )
        }

        private fun tomVurdering(kravgrunnlag: KravgrunnlagHendelse, eksternFagsakRevurdering: EksternFagsakRevurdering): Vurdering {
            return Vurdering(
                perioder = kravgrunnlag.datoperioder(eksternFagsakRevurdering).map {
                    FaktaPeriode(
                        id = UUID.randomUUID(),
                        periode = it,
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
        val id: UUID,
        val periode: Datoperiode,
        val rettsligGrunnlag: Hendelsestype,
        val rettsligGrunnlagUnderkategori: Hendelsesundertype,
    ) {
        fun tilEntity(faktavurderingRef: UUID): FaktastegEntity.FaktaPeriodeEntity {
            return FaktastegEntity.FaktaPeriodeEntity(
                id = id,
                faktavurderingRef = faktavurderingRef,
                periode = DatoperiodeEntity(fom = periode.fom, tom = periode.tom),
                rettsligGrunnlag = rettsligGrunnlag,
                rettsligGrunnlagUnderkategori = rettsligGrunnlagUnderkategori,
            )
        }

        fun tilFrontendDto(kravgrunnlag: KravgrunnlagHendelse): FaktaPeriodeDto {
            return FaktaPeriodeDto(
                id = id.toString(),
                fom = periode.fom,
                tom = periode.tom,
                feilutbetaltBeløp = kravgrunnlag.totaltBeløpFor(periode).toInt(),
                splittbarePerioder = emptyList(),
                rettsligGrunnlag = listOf(
                    RettsligGrunnlagDto(
                        bestemmelse = BestemmelseEllerGrunnlagDto(rettsligGrunnlag.name, rettsligGrunnlag.beskrivelse()),
                        grunnlag = BestemmelseEllerGrunnlagDto(rettsligGrunnlagUnderkategori.name, rettsligGrunnlagUnderkategori.beskrivelse()),
                    ),
                ),
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
