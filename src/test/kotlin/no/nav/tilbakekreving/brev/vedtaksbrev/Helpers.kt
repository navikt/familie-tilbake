package no.nav.tilbakekreving.brev.vedtaksbrev

import no.nav.tilbakekreving.breeeev.BegrunnetPeriode
import no.nav.tilbakekreving.breeeev.Signatur
import no.nav.tilbakekreving.breeeev.VedtaksbrevInfo
import no.nav.tilbakekreving.breeeev.begrunnelse.VilkårsvurderingBegrunnelse
import no.nav.tilbakekreving.breeeev.standardtekster.HjemmelForTilbakekreving
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kontrakter.frontend.models.BeregningsresultatVurderingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.BeregningsresultatsperiodeDto
import no.nav.tilbakekreving.kontrakter.frontend.models.BrevmottakerDto
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.test.januar
import java.util.UUID

private val brevmottaker = BrevmottakerDto(
    navn = "Toasty Testy",
    personIdent = "20046912345",
)

fun vedtaksbrevInfo(
    periodeId: UUID = UUID.randomUUID(),
    vararg påkrevdeBegrunnelser: VilkårsvurderingBegrunnelse = arrayOf(VilkårsvurderingBegrunnelse.IKKE_REDUSERT_SÆRLIGE_GRUNNER),
    skalTilbakerkeves: Boolean = true,
) = VedtaksbrevInfo(
    brukerdata = brevmottaker,
    ytelse = Ytelse.Arbeidsavklaringspenger.brevmeta(),
    signatur = Signatur(
        ansvarligSaksbehandlerIdent = "Z999999",
        ansvarligBeslutterIdent = null,
        ansvarligEnhet = "NAV Arbeid og Ytelser",
    ),
    perioder = listOf(
        BegrunnetPeriode(
            id = periodeId,
            periode = 1.januar(2021) til 31.januar(2021),
            meldingerTilSaksbehandler = emptySet(),
            påkrevdeVurderinger = påkrevdeBegrunnelser.toSet(),
        ),
    ),
    bunntekster = emptySet(),
    skalTilbakekreves = skalTilbakerkeves,
    tilbakekrevingId = UUID.randomUUID().toString(),
    beregningsresultat = listOf(
        BeregningsresultatsperiodeDto(
            fom = 1.januar(2021),
            tom = 31.januar(2021),
            vurdering = BeregningsresultatVurderingDto.GodTro,
            feilutbetaltBeløp = 4000,
            andelAvBeløp = 0,
            renteprosent = 0,
            tilbakekrevingsbeløp = 4000,
            tilbakekrevesBeløpEtterSkatt = 4000,
        ),
    ),
    hjemlerForTilbakekreving = listOf(HjemmelForTilbakekreving.FOLKETRYGDLOVEN_22_15),
)
