package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.api.v1.dto.VurdertAktsomhetDto
import no.nav.tilbakekreving.api.v1.dto.VurdertGodTroDto
import no.nav.tilbakekreving.api.v1.dto.VurdertSærligGrunnDto
import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingDto
import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingsperiodeDto
import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingsresultatDto
import no.nav.tilbakekreving.beregning.Reduksjon
import no.nav.tilbakekreving.beregning.adapter.VilkårsvurderingAdapter
import no.nav.tilbakekreving.beregning.adapter.VilkårsvurdertPeriodeAdapter
import no.nav.tilbakekreving.entities.BeløpIBeholdEntity
import no.nav.tilbakekreving.entities.BeløpIBeholdJaEntity
import no.nav.tilbakekreving.entities.BeløpIBeholdNeiEntity
import no.nav.tilbakekreving.entities.DatoperiodeEntity
import no.nav.tilbakekreving.entities.FeilaktigeOpplysningerFraBrukerEntity
import no.nav.tilbakekreving.entities.ForsettEntity
import no.nav.tilbakekreving.entities.ForstodEllerBurdeForståttEntity
import no.nav.tilbakekreving.entities.GodTroEntity
import no.nav.tilbakekreving.entities.GrovUaktsomhetEntity
import no.nav.tilbakekreving.entities.IkkeVurdertEntity
import no.nav.tilbakekreving.entities.JaEntitySkalReduseres
import no.nav.tilbakekreving.entities.MangelfulleOpplysningerFraBrukerEntity
import no.nav.tilbakekreving.entities.NeiEntitySkalReduseres
import no.nav.tilbakekreving.entities.SimpelUaktsomhetEntity
import no.nav.tilbakekreving.entities.SkalReduseresEntity
import no.nav.tilbakekreving.entities.SærligeGrunnerEntity
import no.nav.tilbakekreving.entities.VilkårsvurderingsperiodeEntity
import no.nav.tilbakekreving.entities.VilkårsvurderingstegEntity
import no.nav.tilbakekreving.entities.VurderingEntity
import no.nav.tilbakekreving.entities.VurdertAktsomhetEntity
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.AnnenVurdering
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunn
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vurdering as Vurderingstype

class Vilkårsvurderingsteg(
    private var vurderinger: List<Vilkårsvurderingsperiode>,
    private val kravgrunnlagHendelse: HistorikkReferanse<UUID, KravgrunnlagHendelse>,
    private val foreldelsesteg: Foreldelsesteg,
) : Saksbehandlingsteg<VurdertVilkårsvurderingDto>, VilkårsvurderingAdapter {
    override val type: Behandlingssteg = Behandlingssteg.VILKÅRSVURDERING

    override fun erFullstending(): Boolean = vurderinger.none { it.vurdering is Vurdering.IkkeVurdert }

    fun tilEntity(): VilkårsvurderingstegEntity {
        return VilkårsvurderingstegEntity(
            vurderinger = vurderinger.map { it.tilEntity() },
            kravgrunnlagHendelseRef = kravgrunnlagHendelse.entry.internId,
            foreldelsesteg = foreldelsesteg.tilEntity(),
        )
    }

    internal fun vurder(
        periode: Datoperiode,
        vurdering: Vurdering,
    ) {
        val id = finnIdForPeriode(periode)
        vurder(id, vurdering)
    }

    internal fun vurder(
        id: UUID,
        vurdering: Vurdering,
    ) {
        // TODO: Ordentlig feilhåndtering i stedet for NoSuchElementException ved ugyldig periode
        vurderinger.single { it.id == id }.vurder(vurdering)
    }

    private fun finnIdForPeriode(periode: Datoperiode): UUID {
        // TODO: Ordentlig feilhåndtering i stedet for NoSuchElementException ved ugyldig periode
        return vurderinger.single { it.periode == periode }.id
    }

    internal fun splittPerioder(perioder: List<Datoperiode>) {
        if (perioder.sortedBy { it.fom } == vurderinger.map { it.periode }.sortedBy { it.fom }) return

        vurderinger = perioder.map { Vilkårsvurderingsperiode.opprett(it) }
    }

    // TODO: Trenger først muligheten til å referere til tidligere vilkårsvurdert periode for å finne ut
    fun harLikePerioder() = false

    override fun perioder(): Set<VilkårsvurdertPeriodeAdapter> {
        return vurderinger.toSet()
    }

    override fun tilFrontendDto(): VurdertVilkårsvurderingDto {
        fun mapAktsomhet(aktsomhet: VurdertAktsomhet): VurdertAktsomhetDto {
            return VurdertAktsomhetDto(
                aktsomhet = when (aktsomhet) {
                    is VurdertAktsomhet.Forsett -> Aktsomhet.FORSETT
                    is VurdertAktsomhet.GrovUaktsomhet -> Aktsomhet.GROV_UAKTSOMHET
                    is VurdertAktsomhet.SimpelUaktsomhet -> Aktsomhet.SIMPEL_UAKTSOMHET
                },
                ileggRenter = aktsomhet.skalIleggesRenter,
                andelTilbakekreves = (aktsomhet.skalReduseres as? VurdertAktsomhet.SkalReduseres.Ja)?.prosentdel?.toBigDecimal(),
                beløpTilbakekreves = null,
                begrunnelse = aktsomhet.begrunnelse,
                særligeGrunner = aktsomhet.særligeGrunner?.grunner?.map {
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
            perioder = vurderinger.map {
                VurdertVilkårsvurderingsperiodeDto(
                    periode = it.periode,
                    feilutbetaltBeløp = kravgrunnlagHendelse.entry.totaltBeløpFor(it.periode),
                    hendelsestype = Hendelsestype.ANNET,
                    reduserteBeløper = listOf(),
                    aktiviteter = listOf(),
                    begrunnelse = it.begrunnelseForTilbakekreving,
                    foreldet = foreldelsesteg.erPeriodeForeldet(it.periode),
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
    ) : VilkårsvurdertPeriodeAdapter {
        val vurdering get() = _vurdering

        fun vurder(vurdering: Vurdering) {
            _vurdering = vurdering
        }

        override fun periode(): Datoperiode = periode

        override fun reduksjon(): Reduksjon = vurdering.reduksjon()

        override fun renter(): Boolean = vurdering.renter()

        override fun vurdering(): Vurderingstype = vurdering.vurderingstype()

        fun tilEntity(): VilkårsvurderingsperiodeEntity {
            return VilkårsvurderingsperiodeEntity(
                id = id,
                periode = DatoperiodeEntity(periode.fom, periode.tom),
                begrunnelseForTilbakekreving = begrunnelseForTilbakekreving,
                vurdering = _vurdering.tilEntity(),
            )
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

            fun fraEntity(entity: VilkårsvurderingsperiodeEntity): Vilkårsvurderingsperiode {
                return Vilkårsvurderingsperiode(
                    id = entity.id,
                    periode = entity.periode.fraEntity(),
                    begrunnelseForTilbakekreving = entity.begrunnelseForTilbakekreving,
                    _vurdering = entity.vurdering.fraEntity(),
                )
            }
        }
    }

    sealed interface Vurdering {
        val begrunnelse: String? get() = null

        fun reduksjon(): Reduksjon

        fun renter(): Boolean

        fun vurderingstype(): Vurderingstype

        fun tilEntity(): VurderingEntity

        class GodTro(
            val beløpIBehold: BeløpIBehold,
            override val begrunnelse: String,
        ) : Vurdering {
            sealed interface BeløpIBehold {
                fun reduksjon(): Reduksjon

                fun tilEntity(): BeløpIBeholdEntity

                class Ja(val beløp: BigDecimal) : BeløpIBehold {
                    override fun reduksjon(): Reduksjon {
                        return Reduksjon.ManueltBeløp(beløp)
                    }

                    override fun tilEntity(): BeløpIBeholdEntity {
                        return BeløpIBeholdJaEntity(
                            beløp = beløp,
                        )
                    }
                }

                data object Nei : BeløpIBehold {
                    override fun reduksjon(): Reduksjon {
                        return Reduksjon.IngenTilbakekreving()
                    }

                    override fun tilEntity(): BeløpIBeholdEntity {
                        return BeløpIBeholdNeiEntity
                    }
                }
            }

            override fun reduksjon(): Reduksjon = beløpIBehold.reduksjon()

            override fun renter(): Boolean = false

            override fun vurderingstype(): Vurderingstype = AnnenVurdering.GOD_TRO

            override fun tilEntity(): VurderingEntity {
                return GodTroEntity(
                    beløpIBehold = beløpIBehold.tilEntity(),
                    begrunnelse = begrunnelse,
                )
            }
        }

        class ForstodEllerBurdeForstått(
            override val begrunnelse: String,
            val aktsomhet: VurdertAktsomhet,
        ) : Vurdering {
            override fun renter(): Boolean = aktsomhet.skalIleggesRenter

            override fun reduksjon(): Reduksjon = aktsomhet.skalReduseres.reduksjon()

            override fun vurderingstype(): Vurderingstype = aktsomhet.vurderingstype

            override fun tilEntity(): VurderingEntity {
                return ForstodEllerBurdeForståttEntity(
                    begrunnelse = begrunnelse,
                    aktsomhet = aktsomhet.tilEntity(),
                )
            }
        }

        class MangelfulleOpplysningerFraBruker(
            override val begrunnelse: String,
            val aktsomhet: VurdertAktsomhet,
        ) : Vurdering {
            override fun renter(): Boolean = aktsomhet.skalIleggesRenter

            override fun reduksjon(): Reduksjon = aktsomhet.skalReduseres.reduksjon()

            override fun vurderingstype(): Vurderingstype = aktsomhet.vurderingstype

            override fun tilEntity(): VurderingEntity {
                return MangelfulleOpplysningerFraBrukerEntity(
                    begrunnelse = begrunnelse,
                    aktsomhet = aktsomhet.tilEntity(),
                )
            }
        }

        class FeilaktigeOpplysningerFraBruker(
            override val begrunnelse: String,
            val aktsomhet: VurdertAktsomhet,
        ) : Vurdering {
            override fun renter(): Boolean = aktsomhet.skalIleggesRenter

            override fun reduksjon(): Reduksjon = aktsomhet.skalReduseres.reduksjon()

            override fun vurderingstype(): Vurderingstype = aktsomhet.vurderingstype

            override fun tilEntity(): VurderingEntity {
                return FeilaktigeOpplysningerFraBrukerEntity(
                    begrunnelse = begrunnelse,
                    aktsomhet = aktsomhet.tilEntity(),
                )
            }
        }

        data object IkkeVurdert : Vurdering {
            override fun renter(): Boolean = false

            override fun reduksjon(): Reduksjon = Reduksjon.FullstendigRefusjon()

            override fun vurderingstype(): Vurderingstype = object : Vurderingstype {
                override val navn: String = "Ikke ferdigvurdert"
            }

            override fun tilEntity(): VurderingEntity {
                return IkkeVurdertEntity()
            }
        }
    }

    sealed interface VurdertAktsomhet {
        val begrunnelse: String
        val skalIleggesRenter: Boolean
        val særligeGrunner: SærligeGrunner?
        val skalReduseres: SkalReduseres
        val vurderingstype: Vurderingstype

        fun tilEntity(): VurdertAktsomhetEntity

        class SimpelUaktsomhet(
            override val begrunnelse: String,
            override val særligeGrunner: SærligeGrunner,
            override val skalReduseres: SkalReduseres,
        ) : VurdertAktsomhet {
            override val skalIleggesRenter = false
            override val vurderingstype: Vurderingstype = Aktsomhet.SIMPEL_UAKTSOMHET

            override fun tilEntity(): VurdertAktsomhetEntity {
                return SimpelUaktsomhetEntity(
                    begrunnelse = begrunnelse,
                    skalReduseres = skalReduseres.tilEntity(),
                    særligGrunner = særligeGrunner.tilEntity(),
                )
            }
        }

        class GrovUaktsomhet(
            override val begrunnelse: String,
            override val særligeGrunner: SærligeGrunner,
            override val skalReduseres: SkalReduseres,
            override val skalIleggesRenter: Boolean,
        ) : VurdertAktsomhet {
            override val vurderingstype: Vurderingstype = Aktsomhet.GROV_UAKTSOMHET

            override fun tilEntity(): VurdertAktsomhetEntity {
                return GrovUaktsomhetEntity(
                    begrunnelse = begrunnelse,
                    skalReduseres = skalReduseres.tilEntity(),
                    skalIleggesRenter = skalIleggesRenter,
                    særligGrunner = særligeGrunner.tilEntity(),
                )
            }
        }

        class Forsett(
            override val begrunnelse: String,
            override val skalIleggesRenter: Boolean,
        ) : VurdertAktsomhet {
            override val særligeGrunner: SærligeGrunner? = null
            override val skalReduseres: SkalReduseres = SkalReduseres.Nei
            override val vurderingstype: Vurderingstype = Aktsomhet.FORSETT

            override fun tilEntity(): VurdertAktsomhetEntity {
                return ForsettEntity(
                    begrunnelse = begrunnelse,
                    skalIleggesRenter = skalIleggesRenter,
                )
            }
        }

        class SærligeGrunner(
            val begrunnelse: String,
            val grunner: Set<SærligGrunn>,
        ) {
            fun tilEntity(): SærligeGrunnerEntity {
                return SærligeGrunnerEntity(
                    begrunnelse = begrunnelse,
                    grunner = grunner.map { it.name },
                )
            }
        }

        sealed interface SkalReduseres {
            fun reduksjon(): Reduksjon

            fun tilEntity(): SkalReduseresEntity

            class Ja(val prosentdel: Int) : SkalReduseres {
                override fun reduksjon(): Reduksjon {
                    return Reduksjon.Prosentdel(prosentdel.toBigDecimal())
                }

                override fun tilEntity(): SkalReduseresEntity {
                    return JaEntitySkalReduseres(prosentdel)
                }
            }

            data object Nei : SkalReduseres {
                override fun reduksjon(): Reduksjon {
                    return Reduksjon.FullstendigRefusjon()
                }

                override fun tilEntity(): SkalReduseresEntity {
                    return NeiEntitySkalReduseres
                }
            }
        }
    }

    companion object {
        fun opprett(
            kravgrunnlagHendelse: HistorikkReferanse<UUID, KravgrunnlagHendelse>,
            foreldelsesteg: Foreldelsesteg,
        ): Vilkårsvurderingsteg {
            return Vilkårsvurderingsteg(
                kravgrunnlagHendelse.entry.datoperioder().map {
                    Vilkårsvurderingsperiode.opprett(it)
                },
                kravgrunnlagHendelse,
                foreldelsesteg,
            )
        }

        fun fraEntity(
            entity: VilkårsvurderingstegEntity,
            kravgrunnlagHendelse: HistorikkReferanse<UUID, KravgrunnlagHendelse>,
        ): Vilkårsvurderingsteg {
            return Vilkårsvurderingsteg(
                vurderinger = entity.vurderinger.map { Vilkårsvurderingsperiode.fraEntity(it) },
                kravgrunnlagHendelse = kravgrunnlagHendelse,
                foreldelsesteg = entity.foreldelsesteg.fraEntity(kravgrunnlagHendelse),
            )
        }
    }
}
