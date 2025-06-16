package no.nav.tilbakekreving.e2e.ytelser

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.config.PdlClientMock
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator.Tilbakekrevingsbeløp.Companion.medFeilutbetaling
import no.nav.tilbakekreving.e2e.TilbakekrevingE2EBase
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.util.kroner
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.random.Random

class TilleggstønaderE2ETest : TilbakekrevingE2EBase() {
    @Test
    fun `kravgrunnlag fører til sak klar til behandling`() {
        val fnr = Random.nextLong(0, 31129999999).toString().padStart(11, '0')
        val fagsystemId = UUID.randomUUID().toString()
        sendKravgrunnlagOgAvventLesing(
            "LOCAL_TILLEGGSSTONADER.KRAVGRUNNLAG",
            KravgrunnlagGenerator.forTillegstønader(
                fødselsnummer = fnr,
                fagsystemId = fagsystemId,
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

        pdlClient.hentPersoninfoHits shouldBe listOf(
            PdlClientMock.PersoninfoHit(
                ident = fnr,
                fagsystem = FagsystemDTO.TS,
            ),
        )

        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        val frontendDto = tilbakekreving.tilFrontendDto()
        frontendDto.behandlinger shouldHaveSize 1
        frontendDto.behandlinger.single().status shouldBe Behandlingsstatus.UTREDES
    }
}
