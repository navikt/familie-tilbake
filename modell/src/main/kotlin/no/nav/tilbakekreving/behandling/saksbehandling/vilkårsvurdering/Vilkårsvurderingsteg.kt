package no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering

import no.nav.tilbakekreving.Rettsgebyr
import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingDto
import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingsperiodeDto
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behandling.saksbehandling.Saksbehandlingsteg
import no.nav.tilbakekreving.beregning.Reduksjon
import no.nav.tilbakekreving.beregning.adapter.VilkårsvurderingAdapter
import no.nav.tilbakekreving.beregning.adapter.VilkårsvurdertPeriodeAdapter
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakRevurdering
import no.nav.tilbakekreving.entities.DatoperiodeEntity
import no.nav.tilbakekreving.entities.VilkårsvurderingsperiodeEntity
import no.nav.tilbakekreving.entities.VilkårsvurderingstegEntity
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vurdering
import java.time.LocalDateTime
import java.util.UUID

class Vilkårsvurderingsteg(
    private var vurderinger: List<Vilkårsvurderingsperiode>,
    private val foreldelsesteg: Foreldelsesteg,
) : Saksbehandlingsteg, VilkårsvurderingAdapter {
    override val type: Behandlingssteg = Behandlingssteg.VILKÅRSVURDERING

    override fun erFullstendig(): Boolean = vurderinger.none { it.vurdering is ForårsaketAvBruker.IkkeVurdert }

    fun tilEntity(): VilkårsvurderingstegEntity {
        return VilkårsvurderingstegEntity(
            vurderinger = vurderinger.map { it.tilEntity() },
        )
    }

    override fun nullstill(
        kravgrunnlag: KravgrunnlagHendelse,
        eksternFagsakRevurdering: EksternFagsakRevurdering,
    ) {
        this.vurderinger = tomVurdering(eksternFagsakRevurdering, kravgrunnlag)
    }

    internal fun vurder(
        periode: Datoperiode,
        vurdering: ForårsaketAvBruker,
    ) {
        val id = finnIdForPeriode(periode)
        vurder(id, vurdering)
    }

    internal fun vurder(
        id: UUID,
        vurdering: ForårsaketAvBruker,
    ) {
        // TODO: Ordentlig feilhåndtering i stedet for NoSuchElementException ved ugyldig periode
        vurderinger.single { it.id == id }.vurder(vurdering)
    }

    private fun finnIdForPeriode(periode: Datoperiode): UUID {
        // TODO: Ordentlig feilhåndtering i stedet for NoSuchElementException ved ugyldig periode
        return vurderinger.single { it.periode == periode }.id
    }

    private fun finnPeriode(periode: Datoperiode): Vilkårsvurderingsperiode {
        val id = finnIdForPeriode(periode)
        return vurderinger.single { it.id == id }
    }

    // TODO: Trenger først muligheten til å referere til tidligere vilkårsvurdert periode for å finne ut
    fun harLikePerioder() = false

    fun oppsummer(periode: Datoperiode) = finnPeriode(periode).vurdering.oppsummerVurdering()

    override fun perioder(): Set<VilkårsvurdertPeriodeAdapter> {
        return vurderinger.toSet()
    }

    fun tilFrontendDto(
        kravgrunnlag: KravgrunnlagHendelse,
    ): VurdertVilkårsvurderingDto {
        return VurdertVilkårsvurderingDto(
            perioder = vurderinger.map {
                VurdertVilkårsvurderingsperiodeDto(
                    periode = it.periode,
                    feilutbetaltBeløp = kravgrunnlag.totaltBeløpFor(it.periode),
                    hendelsestype = Hendelsestype.ANNET,
                    reduserteBeløper = listOf(),
                    aktiviteter = listOf(),
                    begrunnelse = it.vurdering.begrunnelse,
                    foreldet = foreldelsesteg.erPeriodeForeldet(it.periode),
                    vilkårsvurderingsresultatInfo = it.vurdering.tilFrontendDto(),
                )
            },
            rettsgebyr = Rettsgebyr.rettsgebyr, // Todo burde bruke rettsgebyret som var gjeldene ved utbetalingen. Oppdateres etter avklaring med jurist.
            opprettetTid = LocalDateTime.now(),
        )
    }

    class Vilkårsvurderingsperiode(
        val id: UUID,
        val periode: Datoperiode,
        val begrunnelseForTilbakekreving: String? = null,
        private var _vurdering: ForårsaketAvBruker,
    ) : VilkårsvurdertPeriodeAdapter {
        val vurdering get() = _vurdering

        fun vurder(vurdering: ForårsaketAvBruker) {
            _vurdering = vurdering
        }

        override fun periode(): Datoperiode = periode

        override fun reduksjon(): Reduksjon = vurdering.reduksjon()

        override fun renter(): Boolean = vurdering.renter()

        override fun vurdering(): Vurdering = vurdering.vurderingstype()

        fun tilEntity(): VilkårsvurderingsperiodeEntity {
            return VilkårsvurderingsperiodeEntity(
                id = id,
                periode = DatoperiodeEntity(periode.fom, periode.tom),
                begrunnelseForTilbakekreving = begrunnelseForTilbakekreving,
                vurdering = _vurdering.tilEntity(),
            )
        }

        companion object {
            fun opprett(
                periode: Datoperiode,
            ): Vilkårsvurderingsperiode {
                return Vilkårsvurderingsperiode(
                    id = UUID.randomUUID(),
                    periode = periode,
                    begrunnelseForTilbakekreving = null,
                    _vurdering = ForårsaketAvBruker.IkkeVurdert,
                )
            }
        }
    }

    companion object {
        fun opprett(
            eksternFagsakRevurdering: EksternFagsakRevurdering,
            kravgrunnlagHendelse: KravgrunnlagHendelse,
            foreldelsesteg: Foreldelsesteg,
        ): Vilkårsvurderingsteg {
            return Vilkårsvurderingsteg(
                tomVurdering(eksternFagsakRevurdering, kravgrunnlagHendelse),
                foreldelsesteg,
            )
        }

        private fun tomVurdering(
            eksternFagsakRevurdering: EksternFagsakRevurdering,
            kravgrunnlagHendelse: KravgrunnlagHendelse,
        ): List<Vilkårsvurderingsperiode> {
            return kravgrunnlagHendelse.datoperioder().map {
                Vilkårsvurderingsperiode.opprett(
                    periode = eksternFagsakRevurdering.utvidPeriode(it),
                )
            }
        }
    }
}
