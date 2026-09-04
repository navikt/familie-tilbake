package no.nav.tilbakekreving.behandling.saksbehandling

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.SystemKlokke
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagSammenligning
import no.nav.tilbakekreving.test.januar
import org.junit.jupiter.api.Test
import java.util.UUID

class ForeslåVedtakStegTest {
    @Test
    fun `kan sende vedtak til godkjenning`() {
        val foreslåVedtakSteg = ForeslåVedtakSteg.opprett()
        foreslåVedtakSteg.erFullstendig(SystemKlokke) shouldBe false

        foreslåVedtakSteg.håndter()
        foreslåVedtakSteg.erFullstendig(SystemKlokke) shouldBe true
    }

    @Test
    fun `underkjenning blir lagret`() {
        val foreslåVedtakSteg = ForeslåVedtakSteg.opprett()

        foreslåVedtakSteg.underkjennSteget()

        foreslåVedtakSteg.tilEntity(UUID.randomUUID()).tilbakeført shouldBe ÅrsakTilTilbakeføring.Underkjent
    }

    @Test
    fun `steg er påbegynt`() {
        val foreslåVedtakSteg = ForeslåVedtakSteg.opprett()
        foreslåVedtakSteg.erPåbegynt() shouldBe false

        foreslåVedtakSteg.håndter()
        foreslåVedtakSteg.erPåbegynt() shouldBe true
    }

    @Test
    fun `endret periode i kravgrunnlag fører til tilbakeføring`() {
        val foreslåVedtakSteg = ForeslåVedtakSteg.opprett()

        foreslåVedtakSteg.periodeEndret(
            KravgrunnlagSammenligning.Forskjell.EndretPeriode(
                periode = 1.januar(2021) til 31.januar(2021),
                nyPeriode = 1.januar(2021) til 20.januar(2021),
                gammeltBeløp = 1500.kroner,
                nyttBeløp = 1000.kroner,
                etterfølgende = null,
            ),
        )

        foreslåVedtakSteg.tilEntity(UUID.randomUUID()).tilbakeført shouldBe ÅrsakTilTilbakeføring.NyttKravgrunnlag
    }
}
