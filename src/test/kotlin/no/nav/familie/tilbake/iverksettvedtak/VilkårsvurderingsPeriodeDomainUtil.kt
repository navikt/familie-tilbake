package no.nav.familie.tilbake.iverksettvedtak

import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingAktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingGodTro
import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingSærligGrunn
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsperiode
import no.nav.tilbakekreving.api.v1.dto.AktsomhetDto
import no.nav.tilbakekreving.api.v1.dto.SkalUnnlates
import no.nav.tilbakekreving.api.v1.dto.SærligGrunnDto
import no.nav.tilbakekreving.api.v1.dto.VilkårsvurderingsperiodeDto
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.periode.Månedsperiode
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunnType
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

object VilkårsvurderingsPeriodeDomainUtil {
    fun lagGrovtUaktsomVilkårsvurderingsperiode(
        fom: YearMonth,
        tom: YearMonth,
    ) = VilkårsvurderingsperiodeDto(
        periode = Datoperiode(fom, tom),
        begrunnelse = "testverdi",
        aktsomhetDto =
            AktsomhetDto(
                aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                ileggRenter = true,
                andelTilbakekreves = null,
                begrunnelse = "testverdi",
                særligeGrunnerTilReduksjon = false,
                unnlates4Rettsgebyr = SkalUnnlates.TILBAKEKREVES,
                særligeGrunnerBegrunnelse = "testverdi",
                særligeGrunner = listOf(SærligGrunnDto(SærligGrunnType.ANNET, "testverdi")),
            ),
        vilkårsvurderingsresultat =
            Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
    )

    fun lagVilkårsvurderingsperiode(
        periode: Månedsperiode = Månedsperiode(YearMonth.of(2024, 4), YearMonth.of(2024, 5)),
        vilkårsvurderingsresultat: Vilkårsvurderingsresultat = Vilkårsvurderingsresultat.GOD_TRO,
        begrunnelse: String = "begrunnelse",
        aktsomhet: VilkårsvurderingAktsomhet = lagVilkårsvurderingAktsomhet(),
        godTro: VilkårsvurderingGodTro = lagVilkårsvurderingGodTro(),
    ) = Vilkårsvurderingsperiode(
        id = UUID.randomUUID(),
        periode = periode,
        vilkårsvurderingsresultat = vilkårsvurderingsresultat,
        aktsomhet = aktsomhet,
        begrunnelse = begrunnelse,
        godTro = godTro,
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
            vilkårsvurderingSærligeGrunner = setOf(VilkårsvurderingSærligGrunn(særligGrunn = SærligGrunnType.HELT_ELLER_DELVIS_NAVS_FEIL, begrunnelse = "begrunnelse")),
            tilbakekrevSmåbeløp = false,
            særligeGrunnerBegrunnelse = "Særlig grunner begrunnelse",
        )

    fun lagVilkårsvurderingGodTro(beløpTilbakekreves: BigDecimal = BigDecimal(1234)) =
        VilkårsvurderingGodTro(
            id = UUID.randomUUID(),
            beløpErIBehold = true,
            beløpTilbakekreves = beløpTilbakekreves,
            begrunnelse = "God tro begrunnelse",
        )
}
