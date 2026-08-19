package no.nav.tilbakekreving.behandling.saksbehandling

import io.kotest.matchers.collections.shouldBeSingle
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
import no.nav.tilbakekreving.kontrakter.frontend.models.EndretPeriodeDto
import no.nav.tilbakekreving.kontrakter.frontend.models.GodTroDto
import no.nav.tilbakekreving.kontrakter.frontend.models.NyPeriodeDto
import no.nav.tilbakekreving.kontrakter.frontend.models.HeleDto
import no.nav.tilbakekreving.kontrakter.frontend.models.IngentingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.PeriodeDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VilkaarsvurderingIkkeVurdertDto
import no.nav.tilbakekreving.kontrakter.frontend.models.SkalIkkeReduseresDto
import no.nav.tilbakekreving.kontrakter.frontend.models.SkalReduseresDto
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagSammenligning
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.test.februar
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.test.mars
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
    fun `deler av beløpet i behold ny vilkårsvurdering uten reduksjon`() {
        val vilkårsvurderingsteg = Vilkårsvurderingsteg.opprett(
            eksternFagsakBehandling(),
            kravgrunnlag(
                perioder = listOf(
                    kravgrunnlagPeriode(periode = 1.januar(2021) til 31.januar(2021)),
                ),
            ),
        )
        vilkårsvurderingsteg.vurder(
            1.januar(2021) til 31.januar(2021),
            NivåAvForståelse.GodTro(
                beløpIBehold = NivåAvForståelse.GodTro.BeløpIBehold.DelerIBehold(
                    beløp = 1000.kroner,
                    prosentReduksjon = null,
                    relevanteMomenter = listOf(RelevanteMomentGodTro.StørrelseBeløp),
                    annetBegrunnelse = null,
                    begrunnelse = "Begrunnelse for i behold",
                ),
                begrunnelse = "Begrunnelse for beløp i behold (deprecated)",
                begrunnelseForGodTro = "Begrunnelse for god tro",
            ),
        )

        vilkårsvurderingsteg.tilFrontendDto().shouldNotBeNull {
            size shouldBe 1
            this[0].fom shouldBe 1.januar(2021)
            this[0].tom shouldBe 31.januar(2021)
            this[0].valg.shouldBeInstanceOf<GodTroDto> {
                it.begrunnelse shouldBe "Begrunnelse for god tro"
                it.beløpIBehold.shouldBeInstanceOf<DelerDto> { deler ->
                    BigDecimal(deler.beløp) shouldBe 1000.kroner
                    deler.begrunnelse shouldBe "Begrunnelse for beløp i behold (deprecated)"
                    deler.reduksjon.shouldBeInstanceOf<SkalIkkeReduseresDto> { reduksjon ->
                        reduksjon.relevans.size shouldBe 1
                        reduksjon.relevans.first().moment shouldBe "STØRRELSE_BELØP"
                        reduksjon.annetBegrunnelse shouldBe null
                        reduksjon.begrunnelse shouldBe "Begrunnelse for i behold"
                    }
                }
            }
        }
    }

    @Test
    fun `deler av beløpet i behold ny vilkårsvurdering med reduksjon`() {
        val vilkårsvurderingsteg = Vilkårsvurderingsteg.opprett(
            eksternFagsakBehandling(),
            kravgrunnlag(
                perioder = listOf(
                    kravgrunnlagPeriode(periode = 1.januar(2021) til 31.januar(2021)),
                ),
            ),
        )
        vilkårsvurderingsteg.vurder(
            1.januar(2021) til 31.januar(2021),
            NivåAvForståelse.GodTro(
                beløpIBehold = NivåAvForståelse.GodTro.BeløpIBehold.DelerIBehold(
                    beløp = 1000.kroner,
                    prosentReduksjon = 20,
                    relevanteMomenter = listOf(RelevanteMomentGodTro.StørrelseBeløp, RelevanteMomentGodTro.Annet("annet begrunnelse")),
                    annetBegrunnelse = "Annet begrunnelse",
                    begrunnelse = "Begrunnelse for i behold",
                ),
                begrunnelse = "Begrunnelse for beløp i behold (deprecated)",
                begrunnelseForGodTro = "Begrunnelse for god tro",
            ),
        )

        vilkårsvurderingsteg.tilFrontendDto().shouldNotBeNull {
            size shouldBe 1
            this[0].fom shouldBe 1.januar(2021)
            this[0].tom shouldBe 31.januar(2021)
            this[0].valg.shouldBeInstanceOf<GodTroDto> {
                it.begrunnelse shouldBe "Begrunnelse for god tro"
                it.beløpIBehold.shouldBeInstanceOf<DelerDto> { deler ->
                    BigDecimal(deler.beløp) shouldBe 1000.kroner
                    deler.begrunnelse shouldBe "Begrunnelse for beløp i behold (deprecated)"
                    deler.reduksjon.shouldBeInstanceOf<SkalReduseresDto> { reduksjon ->
                        reduksjon.relevans.size shouldBe 2
                        reduksjon.relevans.first().moment shouldBe "STØRRELSE_BELØP"
                        reduksjon.relevans[1].moment shouldBe "ANNET"
                        reduksjon.annetBegrunnelse shouldBe "Annet begrunnelse"
                        reduksjon.begrunnelse shouldBe "Begrunnelse for i behold"
                        reduksjon.prosentReduksjon shouldBe 20
                    }
                }
            }
        }
    }

    @Test
    fun `hele beløpet i behold ny vilkårsvurdering uten reduksjon`() {
        val vilkårsvurderingsteg = Vilkårsvurderingsteg.opprett(
            eksternFagsakBehandling(),
            kravgrunnlag(
                perioder = listOf(
                    kravgrunnlagPeriode(periode = 1.januar(2021) til 31.januar(2021)),
                ),
            ),
        )
        vilkårsvurderingsteg.vurder(
            1.januar(2021) til 31.januar(2021),
            NivåAvForståelse.GodTro(
                beløpIBehold = NivåAvForståelse.GodTro.BeløpIBehold.HeleIBehold(
                    prosentReduksjon = null,
                    relevanteMomenter = listOf(RelevanteMomentGodTro.StørrelseBeløp),
                    annetBegrunnelse = null,
                    begrunnelse = "Begrunnelse for i behold",
                ),
                begrunnelse = "Begrunnelse for beløp i behold (deprecated)",
                begrunnelseForGodTro = "Begrunnelse for god tro",
            ),
        )

        vilkårsvurderingsteg.tilFrontendDto().shouldNotBeNull {
            size shouldBe 1
            this[0].fom shouldBe 1.januar(2021)
            this[0].tom shouldBe 31.januar(2021)
            this[0].valg.shouldBeInstanceOf<GodTroDto> {
                it.begrunnelse shouldBe "Begrunnelse for god tro"
                it.beløpIBehold.shouldBeInstanceOf<HeleDto> { hele ->
                    hele.begrunnelse shouldBe "Begrunnelse for beløp i behold (deprecated)"
                    hele.reduksjon.shouldBeInstanceOf<SkalIkkeReduseresDto> { reduksjon ->
                        reduksjon.relevans.size shouldBe 1
                        reduksjon.relevans.first().moment shouldBe "STØRRELSE_BELØP"
                        reduksjon.annetBegrunnelse shouldBe null
                        reduksjon.begrunnelse shouldBe "Begrunnelse for i behold"
                    }
                }
            }
        }
    }

    @Test
    fun `hele beløpet i behold ny vilkårsvurdering med reduksjon`() {
        val vilkårsvurderingsteg = Vilkårsvurderingsteg.opprett(
            eksternFagsakBehandling(),
            kravgrunnlag(
                perioder = listOf(
                    kravgrunnlagPeriode(periode = 1.januar(2021) til 31.januar(2021)),
                ),
            ),
        )
        vilkårsvurderingsteg.vurder(
            1.januar(2021) til 31.januar(2021),
            NivåAvForståelse.GodTro(
                beløpIBehold = NivåAvForståelse.GodTro.BeløpIBehold.HeleIBehold(
                    prosentReduksjon = 20,
                    relevanteMomenter = listOf(RelevanteMomentGodTro.StørrelseBeløp, RelevanteMomentGodTro.Annet("annet begrunnelse")),
                    annetBegrunnelse = "Annet begrunnelse",
                    begrunnelse = "Begrunnelse for i behold",
                ),
                begrunnelse = "Begrunnelse for beløp i behold (deprecated)",
                begrunnelseForGodTro = "Begrunnelse for god tro",
            ),
        )

        vilkårsvurderingsteg.tilFrontendDto().shouldNotBeNull {
            size shouldBe 1
            this[0].fom shouldBe 1.januar(2021)
            this[0].tom shouldBe 31.januar(2021)
            this[0].valg.shouldBeInstanceOf<GodTroDto> {
                it.begrunnelse shouldBe "Begrunnelse for god tro"
                it.beløpIBehold.shouldBeInstanceOf<HeleDto> { hele ->
                    hele.begrunnelse shouldBe "Begrunnelse for beløp i behold (deprecated)"
                    hele.reduksjon.shouldBeInstanceOf<SkalReduseresDto> { reduksjon ->
                        reduksjon.relevans.size shouldBe 2
                        reduksjon.relevans.first().moment shouldBe "STØRRELSE_BELØP"
                        reduksjon.relevans[1].moment shouldBe "ANNET"
                        reduksjon.annetBegrunnelse shouldBe "Annet begrunnelse"
                        reduksjon.begrunnelse shouldBe "Begrunnelse for i behold"
                        reduksjon.prosentReduksjon shouldBe 20
                    }
                }
            }
        }
    }

    @Test
    fun `beløp ikke  i behold ny vilkårsvurdering`() {
        val vilkårsvurderingsteg = Vilkårsvurderingsteg.opprett(
            eksternFagsakBehandling(),
            kravgrunnlag(
                perioder = listOf(
                    kravgrunnlagPeriode(periode = 1.januar(2021) til 31.januar(2021)),
                ),
            ),
        )
        vilkårsvurderingsteg.vurder(
            1.januar(2021) til 31.januar(2021),
            NivåAvForståelse.GodTro(
                beløpIBehold = NivåAvForståelse.GodTro.BeløpIBehold.Nei(
                    begrunnelse = "Begrunnelse for i behold",
                ),
                begrunnelse = "Begrunnelse for beløp i behold (deprecated)",
                begrunnelseForGodTro = "Begrunnelse for god tro",
            ),
        )

        vilkårsvurderingsteg.tilFrontendDto().shouldNotBeNull {
            size shouldBe 1
            this[0].fom shouldBe 1.januar(2021)
            this[0].tom shouldBe 31.januar(2021)
            this[0].valg.shouldBeInstanceOf<GodTroDto> {
                it.begrunnelse shouldBe "Begrunnelse for god tro"
                it.beløpIBehold.shouldBeInstanceOf<IngentingDto> { ikkeIBehold ->
                    ikkeIBehold.begrunnelse shouldBe "Begrunnelse for beløp i behold (deprecated)"
                }
            }
        }
    }

    @Test
    fun `endring i kravgrunnlag - flere perioder`() {
        val periode1 = 1.januar(2021) til 31.januar(2021)
        val periode2 = 1.februar(2021) til 28.februar(2021)
        val kravgrunnlag = kravgrunnlag(
            perioder = listOf(
                kravgrunnlagPeriode(periode1),
                kravgrunnlagPeriode(periode2),
            ),
        )
        val vilkårsvurderingsteg = Vilkårsvurderingsteg.opprett(
            eksternFagsakRevurdering = eksternFagsakBehandling(),
            kravgrunnlagHendelse = kravgrunnlag,
        )

        vilkårsvurderingsteg.perideEndretBeløp(
            KravgrunnlagSammenligning.Forskjell.JustertBeløp(
                periode = periode1,
                endringIBeløp = 500.kroner,
            ),
        )
        vilkårsvurderingsteg.perideEndretBeløp(
            KravgrunnlagSammenligning.Forskjell.JustertBeløp(
                periode = periode2,
                endringIBeløp = 300.kroner,
            ),
        )

        vilkårsvurderingsteg.tilFrontendDto().shouldBeSingle().endringIKravgrunnlag shouldBe EndretPeriodeDto(
            fom = periode1.fom,
            tom = periode2.tom,
            endringIBeløp = 800,
        )
    }

    @Test
    fun `endring i kravgrunnlag - enkel periode`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        val kravgrunnlag = kravgrunnlag(
            perioder = listOf(
                kravgrunnlagPeriode(periode),
            ),
        )
        val vilkårsvurderingsteg = Vilkårsvurderingsteg.opprett(
            eksternFagsakRevurdering = eksternFagsakBehandling(),
            kravgrunnlagHendelse = kravgrunnlag,
        )

        vilkårsvurderingsteg.perideEndretBeløp(
            KravgrunnlagSammenligning.Forskjell.JustertBeløp(
                periode = periode,
                endringIBeløp = 500.kroner,
            ),
        )

        vilkårsvurderingsteg.tilFrontendDto().shouldBeSingle().endringIKravgrunnlag shouldBe EndretPeriodeDto(
            fom = periode.fom,
            tom = periode.tom,
            endringIBeløp = 500,
        )
    }

    @Test
    fun `endring i kravgrunnlag - har ikke mottatt nytt kravgrunnlag`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        val kravgrunnlag = kravgrunnlag(
            perioder = listOf(
                kravgrunnlagPeriode(periode),
            ),
        )
        val vilkårsvurderingsteg = Vilkårsvurderingsteg.opprett(
            eksternFagsakRevurdering = eksternFagsakBehandling(),
            kravgrunnlagHendelse = kravgrunnlag,
        )

        vilkårsvurderingsteg.tilFrontendDto().shouldBeSingle().endringIKravgrunnlag shouldBe null
    }

    @Test
    fun `endring i kravgrunnlag - to nye perioder før eksisterende periode`() {
        val eksisterendePeriode = 1.mars(2021) til 31.mars(2021)
        val nyPeriode1 = 1.januar(2021) til 31.januar(2021)
        val nyPeriode2 = 1.februar(2021) til 28.februar(2021)
        val kravgrunnlag = kravgrunnlag(perioder = listOf(kravgrunnlagPeriode(eksisterendePeriode)))

        val vilkårsvurderingsteg = Vilkårsvurderingsteg.opprett(
            eksternFagsakBehandling(),
            kravgrunnlag,
        )
        vilkårsvurderingsteg.vurder(eksisterendePeriode, forårsaketAvNav().godTro(beløpIBehold = null))

        vilkårsvurderingsteg.nyPeriode(KravgrunnlagSammenligning.Forskjell.NyPeriode(nyPeriode1, 1000.kroner))
        vilkårsvurderingsteg.nyPeriode(KravgrunnlagSammenligning.Forskjell.NyPeriode(nyPeriode2, 2000.kroner))

        vilkårsvurderingsteg.tilFrontendDto().should {
            it.size shouldBe 2
            it[0].fom shouldBe nyPeriode1.fom
            it[0].tom shouldBe nyPeriode2.tom
            it[0].endringIKravgrunnlag shouldBe NyPeriodeDto(nyPeriode1.fom, nyPeriode2.tom, 3000)
            it[1].fom shouldBe eksisterendePeriode.fom
            it[1].tom shouldBe eksisterendePeriode.tom
        }
    }

    @Test
    fun `endring i kravgrunnlag - ny periode etter eksisterende periode`() {
        val eksisterendePeriode = 1.januar(2021) til 31.januar(2021)
        val nyPeriode = 1.februar(2021) til 28.februar(2021)
        val kravgrunnlag = kravgrunnlag(perioder = listOf(kravgrunnlagPeriode(eksisterendePeriode)))

        val vilkårsvurderingsteg = Vilkårsvurderingsteg.opprett(
            eksternFagsakBehandling(),
            kravgrunnlag,
        )

        vilkårsvurderingsteg.nyPeriode(KravgrunnlagSammenligning.Forskjell.NyPeriode(nyPeriode, 2000.kroner))

        vilkårsvurderingsteg.tilFrontendDto().should {
            it.size shouldBe 1
            it[0].fom shouldBe eksisterendePeriode.fom
            it[0].tom shouldBe nyPeriode.tom
        }
    }

    @Test
    fun `endring i kravgrunnlag - ny periode før eksisterende periode`() {
        val eksisterendePeriode = 1.februar(2021) til 28.februar(2021)
        val nyPeriode = 1.januar(2021) til 31.januar(2021)
        val kravgrunnlag = kravgrunnlag(perioder = listOf(kravgrunnlagPeriode(eksisterendePeriode)))

        val vilkårsvurderingsteg = Vilkårsvurderingsteg.opprett(
            eksternFagsakBehandling(),
            kravgrunnlag,
        )

        vilkårsvurderingsteg.nyPeriode(KravgrunnlagSammenligning.Forskjell.NyPeriode(nyPeriode, 2000.kroner))

        vilkårsvurderingsteg.tilFrontendDto().should {
            it.size shouldBe 1
            it[0].fom shouldBe nyPeriode.fom
            it[0].tom shouldBe eksisterendePeriode.tom
        }
    }

    @Test
    fun `endring i kravgrunnlag - to nye perioder før eksisterende ikke vurdert periode`() {
        val eksisterendePeriode = 1.mars(2021) til 31.mars(2021)
        val nyPeriode1 = 1.januar(2021) til 31.januar(2021)
        val nyPeriode2 = 1.februar(2021) til 28.februar(2021)
        val kravgrunnlag = kravgrunnlag(perioder = listOf(kravgrunnlagPeriode(eksisterendePeriode)))

        val vilkårsvurderingsteg = Vilkårsvurderingsteg.opprett(
            eksternFagsakBehandling(),
            kravgrunnlag,
        )

        vilkårsvurderingsteg.nyPeriode(KravgrunnlagSammenligning.Forskjell.NyPeriode(nyPeriode1, 1000.kroner))
        vilkårsvurderingsteg.nyPeriode(KravgrunnlagSammenligning.Forskjell.NyPeriode(nyPeriode2, 2000.kroner))

        vilkårsvurderingsteg.tilFrontendDto().should {
            it.size shouldBe 1
            it[0].fom shouldBe nyPeriode1.fom
            it[0].tom shouldBe eksisterendePeriode.tom
        }

        val entity = vilkårsvurderingsteg.tilEntity(UUID.randomUUID())
        entity.vurderinger[1].vurdering.forrigePeriodeId shouldBe entity.vurderinger[0].id
        entity.vurderinger[2].vurdering.forrigePeriodeId shouldBe entity.vurderinger[1].id
    }

    @Test
    fun `endring i kravgrunnlag - ny periode mellom to eksisterende perioder`() {
        val eksisterendePeriode1 = 1.januar(2021) til 31.januar(2021)
        val eksisterendePeriode2 = 1.mars(2021) til 31.mars(2021)
        val nyPeriode = 1.februar(2021) til 28.februar(2021)
        val kravgrunnlag = kravgrunnlag(
            perioder = listOf(
                kravgrunnlagPeriode(eksisterendePeriode1),
                kravgrunnlagPeriode(eksisterendePeriode2),
            ),
        )

        val vilkårsvurderingsteg = Vilkårsvurderingsteg.opprett(
            eksternFagsakBehandling(),
            kravgrunnlag,
        )

        vilkårsvurderingsteg.nyPeriode(KravgrunnlagSammenligning.Forskjell.NyPeriode(nyPeriode, 2000.kroner))

        vilkårsvurderingsteg.tilFrontendDto().should {
            it.size shouldBe 1
            it[0].fom shouldBe eksisterendePeriode1.fom
            it[0].tom shouldBe eksisterendePeriode2.tom
            it[0].endringIKravgrunnlag shouldBe NyPeriodeDto(nyPeriode.fom, nyPeriode.tom, 2000)
        }

        val entity = vilkårsvurderingsteg.tilEntity(UUID.randomUUID())
        entity.vurderinger[2].vurdering.forrigePeriodeId shouldBe entity.vurderinger[1].id
    }

    @Test
    fun `endring i kravgrunnlag - ny periode etter ikke vurdert og vurdert periode`() {
        val ikkeVurdertPeriode = 1.januar(2021) til 31.januar(2021)
        val vurdertPeriode = 1.februar(2021) til 28.februar(2021)
        val nyPeriode = 1.mars(2021) til 31.mars(2021)
        val kravgrunnlag = kravgrunnlag(
            perioder = listOf(
                kravgrunnlagPeriode(ikkeVurdertPeriode),
                kravgrunnlagPeriode(vurdertPeriode),
            ),
        )

        val vilkårsvurderingsteg = Vilkårsvurderingsteg.opprett(
            eksternFagsakBehandling(),
            kravgrunnlag,
        )
        val vurdertPeriodeId = vilkårsvurderingsteg.hentVilkårsvurderingsperioder()
            .single { it.periode == PeriodeDto(vurdertPeriode.fom, vurdertPeriode.tom) }
            .periodeId
        vilkårsvurderingsteg.vurder(vurdertPeriodeId, forårsaketAvNav().godTro(beløpIBehold = null))

        vilkårsvurderingsteg.nyPeriode(KravgrunnlagSammenligning.Forskjell.NyPeriode(nyPeriode, 2000.kroner))

        vilkårsvurderingsteg.tilFrontendDto().should {
            it.size shouldBe 3
            it[0].fom shouldBe ikkeVurdertPeriode.fom
            it[0].tom shouldBe ikkeVurdertPeriode.tom
            it[1].fom shouldBe vurdertPeriode.fom
            it[1].tom shouldBe vurdertPeriode.tom
            it[2].fom shouldBe nyPeriode.fom
            it[2].tom shouldBe nyPeriode.tom
        }
    }

    @Test
    fun `endring i kravgrunnlag - ny periode mellom kopiert vurdering`() {
        val vurdertPeriode = 1.januar(2021) til 31.januar(2021)
        val kopiertPeriode = 1.mars(2021) til 31.mars(2021)
        val nyPeriode = 1.februar(2021) til 28.februar(2021)
        val kravgrunnlag = kravgrunnlag(
            perioder = listOf(
                kravgrunnlagPeriode(vurdertPeriode),
                kravgrunnlagPeriode(kopiertPeriode),
            ),
        )

        val vilkårsvurderingsteg = Vilkårsvurderingsteg.opprett(
            eksternFagsakBehandling(),
            kravgrunnlag,
        )
        vilkårsvurderingsteg.vurder(vurdertPeriode, forårsaketAvNav().godTro(beløpIBehold = null))

        vilkårsvurderingsteg.nyPeriode(KravgrunnlagSammenligning.Forskjell.NyPeriode(nyPeriode, 2000.kroner))

        vilkårsvurderingsteg.tilFrontendDto().should {
            it.size shouldBe 3
            it[0].fom shouldBe vurdertPeriode.fom
            it[0].tom shouldBe vurdertPeriode.tom
            it[0].endringIKravgrunnlag shouldBe null
            it[0].valg.shouldBeInstanceOf<GodTroDto>()
            it[1].fom shouldBe nyPeriode.fom
            it[1].tom shouldBe nyPeriode.tom
            it[1].endringIKravgrunnlag shouldBe NyPeriodeDto(nyPeriode.fom, nyPeriode.tom, 2000)
            it[1].valg.shouldBeInstanceOf<VilkaarsvurderingIkkeVurdertDto>()
            it[2].fom shouldBe kopiertPeriode.fom
            it[2].tom shouldBe kopiertPeriode.tom
            it[2].endringIKravgrunnlag shouldBe null
            it[2].valg.shouldBeInstanceOf<VilkaarsvurderingIkkeVurdertDto>()
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
