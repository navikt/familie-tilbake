package no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering

import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingsresultatDto
import no.nav.tilbakekreving.beregning.Reduksjon
import no.nav.tilbakekreving.breeeev.PåkrevdBegrunnelse
import no.nav.tilbakekreving.endring.VurdertUtbetaling
import no.nav.tilbakekreving.entities.AktsomhetsvurderingEntity
import no.nav.tilbakekreving.entities.VurderingType
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vurdering
import java.util.UUID

interface ForårsaketAvBruker {
    val begrunnelse: String?

    fun renter(): Boolean

    fun reduksjon(): Reduksjon

    fun vurderingstype(): Vurdering

    fun tilFrontendDto(): VurdertVilkårsvurderingsresultatDto?

    fun tilEntity(periodeRef: UUID): AktsomhetsvurderingEntity

    fun oppsummerVurdering(): VurdertUtbetaling.Vilkårsvurdering

    fun påkrevdeVurderinger(): Set<PåkrevdBegrunnelse>

    sealed interface Ja : ForårsaketAvBruker

    sealed interface Nei : ForårsaketAvBruker

    data object IkkeVurdert : ForårsaketAvBruker, Vurdering {
        override val begrunnelse: String? = null

        override fun vurderingstype(): Vurdering = this

        override fun renter(): Boolean = false

        override fun reduksjon(): Reduksjon = Reduksjon.FullstendigTilbakekreving()

        override fun tilFrontendDto(): VurdertVilkårsvurderingsresultatDto? = null

        override fun oppsummerVurdering(): VurdertUtbetaling.Vilkårsvurdering {
            throw NotImplementedError("Kan ikke lage statistikk for en uvurdert utbetaling")
        }

        override fun påkrevdeVurderinger(): Set<PåkrevdBegrunnelse> = emptySet()

        override val navn: String
            get() = "IkkeVurdert"

        override fun tilEntity(periodeRef: UUID): AktsomhetsvurderingEntity {
            return AktsomhetsvurderingEntity(
                vurderingType = VurderingType.IKKE_VURDERT,
                begrunnelse = null,
                beløpIBehold = null,
                aktsomhet = null,
                feilaktigEllerMangelfull = null,
            )
        }
    }
}
