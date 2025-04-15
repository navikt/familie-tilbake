package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.api.v1.dto.VurdertForeldelseDto
import no.nav.tilbakekreving.api.v1.dto.VurdertForeldelsesperiodeDto
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.historikk.HistorikkReferanse
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.foreldelse.Foreldelsesvurderingstype
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.time.LocalDate
import java.util.UUID

class Foreldelsesteg(
    private var vurdertePerioder: List<Foreldelseperiode>,
    private val kravgrunnlag: HistorikkReferanse<UUID, KravgrunnlagHendelse>,
) : Saksbehandlingsteg<VurdertForeldelseDto> {
    override val type: Behandlingssteg = Behandlingssteg.FORELDELSE

    override fun erFullstending(): Boolean = vurdertePerioder.all { it.vurdering != Vurdering.IkkeVurdert }

    fun vurderForeldelse(
        periode: Datoperiode,
        vurdering: Vurdering,
    ) {
        val periodeId = finnIdFor(periode) // I fremtiden ønsker vi å sende inn id, ikke periode
        vurderForeldelse(periodeId, vurdering)
    }

    fun vurderForeldelse(
        periodeId: UUID,
        vurdering: Vurdering,
    ) {
        // TODO: Ordentlig feilhåndtering i stedet for NoSuchElementException ved ugyldig periode
        vurdertePerioder.single { it.id == periodeId }.vurderForeldelse(vurdering)
    }

    fun splittPerioder(perioder: List<Datoperiode>) {
        if (perioder.sortedBy { it.fom } == vurdertePerioder.map { it.periode }.sortedBy { it.fom }) return

        vurdertePerioder = perioder.map { Foreldelseperiode.opprett(it) }
    }

    fun erPeriodeForeldet(periode: Datoperiode): Boolean {
        return vurdertePerioder.single { it.periode.inneholder(periode) }.vurdering !is Vurdering.IkkeForeldet
    }

    private fun finnIdFor(periode: Datoperiode): UUID {
        // TODO: Ordentlig feilhåndtering i stedet for NoSuchElementException ved ugyldig periode
        return vurdertePerioder.single { it.periode == periode }.id
    }

    fun perioder(): List<Datoperiode> = vurdertePerioder
        .filter { it.vurdering is Vurdering.Foreldet }
        .map(Foreldelseperiode::periode)

    override fun tilFrontendDto(): VurdertForeldelseDto {
        return VurdertForeldelseDto(
            foreldetPerioder =
                vurdertePerioder.map {
                    VurdertForeldelsesperiodeDto(
                        periode = it.periode,
                        feilutbetaltBeløp = kravgrunnlag.entry.totaltBeløpFor(it.periode),
                        begrunnelse = it.vurdering.begrunnelse,
                        foreldelsesvurderingstype =
                            when (it.vurdering) {
                                is Vurdering.IkkeForeldet -> Foreldelsesvurderingstype.IKKE_FORELDET
                                is Vurdering.Foreldet -> Foreldelsesvurderingstype.FORELDET
                                is Vurdering.IkkeVurdert -> Foreldelsesvurderingstype.IKKE_VURDERT
                                is Vurdering.Tilleggsfrist -> Foreldelsesvurderingstype.TILLEGGSFRIST
                            },
                        foreldelsesfrist = it.vurdering.frist,
                        oppdagelsesdato = (it.vurdering as? Vurdering.Tilleggsfrist)?.oppdaget,
                    )
                },
        )
    }

    class Foreldelseperiode private constructor(
        val id: UUID,
        val periode: Datoperiode,
        private var _vurdering: Vurdering,
    ) {
        val vurdering get() = _vurdering

        fun vurderForeldelse(vurdering: Vurdering) {
            this._vurdering = vurdering
        }

        companion object {
            fun opprett(periode: Datoperiode) =
                Foreldelseperiode(
                    id = UUID.randomUUID(),
                    periode = periode,
                    _vurdering = Vurdering.IkkeVurdert,
                )
        }
    }

    sealed interface Vurdering {
        val begrunnelse: String? get() = null
        val frist: LocalDate? get() = null

        class IkkeForeldet(override val begrunnelse: String) : Vurdering

        object IkkeVurdert : Vurdering

        class Tilleggsfrist(override val frist: LocalDate, val oppdaget: LocalDate) : Vurdering

        class Foreldet(override val begrunnelse: String, override val frist: LocalDate) : Vurdering
    }

    companion object {
        fun opprett(kravgrunnlag: HistorikkReferanse<UUID, KravgrunnlagHendelse>): Foreldelsesteg {
            return Foreldelsesteg(
                vurdertePerioder =
                    kravgrunnlag.entry.datoperioder().map {
                        Foreldelseperiode.opprett(
                            periode = it,
                        )
                    },
                kravgrunnlag = kravgrunnlag,
            )
        }
    }
}
