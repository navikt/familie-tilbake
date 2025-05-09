package no.nav.familie.tilbake.iverksettvedtak

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.inspectors.forExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.foreldelse.ForeldelseService
import no.nav.familie.tilbake.iverksettvedtak.VilkårsvurderingsPeriodeDomainUtil.lagGrovtUaktsomVilkårsvurderingsperiode
import no.nav.familie.tilbake.iverksettvedtak.domain.KodeResultat
import no.nav.familie.tilbake.iverksettvedtak.domain.Tilbakekrevingsbeløp
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
import no.nav.tilbakekreving.api.v1.dto.SærligGrunnDto
import no.nav.tilbakekreving.api.v1.dto.VilkårsvurderingsperiodeDto
import no.nav.tilbakekreving.kontrakter.foreldelse.Foreldelsesvurderingstype
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.periode.Månedsperiode
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunn.ANNET
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunn.GRAD_AV_UAKTSOMHET
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunn.HELT_ELLER_DELVIS_NAVS_FEIL
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import no.nav.tilbakekreving.kontrakter.ytelse.Fagsystem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.math.BigInteger
import java.time.YearMonth
import no.nav.tilbakekreving.februar
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.mars

internal class TilbakekrevingsvedtakBeregningServiceTest : OppslagSpringRunnerTest() {
    override val tømDBEtterHverTest = false

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

    private val perioder =
        listOf(
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
        val kravgrunnlagsbeløpene =
            listOf(
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
            perioder =
                listOf(
                    Månedsperiode(
                        YearMonth.of(2021, 1),
                        YearMonth.of(2021, 2),
                    ),
                ),
            aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
            andelTilbakreves = BigDecimal(50),
            særligeGrunnerTilReduksjon = true,
        )

        val tilbakekrevingsperioder =
            vedtakBeregningService
                .beregnVedtaksperioder(behandling.id, kravgrunnlag)
                .sortedBy { it.periode.fom }
        tilbakekrevingsperioder.shouldNotBeNull()
        tilbakekrevingsperioder.size shouldBe 2
        shouldNotThrowAny { iverksettelseService.validerBeløp(behandling.id, tilbakekrevingsperioder, SecureLog.Context.tom()) }

        val førstePeriode = tilbakekrevingsperioder[0]
        førstePeriode.periode shouldBe Månedsperiode(YearMonth.of(2021, 1), YearMonth.of(2021, 1))
        førstePeriode.renter shouldBe BigDecimal.ZERO
        var feilPostering = førstePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING)
        var ytelsePostering = førstePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(2500),
            uinnkrevdBeløp = BigDecimal(2500),
            kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING,
        )

        val andrePeriode = tilbakekrevingsperioder[1]
        andrePeriode.periode shouldBe Månedsperiode(YearMonth.of(2021, 2), YearMonth.of(2021, 2))
        andrePeriode.renter shouldBe BigDecimal.ZERO
        feilPostering = andrePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING)
        ytelsePostering = andrePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(2500),
            uinnkrevdBeløp = BigDecimal(2500),
            kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING,
        )
    }

    @Test
    fun `beregnVedtaksperioder skal beregne når vilkårsvurderte med 33 prosent andel tilbakekrevesbeløp`() {
        lagAktsomhetVilkårsvurdering(
            perioder =
                listOf(
                    Månedsperiode(
                        YearMonth.of(2021, 1),
                        YearMonth.of(2021, 2),
                    ),
                ),
            aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
            andelTilbakreves = BigDecimal(33),
            særligeGrunnerTilReduksjon = true,
        )

        val tilbakekrevingsperioder =
            vedtakBeregningService
                .beregnVedtaksperioder(behandling.id, kravgrunnlag)
                .sortedBy { it.periode.fom }
        tilbakekrevingsperioder.shouldNotBeNull()
        tilbakekrevingsperioder.size shouldBe 2
        shouldNotThrowAny { iverksettelseService.validerBeløp(behandling.id, tilbakekrevingsperioder, SecureLog.Context.tom()) }

        val førstePeriode = tilbakekrevingsperioder[0]
        førstePeriode.periode shouldBe Månedsperiode(YearMonth.of(2021, 1), YearMonth.of(2021, 1))
        førstePeriode.renter shouldBe BigDecimal.ZERO
        var feilPostering = førstePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING)
        var ytelsePostering = førstePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(1650),
            uinnkrevdBeløp = BigDecimal(3350),
            kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING,
        )

        val andrePeriode = tilbakekrevingsperioder[1]
        andrePeriode.periode shouldBe Månedsperiode(YearMonth.of(2021, 2), YearMonth.of(2021, 2))
        andrePeriode.renter shouldBe BigDecimal.ZERO
        feilPostering = andrePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING)
        ytelsePostering = andrePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(1650),
            uinnkrevdBeløp = BigDecimal(3350),
            kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING,
        )
    }

    @Test
    fun `beregnVedtaksperioder skal beregne når vilkårsvurderte med Forsett aktsomhet`() {
        lagAktsomhetVilkårsvurdering(
            perioder =
                listOf(
                    Månedsperiode(
                        YearMonth.of(2021, 1),
                        YearMonth.of(2021, 2),
                    ),
                ),
            aktsomhet = Aktsomhet.FORSETT,
        )

        val tilbakekrevingsperioder =
            vedtakBeregningService
                .beregnVedtaksperioder(behandling.id, kravgrunnlag)
                .sortedBy { it.periode.fom }
        tilbakekrevingsperioder.shouldNotBeNull()
        tilbakekrevingsperioder.size shouldBe 2
        shouldNotThrowAny { iverksettelseService.validerBeløp(behandling.id, tilbakekrevingsperioder, SecureLog.Context.tom()) }

        val førstePeriode = tilbakekrevingsperioder[0]
        førstePeriode.periode shouldBe Månedsperiode(YearMonth.of(2021, 1), YearMonth.of(2021, 1))
        førstePeriode.renter shouldBe BigDecimal.ZERO
        var feilPostering = førstePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.FULL_TILBAKEKREVING)
        var ytelsePostering = førstePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(5000),
            uinnkrevdBeløp = BigDecimal.ZERO,
            kodeResultat = KodeResultat.FULL_TILBAKEKREVING,
        )

        val andrePeriode = tilbakekrevingsperioder[1]
        andrePeriode.periode shouldBe Månedsperiode(YearMonth.of(2021, 2), YearMonth.of(2021, 2))
        andrePeriode.renter shouldBe BigDecimal.ZERO
        feilPostering = andrePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.FULL_TILBAKEKREVING)
        ytelsePostering = andrePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(5000),
            uinnkrevdBeløp = BigDecimal.ZERO,
            kodeResultat = KodeResultat.FULL_TILBAKEKREVING,
        )
    }

    @Test
    fun `beregnVedtaksperioder skal beregne når vilkårsvurderte med God tro og ingen tilbakekreving`() {
        lagGodTroVilkårsvurdering(
            perioder =
                listOf(
                    Månedsperiode(
                        YearMonth.of(2021, 1),
                        YearMonth.of(2021, 2),
                    ),
                ),
        )

        val tilbakekrevingsperioder =
            vedtakBeregningService
                .beregnVedtaksperioder(behandling.id, kravgrunnlag)
                .sortedBy { it.periode.fom }
        tilbakekrevingsperioder.shouldNotBeNull()
        tilbakekrevingsperioder.size shouldBe 2
        shouldNotThrowAny { iverksettelseService.validerBeløp(behandling.id, tilbakekrevingsperioder, SecureLog.Context.tom()) }

        val førstePeriode = tilbakekrevingsperioder[0]
        førstePeriode.periode shouldBe Månedsperiode(YearMonth.of(2021, 1), YearMonth.of(2021, 1))
        førstePeriode.renter shouldBe BigDecimal.ZERO
        var feilPostering = førstePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.INGEN_TILBAKEKREVING)
        var ytelsePostering = førstePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal.ZERO,
            uinnkrevdBeløp = BigDecimal(5000),
            kodeResultat = KodeResultat.INGEN_TILBAKEKREVING,
        )

        val andrePeriode = tilbakekrevingsperioder[1]
        andrePeriode.periode shouldBe Månedsperiode(YearMonth.of(2021, 2), YearMonth.of(2021, 2))
        andrePeriode.renter shouldBe BigDecimal.ZERO
        feilPostering = andrePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.INGEN_TILBAKEKREVING)
        ytelsePostering = andrePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal.ZERO,
            uinnkrevdBeløp = BigDecimal(5000),
            kodeResultat = KodeResultat.INGEN_TILBAKEKREVING,
        )
    }

    @Test
    fun `beregnVedtaksperioder skal beregne når vilkårsvurderte med God tro og bestemt tilbakekrevesbeløp`() {
        lagGodTroVilkårsvurdering(
            perioder =
                listOf(
                    Månedsperiode(
                        YearMonth.of(2021, 1),
                        YearMonth.of(2021, 2),
                    ),
                ),
            beløpErIBehold = true,
            beløpTilbakekreves = BigDecimal(3000),
        )

        val tilbakekrevingsperioder =
            vedtakBeregningService
                .beregnVedtaksperioder(behandling.id, kravgrunnlag)
                .sortedBy { it.periode.fom }
        tilbakekrevingsperioder.shouldNotBeNull()
        tilbakekrevingsperioder.size shouldBe 2
        shouldNotThrowAny { iverksettelseService.validerBeløp(behandling.id, tilbakekrevingsperioder, SecureLog.Context.tom()) }

        val førstePeriode = tilbakekrevingsperioder[0]
        førstePeriode.periode shouldBe Månedsperiode(YearMonth.of(2021, 1), YearMonth.of(2021, 1))
        førstePeriode.renter shouldBe BigDecimal.ZERO
        var feilPostering = førstePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING)
        var ytelsePostering = førstePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(1500),
            uinnkrevdBeløp = BigDecimal(3500),
            kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING,
        )

        val andrePeriode = tilbakekrevingsperioder[1]
        andrePeriode.periode shouldBe Månedsperiode(YearMonth.of(2021, 2), YearMonth.of(2021, 2))
        andrePeriode.renter shouldBe BigDecimal.ZERO
        feilPostering = andrePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING)
        ytelsePostering = andrePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(1500),
            uinnkrevdBeløp = BigDecimal(3500),
            kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING,
        )
    }

    @Test
    fun `beregnVedtaksperioder skal beregne når vilkårsvurderte med God tro og bestemt tilbakekrevesbeløp med avrunding`() {
        lagGodTroVilkårsvurdering(
            perioder =
                listOf(
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
            .sortedBy { it.periode.fom }
        tilbakekrevingsperioder.shouldNotBeNull()
        tilbakekrevingsperioder.size shouldBe 2
        shouldNotThrowAny { iverksettelseService.validerBeløp(behandling.id, tilbakekrevingsperioder, SecureLog.Context.tom()) }

        val førstePeriode = tilbakekrevingsperioder[0]
        førstePeriode.periode shouldBe Månedsperiode(YearMonth.of(2021, 1), YearMonth.of(2021, 1))
        førstePeriode.renter shouldBe BigDecimal.ZERO
        var feilPostering = førstePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING)
        var ytelsePostering = førstePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(1000),
            uinnkrevdBeløp = BigDecimal(4000),
            kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING,
        )

        val andrePeriode = tilbakekrevingsperioder[1]
        andrePeriode.periode shouldBe Månedsperiode(YearMonth.of(2021, 2), YearMonth.of(2021, 2))
        andrePeriode.renter shouldBe BigDecimal.ZERO
        feilPostering = andrePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING)
        ytelsePostering = andrePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(999),
            uinnkrevdBeløp = BigDecimal(4001),
            kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING,
        )
    }

    @Test
    fun `beregnVedtaksperioder skal beregne når vilkårsvurderte med 50 prosent andeltilbakekrevesbeløp med skatt beløp`() {
        val månedligSkattBeløp = BigDecimal(500)

        kravgrunnlagRepository.deleteById(kravgrunnlag.id)
        val kravgrunnlagsbeløpene =
            listOf(
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
            perioder =
                listOf(
                    Månedsperiode(
                        YearMonth.of(2021, 1),
                        YearMonth.of(2021, 2),
                    ),
                ),
            aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
            andelTilbakreves = BigDecimal(50),
            særligeGrunnerTilReduksjon = true,
        )

        val tilbakekrevingsperioder =
            vedtakBeregningService
                .beregnVedtaksperioder(behandling.id, kravgrunnlag)
                .sortedBy { it.periode.fom }
        tilbakekrevingsperioder.shouldNotBeNull()
        tilbakekrevingsperioder.size shouldBe 2
        shouldNotThrowAny { iverksettelseService.validerBeløp(behandling.id, tilbakekrevingsperioder, SecureLog.Context.tom()) }

        val førstePeriode = tilbakekrevingsperioder[0]
        førstePeriode.periode shouldBe Månedsperiode(YearMonth.of(2021, 1), YearMonth.of(2021, 1))
        førstePeriode.renter shouldBe BigDecimal.ZERO
        var feilPostering = førstePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING)
        var ytelsePostering = førstePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(2500),
            uinnkrevdBeløp = BigDecimal(2500),
            skattBeløp = BigDecimal(250),
            kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING,
        )

        val andrePeriode = tilbakekrevingsperioder[1]
        andrePeriode.periode shouldBe Månedsperiode(YearMonth.of(2021, 2), YearMonth.of(2021, 2))
        andrePeriode.renter shouldBe BigDecimal.ZERO
        feilPostering = andrePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING)
        ytelsePostering = andrePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(2500),
            uinnkrevdBeløp = BigDecimal(2500),
            skattBeløp = BigDecimal(250),
            kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING,
        )
    }

    @Test
    fun `beregnVedtaksperioder skal beregne med en foreldet periode,en vilkårsvurdert periode med 100 prosent tilbakekreving`() {
        lagForeldelse(listOf(perioder[0]))

        lagAktsomhetVilkårsvurdering(listOf(perioder[1]), Aktsomhet.GROV_UAKTSOMHET)

        val tilbakekrevingsperioder =
            vedtakBeregningService
                .beregnVedtaksperioder(behandling.id, kravgrunnlag)
                .sortedBy { it.periode.fom }
        tilbakekrevingsperioder.shouldNotBeNull()
        tilbakekrevingsperioder.size shouldBe 2
        shouldNotThrowAny { iverksettelseService.validerBeløp(behandling.id, tilbakekrevingsperioder, SecureLog.Context.tom()) }

        val førstePeriode = tilbakekrevingsperioder[0]
        førstePeriode.periode shouldBe Månedsperiode(YearMonth.of(2021, 1), YearMonth.of(2021, 1))
        førstePeriode.renter shouldBe BigDecimal.ZERO
        var feilPostering = førstePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.FORELDET)
        var ytelsePostering = førstePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(0),
            uinnkrevdBeløp = BigDecimal(5000),
            skattBeløp = BigDecimal(0),
            kodeResultat = KodeResultat.FORELDET,
        )

        val andrePeriode = tilbakekrevingsperioder[1]
        andrePeriode.periode shouldBe Månedsperiode(YearMonth.of(2021, 2), YearMonth.of(2021, 2))
        andrePeriode.renter shouldBe BigDecimal.ZERO
        feilPostering = andrePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.FULL_TILBAKEKREVING)
        ytelsePostering = andrePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(5000),
            uinnkrevdBeløp = BigDecimal(0),
            skattBeløp = BigDecimal(0),
            kodeResultat = KodeResultat.FULL_TILBAKEKREVING,
        )
    }

    @Test
    fun `beregnVedtaksperioder skal beregne med tre vilkårsvurdert periode med 100 prosent tilbakekreving`() {
        kravgrunnlagRepository.deleteById(kravgrunnlag.id)

        val månedligSkattBeløp = BigDecimal(750)
        val perioder =
            listOf(
                Månedsperiode(YearMonth.of(2021, 1), YearMonth.of(2021, 1)),
                Månedsperiode(YearMonth.of(2021, 2), YearMonth.of(2021, 2)),
                Månedsperiode(YearMonth.of(2021, 3), YearMonth.of(2021, 3)),
            )

        val kravgrunnlagsbeløpene =
            listOf(
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

        val tilbakekrevingsperioder =
            vedtakBeregningService
                .beregnVedtaksperioder(behandling.id, kravgrunnlag)
                .sortedBy { it.periode.fom }
        tilbakekrevingsperioder.shouldNotBeNull()
        tilbakekrevingsperioder.size shouldBe 3
        shouldNotThrowAny { iverksettelseService.validerBeløp(behandling.id, tilbakekrevingsperioder, SecureLog.Context.tom()) }

        val førstePeriode = tilbakekrevingsperioder[0]
        førstePeriode.periode shouldBe Månedsperiode(YearMonth.of(2021, 1), YearMonth.of(2021, 1))
        førstePeriode.renter shouldBe BigDecimal.ZERO
        var feilPostering = førstePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.FULL_TILBAKEKREVING)
        var ytelsePostering = førstePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(5000),
            uinnkrevdBeløp = BigDecimal(0),
            skattBeløp = BigDecimal(750),
            kodeResultat = KodeResultat.FULL_TILBAKEKREVING,
        )

        val andrePeriode = tilbakekrevingsperioder[1]
        andrePeriode.periode shouldBe Månedsperiode(YearMonth.of(2021, 2), YearMonth.of(2021, 2))
        andrePeriode.renter shouldBe BigDecimal.ZERO
        feilPostering = andrePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.FULL_TILBAKEKREVING)
        ytelsePostering = andrePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(5000),
            uinnkrevdBeløp = BigDecimal(0),
            skattBeløp = BigDecimal(750),
            kodeResultat = KodeResultat.FULL_TILBAKEKREVING,
        )

        val tredjePeriode = tilbakekrevingsperioder[2]
        tredjePeriode.periode shouldBe Månedsperiode(YearMonth.of(2021, 3), YearMonth.of(2021, 3))
        tredjePeriode.renter shouldBe BigDecimal.ZERO
        feilPostering = tredjePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.FULL_TILBAKEKREVING)
        ytelsePostering = tredjePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(5000),
            uinnkrevdBeløp = BigDecimal(0),
            skattBeløp = BigDecimal(750),
            kodeResultat = KodeResultat.FULL_TILBAKEKREVING,
        )
    }

    @Test
    fun `beregnVedtaksperioder skal beregne med 2 foreldet, 2 god tro og 3 periode med 100 prosent tilbakekreving`() {
        kravgrunnlagRepository.deleteById(kravgrunnlag.id)

        val perioder =
            listOf(
                Månedsperiode(YearMonth.of(2020, 1), YearMonth.of(2020, 1)),
                Månedsperiode(YearMonth.of(2020, 3), YearMonth.of(2020, 3)),
                Månedsperiode(YearMonth.of(2020, 5), YearMonth.of(2020, 5)),
                Månedsperiode(YearMonth.of(2020, 7), YearMonth.of(2020, 7)),
                Månedsperiode(YearMonth.of(2020, 9), YearMonth.of(2020, 9)),
                Månedsperiode(YearMonth.of(2020, 11), YearMonth.of(2020, 11)),
                Månedsperiode(YearMonth.of(2021, 1), YearMonth.of(2021, 1)),
            )

        val kravgrunnlagsbeløpene =
            listOf(
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
        val godtroPerioder =
            listOf(perioder[2], perioder[3]).map {
                VilkårsvurderingsperiodeDto(
                    periode = it.toDatoperiode(),
                    begrunnelse = "testverdi",
                    godTroDto = GodTroDto(begrunnelse = "testverdi", beløpErIBehold = false),
                    vilkårsvurderingsresultat = Vilkårsvurderingsresultat.GOD_TRO,
                )
            }

        // 5,6,7 beregnet periode er Forsett aktsomhet med Full tilbakekreving
        val aktsomhetPerioder =
            listOf(perioder[4], perioder[5], perioder[6]).map {
                VilkårsvurderingsperiodeDto(
                    periode = it.toDatoperiode(),
                    begrunnelse = "testverdi",
                    aktsomhetDto = AktsomhetDto(aktsomhet = Aktsomhet.FORSETT, begrunnelse = "testverdi"),
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

        val tilbakekrevingsperioder =
            vedtakBeregningService
                .beregnVedtaksperioder(behandling.id, kravgrunnlag)
                .sortedBy { it.periode.fom }
        tilbakekrevingsperioder.shouldNotBeNull()
        tilbakekrevingsperioder.size shouldBe 7
        shouldNotThrowAny { iverksettelseService.validerBeløp(behandling.id, tilbakekrevingsperioder, SecureLog.Context.tom()) }

        val førstePeriode = tilbakekrevingsperioder[0]
        førstePeriode.periode shouldBe Månedsperiode(YearMonth.of(2020, 1), YearMonth.of(2020, 1))
        førstePeriode.renter shouldBe BigDecimal.ZERO
        var feilPostering = førstePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(952), kodeResultat = KodeResultat.FORELDET)
        var ytelsePostering = førstePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(952),
            tilbakekrevesBeløp = BigDecimal.ZERO,
            uinnkrevdBeløp = BigDecimal(952),
            kodeResultat = KodeResultat.FORELDET,
        )

        val andrePeriode = tilbakekrevingsperioder[1]
        andrePeriode.periode shouldBe Månedsperiode(YearMonth.of(2020, 3), YearMonth.of(2020, 3))
        andrePeriode.renter shouldBe BigDecimal.ZERO
        feilPostering = andrePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(952), kodeResultat = KodeResultat.FORELDET)
        ytelsePostering = andrePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(952),
            tilbakekrevesBeløp = BigDecimal.ZERO,
            uinnkrevdBeløp = BigDecimal(952),
            kodeResultat = KodeResultat.FORELDET,
        )

        val tredjePeriode = tilbakekrevingsperioder[2]
        tredjePeriode.periode shouldBe Månedsperiode(YearMonth.of(2020, 5), YearMonth.of(2020, 5))
        tredjePeriode.renter shouldBe BigDecimal.ZERO
        feilPostering = tredjePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(952), kodeResultat = KodeResultat.INGEN_TILBAKEKREVING)
        ytelsePostering = tredjePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(952),
            tilbakekrevesBeløp = BigDecimal.ZERO,
            uinnkrevdBeløp = BigDecimal(952),
            kodeResultat = KodeResultat.INGEN_TILBAKEKREVING,
        )

        val fjerdePeriode = tilbakekrevingsperioder[3]
        fjerdePeriode.periode shouldBe Månedsperiode(YearMonth.of(2020, 7), YearMonth.of(2020, 7))
        fjerdePeriode.renter shouldBe BigDecimal.ZERO
        feilPostering = fjerdePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(952), kodeResultat = KodeResultat.INGEN_TILBAKEKREVING)
        ytelsePostering = fjerdePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(952),
            tilbakekrevesBeløp = BigDecimal.ZERO,
            uinnkrevdBeløp = BigDecimal(952),
            kodeResultat = KodeResultat.INGEN_TILBAKEKREVING,
        )

        val femtePeriode = tilbakekrevingsperioder[4]
        femtePeriode.periode shouldBe Månedsperiode(YearMonth.of(2020, 9), YearMonth.of(2020, 9))
        femtePeriode.renter shouldBe BigDecimal.ZERO
        feilPostering = femtePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(952), kodeResultat = KodeResultat.FULL_TILBAKEKREVING)
        ytelsePostering = femtePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(952),
            tilbakekrevesBeløp = BigDecimal(952),
            uinnkrevdBeløp = BigDecimal.ZERO,
            kodeResultat = KodeResultat.FULL_TILBAKEKREVING,
        )

        val sjettePeriode = tilbakekrevingsperioder[5]
        sjettePeriode.periode shouldBe Månedsperiode(YearMonth.of(2020, 11), YearMonth.of(2020, 11))
        sjettePeriode.renter shouldBe BigDecimal.ZERO
        feilPostering = sjettePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(952), kodeResultat = KodeResultat.FULL_TILBAKEKREVING)
        ytelsePostering = sjettePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(952),
            tilbakekrevesBeløp = BigDecimal(952),
            uinnkrevdBeløp = BigDecimal.ZERO,
            kodeResultat = KodeResultat.FULL_TILBAKEKREVING,
        )

        val sjuendePeriode = tilbakekrevingsperioder[6]
        sjuendePeriode.periode shouldBe Månedsperiode(YearMonth.of(2021, 1), YearMonth.of(2021, 1))
        sjuendePeriode.renter shouldBe BigDecimal.ZERO
        feilPostering = sjuendePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(952), kodeResultat = KodeResultat.FULL_TILBAKEKREVING)
        ytelsePostering = sjuendePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(952),
            tilbakekrevesBeløp = BigDecimal(952),
            uinnkrevdBeløp = BigDecimal.ZERO,
            kodeResultat = KodeResultat.FULL_TILBAKEKREVING,
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
                tilbakekrevSmåbeløp = true,
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
                tilbakekrevSmåbeløp = true,
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
            .sortedBy { it.periode.fom }
        tilbakekrevingsperioder.shouldNotBeNull()
        tilbakekrevingsperioder.size shouldBe 7
        shouldNotThrowAny {
            iverksettelseService.validerBeløp(
                behandling.id,
                tilbakekrevingsperioder,
                SecureLog.Context.tom()
            )
        }

        val førstePeriode = tilbakekrevingsperioder[0]
        førstePeriode.periode shouldBe sortedPerioder[0]
        førstePeriode.renter shouldBe BigDecimal.ZERO
        var feilPostering = førstePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(
            beløp = feilPostering,
            nyttBeløp = BigDecimal(486),
            kodeResultat = KodeResultat.INGEN_TILBAKEKREVING
        )
        var ytelsePostering = førstePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            nyttBeløp = BigDecimal(1544),
            utbetaltBeløp = BigDecimal(2030),
            tilbakekrevesBeløp = BigDecimal.ZERO,
            uinnkrevdBeløp = BigDecimal(486),
            kodeResultat = KodeResultat.INGEN_TILBAKEKREVING,
        )

        val andrePeriode = tilbakekrevingsperioder[1]
        andrePeriode.periode shouldBe sortedPerioder[1]
        andrePeriode.renter shouldBe BigDecimal.ZERO
        feilPostering = andrePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(
            beløp = feilPostering,
            nyttBeløp = BigDecimal(17336),
            kodeResultat = KodeResultat.INGEN_TILBAKEKREVING
        )
        ytelsePostering = andrePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            nyttBeløp = BigDecimal(3574),
            utbetaltBeløp = BigDecimal(20910),
            tilbakekrevesBeløp = BigDecimal.ZERO,
            uinnkrevdBeløp = BigDecimal(17336),
            kodeResultat = KodeResultat.INGEN_TILBAKEKREVING,
        )

        val tredjePeriode = tilbakekrevingsperioder[2]
        tredjePeriode.periode shouldBe sortedPerioder[2]
        tredjePeriode.renter shouldBe BigDecimal.ZERO
        feilPostering = tredjePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(
            beløp = feilPostering,
            nyttBeløp = BigDecimal(17241),
            kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING
        )
        ytelsePostering = tredjePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            nyttBeløp = BigDecimal(5658),
            utbetaltBeløp = BigDecimal(22899),
            tilbakekrevesBeløp = BigDecimal(8621),
            uinnkrevdBeløp = BigDecimal(8620),
            skattBeløp = BigDecimal(4310),
            kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING,
        )

        val fjerdePeriode = tilbakekrevingsperioder[3]
        fjerdePeriode.periode shouldBe sortedPerioder[3]
        fjerdePeriode.renter shouldBe BigDecimal.ZERO
        feilPostering = fjerdePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(
            beløp = feilPostering,
            nyttBeløp = BigDecimal(17241),
            kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING
        )
        ytelsePostering = fjerdePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            nyttBeløp = BigDecimal(5658),
            utbetaltBeløp = BigDecimal(22899),
            tilbakekrevesBeløp = BigDecimal(8620),
            uinnkrevdBeløp = BigDecimal(8621),
            skattBeløp = BigDecimal.ZERO,
            kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING,
        )

        val femtePeriode = tilbakekrevingsperioder[4]
        femtePeriode.periode shouldBe sortedPerioder[4]
        femtePeriode.renter shouldBe BigDecimal(1724)
        feilPostering = femtePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(
            beløp = feilPostering,
            nyttBeløp = BigDecimal(17241),
            kodeResultat = KodeResultat.FULL_TILBAKEKREVING
        )
        ytelsePostering = femtePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            nyttBeløp = BigDecimal(5658),
            utbetaltBeløp = BigDecimal(22899),
            tilbakekrevesBeløp = BigDecimal(17241),
            uinnkrevdBeløp = BigDecimal.ZERO,
            skattBeløp = BigDecimal(8620),
            kodeResultat = KodeResultat.FULL_TILBAKEKREVING,
        )

        val sjettePeriode = tilbakekrevingsperioder[5]
        sjettePeriode.periode shouldBe sortedPerioder[5]
        sjettePeriode.renter shouldBe BigDecimal(1724)
        feilPostering = sjettePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(
            beløp = feilPostering,
            nyttBeløp = BigDecimal(17241),
            kodeResultat = KodeResultat.FULL_TILBAKEKREVING
        )
        ytelsePostering = sjettePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            nyttBeløp = BigDecimal(5658),
            utbetaltBeløp = BigDecimal(22899),
            tilbakekrevesBeløp = BigDecimal(17241),
            uinnkrevdBeløp = BigDecimal.ZERO,
            skattBeløp = BigDecimal(8620),
            kodeResultat = KodeResultat.FULL_TILBAKEKREVING,
        )

        val sjuendePeriode = tilbakekrevingsperioder[6]
        sjuendePeriode.periode shouldBe sortedPerioder[6]
        sjuendePeriode.renter shouldBe BigDecimal(1736)
        feilPostering = sjuendePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(
            beløp = feilPostering,
            nyttBeløp = BigDecimal(17364),
            kodeResultat = KodeResultat.FULL_TILBAKEKREVING
        )
        ytelsePostering = sjuendePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            nyttBeløp = BigDecimal(25485),
            utbetaltBeløp = BigDecimal(42849),
            tilbakekrevesBeløp = BigDecimal(17364),
            uinnkrevdBeløp = BigDecimal.ZERO,
            skattBeløp = BigDecimal(8682),
            kodeResultat = KodeResultat.FULL_TILBAKEKREVING,
        )
    }

    @Test
    fun `beregnVedtaksperioder skal beregne periode med 100 prosent tilbakekreving og renter skal rundes ned`() {
        kravgrunnlagRepository.deleteById(kravgrunnlag.id)
        fagsakRepository.update(fagsakRepository.findByIdOrThrow(fagsak.id).copy(fagsystem = Fagsystem.EF))

        val kravgrunnlagxml = readXml("/kravgrunnlagxml/kravgrunnlag_EF_med_renter_avrundingsfeil_ned.xml")
        val kravgrunnlag =
            KravgrunnlagMapper.tilKravgrunnlag431(
                KravgrunnlagUtil.unmarshalKravgrunnlag(kravgrunnlagxml),
                behandling.id,
            )
        kravgrunnlagRepository.insert(kravgrunnlag)

        val periode = kravgrunnlag.perioder.first().periode

        val grovUaktsomhetPeriode = lagGrovtUaktsomVilkårsvurderingsperiode(periode.fom, periode.tom)

        vilkårsvurderingService.lagreVilkårsvurdering(
            behandling.id,
            BehandlingsstegVilkårsvurderingDto(
                listOf(grovUaktsomhetPeriode),
            ),
        )

        val tilbakekrevingsperioder =
            vedtakBeregningService
                .beregnVedtaksperioder(behandling.id, kravgrunnlag)
                .sortedBy { it.periode.fom }
        tilbakekrevingsperioder.size shouldBe 1
        shouldNotThrowAny { iverksettelseService.validerBeløp(behandling.id, tilbakekrevingsperioder, SecureLog.Context.tom()) }

        val tilbakekrevingsperiode = tilbakekrevingsperioder[0]
        tilbakekrevingsperiode.periode shouldBe periode
        tilbakekrevingsperiode.renter shouldBe BigDecimal(1860)
    }

    @Test
    fun `beregnVedtaksperioder som beregner flere perioder i samme vilkårsperiode med 100 prosent tilbakekreving og renter skal aldri overstige 10%`() {
        kravgrunnlagRepository.deleteById(kravgrunnlag.id)
        fagsakRepository.update(fagsakRepository.findByIdOrThrow(fagsak.id).copy(fagsystem = Fagsystem.EF))

        val kravgrunnlagxml = readXml("/kravgrunnlagxml/kravgrunnlag_EF_med_3_perioder_med_renter_avrunding_ned.xml")
        val kravgrunnlag =
            KravgrunnlagMapper.tilKravgrunnlag431(
                KravgrunnlagUtil.unmarshalKravgrunnlag(kravgrunnlagxml),
                behandling.id,
            )
        kravgrunnlagRepository.insert(kravgrunnlag)

        val sortedPerioder = kravgrunnlag.perioder.map { it.periode }.sortedBy { it.fom }

        val grovUaktsomhetPeriode = lagGrovtUaktsomVilkårsvurderingsperiode(sortedPerioder.first().fom, sortedPerioder.last().tom)

        vilkårsvurderingService.lagreVilkårsvurdering(
            behandling.id,
            BehandlingsstegVilkårsvurderingDto(
                listOf(grovUaktsomhetPeriode),
            ),
        )

        val tilbakekrevingsperioder =
            vedtakBeregningService
                .beregnVedtaksperioder(behandling.id, kravgrunnlag)
                .sortedBy { it.periode.fom }
        tilbakekrevingsperioder.size shouldBe 3
        shouldNotThrowAny { iverksettelseService.validerBeløp(behandling.id, tilbakekrevingsperioder, SecureLog.Context.tom()) }

        tilbakekrevingsperioder[0].periode shouldBe sortedPerioder[0]
        tilbakekrevingsperioder[0].renter shouldBe BigDecimal(1861)

        tilbakekrevingsperioder[1].periode shouldBe sortedPerioder[1]
        tilbakekrevingsperioder[1].renter shouldBe BigDecimal(1861)

        tilbakekrevingsperioder[2].periode shouldBe sortedPerioder[2]
        tilbakekrevingsperioder[2].renter shouldBe BigDecimal(1860)

        tilbakekrevingsperioder.sumOf { it.renter } shouldBe BigDecimal(5582)
    }

    @Test
    fun `beregnVedtaksperioder som beregner flere perioder i separate vilkårsperioder med 100 prosent tilbakekreving og renter skal skal avrunde hver renteperiode ned`() {
        kravgrunnlagRepository.deleteById(kravgrunnlag.id)
        fagsakRepository.update(fagsakRepository.findByIdOrThrow(fagsak.id).copy(fagsystem = Fagsystem.EF))

        val kravgrunnlagxml = readXml("/kravgrunnlagxml/kravgrunnlag_EF_med_3_perioder_med_renter_avrunding_ned.xml")
        val kravgrunnlag =
            KravgrunnlagMapper.tilKravgrunnlag431(
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
            BehandlingsstegVilkårsvurderingDto(
                listOf(grovUaktsomhetPeriode1, grovUaktsomhetPeriode2, grovUaktsomhetPeriode3),
            ),
        )

        val tilbakekrevingsperioder =
            vedtakBeregningService
                .beregnVedtaksperioder(behandling.id, kravgrunnlag)
                .sortedBy { it.periode.fom }
        tilbakekrevingsperioder.size shouldBe 3
        shouldNotThrowAny { iverksettelseService.validerBeløp(behandling.id, tilbakekrevingsperioder, SecureLog.Context.tom()) }

        tilbakekrevingsperioder[0].periode shouldBe sortedPerioder[0]
        tilbakekrevingsperioder[0].renter shouldBe BigDecimal(1861)
        tilbakekrevingsperioder[1].periode shouldBe sortedPerioder[1]
        tilbakekrevingsperioder[1].renter shouldBe BigDecimal(1861)
        tilbakekrevingsperioder[2].periode shouldBe sortedPerioder[2]
        tilbakekrevingsperioder[2].renter shouldBe BigDecimal(1860)
    }

    @Test
    fun `beregnVedtaksperioder skal beregne EF perioder med FORTSETT aktsomhet med full tilbakekreving med 10 prosent renter`() {
        kravgrunnlagRepository.deleteById(kravgrunnlag.id)
        fagsakRepository.update(fagsakRepository.findByIdOrThrow(fagsak.id).copy(fagsystem = Fagsystem.EF))

        val kravgrunnlagxml = readXml("/kravgrunnlagxml/kravgrunnlag_EF_med_renter_avrunding.xml")
        val kravgrunnlag =
            KravgrunnlagMapper.tilKravgrunnlag431(
                KravgrunnlagUtil.unmarshalKravgrunnlag(kravgrunnlagxml),
                behandling.id,
            )
        kravgrunnlagRepository.insert(kravgrunnlag)

        val sortedPerioder = kravgrunnlag.perioder.map { it.periode }.sortedBy { it.fom }

        val aktsomhetPeriode =
            VilkårsvurderingsperiodeDto(
                periode =
                    Datoperiode(
                        sortedPerioder[0].fom,
                        sortedPerioder[6].tom,
                    ),
                begrunnelse = "testverdi",
                aktsomhetDto =
                    AktsomhetDto(
                        aktsomhet = Aktsomhet.FORSETT,
                        begrunnelse = "fortsett begrunnelse",
                    ),
                vilkårsvurderingsresultat =
                    Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
            )

        vilkårsvurderingService.lagreVilkårsvurdering(behandling.id, BehandlingsstegVilkårsvurderingDto(listOf(aktsomhetPeriode)))

        val tilbakekrevingsperioder =
            vedtakBeregningService
                .beregnVedtaksperioder(behandling.id, kravgrunnlag)
                .sortedBy { it.periode.fom }
        tilbakekrevingsperioder.shouldNotBeNull()
        tilbakekrevingsperioder.size shouldBe 7
        shouldNotThrowAny { iverksettelseService.validerBeløp(behandling.id, tilbakekrevingsperioder, SecureLog.Context.tom()) }

        val førstePeriode = tilbakekrevingsperioder[0]
        førstePeriode.periode shouldBe sortedPerioder[0]
        førstePeriode.renter shouldBe BigDecimal(21)
        var feilPostering = førstePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(209), kodeResultat = KodeResultat.FULL_TILBAKEKREVING)
        var ytelsePostering = førstePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            nyttBeløp = BigDecimal(4028),
            utbetaltBeløp = BigDecimal(4237),
            tilbakekrevesBeløp = BigDecimal(209),
            uinnkrevdBeløp = BigDecimal.ZERO,
            skattBeløp = BigDecimal(104),
            kodeResultat = KodeResultat.FULL_TILBAKEKREVING,
        )

        val andrePeriode = tilbakekrevingsperioder[1]
        andrePeriode.periode shouldBe sortedPerioder[1]
        andrePeriode.renter shouldBe BigDecimal(21)
        feilPostering = andrePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(208), kodeResultat = KodeResultat.FULL_TILBAKEKREVING)
        ytelsePostering = andrePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            nyttBeløp = BigDecimal(5070),
            utbetaltBeløp = BigDecimal(5278),
            tilbakekrevesBeløp = BigDecimal(208),
            uinnkrevdBeløp = BigDecimal.ZERO,
            skattBeløp = BigDecimal(104),
            kodeResultat = KodeResultat.FULL_TILBAKEKREVING,
        )

        val tredjePeriode = tilbakekrevingsperioder[2]
        tredjePeriode.periode shouldBe sortedPerioder[2]
        tredjePeriode.renter shouldBe BigDecimal(438)
        feilPostering = tredjePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(4375), kodeResultat = KodeResultat.FULL_TILBAKEKREVING)
        ytelsePostering = tredjePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            nyttBeløp = BigDecimal(5070),
            utbetaltBeløp = BigDecimal(9445),
            tilbakekrevesBeløp = BigDecimal(4375),
            uinnkrevdBeløp = BigDecimal.ZERO,
            skattBeløp = BigDecimal.ZERO,
            kodeResultat = KodeResultat.FULL_TILBAKEKREVING,
        )

        val fjerdePeriode = tilbakekrevingsperioder[3]
        fjerdePeriode.periode shouldBe sortedPerioder[3]
        fjerdePeriode.renter shouldBe BigDecimal(437)
        feilPostering = fjerdePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(4375), kodeResultat = KodeResultat.FULL_TILBAKEKREVING)
        ytelsePostering = fjerdePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            nyttBeløp = BigDecimal(5070),
            utbetaltBeløp = BigDecimal(9445),
            tilbakekrevesBeløp = BigDecimal(4375),
            uinnkrevdBeløp = BigDecimal.ZERO,
            skattBeløp = BigDecimal(2187),
            kodeResultat = KodeResultat.FULL_TILBAKEKREVING,
        )

        val femtePeriode = tilbakekrevingsperioder[4]
        femtePeriode.periode shouldBe sortedPerioder[4]
        femtePeriode.renter shouldBe BigDecimal(437)
        feilPostering = femtePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(4375), kodeResultat = KodeResultat.FULL_TILBAKEKREVING)
        ytelsePostering = femtePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            nyttBeløp = BigDecimal(5070),
            utbetaltBeløp = BigDecimal(9445),
            tilbakekrevesBeløp = BigDecimal(4375),
            uinnkrevdBeløp = BigDecimal.ZERO,
            skattBeløp = BigDecimal(2187),
            kodeResultat = KodeResultat.FULL_TILBAKEKREVING,
        )

        val sjettePeriode = tilbakekrevingsperioder[5]
        sjettePeriode.periode shouldBe sortedPerioder[5]
        sjettePeriode.renter shouldBe BigDecimal(375)
        feilPostering = sjettePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(3750), kodeResultat = KodeResultat.FULL_TILBAKEKREVING)
        ytelsePostering = sjettePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            nyttBeløp = BigDecimal(10695),
            utbetaltBeløp = BigDecimal(14445),
            tilbakekrevesBeløp = BigDecimal(3750),
            uinnkrevdBeløp = BigDecimal.ZERO,
            skattBeløp = BigDecimal(1875),
            kodeResultat = KodeResultat.FULL_TILBAKEKREVING,
        )

        val sjuendePeriode = tilbakekrevingsperioder[6]
        sjuendePeriode.periode shouldBe sortedPerioder[6]
        sjuendePeriode.renter shouldBe BigDecimal(375)
        feilPostering = sjuendePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(3750), kodeResultat = KodeResultat.FULL_TILBAKEKREVING)
        ytelsePostering = sjuendePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            nyttBeløp = BigDecimal(10695),
            utbetaltBeløp = BigDecimal(14445),
            tilbakekrevesBeløp = BigDecimal(3750),
            uinnkrevdBeløp = BigDecimal.ZERO,
            skattBeløp = BigDecimal(1875),
            kodeResultat = KodeResultat.FULL_TILBAKEKREVING,
        )
    }

    @Test
    fun `beregnVedtaksperioder skal beregne EF perioder med 50 prosent tilbakekreving og skatt avrunding`() {
        kravgrunnlagRepository.deleteById(kravgrunnlag.id)
        fagsakRepository.update(fagsakRepository.findByIdOrThrow(fagsak.id).copy(fagsystem = Fagsystem.EF))

        val kravgrunnlagxml = readXml("/kravgrunnlagxml/kravgrunnlag_EF_med_skatt_avrunding.xml")
        val kravgrunnlag =
            KravgrunnlagMapper.tilKravgrunnlag431(
                KravgrunnlagUtil.unmarshalKravgrunnlag(kravgrunnlagxml),
                behandling.id,
            )
        kravgrunnlagRepository.insert(kravgrunnlag)

        val sortedPerioder = kravgrunnlag.perioder.map { it.periode }.sortedBy { it.fom }

        val aktsomhetPeriode =
            VilkårsvurderingsperiodeDto(
                periode =
                    Datoperiode(
                        sortedPerioder[0].fom,
                        sortedPerioder[1].tom,
                    ),
                begrunnelse = "testverdi",
                aktsomhetDto =
                    AktsomhetDto(
                        aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
                        begrunnelse = "simpel uaktsomhet begrunnelse",
                        tilbakekrevSmåbeløp = true,
                        særligeGrunnerBegrunnelse = "test",
                        særligeGrunnerTilReduksjon = true,
                        særligeGrunner =
                            listOf(
                                SærligGrunnDto(
                                    HELT_ELLER_DELVIS_NAVS_FEIL,
                                ),
                            ),
                        andelTilbakekreves = BigDecimal(50),
                    ),
                vilkårsvurderingsresultat =
                    Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
            )

        vilkårsvurderingService.lagreVilkårsvurdering(behandling.id, BehandlingsstegVilkårsvurderingDto(listOf(aktsomhetPeriode)))

        val tilbakekrevingsperioder =
            vedtakBeregningService
                .beregnVedtaksperioder(behandling.id, kravgrunnlag)
                .sortedBy { it.periode.fom }
        tilbakekrevingsperioder.shouldNotBeNull()
        tilbakekrevingsperioder.size shouldBe 2
        shouldNotThrowAny { iverksettelseService.validerBeløp(behandling.id, tilbakekrevingsperioder, SecureLog.Context.tom()) }

        val førstePeriode = tilbakekrevingsperioder[0]
        førstePeriode.periode shouldBe sortedPerioder[0]
        førstePeriode.renter shouldBe BigDecimal(0)
        var feilPostering = førstePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(1755), kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING)
        var ytelsePostering = førstePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            nyttBeløp = BigDecimal(18195),
            utbetaltBeløp = BigDecimal(19950),
            tilbakekrevesBeløp = BigDecimal(878),
            uinnkrevdBeløp = BigDecimal(877),
            skattBeløp = BigDecimal(386),
            kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING,
        )

        val andrePeriode = tilbakekrevingsperioder[1]
        andrePeriode.periode shouldBe sortedPerioder[1]
        andrePeriode.renter shouldBe BigDecimal(0)
        feilPostering = andrePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(1755), kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING)
        ytelsePostering = andrePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            nyttBeløp = BigDecimal(18195),
            utbetaltBeløp = BigDecimal(19950),
            tilbakekrevesBeløp = BigDecimal(877),
            uinnkrevdBeløp = BigDecimal(878),
            skattBeløp = BigDecimal(438),
            kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING,
        )
    }

    @Test
    fun `beregnVedtaksperioder skal beregne EF perioder med delvis tilbakekreving og skatt avrunding`() {
        kravgrunnlagRepository.deleteById(kravgrunnlag.id)
        fagsakRepository.update(fagsakRepository.findByIdOrThrow(fagsak.id).copy(fagsystem = Fagsystem.EF))

        val kravgrunnlagxml = readXml("/kravgrunnlagxml/kravgrunnlag_EF_med_skatt_avrunding_3.xml")
        val kravgrunnlag =
            KravgrunnlagMapper.tilKravgrunnlag431(
                KravgrunnlagUtil.unmarshalKravgrunnlag(kravgrunnlagxml),
                behandling.id,
            )
        kravgrunnlagRepository.insert(kravgrunnlag)

        val sortedPerioder = kravgrunnlag.perioder.map { it.periode }.sortedBy { it.fom }

        val aktsomhetPeriode =
            VilkårsvurderingsperiodeDto(
                periode =
                    Datoperiode(
                        sortedPerioder[0].fom,
                        sortedPerioder[2].tom,
                    ),
                begrunnelse = "testverdi",
                aktsomhetDto =
                    AktsomhetDto(
                        aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
                        begrunnelse = "simpel uaktsomhet begrunnelse",
                        tilbakekrevSmåbeløp = true,
                        særligeGrunnerBegrunnelse = "test",
                        særligeGrunnerTilReduksjon = false,
                        særligeGrunner = listOf(SærligGrunnDto(GRAD_AV_UAKTSOMHET), SærligGrunnDto(HELT_ELLER_DELVIS_NAVS_FEIL)),
                        andelTilbakekreves = BigDecimal(100),
                    ),
                vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
            )

        val aktsomhetPeriode1 =
            VilkårsvurderingsperiodeDto(
                periode =
                    Datoperiode(
                        sortedPerioder[3].fom,
                        sortedPerioder[3].tom,
                    ),
                begrunnelse = "testverdi",
                aktsomhetDto =
                    AktsomhetDto(
                        aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
                        begrunnelse = "simpel uaktsomhet begrunnelse",
                        tilbakekrevSmåbeløp = true,
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

        val tilbakekrevingsperioder =
            vedtakBeregningService
                .beregnVedtaksperioder(behandling.id, kravgrunnlag)
                .sortedBy { it.periode.fom }
        tilbakekrevingsperioder.shouldNotBeNull()
        tilbakekrevingsperioder.size shouldBe 4
        shouldNotThrowAny { iverksettelseService.validerBeløp(behandling.id, tilbakekrevingsperioder, SecureLog.Context.tom()) }

        val førstePeriode = tilbakekrevingsperioder[0]
        førstePeriode.periode shouldBe sortedPerioder[0]
        førstePeriode.renter shouldBe BigDecimal(0)
        var feilPostering = førstePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(637), kodeResultat = KodeResultat.FULL_TILBAKEKREVING)
        var ytelsePostering = førstePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(8782),
            tilbakekrevesBeløp = BigDecimal(637),
            uinnkrevdBeløp = BigDecimal.ZERO,
            skattBeløp = BigDecimal(217),
            kodeResultat = KodeResultat.FULL_TILBAKEKREVING,
        )

        val andrePeriode = tilbakekrevingsperioder[1]
        andrePeriode.periode shouldBe sortedPerioder[1]
        andrePeriode.renter shouldBe BigDecimal(0)
        feilPostering = andrePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(1087), kodeResultat = KodeResultat.FULL_TILBAKEKREVING)
        ytelsePostering = andrePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(8782),
            tilbakekrevesBeløp = BigDecimal(1087),
            uinnkrevdBeløp = BigDecimal.ZERO,
            skattBeløp = BigDecimal(369),
            kodeResultat = KodeResultat.FULL_TILBAKEKREVING,
        )

        val tredjePeriode = tilbakekrevingsperioder[2]
        tredjePeriode.periode shouldBe sortedPerioder[2]
        tredjePeriode.renter shouldBe BigDecimal(0)
        feilPostering = tredjePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(2250), kodeResultat = KodeResultat.FULL_TILBAKEKREVING)
        ytelsePostering = tredjePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(8782),
            tilbakekrevesBeløp = BigDecimal(2250),
            uinnkrevdBeløp = BigDecimal.ZERO,
            skattBeløp = BigDecimal(382),
            kodeResultat = KodeResultat.FULL_TILBAKEKREVING,
        )

        val fjerdePeriode = tilbakekrevingsperioder[3]
        fjerdePeriode.periode shouldBe sortedPerioder[3]
        fjerdePeriode.renter shouldBe BigDecimal(0)
        feilPostering = fjerdePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(8782), kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING)
        ytelsePostering = fjerdePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(8782),
            tilbakekrevesBeløp = BigDecimal(5972),
            uinnkrevdBeløp = BigDecimal(2810),
            skattBeløp = BigDecimal(2030),
            kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING,
        )
    }

    @Test
    fun `beregnVedtaksperioder skal beregne EF perioder med full tilbakekreving og skatt avrunding`() {
        kravgrunnlagRepository.deleteById(kravgrunnlag.id)
        fagsakRepository.update(fagsakRepository.findByIdOrThrow(fagsak.id).copy(fagsystem = Fagsystem.EF))

        val kravgrunnlagxml = readXml("/kravgrunnlagxml/kravgrunnlag_EF_med_skatt_avrunding_2.xml")
        val kravgrunnlag =
            KravgrunnlagMapper.tilKravgrunnlag431(
                KravgrunnlagUtil.unmarshalKravgrunnlag(kravgrunnlagxml),
                behandling.id,
            )
        kravgrunnlagRepository.insert(kravgrunnlag)

        val sortedPerioder = kravgrunnlag.perioder.map { it.periode }.sortedBy { it.fom }

        val aktsomhetPeriode =
            VilkårsvurderingsperiodeDto(
                periode =
                    Datoperiode(
                        sortedPerioder[0].fom,
                        sortedPerioder[3].tom,
                    ),
                begrunnelse = "testverdi",
                aktsomhetDto =
                    AktsomhetDto(
                        aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
                        begrunnelse = "simpel uaktsomhet begrunnelse",
                        tilbakekrevSmåbeløp = true,
                        særligeGrunnerBegrunnelse = "test",
                        særligeGrunnerTilReduksjon = true,
                        særligeGrunner =
                            listOf(
                                SærligGrunnDto(
                                    HELT_ELLER_DELVIS_NAVS_FEIL,
                                ),
                            ),
                        andelTilbakekreves = BigDecimal(100),
                    ),
                vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
            )

        vilkårsvurderingService.lagreVilkårsvurdering(behandling.id, BehandlingsstegVilkårsvurderingDto(listOf(aktsomhetPeriode)))

        val tilbakekrevingsperioder =
            vedtakBeregningService
                .beregnVedtaksperioder(behandling.id, kravgrunnlag)
                .sortedBy { it.periode.fom }
        tilbakekrevingsperioder.shouldNotBeNull()
        tilbakekrevingsperioder.size shouldBe 4
        shouldNotThrowAny { iverksettelseService.validerBeløp(behandling.id, tilbakekrevingsperioder, SecureLog.Context.tom()) }

        val førstePeriode = tilbakekrevingsperioder[0]
        førstePeriode.periode shouldBe sortedPerioder[0]
        førstePeriode.renter shouldBe BigDecimal(0)
        var feilPostering = førstePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(2962), kodeResultat = KodeResultat.FULL_TILBAKEKREVING)
        var ytelsePostering = førstePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(12607),
            tilbakekrevesBeløp = BigDecimal(2962),
            uinnkrevdBeløp = BigDecimal.ZERO,
            skattBeløp = BigDecimal(429),
            kodeResultat = KodeResultat.FULL_TILBAKEKREVING,
        )

        val andrePeriode = tilbakekrevingsperioder[1]
        andrePeriode.periode shouldBe sortedPerioder[1]
        andrePeriode.renter shouldBe BigDecimal(0)
        feilPostering = andrePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(1725), kodeResultat = KodeResultat.FULL_TILBAKEKREVING)
        ytelsePostering = andrePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(12607),
            tilbakekrevesBeløp = BigDecimal(1725),
            uinnkrevdBeløp = BigDecimal.ZERO,
            skattBeløp = BigDecimal(431),
            kodeResultat = KodeResultat.FULL_TILBAKEKREVING,
        )

        val tredjePeriode = tilbakekrevingsperioder[2]
        tredjePeriode.periode shouldBe sortedPerioder[2]
        tredjePeriode.renter shouldBe BigDecimal(0)
        feilPostering = tredjePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(1050), kodeResultat = KodeResultat.FULL_TILBAKEKREVING)
        ytelsePostering = tredjePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(12607),
            tilbakekrevesBeløp = BigDecimal(1050),
            uinnkrevdBeløp = BigDecimal.ZERO,
            skattBeløp = BigDecimal(262),
            kodeResultat = KodeResultat.FULL_TILBAKEKREVING,
        )

        val fjerdePeriode = tilbakekrevingsperioder[3]
        fjerdePeriode.periode shouldBe sortedPerioder[3]
        fjerdePeriode.renter shouldBe BigDecimal(0)
        feilPostering = fjerdePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(150), kodeResultat = KodeResultat.FULL_TILBAKEKREVING)
        ytelsePostering = fjerdePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(
            beløp = ytelsePostering,
            utbetaltBeløp = BigDecimal(12607),
            tilbakekrevesBeløp = BigDecimal(150),
            uinnkrevdBeløp = BigDecimal.ZERO,
            skattBeløp = BigDecimal(38),
            kodeResultat = KodeResultat.FULL_TILBAKEKREVING,
        )
    }

    private fun lagForeldelse(perioder: List<Månedsperiode>) {
        val foreldelsesdata =
            BehandlingsstegForeldelseDto(
                perioder.map {
                    ForeldelsesperiodeDto(
                        periode = it.toDatoperiode(),
                        begrunnelse = "testverdi",
                        foreldelsesvurderingstype =
                            Foreldelsesvurderingstype.FORELDET,
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
        val vilkårsperioder =
            perioder.map {
                VilkårsvurderingsperiodeDto(
                    periode = it.toDatoperiode(),
                    begrunnelse = "testverdi",
                    aktsomhetDto =
                        AktsomhetDto(
                            aktsomhet = aktsomhet,
                            andelTilbakekreves = andelTilbakreves,
                            beløpTilbakekreves = beløpTilbakekreves,
                            begrunnelse = "testverdi",
                            særligeGrunnerTilReduksjon = særligeGrunnerTilReduksjon,
                            tilbakekrevSmåbeløp = true,
                            særligeGrunnerBegrunnelse = "testverdi",
                            særligeGrunner =
                                listOf(
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
        val vilkårsperioder =
            perioder.map {
                VilkårsvurderingsperiodeDto(
                    periode = it.toDatoperiode(),
                    begrunnelse = "testverdi",
                    godTroDto =
                        GodTroDto(
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
    ): Kravgrunnlag431 =
        Kravgrunnlag431(
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
            kontrollfelt = "testverdi",
            referanse = behandling.aktivFagsystemsbehandling.eksternId,
            eksternKravgrunnlagId = BigInteger.ZERO,
            saksbehandlerId = "testverdi",
            perioder = lagKravgrunnlagsperiode(perioder, månedligSkattBeløp, kravgrunnlagsbeløpene),
        )

    private fun lagKravgrunnlagsperiode(
        perioder: List<Månedsperiode>,
        månedligSkattBeløp: BigDecimal,
        kravgrunnlagsbeløpene: List<Kravgrunnlagsbeløp>,
    ): Set<Kravgrunnlagsperiode432> =
        perioder
            .map {
                Kravgrunnlagsperiode432(
                    periode = it,
                    månedligSkattebeløp = månedligSkattBeløp,
                    beløp = lagKravgrunnlagsbeløp(kravgrunnlagsbeløpene),
                )
            }.toSet()

    private fun lagKravgrunnlagsbeløp(kravgrunnlagsbeløpene: List<Kravgrunnlagsbeløp>): Set<Kravgrunnlagsbeløp433> =
        kravgrunnlagsbeløpene
            .map {
                Kravgrunnlagsbeløp433(
                    klassekode = Klassekode.BATR,
                    klassetype = it.klassetype,
                    opprinneligUtbetalingsbeløp = it.opprinneligUtbetalingsbeløp,
                    nyttBeløp = it.nyttBeløp,
                    tilbakekrevesBeløp = it.tilbakekrevesBeløp,
                    uinnkrevdBeløp = it.uinnkrevdBeløp,
                    skatteprosent = it.skatteprosent,
                )
            }.toSet()

    private fun assertBeløp(
        beløp: Tilbakekrevingsbeløp,
        nyttBeløp: BigDecimal = BigDecimal.ZERO,
        utbetaltBeløp: BigDecimal = BigDecimal.ZERO,
        tilbakekrevesBeløp: BigDecimal = BigDecimal.ZERO,
        uinnkrevdBeløp: BigDecimal = BigDecimal.ZERO,
        skattBeløp: BigDecimal = BigDecimal.ZERO,
        kodeResultat: KodeResultat,
    ) {
        beløp.nyttBeløp shouldBe nyttBeløp
        beløp.utbetaltBeløp shouldBe utbetaltBeløp
        beløp.utbetaltBeløp shouldBe utbetaltBeløp
        beløp.tilbakekrevesBeløp shouldBe tilbakekrevesBeløp
        beløp.uinnkrevdBeløp shouldBe uinnkrevdBeløp
        beløp.skattBeløp shouldBe skattBeløp
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
