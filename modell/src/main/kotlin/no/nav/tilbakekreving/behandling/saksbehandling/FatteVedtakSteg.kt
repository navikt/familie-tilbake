package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.FrontendDto
import no.nav.tilbakekreving.api.v1.dto.Totrinnsstegsinfo
import no.nav.tilbakekreving.api.v1.dto.TotrinnsvurderingDto
import no.nav.tilbakekreving.entities.FatteVedtakStegEntity
import no.nav.tilbakekreving.entities.VurdertStegEntity
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.saksbehandler.Behandler

class FatteVedtakSteg private constructor(
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
    ) {
        if (ansvarligSaksbehandler == beslutter) error("Beslutter kan ikke være ansvarlig saksbehandler")
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

        fun tilEntity(): VurdertStegEntity {
            return VurdertStegEntity(
                steg = steg,
                vurdering = when (this.vurdering) {
                    is Vurdering.IkkeVurdert -> "Ikke Vurdert"
                    is Vurdering.Godkjent -> "Godkjent"
                    is Vurdering.Underkjent -> "Underkjent"
                },
                begrunnelse = (vurdering as? Vurdering.Underkjent)?.begrunnelse,
            )
        }

        companion object {
            fun fraEntity(entity: VurdertStegEntity): VurdertSteg {
                val vurdering = when {
                    entity.vurdering.equals("Ikke Vurdert") -> Vurdering.IkkeVurdert
                    entity.vurdering.equals("Godkjent") -> Vurdering.Godkjent
                    entity.vurdering.equals("Underkjent") -> Vurdering.Underkjent(entity.begrunnelse!!)
                    else -> throw IllegalArgumentException("Ugyldig vurdering ${entity.vurdering}")
                }
                return VurdertSteg(
                    steg = entity.steg,
                    vurdering = vurdering,
                )
            }
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

        fun fraEntity(entity: FatteVedtakStegEntity): FatteVedtakSteg {
            return FatteVedtakSteg(
                vurderteSteg = entity.vurderteStegEntities.map { VurdertSteg.fraEntity(it) },
                _ansvarligBeslutter = entity.ansvarligBeslutter?.fraEntity(),
            )
        }
    }
}
