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
    private val vurdertePerioder: List<Foreldelseperiode>,
    private val kravgrunnlag: HistorikkReferanse<UUID, KravgrunnlagHendelse>,
) : Saksbehandlingsteg<VurdertForeldelseDto> {
    override val type: Behandlingssteg = Behandlingssteg.FORELDELSE

    override fun erFullstending(): Boolean = vurdertePerioder.all { it.vurdering != Foreldelseperiode.Vurdering.IkkeVurdert }

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
                                is Foreldelseperiode.Vurdering.IkkeForeldet -> Foreldelsesvurderingstype.IKKE_FORELDET
                                is Foreldelseperiode.Vurdering.Foreldet -> Foreldelsesvurderingstype.FORELDET
                                is Foreldelseperiode.Vurdering.IkkeVurdert -> Foreldelsesvurderingstype.IKKE_VURDERT
                                is Foreldelseperiode.Vurdering.Tilleggsfrist -> Foreldelsesvurderingstype.TILLEGGSFRIST
                            },
                        foreldelsesfrist = (it.vurdering as? Foreldelseperiode.Vurdering.Tilleggsfrist)?.frist,
                    )
                },
        )
    }

    class Foreldelseperiode(
        val id: UUID,
        val periode: Datoperiode,
        val vurdering: Vurdering,
    ) {
        sealed interface Vurdering {
            val begrunnelse: String? get() = null
            val oppdaget: LocalDate? get() = null

            class IkkeForeldet(override val begrunnelse: String) : Vurdering

            object IkkeVurdert : Vurdering

            class Tilleggsfrist(val frist: LocalDate, override val oppdaget: LocalDate) : Vurdering

            class Foreldet(override val begrunnelse: String, override val oppdaget: LocalDate) : Vurdering
        }
    }
}
