package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.api.v1.dto.VurdertAktsomhetDto
import no.nav.tilbakekreving.api.v1.dto.VurdertGodTroDto
import no.nav.tilbakekreving.api.v1.dto.VurdertSærligGrunnDto
import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingDto
import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingsperiodeDto
import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingsresultatDto
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunn
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class Vilkårsvurderderingsteg(
    private val vurderinger: List<Vilkårsvurderingsperiode>,
    private val kravgrunnlagHendelse: HistorikkReferanse<UUID, KravgrunnlagHendelse>,
) : Saksbehandlingsteg<VurdertVilkårsvurderingDto> {
    override val type: Behandlingssteg = Behandlingssteg.GRUNNLAG

    override fun erFullstending(): Boolean = vurderinger.all { it.vurdering !is Vurdering.IkkeVurdert }

    fun vurder(
        periode: Datoperiode,
        vurdering: Vurdering,
    ) {
        val id = finnIdForPeriode(periode)
        vurder(id, vurdering)
    }

    fun vurder(
        id: UUID,
        vurdering: Vurdering,
    ) {
        vurderinger.single { it.id == id }.vurder(vurdering)
    }

    private fun finnIdForPeriode(periode: Datoperiode): UUID {
        return vurderinger.single { it.periode == periode }.id
    }

    override fun tilFrontendDto(): VurdertVilkårsvurderingDto {
        fun mapAktsomhet(aktsomhet: VurdertAktsomhet): VurdertAktsomhetDto {
            return VurdertAktsomhetDto(
                aktsomhet =
                    when (aktsomhet) {
                        is VurdertAktsomhet.Forsett -> Aktsomhet.FORSETT
                        is VurdertAktsomhet.GrovUaktsomhet -> Aktsomhet.GROV_UAKTSOMHET
                        is VurdertAktsomhet.SimpelUaktsomhet -> Aktsomhet.SIMPEL_UAKTSOMHET
                    },
                ileggRenter = aktsomhet.skalIleggesRenter,
                andelTilbakekreves = (aktsomhet.skalReduseres as? VurdertAktsomhet.SkalReduseres.Ja)?.prosentdel?.toBigDecimal(),
                beløpTilbakekreves = null,
                begrunnelse = aktsomhet.begrunnelse,
                særligeGrunner =
                    aktsomhet.særligeGrunner?.grunner?.map {
                        VurdertSærligGrunnDto(
                            særligGrunn = it,
                            // TODO: Trenger kanskje egen sealed interface også siden bare annet skal ha grunn
                            begrunnelse = null,
                        )
                    },
                særligeGrunnerTilReduksjon = aktsomhet.skalReduseres is VurdertAktsomhet.SkalReduseres.Ja,
                tilbakekrevSmåbeløp = false,
                særligeGrunnerBegrunnelse = aktsomhet.særligeGrunner?.begrunnelse,
            )
        }
        return VurdertVilkårsvurderingDto(
            perioder =
                vurderinger.map {
                    VurdertVilkårsvurderingsperiodeDto(
                        periode = it.periode,
                        feilutbetaltBeløp = kravgrunnlagHendelse.entry.totaltBeløpFor(it.periode),
                        hendelsestype = Hendelsestype.ANNET,
                        reduserteBeløper = listOf(),
                        aktiviteter = listOf(),
                        begrunnelse = it.begrunnelseForTilbakekreving,
                        // TODO: Spør foreldelsevurderingen
                        foreldet = false,
                        vilkårsvurderingsresultatInfo =
                            it.vurdering.let { vurdering ->
                                when (vurdering) {
                                    is Vurdering.FeilaktigeOpplysningerFraBruker ->
                                        VurdertVilkårsvurderingsresultatDto(
                                            vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
                                            aktsomhet = mapAktsomhet(vurdering.aktsomhet),
                                        )
                                    is Vurdering.MangelfulleOpplysningerFraBruker ->
                                        VurdertVilkårsvurderingsresultatDto(
                                            vilkårsvurderingsresultat = Vilkårsvurderingsresultat.MANGELFULLE_OPPLYSNINGER_FRA_BRUKER,
                                            aktsomhet = mapAktsomhet(vurdering.aktsomhet),
                                        )
                                    is Vurdering.ForstodEllerBurdeForstått ->
                                        VurdertVilkårsvurderingsresultatDto(
                                            vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
                                            aktsomhet = mapAktsomhet(vurdering.aktsomhet),
                                        )
                                    is Vurdering.GodTro ->
                                        VurdertVilkårsvurderingsresultatDto(
                                            vilkårsvurderingsresultat = Vilkårsvurderingsresultat.GOD_TRO,
                                            godTro =
                                                VurdertGodTroDto(
                                                    beløpErIBehold = vurdering.beløpIBehold is Vurdering.GodTro.BeløpIBehold.Ja,
                                                    beløpTilbakekreves = (vurdering.beløpIBehold as? Vurdering.GodTro.BeløpIBehold.Ja)?.beløp,
                                                    begrunnelse = vurdering.begrunnelse,
                                                ),
                                        )
                                    Vurdering.IkkeVurdert -> null
                                }
                            },
                    )
                },
            rettsgebyr = 0,
            opprettetTid = LocalDateTime.now(),
        )
    }

    class Vilkårsvurderingsperiode(
        val id: UUID,
        val periode: Datoperiode,
        val begrunnelseForTilbakekreving: String? = null,
        private var _vurdering: Vurdering,
    ) {
        val vurdering get() = _vurdering

        fun vurder(vurdering: Vurdering) {
            _vurdering = vurdering
        }

        companion object {
            fun opprett(periode: Datoperiode): Vilkårsvurderingsperiode {
                return Vilkårsvurderingsperiode(
                    id = UUID.randomUUID(),
                    periode = periode,
                    begrunnelseForTilbakekreving = null,
                    _vurdering = Vurdering.IkkeVurdert,
                )
            }
        }
    }

    sealed interface Vurdering {
        val begrunnelse: String? get() = null

        class GodTro(
            val beløpIBehold: BeløpIBehold,
            override val begrunnelse: String,
        ) : Vurdering {
            sealed interface BeløpIBehold {
                class Ja(val beløp: BigDecimal) : BeløpIBehold

                data object Nei : BeløpIBehold
            }
        }

        class ForstodEllerBurdeForstått(
            override val begrunnelse: String,
            val aktsomhet: VurdertAktsomhet,
        ) : Vurdering

        class MangelfulleOpplysningerFraBruker(
            override val begrunnelse: String,
            val aktsomhet: VurdertAktsomhet,
        ) : Vurdering

        class FeilaktigeOpplysningerFraBruker(
            override val begrunnelse: String,
            val aktsomhet: VurdertAktsomhet,
        ) : Vurdering

        data object IkkeVurdert : Vurdering
    }

    sealed interface VurdertAktsomhet {
        val begrunnelse: String
        val skalIleggesRenter: Boolean
        val særligeGrunner: SærligeGrunner?
        val skalReduseres: SkalReduseres

        class SimpelUaktsomhet(
            override val begrunnelse: String,
            override val særligeGrunner: SærligeGrunner,
            override val skalReduseres: SkalReduseres,
        ) : VurdertAktsomhet {
            override val skalIleggesRenter = false
        }

        class GrovUaktsomhet(
            override val begrunnelse: String,
            override val særligeGrunner: SærligeGrunner,
            override val skalReduseres: SkalReduseres,
            override val skalIleggesRenter: Boolean,
        ) : VurdertAktsomhet

        class Forsett(
            override val begrunnelse: String,
            override val skalIleggesRenter: Boolean,
        ) : VurdertAktsomhet {
            override val særligeGrunner: SærligeGrunner? = null
            override val skalReduseres: SkalReduseres = SkalReduseres.Nei
        }

        class SærligeGrunner(
            val begrunnelse: String,
            val grunner: Set<SærligGrunn>,
        )

        sealed interface SkalReduseres {
            class Ja(val prosentdel: Int) : SkalReduseres

            data object Nei : SkalReduseres
        }
    }

    companion object {
        fun opprett(kravgrunnlagHendelse: HistorikkReferanse<UUID, KravgrunnlagHendelse>): Vilkårsvurderderingsteg {
            return Vilkårsvurderderingsteg(
                kravgrunnlagHendelse.entry.datoperioder().map {
                    Vilkårsvurderingsperiode.opprett(it)
                },
                kravgrunnlagHendelse,
            )
        }
    }
}
