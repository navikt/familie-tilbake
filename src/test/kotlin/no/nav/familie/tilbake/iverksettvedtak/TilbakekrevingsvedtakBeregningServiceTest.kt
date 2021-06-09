package no.nav.familie.tilbake.iverksettvedtak

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.api.dto.AktsomhetDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegForeldelseDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegVilkårsvurderingDto
import no.nav.familie.tilbake.api.dto.ForeldelsesperiodeDto
import no.nav.familie.tilbake.api.dto.GodTroDto
import no.nav.familie.tilbake.api.dto.PeriodeDto
import no.nav.familie.tilbake.api.dto.SærligGrunnDto
import no.nav.familie.tilbake.api.dto.VilkårsvurderingsperiodeDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.foreldelse.ForeldelseService
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesvurderingstype
import no.nav.familie.tilbake.iverksettvedtak.domain.KodeResultat
import no.nav.familie.tilbake.iverksettvedtak.domain.Tilbakekrevingsbeløp
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.Fagområdekode
import no.nav.familie.tilbake.kravgrunnlag.domain.GjelderType
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassekode
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsbeløp433
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsperiode432
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravstatuskode
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.tilbake.vilkårsvurdering.domain.Aktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.SærligGrunn
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsresultat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.math.BigInteger
import java.time.YearMonth
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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

    private val fagsak = Testdata.fagsak
    private val behandling = Testdata.behandling

    private val perioder = listOf(Periode(YearMonth.of(2021, 1), YearMonth.of(2021, 1)),
                                  Periode(YearMonth.of(2021, 2), YearMonth.of(2021, 2)))

    private lateinit var kravgrunnlag: Kravgrunnlag431

    @BeforeEach
    fun init() {
        fagsakRepository.insert(fagsak)
        behandlingRepository.insert(behandling)

        val månedligSkattBeløp = BigDecimal.ZERO
        val kravgrunnlagsbeløpene = listOf(Kravgrunnlagsbeløp(klassetype = Klassetype.FEIL, nyttBeløp = BigDecimal(5000)),
                                           Kravgrunnlagsbeløp(klassetype = Klassetype.YTEL,
                                                              opprinneligUtbetalingsbeløp = BigDecimal(5000),
                                                              tilbakekrevesBeløp = BigDecimal(5000)))

        kravgrunnlag = lagKravgrunnlag(perioder, månedligSkattBeløp, kravgrunnlagsbeløpene)
        kravgrunnlagRepository.insert(kravgrunnlag)
    }

    @Test
    fun `beregnVedtaksperioder skal beregne når vilkårsvurderte med 50 prosent andel tilbakekrevesbeløp`() {
        lagAktsomhetVilkårsvurdering(perioder = listOf(Periode(YearMonth.of(2021, 1),
                                                               YearMonth.of(2021, 2))),
                                     aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
                                     andelTilbakreves = BigDecimal(50), særligeGrunnerTilReduksjon = true)

        var tilbakekrevingsperioder = vedtakBeregningService.beregnVedtaksperioder(behandling.id, kravgrunnlag)
        assertNotNull(tilbakekrevingsperioder)
        assertEquals(2, tilbakekrevingsperioder.size)
        tilbakekrevingsperioder = tilbakekrevingsperioder.sortedBy { it.periode.fom }

        val førstePeriode = tilbakekrevingsperioder[0]
        assertEquals(Periode(YearMonth.of(2021, 1), YearMonth.of(2021, 1)), førstePeriode.periode)
        assertEquals(BigDecimal.ZERO, førstePeriode.renter)
        var feilPostering = førstePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING)
        var ytelsePostering = førstePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(beløp = ytelsePostering,
                    utbetaltBeløp = BigDecimal(5000),
                    tilbakekrevesBeløp = BigDecimal(2500),
                    uinnkrevdBeløp = BigDecimal(2500),
                    kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING)

        val andrePeriode = tilbakekrevingsperioder[1]
        assertEquals(Periode(YearMonth.of(2021, 2), YearMonth.of(2021, 2)), andrePeriode.periode)
        assertEquals(BigDecimal.ZERO, andrePeriode.renter)
        feilPostering = andrePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING)
        ytelsePostering = andrePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(beløp = ytelsePostering,
                    utbetaltBeløp = BigDecimal(5000),
                    tilbakekrevesBeløp = BigDecimal(2500),
                    uinnkrevdBeløp = BigDecimal(2500),
                    kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING)

    }

    @Test
    fun `beregnVedtaksperioder skal beregne når vilkårsvurderte med 33 prosent andel tilbakekrevesbeløp`() {
        lagAktsomhetVilkårsvurdering(perioder = listOf(Periode(YearMonth.of(2021, 1),
                                                               YearMonth.of(2021, 2))),
                                     aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
                                     andelTilbakreves = BigDecimal(33), særligeGrunnerTilReduksjon = true)

        var tilbakekrevingsperioder = vedtakBeregningService.beregnVedtaksperioder(behandling.id, kravgrunnlag)
        assertNotNull(tilbakekrevingsperioder)
        assertEquals(2, tilbakekrevingsperioder.size)
        tilbakekrevingsperioder = tilbakekrevingsperioder.sortedBy { it.periode.fom }

        val førstePeriode = tilbakekrevingsperioder[0]
        assertEquals(Periode(YearMonth.of(2021, 1), YearMonth.of(2021, 1)), førstePeriode.periode)
        assertEquals(BigDecimal.ZERO, førstePeriode.renter)
        var feilPostering = førstePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING)
        var ytelsePostering = førstePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(beløp = ytelsePostering,
                    utbetaltBeløp = BigDecimal(5000),
                    tilbakekrevesBeløp = BigDecimal(1650),
                    uinnkrevdBeløp = BigDecimal(3350),
                    kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING)

        val andrePeriode = tilbakekrevingsperioder[1]
        assertEquals(Periode(YearMonth.of(2021, 2), YearMonth.of(2021, 2)), andrePeriode.periode)
        assertEquals(BigDecimal.ZERO, andrePeriode.renter)
        feilPostering = andrePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING)
        ytelsePostering = andrePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(beløp = ytelsePostering,
                    utbetaltBeløp = BigDecimal(5000),
                    tilbakekrevesBeløp = BigDecimal(1650),
                    uinnkrevdBeløp = BigDecimal(3350),
                    kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING)

    }

    @Test
    fun `beregnVedtaksperioder skal beregne når vilkårsvurderte med Forsett aktsomhet`() {
        lagAktsomhetVilkårsvurdering(perioder = listOf(Periode(YearMonth.of(2021, 1),
                                                               YearMonth.of(2021, 2))),
                                     aktsomhet = Aktsomhet.FORSETT)

        var tilbakekrevingsperioder = vedtakBeregningService.beregnVedtaksperioder(behandling.id, kravgrunnlag)
        assertNotNull(tilbakekrevingsperioder)
        assertEquals(2, tilbakekrevingsperioder.size)
        tilbakekrevingsperioder = tilbakekrevingsperioder.sortedBy { it.periode.fom }

        val førstePeriode = tilbakekrevingsperioder[0]
        assertEquals(Periode(YearMonth.of(2021, 1), YearMonth.of(2021, 1)), førstePeriode.periode)
        assertEquals(BigDecimal.ZERO, førstePeriode.renter)
        var feilPostering = førstePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.FULL_TILBAKEKREVING)
        var ytelsePostering = førstePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(beløp = ytelsePostering,
                    utbetaltBeløp = BigDecimal(5000),
                    tilbakekrevesBeløp = BigDecimal(5000),
                    uinnkrevdBeløp = BigDecimal.ZERO,
                    kodeResultat = KodeResultat.FULL_TILBAKEKREVING)

        val andrePeriode = tilbakekrevingsperioder[1]
        assertEquals(Periode(YearMonth.of(2021, 2), YearMonth.of(2021, 2)), andrePeriode.periode)
        assertEquals(BigDecimal.ZERO, andrePeriode.renter)
        feilPostering = andrePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.FULL_TILBAKEKREVING)
        ytelsePostering = andrePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(beløp = ytelsePostering,
                    utbetaltBeløp = BigDecimal(5000),
                    tilbakekrevesBeløp = BigDecimal(5000),
                    uinnkrevdBeløp = BigDecimal.ZERO,
                    kodeResultat = KodeResultat.FULL_TILBAKEKREVING)

    }

    @Test
    fun `beregnVedtaksperioder skal beregne når vilkårsvurderte med God tro og ingen tilbakekreving`() {
        lagGodTroVilkårsvurdering(perioder = listOf(Periode(YearMonth.of(2021, 1),
                                                            YearMonth.of(2021, 2))))

        var tilbakekrevingsperioder = vedtakBeregningService.beregnVedtaksperioder(behandling.id, kravgrunnlag)
        assertNotNull(tilbakekrevingsperioder)
        assertEquals(2, tilbakekrevingsperioder.size)
        tilbakekrevingsperioder = tilbakekrevingsperioder.sortedBy { it.periode.fom }

        val førstePeriode = tilbakekrevingsperioder[0]
        assertEquals(Periode(YearMonth.of(2021, 1), YearMonth.of(2021, 1)), førstePeriode.periode)
        assertEquals(BigDecimal.ZERO, førstePeriode.renter)
        var feilPostering = førstePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.INGEN_TILBAKEKREVING)
        var ytelsePostering = førstePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(beløp = ytelsePostering,
                    utbetaltBeløp = BigDecimal(5000),
                    tilbakekrevesBeløp = BigDecimal.ZERO,
                    uinnkrevdBeløp = BigDecimal(5000),
                    kodeResultat = KodeResultat.INGEN_TILBAKEKREVING)

        val andrePeriode = tilbakekrevingsperioder[1]
        assertEquals(Periode(YearMonth.of(2021, 2), YearMonth.of(2021, 2)), andrePeriode.periode)
        assertEquals(BigDecimal.ZERO, andrePeriode.renter)
        feilPostering = andrePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.INGEN_TILBAKEKREVING)
        ytelsePostering = andrePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(beløp = ytelsePostering,
                    utbetaltBeløp = BigDecimal(5000),
                    tilbakekrevesBeløp = BigDecimal.ZERO,
                    uinnkrevdBeløp = BigDecimal(5000),
                    kodeResultat = KodeResultat.INGEN_TILBAKEKREVING)

    }

    @Test
    fun `beregnVedtaksperioder skal beregne når vilkårsvurderte med God tro og bestemt tilbakekrevesbeløp`() {
        lagGodTroVilkårsvurdering(perioder = listOf(Periode(YearMonth.of(2021, 1),
                                                            YearMonth.of(2021, 2))),
                                  beløpErIBehold = true, beløpTilbakekreves = BigDecimal(3000))

        var tilbakekrevingsperioder = vedtakBeregningService.beregnVedtaksperioder(behandling.id, kravgrunnlag)
        assertNotNull(tilbakekrevingsperioder)
        assertEquals(2, tilbakekrevingsperioder.size)
        tilbakekrevingsperioder = tilbakekrevingsperioder.sortedBy { it.periode.fom }

        val førstePeriode = tilbakekrevingsperioder[0]
        assertEquals(Periode(YearMonth.of(2021, 1), YearMonth.of(2021, 1)), førstePeriode.periode)
        assertEquals(BigDecimal.ZERO, førstePeriode.renter)
        var feilPostering = førstePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING)
        var ytelsePostering = førstePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(beløp = ytelsePostering,
                    utbetaltBeløp = BigDecimal(5000),
                    tilbakekrevesBeløp = BigDecimal(1500),
                    uinnkrevdBeløp = BigDecimal(3500),
                    kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING)

        val andrePeriode = tilbakekrevingsperioder[1]
        assertEquals(Periode(YearMonth.of(2021, 2), YearMonth.of(2021, 2)), andrePeriode.periode)
        assertEquals(BigDecimal.ZERO, andrePeriode.renter)
        feilPostering = andrePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING)
        ytelsePostering = andrePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(beløp = ytelsePostering,
                    utbetaltBeløp = BigDecimal(5000),
                    tilbakekrevesBeløp = BigDecimal(1500),
                    uinnkrevdBeløp = BigDecimal(3500),
                    kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING)

    }

    @Test
    fun `beregnVedtaksperioder skal beregne når vilkårsvurderte med God tro og bestemt tilbakekrevesbeløp med avrunding`() {
        lagGodTroVilkårsvurdering(perioder = listOf(Periode(YearMonth.of(2021, 1),
                                                            YearMonth.of(2021, 2))),
                                  beløpErIBehold = true, beløpTilbakekreves = BigDecimal(1999))

        var tilbakekrevingsperioder = vedtakBeregningService.beregnVedtaksperioder(behandling.id, kravgrunnlag)
        assertNotNull(tilbakekrevingsperioder)
        assertEquals(2, tilbakekrevingsperioder.size)
        tilbakekrevingsperioder = tilbakekrevingsperioder.sortedBy { it.periode.fom }

        val førstePeriode = tilbakekrevingsperioder[0]
        assertEquals(Periode(YearMonth.of(2021, 1), YearMonth.of(2021, 1)), førstePeriode.periode)
        assertEquals(BigDecimal.ZERO, førstePeriode.renter)
        var feilPostering = førstePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING)
        var ytelsePostering = førstePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(beløp = ytelsePostering,
                    utbetaltBeløp = BigDecimal(5000),
                    tilbakekrevesBeløp = BigDecimal(999),
                    uinnkrevdBeløp = BigDecimal(4001),
                    kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING)

        val andrePeriode = tilbakekrevingsperioder[1]
        assertEquals(Periode(YearMonth.of(2021, 2), YearMonth.of(2021, 2)), andrePeriode.periode)
        assertEquals(BigDecimal.ZERO, andrePeriode.renter)
        feilPostering = andrePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING)
        ytelsePostering = andrePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(beløp = ytelsePostering,
                    utbetaltBeløp = BigDecimal(5000),
                    tilbakekrevesBeløp = BigDecimal(1000),
                    uinnkrevdBeløp = BigDecimal(4000),
                    kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING)
    }

    @Test
    fun `beregnVedtaksperioder skal beregne når vilkårsvurderte med 50 prosent andeltilbakekrevesbeløp med skatt beløp`() {
        val månedligSkattBeløp = BigDecimal(500)

        kravgrunnlagRepository.deleteById(kravgrunnlag.id)
        val kravgrunnlagsbeløpene = listOf(Kravgrunnlagsbeløp(klassetype = Klassetype.FEIL,
                                                              nyttBeløp = BigDecimal(5000),
                                                              skatteprosent = BigDecimal(10)),
                                           Kravgrunnlagsbeløp(klassetype = Klassetype.YTEL,
                                                              opprinneligUtbetalingsbeløp = BigDecimal(5000),
                                                              tilbakekrevesBeløp = BigDecimal(5000),
                                                              skatteprosent = BigDecimal(10)))

        val kravgrunnlag = lagKravgrunnlag(perioder, månedligSkattBeløp, kravgrunnlagsbeløpene)
        kravgrunnlagRepository.insert(kravgrunnlag)

        lagAktsomhetVilkårsvurdering(perioder = listOf(Periode(YearMonth.of(2021, 1),
                                                               YearMonth.of(2021, 2))),
                                     aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
                                     andelTilbakreves = BigDecimal(50), særligeGrunnerTilReduksjon = true)

        var tilbakekrevingsperioder = vedtakBeregningService.beregnVedtaksperioder(behandling.id, kravgrunnlag)
        assertNotNull(tilbakekrevingsperioder)
        assertEquals(2, tilbakekrevingsperioder.size)
        tilbakekrevingsperioder = tilbakekrevingsperioder.sortedBy { it.periode.fom }

        val førstePeriode = tilbakekrevingsperioder[0]
        assertEquals(Periode(YearMonth.of(2021, 1), YearMonth.of(2021, 1)), førstePeriode.periode)
        assertEquals(BigDecimal.ZERO, førstePeriode.renter)
        var feilPostering = førstePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING)
        var ytelsePostering = førstePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(beløp = ytelsePostering,
                    utbetaltBeløp = BigDecimal(5000),
                    tilbakekrevesBeløp = BigDecimal(2500),
                    uinnkrevdBeløp = BigDecimal(2500),
                    skattBeløp = BigDecimal(250),
                    kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING)

        val andrePeriode = tilbakekrevingsperioder[1]
        assertEquals(Periode(YearMonth.of(2021, 2), YearMonth.of(2021, 2)), andrePeriode.periode)
        assertEquals(BigDecimal.ZERO, andrePeriode.renter)
        feilPostering = andrePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING)
        ytelsePostering = andrePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(beløp = ytelsePostering,
                    utbetaltBeløp = BigDecimal(5000),
                    tilbakekrevesBeløp = BigDecimal(2500),
                    uinnkrevdBeløp = BigDecimal(2500),
                    skattBeløp = BigDecimal(250),
                    kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING)

    }

    @Test
    fun `beregnVedtaksperioder skal beregne når vilkårsvurderte med 50 prosent andeltilbakekrevesbeløp,med skatt avrunding`() {
        val månedligSkattBeløp = BigDecimal(1291)

        kravgrunnlagRepository.deleteById(kravgrunnlag.id)
        val kravgrunnlagsbeløpene = listOf(Kravgrunnlagsbeløp(klassetype = Klassetype.FEIL,
                                                              nyttBeløp = BigDecimal(5000),
                                                              skatteprosent = BigDecimal(25.82)),
                                           Kravgrunnlagsbeløp(klassetype = Klassetype.YTEL,
                                                              opprinneligUtbetalingsbeløp = BigDecimal(5000),
                                                              tilbakekrevesBeløp = BigDecimal(5000),
                                                              skatteprosent = BigDecimal(25.82)))

        val kravgrunnlag = lagKravgrunnlag(perioder, månedligSkattBeløp, kravgrunnlagsbeløpene)
        kravgrunnlagRepository.insert(kravgrunnlag)

        lagAktsomhetVilkårsvurdering(perioder = listOf(Periode(YearMonth.of(2021, 1),
                                                               YearMonth.of(2021, 2))),
                                     aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
                                     andelTilbakreves = BigDecimal(50), særligeGrunnerTilReduksjon = true)

        var tilbakekrevingsperioder = vedtakBeregningService.beregnVedtaksperioder(behandling.id, kravgrunnlag)
        assertNotNull(tilbakekrevingsperioder)
        assertEquals(2, tilbakekrevingsperioder.size)
        tilbakekrevingsperioder = tilbakekrevingsperioder.sortedBy { it.periode.fom }

        val førstePeriode = tilbakekrevingsperioder[0]
        assertEquals(Periode(YearMonth.of(2021, 1), YearMonth.of(2021, 1)), førstePeriode.periode)
        assertEquals(BigDecimal.ZERO, førstePeriode.renter)
        var feilPostering = førstePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING)
        var ytelsePostering = førstePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(beløp = ytelsePostering,
                    utbetaltBeløp = BigDecimal(5000),
                    tilbakekrevesBeløp = BigDecimal(2500),
                    uinnkrevdBeløp = BigDecimal(2500),
                    skattBeløp = BigDecimal(646),
                    kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING)

        val andrePeriode = tilbakekrevingsperioder[1]
        assertEquals(Periode(YearMonth.of(2021, 2), YearMonth.of(2021, 2)), andrePeriode.periode)
        assertEquals(BigDecimal.ZERO, andrePeriode.renter)
        feilPostering = andrePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING)
        ytelsePostering = andrePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(beløp = ytelsePostering,
                    utbetaltBeløp = BigDecimal(5000),
                    tilbakekrevesBeløp = BigDecimal(2500),
                    uinnkrevdBeløp = BigDecimal(2500),
                    skattBeløp = BigDecimal(645),
                    kodeResultat = KodeResultat.DELVIS_TILBAKEKREVING)

    }

    @Test
    fun `beregnVedtaksperioder skal beregne med en foreldet periode,en vilkårsvurdert periode med 100 prosent tilbakekreving`() {
        val foreldelsesdata = BehandlingsstegForeldelseDto(listOf(ForeldelsesperiodeDto(periode = PeriodeDto(perioder[0]),
                                                                                        begrunnelse = "testverdi",
                                                                                        foreldelsesvurderingstype =
                                                                                        Foreldelsesvurderingstype.FORELDET)))
        foreldelsesService.lagreVurdertForeldelse(behandling.id, foreldelsesdata)

        lagAktsomhetVilkårsvurdering(listOf(perioder[1]), Aktsomhet.GROV_UAKTSOMHET)

        var tilbakekrevingsperioder = vedtakBeregningService.beregnVedtaksperioder(behandling.id, kravgrunnlag)
        assertNotNull(tilbakekrevingsperioder)
        assertEquals(2, tilbakekrevingsperioder.size)
        tilbakekrevingsperioder = tilbakekrevingsperioder.sortedBy { it.periode.fom }

        val førstePeriode = tilbakekrevingsperioder[0]
        assertEquals(Periode(YearMonth.of(2021, 1), YearMonth.of(2021, 1)), førstePeriode.periode)
        assertEquals(BigDecimal.ZERO, førstePeriode.renter)
        var feilPostering = førstePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.FORELDET)
        var ytelsePostering = førstePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(beløp = ytelsePostering,
                    utbetaltBeløp = BigDecimal(5000),
                    tilbakekrevesBeløp = BigDecimal(0),
                    uinnkrevdBeløp = BigDecimal(5000),
                    skattBeløp = BigDecimal(0),
                    kodeResultat = KodeResultat.FORELDET)

        val andrePeriode = tilbakekrevingsperioder[1]
        assertEquals(Periode(YearMonth.of(2021, 2), YearMonth.of(2021, 2)), andrePeriode.periode)
        assertEquals(BigDecimal.ZERO, andrePeriode.renter)
        feilPostering = andrePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.FULL_TILBAKEKREVING)
        ytelsePostering = andrePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(beløp = ytelsePostering,
                    utbetaltBeløp = BigDecimal(5000),
                    tilbakekrevesBeløp = BigDecimal(5000),
                    uinnkrevdBeløp = BigDecimal(0),
                    skattBeløp = BigDecimal(0),
                    kodeResultat = KodeResultat.FULL_TILBAKEKREVING)

    }

    @Test
    fun `beregnVedtaksperioder skal beregne med tre vilkårsvurdert periode med 100 prosent tilbakekreving`() {
        kravgrunnlagRepository.deleteById(kravgrunnlag.id)

        val månedligSkattBeløp = BigDecimal(750)
        val perioder = listOf(Periode(YearMonth.of(2021, 1), YearMonth.of(2021, 1)),
                              Periode(YearMonth.of(2021, 2), YearMonth.of(2021, 2)),
                              Periode(YearMonth.of(2021, 3), YearMonth.of(2021, 3)))

        val kravgrunnlagsbeløpene = listOf(Kravgrunnlagsbeløp(klassetype = Klassetype.FEIL,
                                                              nyttBeløp = BigDecimal(5000),
                                                              skatteprosent = BigDecimal(15)),
                                           Kravgrunnlagsbeløp(klassetype = Klassetype.YTEL,
                                                              opprinneligUtbetalingsbeløp = BigDecimal(5000),
                                                              tilbakekrevesBeløp = BigDecimal(5000),
                                                              skatteprosent = BigDecimal(15)))
        val kravgrunnlag = lagKravgrunnlag(perioder, månedligSkattBeløp, kravgrunnlagsbeløpene)
        kravgrunnlagRepository.insert(kravgrunnlag)

        //en beregnet periode med 100 prosent tilbakekreving
        lagAktsomhetVilkårsvurdering(listOf(Periode(YearMonth.of(2021, 1), YearMonth.of(2021, 3))), Aktsomhet.GROV_UAKTSOMHET)

        var tilbakekrevingsperioder = vedtakBeregningService.beregnVedtaksperioder(behandling.id, kravgrunnlag)
        assertNotNull(tilbakekrevingsperioder)
        assertEquals(3, tilbakekrevingsperioder.size)
        tilbakekrevingsperioder = tilbakekrevingsperioder.sortedBy { it.periode.fom }

        val førstePeriode = tilbakekrevingsperioder[0]
        assertEquals(Periode(YearMonth.of(2021, 1), YearMonth.of(2021, 1)), førstePeriode.periode)
        assertEquals(BigDecimal.ZERO, førstePeriode.renter)
        var feilPostering = førstePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.FULL_TILBAKEKREVING)
        var ytelsePostering = førstePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(beløp = ytelsePostering,
                    utbetaltBeløp = BigDecimal(5000),
                    tilbakekrevesBeløp = BigDecimal(5000),
                    uinnkrevdBeløp = BigDecimal(0),
                    skattBeløp = BigDecimal(750),
                    kodeResultat = KodeResultat.FULL_TILBAKEKREVING)

        val andrePeriode = tilbakekrevingsperioder[1]
        assertEquals(Periode(YearMonth.of(2021, 2), YearMonth.of(2021, 2)), andrePeriode.periode)
        assertEquals(BigDecimal.ZERO, andrePeriode.renter)
        feilPostering = andrePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.FULL_TILBAKEKREVING)
        ytelsePostering = andrePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(beløp = ytelsePostering,
                    utbetaltBeløp = BigDecimal(5000),
                    tilbakekrevesBeløp = BigDecimal(5000),
                    uinnkrevdBeløp = BigDecimal(0),
                    skattBeløp = BigDecimal(750),
                    kodeResultat = KodeResultat.FULL_TILBAKEKREVING)

        val tredjePeriode = tilbakekrevingsperioder[2]
        assertEquals(Periode(YearMonth.of(2021, 3), YearMonth.of(2021, 3)), tredjePeriode.periode)
        assertEquals(BigDecimal.ZERO, tredjePeriode.renter)
        feilPostering = tredjePeriode.beløp.first { Klassetype.FEIL == it.klassetype }
        assertBeløp(beløp = feilPostering, nyttBeløp = BigDecimal(5000), kodeResultat = KodeResultat.FULL_TILBAKEKREVING)
        ytelsePostering = tredjePeriode.beløp.first { Klassetype.YTEL == it.klassetype }
        assertBeløp(beløp = ytelsePostering,
                    utbetaltBeløp = BigDecimal(5000),
                    tilbakekrevesBeløp = BigDecimal(5000),
                    uinnkrevdBeløp = BigDecimal(0),
                    skattBeløp = BigDecimal(750),
                    kodeResultat = KodeResultat.FULL_TILBAKEKREVING)

    }

    private fun lagAktsomhetVilkårsvurdering(perioder: List<Periode>,
                                             aktsomhet: Aktsomhet,
                                             andelTilbakreves: BigDecimal? = null,
                                             beløpTilbakekreves: BigDecimal? = null,
                                             særligeGrunnerTilReduksjon: Boolean = false) {
        val vilkårsperioder = perioder.map {
            VilkårsvurderingsperiodeDto(periode = PeriodeDto(it),
                                        begrunnelse = "testverdi",
                                        aktsomhetDto = AktsomhetDto(
                                                aktsomhet = aktsomhet,
                                                andelTilbakekreves = andelTilbakreves,
                                                beløpTilbakekreves = beløpTilbakekreves,
                                                begrunnelse = "testverdi",
                                                særligeGrunnerTilReduksjon = særligeGrunnerTilReduksjon,
                                                tilbakekrevSmåbeløp = true,
                                                særligeGrunnerBegrunnelse = "testverdi",
                                                særligeGrunner = listOf(SærligGrunnDto(særligGrunn = SærligGrunn.ANNET,
                                                                                       begrunnelse = "testverdi"))),
                                        vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT)
        }
        vilkårsvurderingService.lagreVilkårsvurdering(behandling.id, BehandlingsstegVilkårsvurderingDto(vilkårsperioder))

    }

    private fun lagGodTroVilkårsvurdering(perioder: List<Periode>,
                                          beløpErIBehold: Boolean = false,
                                          beløpTilbakekreves: BigDecimal? = null) {
        val vilkårsperioder = perioder.map {
            VilkårsvurderingsperiodeDto(periode = PeriodeDto(it),
                                        begrunnelse = "testverdi",
                                        godTroDto = GodTroDto(
                                                begrunnelse = "testverdi",
                                                beløpErIBehold = beløpErIBehold,
                                                beløpTilbakekreves = beløpTilbakekreves),
                                        vilkårsvurderingsresultat = Vilkårsvurderingsresultat.GOD_TRO)
        }
        vilkårsvurderingService.lagreVilkårsvurdering(behandling.id, BehandlingsstegVilkårsvurderingDto(vilkårsperioder))
    }

    private fun lagKravgrunnlag(perioder: List<Periode>,
                                månedligSkattBeløp: BigDecimal,
                                kravgrunnlagsbeløpene: List<Kravgrunnlagsbeløp>): Kravgrunnlag431 {
        return Kravgrunnlag431(
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
                perioder = lagKravgrunnlagsperiode(perioder, månedligSkattBeløp, kravgrunnlagsbeløpene))
    }

    private fun lagKravgrunnlagsperiode(perioder: List<Periode>,
                                        månedligSkattBeløp: BigDecimal,
                                        kravgrunnlagsbeløpene: List<Kravgrunnlagsbeløp>): Set<Kravgrunnlagsperiode432> {
        return perioder.map {
            Kravgrunnlagsperiode432(periode = it,
                                    månedligSkattebeløp = månedligSkattBeløp,
                                    beløp = lagKravgrunnlagsbeløp(kravgrunnlagsbeløpene))
        }.toSet()
    }

    private fun lagKravgrunnlagsbeløp(kravgrunnlagsbeløpene: List<Kravgrunnlagsbeløp>): Set<Kravgrunnlagsbeløp433> {
        return kravgrunnlagsbeløpene.map {
            Kravgrunnlagsbeløp433(klassekode = Klassekode.BATR,
                                  klassetype = it.klassetype,
                                  opprinneligUtbetalingsbeløp = it.opprinneligUtbetalingsbeløp,
                                  nyttBeløp = it.nyttBeløp,
                                  tilbakekrevesBeløp = it.tilbakekrevesBeløp,
                                  uinnkrevdBeløp = it.uinnkrevdBeløp,
                                  skatteprosent = it.skatteprosent)
        }.toSet()
    }

    private fun assertBeløp(beløp: Tilbakekrevingsbeløp,
                            nyttBeløp: BigDecimal = BigDecimal.ZERO,
                            utbetaltBeløp: BigDecimal = BigDecimal.ZERO,
                            tilbakekrevesBeløp: BigDecimal = BigDecimal.ZERO,
                            uinnkrevdBeløp: BigDecimal = BigDecimal.ZERO,
                            skattBeløp: BigDecimal = BigDecimal.ZERO,
                            kodeResultat: KodeResultat) {
        assertEquals(nyttBeløp, beløp.nyttBeløp)
        assertEquals(utbetaltBeløp, beløp.utbetaltBeløp)
        assertEquals(utbetaltBeløp, beløp.utbetaltBeløp)
        assertEquals(tilbakekrevesBeløp, beløp.tilbakekrevesBeløp)
        assertEquals(uinnkrevdBeløp, beløp.uinnkrevdBeløp)
        assertEquals(skattBeløp, beløp.skattBeløp)
        assertEquals(kodeResultat, beløp.kodeResultat)
    }

    internal data class Kravgrunnlagsbeløp(val klassetype: Klassetype,
                                           val opprinneligUtbetalingsbeløp: BigDecimal = BigDecimal.ZERO,
                                           val nyttBeløp: BigDecimal = BigDecimal.ZERO,
                                           val tilbakekrevesBeløp: BigDecimal = BigDecimal.ZERO,
                                           val uinnkrevdBeløp: BigDecimal = BigDecimal.ZERO,
                                           val skatteprosent: BigDecimal = BigDecimal.ZERO)

}
