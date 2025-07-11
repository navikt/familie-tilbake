package no.nav.familie.tilbake.vilkårsvurdering

import no.nav.familie.tilbake.behandling.Fagsystem
import no.nav.familie.tilbake.beregning.Kravgrunnlag431Adapter
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetaling
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetalingsperiode
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurdering
import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingAktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingGodTro
import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingSærligGrunn
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsperiode
import no.nav.tilbakekreving.Rettsgebyr
import no.nav.tilbakekreving.api.v1.dto.AktivitetDto
import no.nav.tilbakekreving.api.v1.dto.AktsomhetDto
import no.nav.tilbakekreving.api.v1.dto.GodTroDto
import no.nav.tilbakekreving.api.v1.dto.RedusertBeløpDto
import no.nav.tilbakekreving.api.v1.dto.SærligGrunnDto
import no.nav.tilbakekreving.api.v1.dto.VilkårsvurderingsperiodeDto
import no.nav.tilbakekreving.api.v1.dto.VurdertAktsomhetDto
import no.nav.tilbakekreving.api.v1.dto.VurdertGodTroDto
import no.nav.tilbakekreving.api.v1.dto.VurdertSærligGrunnDto
import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingDto
import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingsperiodeDto
import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingsresultatDto
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.periode.Månedsperiode
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

object VilkårsvurderingMapper {
    fun tilRespons(
        vilkårsvurdering: Vilkårsvurdering?,
        perioder: List<Månedsperiode>,
        foreldetPerioderMedBegrunnelse: Map<Månedsperiode, String>,
        faktaFeilutbetaling: FaktaFeilutbetaling,
        kravgrunnlag431: Kravgrunnlag431,
    ): VurdertVilkårsvurderingDto {
        // allerede behandlet perioder uten perioder som er foreldet
        val kravgrunnlagAdapter = Kravgrunnlag431Adapter(kravgrunnlag431)
        val vilkårsvurdertePerioder =
            vilkårsvurdering
                ?.perioder
                ?.filter { it.periode !in foreldetPerioderMedBegrunnelse }
                ?.map {
                    VurdertVilkårsvurderingsperiodeDto(
                        periode = it.periode.toDatoperiode(),
                        feilutbetaltBeløp = kravgrunnlagAdapter.feilutbetaltBeløp(it.periode.toDatoperiode()),
                        hendelsestype =
                            hentHendelsestype(
                                faktaFeilutbetaling.perioder,
                                it.periode,
                            ),
                        reduserteBeløper = utledReduserteBeløp(kravgrunnlag431, it.periode),
                        aktiviteter = hentAktiviteter(kravgrunnlag431, it.periode),
                        begrunnelse = it.begrunnelse,
                        foreldet = false,
                        vilkårsvurderingsresultatInfo = tilVilkårsvurderingsresultatDto(it),
                    )
                }

        val ikkeBehandletPerioder =
            perioder.map {
                VurdertVilkårsvurderingsperiodeDto(
                    periode = it.toDatoperiode(),
                    feilutbetaltBeløp = kravgrunnlagAdapter.feilutbetaltBeløp(it.toDatoperiode()),
                    hendelsestype = hentHendelsestype(faktaFeilutbetaling.perioder, it),
                    reduserteBeløper = utledReduserteBeløp(kravgrunnlag431, it),
                    aktiviteter = hentAktiviteter(kravgrunnlag431, it),
                    foreldet = false,
                )
            }

        val foreldetPerioder =
            foreldetPerioderMedBegrunnelse.map { (periode, begrunnelse) ->
                VurdertVilkårsvurderingsperiodeDto(
                    periode = periode.toDatoperiode(),
                    feilutbetaltBeløp = kravgrunnlagAdapter.feilutbetaltBeløp(periode.toDatoperiode()),
                    hendelsestype = hentHendelsestype(faktaFeilutbetaling.perioder, periode),
                    reduserteBeløper = utledReduserteBeløp(kravgrunnlag431, periode),
                    aktiviteter = hentAktiviteter(kravgrunnlag431, periode),
                    foreldet = true,
                    begrunnelse = begrunnelse,
                )
            }

        val samletPerioder = ikkeBehandletPerioder.toMutableList()
        samletPerioder.addAll(foreldetPerioder)
        vilkårsvurdertePerioder?.let { samletPerioder.addAll(it) }

        return VurdertVilkårsvurderingDto(
            perioder = samletPerioder.sortedBy { it.periode.fom },
            rettsgebyr = Rettsgebyr.rettsgebyr,
            opprettetTid = vilkårsvurdering?.sporbar?.opprettetTid,
        )
    }

    fun tilDomene(
        behandlingId: UUID,
        vilkårsvurderingsperioder: List<VilkårsvurderingsperiodeDto>,
        fagsystem: Fagsystem,
    ): Vilkårsvurdering {
        val vilkårsvurderingsperiode =
            vilkårsvurderingsperioder
                .map {
                    Vilkårsvurderingsperiode(
                        periode = Månedsperiode(it.periode.fom, it.periode.tom),
                        begrunnelse = it.begrunnelse,
                        vilkårsvurderingsresultat = it.vilkårsvurderingsresultat,
                        godTro = tilDomeneGodTro(it.godTroDto),
                        aktsomhet = tilDomeneAktsomhet(it.aktsomhetDto, fagsystem),
                    )
                }.toSet()
        return Vilkårsvurdering(
            behandlingId = behandlingId,
            perioder = vilkårsvurderingsperiode,
        )
    }

    private fun tilVilkårsvurderingsresultatDto(vilkårsvurderingsperiode: Vilkårsvurderingsperiode): VurdertVilkårsvurderingsresultatDto =
        VurdertVilkårsvurderingsresultatDto(
            vilkårsvurderingsresultat = vilkårsvurderingsperiode.vilkårsvurderingsresultat,
            godTro = tilGodTroDto(vilkårsvurderingsperiode.godTro),
            aktsomhet = tilAktsomhetDto(vilkårsvurderingsperiode.aktsomhet),
        )

    private fun tilGodTroDto(vilkårsvurderingGodTro: VilkårsvurderingGodTro?): VurdertGodTroDto? {
        if (vilkårsvurderingGodTro != null) {
            return VurdertGodTroDto(
                begrunnelse = vilkårsvurderingGodTro.begrunnelse,
                beløpErIBehold = vilkårsvurderingGodTro.beløpErIBehold,
                beløpTilbakekreves = vilkårsvurderingGodTro.beløpTilbakekreves,
            )
        }
        return null
    }

    private fun tilDomeneGodTro(godTroDto: GodTroDto?): VilkårsvurderingGodTro? {
        if (godTroDto != null) {
            return VilkårsvurderingGodTro(
                begrunnelse = godTroDto.begrunnelse,
                beløpErIBehold = godTroDto.beløpErIBehold,
                beløpTilbakekreves = godTroDto.beløpTilbakekreves,
            )
        }
        return null
    }

    private fun tilAktsomhetDto(vilkårsvurderingAktsomhet: VilkårsvurderingAktsomhet?): VurdertAktsomhetDto? {
        if (vilkårsvurderingAktsomhet != null) {
            return VurdertAktsomhetDto(
                aktsomhet = vilkårsvurderingAktsomhet.aktsomhet,
                ileggRenter = vilkårsvurderingAktsomhet.ileggRenter,
                andelTilbakekreves = vilkårsvurderingAktsomhet.andelTilbakekreves,
                beløpTilbakekreves = vilkårsvurderingAktsomhet.manueltSattBeløp,
                begrunnelse = vilkårsvurderingAktsomhet.begrunnelse,
                særligeGrunnerTilReduksjon = vilkårsvurderingAktsomhet.særligeGrunnerTilReduksjon,
                særligeGrunnerBegrunnelse = vilkårsvurderingAktsomhet.særligeGrunnerBegrunnelse,
                særligeGrunner =
                    tilSærligGrunnerDto(
                        vilkårsvurderingAktsomhet
                            .vilkårsvurderingSærligeGrunner,
                    ),
                tilbakekrevSmåbeløp = vilkårsvurderingAktsomhet.tilbakekrevSmåbeløp,
            )
        }
        return null
    }

    private fun tilDomeneAktsomhet(
        aktsomhetDto: AktsomhetDto?,
        fagsystem: Fagsystem,
    ): VilkårsvurderingAktsomhet? {
        if (aktsomhetDto != null) {
            return VilkårsvurderingAktsomhet(
                aktsomhet = aktsomhetDto.aktsomhet,
                ileggRenter = utledIleggRenter(aktsomhetDto.ileggRenter, fagsystem),
                andelTilbakekreves = aktsomhetDto.andelTilbakekreves,
                manueltSattBeløp = aktsomhetDto.beløpTilbakekreves,
                begrunnelse = aktsomhetDto.begrunnelse,
                særligeGrunnerTilReduksjon = aktsomhetDto.særligeGrunnerTilReduksjon,
                særligeGrunnerBegrunnelse = aktsomhetDto.særligeGrunnerBegrunnelse,
                vilkårsvurderingSærligeGrunner = tilSærligGrunnerDomene(aktsomhetDto.særligeGrunner),
                tilbakekrevSmåbeløp = aktsomhetDto.tilbakekrevSmåbeløp,
            )
        }
        return null
    }

    private fun tilSærligGrunnerDto(særligGrunner: Set<VilkårsvurderingSærligGrunn>): List<VurdertSærligGrunnDto> =
        særligGrunner.map {
            VurdertSærligGrunnDto(
                særligGrunn = it.særligGrunn,
                begrunnelse = it.begrunnelse,
            )
        }

    private fun tilSærligGrunnerDomene(særligGrunner: List<SærligGrunnDto>?): Set<VilkårsvurderingSærligGrunn> =
        særligGrunner
            ?.map {
                VilkårsvurderingSærligGrunn(
                    særligGrunn = it.særligGrunn,
                    begrunnelse = it.begrunnelse,
                )
            }?.toSet() ?: emptySet()

    private fun hentHendelsestype(
        faktaPerioder: Set<FaktaFeilutbetalingsperiode>,
        vurdertVilkårsperiode: Månedsperiode,
    ): Hendelsestype = faktaPerioder.first { it.periode.overlapper(vurdertVilkårsperiode) }.hendelsestype

    private fun utledReduserteBeløp(
        kravgrunnlag431: Kravgrunnlag431,
        vurdertVilkårsperiode: Månedsperiode,
    ): List<RedusertBeløpDto> {
        val perioder = kravgrunnlag431.perioder.filter { vurdertVilkårsperiode.overlapper(it.periode) }
        val redusertBeløper = mutableListOf<RedusertBeløpDto>()
        // reduserte beløper for SKAT/TREK
        perioder.forEach { periode ->
            periode.beløp
                .filter { Klassetype.SKAT == it.klassetype || Klassetype.TREK == it.klassetype }
                .filter { it.opprinneligUtbetalingsbeløp.signum() == -1 }
                .forEach {
                    redusertBeløper.add(
                        RedusertBeløpDto(
                            true,
                            it.opprinneligUtbetalingsbeløp.abs(),
                        ),
                    )
                }
        }
        // reduserte beløper for JUST(etterbetaling)
        perioder.forEach { periode ->
            periode.beløp
                .filter { Klassetype.JUST == it.klassetype }
                .filter { it.opprinneligUtbetalingsbeløp.signum() == 0 && it.nyttBeløp.signum() == 1 }
                .forEach {
                    redusertBeløper.add(
                        RedusertBeløpDto(
                            false,
                            it.nyttBeløp,
                        ),
                    )
                }
        }
        return redusertBeløper
    }

    private fun hentAktiviteter(
        kravgrunnlag431: Kravgrunnlag431,
        vurdertVilkårsperiode: Månedsperiode,
    ): List<AktivitetDto> {
        val perioder = kravgrunnlag431.perioder.filter { vurdertVilkårsperiode.overlapper(it.periode) }
        val aktiviteter = mutableListOf<AktivitetDto>()
        perioder.forEach { periode ->
            periode.beløp
                .filter { Klassetype.YTEL == it.klassetype && it.tilbakekrevesBeløp.compareTo(BigDecimal.ZERO) != 0 }
                .forEach {
                    aktiviteter.add(
                        AktivitetDto(
                            aktivitet = it.klassekode.aktivitet,
                            beløp = it.tilbakekrevesBeløp.setScale(0, RoundingMode.HALF_UP),
                        ),
                    )
                }
        }
        // oppsummere samme aktiviteter
        val aktivitetMap = mutableMapOf<String, BigDecimal>()
        aktiviteter.forEach {
            val beløp = aktivitetMap[it.aktivitet]
            if (beløp != null) {
                aktivitetMap[it.aktivitet] = beløp.plus(it.beløp)
            } else {
                aktivitetMap[it.aktivitet] = it.beløp
            }
        }
        return aktivitetMap.map { AktivitetDto(it.key, it.value) }
    }

    private fun utledIleggRenter(
        ileggRenter: Boolean?,
        fagsystem: Fagsystem,
    ): Boolean? =
        when {
            ileggRenter != null && listOf(Fagsystem.BA, Fagsystem.KONT).contains(fagsystem) -> false
            else -> ileggRenter
        }
}
