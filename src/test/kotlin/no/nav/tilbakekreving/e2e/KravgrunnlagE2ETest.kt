package no.nav.tilbakekreving.e2e

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.tilbakekreving.TilbakekrevingRepository
import no.nav.tilbakekreving.april
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator.NyKlassekode
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator.Tilbakekrevingsbeløp
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator.Tilbakekrevingsbeløp.Companion.medFeilutbetaling
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator.Tilbakekrevingsperiode
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagMediator
import no.nav.tilbakekreving.mai
import no.nav.tilbakekreving.mars
import no.nav.tilbakekreving.util.kroner
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class KravgrunnlagE2ETest : TilbakekrevingE2EBase() {
    @Autowired
    private lateinit var tilbakekrevingRepository: TilbakekrevingRepository

    @Autowired
    private lateinit var kravgrunnlagMediator: KravgrunnlagMediator

    @Test
    fun `kan lese kravgrunnlag for tilleggsstønader`() {
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
        tilbakekreving.shouldNotBeNull()
    }

    @Test
    fun `lagrer bare en gang dersom noe feiler under håndtering av kravgrunnlag`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(QUEUE_NAME, KravgrunnlagGenerator.forTilleggsstønader(fagsystemId = fagsystemId, fødselsnummer = "feil1234567"))

        kravgrunnlagMediator.lesKravgrunnlag()

        tilbakekrevingRepository.hentAlleTilbakekrevinger()?.count { it.eksternFagsak.eksternId == fagsystemId } shouldBe 1
    }

    @Test
    fun `flere konsumenter leser fra tabellen samtidig`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlag(QUEUE_NAME, KravgrunnlagGenerator.forTilleggsstønader(fagsystemId = fagsystemId, fødselsnummer = "sleepy12345"))

        runBlocking(Dispatchers.IO) {
            (0..4).map {
                launch { kravgrunnlagMediator.lesKravgrunnlag() }
            }.joinAll()
        }

        tilbakekrevingRepository.hentAlleTilbakekrevinger()?.count { it.eksternFagsak.eksternId == fagsystemId } shouldBe 1
    }

    companion object {
        const val QUEUE_NAME = "LOCAL_TILLEGGSSTONADER.KRAVGRUNNLAG"
    }
}
