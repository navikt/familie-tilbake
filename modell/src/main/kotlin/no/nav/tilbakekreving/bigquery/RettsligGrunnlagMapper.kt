package no.nav.tilbakekreving.bigquery

import no.nav.tilbakekreving.api.v1.dto.RettsligGrunnlag
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ForårsaketAvBruker
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.NivåAvForståelse
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.AnnenVurdering
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vurdering

object RettsligGrunnlagMapper {
    fun fraVurdering(vurdering: Vurdering): RettsligGrunnlag? = when (vurdering) {
        AnnenVurdering.GOD_TRO -> RettsligGrunnlag.GOD_TRO
        NivåAvForståelse.Type.Forstod -> RettsligGrunnlag.FORSTOD
        NivåAvForståelse.Type.BurdeForstått,
        NivåAvForståelse.Type.MåForstått,
        -> RettsligGrunnlag.BURDE_FORSTÅTT
        Aktsomhet.SIMPEL_UAKTSOMHET -> RettsligGrunnlag.UAKTSOM
        Aktsomhet.GROV_UAKTSOMHET -> RettsligGrunnlag.GROVT_UAKTSOM
        Aktsomhet.FORSETT -> RettsligGrunnlag.FORSETT
        is ForårsaketAvBruker.IkkeVurdert -> null
        else -> error("Ukjent vurderingstype for BigQuery: ${vurdering.navn}")
    }

    /**
     * Gammel løsning lagrer forsto og burde forstått samlet med en aktsomhetsgrad.
     * Forsett gir FORSTOD, mens simpel og grov uaktsomhet gir BURDE_FORSTÅTT.
     * Sistnevnte inkluderer «måtte forstå», som også grupperes under BURDE_FORSTÅTT i ny løsning.
     */
    fun fraVilkårsvurderingsresultat(
        resultat: Vilkårsvurderingsresultat,
        aktsomhet: Aktsomhet?,
    ): RettsligGrunnlag? = when (resultat) {
        Vilkårsvurderingsresultat.GOD_TRO -> RettsligGrunnlag.GOD_TRO
        Vilkårsvurderingsresultat.UDEFINERT -> null
        Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT -> when (requireNotNull(aktsomhet) { "Aktsomhet mangler for vurdering av mottakers forståelse" }) {
            Aktsomhet.FORSETT -> RettsligGrunnlag.FORSTOD
            Aktsomhet.GROV_UAKTSOMHET,
            Aktsomhet.SIMPEL_UAKTSOMHET,
            -> RettsligGrunnlag.BURDE_FORSTÅTT
        }
        Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
        Vilkårsvurderingsresultat.MANGELFULLE_OPPLYSNINGER_FRA_BRUKER,
        -> fraVurdering(requireNotNull(aktsomhet) { "Aktsomhet mangler for vurdering av opplysninger fra bruker" })
    }
}
