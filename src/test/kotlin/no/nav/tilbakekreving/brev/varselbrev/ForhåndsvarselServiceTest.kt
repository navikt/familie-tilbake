package no.nav.tilbakekreving.brev.varselbrev

import io.kotest.inspectors.forOne
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator.Tilbakekrevingsbeløp.Companion.medFeilutbetaling
import no.nav.tilbakekreving.e2e.TilbakekrevingE2EBase
import no.nav.tilbakekreving.e2e.ytelser.TilleggsstønaderE2ETest.Companion.TILLEGGSSTØNADER_KØ_NAVN
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.util.kroner
import org.junit.jupiter.api.Test

class ForhåndsvarselServiceTest : TilbakekrevingE2EBase() {
    @Test
    fun `henter tekster til varselbrev`() {
        val fnr = "12312312311"
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        val vedtakId = KravgrunnlagGenerator.nextPaddedId(6)
        val ansvarligEnhet = KravgrunnlagGenerator.nextPaddedId(4)
        sendKravgrunnlagOgAvventLesing(
            TILLEGGSSTØNADER_KØ_NAVN,
            KravgrunnlagGenerator.forTilleggsstønader(
                fødselsnummer = fnr,
                fagsystemId = fagsystemId,
                vedtakId = vedtakId,
                ansvarligEnhet = ansvarligEnhet,
                perioder = listOf(
                    KravgrunnlagGenerator.Tilbakekrevingsperiode(
                        1.januar(2021) til 1.januar(2021),
                        tilbakekrevingsbeløp = listOf(
                            KravgrunnlagGenerator.Tilbakekrevingsbeløp.forKlassekode(
                                klassekode = KravgrunnlagGenerator.NyKlassekode.TSTBASISP4_OP,
                                beløpTilbakekreves = 2000.kroner,
                                beløpOpprinneligUtbetalt = 20000.kroner,
                            ),
                        ).medFeilutbetaling(KravgrunnlagGenerator.NyKlassekode.KL_KODE_FEIL_ARBYT),
                    ),
                ),
            ),
        )
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()

        val tekster = forhåndsvarselService.hentVarselbrevTekster(tilbakekreving)

        tekster.shouldNotBeNull()
        tekster.shouldNotBeEmpty()
        tekster.get(0).title shouldBe "overskrift"
        tekster.get(0).body shouldContain "Nav vurderer om du må betale tilbake"
        tekster.forOne {
            it.title shouldBe "Dette har skjedd"
            it.body shouldContain "og endringen har ført til at du har fått utbetalt for mye."
        }
        tekster.forOne {
            it.title shouldBe "Dette legger vi vekt på i vurderingen vår"
            it.body shouldContain "For å avgjøre om vi kan kreve tilbake,"
        }
        tekster.forOne {
            it.title shouldBe "Slik uttaler du deg"
            it.body shouldContain "Du kan sende uttalelsen din ved å logge deg inn på"
        }
        tekster.forOne {
            it.title shouldBe "Har du spørsmål?"
            it.body shouldContain "Du finner mer informasjon på nav.no/tilleggsstonad."
        }
        tekster.forOne {
            it.title shouldBe "Du har rett til innsyn"
            it.body shouldContain "På nav.no/dittnav kan du se dokumentene i saken din"
        }
    }
}
