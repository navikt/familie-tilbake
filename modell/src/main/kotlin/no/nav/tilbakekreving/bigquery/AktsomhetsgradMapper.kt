package no.nav.tilbakekreving.bigquery

import no.nav.tilbakekreving.api.v1.dto.Aktsomhetsgrad
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ForårsaketAvBruker
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.NivåAvForståelse
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.AnnenVurdering
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vurdering

object AktsomhetsgradMapper {
    fun fraVurdering(vurdering: Vurdering): Aktsomhetsgrad? = when (vurdering) {
        AnnenVurdering.GOD_TRO -> Aktsomhetsgrad.GOD_TRO
        NivåAvForståelse.Type.Forstod -> Aktsomhetsgrad.FORSTOD
        NivåAvForståelse.Type.BurdeForstått,
        NivåAvForståelse.Type.MåForstått,
        -> Aktsomhetsgrad.BURDE_FORSTÅTT
        Aktsomhet.SIMPEL_UAKTSOMHET -> Aktsomhetsgrad.UAKTSOM
        Aktsomhet.GROV_UAKTSOMHET -> Aktsomhetsgrad.GROVT_UAKTSOM
        Aktsomhet.FORSETT -> Aktsomhetsgrad.FORSETT
        is ForårsaketAvBruker.IkkeVurdert -> null
        else -> error("Ukjent vurderingstype for BigQuery: ${vurdering.navn}")
    }

    fun fraVilkårsvurderingsresultat(
        resultat: Vilkårsvurderingsresultat,
        aktsomhet: Aktsomhet?,
    ): Aktsomhetsgrad? = when (resultat) {
        Vilkårsvurderingsresultat.GOD_TRO -> Aktsomhetsgrad.GOD_TRO
        Vilkårsvurderingsresultat.UDEFINERT -> null
        Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT -> when (requireNotNull(aktsomhet) { "Aktsomhet mangler for vurdering av mottakers forståelse" }) {
            Aktsomhet.FORSETT -> Aktsomhetsgrad.FORSTOD
            Aktsomhet.GROV_UAKTSOMHET,
            Aktsomhet.SIMPEL_UAKTSOMHET,
            -> Aktsomhetsgrad.BURDE_FORSTÅTT
        }
        Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
        Vilkårsvurderingsresultat.MANGELFULLE_OPPLYSNINGER_FRA_BRUKER,
        -> fraVurdering(requireNotNull(aktsomhet) { "Aktsomhet mangler for vurdering av opplysninger fra bruker" })
    }
}
