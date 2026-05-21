package no.nav.tilbakekreving.behandling.saksbehandling

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.KlokkeStub
import no.nav.tilbakekreving.SystemKlokke
import no.nav.tilbakekreving.behandlingslogg.Behandlingslogg
import no.nav.tilbakekreving.eksternFagsakBehandling
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.test.februar
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.test.november
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

        foreldelsesteg.automatiskVurder(kravgrunnlag, klokke = KlokkeStub(fom.plusMonths(30)), Behandlingslogg(mutableListOf()), UUID.randomUUID())

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

        foreldelsesteg.automatiskVurder(kravgrunnlag, klokke = KlokkeStub(fom.plusMonths(30).plusDays(1)), Behandlingslogg(mutableListOf()), UUID.randomUUID())

        foreldelsesteg.erFullstendig(SystemKlokke) shouldBe false
    }

    @Test
    fun `manuell behandling hvor periode er satt til foreldet beholdes etter ny automatisk vurdering`() {
        val fom = 1.januar(2024)
        val periode = fom til 31.januar(2024)
        val kravgrunnlag = kravgrunnlag(perioder = listOf(kravgrunnlagPeriode(periode)))
        val foreldelsesteg = Foreldelsesteg.opprett(eksternFagsakBehandling(), kravgrunnlag)
        val klokke = KlokkeStub(fom.plusMonths(10))

        foreldelsesteg.automatiskVurder(kravgrunnlag, klokke = klokke, Behandlingslogg(mutableListOf()), UUID.randomUUID())
        foreldelsesteg.vurderForeldelse(periode, Foreldelsesteg.Vurdering.Foreldet("Begrunnelse"))

        foreldelsesteg.automatiskVurder(kravgrunnlag, klokke = klokke, Behandlingslogg(mutableListOf()), UUID.randomUUID())

        foreldelsesteg.erPeriodeForeldet(periode) shouldBe true
    }

    @Test
    fun `automatisk vurdering begrunnelsetekst får en begrunnelse for vurderingen`() {
        val fom = 1.januar(2024)
        val kravgrunnlag = kravgrunnlag(
            perioder = listOf(kravgrunnlagPeriode(fom til 31.januar(2024))),
        )
        val foreldelsesteg = Foreldelsesteg.opprett(eksternFagsakBehandling(), kravgrunnlag)
        val vurderingsdato = 11.november(2024)

        foreldelsesteg.automatiskVurder(kravgrunnlag, klokke = KlokkeStub(vurderingsdato), Behandlingslogg(mutableListOf()), UUID.randomUUID())

        val begrunnelse = foreldelsesteg.tilFrontendDto(kravgrunnlag).foreldetPerioder.single().begrunnelse
        begrunnelse shouldBe """
            Ingen perioder er foreldet fordi det er mindre enn tre år siden første feilutbetaling fant sted. Dette følger av foreldelsesloven §§ 2 og 3.

            Perioden er automatisk vurdert fordi det er mer enn 6 måneder til foreldelse inntreffer.

            Ved den automatiske vurderingen av foreldelse er det tatt utgangspunkt i 1. januar 2024, som er den første dagen i feilutbetalingsperioden.

            Merk at foreldelse skal vurderes fra utbetalingstidspunktet, og at første dag i feilutbetalingsperioden har blitt valgt på grunn av automatiseringshensyn.

            Automatisk vurdering av foreldelse ble gjort 11. november 2024, som er den datoen saken ble sendt til beslutter
        """.trimIndent()
    }

    @Test
    fun `setter tilbake til IkkeVurdert dersom behandlingen ikke lenger treffer reglene for automatisering`() {
        val fom = 1.januar(2024)
        val periode = fom til 31.januar(2024)
        val kravgrunnlag = kravgrunnlag(perioder = listOf(kravgrunnlagPeriode(periode)))
        val foreldelsesteg = Foreldelsesteg.opprett(eksternFagsakBehandling(), kravgrunnlag)
        val klokke = KlokkeStub(fom.plusMonths(10).withDayOfMonth(11))

        foreldelsesteg.automatiskVurder(kravgrunnlag, klokke = klokke, Behandlingslogg(mutableListOf()), UUID.randomUUID())
        foreldelsesteg.erFullstendig(klokke) shouldBe true

        klokke.settTid(fom.plusMonths(30).plusDays(1))
        foreldelsesteg.automatiskVurder(kravgrunnlag, klokke = klokke, Behandlingslogg(mutableListOf()), UUID.randomUUID())

        foreldelsesteg.erFullstendig(klokke) shouldBe false
    }
}
