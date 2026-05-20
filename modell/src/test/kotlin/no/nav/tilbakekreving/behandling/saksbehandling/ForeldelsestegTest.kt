package no.nav.tilbakekreving.behandling.saksbehandling

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.KlokkeStub
import no.nav.tilbakekreving.SystemKlokke
import no.nav.tilbakekreving.eksternFagsakBehandling
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.test.februar
import no.nav.tilbakekreving.test.januar
import org.junit.jupiter.api.Test
import java.util.UUID

class ForeldelsestegTest {
    @Test
    fun `vurdering av deler av periode`() {
        val foreldelsesteg =
            Foreldelsesteg.opprett(
                eksternFagsakBehandling(),
                kravgrunnlag(
                    perioder =
                        listOf(
                            kravgrunnlagPeriode(1.januar(2021) til 31.januar(2021)),
                            kravgrunnlagPeriode(1.februar(2021) til 28.februar(2021)),
                        ),
                ),
            )

        foreldelsesteg.vurderForeldelse(
            1.januar(2021) til 31.januar(2021),
            Foreldelsesteg.Vurdering.IkkeForeldet("Starten av utbetalingsperioden er innenfor foreldelsesfristen"),
        )

        foreldelsesteg.erFullstendig(SystemKlokke) shouldBe false
    }

    @Test
    fun `vurdering av deler hele perioden`() {
        val foreldelsesteg =
            Foreldelsesteg.opprett(
                eksternFagsakBehandling(),
                kravgrunnlag(
                    perioder =
                        listOf(
                            kravgrunnlagPeriode(1.januar(2021) til 31.januar(2021)),
                            kravgrunnlagPeriode(1.februar(2021) til 28.februar(2021)),
                        ),
                ),
            )

        foreldelsesteg.vurderForeldelse(
            1.januar(2021) til 31.januar(2021),
            Foreldelsesteg.Vurdering.IkkeForeldet("Starten av utbetalingsperioden er innenfor foreldelsesfristen"),
        )
        foreldelsesteg.vurderForeldelse(
            1.februar(2021) til 28.februar(2021),
            Foreldelsesteg.Vurdering.IkkeForeldet("Starten av utbetalingsperioden er innenfor foreldelsesfristen"),
        )

        foreldelsesteg.erFullstendig(SystemKlokke) shouldBe true
    }

    @Test
    fun `sjekk foreldelse på deler av splittet periode`() {
        val foreldelsesteg =
            Foreldelsesteg.opprett(
                eksternFagsakBehandling(),
                kravgrunnlag(
                    perioder =
                        listOf(
                            kravgrunnlagPeriode(1.januar(2021) til 28.februar(2021)),
                        ),
                ),
            )

        foreldelsesteg.vurderForeldelse(1.januar(2021) til 28.februar(2021), Foreldelsesteg.Vurdering.IkkeForeldet(""))

        foreldelsesteg.erPeriodeForeldet(1.januar(2021) til 31.januar(2021)) shouldBe false
    }

    @Test
    fun `underkjenning blir lagret`() {
        val forelgelse = Foreldelsesteg.opprett(
            eksternFagsakRevurdering = eksternFagsakBehandling(),
            kravgrunnlag = kravgrunnlag(),
        )

        forelgelse.underkjennSteget()

        forelgelse.tilEntity(UUID.randomUUID()).trengerNyVurdering shouldBe true
    }

    @Test
    fun `vurderer alle perioder automatisk innenfor 30 måneder`() {
        val fom = 1.januar(2024)
        val kravgrunnlag = kravgrunnlag(
            perioder = listOf(
                kravgrunnlagPeriode(fom til 31.januar(2024)),
                kravgrunnlagPeriode(1.februar(2024) til 28.februar(2024)),
            ),
        )
        val foreldelsesteg = Foreldelsesteg.opprett(eksternFagsakBehandling(), kravgrunnlag)

        foreldelsesteg.automatiskVurder(kravgrunnlag, klokke = KlokkeStub(fom.plusMonths(30)))

        foreldelsesteg.erFullstendig(SystemKlokke) shouldBe true
        foreldelsesteg.erPeriodeForeldet(fom til 31.januar(2024)) shouldBe false
        foreldelsesteg.erPeriodeForeldet(1.februar(2024) til 28.februar(2024)) shouldBe false
    }

    @Test
    fun `vurderes ikke automatisk når perioden er eldre enn 30 måneder`() {
        val fom = 1.januar(2024)
        val kravgrunnlag = kravgrunnlag(
            perioder = listOf(kravgrunnlagPeriode(fom til 31.januar(2024))),
        )
        val foreldelsesteg = Foreldelsesteg.opprett(eksternFagsakBehandling(), kravgrunnlag)

        foreldelsesteg.automatiskVurder(kravgrunnlag, klokke = KlokkeStub(fom.plusMonths(30).plusDays(1)))

        foreldelsesteg.erFullstendig(SystemKlokke) shouldBe false
    }

    @Test
    fun `manuell behandling hvor periode er satt til foreldet beholdes etter ny automatisk vurdering`() {
        val fom = 1.januar(2024)
        val periode = fom til 31.januar(2024)
        val kravgrunnlag = kravgrunnlag(perioder = listOf(kravgrunnlagPeriode(periode)))
        val foreldelsesteg = Foreldelsesteg.opprett(eksternFagsakBehandling(), kravgrunnlag)
        val klokke = KlokkeStub(fom.plusMonths(10))

        foreldelsesteg.automatiskVurder(kravgrunnlag, klokke = klokke)
        foreldelsesteg.vurderForeldelse(periode, Foreldelsesteg.Vurdering.Foreldet("Begrunnelse"))

        foreldelsesteg.automatiskVurder(kravgrunnlag, klokke = klokke)

        foreldelsesteg.erPeriodeForeldet(periode) shouldBe true
    }

    @Test
    fun `setter tilbake til IkkeVurdert dersom behandlingen ikke lenger treffer reglene for automatisering`() {
        val fom = 1.januar(2024)
        val periode = fom til 31.januar(2024)
        val kravgrunnlag = kravgrunnlag(perioder = listOf(kravgrunnlagPeriode(periode)))
        val foreldelsesteg = Foreldelsesteg.opprett(eksternFagsakBehandling(), kravgrunnlag)
        val klokke = KlokkeStub(fom.plusMonths(10))

        foreldelsesteg.automatiskVurder(kravgrunnlag, klokke = klokke)
        foreldelsesteg.erFullstendig(klokke) shouldBe true

        klokke.settTid(fom.plusMonths(30).plusDays(1))
        foreldelsesteg.automatiskVurder(kravgrunnlag, klokke = klokke)

        foreldelsesteg.erFullstendig(klokke) shouldBe false
    }
}
