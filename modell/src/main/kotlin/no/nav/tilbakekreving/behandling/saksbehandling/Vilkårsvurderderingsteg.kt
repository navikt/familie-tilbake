package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.api.v1.dto.AktivitetDto
import no.nav.tilbakekreving.api.v1.dto.RedusertBeløpDto
import no.nav.tilbakekreving.api.v1.dto.VurdertAktsomhetDto
import no.nav.tilbakekreving.api.v1.dto.VurdertGodTroDto
import no.nav.tilbakekreving.api.v1.dto.VurdertSærligGrunnDto
import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingDto
import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingsperiodeDto
import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingsresultatDto
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunn
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class Vilkårsvurderderingsteg : Saksbehandlingsteg<VurdertVilkårsvurderingDto> {
    override val type: Behandlingssteg = Behandlingssteg.GRUNNLAG

    override fun erFullstending(): Boolean = true

    override fun tilFrontendDto(): VurdertVilkårsvurderingDto {
        return VurdertVilkårsvurderingDto(
            perioder =
                listOf(
                    VurdertVilkårsvurderingsperiodeDto(
                        periode = Datoperiode(LocalDate.now().minusDays(31), LocalDate.now().minusDays(15)),
                        feilutbetaltBeløp = BigDecimal("0.0"),
                        hendelsestype = Hendelsestype.SATSER,
                        reduserteBeløper =
                            listOf(
                                RedusertBeløpDto(
                                    trekk = true,
                                    beløp = BigDecimal("0.0"),
                                ),
                            ),
                        aktiviteter = emptyList(),
                        vilkårsvurderingsresultatInfo = null,
                        begrunnelse = null,
                        foreldet = false,
                    ),
                    VurdertVilkårsvurderingsperiodeDto(
                        periode =
                            Datoperiode(
                                fom = LocalDate.now().minusDays(14),
                                tom = LocalDate.now(),
                            ),
                        feilutbetaltBeløp = BigDecimal("0.0"),
                        hendelsestype = Hendelsestype.SATSER,
                        reduserteBeløper =
                            listOf(
                                RedusertBeløpDto(
                                    trekk = true,
                                    beløp = BigDecimal("0.0"),
                                ),
                            ),
                        aktiviteter =
                            listOf(
                                AktivitetDto(
                                    aktivitet = "",
                                    beløp = BigDecimal("0.0"),
                                ),
                            ),
                        vilkårsvurderingsresultatInfo =
                            VurdertVilkårsvurderingsresultatDto(
                                vilkårsvurderingsresultat = Vilkårsvurderingsresultat.GOD_TRO,
                                godTro =
                                    VurdertGodTroDto(
                                        beløpErIBehold = true,
                                        beløpTilbakekreves = BigDecimal("0.0"),
                                        begrunnelse = "",
                                    ),
                                aktsomhet =
                                    VurdertAktsomhetDto(
                                        aktsomhet = Aktsomhet.FORSETT,
                                        ileggRenter = false,
                                        andelTilbakekreves = BigDecimal("1.0"),
                                        beløpTilbakekreves = BigDecimal("0.0"),
                                        begrunnelse = "",
                                        særligeGrunner =
                                            listOf(
                                                VurdertSærligGrunnDto(
                                                    SærligGrunn.HELT_ELLER_DELVIS_NAVS_FEIL,
                                                    begrunnelse = "",
                                                ),
                                            ),
                                    ),
                            ),
                        begrunnelse = "",
                        foreldet = false,
                    ),
                ),
            rettsgebyr = 0,
            opprettetTid = LocalDateTime.now(),
        )
    }
}
