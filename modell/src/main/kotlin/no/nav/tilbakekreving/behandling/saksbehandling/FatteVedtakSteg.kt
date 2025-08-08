package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.FrontendDto
import no.nav.tilbakekreving.api.v1.dto.Totrinnsstegsinfo
import no.nav.tilbakekreving.api.v1.dto.TotrinnsvurderingDto
import no.nav.tilbakekreving.entities.FatteVedtakStegEntity
import no.nav.tilbakekreving.entities.VurdertStegEntity
import no.nav.tilbakekreving.entities.VurdertStegType
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.saksbehandler.Behandler

class FatteVedtakSteg internal constructor(
    private val vurderteSteg: List<VurdertSteg>,
    private var _ansvarligBeslutter: Behandler?,
) : Saksbehandlingsteg<TotrinnsvurderingDto> {
    override val type: Behandlingssteg = Behandlingssteg.FATTE_VEDTAK
    val ansvarligBeslutter: Behandler? = _ansvarligBeslutter

    override fun erFullstending(): Boolean = vurderteSteg.all { it.erFerdigvurdert() }

    internal fun håndter(
        beslutter: Behandler,
        ansvarligSaksbehandler: Behandler,
        behandlingssteg: Behandlingssteg,
        vurdering: Vurdering,
        sporing: Sporing,
    ) {
        if (ansvarligSaksbehandler == beslutter) ModellFeil.IngenTilgangException("Beslutter kan ikke være ansvarlig saksbehandler", sporing)
        _ansvarligBeslutter = beslutter
        vurderteSteg.single { it.erFor(behandlingssteg) }
            .oppdaterVurdering(vurdering)
    }

    override fun tilFrontendDto(): TotrinnsvurderingDto {
        return TotrinnsvurderingDto(vurderteSteg.map(VurdertSteg::tilFrontendDto))
    }

    fun tilEntity(): FatteVedtakStegEntity {
        return FatteVedtakStegEntity(
            vurderteStegEntities = vurderteSteg.map { it.tilEntity() },
            ansvarligBeslutter = _ansvarligBeslutter?.tilEntity(),
        )
    }

    class VurdertSteg(
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

        fun tilEntity(): VurdertStegEntity {
            return VurdertStegEntity(
                steg = steg,
                vurdering = when (this.vurdering) {
                    is Vurdering.IkkeVurdert -> VurdertStegType.IKKE_VURDERT
                    is Vurdering.Godkjent -> VurdertStegType.GODKJENT
                    is Vurdering.Underkjent -> VurdertStegType.UNDERKJENT
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
        fun opprett(): FatteVedtakSteg {
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
            )
        }
    }
}
