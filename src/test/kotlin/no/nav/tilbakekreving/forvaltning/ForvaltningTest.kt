package no.nav.tilbakekreving.forvaltning

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.april
import no.nav.tilbakekreving.e2e.KravgrunnlagE2ETest.Companion.QUEUE_NAME
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator.NyKlassekode
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator.Tilbakekrevingsbeløp
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator.Tilbakekrevingsbeløp.Companion.medFeilutbetaling
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator.Tilbakekrevingsperiode
import no.nav.tilbakekreving.e2e.TilbakekrevingE2EBase
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.mai
import no.nav.tilbakekreving.mars
import no.nav.tilbakekreving.util.kroner
import org.junit.jupiter.api.Test
import java.util.UUID

class ForvaltningTest : TilbakekrevingE2EBase() {
    @Test
    fun `hentBehandlingsinfo skal hente info basert på eksternFagsakId og ytelsestype for tilleggsstønader`() {
        val fagsystemId = UUID.randomUUID().toString()
        sendKravgrunnlagOgAvventLesing(
            QUEUE_NAME,
            KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
                perioder = listOf(
                    Tilbakekrevingsperiode(
                        periode = 3.mars(2025) til 3.mars(2025),
                        tilbakekrevingsbeløp = listOf(
                            Tilbakekrevingsbeløp.forKlassekode(
                                klassekode = NyKlassekode.TSTBASISP4_OP,
                                beløpTilbakekreves = 4016.kroner,
                                beløpOpprinneligUtbetalt = 4217.kroner,
                            ),
                        ).medFeilutbetaling(NyKlassekode.KL_KODE_FEIL_ARBYT),
                    ),
                    Tilbakekrevingsperiode(
                        periode = 1.april(2025) til 1.april(2025),
                        tilbakekrevingsbeløp = listOf(
                            Tilbakekrevingsbeløp.forKlassekode(
                                klassekode = NyKlassekode.TSTBASISP4_OP,
                                beløpTilbakekreves = 4418.kroner,
                                beløpOpprinneligUtbetalt = 4418.kroner,
                            ),
                        ).medFeilutbetaling(NyKlassekode.KL_KODE_FEIL_ARBYT),
                    ),
                    Tilbakekrevingsperiode(
                        periode = 1.mai(2025) til 1.mai(2025),
                        tilbakekrevingsbeløp = listOf(
                            Tilbakekrevingsbeløp.forKlassekode(
                                klassekode = NyKlassekode.TSTBASISP4_OP,
                                beløpTilbakekreves = 4418.kroner,
                                beløpOpprinneligUtbetalt = 4418.kroner,
                            ),
                        ).medFeilutbetaling(NyKlassekode.KL_KODE_FEIL_ARBYT),
                    ),
                ),
            ),
        )
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId)
        val behandling = tilbakekreving?.behandlingHistorikk?.nåværende()?.entry
        val kravgrunnlag = tilbakekreving?.kravgrunnlagHistorikk?.nåværende()?.entry

        val behandlingInfo = tilbakekrevingService.hentBehandlingsinfo(tilbakekreving!!)

        behandlingInfo.first().behandlingId.shouldBe(behandling?.id)
        behandlingInfo.first().eksternId.shouldBe(kravgrunnlag?.referanse)
    }
}
