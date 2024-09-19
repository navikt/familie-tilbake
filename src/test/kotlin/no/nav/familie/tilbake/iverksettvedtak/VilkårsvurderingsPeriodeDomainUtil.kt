package no.nav.familie.tilbake.iverksettvedtak

import no.nav.familie.kontrakter.felles.Datoperiode
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.tilbake.api.dto.AktsomhetDto
import no.nav.familie.tilbake.api.dto.SærligGrunnDto
import no.nav.familie.tilbake.api.dto.VilkårsvurderingsperiodeDto
import no.nav.familie.tilbake.vilkårsvurdering.domain.Aktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.SærligGrunn
import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingAktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingSærligGrunn
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsperiode
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsresultat
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

object VilkårsvurderingsPeriodeDomainUtil {
    fun lagGrovtUaktsomVilkårsvurderingsperiode(
        fom: YearMonth,
        tom: YearMonth,
    ) =
        VilkårsvurderingsperiodeDto(
            periode = Datoperiode(fom, tom),
            begrunnelse = "testverdi",
            aktsomhetDto =
                AktsomhetDto(
                    aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                    ileggRenter = true,
                    andelTilbakekreves = null,
                    begrunnelse = "testverdi",
                    særligeGrunnerTilReduksjon = false,
                    tilbakekrevSmåbeløp = true,
                    særligeGrunnerBegrunnelse = "testverdi",
                    særligeGrunner = listOf(SærligGrunnDto(SærligGrunn.ANNET, "testverdi")),
                ),
            vilkårsvurderingsresultat =
                Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
        )

    fun lagVilkårsvurderingsperiode(
        periode: Månedsperiode = Månedsperiode(YearMonth.of(2024, 4), YearMonth.of(2024, 5)),
        vilkårsvurderingsresultat: Vilkårsvurderingsresultat = Vilkårsvurderingsresultat.GOD_TRO,
        begrunnelse: String = "begrunnelse",
        aktsomhet: VilkårsvurderingAktsomhet = lagVilkårsvurderingAktsomhet(),
    ) =
        Vilkårsvurderingsperiode(
            id = UUID.randomUUID(),
            periode = periode,
            vilkårsvurderingsresultat = vilkårsvurderingsresultat,
            aktsomhet = aktsomhet,
            begrunnelse = begrunnelse,
        )

    fun lagVilkårsvurderingAktsomhet(andelTilbakekreves: BigDecimal = BigDecimal(50)) =
        VilkårsvurderingAktsomhet(
            id = UUID.randomUUID(),
            aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
            andelTilbakekreves = andelTilbakekreves,
            begrunnelse = "Aktsomhet begrunnelse",
            manueltSattBeløp = null,
            ileggRenter = false,
            særligeGrunnerTilReduksjon = true,
            vilkårsvurderingSærligeGrunner = setOf(VilkårsvurderingSærligGrunn(særligGrunn = SærligGrunn.HELT_ELLER_DELVIS_NAVS_FEIL, begrunnelse = "begrunnelse")),
            tilbakekrevSmåbeløp = false,
            særligeGrunnerBegrunnelse = "Særlig grunner begrunnelse",
        )
}
