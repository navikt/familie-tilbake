package no.nav.tilbakekreving.e2e

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.nondeterministic.eventuallyConfig
import io.kotest.common.runBlocking
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.april
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator.NyKlassekode
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator.Tilbakekrevingsbeløp
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator.Tilbakekrevingsbeløp.Companion.medFeilutbetaling
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator.Tilbakekrevingsperiode
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagBufferRepository
import no.nav.tilbakekreving.mai
import no.nav.tilbakekreving.mars
import no.nav.tilbakekreving.util.kroner
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

class KravgrunnlagE2ETest : TilbakekrevingE2EBase() {
    @Autowired
    private lateinit var kravgrunnlagBufferRepository: KravgrunnlagBufferRepository

    @Autowired
    private lateinit var tilbakekrevingService: TilbakekrevingService

    @Test
    fun `kan lese kravgrunnlag for tilleggstønader`() {
        val fagsystemId = UUID.randomUUID().toString()
        sendMessage(
            "LOCAL_TILLEGGSSTONADER.KRAVGRUNNLAG",
            KravgrunnlagGenerator.forTillegstønader(
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

        runBlocking {
            eventually(
                eventuallyConfig {
                    duration = 1000.milliseconds
                    interval = 10.milliseconds
                },
            ) {
                kravgrunnlagBufferRepository.hent().size shouldBe 1
            }
        }

        tilbakekrevingService.lesKravgrunnlag()

        kravgrunnlagBufferRepository.hentUlesteKravgrunnlag().size shouldBe 0

        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId)
        tilbakekreving.shouldNotBeNull()
    }
}
