package no.nav.tilbakekreving.beregning.slåSammenPerioder

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.SystemKlokke
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.NivåAvForståelse
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.Vilkårsvurderingsteg
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.eksternFagsakBehandling
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakRevurdering
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.kontrakter.frontend.models.SammenslaaingDto
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.test.april
import no.nav.tilbakekreving.test.februar
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.test.mars
import org.junit.jupiter.api.Test
import java.util.UUID

class SlåSammenTest {
    val eksternFagsakRevurdering = eksternFagsakBehandling(
        utvidPerioder = listOf(
            EksternFagsakRevurdering.UtvidetPeriode(
                UUID.randomUUID(),
                2.februar(2025) til 1.april(2025),
                1.januar(2025) til 27.mars(2025),
            ),
        ),
    )
    val kravgrunnlagHendelse = kravgrunnlag(
        perioder = listOf(
            kravgrunnlagPeriode(2.februar(2025) til 13.februar(2025)),
            kravgrunnlagPeriode(16.februar(2025) til 27.februar(2025)),
            kravgrunnlagPeriode(2.mars(2025) til 13.mars(2025)),
            kravgrunnlagPeriode(16.mars(2025) til 27.mars(2025)),
        ),
    )
    val foreldelsesteg = Foreldelsesteg.opprett(eksternFagsakRevurdering, kravgrunnlagHendelse)

    @Test
    fun `slå sammen vilkårsvurdering perioder etter splitting`() {
        vurderForeldelse()

        val vilkårsvurderingsteg = Vilkårsvurderingsteg.opprett(
            eksternFagsakRevurdering,
            kravgrunnlagHendelse,
        )
        val perioder = vilkårsvurderingsteg.hentVilkårsvurderingsperioder()

        vilkårsvurderingsteg.splittVilkårsvurdering(perioder[1].periodeId)
        vilkårsvurderingsteg.tilFrontendDto(
            kravgrunnlag = kravgrunnlagHendelse,
            revurdering = eksternFagsakRevurdering,
            foreldelsesteg = foreldelsesteg,
            klokke = SystemKlokke,
        ).perioder.shouldHaveSize(2)

        vilkårsvurderingsteg.kopierVurderingerForSammenslåing(
            SammenslaaingDto(
                vilkårsvurderingId = perioder[1].periodeId,
                slåesSammenMedId = perioder[0].periodeId,
            ),
            Sporing("ukjent", null),
        )

        val frontendDto = vilkårsvurderingsteg.tilFrontendDto(
            kravgrunnlag = kravgrunnlagHendelse,
            revurdering = eksternFagsakRevurdering,
            foreldelsesteg = foreldelsesteg,
            klokke = SystemKlokke,
        )
        frontendDto.perioder.shouldHaveSize(1)
        frontendDto.perioder.first().periode.fom shouldBe 2.februar(2025)
        frontendDto.perioder.first().periode.tom shouldBe 27.mars(2025)
        frontendDto.perioder.first().feilutbetaltBeløp shouldBe 8000.kroner
    }

    @Test
    fun `slå sammen vilkårsvurdering perioder etter at vurdering er gjort på splittet perioder`() {
        vurderForeldelse()

        val vilkårsvurderingsteg = Vilkårsvurderingsteg.opprett(
            eksternFagsakRevurdering,
            kravgrunnlagHendelse,
        )
        val perioder = vilkårsvurderingsteg.hentVilkårsvurderingsperioder()

        vilkårsvurderingsteg.splittVilkårsvurdering(perioder[2].periodeId)
        vilkårsvurderingsteg.tilFrontendDto(
            kravgrunnlag = kravgrunnlagHendelse,
            revurdering = eksternFagsakRevurdering,
            foreldelsesteg = foreldelsesteg,
            klokke = SystemKlokke,
        ).perioder.shouldHaveSize(2)

        vilkårsvurderingsteg.vurder(
            periode = 2.februar(2025) til 27.februar(2025),
            vurdering = NivåAvForståelse.GodTro(
                begrunnelse = "God tro",
                beløpIBehold = NivåAvForståelse.GodTro.BeløpIBehold.Nei,
                begrunnelseForGodTro = "begrunnelse for god tro",
            ),
        )

        vilkårsvurderingsteg.vurder(
            periode = 2.mars(2025) til 27.mars(2025),
            vurdering = NivåAvForståelse.Forstod(
                aktsomhet = NivåAvForståelse.Aktsomhet.Forsett("begrunnelse for forsett"),
                begrunnelse = "Begrunnelse",
            ),
        )

        vilkårsvurderingsteg.splittVilkårsvurdering(perioder[3].periodeId)
        vilkårsvurderingsteg.tilFrontendDto(
            kravgrunnlag = kravgrunnlagHendelse,
            revurdering = eksternFagsakRevurdering,
            foreldelsesteg = foreldelsesteg,
            klokke = SystemKlokke,
        ).perioder.shouldHaveSize(3)

        vilkårsvurderingsteg.kopierVurderingerForSammenslåing(
            SammenslaaingDto(
                vilkårsvurderingId = perioder[2].periodeId,
                slåesSammenMedId = perioder[1].periodeId,
            ),
            Sporing("ukjent", null),
        )

        val frontendDto = vilkårsvurderingsteg.tilFrontendDto(
            kravgrunnlag = kravgrunnlagHendelse,
            revurdering = eksternFagsakRevurdering,
            foreldelsesteg = foreldelsesteg,
            klokke = SystemKlokke,
        )
        frontendDto.perioder.shouldHaveSize(2)
        frontendDto.perioder.first().periode.fom shouldBe 2.februar(2025)
        frontendDto.perioder.first().periode.tom shouldBe 13.mars(2025)
        frontendDto.perioder.first().feilutbetaltBeløp shouldBe 6000.kroner
        frontendDto.perioder.first().vilkårsvurderingsresultatInfo?.vilkårsvurderingsresultat shouldBe Vilkårsvurderingsresultat.GOD_TRO
        frontendDto.perioder[1].periode.fom shouldBe 16.mars(2025)
        frontendDto.perioder[1].periode.tom shouldBe 27.mars(2025)
        frontendDto.perioder[1].feilutbetaltBeløp shouldBe 2000.kroner
        frontendDto.perioder[1].vilkårsvurderingsresultatInfo?.vilkårsvurderingsresultat shouldBe null
    }

    @Test
    fun `kaster exception hvis to perioder som ikke er inntilhverandre skal slåes sammen`() {
        vurderForeldelse()
        val vilkårsvurderingsteg = Vilkårsvurderingsteg.opprett(
            eksternFagsakRevurdering,
            kravgrunnlagHendelse,
        )
        val perioder = vilkårsvurderingsteg.hentVilkårsvurderingsperioder()

        vilkårsvurderingsteg.splittVilkårsvurdering(perioder[2].periodeId)
        vilkårsvurderingsteg.tilFrontendDto(
            kravgrunnlag = kravgrunnlagHendelse,
            revurdering = eksternFagsakRevurdering,
            foreldelsesteg = foreldelsesteg,
            klokke = SystemKlokke,
        ).perioder.shouldHaveSize(2)

        val exception = shouldThrow<ModellFeil.UgyldigOperasjonException> {
            vilkårsvurderingsteg.kopierVurderingerForSammenslåing(
                SammenslaaingDto(
                    vilkårsvurderingId = perioder[3].periodeId,
                    slåesSammenMedId = perioder[1].periodeId,
                ),
                Sporing("ukjent", null),
            )
        }

        exception.melding shouldBe ("Kun perioder som er inntil hverandre kan slås sammen. Periode med id ${perioder[3].periodeId} og periode med id ${perioder[1].periodeId} er ikke inntil hverandre.")
    }

    private fun vurderForeldelse() =
        kravgrunnlagHendelse.perioder().forEach {
            foreldelsesteg.vurderForeldelse(
                it.periode(),
                Foreldelsesteg.Vurdering.IkkeForeldet(
                    begrunnelse = "Ikke forelget",
                ),
            )
        }
}
