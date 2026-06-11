package no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering

import no.nav.tilbakekreving.Klokke
import no.nav.tilbakekreving.Rettsgebyr
import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingDto
import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingsperiodeDto
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behandling.saksbehandling.Saksbehandlingsteg
import no.nav.tilbakekreving.beregning.Reduksjon
import no.nav.tilbakekreving.beregning.adapter.VilkårsvurderingAdapter
import no.nav.tilbakekreving.beregning.adapter.VilkårsvurdertPeriodeAdapter
import no.nav.tilbakekreving.breeeev.BegrunnetPeriode
import no.nav.tilbakekreving.breeeev.begrunnelse.MeldingTilSaksbehandler
import no.nav.tilbakekreving.breeeev.standardtekster.HjemmelForTilbakekreving
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakRevurdering
import no.nav.tilbakekreving.entities.DatoperiodeEntity
import no.nav.tilbakekreving.entities.VilkårsvurderingsperiodeEntity
import no.nav.tilbakekreving.entities.VilkårsvurderingstegEntity
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.frontend.models.PeriodeDto
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vurdering
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

class Vilkårsvurderingsteg(
    private val id: UUID,
    private var vurderinger: List<Vilkårsvurderingsperiode>,
    private var underkjent: Boolean,
) : Saksbehandlingsteg, VilkårsvurderingAdapter {
    override val type: Behandlingssteg = Behandlingssteg.VILKÅRSVURDERING

    override fun erFullstendig(klokke: Klokke): Boolean = vurderinger.none { it.vurdering is ForårsaketAvBruker.IkkeVurdert }

    override fun erUnderkjent(): Boolean {
        return underkjent
    }

    override fun underkjennSteget() {
        this.underkjent = true
    }

    fun tilEntity(behandlingRef: UUID): VilkårsvurderingstegEntity {
        return VilkårsvurderingstegEntity(
            id = id,
            behandlingRef = behandlingRef,
            vurderinger = vurderinger.map { it.tilEntity(id) },
            trengerNyVurdering = underkjent,
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
        val funnetVurdering = vurderinger.single { it.vurdering !is ForårsaketAvBruker.KopiertVurdering && it.periode.overlapper(periode) }.id
        vurder(funnetVurdering, vurdering)
    }

    internal fun vurder(
        id: UUID,
        vurdering: ForårsaketAvBruker,
    ) {
        // TODO: Ordentlig feilhåndtering i stedet for NoSuchElementException ved ugyldig periode
        vurderinger.single { it.id == id }.vurder(vurdering)
        underkjent = false
    }

    private fun finnIdForPeriode(periode: Datoperiode): UUID {
        // TODO: Ordentlig feilhåndtering i stedet for NoSuchElementException ved ugyldig periode
        return vurderinger.single { it.periode == periode }.id
    }

    private fun finnPeriode(periode: Datoperiode): Vilkårsvurderingsperiode {
        val id = finnIdForPeriode(periode)
        return vurderinger.single { it.id == id }
    }

    fun oppsummer(periode: Datoperiode) = finnPeriode(periode).vurdering.oppsummerVurdering()

    fun hjemlerForTilbakekreving(): List<HjemmelForTilbakekreving> = if (vurderinger.any { it.renter() }) {
        listOf(HjemmelForTilbakekreving.FOLKETRYGDLOVEN_22_17A)
    } else {
        emptyList()
    }

    override fun perioder(): Set<VilkårsvurdertPeriodeAdapter> {
        return vurderinger.toSet()
    }

    fun tilFrontendDto(
        kravgrunnlag: KravgrunnlagHendelse,
        revurdering: EksternFagsakRevurdering,
        foreldelsesteg: Foreldelsesteg,
        klokke: Klokke,
    ): VurdertVilkårsvurderingDto {
        return VurdertVilkårsvurderingDto(
            perioder = slåSammenPerioder(kravgrunnlag, foreldelsesteg),
            // Datoen revurdering ble vedtatt er ikke riktig her, men fører til det høyeste rettsgebyret som kan være relevant.
            // Vi gir heller saksbehandler mulighet til å si at beløpet er under/over 4x rettsgebyr til vi klarer å finne datoen vi skal velge rettsgebyr for
            kanUnnlates4xRettsgebyr = KanUnnlates4xRettsgebyr.kanUnnlates(revurdering.vedtaksdato, kravgrunnlag.feilutbetaltBeløpForAllePerioder()),
            rettsgebyr = Rettsgebyr.rettsgebyr, // Todo burde bruke rettsgebyret som var gjeldene ved utbetalingen. Oppdateres etter avklaring med jurist.
            opprettetTid = klokke.nå(),
        )
    }

    private fun slåSammenPerioder(
        kravgrunnlag: KravgrunnlagHendelse,
        foreldelsesteg: Foreldelsesteg,
    ): List<VurdertVilkårsvurderingsperiodeDto> {
        val sammenslåttePerioder = vurderinger
            .groupBy { it.vurdering.underliggendeVurdering() }
            .map { (_, gruppe) ->
                val første = gruppe.minBy { it.periode.fom }
                val siste = gruppe.maxBy { it.periode.tom }
                lagSammenslåttPeriode(første, siste)
            }

        return sammenslåttePerioder.map { periode ->
            VurdertVilkårsvurderingsperiodeDto(
                periode = periode.periode,
                feilutbetaltBeløp = kravgrunnlag.totaltBeløpFor(periode.periode),
                hendelsestype = Hendelsestype.ANNET,
                reduserteBeløper = listOf(),
                aktiviteter = listOf(),
                begrunnelse = periode.vurdering.begrunnelse,
                foreldet = foreldelsesteg.erSammenslåttPeriodeForeldet(periode.periode),
                vilkårsvurderingsresultatInfo = periode.vurdering.tilFrontendDto(),
            )
        }
    }

    private fun lagSammenslåttPeriode(
        første: Vilkårsvurderingsperiode,
        siste: Vilkårsvurderingsperiode,
    ) = Vilkårsvurderingsperiode(
        id = første.id,
        periode = Datoperiode(første.periode.fom, siste.periode.tom),
        begrunnelseForTilbakekreving = første.begrunnelseForTilbakekreving,
        _vurdering = første.vurdering,
    )

    fun splittVilkårsvurdering(splittFra: LocalDate) {
        val funnetPerioden = vurderinger.single { it.periode.fom == splittFra }.id
        vurder(funnetPerioden, ForårsaketAvBruker.IkkeVurdert())
    }

    fun slåSammenMedForrigePeriode(slåSammenDato: LocalDate) {
        val funnetPerioden = vurderinger.single { it.periode.fom == slåSammenDato }
        val forrigePeriode = vurderinger.last { it.periode.fom < slåSammenDato }
        vurder(funnetPerioden.id, forrigePeriode.vurdering)
    }

    internal fun hentVilkårsvurderingsperioder(): List<PeriodeDto> {
        return vurderinger.map { PeriodeDto(it.periode.fom, it.periode.tom) }
    }

    fun vurdertePerioderForBrev(
        meldingerTilSaksbehandler: Set<MeldingTilSaksbehandler>,
    ): List<BegrunnetPeriode> {
        val samletPeriode = vurderinger.minOf { it.periode.fom } til vurderinger.maxOf { it.periode.tom }
        LoggerFactory.getLogger("vilkårsvurdering").info("Tilgjengelige periode id-er {}", vurderinger.map { "${it.id}=${it.periode.fom}-${it.periode.tom}" })
        return vurderinger.firstOrNull()?.let {
            listOf(
                BegrunnetPeriode(
                    id = it.id,
                    periode = samletPeriode,
                    påkrevdeVurderinger = it.vurdering.påkrevdeVurderinger(),
                    meldingerTilSaksbehandler = meldingerTilSaksbehandler,
                ),
            )
        } ?: emptyList()
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

        fun tilEntity(vurderingRef: UUID): VilkårsvurderingsperiodeEntity {
            return VilkårsvurderingsperiodeEntity(
                id = id,
                vurderingRef = vurderingRef,
                periode = DatoperiodeEntity(periode.fom, periode.tom),
                begrunnelseForTilbakekreving = begrunnelseForTilbakekreving,
                vurdering = _vurdering.tilEntity(id),
            )
        }

        companion object {
            fun opprett(
                periode: Datoperiode,
                forrigePeriode: Vilkårsvurderingsperiode?,
            ): Vilkårsvurderingsperiode {
                val id = UUID.randomUUID()
                return Vilkårsvurderingsperiode(
                    id = id,
                    periode = periode,
                    _vurdering = when (forrigePeriode) {
                        null -> ForårsaketAvBruker.IkkeVurdert()
                        else -> ForårsaketAvBruker.KopiertVurdering(
                            forrigeVurdering = forrigePeriode,
                            forrigePeriodeId = forrigePeriode.id,
                        )
                    },
                )
            }
        }
    }

    companion object {
        fun opprett(
            eksternFagsakRevurdering: EksternFagsakRevurdering,
            kravgrunnlagHendelse: KravgrunnlagHendelse,
        ): Vilkårsvurderingsteg {
            return Vilkårsvurderingsteg(
                id = UUID.randomUUID(),
                vurderinger = tomVurdering(eksternFagsakRevurdering, kravgrunnlagHendelse),
                underkjent = false,
            )
        }

        private fun tomVurdering(
            eksternFagsakRevurdering: EksternFagsakRevurdering,
            kravgrunnlagHendelse: KravgrunnlagHendelse,
        ): List<Vilkårsvurderingsperiode> {
            val perioder = mutableListOf<Vilkårsvurderingsperiode>()
            kravgrunnlagHendelse.datoperioder(eksternFagsakRevurdering).forEach { periode ->
                val utvidetPeriode = eksternFagsakRevurdering.utvidPeriode(periode)
                val vurderingTilForrigePeriode = perioder.lastOrNull()
                perioder.add(
                    Vilkårsvurderingsperiode.opprett(
                        periode = utvidetPeriode,
                        forrigePeriode = vurderingTilForrigePeriode,
                    ),
                )
            }
            return perioder
        }
    }
}
