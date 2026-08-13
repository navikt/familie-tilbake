package no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering

import no.nav.tilbakekreving.Klokke
import no.nav.tilbakekreving.Rettsgebyr
import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingDto
import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingsperiodeDto
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behandling.saksbehandling.Saksbehandlingsteg
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.Vilkårsvurderingsteg.Vilkårsvurderingsperiode.Companion.tilFrontendDto
import no.nav.tilbakekreving.behandling.saksbehandling.ÅrsakTilTilbakeføring
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
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.frontend.models.PeriodeDto
import no.nav.tilbakekreving.kontrakter.frontend.models.PeriodeInfoDto
import no.nav.tilbakekreving.kontrakter.frontend.models.SammenslaaingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VilkaarsvurderingDto
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode.Companion.overordnet
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vurdering
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagSammenligning
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.math.abs

class Vilkårsvurderingsteg(
    private val id: UUID,
    private var vurderinger: List<Vilkårsvurderingsperiode>,
    private var tilbakeført: ÅrsakTilTilbakeføring?,
) : Saksbehandlingsteg, VilkårsvurderingAdapter {
    override val type: Behandlingssteg = Behandlingssteg.VILKÅRSVURDERING

    override fun erFullstendig(klokke: Klokke): Boolean = vurderinger.none { it.vurdering is ForårsaketAvBruker.IkkeVurdert }

    override fun erPåbegynt(): Boolean = vurderinger.any { it.vurdering.underliggendeVurdering() !is ForårsaketAvBruker.IkkeVurdert }

    override fun trengerNyVurdering(): ÅrsakTilTilbakeføring? {
        return tilbakeført
    }

    override fun underkjennSteget() {
        tilbakeført = ÅrsakTilTilbakeføring.Underkjent
    }

    fun tilEntity(behandlingRef: UUID): VilkårsvurderingstegEntity {
        return VilkårsvurderingstegEntity(
            id = id,
            behandlingRef = behandlingRef,
            vurderinger = vurderinger.map { it.tilEntity(id) },
            tilbakeført = tilbakeført,
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
        tilbakeført = null
    }

    private fun finnIdForPeriode(periode: Datoperiode): UUID {
        // TODO: Ordentlig feilhåndtering i stedet for NoSuchElementException ved ugyldig periode
        return vurderinger.single { it.periode == periode }.id
    }

    private fun finnPeriode(periode: Datoperiode): Vilkårsvurderingsperiode {
        val id = finnIdForPeriode(periode)
        return vurderinger.single { it.id == id }
    }

    private fun finnPeriodeMedId(id: UUID): Vilkårsvurderingsperiode {
        return vurderinger.single { it.id == id }
    }

    override fun perideEndretBeløp(forskjell: KravgrunnlagSammenligning.Forskjell.JustertBeløp) {
        tilbakeført = ÅrsakTilTilbakeføring.NyttKravgrunnlag
        finnPeriode(forskjell.periode).periodeEndretBeløp(forskjell)
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
            perioder = slåSammenPerioder(kravgrunnlag, revurdering, foreldelsesteg),
            // Datoen revurdering ble vedtatt er ikke riktig her, men fører til det høyeste rettsgebyret som kan være relevant.
            // Vi gir heller saksbehandler mulighet til å si at beløpet er under/over 4x rettsgebyr til vi klarer å finne datoen vi skal velge rettsgebyr for
            kanUnnlates4xRettsgebyr = KanUnnlates4xRettsgebyr.kanUnnlates(
                fullstendigVedtaksperiode = kravgrunnlag.perioder().map { it.periode() }.overordnet(),
                årForRettsgebyr = null,
                beløp = kravgrunnlag.feilutbetaltBeløpForAllePerioder(),
            ) != KanUnnlates4xRettsgebyr.KanUnnlates.Nei,
            rettsgebyr = Rettsgebyr.rettsgebyr, // Todo burde bruke rettsgebyret som var gjeldene ved utbetalingen. Oppdateres etter avklaring med jurist.
            opprettetTid = klokke.nå(),
        )
    }

    fun tilFrontendDto() = vurderinger.tilFrontendDto()

    private fun slåSammenPerioder(
        kravgrunnlag: KravgrunnlagHendelse,
        revurdering: EksternFagsakRevurdering,
        foreldelsesteg: Foreldelsesteg,
    ): List<VurdertVilkårsvurderingsperiodeDto> {
        return vurderinger
            .groupBy { it.vurdering.underliggendeVurdering() }
            .map { (_, gruppe) ->
                val første = gruppe.minBy { it.periode.fom }
                val siste = gruppe.maxBy { it.periode.tom }
                val totalPeriode = første.periode.fom til siste.periode.tom
                VurdertVilkårsvurderingsperiodeDto(
                    periode = totalPeriode,
                    feilutbetaltBeløp = kravgrunnlag.totaltBeløpFor(totalPeriode, revurdering),
                    hendelsestype = Hendelsestype.ANNET,
                    reduserteBeløper = listOf(),
                    aktiviteter = listOf(),
                    begrunnelse = første.vurdering.begrunnelse,
                    foreldet = foreldelsesteg.erSammenslåttPeriodeForeldet(totalPeriode),
                    vilkårsvurderingsresultatInfo = første.vurdering.tilFrontendDto(),
                )
            }
    }

    fun splittVilkårsvurdering(vilkårsvurderingId: UUID) {
        vurder(vilkårsvurderingId, ForårsaketAvBruker.IkkeVurdert())
    }

    internal fun hentVilkårsvurderingsperioder(): List<PeriodeInfoDto> {
        return vurderinger.map {
            PeriodeInfoDto(
                periodeId = it.id,
                periode = PeriodeDto(it.periode.fom, it.periode.tom),
            )
        }
    }

    fun kopierVurderingerForSammenslåing(sammenslaaingDto: SammenslaaingDto, sporing: Sporing) {
        validerPeriodeneErInntilHverandre(
            sammenslaaingDto.vilkårsvurderingId,
            sammenslaaingDto.slåesSammenMedId,
            sporing,
        )
        val periodeSomSlåesSammen = finnPeriodeMedId(sammenslaaingDto.vilkårsvurderingId)
        val kopierFraVurdering = finnPeriodeMedId(sammenslaaingDto.slåesSammenMedId)
        periodeSomSlåesSammen.kopierVurdering(kopierFraVurdering)
    }

    private fun validerPeriodeneErInntilHverandre(
        førstePeriodeId: UUID,
        andrePeriodeId: UUID,
        sporing: Sporing,
    ) {
        val førstePeriodeIndeks = vurderinger.indexOfFirst { it.id == førstePeriodeId }
        val andrePeriodeIndeks = vurderinger.indexOfFirst { it.id == andrePeriodeId }
        if (abs(førstePeriodeIndeks - andrePeriodeIndeks) != 1) {
            throw ModellFeil.UgyldigOperasjonException(
                melding = "Kun perioder som er inntil hverandre kan slås sammen. Periode med id ${vurderinger[førstePeriodeIndeks].id} og periode med id ${vurderinger[andrePeriodeIndeks].id} er ikke inntil hverandre.",
                sporing = sporing,
            )
        }
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
        val begrunnelseForTilbakekreving: String?,
        private var _vurdering: ForårsaketAvBruker,
        var endretAvKravgrunnlag: KravgrunnlagSammenligning.Forskjell?,
    ) : VilkårsvurdertPeriodeAdapter {
        val vurdering get() = _vurdering

        fun vurder(vurdering: ForårsaketAvBruker) {
            _vurdering = vurdering
        }

        fun kopierVurdering(kopierFra: Vilkårsvurderingsperiode) {
            _vurdering = ForårsaketAvBruker.KopiertVurdering(
                forrigeVurdering = kopierFra,
                forrigePeriodeId = kopierFra.id,
            )
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
                endretAvKravgrunnlag = endretAvKravgrunnlag?.tilEntity(
                    faktavurderingPeriodeRef = null,
                    vilkårsvurderingPeriodeRef = id,
                ),
            )
        }

        fun periodeEndretBeløp(endretBeløp: KravgrunnlagSammenligning.Forskjell.JustertBeløp) {
            endretAvKravgrunnlag = endretBeløp
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
                    begrunnelseForTilbakekreving = null,
                    endretAvKravgrunnlag = null,
                )
            }

            fun Iterable<Vilkårsvurderingsperiode>.tilFrontendDto(): List<VilkaarsvurderingDto> = groupBy { it.vurdering.underliggendeVurdering() }
                .map { (underliggendeVurdering, perioder) ->
                    VilkaarsvurderingDto(
                        id = perioder.first().id,
                        fom = perioder.minOf { it.periode.fom },
                        tom = perioder.maxOf { it.periode.tom },
                        delbarePerioder = perioder.map {
                            PeriodeInfoDto(
                                periodeId = it.id,
                                periode = PeriodeDto(it.periode.fom, it.periode.tom),
                            )
                        },
                        valg = underliggendeVurdering.tilNyFrontendDto(),
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
                tilbakeført = null,
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
