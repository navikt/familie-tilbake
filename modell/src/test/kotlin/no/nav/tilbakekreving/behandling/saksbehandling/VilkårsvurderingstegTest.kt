package no.nav.tilbakekreving.behandling.saksbehandling

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvBruker
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvNav
import no.nav.tilbakekreving.SystemKlokke
import no.nav.tilbakekreving.api.v1.dto.SkalUnnlates
import no.nav.tilbakekreving.api.v1.dto.VurdertAktsomhetDto
import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingsperiodeDto
import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingsresultatDto
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.NivåAvForståelse
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.Vilkårsvurderingsteg
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.prosent
import no.nav.tilbakekreving.beregning.Reduksjon
import no.nav.tilbakekreving.eksternFagsakBehandling
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.frontend.models.DelerDto
import no.nav.tilbakekreving.kontrakter.frontend.models.GodTroDto
import no.nav.tilbakekreving.kontrakter.frontend.models.PeriodeDto
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagSammenligning
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.test.februar
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.test.prosentReduksjon
import no.nav.tilbakekreving.test.skalIkkeUnnlates
import no.nav.tilbakekreving.test.skalUnnlates
import no.nav.tilbakekreving.test.uaktsomt
import no.nav.tilbakekreving.ytelsesbeløp
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class VilkårsvurderingstegTest {
    @Test
    @Disabled
    fun `vilkårsvurdering på en av to perioder`() {
        val kravgrunnlag = kravgrunnlag(
            perioder = listOf(
                kravgrunnlagPeriode(1.januar(2021) til 31.januar(2021)),
                kravgrunnlagPeriode(1.februar(2021) til 28.februar(2021)),
            ),
        )
        val vilkårsvurderingsteg = Vilkårsvurderingsteg.opprett(
            eksternFagsakBehandling(),
            kravgrunnlag,
        )
        vilkårsvurderingsteg.vurder(
            1.januar(2021) til 31.januar(2021),
            forårsaketAvNav().godTro(beløpIBehold = null),
        )

        vilkårsvurderingsteg.erFullstendig(SystemKlokke) shouldBe false
    }

    @Test
    fun `vilkårsvurdering på begge perioder`() {
        val kravgrunnlag = kravgrunnlag(
            perioder =
                listOf(
                    kravgrunnlagPeriode(1.januar(2021) til 31.januar(2021)),
                    kravgrunnlagPeriode(1.februar(2021) til 28.februar(2021)),
                ),
        )
        val vilkårsvurderingsteg =
            Vilkårsvurderingsteg.opprett(
                eksternFagsakRevurdering = eksternFagsakBehandling(),
                kravgrunnlagHendelse = kravgrunnlag,
            )
        vilkårsvurderingsteg.vurder(
            1.januar(2021) til 28.februar(2021),
            forårsaketAvNav().godTro(beløpIBehold = null),
        )
        vilkårsvurderingsteg.erFullstendig(SystemKlokke) shouldBe true
    }

    @Test
    fun `vilkårsvurdering for under 4x rettgebyr med delvis tilbakekreving`() {
        val kravgrunnlag = kravgrunnlag(
            perioder = listOf(
                kravgrunnlagPeriode(1.januar(2021) til 31.januar(2021)),
                kravgrunnlagPeriode(1.februar(2021) til 28.februar(2021)),
            ),
        )
        val vilkårsvurderingsteg =
            Vilkårsvurderingsteg.opprett(
                eksternFagsakRevurdering = eksternFagsakBehandling(),
                kravgrunnlagHendelse = kravgrunnlag,
            )
        vilkårsvurderingsteg.vurder(
            1.januar(2021) til 31.januar(2021),
            forårsaketAvNav().burdeForstått(uaktsomt(skalIkkeUnnlates(), 50.prosentReduksjon)),
        )
        vilkårsvurderingsteg.perioder().first().reduksjon().shouldBeInstanceOf<Reduksjon.Prosentdel>()
    }

    @Test
    fun `vilkårsvurdering for under 4x rettgebyr med ingen tilbakekreving`() {
        val kravgrunnlag = kravgrunnlag(
            perioder = listOf(
                kravgrunnlagPeriode(1.januar(2021) til 31.januar(2021)),
                kravgrunnlagPeriode(1.februar(2021) til 28.februar(2021)),
            ),
        )
        val vilkårsvurderingsteg =
            Vilkårsvurderingsteg.opprett(
                eksternFagsakRevurdering = eksternFagsakBehandling(),
                kravgrunnlagHendelse = kravgrunnlag,
            )
        vilkårsvurderingsteg.vurder(
            1.januar(2021) til 31.januar(2021),
            forårsaketAvNav().burdeForstått(uaktsomt(skalUnnlates())),
        )

        vilkårsvurderingsteg.perioder().first().reduksjon().shouldBeInstanceOf<Reduksjon.IngenTilbakekreving>()
    }

    @Test
    fun `vilkårsvurdering for under 4x rettgebyr, forårsaket av bruker, uaktsomt - gir riktig frontend verdier`() {
        val kravgrunnlag = kravgrunnlag(
            perioder = listOf(
                kravgrunnlagPeriode(1.januar(2021) til 31.januar(2021)),
            ),
        )
        val revurdering = eksternFagsakBehandling()
        val vilkårsvurderingsteg = Vilkårsvurderingsteg.opprett(
            eksternFagsakRevurdering = eksternFagsakBehandling(),
            kravgrunnlagHendelse = kravgrunnlag,
        )
        val foreldelsesteg = Foreldelsesteg.opprett(revurdering, kravgrunnlag)
        foreldelsesteg.vurderForeldelse(
            1.januar(2021) til 31.januar(2021),
            Foreldelsesteg.Vurdering.IkkeForeldet(
                begrunnelse = "Ikke forelget",
            ),
        )

        vilkårsvurderingsteg.vurder(
            1.januar(2021) til 31.januar(2021),
            forårsaketAvBruker().uaktsomt(skalUnnlates()),
        )

        vilkårsvurderingsteg.tilFrontendDto(kravgrunnlag, revurdering, foreldelsesteg, SystemKlokke).perioder.single() shouldBe VurdertVilkårsvurderingsperiodeDto(
            periode = 1.januar(2021) til 31.januar(2021),
            feilutbetaltBeløp = 2000.kroner,
            hendelsestype = Hendelsestype.ANNET,
            reduserteBeløper = emptyList(),
            aktiviteter = emptyList(),
            vilkårsvurderingsresultatInfo = VurdertVilkårsvurderingsresultatDto(
                vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
                godTro = null,
                aktsomhet = VurdertAktsomhetDto(
                    aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
                    ileggRenter = false,
                    andelTilbakekreves = 0.prosent,
                    beløpTilbakekreves = null,
                    begrunnelse = "",
                    særligeGrunner = null,
                    særligeGrunnerTilReduksjon = false,
                    tilbakekrevSmåbeløp = false,
                    unnlates4Rettsgebyr = SkalUnnlates.UNNLATES,
                    særligeGrunnerBegrunnelse = null,
                ),
            ),
            begrunnelse = "",
            foreldet = false,
        )
    }

    @Test
    fun `underkjenning blir lagret`() {
        val vilkårsvurderingssteg = Vilkårsvurderingsteg.opprett(
            eksternFagsakRevurdering = eksternFagsakBehandling(),
            kravgrunnlagHendelse = kravgrunnlag(),
        )

        vilkårsvurderingssteg.underkjennSteget()

        vilkårsvurderingssteg.tilEntity(UUID.randomUUID()).tilbakeført shouldBe ÅrsakTilTilbakeføring.Underkjent
    }

    @Test
    fun `beløp under 4x rettsgebyr`() {
        val kravgrunnlag = kravgrunnlag(
            perioder = listOf(
                kravgrunnlagPeriode(1.januar(2024) til 31.januar(2024), ytelsesbeløp(2554.kroner)),
                kravgrunnlagPeriode(1.februar(2024) til 28.februar(2024), ytelsesbeløp(2553.kroner)),
            ),
        )
        val revurdering = eksternFagsakBehandling()

        val foreldelsesteg = Foreldelsesteg.opprett(revurdering, kravgrunnlag)
        foreldelsesteg.vurderForeldelse(
            1.januar(2024) til 31.januar(2024),
            Foreldelsesteg.Vurdering.IkkeForeldet(
                begrunnelse = "Ikke forelget",
            ),
        )
        foreldelsesteg.vurderForeldelse(
            1.februar(2024) til 28.februar(2024),
            Foreldelsesteg.Vurdering.IkkeForeldet(
                begrunnelse = "Ikke forelget",
            ),
        )

        val vilkårsvurderingsteg = Vilkårsvurderingsteg.opprett(
            eksternFagsakRevurdering = revurdering,
            kravgrunnlagHendelse = kravgrunnlag,
        )
        vilkårsvurderingsteg.tilFrontendDto(kravgrunnlag, revurdering, foreldelsesteg, SystemKlokke).kanUnnlates4xRettsgebyr shouldBe true
    }

    @Test
    fun `beløp over 4x rettsgebyr`() {
        val kravgrunnlag = kravgrunnlag(
            perioder = listOf(
                kravgrunnlagPeriode(1.januar(2024) til 31.januar(2024), ytelsesbeløp(2554.kroner)),
                kravgrunnlagPeriode(1.februar(2024) til 28.februar(2024), ytelsesbeløp(2554.kroner)),
            ),
        )
        val revurdering = eksternFagsakBehandling()

        val foreldelsesteg = Foreldelsesteg.opprett(revurdering, kravgrunnlag)
        foreldelsesteg.vurderForeldelse(
            1.januar(2024) til 31.januar(2024),
            Foreldelsesteg.Vurdering.IkkeForeldet(
                begrunnelse = "Ikke forelget",
            ),
        )
        foreldelsesteg.vurderForeldelse(
            1.februar(2024) til 28.februar(2024),
            Foreldelsesteg.Vurdering.IkkeForeldet(
                begrunnelse = "Ikke forelget",
            ),
        )

        val vilkårsvurderingsteg = Vilkårsvurderingsteg.opprett(
            eksternFagsakRevurdering = revurdering,
            kravgrunnlagHendelse = kravgrunnlag,
        )
        vilkårsvurderingsteg.tilFrontendDto(kravgrunnlag, revurdering, foreldelsesteg, SystemKlokke).kanUnnlates4xRettsgebyr shouldBe false
    }

    @Test
    fun `henter alle vilkårsvurderingsperiodene`() {
        val kravgrunnlag = kravgrunnlag(
            perioder = listOf(
                kravgrunnlagPeriode(1.januar(2021) til 31.januar(2021)),
                kravgrunnlagPeriode(1.februar(2021) til 28.februar(2021)),
            ),
        )
        val vilkårsvurderingsteg = Vilkårsvurderingsteg.opprett(
            eksternFagsakBehandling(),
            kravgrunnlag,
        )

        vilkårsvurderingsteg.hentVilkårsvurderingsperioder() shouldNotBeNull {
            size shouldBe 2
            this[0].periode shouldBe PeriodeDto(1.januar(2021), 31.januar(2021))
            this[1].periode shouldBe PeriodeDto(1.februar(2021), 28.februar(2021))
        }
    }

    @Test
    fun `ikke påbegynt`() {
        val vilkårsvurderingsteg = Vilkårsvurderingsteg.opprett(eksternFagsakBehandling(), kravgrunnlag())
        vilkårsvurderingsteg.erPåbegynt() shouldBe false
    }

    @Test
    fun `en vurdert periode`() {
        val vilkårsvurderingsteg = Vilkårsvurderingsteg.opprett(
            eksternFagsakBehandling(),
            kravgrunnlag(
                perioder = listOf(
                    kravgrunnlagPeriode(1.januar(2021) til 31.januar(2021)),
                    kravgrunnlagPeriode(1.februar(2021) til 28.februar(2021)),
                ),
            ),
        )
        vilkårsvurderingsteg.vurder(1.januar(2021) til 31.januar(2021), forårsaketAvNav().burdeForstått())
        vilkårsvurderingsteg.erPåbegynt() shouldBe true
    }

    @Test
    fun `beløp i behold ny vilkårsvurdering`() {
        val vilkårsvurderingsteg = Vilkårsvurderingsteg.opprett(
            eksternFagsakBehandling(),
            kravgrunnlag(
                perioder = listOf(
                    kravgrunnlagPeriode(1.januar(2021) til 31.januar(2021)),
                ),
            ),
        )
        vilkårsvurderingsteg.vurder(
            1.januar(2021) til 31.januar(2021),
            NivåAvForståelse.GodTro(
                beløpIBehold = NivåAvForståelse.GodTro.BeløpIBehold.DelerIBehold(1000.kroner),
                begrunnelse = "Begrunnelse for beløp i behold",
                begrunnelseForGodTro = "Begrunnelse for god tro",
            ),
        )

        vilkårsvurderingsteg.tilFrontendDto().shouldNotBeNull {
            size shouldBe 1
            this[0].fom shouldBe 1.januar(2021)
            this[0].tom shouldBe 31.januar(2021)
            this[0].valg.shouldBeInstanceOf<GodTroDto> {
                it.begrunnelse shouldBe "Begrunnelse for god tro"
                it.beløpIBehold.shouldBeInstanceOf<DelerDto> {
                    BigDecimal(it.beløp) shouldBe 1000.kroner
                    it.begrunnelse shouldBe "Begrunnelse for beløp i behold"
                }
            }
        }
    }

    @Test
    fun `ny periode i kravgrunnlag legges til i vilkårsvurderingen`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        val nyPeriode = 1.februar(2021) til 28.februar(2021)
        val kravgrunnlag = kravgrunnlag(perioder = listOf(kravgrunnlagPeriode(periode)))

        val vilkårsvurderingsteg = Vilkårsvurderingsteg.opprett(
            eksternFagsakBehandling(),
            kravgrunnlag,
        )
        vilkårsvurderingsteg.vurder(periode, forårsaketAvNav().godTro(beløpIBehold = null))

        vilkårsvurderingsteg.trengerNyVurdering() shouldBe null
        vilkårsvurderingsteg.nyPeriode(KravgrunnlagSammenligning.Forskjell.NyPeriode(nyPeriode, 2000.kroner))

        vilkårsvurderingsteg.trengerNyVurdering() shouldBe ÅrsakTilTilbakeføring.NyttKravgrunnlag
        vilkårsvurderingsteg.tilFrontendDto().should {
            it[0].fom shouldBe periode.fom
            it[0].tom shouldBe periode.tom
            it[1].fom shouldBe nyPeriode.fom
            it[1].tom shouldBe nyPeriode.tom
        }
    }
}
