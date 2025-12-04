package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.kontrakter.frontend.models.BestemmelseEllerGrunnlagDto
import no.nav.kontrakter.frontend.models.FaktaOmFeilutbetalingDto
import no.nav.kontrakter.frontend.models.FaktaPeriodeDto
import no.nav.kontrakter.frontend.models.FeilutbetalingDto
import no.nav.kontrakter.frontend.models.MuligeRettsligGrunnlagDto
import no.nav.kontrakter.frontend.models.OppdagetDto
import no.nav.kontrakter.frontend.models.OppdaterFaktaPeriodeDto
import no.nav.kontrakter.frontend.models.RettsligGrunnlagDto
import no.nav.kontrakter.frontend.models.VurderingDto
import no.nav.tilbakekreving.api.v1.dto.FaktaFeilutbetalingDto
import no.nav.tilbakekreving.api.v1.dto.FeilutbetalingsperiodeDto
import no.nav.tilbakekreving.api.v1.dto.VurderingAvBrukersUttalelseDto
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.brev.BrevHistorikk
import no.nav.tilbakekreving.brev.Varselbrev
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
import java.time.LocalDate
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

    internal fun vurder(perioder: List<OppdaterFaktaPeriodeDto>) {
        vurdering.vurder(perioder)
    }

    internal fun vurder(oppdaget: OppdagetDto) {
        vurdering.vurder(oppdaget)
    }

    internal fun vurder(årsak: String) {
        vurdering.vurder(årsak)
    }

    fun nyTilFrontendDto(kravgrunnlag: KravgrunnlagHendelse, revurdering: EksternFagsakRevurdering, varselbrev: Varselbrev?): FaktaOmFeilutbetalingDto {
        val beløpTilbakekreves = kravgrunnlag.feilutbetaltBeløpForAllePerioder().toInt()
        return FaktaOmFeilutbetalingDto(
            perioder = vurdering.perioder.map {
                it.tilFrontendDto(
                    kravgrunnlag = kravgrunnlag,
                )
            },
            feilutbetaling = FeilutbetalingDto(
                beløp = beløpTilbakekreves,
                fom = vurdering.perioder.minOf { it.periode.fom },
                tom = vurdering.perioder.minOf { it.periode.tom },
                revurdering = revurdering.tilFrontendDto(),
            ),
            muligeRettsligGrunnlag = listOf(
                MuligeRettsligGrunnlagDto(
                    BestemmelseEllerGrunnlagDto(Hendelsestype.ANNET.name, Hendelsestype.ANNET.beskrivelse()),
                    listOf(BestemmelseEllerGrunnlagDto(Hendelsesundertype.ANNET_FRITEKST.name, Hendelsesundertype.ANNET_FRITEKST.beskrivelse())),
                ),
            ),
            vurdering = vurdering.tilFrontendDto(),
            tidligereVarsletBeløp = varselbrev?.hentVarsletBeløp()?.toInt()?.takeIf { it != beløpTilbakekreves },
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
        return vurdering.tilEntity(id, behandlingRef)
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
                oppdaget = Vurdering.Oppdaget.IkkeVurdert,
            )
        }
    }

    class Vurdering(
        val perioder: List<FaktaPeriode>,
        var årsakTilFeilutbetaling: String,
        val uttalelse: Uttalelse,
        private var oppdaget: Oppdaget,
    ) {
        fun erFullstendig(): Boolean {
            return uttalelse.erFullstendig()
        }

        fun tilFrontendDto(): VurderingDto {
            return VurderingDto(
                årsak = årsakTilFeilutbetaling,
                oppdaget = oppdaget.tilFrontendDto(),
            )
        }

        fun vurder(perioder: List<OppdaterFaktaPeriodeDto>) {
            perioder.forEach { oppdatering ->
                val periode = this.perioder.single { it.id.toString() == oppdatering.id }
                periode.vurder(oppdatering)
            }
        }

        fun vurder(oppdaget: OppdagetDto) {
            this.oppdaget = Oppdaget.Vurdering(
                id = (this.oppdaget as? Oppdaget.Vurdering)?.id ?: UUID.randomUUID(),
                dato = oppdaget.dato!!,
                beskrivelse = oppdaget.beskrivelse!!,
                av = when (oppdaget.av) {
                    OppdagetDto.Av.NAV -> Oppdaget.Av.Nav
                    OppdagetDto.Av.BRUKER -> Oppdaget.Av.Bruker
                    OppdagetDto.Av.IKKE_VURDERT -> throw IllegalArgumentException("Kan ikke vurdere oppdaget som IKKE_VURDERT")
                },
            )
        }

        fun vurder(årsak: String) {
            this.årsakTilFeilutbetaling = årsak
        }

        fun tilEntity(
            id: UUID,
            behandlingRef: UUID,
        ): FaktastegEntity {
            return FaktastegEntity(
                id = id,
                behandlingRef = behandlingRef,
                perioder = perioder.map { it.tilEntity(id) },
                uttalelse = when (uttalelse) {
                    is Uttalelse.Ja -> FaktastegEntity.Uttalelse.Ja
                    is Uttalelse.Nei -> FaktastegEntity.Uttalelse.Nei
                    is Uttalelse.IkkeAktuelt -> FaktastegEntity.Uttalelse.IkkeAktuelt
                    is Uttalelse.IkkeVurdert -> FaktastegEntity.Uttalelse.IkkeVurdert
                },
                årsakTilFeilutbetaling = årsakTilFeilutbetaling,
                vurderingAvBrukersUttalelse = (uttalelse as? Uttalelse.Ja)?.begrunnelse,
                oppdaget = oppdaget.tilEntity(id),
            )
        }

        sealed interface Oppdaget {
            fun tilFrontendDto(): OppdagetDto

            fun tilEntity(faktavurderingRef: UUID): FaktastegEntity.OppdagetEntity?

            class Vurdering(
                val id: UUID,
                val dato: LocalDate,
                val beskrivelse: String,
                val av: Av,
            ) : Oppdaget {
                override fun tilFrontendDto(): OppdagetDto {
                    return OppdagetDto(
                        dato = dato,
                        av = when (av) {
                            Av.Nav -> OppdagetDto.Av.NAV
                            Av.Bruker -> OppdagetDto.Av.BRUKER
                        },
                        beskrivelse = beskrivelse,
                    )
                }

                override fun tilEntity(faktavurderingRef: UUID): FaktastegEntity.OppdagetEntity {
                    return FaktastegEntity.OppdagetEntity(
                        id = id,
                        av = when (av) {
                            Av.Nav -> FaktastegEntity.OppdagetAv.Nav
                            Av.Bruker -> FaktastegEntity.OppdagetAv.Bruker
                        },
                        dato = dato,
                        beskrivelse = beskrivelse,
                        faktavurderingRef = faktavurderingRef,
                    )
                }
            }

            data object IkkeVurdert : Oppdaget {
                override fun tilFrontendDto(): OppdagetDto {
                    return OppdagetDto(
                        dato = null,
                        av = OppdagetDto.Av.IKKE_VURDERT,
                        beskrivelse = null,
                    )
                }

                override fun tilEntity(faktavurderingRef: UUID): FaktastegEntity.OppdagetEntity? {
                    return null
                }
            }

            enum class Av {
                Nav,
                Bruker,
            }
        }
    }

    class FaktaPeriode(
        val id: UUID,
        val periode: Datoperiode,
        var rettsligGrunnlag: Hendelsestype,
        var rettsligGrunnlagUnderkategori: Hendelsesundertype,
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
                        bestemmelse = rettsligGrunnlag.name,
                        grunnlag = rettsligGrunnlagUnderkategori.name,
                    ),
                ),
            )
        }

        fun vurder(oppdatering: OppdaterFaktaPeriodeDto) {
            rettsligGrunnlag = enumValueOf(oppdatering.rettsligGrunnlag.single().bestemmelse)
            rettsligGrunnlagUnderkategori = enumValueOf(oppdatering.rettsligGrunnlag.single().grunnlag)
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
