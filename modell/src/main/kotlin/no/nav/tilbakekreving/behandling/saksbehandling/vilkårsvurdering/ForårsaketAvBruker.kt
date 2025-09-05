package no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering

import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingsresultatDto
import no.nav.tilbakekreving.beregning.Reduksjon
import no.nav.tilbakekreving.entities.AktsomhetsvurderingEntity
import no.nav.tilbakekreving.entities.VurderingType
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vurdering

interface ForårsaketAvBruker {
    val begrunnelse: String

    fun renter(): Boolean

    fun reduksjon(): Reduksjon

    fun vurderingstype(): Vurdering

    fun tilFrontendDto(): VurdertVilkårsvurderingsresultatDto?

    fun tilEntity(): AktsomhetsvurderingEntity

    sealed interface Ja : ForårsaketAvBruker

    sealed interface Nei : ForårsaketAvBruker {
        override fun renter(): Boolean = false
    }

    data object IkkeVurdert : ForårsaketAvBruker, Vurdering {
        override val begrunnelse: String = ""

        override fun vurderingstype(): Vurdering = this

        override fun renter(): Boolean = false

        override fun reduksjon(): Reduksjon = Reduksjon.FullstendigRefusjon()

        override fun tilFrontendDto(): VurdertVilkårsvurderingsresultatDto? = null

        override val navn: String
            get() = "IkkeVurdert"

        override fun tilEntity(): AktsomhetsvurderingEntity {
            return AktsomhetsvurderingEntity(
                VurderingType.IKKE_VURDERT,
                begrunnelse = null,
                beløpIBehold = null,
                aktsomhet = null,
            )
        }
    }
}
