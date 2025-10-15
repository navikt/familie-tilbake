package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.FrontendDto
import no.nav.tilbakekreving.api.v1.dto.Totrinnsstegsinfo
import no.nav.tilbakekreving.api.v1.dto.TotrinnsvurderingDto
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakRevurdering
import no.nav.tilbakekreving.entities.FatteVedtakStegEntity
import no.nav.tilbakekreving.entities.VurdertStegEntity
import no.nav.tilbakekreving.entities.VurdertStegType
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.saksbehandler.Behandler
import java.util.UUID

class FatteVedtakSteg internal constructor(
    private val id: UUID,
    private val vurderteSteg: List<VurdertSteg>,
    private var _ansvarligBeslutter: Behandler?,
) : Saksbehandlingsteg, FrontendDto<TotrinnsvurderingDto> {
    override val type: Behandlingssteg = Behandlingssteg.FATTE_VEDTAK
    val ansvarligBeslutter: Behandler? get() = _ansvarligBeslutter

    override fun erFullstendig(): Boolean = vurderteSteg.all { it.erFerdigvurdert() }

    override val behandlingsstatus: Behandlingsstatus = Behandlingsstatus.FATTER_VEDTAK

    override fun nullstill(
        kravgrunnlag: KravgrunnlagHendelse,
        eksternFagsakRevurdering: EksternFagsakRevurdering,
    ) {}

    internal fun håndter(
        beslutter: Behandler,
        ansvarligSaksbehandler: Behandler,
        behandlingssteg: Behandlingssteg,
        vurdering: Vurdering,
        sporing: Sporing,
    ) {
        if (ansvarligSaksbehandler == beslutter) throw ModellFeil.IngenTilgangException("Beslutter kan ikke være ansvarlig saksbehandler", sporing)
        _ansvarligBeslutter = beslutter
        vurderteSteg.single { it.erFor(behandlingssteg) }
            .oppdaterVurdering(vurdering)
    }

    override fun tilFrontendDto(): TotrinnsvurderingDto {
        return TotrinnsvurderingDto(vurderteSteg.map(VurdertSteg::tilFrontendDto))
    }

    fun tilEntity(behandlingRef: UUID): FatteVedtakStegEntity {
        return FatteVedtakStegEntity(
            id = id,
            behandlingRef = behandlingRef,
            vurderteStegEntities = vurderteSteg.map { it.tilEntity(id) },
            ansvarligBeslutter = _ansvarligBeslutter?.tilEntity(),
        )
    }

    class VurdertSteg(
        private val id: UUID,
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

        fun tilEntity(fattevedtakRef: UUID): VurdertStegEntity {
            return VurdertStegEntity(
                id = id,
                fattevedtakRef = fattevedtakRef,
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
                id = UUID.randomUUID(),
                vurderteSteg = listOf(
                    VurdertSteg(
                        id = UUID.randomUUID(),
                        steg = Behandlingssteg.FAKTA,
                        vurdering = Vurdering.IkkeVurdert,
                    ),
                    VurdertSteg(
                        id = UUID.randomUUID(),
                        steg = Behandlingssteg.FORELDELSE,
                        vurdering = Vurdering.IkkeVurdert,
                    ),
                    VurdertSteg(
                        id = UUID.randomUUID(),
                        steg = Behandlingssteg.VILKÅRSVURDERING,
                        vurdering = Vurdering.IkkeVurdert,
                    ),
                    VurdertSteg(
                        id = UUID.randomUUID(),
                        steg = Behandlingssteg.FORESLÅ_VEDTAK,
                        vurdering = Vurdering.IkkeVurdert,
                    ),
                ),
                _ansvarligBeslutter = null,
            )
        }
    }
}
