package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.FrontendDto
import no.nav.tilbakekreving.api.v1.dto.Totrinnsstegsinfo
import no.nav.tilbakekreving.api.v1.dto.TotrinnsvurderingDto
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.saksbehandler.Behandler
import no.nav.tilbakekreving.saksbehandler.Saksbehandling

class FatteVedtakSteg private constructor(
    private val vurderteSteg: List<VurdertSteg>,
    private var _ansvarligBeslutter: Behandler?,
    private val saksbehandling: Saksbehandling,
) : Saksbehandlingsteg<TotrinnsvurderingDto> {
    override val type: Behandlingssteg = Behandlingssteg.FATTE_VEDTAK
    val ansvarligBeslutter: Behandler? = _ansvarligBeslutter

    override fun erFullstending(): Boolean = vurderteSteg.all { it.erFerdigvurdert() }

    internal fun håndter(
        beslutter: Behandler,
        behandlingssteg: Behandlingssteg,
        vurdering: Vurdering,
    ) {
        if (saksbehandling.ansvarligSaksbehandler() == beslutter) error("Beslutter kan ikke være ansvarlig saksbehandler")
        _ansvarligBeslutter = beslutter
        vurderteSteg.single { it.erFor(behandlingssteg) }
            .oppdaterVurdering(vurdering)
    }

    override fun tilFrontendDto(): TotrinnsvurderingDto {
        return TotrinnsvurderingDto(vurderteSteg.map(VurdertSteg::tilFrontendDto))
    }

    private class VurdertSteg(
        private val steg: Behandlingssteg,
        private var vurdering: Vurdering,
    ) : FrontendDto<Totrinnsstegsinfo> {
        fun erFor(steg: Behandlingssteg): Boolean {
            return this.steg == steg
        }

        fun oppdaterVurdering(vurdering: Vurdering) {
            this.vurdering = vurdering
        }

        fun erFerdigvurdert(): Boolean {
            return vurdering !is Vurdering.IkkeVurdert
        }

        override fun tilFrontendDto(): Totrinnsstegsinfo {
            return Totrinnsstegsinfo(
                behandlingssteg = steg,
                godkjent = when (vurdering) {
                    Vurdering.Godkjent -> true
                    Vurdering.IkkeVurdert -> null
                    is Vurdering.Underkjent -> false
                },
                begrunnelse = (vurdering as? Vurdering.Underkjent)?.begrunnelse,
            )
        }
    }

    sealed interface Vurdering {
        data object IkkeVurdert : Vurdering

        data object Godkjent : Vurdering

        class Underkjent(val begrunnelse: String) : Vurdering
    }

    companion object {
        fun opprett(saksbehandling: Saksbehandling): FatteVedtakSteg {
            return FatteVedtakSteg(
                vurderteSteg = listOf(
                    VurdertSteg(
                        Behandlingssteg.FAKTA,
                        Vurdering.IkkeVurdert,
                    ),
                    VurdertSteg(
                        Behandlingssteg.FORELDELSE,
                        Vurdering.IkkeVurdert,
                    ),
                    VurdertSteg(
                        Behandlingssteg.VILKÅRSVURDERING,
                        Vurdering.IkkeVurdert,
                    ),
                    VurdertSteg(
                        Behandlingssteg.FORESLÅ_VEDTAK,
                        Vurdering.IkkeVurdert,
                    ),
                ),
                _ansvarligBeslutter = null,
                saksbehandling = saksbehandling,
            )
        }
    }
}
