package no.nav.familie.tilbake.iverksettvedtak

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.Fagsystem
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.foreldelse.ForeldelseService
import no.nav.familie.tilbake.iverksettvedtak.VilkårsvurderingsPeriodeDomainUtil.lagGrovtUaktsomVilkårsvurderingsperiode
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagMapper
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagUtil
import no.nav.familie.tilbake.kravgrunnlag.domain.Fagområdekode
import no.nav.familie.tilbake.kravgrunnlag.domain.GjelderType
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassekode
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsbeløp433
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsperiode432
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravstatuskode
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingService
import no.nav.tilbakekreving.api.v1.dto.AktsomhetDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegForeldelseDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegVilkårsvurderingDto
import no.nav.tilbakekreving.api.v1.dto.ForeldelsesperiodeDto
import no.nav.tilbakekreving.api.v1.dto.GodTroDto
import no.nav.tilbakekreving.api.v1.dto.SkalUnnlates
import no.nav.tilbakekreving.api.v1.dto.SærligGrunnDto
import no.nav.tilbakekreving.api.v1.dto.VilkårsvurderingsperiodeDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.PosteringDto
import no.nav.tilbakekreving.kontrakter.foreldelse.Foreldelsesvurderingstype
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.periode.Månedsperiode
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunnType.ANNET
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunnType.GRAD_AV_UAKTSOMHET
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunnType.HELT_ELLER_DELVIS_NAVS_FEIL
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import no.nav.tilbakekreving.shouldGiveOne
import no.nav.tilbakekreving.test.februar
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.test.mars
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.math.BigInteger
import java.time.YearMonth

internal class TilbakekrevingsvedtakBeregningServiceTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var vilkårsvurderingService: VilkårsvurderingService

    @Autowired
    private lateinit var foreldelsesService: ForeldelseService

    @Autowired
    private lateinit var vedtakBeregningService: TilbakekrevingsvedtakBeregningService

    @Autowired
    private lateinit var iverksettelseService: IverksettelseService

    private lateinit var fagsak: Fagsak
    private lateinit var behandling: Behandling

    private val perioder = listOf(
        Månedsperiode(YearMonth.of(2021, 1), YearMonth.of(2021, 1)),
        Månedsperiode(YearMonth.of(2021, 2), YearMonth.of(2021, 2)),
    )

    private lateinit var kravgrunnlag: Kravgrunnlag431

    @BeforeEach
    fun init() {
        fagsak = Testdata.fagsak()
        behandling = Testdata.lagBehandling(fagsakId = fagsak.id)
        fagsakRepository.insert(fagsak)
        behandlingRepository.insert(behandling)

        val månedligSkattBeløp = BigDecimal.ZERO
        val kravgrunnlagsbeløpene = listOf(
            Kravgrunnlagsbeløp(klassetype = Klassetype.FEIL, nyttBeløp = BigDecimal(5000)),
            Kravgrunnlagsbeløp(
                klassetype = Klassetype.YTEL,
                opprinneligUtbetalingsbeløp = BigDecimal(5000),
                tilbakekrevesBeløp = BigDecimal(5000),
            ),
        )

        kravgrunnlag = lagKravgrunnlag(perioder, månedligSkattBeløp, kravgrunnlagsbeløpene)
        kravgrunnlagRepository.insert(kravgrunnlag)
    }

    @Test
    fun `beregnVedtaksperioder skal beregne når vilkårsvurderte med 50 prosent andel tilbakekrevesbeløp`() {
        lagAktsomhetVilkårsvurdering(
            perioder = listOf(
                Månedsperiode(
                    YearMonth.of(2021, 1),
                    YearMonth.of(2021, 2),
                ),
            ),
            aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
            andelTilbakreves = BigDecimal(50),
            særligeGrunnerTilReduksjon = true,
        )

        val tilbakekrevingsperioder = vedtakBeregningService
            .beregnVedtaksperioder(behandling.id, kravgrunnlag)
            .sortedBy { it.periodeFom }
        tilbakekrevingsperioder.shouldNotBeNull()
        tilbakekrevingsperioder.size shouldBe 2

        val førstePeriode = tilbakekrevingsperioder[0]
        førstePeriode.periodeFom shouldBe 1.januar(2021)
        førstePeriode.periodeTom shouldBe 31.januar(2021)
        førstePeriode.belopRenter shouldBe BigDecimal.ZERO
        var feilPostering = førstePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_BA" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_BA,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(5000),
            kodeResultat = "",
        )
        var ytelsePostering = førstePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "BATR" }
        assertBeløp(
            klassekode = Klassekode.BATR,
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(2500),
            uinnkrevdBeløp = BigDecimal(2500),
            kodeResultat = "DELVIS_TILBAKEKREV",
        )

        val andrePeriode = tilbakekrevingsperioder[1]
        andrePeriode.periodeFom shouldBe 1.februar(2021)
        andrePeriode.periodeTom shouldBe 28.februar(2021)
        andrePeriode.belopRenter shouldBe BigDecimal.ZERO
        feilPostering = andrePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_BA" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_BA,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(5000),
            kodeResultat = "",
        )
        ytelsePostering = andrePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "BATR" }
        assertBeløp(
            klassekode = Klassekode.BATR,
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(2500),
            uinnkrevdBeløp = BigDecimal(2500),
            kodeResultat = "DELVIS_TILBAKEKREV",
        )
    }

    @Test
    fun `beregnVedtaksperioder skal beregne når vilkårsvurderte med 33 prosent andel tilbakekrevesbeløp`() {
        lagAktsomhetVilkårsvurdering(
            perioder = listOf(
                Månedsperiode(
                    YearMonth.of(2021, 1),
                    YearMonth.of(2021, 2),
                ),
            ),
            aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
            andelTilbakreves = BigDecimal(33),
            særligeGrunnerTilReduksjon = true,
        )

        val tilbakekrevingsperioder = vedtakBeregningService
            .beregnVedtaksperioder(behandling.id, kravgrunnlag)
            .sortedBy { it.periodeFom }
        tilbakekrevingsperioder.shouldNotBeNull()
        tilbakekrevingsperioder.size shouldBe 2

        val førstePeriode = tilbakekrevingsperioder[0]
        førstePeriode.periodeFom shouldBe 1.januar(2021)
        førstePeriode.periodeTom shouldBe 31.januar(2021)
        førstePeriode.belopRenter shouldBe BigDecimal.ZERO
        var feilPostering = førstePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_BA" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_BA,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(5000),
            kodeResultat = "",
        )
        var ytelsePostering = førstePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "BATR" }
        assertBeløp(
            klassekode = Klassekode.BATR,
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(1650),
            uinnkrevdBeløp = BigDecimal(3350),
            kodeResultat = "DELVIS_TILBAKEKREV",
        )

        val andrePeriode = tilbakekrevingsperioder[1]
        andrePeriode.periodeFom shouldBe 1.februar(2021)
        andrePeriode.periodeTom shouldBe 28.februar(2021)
        andrePeriode.belopRenter shouldBe BigDecimal.ZERO
        feilPostering = andrePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_BA" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_BA,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(5000),
            kodeResultat = "",
        )
        ytelsePostering = andrePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "BATR" }
        assertBeløp(
            klassekode = Klassekode.BATR,
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(1650),
            uinnkrevdBeløp = BigDecimal(3350),
            kodeResultat = "DELVIS_TILBAKEKREV",
        )
    }

    @Test
    fun `beregnVedtaksperioder skal beregne når vilkårsvurderte med Forsett aktsomhet`() {
        lagAktsomhetVilkårsvurdering(
            perioder = listOf(
                Månedsperiode(
                    YearMonth.of(2021, 1),
                    YearMonth.of(2021, 2),
                ),
            ),
            aktsomhet = Aktsomhet.FORSETT,
        )

        val tilbakekrevingsperioder = vedtakBeregningService
            .beregnVedtaksperioder(behandling.id, kravgrunnlag)
            .sortedBy { it.periodeFom }
        tilbakekrevingsperioder.shouldNotBeNull()
        tilbakekrevingsperioder.size shouldBe 2

        val førstePeriode = tilbakekrevingsperioder[0]
        førstePeriode.periodeFom shouldBe 1.januar(2021)
        førstePeriode.periodeTom shouldBe 31.januar(2021)
        førstePeriode.belopRenter shouldBe BigDecimal.ZERO
        var feilPostering = førstePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_BA" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_BA,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(5000),
            kodeResultat = "",
        )
        var ytelsePostering = førstePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "BATR" }
        assertBeløp(
            klassekode = Klassekode.BATR,
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(5000),
            uinnkrevdBeløp = BigDecimal.ZERO,
            kodeResultat = "FULL_TILBAKEKREV",
        )

        val andrePeriode = tilbakekrevingsperioder[1]
        andrePeriode.periodeFom shouldBe 1.februar(2021)
        andrePeriode.periodeTom shouldBe 28.februar(2021)
        andrePeriode.belopRenter shouldBe BigDecimal.ZERO
        feilPostering = andrePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_BA" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_BA,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(5000),
            kodeResultat = "",
        )
        ytelsePostering = andrePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "BATR" }
        assertBeløp(
            klassekode = Klassekode.BATR,
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(5000),
            uinnkrevdBeløp = BigDecimal.ZERO,
            kodeResultat = "FULL_TILBAKEKREV",
        )
    }

    @Test
    fun `beregnVedtaksperioder skal beregne når vilkårsvurderte med God tro og ingen tilbakekreving`() {
        lagGodTroVilkårsvurdering(
            perioder = listOf(
                Månedsperiode(
                    YearMonth.of(2021, 1),
                    YearMonth.of(2021, 2),
                ),
            ),
        )

        val tilbakekrevingsperioder = vedtakBeregningService
            .beregnVedtaksperioder(behandling.id, kravgrunnlag)
            .sortedBy { it.periodeFom }
        tilbakekrevingsperioder.shouldNotBeNull()
        tilbakekrevingsperioder.size shouldBe 2

        val førstePeriode = tilbakekrevingsperioder[0]
        førstePeriode.periodeFom shouldBe 1.januar(2021)
        førstePeriode.periodeTom shouldBe 31.januar(2021)
        førstePeriode.belopRenter shouldBe BigDecimal.ZERO
        var feilPostering = førstePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_BA" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_BA,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(5000),
            kodeResultat = "",
        )
        var ytelsePostering = førstePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "BATR" }
        assertBeløp(
            klassekode = Klassekode.BATR,
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal.ZERO,
            uinnkrevdBeløp = BigDecimal(5000),
            kodeResultat = "INGEN_TILBAKEKREV",
        )

        val andrePeriode = tilbakekrevingsperioder[1]
        andrePeriode.periodeFom shouldBe 1.februar(2021)
        andrePeriode.periodeTom shouldBe 28.februar(2021)
        andrePeriode.belopRenter shouldBe BigDecimal.ZERO
        feilPostering = andrePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_BA" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_BA,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(5000),
            kodeResultat = "",
        )
        ytelsePostering = andrePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "BATR" }
        assertBeløp(
            klassekode = Klassekode.BATR,
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal.ZERO,
            uinnkrevdBeløp = BigDecimal(5000),
            kodeResultat = "INGEN_TILBAKEKREV",
        )
    }

    @Test
    fun `beregnVedtaksperioder skal beregne når vilkårsvurderte med God tro og bestemt tilbakekrevesbeløp`() {
        lagGodTroVilkårsvurdering(
            perioder = listOf(
                Månedsperiode(
                    YearMonth.of(2021, 1),
                    YearMonth.of(2021, 2),
                ),
            ),
            beløpErIBehold = true,
            beløpTilbakekreves = BigDecimal(3000),
        )

        val tilbakekrevingsperioder = vedtakBeregningService
            .beregnVedtaksperioder(behandling.id, kravgrunnlag)
            .sortedBy { it.periodeFom }
        tilbakekrevingsperioder.shouldNotBeNull()
        tilbakekrevingsperioder.size shouldBe 2

        val førstePeriode = tilbakekrevingsperioder[0]
        førstePeriode.periodeFom shouldBe 1.januar(2021)
        førstePeriode.periodeTom shouldBe 31.januar(2021)
        førstePeriode.belopRenter shouldBe BigDecimal.ZERO
        var feilPostering = førstePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_BA" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_BA,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(5000),
            kodeResultat = "",
        )
        var ytelsePostering = førstePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "BATR" }
        assertBeløp(
            klassekode = Klassekode.BATR,
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(1500),
            uinnkrevdBeløp = BigDecimal(3500),
            kodeResultat = "DELVIS_TILBAKEKREV",
        )

        val andrePeriode = tilbakekrevingsperioder[1]
        andrePeriode.periodeFom shouldBe 1.februar(2021)
        andrePeriode.periodeTom shouldBe 28.februar(2021)
        andrePeriode.belopRenter shouldBe BigDecimal.ZERO
        feilPostering = andrePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_BA" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_BA,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(5000),
            kodeResultat = "",
        )
        ytelsePostering = andrePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "BATR" }
        assertBeløp(
            klassekode = Klassekode.BATR,
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(1500),
            uinnkrevdBeløp = BigDecimal(3500),
            kodeResultat = "DELVIS_TILBAKEKREV",
        )
    }

    @Test
    fun `beregnVedtaksperioder skal beregne når vilkårsvurderte med God tro og bestemt tilbakekrevesbeløp med avrunding`() {
        lagGodTroVilkårsvurdering(
            perioder = listOf(
                Månedsperiode(
                    YearMonth.of(2021, 1),
                    YearMonth.of(2021, 2),
                ),
            ),
            beløpErIBehold = true,
            beløpTilbakekreves = BigDecimal(1999),
        )

        val tilbakekrevingsperioder = vedtakBeregningService
            .beregnVedtaksperioder(behandling.id, kravgrunnlag)
            .sortedBy { it.periodeFom }
        tilbakekrevingsperioder.shouldNotBeNull()
        tilbakekrevingsperioder.size shouldBe 2

        val førstePeriode = tilbakekrevingsperioder[0]
        førstePeriode.periodeFom shouldBe 1.januar(2021)
        førstePeriode.periodeTom shouldBe 31.januar(2021)
        førstePeriode.belopRenter shouldBe BigDecimal.ZERO
        var feilPostering = førstePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_BA" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_BA,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(5000),
            kodeResultat = "",
        )
        var ytelsePostering = førstePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "BATR" }
        assertBeløp(
            klassekode = Klassekode.BATR,
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(1000),
            uinnkrevdBeløp = BigDecimal(4000),
            kodeResultat = "DELVIS_TILBAKEKREV",
        )

        val andrePeriode = tilbakekrevingsperioder[1]
        andrePeriode.periodeFom shouldBe 1.februar(2021)
        andrePeriode.periodeTom shouldBe 28.februar(2021)
        andrePeriode.belopRenter shouldBe BigDecimal.ZERO
        feilPostering = andrePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_BA" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_BA,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(5000),
            kodeResultat = "",
        )
        ytelsePostering = andrePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "BATR" }
        assertBeløp(
            klassekode = Klassekode.BATR,
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(999),
            uinnkrevdBeløp = BigDecimal(4001),
            kodeResultat = "DELVIS_TILBAKEKREV",
        )
    }

    @Test
    fun `beregnVedtaksperioder skal beregne når vilkårsvurderte med 50 prosent andeltilbakekrevesbeløp med skatt beløp`() {
        val månedligSkattBeløp = BigDecimal(500)

        kravgrunnlagRepository.deleteById(kravgrunnlag.id)
        val kravgrunnlagsbeløpene = listOf(
            Kravgrunnlagsbeløp(
                klassetype = Klassetype.FEIL,
                nyttBeløp = BigDecimal(5000),
                skatteprosent = BigDecimal(10),
            ),
            Kravgrunnlagsbeløp(
                klassetype = Klassetype.YTEL,
                opprinneligUtbetalingsbeløp = BigDecimal(5000),
                tilbakekrevesBeløp = BigDecimal(5000),
                skatteprosent = BigDecimal(10),
            ),
        )

        val kravgrunnlag = lagKravgrunnlag(perioder, månedligSkattBeløp, kravgrunnlagsbeløpene)
        kravgrunnlagRepository.insert(kravgrunnlag)

        lagAktsomhetVilkårsvurdering(
            perioder = listOf(
                Månedsperiode(
                    YearMonth.of(2021, 1),
                    YearMonth.of(2021, 2),
                ),
            ),
            aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
            andelTilbakreves = BigDecimal(50),
            særligeGrunnerTilReduksjon = true,
        )

        val tilbakekrevingsperioder = vedtakBeregningService
            .beregnVedtaksperioder(behandling.id, kravgrunnlag)
            .sortedBy { it.periodeFom }
        tilbakekrevingsperioder.shouldNotBeNull()
        tilbakekrevingsperioder.size shouldBe 2

        val førstePeriode = tilbakekrevingsperioder[0]
        førstePeriode.periodeFom shouldBe 1.januar(2021)
        førstePeriode.periodeTom shouldBe 31.januar(2021)
        førstePeriode.belopRenter shouldBe BigDecimal.ZERO
        var feilPostering = førstePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_BA" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_BA,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(5000),
            kodeResultat = "",
        )
        var ytelsePostering = førstePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "BATR" }
        assertBeløp(
            klassekode = Klassekode.BATR,
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(2500),
            uinnkrevdBeløp = BigDecimal(2500),
            skattBeløp = BigDecimal(250),
            kodeResultat = "DELVIS_TILBAKEKREV",
        )

        val andrePeriode = tilbakekrevingsperioder[1]
        andrePeriode.periodeFom shouldBe 1.februar(2021)
        andrePeriode.periodeTom shouldBe 28.februar(2021)
        andrePeriode.belopRenter shouldBe BigDecimal.ZERO
        feilPostering = andrePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_BA" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_BA,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(5000),
            kodeResultat = "",
        )
        ytelsePostering = andrePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "BATR" }
        assertBeløp(
            klassekode = Klassekode.BATR,
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(2500),
            uinnkrevdBeløp = BigDecimal(2500),
            skattBeløp = BigDecimal(250),
            kodeResultat = "DELVIS_TILBAKEKREV",
        )
    }

    @Test
    fun `beregnVedtaksperioder skal beregne med en foreldet periode,en vilkårsvurdert periode med 100 prosent tilbakekreving`() {
        lagForeldelse(listOf(perioder[0]))

        lagAktsomhetVilkårsvurdering(listOf(perioder[1]), Aktsomhet.GROV_UAKTSOMHET)

        val tilbakekrevingsperioder = vedtakBeregningService
            .beregnVedtaksperioder(behandling.id, kravgrunnlag)
            .sortedBy { it.periodeFom }
        tilbakekrevingsperioder.shouldNotBeNull()
        tilbakekrevingsperioder.size shouldBe 2

        val førstePeriode = tilbakekrevingsperioder[0]
        førstePeriode.periodeFom shouldBe 1.januar(2021)
        førstePeriode.periodeTom shouldBe 31.januar(2021)
        førstePeriode.belopRenter shouldBe BigDecimal.ZERO
        var feilPostering = førstePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_BA" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_BA,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(5000),
            kodeResultat = "",
        )
        var ytelsePostering = førstePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "BATR" }
        assertBeløp(
            klassekode = Klassekode.BATR,
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(0),
            uinnkrevdBeløp = BigDecimal(5000),
            skattBeløp = BigDecimal(0),
            kodeResultat = "FORELDET",
        )

        val andrePeriode = tilbakekrevingsperioder[1]
        andrePeriode.periodeFom shouldBe 1.februar(2021)
        andrePeriode.periodeTom shouldBe 28.februar(2021)
        andrePeriode.belopRenter shouldBe BigDecimal.ZERO
        feilPostering = andrePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_BA" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_BA,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(5000),
            kodeResultat = "",
        )
        ytelsePostering = andrePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "BATR" }
        assertBeløp(
            klassekode = Klassekode.BATR,
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(5000),
            uinnkrevdBeløp = BigDecimal(0),
            skattBeløp = BigDecimal(0),
            kodeResultat = "FULL_TILBAKEKREV",
        )
    }

    @Test
    fun `beregnVedtaksperioder skal beregne med tre vilkårsvurdert periode med 100 prosent tilbakekreving`() {
        kravgrunnlagRepository.deleteById(kravgrunnlag.id)

        val månedligSkattBeløp = BigDecimal(750)
        val perioder = listOf(
            Månedsperiode(YearMonth.of(2021, 1), YearMonth.of(2021, 1)),
            Månedsperiode(YearMonth.of(2021, 2), YearMonth.of(2021, 2)),
            Månedsperiode(YearMonth.of(2021, 3), YearMonth.of(2021, 3)),
        )

        val kravgrunnlagsbeløpene = listOf(
            Kravgrunnlagsbeløp(
                klassetype = Klassetype.FEIL,
                nyttBeløp = BigDecimal(5000),
                skatteprosent = BigDecimal(15),
            ),
            Kravgrunnlagsbeløp(
                klassetype = Klassetype.YTEL,
                opprinneligUtbetalingsbeløp = BigDecimal(5000),
                tilbakekrevesBeløp = BigDecimal(5000),
                skatteprosent = BigDecimal(15),
            ),
        )
        val kravgrunnlag = lagKravgrunnlag(perioder, månedligSkattBeløp, kravgrunnlagsbeløpene)
        kravgrunnlagRepository.insert(kravgrunnlag)

        // en beregnet periode med 100 prosent tilbakekreving
        lagAktsomhetVilkårsvurdering(
            listOf(Månedsperiode(YearMonth.of(2021, 1), YearMonth.of(2021, 3))),
            Aktsomhet.GROV_UAKTSOMHET,
        )

        val tilbakekrevingsperioder = vedtakBeregningService
            .beregnVedtaksperioder(behandling.id, kravgrunnlag)
            .sortedBy { it.periodeFom }
        tilbakekrevingsperioder.shouldNotBeNull()
        tilbakekrevingsperioder.size shouldBe 3

        val førstePeriode = tilbakekrevingsperioder[0]
        førstePeriode.periodeFom shouldBe 1.januar(2021)
        førstePeriode.periodeTom shouldBe 31.januar(2021)
        førstePeriode.belopRenter shouldBe BigDecimal.ZERO
        var feilPostering = førstePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_BA" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_BA,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(5000),
            kodeResultat = "",
        )
        var ytelsePostering = førstePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "BATR" }
        assertBeløp(
            klassekode = Klassekode.BATR,
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(5000),
            uinnkrevdBeløp = BigDecimal(0),
            skattBeløp = BigDecimal(750),
            kodeResultat = "FULL_TILBAKEKREV",
        )

        val andrePeriode = tilbakekrevingsperioder[1]
        andrePeriode.periodeFom shouldBe 1.februar(2021)
        andrePeriode.periodeTom shouldBe 28.februar(2021)
        andrePeriode.belopRenter shouldBe BigDecimal.ZERO
        feilPostering = andrePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_BA" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_BA,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(5000),
            kodeResultat = "",
        )
        ytelsePostering = andrePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "BATR" }
        assertBeløp(
            klassekode = Klassekode.BATR,
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(5000),
            uinnkrevdBeløp = BigDecimal(0),
            skattBeløp = BigDecimal(750),
            kodeResultat = "FULL_TILBAKEKREV",
        )

        val tredjePeriode = tilbakekrevingsperioder[2]
        tredjePeriode.periodeFom shouldBe 1.mars(2021)
        tredjePeriode.periodeTom shouldBe 31.mars(2021)
        tredjePeriode.belopRenter shouldBe BigDecimal.ZERO
        feilPostering = tredjePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_BA" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_BA,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(5000),
            kodeResultat = "",
        )
        ytelsePostering = tredjePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "BATR" }
        assertBeløp(
            klassekode = Klassekode.BATR,
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(5000),
            uinnkrevdBeløp = BigDecimal(0),
            skattBeløp = BigDecimal(750),
            kodeResultat = "FULL_TILBAKEKREV",
        )
    }

    @Test
    fun `beregnVedtaksperioder skal beregne med 2 foreldet, 2 god tro og 3 periode med 100 prosent tilbakekreving`() {
        kravgrunnlagRepository.deleteById(kravgrunnlag.id)

        val perioder = listOf(
            Månedsperiode(YearMonth.of(2020, 1), YearMonth.of(2020, 1)),
            Månedsperiode(YearMonth.of(2020, 3), YearMonth.of(2020, 3)),
            Månedsperiode(YearMonth.of(2020, 5), YearMonth.of(2020, 5)),
            Månedsperiode(YearMonth.of(2020, 7), YearMonth.of(2020, 7)),
            Månedsperiode(YearMonth.of(2020, 9), YearMonth.of(2020, 9)),
            Månedsperiode(YearMonth.of(2020, 11), YearMonth.of(2020, 11)),
            Månedsperiode(YearMonth.of(2021, 1), YearMonth.of(2021, 1)),
        )

        val kravgrunnlagsbeløpene = listOf(
            Kravgrunnlagsbeløp(
                klassetype = Klassetype.FEIL,
                nyttBeløp = BigDecimal(952.38),
            ),
            Kravgrunnlagsbeløp(
                klassetype = Klassetype.YTEL,
                opprinneligUtbetalingsbeløp = BigDecimal(952.38),
                tilbakekrevesBeløp = BigDecimal(952.38),
            ),
        )
        val kravgrunnlag = lagKravgrunnlag(perioder, BigDecimal.ZERO, kravgrunnlagsbeløpene)
        kravgrunnlagRepository.insert(kravgrunnlag)

        // 1,2 beregnet periode er foreldet
        lagForeldelse(listOf(perioder[0], perioder[1]))

        // 3,4 beregnet periode er godtro med ingen tilbakekreving
        val godtroPerioder = listOf(perioder[2], perioder[3]).map {
            VilkårsvurderingsperiodeDto(
                periode = it.toDatoperiode(),
                begrunnelse = "testverdi",
                godTroDto = GodTroDto(begrunnelse = "testverdi", beløpErIBehold = false),
                vilkårsvurderingsresultat = Vilkårsvurderingsresultat.GOD_TRO,
            )
        }

        // 5,6,7 beregnet periode er Forsett aktsomhet med Full tilbakekreving
        val aktsomhetPerioder = listOf(perioder[4], perioder[5], perioder[6]).map {
            VilkårsvurderingsperiodeDto(
                periode = it.toDatoperiode(),
                begrunnelse = "testverdi",
                aktsomhetDto = AktsomhetDto(aktsomhet = Aktsomhet.FORSETT, unnlates4Rettsgebyr = SkalUnnlates.TILBAKEKREVES, begrunnelse = "testverdi"),
                vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
            )
        }
        vilkårsvurderingService.lagreVilkårsvurdering(
            behandling.id,
            BehandlingsstegVilkårsvurderingDto(
                godtroPerioder +
                    aktsomhetPerioder,
            ),
        )

        val tilbakekrevingsperioder = vedtakBeregningService
            .beregnVedtaksperioder(behandling.id, kravgrunnlag)
            .sortedBy { it.periodeFom }
        tilbakekrevingsperioder.shouldNotBeNull()
        tilbakekrevingsperioder.size shouldBe 7

        val førstePeriode = tilbakekrevingsperioder[0]
        førstePeriode.periodeFom shouldBe Månedsperiode(YearMonth.of(2020, 1), YearMonth.of(2020, 1)).fomDato
        førstePeriode.periodeTom shouldBe Månedsperiode(YearMonth.of(2020, 1), YearMonth.of(2020, 1)).tomDato
        førstePeriode.belopRenter shouldBe BigDecimal.ZERO
        var feilPostering = førstePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_BA" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_BA,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(952),
            kodeResultat = "",
        )
        var ytelsePostering = førstePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "BATR" }
        assertBeløp(
            klassekode = Klassekode.BATR,
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(952),
            tilbakekrevesBeløp = BigDecimal.ZERO,
            uinnkrevdBeløp = BigDecimal(952),
            kodeResultat = "FORELDET",
        )

        val andrePeriode = tilbakekrevingsperioder[1]
        andrePeriode.periodeFom shouldBe Månedsperiode(YearMonth.of(2020, 3), YearMonth.of(2020, 3)).fomDato
        andrePeriode.periodeTom shouldBe Månedsperiode(YearMonth.of(2020, 3), YearMonth.of(2020, 3)).tomDato
        andrePeriode.belopRenter shouldBe BigDecimal.ZERO
        feilPostering = andrePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_BA" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_BA,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(952),
            kodeResultat = "",
        )
        ytelsePostering = andrePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "BATR" }
        assertBeløp(
            klassekode = Klassekode.BATR,
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(952),
            tilbakekrevesBeløp = BigDecimal.ZERO,
            uinnkrevdBeløp = BigDecimal(952),
            kodeResultat = "FORELDET",
        )

        val tredjePeriode = tilbakekrevingsperioder[2]
        tredjePeriode.periodeFom shouldBe Månedsperiode(YearMonth.of(2020, 5), YearMonth.of(2020, 5)).fomDato
        tredjePeriode.periodeTom shouldBe Månedsperiode(YearMonth.of(2020, 5), YearMonth.of(2020, 5)).tomDato
        tredjePeriode.belopRenter shouldBe BigDecimal.ZERO
        feilPostering = tredjePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_BA" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_BA,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(952),
            kodeResultat = "",
        )
        ytelsePostering = tredjePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "BATR" }
        assertBeløp(
            klassekode = Klassekode.BATR,
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(952),
            tilbakekrevesBeløp = BigDecimal.ZERO,
            uinnkrevdBeløp = BigDecimal(952),
            kodeResultat = "INGEN_TILBAKEKREV",
        )

        val fjerdePeriode = tilbakekrevingsperioder[3]
        fjerdePeriode.periodeFom shouldBe Månedsperiode(YearMonth.of(2020, 7), YearMonth.of(2020, 7)).fomDato
        fjerdePeriode.periodeTom shouldBe Månedsperiode(YearMonth.of(2020, 7), YearMonth.of(2020, 7)).tomDato
        fjerdePeriode.belopRenter shouldBe BigDecimal.ZERO
        feilPostering = fjerdePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_BA" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_BA,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(952),
            kodeResultat = "",
        )
        ytelsePostering = fjerdePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "BATR" }
        assertBeløp(
            klassekode = Klassekode.BATR,
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(952),
            tilbakekrevesBeløp = BigDecimal.ZERO,
            uinnkrevdBeløp = BigDecimal(952),
            kodeResultat = "INGEN_TILBAKEKREV",
        )

        val femtePeriode = tilbakekrevingsperioder[4]
        femtePeriode.periodeFom shouldBe Månedsperiode(YearMonth.of(2020, 9), YearMonth.of(2020, 9)).fomDato
        femtePeriode.periodeTom shouldBe Månedsperiode(YearMonth.of(2020, 9), YearMonth.of(2020, 9)).tomDato
        femtePeriode.belopRenter shouldBe BigDecimal.ZERO
        feilPostering = femtePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_BA" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_BA,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(952),
            kodeResultat = "",
        )
        ytelsePostering = femtePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "BATR" }
        assertBeløp(
            klassekode = Klassekode.BATR,
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(952),
            tilbakekrevesBeløp = BigDecimal(952),
            uinnkrevdBeløp = BigDecimal.ZERO,
            kodeResultat = "FULL_TILBAKEKREV",
        )

        val sjettePeriode = tilbakekrevingsperioder[5]
        sjettePeriode.periodeFom shouldBe Månedsperiode(YearMonth.of(2020, 11), YearMonth.of(2020, 11)).fomDato
        sjettePeriode.periodeTom shouldBe Månedsperiode(YearMonth.of(2020, 11), YearMonth.of(2020, 11)).tomDato
        sjettePeriode.belopRenter shouldBe BigDecimal.ZERO
        feilPostering = sjettePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_BA" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_BA,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(952),
            kodeResultat = "",
        )
        ytelsePostering = sjettePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "BATR" }
        assertBeløp(
            klassekode = Klassekode.BATR,
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(952),
            tilbakekrevesBeløp = BigDecimal(952),
            uinnkrevdBeløp = BigDecimal.ZERO,
            kodeResultat = "FULL_TILBAKEKREV",
        )

        val sjuendePeriode = tilbakekrevingsperioder[6]
        sjuendePeriode.periodeFom shouldBe 1.januar(2021)
        sjuendePeriode.periodeTom shouldBe 31.januar(2021)
        sjuendePeriode.belopRenter shouldBe BigDecimal.ZERO
        feilPostering = sjuendePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_BA" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_BA,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(952),
            kodeResultat = "",
        )
        ytelsePostering = sjuendePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "BATR" }
        assertBeløp(
            klassekode = Klassekode.BATR,
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(952),
            tilbakekrevesBeløp = BigDecimal(952),
            uinnkrevdBeløp = BigDecimal.ZERO,
            kodeResultat = "FULL_TILBAKEKREV",
        )
    }

    @Test
    fun `beregnVedtaksperioder skal beregne perioder med 2 god tro, 3 50 prosent og 3 100 prosent tilbakekreving med renter`() {
        kravgrunnlagRepository.deleteById(kravgrunnlag.id)
        fagsakRepository.update(fagsakRepository.findByIdOrThrow(fagsak.id).copy(fagsystem = Fagsystem.EF))

        val kravgrunnlagxml = readXml("/kravgrunnlagxml/kravgrunnlag_EF_med_renter.xml")
        val kravgrunnlag = KravgrunnlagMapper.tilKravgrunnlag431(
            KravgrunnlagUtil.unmarshalKravgrunnlag(kravgrunnlagxml),
            behandling.id,
        )
        kravgrunnlagRepository.insert(kravgrunnlag)

        val sortedPerioder = kravgrunnlag.perioder.map { it.periode }.sortedBy { it.fom }

        // 1,2 perioder er vilkårsvurdert med god tro(ingen tilbakebetaling)
        val godTroPeriode = VilkårsvurderingsperiodeDto(
            periode = Datoperiode(sortedPerioder[0].fom, sortedPerioder[1].tom),
            begrunnelse = "testverdi",
            godTroDto = GodTroDto(
                begrunnelse = "testverdi",
                beløpErIBehold = false,
            ),
            vilkårsvurderingsresultat = Vilkårsvurderingsresultat.GOD_TRO,
        )

        val særligGrunner = listOf(SærligGrunnDto(ANNET, "testverdi"))
        // 3,4 perioder er vilkårsvurdert med SIMPEL UAKTSOMHET(50 prosent tilbakebetaling)
        val simpelUaktsomhetPeriode = VilkårsvurderingsperiodeDto(
            periode = Datoperiode(
                sortedPerioder[2].fom,
                sortedPerioder[3].tom,
            ),
            begrunnelse = "testverdi",
            aktsomhetDto = AktsomhetDto(
                aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
                ileggRenter = false,
                andelTilbakekreves = BigDecimal(50),
                begrunnelse = "testverdi",
                særligeGrunnerTilReduksjon = true,
                unnlates4Rettsgebyr = SkalUnnlates.TILBAKEKREVES,
                særligeGrunnerBegrunnelse = "testverdi",
                særligeGrunner = særligGrunner,
            ),
            vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
        )

        // 5,6,7 perioder er vilkårsvurdert med GROV UAKTSOMHET(100 prosent tilbakebetaling)
        val grovUaktsomhetPeriode = VilkårsvurderingsperiodeDto(
            periode = Datoperiode(
                sortedPerioder[4].fom,
                sortedPerioder[6].tom,
            ),
            begrunnelse = "testverdi",
            aktsomhetDto = AktsomhetDto(
                aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                ileggRenter = true,
                andelTilbakekreves = null,
                begrunnelse = "testverdi",
                særligeGrunnerTilReduksjon = false,
                unnlates4Rettsgebyr = SkalUnnlates.TILBAKEKREVES,
                særligeGrunnerBegrunnelse = "testverdi",
                særligeGrunner = særligGrunner,
            ),
            vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
        )
        vilkårsvurderingService.lagreVilkårsvurdering(
            behandling.id,
            BehandlingsstegVilkårsvurderingDto(
                listOf(
                    godTroPeriode,
                    simpelUaktsomhetPeriode,
                    grovUaktsomhetPeriode,
                ),
            ),
        )

        val tilbakekrevingsperioder = vedtakBeregningService
            .beregnVedtaksperioder(behandling.id, kravgrunnlag)
            .sortedBy { it.periodeFom }
        tilbakekrevingsperioder.shouldNotBeNull()
        tilbakekrevingsperioder.size shouldBe 7

        val førstePeriode = tilbakekrevingsperioder[0]
        førstePeriode.periodeFom shouldBe sortedPerioder[0].fomDato
        førstePeriode.periodeTom shouldBe sortedPerioder[0].tomDato
        førstePeriode.belopRenter shouldBe BigDecimal.ZERO
        var feilPostering = førstePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_EFOG" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_EFOG,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(486),
            kodeResultat = "",
        )
        var ytelsePostering = førstePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "EFOG" }
        assertBeløp(
            klassekode = Klassekode.EFOG,
            beløp = ytelsePostering,
            nyttBeløp = BigDecimal(1544),
            utbetaltBeløp = BigDecimal(2030),
            tilbakekrevesBeløp = BigDecimal.ZERO,
            uinnkrevdBeløp = BigDecimal(486),
            kodeResultat = "INGEN_TILBAKEKREV",
        )

        val andrePeriode = tilbakekrevingsperioder[1]
        andrePeriode.periodeFom shouldBe sortedPerioder[1].fomDato
        andrePeriode.periodeTom shouldBe sortedPerioder[1].tomDato
        andrePeriode.belopRenter shouldBe BigDecimal.ZERO
        feilPostering = andrePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_EFOG" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_EFOG,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(17336),
            kodeResultat = "",
        )
        ytelsePostering = andrePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "EFOG" }
        assertBeløp(
            klassekode = Klassekode.EFOG,
            beløp = ytelsePostering,
            nyttBeløp = BigDecimal(3574),
            utbetaltBeløp = BigDecimal(20910),
            tilbakekrevesBeløp = BigDecimal.ZERO,
            uinnkrevdBeløp = BigDecimal(17336),
            kodeResultat = "INGEN_TILBAKEKREV",
        )

        val tredjePeriode = tilbakekrevingsperioder[2]
        tredjePeriode.periodeFom shouldBe sortedPerioder[2].fomDato
        tredjePeriode.periodeTom shouldBe sortedPerioder[2].tomDato
        tredjePeriode.belopRenter shouldBe BigDecimal.ZERO
        feilPostering = tredjePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_EFOG" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_EFOG,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(17241),
            kodeResultat = "",
        )
        ytelsePostering = tredjePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "EFOG" }
        assertBeløp(
            klassekode = Klassekode.EFOG,
            beløp = ytelsePostering,
            nyttBeløp = BigDecimal(5658),
            utbetaltBeløp = BigDecimal(22899),
            tilbakekrevesBeløp = BigDecimal(8621),
            uinnkrevdBeløp = BigDecimal(8620),
            skattBeløp = BigDecimal(4310),
            kodeResultat = "DELVIS_TILBAKEKREV",
        )

        val fjerdePeriode = tilbakekrevingsperioder[3]
        fjerdePeriode.periodeFom shouldBe sortedPerioder[3].fomDato
        fjerdePeriode.periodeTom shouldBe sortedPerioder[3].tomDato
        fjerdePeriode.belopRenter shouldBe BigDecimal.ZERO
        feilPostering = fjerdePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_EFOG" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_EFOG,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(17241),
            kodeResultat = "",
        )
        ytelsePostering = fjerdePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "EFOG" }
        assertBeløp(
            klassekode = Klassekode.EFOG,
            beløp = ytelsePostering,
            nyttBeløp = BigDecimal(5658),
            utbetaltBeløp = BigDecimal(22899),
            tilbakekrevesBeløp = BigDecimal(8620),
            uinnkrevdBeløp = BigDecimal(8621),
            skattBeløp = BigDecimal.ZERO,
            kodeResultat = "DELVIS_TILBAKEKREV",
        )

        val femtePeriode = tilbakekrevingsperioder[4]
        femtePeriode.periodeFom shouldBe sortedPerioder[4].fomDato
        femtePeriode.periodeTom shouldBe sortedPerioder[4].tomDato
        femtePeriode.belopRenter shouldBe BigDecimal(1724)
        feilPostering = femtePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_EFOG" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_EFOG,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(17241),
            kodeResultat = "",
        )
        ytelsePostering = femtePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "EFOG" }
        assertBeløp(
            klassekode = Klassekode.EFOG,
            beløp = ytelsePostering,
            nyttBeløp = BigDecimal(5658),
            utbetaltBeløp = BigDecimal(22899),
            tilbakekrevesBeløp = BigDecimal(17241),
            uinnkrevdBeløp = BigDecimal.ZERO,
            skattBeløp = BigDecimal(8620),
            kodeResultat = "FULL_TILBAKEKREV",
        )

        val sjettePeriode = tilbakekrevingsperioder[5]
        sjettePeriode.periodeFom shouldBe sortedPerioder[5].fomDato
        sjettePeriode.periodeTom shouldBe sortedPerioder[5].tomDato
        sjettePeriode.belopRenter shouldBe BigDecimal(1724)
        feilPostering = sjettePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_EFOG" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_EFOG,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(17241),
            kodeResultat = "",
        )
        ytelsePostering = sjettePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "EFOG" }
        assertBeløp(
            klassekode = Klassekode.EFOG,
            beløp = ytelsePostering,
            nyttBeløp = BigDecimal(5658),
            utbetaltBeløp = BigDecimal(22899),
            tilbakekrevesBeløp = BigDecimal(17241),
            uinnkrevdBeløp = BigDecimal.ZERO,
            skattBeløp = BigDecimal(8620),
            kodeResultat = "FULL_TILBAKEKREV",
        )

        val sjuendePeriode = tilbakekrevingsperioder[6]
        sjuendePeriode.periodeFom shouldBe sortedPerioder[6].fomDato
        sjuendePeriode.periodeTom shouldBe sortedPerioder[6].tomDato
        sjuendePeriode.belopRenter shouldBe BigDecimal(1736)
        feilPostering = sjuendePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_EFOG" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_EFOG,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(17364),
            kodeResultat = "",
        )
        ytelsePostering = sjuendePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "EFOG" }
        assertBeløp(
            klassekode = Klassekode.EFOG,
            beløp = ytelsePostering,
            nyttBeløp = BigDecimal(25485),
            utbetaltBeløp = BigDecimal(42849),
            tilbakekrevesBeløp = BigDecimal(17364),
            uinnkrevdBeløp = BigDecimal.ZERO,
            skattBeløp = BigDecimal(8682),
            kodeResultat = "FULL_TILBAKEKREV",
        )
    }

    @Test
    fun `beregnVedtaksperioder skal beregne periode med 100 prosent tilbakekreving og renter skal rundes ned`() {
        kravgrunnlagRepository.deleteById(kravgrunnlag.id)
        fagsakRepository.update(fagsakRepository.findByIdOrThrow(fagsak.id).copy(fagsystem = Fagsystem.EF))

        val kravgrunnlagxml = readXml("/kravgrunnlagxml/kravgrunnlag_EF_med_renter_avrundingsfeil_ned.xml")
        val kravgrunnlag = KravgrunnlagMapper.tilKravgrunnlag431(
            KravgrunnlagUtil.unmarshalKravgrunnlag(kravgrunnlagxml),
            behandling.id,
        )
        kravgrunnlagRepository.insert(kravgrunnlag)

        val periode = kravgrunnlag.perioder.first().periode

        val grovUaktsomhetPeriode = lagGrovtUaktsomVilkårsvurderingsperiode(periode.fom, periode.tom)

        vilkårsvurderingService.lagreVilkårsvurdering(
            behandling.id,
            BehandlingsstegVilkårsvurderingDto(listOf(grovUaktsomhetPeriode)),
        )

        val tilbakekrevingsperioder = vedtakBeregningService
            .beregnVedtaksperioder(behandling.id, kravgrunnlag)
            .sortedBy { it.periodeFom }
        tilbakekrevingsperioder.size shouldBe 1

        val tilbakekrevingsperiode = tilbakekrevingsperioder[0]
        tilbakekrevingsperiode.periodeFom shouldBe periode.fomDato
        tilbakekrevingsperiode.periodeTom shouldBe periode.tomDato
        tilbakekrevingsperiode.belopRenter shouldBe BigDecimal(1860)
    }

    @Test
    fun `beregnVedtaksperioder som beregner flere perioder i samme vilkårsperiode med 100 prosent tilbakekreving og renter skal aldri overstige 10%`() {
        kravgrunnlagRepository.deleteById(kravgrunnlag.id)
        fagsakRepository.update(fagsakRepository.findByIdOrThrow(fagsak.id).copy(fagsystem = Fagsystem.EF))

        val kravgrunnlagxml = readXml("/kravgrunnlagxml/kravgrunnlag_EF_med_3_perioder_med_renter_avrunding_ned.xml")
        val kravgrunnlag = KravgrunnlagMapper.tilKravgrunnlag431(
            KravgrunnlagUtil.unmarshalKravgrunnlag(kravgrunnlagxml),
            behandling.id,
        )
        kravgrunnlagRepository.insert(kravgrunnlag)

        val sortedPerioder = kravgrunnlag.perioder.map { it.periode }.sortedBy { it.fom }

        val grovUaktsomhetPeriode = lagGrovtUaktsomVilkårsvurderingsperiode(sortedPerioder.first().fom, sortedPerioder.last().tom)

        vilkårsvurderingService.lagreVilkårsvurdering(
            behandling.id,
            BehandlingsstegVilkårsvurderingDto(listOf(grovUaktsomhetPeriode)),
        )

        val tilbakekrevingsperioder = vedtakBeregningService
            .beregnVedtaksperioder(behandling.id, kravgrunnlag)
            .sortedBy { it.periodeFom }
        tilbakekrevingsperioder.size shouldBe 3

        tilbakekrevingsperioder[0].periodeFom shouldBe sortedPerioder[0].fomDato
        tilbakekrevingsperioder[0].periodeTom shouldBe sortedPerioder[0].tomDato
        tilbakekrevingsperioder[0].belopRenter shouldBe BigDecimal(1861)

        tilbakekrevingsperioder[1].periodeFom shouldBe sortedPerioder[1].fomDato
        tilbakekrevingsperioder[1].periodeTom shouldBe sortedPerioder[1].tomDato
        tilbakekrevingsperioder[1].belopRenter shouldBe BigDecimal(1861)

        tilbakekrevingsperioder[2].periodeFom shouldBe sortedPerioder[2].fomDato
        tilbakekrevingsperioder[2].periodeTom shouldBe sortedPerioder[2].tomDato
        tilbakekrevingsperioder[2].belopRenter shouldBe BigDecimal(1860)

        tilbakekrevingsperioder.sumOf { it.belopRenter } shouldBe BigDecimal(5582)
    }

    @Test
    fun `beregnVedtaksperioder som beregner flere perioder i separate vilkårsperioder med 100 prosent tilbakekreving og renter skal avrunde hver renteperiode ned`() {
        kravgrunnlagRepository.deleteById(kravgrunnlag.id)
        fagsakRepository.update(fagsakRepository.findByIdOrThrow(fagsak.id).copy(fagsystem = Fagsystem.EF))

        val kravgrunnlagxml = readXml("/kravgrunnlagxml/kravgrunnlag_EF_med_3_perioder_med_renter_avrunding_ned.xml")
        val kravgrunnlag = KravgrunnlagMapper.tilKravgrunnlag431(
            KravgrunnlagUtil.unmarshalKravgrunnlag(kravgrunnlagxml),
            behandling.id,
        )
        kravgrunnlagRepository.insert(kravgrunnlag)

        val sortedPerioder = kravgrunnlag.perioder.map { it.periode }.sortedBy { it.fom }

        val grovUaktsomhetPeriode1 = lagGrovtUaktsomVilkårsvurderingsperiode(sortedPerioder[0].fom, sortedPerioder[0].tom)
        val grovUaktsomhetPeriode2 = lagGrovtUaktsomVilkårsvurderingsperiode(sortedPerioder[1].fom, sortedPerioder[1].tom)
        val grovUaktsomhetPeriode3 = lagGrovtUaktsomVilkårsvurderingsperiode(sortedPerioder[2].fom, sortedPerioder[2].tom)

        vilkårsvurderingService.lagreVilkårsvurdering(
            behandling.id,
            BehandlingsstegVilkårsvurderingDto(listOf(grovUaktsomhetPeriode1, grovUaktsomhetPeriode2, grovUaktsomhetPeriode3)),
        )

        val tilbakekrevingsperioder = vedtakBeregningService
            .beregnVedtaksperioder(behandling.id, kravgrunnlag)
            .sortedBy { it.periodeFom }
        tilbakekrevingsperioder.size shouldBe 3

        tilbakekrevingsperioder[0].periodeFom shouldBe sortedPerioder[0].fomDato
        tilbakekrevingsperioder[0].periodeTom shouldBe sortedPerioder[0].tomDato
        tilbakekrevingsperioder[0].belopRenter shouldBe BigDecimal(1861)
        tilbakekrevingsperioder[1].periodeFom shouldBe sortedPerioder[1].fomDato
        tilbakekrevingsperioder[1].periodeTom shouldBe sortedPerioder[1].tomDato
        tilbakekrevingsperioder[1].belopRenter shouldBe BigDecimal(1861)
        tilbakekrevingsperioder[2].periodeFom shouldBe sortedPerioder[2].fomDato
        tilbakekrevingsperioder[2].periodeTom shouldBe sortedPerioder[2].tomDato
        tilbakekrevingsperioder[2].belopRenter shouldBe BigDecimal(1860)
    }

    @Test
    fun `beregnVedtaksperioder skal beregne EF perioder med FORTSETT aktsomhet med full tilbakekreving med 10 prosent renter`() {
        kravgrunnlagRepository.deleteById(kravgrunnlag.id)
        fagsakRepository.update(fagsakRepository.findByIdOrThrow(fagsak.id).copy(fagsystem = Fagsystem.EF))

        val kravgrunnlagxml = readXml("/kravgrunnlagxml/kravgrunnlag_EF_med_renter_avrunding.xml")
        val kravgrunnlag = KravgrunnlagMapper.tilKravgrunnlag431(
            KravgrunnlagUtil.unmarshalKravgrunnlag(kravgrunnlagxml),
            behandling.id,
        )
        kravgrunnlagRepository.insert(kravgrunnlag)

        val sortedPerioder = kravgrunnlag.perioder.map { it.periode }.sortedBy { it.fom }

        val aktsomhetPeriode = VilkårsvurderingsperiodeDto(
            periode = Datoperiode(
                sortedPerioder[0].fom,
                sortedPerioder[6].tom,
            ),
            begrunnelse = "testverdi",
            aktsomhetDto = AktsomhetDto(
                aktsomhet = Aktsomhet.FORSETT,
                unnlates4Rettsgebyr = SkalUnnlates.TILBAKEKREVES,
                begrunnelse = "fortsett begrunnelse",
            ),
            vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
        )

        vilkårsvurderingService.lagreVilkårsvurdering(behandling.id, BehandlingsstegVilkårsvurderingDto(listOf(aktsomhetPeriode)))

        val tilbakekrevingsperioder = vedtakBeregningService
            .beregnVedtaksperioder(behandling.id, kravgrunnlag)
            .sortedBy { it.periodeFom }
        tilbakekrevingsperioder.shouldNotBeNull()
        tilbakekrevingsperioder.size shouldBe 7

        val førstePeriode = tilbakekrevingsperioder[0]
        førstePeriode.periodeFom shouldBe sortedPerioder[0].fomDato
        førstePeriode.periodeTom shouldBe sortedPerioder[0].tomDato
        førstePeriode.belopRenter shouldBe BigDecimal(21)
        var feilPostering = førstePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_EFOG" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_EFOG,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(209),
            kodeResultat = "",
        )
        var ytelsePostering = førstePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "EFOG" }
        assertBeløp(
            klassekode = Klassekode.EFOG,
            beløp = ytelsePostering,
            nyttBeløp = BigDecimal(4028),
            utbetaltBeløp = BigDecimal(4237),
            tilbakekrevesBeløp = BigDecimal(209),
            uinnkrevdBeløp = BigDecimal.ZERO,
            skattBeløp = BigDecimal(105),
            kodeResultat = "FULL_TILBAKEKREV",
        )

        val andrePeriode = tilbakekrevingsperioder[1]
        andrePeriode.periodeFom shouldBe sortedPerioder[1].fomDato
        andrePeriode.periodeTom shouldBe sortedPerioder[1].tomDato
        andrePeriode.belopRenter shouldBe BigDecimal(21)
        feilPostering = andrePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_EFOG" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_EFOG,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(208),
            kodeResultat = "",
        )
        ytelsePostering = andrePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "EFOG" }
        assertBeløp(
            klassekode = Klassekode.EFOG,
            beløp = ytelsePostering,
            nyttBeløp = BigDecimal(5070),
            utbetaltBeløp = BigDecimal(5278),
            tilbakekrevesBeløp = BigDecimal(208),
            uinnkrevdBeløp = BigDecimal.ZERO,
            skattBeløp = BigDecimal(104),
            kodeResultat = "FULL_TILBAKEKREV",
        )

        val tredjePeriode = tilbakekrevingsperioder[2]
        tredjePeriode.periodeFom shouldBe sortedPerioder[2].fomDato
        tredjePeriode.periodeTom shouldBe sortedPerioder[2].tomDato
        tredjePeriode.belopRenter shouldBe BigDecimal(438)
        feilPostering = tredjePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_EFOG" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_EFOG,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(4375),
            kodeResultat = "",
        )
        ytelsePostering = tredjePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "EFOG" }
        assertBeløp(
            klassekode = Klassekode.EFOG,
            beløp = ytelsePostering,
            nyttBeløp = BigDecimal(5070),
            utbetaltBeløp = BigDecimal(9445),
            tilbakekrevesBeløp = BigDecimal(4375),
            uinnkrevdBeløp = BigDecimal.ZERO,
            skattBeløp = BigDecimal.ZERO,
            kodeResultat = "FULL_TILBAKEKREV",
        )

        val fjerdePeriode = tilbakekrevingsperioder[3]
        fjerdePeriode.periodeFom shouldBe sortedPerioder[3].fomDato
        fjerdePeriode.periodeTom shouldBe sortedPerioder[3].tomDato
        fjerdePeriode.belopRenter shouldBe BigDecimal(437)
        feilPostering = fjerdePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_EFOG" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_EFOG,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(4375),
            kodeResultat = "",
        )
        ytelsePostering = fjerdePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "EFOG" }
        assertBeløp(
            klassekode = Klassekode.EFOG,
            beløp = ytelsePostering,
            nyttBeløp = BigDecimal(5070),
            utbetaltBeløp = BigDecimal(9445),
            tilbakekrevesBeløp = BigDecimal(4375),
            uinnkrevdBeløp = BigDecimal.ZERO,
            skattBeløp = BigDecimal(2187),
            kodeResultat = "FULL_TILBAKEKREV",
        )

        val femtePeriode = tilbakekrevingsperioder[4]
        femtePeriode.periodeFom shouldBe sortedPerioder[4].fomDato
        femtePeriode.periodeTom shouldBe sortedPerioder[4].tomDato
        femtePeriode.belopRenter shouldBe BigDecimal(437)
        feilPostering = femtePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_EFOG" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_EFOG,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(4375),
            kodeResultat = "",
        )
        ytelsePostering = femtePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "EFOG" }
        assertBeløp(
            klassekode = Klassekode.EFOG,
            beløp = ytelsePostering,
            nyttBeløp = BigDecimal(5070),
            utbetaltBeløp = BigDecimal(9445),
            tilbakekrevesBeløp = BigDecimal(4375),
            uinnkrevdBeløp = BigDecimal.ZERO,
            skattBeløp = BigDecimal(2187),
            kodeResultat = "FULL_TILBAKEKREV",
        )

        val sjettePeriode = tilbakekrevingsperioder[5]
        sjettePeriode.periodeFom shouldBe sortedPerioder[5].fomDato
        sjettePeriode.periodeTom shouldBe sortedPerioder[5].tomDato
        sjettePeriode.belopRenter shouldBe BigDecimal(375)
        feilPostering = sjettePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_EFOG" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_EFOG,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(3750),
            kodeResultat = "",
        )
        ytelsePostering = sjettePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "EFOG" }
        assertBeløp(
            klassekode = Klassekode.EFOG,
            beløp = ytelsePostering,
            nyttBeløp = BigDecimal(10695),
            utbetaltBeløp = BigDecimal(14445),
            tilbakekrevesBeløp = BigDecimal(3750),
            uinnkrevdBeløp = BigDecimal.ZERO,
            skattBeløp = BigDecimal(1875),
            kodeResultat = "FULL_TILBAKEKREV",
        )

        val sjuendePeriode = tilbakekrevingsperioder[6]
        sjuendePeriode.periodeFom shouldBe sortedPerioder[6].fomDato
        sjuendePeriode.periodeTom shouldBe sortedPerioder[6].tomDato
        sjuendePeriode.belopRenter shouldBe BigDecimal(375)
        feilPostering = sjuendePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_EFOG" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_EFOG,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(3750),
            kodeResultat = "",
        )
        ytelsePostering = sjuendePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "EFOG" }
        assertBeløp(
            klassekode = Klassekode.EFOG,
            beløp = ytelsePostering,
            nyttBeløp = BigDecimal(10695),
            utbetaltBeløp = BigDecimal(14445),
            tilbakekrevesBeløp = BigDecimal(3750),
            uinnkrevdBeløp = BigDecimal.ZERO,
            skattBeløp = BigDecimal(1875),
            kodeResultat = "FULL_TILBAKEKREV",
        )
    }

    @Test
    fun `beregnVedtaksperioder skal beregne EF perioder med 50 prosent tilbakekreving og skatt avrunding`() {
        kravgrunnlagRepository.deleteById(kravgrunnlag.id)
        fagsakRepository.update(fagsakRepository.findByIdOrThrow(fagsak.id).copy(fagsystem = Fagsystem.EF))

        val kravgrunnlagxml = readXml("/kravgrunnlagxml/kravgrunnlag_EF_med_skatt_avrunding.xml")
        val kravgrunnlag = KravgrunnlagMapper.tilKravgrunnlag431(
            KravgrunnlagUtil.unmarshalKravgrunnlag(kravgrunnlagxml),
            behandling.id,
        )
        kravgrunnlagRepository.insert(kravgrunnlag)

        val sortedPerioder = kravgrunnlag.perioder.map { it.periode }.sortedBy { it.fom }

        val aktsomhetPeriode = VilkårsvurderingsperiodeDto(
            periode = Datoperiode(
                sortedPerioder[0].fom,
                sortedPerioder[1].tom,
            ),
            begrunnelse = "testverdi",
            aktsomhetDto = AktsomhetDto(
                aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
                begrunnelse = "simpel uaktsomhet begrunnelse",
                unnlates4Rettsgebyr = SkalUnnlates.TILBAKEKREVES,
                særligeGrunnerBegrunnelse = "test",
                særligeGrunnerTilReduksjon = true,
                særligeGrunner = listOf(
                    SærligGrunnDto(
                        HELT_ELLER_DELVIS_NAVS_FEIL,
                    ),
                ),
                andelTilbakekreves = BigDecimal(50),
            ),
            vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
        )

        vilkårsvurderingService.lagreVilkårsvurdering(behandling.id, BehandlingsstegVilkårsvurderingDto(listOf(aktsomhetPeriode)))

        val tilbakekrevingsperioder = vedtakBeregningService
            .beregnVedtaksperioder(behandling.id, kravgrunnlag)
            .sortedBy { it.periodeFom }
        tilbakekrevingsperioder.shouldNotBeNull()
        tilbakekrevingsperioder.size shouldBe 2

        val førstePeriode = tilbakekrevingsperioder[0]
        førstePeriode.periodeFom shouldBe sortedPerioder[0].fomDato
        førstePeriode.periodeTom shouldBe sortedPerioder[0].tomDato
        førstePeriode.belopRenter shouldBe BigDecimal(0)
        var feilPostering = førstePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_EFOG" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_EFOG,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(1755),
            kodeResultat = "",
        )
        var ytelsePostering = førstePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "EFOG" }
        assertBeløp(
            klassekode = Klassekode.EFOG,
            beløp = ytelsePostering,
            nyttBeløp = BigDecimal(18195),
            utbetaltBeløp = BigDecimal(19950),
            tilbakekrevesBeløp = BigDecimal(878),
            uinnkrevdBeløp = BigDecimal(877),
            skattBeløp = BigDecimal(386),
            kodeResultat = "DELVIS_TILBAKEKREV",
        )

        val andrePeriode = tilbakekrevingsperioder[1]
        andrePeriode.periodeFom shouldBe sortedPerioder[1].fomDato
        andrePeriode.periodeTom shouldBe sortedPerioder[1].tomDato
        andrePeriode.belopRenter shouldBe BigDecimal(0)
        feilPostering = andrePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_EFOG" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_EFOG,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(1755),
            kodeResultat = "",
        )
        ytelsePostering = andrePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "EFOG" }
        assertBeløp(
            klassekode = Klassekode.EFOG,
            beløp = ytelsePostering,
            nyttBeløp = BigDecimal(18195),
            utbetaltBeløp = BigDecimal(19950),
            tilbakekrevesBeløp = BigDecimal(877),
            uinnkrevdBeløp = BigDecimal(878),
            skattBeløp = BigDecimal(439),
            kodeResultat = "DELVIS_TILBAKEKREV",
        )
    }

    @Test
    fun `beregnVedtaksperioder skal beregne EF perioder med delvis tilbakekreving og skatt avrunding`() {
        kravgrunnlagRepository.deleteById(kravgrunnlag.id)
        fagsakRepository.update(fagsakRepository.findByIdOrThrow(fagsak.id).copy(fagsystem = Fagsystem.EF))

        val kravgrunnlagxml = readXml("/kravgrunnlagxml/kravgrunnlag_EF_med_skatt_avrunding_3.xml")
        val kravgrunnlag = KravgrunnlagMapper.tilKravgrunnlag431(
            KravgrunnlagUtil.unmarshalKravgrunnlag(kravgrunnlagxml),
            behandling.id,
        )
        kravgrunnlagRepository.insert(kravgrunnlag)

        val sortedPerioder = kravgrunnlag.perioder.map { it.periode }.sortedBy { it.fom }

        val aktsomhetPeriode = VilkårsvurderingsperiodeDto(
            periode = Datoperiode(
                sortedPerioder[0].fom,
                sortedPerioder[2].tom,
            ),
            begrunnelse = "testverdi",
            aktsomhetDto = AktsomhetDto(
                aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
                begrunnelse = "simpel uaktsomhet begrunnelse",
                unnlates4Rettsgebyr = SkalUnnlates.TILBAKEKREVES,
                særligeGrunnerBegrunnelse = "test",
                særligeGrunnerTilReduksjon = false,
                særligeGrunner = listOf(SærligGrunnDto(GRAD_AV_UAKTSOMHET), SærligGrunnDto(HELT_ELLER_DELVIS_NAVS_FEIL)),
                andelTilbakekreves = BigDecimal(100),
            ),
            vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
        )

        val aktsomhetPeriode1 = VilkårsvurderingsperiodeDto(
            periode = Datoperiode(
                sortedPerioder[3].fom,
                sortedPerioder[3].tom,
            ),
            begrunnelse = "testverdi",
            aktsomhetDto = AktsomhetDto(
                aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
                begrunnelse = "simpel uaktsomhet begrunnelse",
                unnlates4Rettsgebyr = SkalUnnlates.TILBAKEKREVES,
                særligeGrunnerBegrunnelse = "test",
                særligeGrunnerTilReduksjon = true,
                særligeGrunner = listOf(SærligGrunnDto(HELT_ELLER_DELVIS_NAVS_FEIL)),
                andelTilbakekreves = BigDecimal(68),
            ),
            vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
        )

        vilkårsvurderingService.lagreVilkårsvurdering(
            behandling.id,
            BehandlingsstegVilkårsvurderingDto(listOf(aktsomhetPeriode, aktsomhetPeriode1)),
        )

        val tilbakekrevingsperioder = vedtakBeregningService
            .beregnVedtaksperioder(behandling.id, kravgrunnlag)
            .sortedBy { it.periodeFom }
        tilbakekrevingsperioder.shouldNotBeNull()
        tilbakekrevingsperioder.size shouldBe 4

        val førstePeriode = tilbakekrevingsperioder[0]
        førstePeriode.periodeFom shouldBe sortedPerioder[0].fomDato
        førstePeriode.periodeTom shouldBe sortedPerioder[0].tomDato
        førstePeriode.belopRenter shouldBe BigDecimal(0)
        var feilPostering = førstePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_EFOG" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_EFOG,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(637),
            kodeResultat = "",
        )
        var ytelsePostering = førstePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "EFOG" }
        assertBeløp(
            klassekode = Klassekode.EFOG,
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(8782),
            tilbakekrevesBeløp = BigDecimal(637),
            uinnkrevdBeløp = BigDecimal.ZERO,
            skattBeløp = BigDecimal(217),
            kodeResultat = "FULL_TILBAKEKREV",
        )

        val andrePeriode = tilbakekrevingsperioder[1]
        andrePeriode.periodeFom shouldBe sortedPerioder[1].fomDato
        andrePeriode.periodeTom shouldBe sortedPerioder[1].tomDato
        andrePeriode.belopRenter shouldBe BigDecimal(0)
        feilPostering = andrePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_EFOG" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_EFOG,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(1087),
            kodeResultat = "",
        )
        ytelsePostering = andrePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "EFOG" }
        assertBeløp(
            klassekode = Klassekode.EFOG,
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(8782),
            tilbakekrevesBeløp = BigDecimal(1087),
            uinnkrevdBeløp = BigDecimal.ZERO,
            skattBeløp = BigDecimal(369),
            kodeResultat = "FULL_TILBAKEKREV",
        )

        val tredjePeriode = tilbakekrevingsperioder[2]
        tredjePeriode.periodeFom shouldBe sortedPerioder[2].fomDato
        tredjePeriode.periodeTom shouldBe sortedPerioder[2].tomDato
        tredjePeriode.belopRenter shouldBe BigDecimal(0)
        feilPostering = tredjePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_EFOG" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_EFOG,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(2250),
            kodeResultat = "",
        )
        ytelsePostering = tredjePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "EFOG" }
        assertBeløp(
            klassekode = Klassekode.EFOG,
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(8782),
            tilbakekrevesBeløp = BigDecimal(2250),
            uinnkrevdBeløp = BigDecimal.ZERO,
            skattBeløp = BigDecimal(382),
            kodeResultat = "FULL_TILBAKEKREV",
        )

        val fjerdePeriode = tilbakekrevingsperioder[3]
        fjerdePeriode.periodeFom shouldBe sortedPerioder[3].fomDato
        fjerdePeriode.periodeTom shouldBe sortedPerioder[3].tomDato
        fjerdePeriode.belopRenter shouldBe BigDecimal(0)
        feilPostering = fjerdePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_EFOG" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_EFOG,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(8782),
            kodeResultat = "",
        )
        ytelsePostering = fjerdePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "EFOG" }
        assertBeløp(
            klassekode = Klassekode.EFOG,
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(8782),
            tilbakekrevesBeløp = BigDecimal(5972),
            uinnkrevdBeløp = BigDecimal(2810),
            skattBeløp = BigDecimal(2030),
            kodeResultat = "DELVIS_TILBAKEKREV",
        )
    }

    @Test
    fun `beregnVedtaksperioder skal beregne EF perioder med full tilbakekreving og skatt avrunding`() {
        kravgrunnlagRepository.deleteById(kravgrunnlag.id)
        fagsakRepository.update(fagsakRepository.findByIdOrThrow(fagsak.id).copy(fagsystem = Fagsystem.EF))

        val kravgrunnlagxml = readXml("/kravgrunnlagxml/kravgrunnlag_EF_med_skatt_avrunding_2.xml")
        val kravgrunnlag = KravgrunnlagMapper.tilKravgrunnlag431(
            KravgrunnlagUtil.unmarshalKravgrunnlag(kravgrunnlagxml),
            behandling.id,
        )
        kravgrunnlagRepository.insert(kravgrunnlag)

        val sortedPerioder = kravgrunnlag.perioder.map { it.periode }.sortedBy { it.fom }

        val aktsomhetPeriode = VilkårsvurderingsperiodeDto(
            periode = Datoperiode(
                sortedPerioder[0].fom,
                sortedPerioder[3].tom,
            ),
            begrunnelse = "testverdi",
            aktsomhetDto = AktsomhetDto(
                aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
                begrunnelse = "simpel uaktsomhet begrunnelse",
                unnlates4Rettsgebyr = SkalUnnlates.TILBAKEKREVES,
                særligeGrunnerBegrunnelse = "test",
                særligeGrunnerTilReduksjon = true,
                særligeGrunner = listOf(
                    SærligGrunnDto(
                        HELT_ELLER_DELVIS_NAVS_FEIL,
                    ),
                ),
                andelTilbakekreves = BigDecimal(100),
            ),
            vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
        )

        vilkårsvurderingService.lagreVilkårsvurdering(behandling.id, BehandlingsstegVilkårsvurderingDto(listOf(aktsomhetPeriode)))

        val tilbakekrevingsperioder = vedtakBeregningService
            .beregnVedtaksperioder(behandling.id, kravgrunnlag)
            .sortedBy { it.periodeFom }
        tilbakekrevingsperioder.shouldNotBeNull()
        tilbakekrevingsperioder.size shouldBe 4

        val førstePeriode = tilbakekrevingsperioder[0]
        førstePeriode.periodeFom shouldBe sortedPerioder[0].fomDato
        førstePeriode.periodeTom shouldBe sortedPerioder[0].tomDato
        førstePeriode.belopRenter shouldBe BigDecimal(0)
        var feilPostering = førstePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_EFOG" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_EFOG,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(2962),
            kodeResultat = "",
        )
        var ytelsePostering = førstePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "EFOG" }
        assertBeløp(
            klassekode = Klassekode.EFOG,
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(12607),
            tilbakekrevesBeløp = BigDecimal(2962),
            uinnkrevdBeløp = BigDecimal.ZERO,
            skattBeløp = BigDecimal(430),
            kodeResultat = "FULL_TILBAKEKREV",
        )

        val andrePeriode = tilbakekrevingsperioder[1]
        andrePeriode.periodeFom shouldBe sortedPerioder[1].fomDato
        andrePeriode.periodeTom shouldBe sortedPerioder[1].tomDato
        andrePeriode.belopRenter shouldBe BigDecimal(0)
        feilPostering = andrePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_EFOG" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_EFOG,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(1725),
            kodeResultat = "",
        )
        ytelsePostering = andrePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "EFOG" }
        assertBeløp(
            klassekode = Klassekode.EFOG,
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(12607),
            tilbakekrevesBeløp = BigDecimal(1725),
            uinnkrevdBeløp = BigDecimal.ZERO,
            skattBeløp = BigDecimal(431),
            kodeResultat = "FULL_TILBAKEKREV",
        )

        val tredjePeriode = tilbakekrevingsperioder[2]
        tredjePeriode.periodeFom shouldBe sortedPerioder[2].fomDato
        tredjePeriode.periodeTom shouldBe sortedPerioder[2].tomDato
        tredjePeriode.belopRenter shouldBe BigDecimal(0)
        feilPostering = tredjePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_EFOG" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_EFOG,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(1050),
            kodeResultat = "",
        )
        ytelsePostering = tredjePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "EFOG" }
        assertBeløp(
            klassekode = Klassekode.EFOG,
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(12607),
            tilbakekrevesBeløp = BigDecimal(1050),
            uinnkrevdBeløp = BigDecimal.ZERO,
            skattBeløp = BigDecimal(262),
            kodeResultat = "FULL_TILBAKEKREV",
        )

        val fjerdePeriode = tilbakekrevingsperioder[3]
        fjerdePeriode.periodeFom shouldBe sortedPerioder[3].fomDato
        fjerdePeriode.periodeTom shouldBe sortedPerioder[3].tomDato
        fjerdePeriode.belopRenter shouldBe BigDecimal(0)
        feilPostering = fjerdePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_EFOG" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_EFOG,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(150),
            kodeResultat = "",
        )
        ytelsePostering = fjerdePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "EFOG" }
        assertBeløp(
            klassekode = Klassekode.EFOG,
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(12607),
            tilbakekrevesBeløp = BigDecimal(150),
            uinnkrevdBeløp = BigDecimal.ZERO,
            skattBeløp = BigDecimal(38),
            kodeResultat = "FULL_TILBAKEKREV",
        )
    }

    @Test
    fun `barnetrygd og utvidet barnetrygd i samme kravgrunnlagsperiode`() {
        kravgrunnlagRepository.deleteById(kravgrunnlag.id)
        fagsakRepository.update(fagsakRepository.findByIdOrThrow(fagsak.id).copy(fagsystem = Fagsystem.EF))

        val kravgrunnlagxml = readXml("/kravgrunnlagxml/BA_utvidet_barnetrygd.xml")
        val kravgrunnlag = KravgrunnlagMapper.tilKravgrunnlag431(
            KravgrunnlagUtil.unmarshalKravgrunnlag(kravgrunnlagxml),
            behandling.id,
        )
        kravgrunnlagRepository.insert(kravgrunnlag)

        val sortedPerioder = kravgrunnlag.perioder.map { it.periode }.sortedBy { it.fom }

        val aktsomhetPeriode = VilkårsvurderingsperiodeDto(
            periode = 1.januar() til 31.januar(),
            begrunnelse = "testverdi",
            aktsomhetDto = AktsomhetDto(
                aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
                begrunnelse = "simpel uaktsomhet begrunnelse",
                unnlates4Rettsgebyr = SkalUnnlates.TILBAKEKREVES,
                særligeGrunnerBegrunnelse = "test",
                særligeGrunnerTilReduksjon = true,
                særligeGrunner = listOf(SærligGrunnDto(HELT_ELLER_DELVIS_NAVS_FEIL)),
                andelTilbakekreves = BigDecimal(50),
            ),
            vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
        )

        vilkårsvurderingService.lagreVilkårsvurdering(behandling.id, BehandlingsstegVilkårsvurderingDto(listOf(aktsomhetPeriode)))

        val tilbakekrevingsperioder = vedtakBeregningService
            .beregnVedtaksperioder(behandling.id, kravgrunnlag)
            .sortedBy { it.periodeFom }
        tilbakekrevingsperioder.shouldNotBeNull()
        tilbakekrevingsperioder.size shouldBe 1

        val førstePeriode = tilbakekrevingsperioder[0]
        førstePeriode.periodeFom shouldBe sortedPerioder[0].fomDato
        førstePeriode.periodeTom shouldBe sortedPerioder[0].tomDato
        førstePeriode.belopRenter shouldBe BigDecimal(0)
        val feilPostering = førstePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "KL_KODE_FEIL_BA" }
        assertBeløp(
            klassekode = Klassekode.KL_KODE_FEIL_BA,
            beløp = feilPostering,
            nyttBeløp = BigDecimal(2500),
            kodeResultat = "",
        )
        val batrPostering = førstePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "BATR" }
        val bautvOpPostering = førstePeriode.posteringer.shouldGiveOne { it.kodeKlasse shouldBe "BAUTV-OP" }
        assertBeløp(
            klassekode = Klassekode.BATR,
            beløp = batrPostering,
            utbetaltBeløp = BigDecimal(1000),
            tilbakekrevesBeløp = BigDecimal(500),
            uinnkrevdBeløp = BigDecimal(500),
            skattBeløp = BigDecimal(0),
            kodeResultat = "DELVIS_TILBAKEKREV",
        )
        assertBeløp(
            klassekode = Klassekode.BAUTV_OP,
            beløp = bautvOpPostering,
            utbetaltBeløp = BigDecimal(1500),
            tilbakekrevesBeløp = BigDecimal(750),
            uinnkrevdBeløp = BigDecimal(750),
            skattBeløp = BigDecimal(0),
            kodeResultat = "DELVIS_TILBAKEKREV",
        )
    }

    private fun lagForeldelse(perioder: List<Månedsperiode>) {
        val foreldelsesdata = BehandlingsstegForeldelseDto(
            perioder.map {
                ForeldelsesperiodeDto(
                    periode = it.toDatoperiode(),
                    begrunnelse = "testverdi",
                    foreldelsesvurderingstype = Foreldelsesvurderingstype.FORELDET,
                )
            },
        )
        foreldelsesService.lagreVurdertForeldelse(behandling.id, foreldelsesdata, SecureLog.Context.tom())
    }

    private fun lagAktsomhetVilkårsvurdering(
        perioder: List<Månedsperiode>,
        aktsomhet: Aktsomhet,
        andelTilbakreves: BigDecimal? = null,
        beløpTilbakekreves: BigDecimal? = null,
        særligeGrunnerTilReduksjon: Boolean = false,
    ) {
        val vilkårsperioder = perioder.map {
            VilkårsvurderingsperiodeDto(
                periode = it.toDatoperiode(),
                begrunnelse = "testverdi",
                aktsomhetDto = AktsomhetDto(
                    aktsomhet = aktsomhet,
                    andelTilbakekreves = andelTilbakreves,
                    beløpTilbakekreves = beløpTilbakekreves,
                    begrunnelse = "testverdi",
                    særligeGrunnerTilReduksjon = særligeGrunnerTilReduksjon,
                    unnlates4Rettsgebyr = SkalUnnlates.TILBAKEKREVES,
                    særligeGrunnerBegrunnelse = "testverdi",
                    særligeGrunner = listOf(
                        SærligGrunnDto(
                            ANNET,
                            "testverdi",
                        ),
                    ),
                ),
                vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
            )
        }
        vilkårsvurderingService.lagreVilkårsvurdering(behandling.id, BehandlingsstegVilkårsvurderingDto(vilkårsperioder))
    }

    private fun lagGodTroVilkårsvurdering(
        perioder: List<Månedsperiode>,
        beløpErIBehold: Boolean = false,
        beløpTilbakekreves: BigDecimal? = null,
    ) {
        val vilkårsperioder = perioder.map {
            VilkårsvurderingsperiodeDto(
                periode = it.toDatoperiode(),
                begrunnelse = "testverdi",
                godTroDto = GodTroDto(
                    begrunnelse = "testverdi",
                    beløpErIBehold = beløpErIBehold,
                    beløpTilbakekreves = beløpTilbakekreves,
                ),
                vilkårsvurderingsresultat = Vilkårsvurderingsresultat.GOD_TRO,
            )
        }
        vilkårsvurderingService.lagreVilkårsvurdering(behandling.id, BehandlingsstegVilkårsvurderingDto(vilkårsperioder))
    }

    private fun lagKravgrunnlag(
        perioder: List<Månedsperiode>,
        månedligSkattBeløp: BigDecimal,
        kravgrunnlagsbeløpene: List<Kravgrunnlagsbeløp>,
    ): Kravgrunnlag431 = Kravgrunnlag431(
        behandlingId = behandling.id,
        vedtakId = BigInteger.ZERO,
        kravstatuskode = Kravstatuskode.NYTT,
        fagområdekode = Fagområdekode.BA,
        fagsystemId = fagsak.eksternFagsakId,
        gjelderVedtakId = "testverdi",
        gjelderType = GjelderType.PERSON,
        utbetalesTilId = "testverdi",
        utbetIdType = GjelderType.PERSON,
        ansvarligEnhet = "testverdi",
        bostedsenhet = "testverdi",
        behandlingsenhet = "testverdi",
        kontrollfelt = "2025-12-24-11.12.13.123456",
        referanse = behandling.aktivFagsystemsbehandling.eksternId,
        eksternKravgrunnlagId = BigInteger.ZERO,
        saksbehandlerId = "testverdi",
        perioder = lagKravgrunnlagsperiode(perioder, månedligSkattBeløp, kravgrunnlagsbeløpene),
    )

    private fun lagKravgrunnlagsperiode(
        perioder: List<Månedsperiode>,
        månedligSkattBeløp: BigDecimal,
        kravgrunnlagsbeløpene: List<Kravgrunnlagsbeløp>,
    ): Set<Kravgrunnlagsperiode432> = perioder.map {
        Kravgrunnlagsperiode432(
            periode = it,
            månedligSkattebeløp = månedligSkattBeløp,
            beløp = lagKravgrunnlagsbeløp(kravgrunnlagsbeløpene),
        )
    }.toSet()

    private fun lagKravgrunnlagsbeløp(
        kravgrunnlagsbeløpene: List<Kravgrunnlagsbeløp>,
    ): Set<Kravgrunnlagsbeløp433> = kravgrunnlagsbeløpene.map {
        Kravgrunnlagsbeløp433(
            klassekode = when (it.klassetype) {
                Klassetype.FEIL -> Klassekode.KL_KODE_FEIL_BA
                Klassetype.YTEL -> Klassekode.BATR
                else -> error("Not implemented.")
            },
            klassetype = it.klassetype,
            opprinneligUtbetalingsbeløp = it.opprinneligUtbetalingsbeløp,
            nyttBeløp = it.nyttBeløp,
            tilbakekrevesBeløp = it.tilbakekrevesBeløp,
            uinnkrevdBeløp = it.uinnkrevdBeløp,
            skatteprosent = it.skatteprosent,
        )
    }.toSet()

    private fun assertBeløp(
        klassekode: Klassekode,
        beløp: PosteringDto,
        nyttBeløp: BigDecimal = BigDecimal.ZERO,
        utbetaltBeløp: BigDecimal = BigDecimal.ZERO,
        tilbakekrevesBeløp: BigDecimal = BigDecimal.ZERO,
        uinnkrevdBeløp: BigDecimal = BigDecimal.ZERO,
        skattBeløp: BigDecimal = BigDecimal.ZERO,
        kodeResultat: String,
    ) {
        beløp.kodeKlasse shouldBe klassekode.tilKlassekodeNavn()
        beløp.belopNy shouldBe nyttBeløp
        beløp.belopOpprinneligUtbetalt shouldBe utbetaltBeløp
        beløp.belopTilbakekreves shouldBe tilbakekrevesBeløp
        beløp.belopUinnkrevd shouldBe uinnkrevdBeløp
        beløp.belopSkatt shouldBe skattBeløp
        beløp.kodeResultat shouldBe kodeResultat
    }

    internal data class Kravgrunnlagsbeløp(
        val klassetype: Klassetype,
        val opprinneligUtbetalingsbeløp: BigDecimal = BigDecimal.ZERO,
        val nyttBeløp: BigDecimal = BigDecimal.ZERO,
        val tilbakekrevesBeløp: BigDecimal = BigDecimal.ZERO,
        val uinnkrevdBeløp: BigDecimal = BigDecimal.ZERO,
        val skatteprosent: BigDecimal = BigDecimal.ZERO,
    )
}
