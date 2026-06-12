package no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering

import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingsresultatDto
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.Vilkårsvurderingsteg.Vilkårsvurderingsperiode
import no.nav.tilbakekreving.beregning.Reduksjon
import no.nav.tilbakekreving.breeeev.begrunnelse.VilkårsvurderingBegrunnelse
import no.nav.tilbakekreving.endring.VurdertUtbetaling
import no.nav.tilbakekreving.entities.AktsomhetsvurderingEntity
import no.nav.tilbakekreving.entities.VurderingType
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vurdering
import java.util.UUID

interface ForårsaketAvBruker {
    fun underliggendeVurdering(): ForårsaketAvBruker = this

    val begrunnelse: String?

    fun renter(): Boolean

    fun reduksjon(): Reduksjon

    fun vurderingstype(): Vurdering

    fun tilFrontendDto(): VurdertVilkårsvurderingsresultatDto?

    fun tilEntity(periodeRef: UUID): AktsomhetsvurderingEntity

    fun oppsummerVurdering(): VurdertUtbetaling.Vilkårsvurdering

    fun påkrevdeVurderinger(): Set<VilkårsvurderingBegrunnelse>

    sealed interface Ja : ForårsaketAvBruker

    sealed interface Nei : ForårsaketAvBruker

    class IkkeVurdert : ForårsaketAvBruker, Vurdering {
        override val begrunnelse: String? = null

        override fun vurderingstype(): Vurdering = this

        override fun renter(): Boolean = false

        override fun reduksjon(): Reduksjon = Reduksjon.FullstendigTilbakekreving()

        override fun tilFrontendDto(): VurdertVilkårsvurderingsresultatDto? = null

        override fun oppsummerVurdering(): VurdertUtbetaling.Vilkårsvurdering {
            throw NotImplementedError("Kan ikke lage statistikk for en uvurdert utbetaling")
        }

        override fun påkrevdeVurderinger(): Set<VilkårsvurderingBegrunnelse> = emptySet()

        override val navn: String
            get() = "IkkeVurdert"

        override fun tilEntity(periodeRef: UUID): AktsomhetsvurderingEntity {
            return AktsomhetsvurderingEntity(
                vurderingType = VurderingType.IKKE_VURDERT,
                mottakersForståelse = null,
                begrunnelse = null,
                beløpIBehold = null,
                aktsomhet = null,
                kanUnnlates = null,
                særligGrunner = null,
                feilaktigEllerMangelfull = null,
                forrigePeriodeId = null,
            )
        }
    }

    class KopiertVurdering(
        private val forrigeVurdering: Vilkårsvurderingsperiode,
        val forrigePeriodeId: UUID?,
    ) : ForårsaketAvBruker by forrigeVurdering.vurdering {
        override fun underliggendeVurdering(): ForårsaketAvBruker {
            return forrigeVurdering.vurdering.underliggendeVurdering()
        }

        override fun tilEntity(periodeRef: UUID): AktsomhetsvurderingEntity {
            return AktsomhetsvurderingEntity(
                vurderingType = VurderingType.KOPIERT_VURDERING,
                mottakersForståelse = null,
                begrunnelse = null,
                beløpIBehold = null,
                aktsomhet = null,
                kanUnnlates = null,
                særligGrunner = null,
                feilaktigEllerMangelfull = null,
                forrigePeriodeId = forrigePeriodeId,
            )
        }
    }
}
