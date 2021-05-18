package no.nav.familie.tilbake.vilkårsvurdering

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.api.dto.AktivitetDto
import no.nav.familie.tilbake.api.dto.AktsomhetDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegVilkårsvurderingDto
import no.nav.familie.tilbake.api.dto.GodTroDto
import no.nav.familie.tilbake.api.dto.PeriodeDto
import no.nav.familie.tilbake.api.dto.SærligGrunnDto
import no.nav.familie.tilbake.api.dto.VilkårsvurderingsperiodeDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingRepository
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetaling
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetalingsperiode
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsestype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsesundertype
import no.nav.familie.tilbake.foreldelse.VurdertForeldelseRepository
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesperiode
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesvurderingstype
import no.nav.familie.tilbake.foreldelse.domain.VurdertForeldelse
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassekode
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsbeløp433
import no.nav.familie.tilbake.vilkårsvurdering.domain.Aktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.SærligGrunn
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsresultat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class VilkårsvurderingServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var faktaFeilutbetalingRepository: FaktaFeilutbetalingRepository

    @Autowired
    private lateinit var foreldelseRepository: VurdertForeldelseRepository

    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @Autowired
    private lateinit var vilkårsvurderingService: VilkårsvurderingService

    private val behandling = Testdata.behandling

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(behandling)
        val førstePeriode = Testdata.kravgrunnlagsperiode432
                .copy(id = UUID.randomUUID(),
                      periode = Periode(fom = YearMonth.of(2020, 1),
                                        tom = YearMonth.of(2020, 1)),
                      beløp = setOf(Testdata.feilKravgrunnlagsbeløp433.copy(id = UUID.randomUUID()),
                                    Testdata.ytelKravgrunnlagsbeløp433.copy(id = UUID.randomUUID())))
        val andrePeriode = Testdata.kravgrunnlagsperiode432
                .copy(id = UUID.randomUUID(),
                      periode = Periode(fom = YearMonth.of(2020, 2),
                                        tom = YearMonth.of(2020, 2)),
                      beløp = setOf(Testdata.feilKravgrunnlagsbeløp433.copy(id = UUID.randomUUID()),
                                    Testdata.ytelKravgrunnlagsbeløp433.copy(id = UUID.randomUUID())))

        val kravgrunnlag431 = Testdata.kravgrunnlag431.copy(perioder = setOf(førstePeriode, andrePeriode))
        kravgrunnlagRepository.insert(kravgrunnlag431)

        val periode = FaktaFeilutbetalingsperiode(periode = Periode(førstePeriode.periode.fom,
                                                                    andrePeriode.periode.tom),
                                                  hendelsestype = Hendelsestype.BA_ANNET,
                                                  hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST)
        faktaFeilutbetalingRepository.insert(FaktaFeilutbetaling(behandlingId = behandling.id,
                                                                 begrunnelse = "fakta begrunnelse",
                                                                 perioder = setOf(periode)))

        lagBehandlingsstegstilstand(Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
    }

    @Test
    fun `hentVilkårsvurdering skal hente vilkårsvurdering fra fakta perioder`() {
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.KLAR)

        val vurdertVilkårsvurderingDto = vilkårsvurderingService.hentVilkårsvurdering(behandling.id)
        assertEquals(Constants.rettsgebyr, vurdertVilkårsvurderingDto.rettsgebyr)
        assertTrue { vurdertVilkårsvurderingDto.perioder.isNotEmpty() }
        assertEquals(1, vurdertVilkårsvurderingDto.perioder.size)
        val vurdertPeriode = vurdertVilkårsvurderingDto.perioder[0]
        assertEquals(PeriodeDto(LocalDate.of(2020, 1, 1),
                                LocalDate.of(2020, 2, 29)), vurdertPeriode.periode)
        assertEquals(Hendelsestype.BA_ANNET, vurdertPeriode.hendelsestype)
        assertEquals(BigDecimal("20000"), vurdertPeriode.feilutbetaltBeløp)
        assertTrue { vurdertPeriode.reduserteBeløper.isEmpty() }
        assertAktiviteter(vurdertPeriode.aktiviteter)
        assertEquals(BigDecimal(20000), vurdertPeriode.aktiviteter[0].beløp)
        assertFalse { vurdertPeriode.foreldet }
        assertNull(vurdertPeriode.begrunnelse)
        assertNull(vurdertPeriode.vilkårsvurderingsresultatInfo)
    }

    @Test
    fun `hentVilkårsvurdering skal hente vilkårsvurdering fra foreldelse perioder som ikke er foreldet`() {
        lagForeldese(Foreldelsesvurderingstype.FORELDET, Foreldelsesvurderingstype.IKKE_FORELDET)
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.KLAR)

        val vurdertVilkårsvurderingDto = vilkårsvurderingService.hentVilkårsvurdering(behandling.id)
        assertEquals(Constants.rettsgebyr, vurdertVilkårsvurderingDto.rettsgebyr)
        assertTrue { vurdertVilkårsvurderingDto.perioder.isNotEmpty() }
        assertEquals(2, vurdertVilkårsvurderingDto.perioder.size)

        val foreldetPeriode = vurdertVilkårsvurderingDto.perioder[0]
        assertEquals(PeriodeDto(LocalDate.of(2020, 1, 1),
                                LocalDate.of(2020, 1, 31)), foreldetPeriode.periode)
        assertEquals(Hendelsestype.BA_ANNET, foreldetPeriode.hendelsestype)
        assertEquals(BigDecimal("10000"), foreldetPeriode.feilutbetaltBeløp)
        assertTrue { foreldetPeriode.foreldet }
        assertTrue { foreldetPeriode.reduserteBeløper.isEmpty() }
        assertAktiviteter(foreldetPeriode.aktiviteter)
        assertEquals(BigDecimal(10000), foreldetPeriode.aktiviteter[0].beløp)
        assertEquals("foreldelse begrunnelse 1", foreldetPeriode.begrunnelse)
        assertNull(foreldetPeriode.vilkårsvurderingsresultatInfo)

        val ikkeForeldetPeriode = vurdertVilkårsvurderingDto.perioder[1]
        assertEquals(PeriodeDto(LocalDate.of(2020, 2, 1),
                                LocalDate.of(2020, 2, 29)), ikkeForeldetPeriode.periode)
        assertEquals(Hendelsestype.BA_ANNET, ikkeForeldetPeriode.hendelsestype)
        assertFalse { ikkeForeldetPeriode.foreldet }
        assertEquals(BigDecimal("10000"), ikkeForeldetPeriode.feilutbetaltBeløp)
        assertEquals(BigDecimal(10000), ikkeForeldetPeriode.aktiviteter[0].beløp)
        assertTrue { ikkeForeldetPeriode.reduserteBeløper.isEmpty() }
        assertAktiviteter(ikkeForeldetPeriode.aktiviteter)
        assertNull(ikkeForeldetPeriode.begrunnelse)
        assertNull(ikkeForeldetPeriode.vilkårsvurderingsresultatInfo)
    }

    @Test
    fun `hentVilkårsvurdering skal hente vilkårsvurdering når perioder er delt opp`() {
        //delt opp i to perioder
        val periode1 = PeriodeDto(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31))
        val periode2 = PeriodeDto(LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 29))
        val behandlingsstegVilkårsvurderingDto = lagVilkårsvurderingMedGodTro(perioder = listOf(periode1, periode2))
        vilkårsvurderingService.lagreVilkårsvurdering(behandling.id, behandlingsstegVilkårsvurderingDto)

        val vurdertVilkårsvurderingDto = vilkårsvurderingService.hentVilkårsvurdering(behandling.id)
        assertEquals(Constants.rettsgebyr, vurdertVilkårsvurderingDto.rettsgebyr)
        assertTrue { vurdertVilkårsvurderingDto.perioder.isNotEmpty() }
        assertEquals(2, vurdertVilkårsvurderingDto.perioder.size)

        val førstePeriode = vurdertVilkårsvurderingDto.perioder[0]
        assertEquals(periode1, førstePeriode.periode)
        assertEquals(Hendelsestype.BA_ANNET, førstePeriode.hendelsestype)
        assertEquals(BigDecimal(10000), førstePeriode.feilutbetaltBeløp)
        assertFalse { førstePeriode.foreldet }
        assertAktiviteter(førstePeriode.aktiviteter)
        assertEquals(BigDecimal(10000), førstePeriode.aktiviteter[0].beløp)
        var vilkårsvurderingsresultatDto = førstePeriode.vilkårsvurderingsresultatInfo
        assertNotNull(vilkårsvurderingsresultatDto)
        assertEquals(Vilkårsvurderingsresultat.GOD_TRO, vilkårsvurderingsresultatDto.vilkårsvurderingsresultat)

        val andrePeriode = vurdertVilkårsvurderingDto.perioder[1]
        assertEquals(periode2, andrePeriode.periode)
        assertEquals(Hendelsestype.BA_ANNET, andrePeriode.hendelsestype)
        assertEquals(BigDecimal(10000), andrePeriode.feilutbetaltBeløp)
        assertFalse { andrePeriode.foreldet }
        assertAktiviteter(andrePeriode.aktiviteter)
        assertEquals(BigDecimal(10000), andrePeriode.aktiviteter[0].beløp)
        vilkårsvurderingsresultatDto = andrePeriode.vilkårsvurderingsresultatInfo
        assertNotNull(vilkårsvurderingsresultatDto)
        assertEquals(Vilkårsvurderingsresultat.GOD_TRO, vilkårsvurderingsresultatDto.vilkårsvurderingsresultat)
    }

    @Test
    fun `hentVilkårsvurdering skal hente vilkårsvurdering med reduserte beløper`() {
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.KLAR)

        val kravgrunnlag431 = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
        val justBeløp = lagKravgrunnlagsbeløp(klassetype = Klassetype.JUST,
                                              nyttBeløp = BigDecimal(5000),
                                              opprinneligUtbetalingsbeløp = BigDecimal.ZERO)
        val trekBeløp = lagKravgrunnlagsbeløp(klassetype = Klassetype.TREK,
                                              nyttBeløp = BigDecimal.ZERO,
                                              opprinneligUtbetalingsbeløp = BigDecimal(-2000))
        val skatBeløp = lagKravgrunnlagsbeløp(klassetype = Klassetype.SKAT,
                                              nyttBeløp = BigDecimal.ZERO,
                                              opprinneligUtbetalingsbeløp = BigDecimal(-2000))
        val førstePeriode = kravgrunnlag431.perioder
                .toList()[0]
                .copy(beløp = setOf(Testdata.feilKravgrunnlagsbeløp433.copy(id = UUID.randomUUID()),
                                    Testdata.ytelKravgrunnlagsbeløp433.copy(id = UUID.randomUUID()),
                                    justBeløp))
        val andrePeriode = kravgrunnlag431.perioder
                .toList()[1]
                .copy(beløp = setOf(Testdata.feilKravgrunnlagsbeløp433.copy(id = UUID.randomUUID()),
                                    Testdata.ytelKravgrunnlagsbeløp433.copy(id = UUID.randomUUID()),
                                    trekBeløp,
                                    skatBeløp))
        kravgrunnlagRepository.update(kravgrunnlag431.copy(perioder = setOf(førstePeriode, andrePeriode)))

        val vurdertVilkårsvurderingDto = vilkårsvurderingService.hentVilkårsvurdering(behandling.id)
        assertEquals(Constants.rettsgebyr, vurdertVilkårsvurderingDto.rettsgebyr)
        assertTrue { vurdertVilkårsvurderingDto.perioder.isNotEmpty() }
        assertEquals(1, vurdertVilkårsvurderingDto.perioder.size)
        val vurdertPeriode = vurdertVilkårsvurderingDto.perioder[0]
        assertEquals(PeriodeDto(LocalDate.of(2020, 1, 1),
                                LocalDate.of(2020, 2, 29)), vurdertPeriode.periode)
        assertEquals(Hendelsestype.BA_ANNET, vurdertPeriode.hendelsestype)
        assertEquals(BigDecimal("20000"), vurdertPeriode.feilutbetaltBeløp)
        assertAktiviteter(vurdertPeriode.aktiviteter)
        assertEquals(BigDecimal(20000), vurdertPeriode.aktiviteter[0].beløp)
        assertFalse { vurdertPeriode.foreldet }

        assertTrue { vurdertPeriode.reduserteBeløper.isNotEmpty() }
        assertEquals(3, vurdertPeriode.reduserteBeløper.size)
        var redusertBeløp = vurdertPeriode.reduserteBeløper[0]
        assertTrue { redusertBeløp.trekk }
        assertEquals(BigDecimal("2000.00"), redusertBeløp.beløp)
        redusertBeløp = vurdertPeriode.reduserteBeløper[1]
        assertTrue { redusertBeløp.trekk }
        assertEquals(BigDecimal("2000.00"), redusertBeløp.beløp)
        redusertBeløp = vurdertPeriode.reduserteBeløper[2]
        assertFalse { redusertBeløp.trekk }
        assertEquals(BigDecimal("5000.00"), redusertBeløp.beløp)

        assertNull(vurdertPeriode.vilkårsvurderingsresultatInfo)
        assertNull(vurdertPeriode.begrunnelse)
    }

    @Test
    fun `hentVilkårsvurdering skal hente allerede lagret simpel aktsomhet vilkårsvurdering`() {
        val behandlingsstegVilkårsvurderingDto =
                lagVilkårsvurderingMedSimpelAktsomhet(særligGrunn = SærligGrunnDto(SærligGrunn.GRAD_AV_UAKTSOMHET))
        vilkårsvurderingService.lagreVilkårsvurdering(behandlingId = behandling.id,
                                                      behandlingsstegVilkårsvurderingDto = behandlingsstegVilkårsvurderingDto)

        val vurdertVilkårsvurderingDto = vilkårsvurderingService.hentVilkårsvurdering(behandling.id)
        assertEquals(Constants.rettsgebyr, vurdertVilkårsvurderingDto.rettsgebyr)
        assertTrue { vurdertVilkårsvurderingDto.perioder.isNotEmpty() }
        assertEquals(1, vurdertVilkårsvurderingDto.perioder.size)
        val vurdertPeriode = vurdertVilkårsvurderingDto.perioder[0]
        assertEquals(PeriodeDto(LocalDate.of(2020, 1, 1),
                                LocalDate.of(2020, 2, 29)), vurdertPeriode.periode)
        assertEquals(Hendelsestype.BA_ANNET, vurdertPeriode.hendelsestype)
        assertEquals(BigDecimal("20000"), vurdertPeriode.feilutbetaltBeløp)
        assertAktiviteter(vurdertPeriode.aktiviteter)
        assertEquals(BigDecimal(20000), vurdertPeriode.aktiviteter[0].beløp)
        assertFalse { vurdertPeriode.foreldet }
        assertEquals("Vilkårsvurdering begrunnelse", vurdertPeriode.begrunnelse)

        val vilkårsvurderingsresultatDto = vurdertPeriode.vilkårsvurderingsresultatInfo
        assertNotNull(vilkårsvurderingsresultatDto)
        assertNull(vilkårsvurderingsresultatDto.godTro)
        assertEquals(Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT, vilkårsvurderingsresultatDto.vilkårsvurderingsresultat)
        val aktsomhetDto = vilkårsvurderingsresultatDto.aktsomhet
        assertNotNull(aktsomhetDto)
        assertEquals(Aktsomhet.SIMPEL_UAKTSOMHET, aktsomhetDto.aktsomhet)
        assertEquals("Aktsomhet begrunnelse", aktsomhetDto.begrunnelse)
        assertFalse { aktsomhetDto.tilbakekrevSmåbeløp }
        assertFalse { aktsomhetDto.særligeGrunnerTilReduksjon }
        assertNull(aktsomhetDto.andelTilbakekreves)
        assertNull(aktsomhetDto.ileggRenter)
        assertNull(aktsomhetDto.beløpTilbakekreves)
        assertEquals("Særlig grunner begrunnelse", aktsomhetDto.særligeGrunnerBegrunnelse)
        val særligGrunner = aktsomhetDto.særligeGrunner
        assertNotNull(særligGrunner)
        assertTrue { særligGrunner.any { SærligGrunn.GRAD_AV_UAKTSOMHET == it.særligGrunn } }
        assertTrue { særligGrunner.all { it.begrunnelse == null } }
    }

    @Test
    fun `hentVilkårsvurdering skal hente allerede lagret god tro vilkårsvurdering`() {
        val behandlingsstegVilkårsvurderingDto =
                lagVilkårsvurderingMedGodTro(perioder = listOf(PeriodeDto(YearMonth.of(2020, 1), YearMonth.of(2020, 2))))
        vilkårsvurderingService.lagreVilkårsvurdering(behandlingId = behandling.id,
                                                      behandlingsstegVilkårsvurderingDto = behandlingsstegVilkårsvurderingDto)

        val vurdertVilkårsvurderingDto = vilkårsvurderingService.hentVilkårsvurdering(behandling.id)
        assertEquals(Constants.rettsgebyr, vurdertVilkårsvurderingDto.rettsgebyr)
        assertTrue { vurdertVilkårsvurderingDto.perioder.isNotEmpty() }
        assertEquals(1, vurdertVilkårsvurderingDto.perioder.size)
        val vurdertPeriode = vurdertVilkårsvurderingDto.perioder[0]
        assertEquals(PeriodeDto(LocalDate.of(2020, 1, 1),
                                LocalDate.of(2020, 2, 29)), vurdertPeriode.periode)
        assertEquals(Hendelsestype.BA_ANNET, vurdertPeriode.hendelsestype)
        assertEquals(BigDecimal("20000"), vurdertPeriode.feilutbetaltBeløp)
        assertAktiviteter(vurdertPeriode.aktiviteter)
        assertEquals(BigDecimal(20000), vurdertPeriode.aktiviteter[0].beløp)
        assertFalse { vurdertPeriode.foreldet }
        assertEquals("Vilkårsvurdering begrunnelse", vurdertPeriode.begrunnelse)

        val vilkårsvurderingsresultatDto = vurdertPeriode.vilkårsvurderingsresultatInfo
        assertNotNull(vilkårsvurderingsresultatDto)
        assertNull(vilkårsvurderingsresultatDto.aktsomhet)
        assertEquals(Vilkårsvurderingsresultat.GOD_TRO, vilkårsvurderingsresultatDto.vilkårsvurderingsresultat)
        val godTroDto = vilkårsvurderingsresultatDto.godTro
        assertNotNull(godTroDto)
        assertTrue { godTroDto.beløpErIBehold }
        assertEquals("God tro begrunnelse", godTroDto.begrunnelse)
        assertNull(godTroDto.beløpTilbakekreves)
    }

    @Test
    fun `hentVilkårsvurdering skal hente foreldelse perioder som endret til IKKE_FORELDET`() {
        // en periode med FORELDET og andre er IKKE_FORELDET
        lagForeldese(Foreldelsesvurderingstype.FORELDET, Foreldelsesvurderingstype.IKKE_FORELDET)
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.KLAR)

        var vurdertVilkårsvurderingDto = vilkårsvurderingService.hentVilkårsvurdering(behandling.id)
        assertTrue { vurdertVilkårsvurderingDto.perioder.isNotEmpty() }
        assertEquals(2, vurdertVilkårsvurderingDto.perioder.size)
        assertEquals(1, vurdertVilkårsvurderingDto.perioder.count { it.foreldet })
        assertEquals(1, vurdertVilkårsvurderingDto.perioder.count { !it.foreldet })

        // behandle vilkårsvurdering
        vilkårsvurderingService
                .lagreVilkårsvurdering(behandling.id,
                                       lagVilkårsvurderingMedGodTro(perioder = listOf(PeriodeDto(YearMonth.of(2020, 2),
                                                                                                 YearMonth.of(2020, 2)))))
        lagBehandlingsstegstilstand(Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.KLAR)

        // endret begge perioder til IKKE_FORELDET
        val vurdertForeldelse = foreldelseRepository.findByBehandlingIdAndAktivIsTrue(behandlingId = behandling.id)!!
        oppdaterForeldelsesvurdering(vurdertForeldelse, Foreldelsesvurderingstype.IKKE_FORELDET,
                                     Foreldelsesvurderingstype.IKKE_FORELDET)

        vurdertVilkårsvurderingDto = vilkårsvurderingService.hentVilkårsvurdering(behandling.id)
        assertTrue { vurdertVilkårsvurderingDto.perioder.isNotEmpty() }
        assertEquals(2, vurdertVilkårsvurderingDto.perioder.size)
        assertEquals(2, vurdertVilkårsvurderingDto.perioder.count { !it.foreldet })

        val ikkeVurdertPeriode = vurdertVilkårsvurderingDto.perioder[0]
        assertEquals(PeriodeDto(LocalDate.of(2020, 1, 1),
                                LocalDate.of(2020, 1, 31)), ikkeVurdertPeriode.periode)
        assertEquals(Hendelsestype.BA_ANNET, ikkeVurdertPeriode.hendelsestype)
        assertEquals(BigDecimal("10000"), ikkeVurdertPeriode.feilutbetaltBeløp)
        assertTrue { ikkeVurdertPeriode.reduserteBeløper.isEmpty() }
        assertAktiviteter(ikkeVurdertPeriode.aktiviteter)
        assertEquals(BigDecimal(10000), ikkeVurdertPeriode.aktiviteter[0].beløp)
        assertNull(ikkeVurdertPeriode.vilkårsvurderingsresultatInfo)
        assertNull(ikkeVurdertPeriode.begrunnelse)

        val vurdertPeriode = vurdertVilkårsvurderingDto.perioder[1]
        assertEquals(PeriodeDto(LocalDate.of(2020, 2, 1),
                                LocalDate.of(2020, 2, 29)), vurdertPeriode.periode)
        assertEquals(Hendelsestype.BA_ANNET, vurdertPeriode.hendelsestype)
        assertEquals(BigDecimal("10000"), vurdertPeriode.feilutbetaltBeløp)
        assertAktiviteter(vurdertPeriode.aktiviteter)
        assertEquals(BigDecimal(10000), vurdertPeriode.aktiviteter[0].beløp)
        assertEquals("Vilkårsvurdering begrunnelse", vurdertPeriode.begrunnelse)

        val vilkårsvurderingsresultatDto = vurdertPeriode.vilkårsvurderingsresultatInfo
        assertNotNull(vilkårsvurderingsresultatDto)
        assertNull(vilkårsvurderingsresultatDto.aktsomhet)
        assertEquals(Vilkårsvurderingsresultat.GOD_TRO, vilkårsvurderingsresultatDto.vilkårsvurderingsresultat)
        val godTroDto = vilkårsvurderingsresultatDto.godTro
        assertNotNull(godTroDto)
        assertTrue { godTroDto.beløpErIBehold }
        assertEquals("God tro begrunnelse", godTroDto.begrunnelse)
        assertNull(godTroDto.beløpTilbakekreves)
    }

    @Test
    fun `hentVilkårsvurdering skal hente perioder som endret til FORELDET`() {
        // en periode med FORELDET og andre er IKKE_FORELDET
        lagForeldese(Foreldelsesvurderingstype.FORELDET, Foreldelsesvurderingstype.IKKE_FORELDET)
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.KLAR)

        var vurdertVilkårsvurderingDto = vilkårsvurderingService.hentVilkårsvurdering(behandling.id)
        assertTrue { vurdertVilkårsvurderingDto.perioder.isNotEmpty() }
        assertEquals(2, vurdertVilkårsvurderingDto.perioder.size)
        assertEquals(1, vurdertVilkårsvurderingDto.perioder.count { it.foreldet })
        assertEquals(1, vurdertVilkårsvurderingDto.perioder.count { !it.foreldet })

        // behandle vilkårsvurdering
        vilkårsvurderingService
                .lagreVilkårsvurdering(behandling.id,
                                       lagVilkårsvurderingMedGodTro(perioder = listOf(PeriodeDto(YearMonth.of(2020, 2),
                                                                                                 YearMonth.of(2020, 2)))))
        lagBehandlingsstegstilstand(Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.KLAR)

        // endret begge perioder til FORELDET
        val vurdertForeldelse = foreldelseRepository.findByBehandlingIdAndAktivIsTrue(behandlingId = behandling.id)!!
        oppdaterForeldelsesvurdering(vurdertForeldelse, Foreldelsesvurderingstype.FORELDET,
                                     Foreldelsesvurderingstype.FORELDET)

        vurdertVilkårsvurderingDto = vilkårsvurderingService.hentVilkårsvurdering(behandling.id)
        assertTrue { vurdertVilkårsvurderingDto.perioder.isNotEmpty() }
        assertEquals(2, vurdertVilkårsvurderingDto.perioder.size)
        assertEquals(2, vurdertVilkårsvurderingDto.perioder.count { it.foreldet })

        val førsteForeldetPeriode = vurdertVilkårsvurderingDto.perioder[0]
        assertEquals(PeriodeDto(LocalDate.of(2020, 1, 1),
                                LocalDate.of(2020, 1, 31)), førsteForeldetPeriode.periode)
        assertEquals(Hendelsestype.BA_ANNET, førsteForeldetPeriode.hendelsestype)
        assertEquals(BigDecimal("10000"), førsteForeldetPeriode.feilutbetaltBeløp)
        assertTrue { førsteForeldetPeriode.reduserteBeløper.isEmpty() }
        assertAktiviteter(førsteForeldetPeriode.aktiviteter)
        assertEquals(BigDecimal(10000), førsteForeldetPeriode.aktiviteter[0].beløp)
        assertNull(førsteForeldetPeriode.vilkårsvurderingsresultatInfo)
        assertEquals("foreldelse begrunnelse 1", førsteForeldetPeriode.begrunnelse)

        val andreForeldetPeriode = vurdertVilkårsvurderingDto.perioder[1]
        assertEquals(PeriodeDto(LocalDate.of(2020, 2, 1),
                                LocalDate.of(2020, 2, 29)), andreForeldetPeriode.periode)
        assertEquals(Hendelsestype.BA_ANNET, andreForeldetPeriode.hendelsestype)
        assertEquals(BigDecimal("10000"), andreForeldetPeriode.feilutbetaltBeløp)
        assertTrue { andreForeldetPeriode.reduserteBeløper.isEmpty() }
        assertAktiviteter(andreForeldetPeriode.aktiviteter)
        assertEquals(BigDecimal(10000), andreForeldetPeriode.aktiviteter[0].beløp)
        assertNull(andreForeldetPeriode.vilkårsvurderingsresultatInfo)
        assertEquals("foreldelse begrunnelse 2", andreForeldetPeriode.begrunnelse)
    }


    @Test
    fun `lagreVilkårsvurdering skal ikke lagre vilkårsvurdering når andelTilbakekreves er mer enn 100 prosent `() {
        val exception = assertFailsWith<RuntimeException> {
            vilkårsvurderingService
                    .lagreVilkårsvurdering(behandling.id,
                                           lagVilkårsvurderingMedSimpelAktsomhet(andelTilbakekreves = BigDecimal(120),
                                                                                 særligGrunn =
                                                                                 SærligGrunnDto(SærligGrunn.GRAD_AV_UAKTSOMHET)))
        }
        assertEquals("Andel som skal tilbakekreves kan ikke være mer enn 100 prosent", exception.message)
    }

    @Test
    fun `lagreVilkårsvurdering skal ikke lagre vilkårsvurdering når ANNET særlig grunner mangler ANNET begrunnelse`() {
        val exception = assertFailsWith<RuntimeException> {
            vilkårsvurderingService
                    .lagreVilkårsvurdering(behandling.id,
                                           lagVilkårsvurderingMedSimpelAktsomhet(særligGrunn = SærligGrunnDto(SærligGrunn.ANNET)))
        }
        assertEquals("ANNET særlig grunner må ha ANNET begrunnelse", exception.message)
    }

    @Test
    fun `lagreVilkårsvurdering skal ikke lagre vilkårsvurdering når manueltSattBeløp er mer enn feilutbetalt beløp`() {
        //forutsetter at kravgrunnlag har 20000 som feilutbetalt beløp fra Testdata
        val behandlingsstegVilkårsvurderingDto =
                lagVilkårsvurderingMedSimpelAktsomhet(manueltSattBeløp = BigDecimal(30000),
                                                      særligGrunn = SærligGrunnDto(SærligGrunn.GRAD_AV_UAKTSOMHET))
        val exception = assertFailsWith<RuntimeException> {
            vilkårsvurderingService.lagreVilkårsvurdering(behandling.id,
                                                          behandlingsstegVilkårsvurderingDto)
        }
        assertEquals("Beløp som skal tilbakekreves kan ikke være mer enn feilutbetalt beløp", exception.message)
    }

    @Test
    fun `lagreVilkårsvurdering skal ikke lagre vilkårsvurdering når tilbakekrevesBeløp er mer enn feilutbetalt beløp`() {
        //forutsetter at kravgrunnlag har 20000 som feilutbetalt beløp fra Testdata
        val exception = assertFailsWith<RuntimeException> {
            vilkårsvurderingService.lagreVilkårsvurdering(behandling.id,
                                                          lagVilkårsvurderingMedGodTro(listOf(PeriodeDto(YearMonth.of(2020, 1),
                                                                                                         YearMonth.of(2020, 2))),
                                                                                       BigDecimal(30000)))
        }
        assertEquals("Beløp som skal tilbakekreves kan ikke være mer enn feilutbetalt beløp", exception.message)
    }

    @Test
    fun `lagreVilkårsvurdering skal lagre vilkårsvurdering med false ileggRenter for barnetrygd behandling`() {
        //forutsetter at behandling opprettet for barnetrygd fra Testdata
        vilkårsvurderingService
                .lagreVilkårsvurdering(behandling.id,
                                       lagVilkårsvurderingMedSimpelAktsomhet(ileggRenter = true,
                                                                             særligGrunn =
                                                                             SærligGrunnDto(SærligGrunn.GRAD_AV_UAKTSOMHET)))

        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
        assertNotNull(vilkårsvurdering)

        assertTrue { vilkårsvurdering.perioder.isNotEmpty() }
        assertEquals(1, vilkårsvurdering.perioder.size)
        val vurdertPeriode = vilkårsvurdering.perioder.toList()[0]
        assertEquals(Periode(YearMonth.of(2020, 1),
                             YearMonth.of(2020, 2)), vurdertPeriode.periode)
        assertEquals("Vilkårsvurdering begrunnelse", vurdertPeriode.begrunnelse)

        assertNotNull(vurdertPeriode.aktsomhet)
        assertNull(vurdertPeriode.godTro)
        assertEquals(Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT, vurdertPeriode.vilkårsvurderingsresultat)

        val aktsomhet = vurdertPeriode.aktsomhet
        assertNotNull(aktsomhet)
        assertEquals(Aktsomhet.SIMPEL_UAKTSOMHET, aktsomhet.aktsomhet)
        assertEquals("Aktsomhet begrunnelse", aktsomhet.begrunnelse)
        assertFalse { aktsomhet.tilbakekrevSmåbeløp }
        assertFalse { aktsomhet.særligeGrunnerTilReduksjon }
        assertNull(aktsomhet.andelTilbakekreves)
        assertTrue { aktsomhet.ileggRenter != null && aktsomhet.ileggRenter == false }
        assertNull(aktsomhet.manueltSattBeløp)
        assertEquals("Særlig grunner begrunnelse", aktsomhet.særligeGrunnerBegrunnelse)

        val særligGrunner = aktsomhet.vilkårsvurderingSærligeGrunner
        assertNotNull(særligGrunner)
        assertTrue { særligGrunner.any { SærligGrunn.GRAD_AV_UAKTSOMHET == it.særligGrunn } }
        assertTrue { særligGrunner.all { it.begrunnelse == null } }
    }

    private fun lagBehandlingsstegstilstand(behandlingssteg: Behandlingssteg,
                                            behandlingsstegstatus: Behandlingsstegstatus) {
        behandlingsstegstilstandRepository.insert(Behandlingsstegstilstand(behandlingssteg = behandlingssteg,
                                                                           behandlingsstegsstatus = behandlingsstegstatus,
                                                                           behandlingId = behandling.id))
    }

    private fun lagKravgrunnlagsbeløp(klassetype: Klassetype,
                                      nyttBeløp: BigDecimal,
                                      opprinneligUtbetalingsbeløp: BigDecimal): Kravgrunnlagsbeløp433 {
        return Kravgrunnlagsbeløp433(id = UUID.randomUUID(),
                                     klassetype = klassetype,
                                     klassekode = Klassekode.BATR,
                                     opprinneligUtbetalingsbeløp = opprinneligUtbetalingsbeløp,
                                     nyttBeløp = nyttBeløp,
                                     tilbakekrevesBeløp = BigDecimal.ZERO,
                                     skatteprosent = BigDecimal.ZERO,
                                     uinnkrevdBeløp = BigDecimal.ZERO,
                                     resultatkode = "testverdi",
                                     årsakskode = "testverdi",
                                     skyldkode = "testverdi")
    }

    private fun assertAktiviteter(aktiviteter: List<AktivitetDto>) {
        assertTrue { aktiviteter.isNotEmpty() }
        assertEquals(1, aktiviteter.size)
        assertEquals(Klassekode.BATR.aktivitet, aktiviteter[0].aktivitet)
    }

    private fun lagVilkårsvurderingMedSimpelAktsomhet(andelTilbakekreves: BigDecimal? = null,
                                                      manueltSattBeløp: BigDecimal? = null,
                                                      ileggRenter: Boolean? = null,
                                                      særligGrunn: SærligGrunnDto)
            : BehandlingsstegVilkårsvurderingDto {
        val periode = VilkårsvurderingsperiodeDto(periode = PeriodeDto(YearMonth.of(2020, 1),
                                                                       YearMonth.of(2020, 2)),
                                                  vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
                                                  begrunnelse = "Vilkårsvurdering begrunnelse",
                                                  aktsomhetDto = AktsomhetDto(aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
                                                                              andelTilbakekreves = andelTilbakekreves,
                                                                              beløpTilbakekreves = manueltSattBeløp,
                                                                              ileggRenter = ileggRenter,
                                                                              begrunnelse = "Aktsomhet begrunnelse",
                                                                              særligeGrunner = listOf(særligGrunn),
                                                                              tilbakekrevSmåbeløp = false,
                                                                              særligeGrunnerBegrunnelse =
                                                                              "Særlig grunner begrunnelse"))
        return BehandlingsstegVilkårsvurderingDto(listOf(periode))
    }

    private fun lagVilkårsvurderingMedGodTro(perioder: List<PeriodeDto>,
                                             beløpTilbakekreves: BigDecimal? = null): BehandlingsstegVilkårsvurderingDto {
        return BehandlingsstegVilkårsvurderingDto(vilkårsvurderingsperioder = perioder.map {
            VilkårsvurderingsperiodeDto(periode = it,
                                        vilkårsvurderingsresultat = Vilkårsvurderingsresultat.GOD_TRO,
                                        begrunnelse = "Vilkårsvurdering begrunnelse",
                                        godTroDto = GodTroDto(begrunnelse = "God tro begrunnelse",
                                                              beløpErIBehold = true,
                                                              beløpTilbakekreves = beløpTilbakekreves))
        })
    }

    private fun lagForeldese(vararg foreldelsesvurderingstyper: Foreldelsesvurderingstype) {
        val foreldelsesperioder =
                setOf(Foreldelsesperiode(periode = Periode(YearMonth.of(2020, 1), YearMonth.of(2020, 1)),
                                         foreldelsesvurderingstype = foreldelsesvurderingstyper[0],
                                         begrunnelse = "foreldelse begrunnelse 1"),
                      Foreldelsesperiode(periode = Periode(YearMonth.of(2020, 2), YearMonth.of(2020, 2)),
                                         foreldelsesvurderingstype = foreldelsesvurderingstyper[1],
                                         begrunnelse = "foreldelse begrunnelse 2"))
        foreldelseRepository.insert(VurdertForeldelse(behandlingId = behandling.id,
                                                      foreldelsesperioder = foreldelsesperioder))
    }

    private fun oppdaterForeldelsesvurdering(vurdertForeldelse: VurdertForeldelse,
                                             vararg foreldelsesvurderingstyper: Foreldelsesvurderingstype) {
        val foreldelsesperioder = setOf(Foreldelsesperiode(periode = Periode(YearMonth.of(2020, 1),
                                                                             YearMonth.of(2020, 1)),
                                                           foreldelsesvurderingstype = foreldelsesvurderingstyper[0],
                                                           begrunnelse = "foreldelse begrunnelse 1"),
                                        Foreldelsesperiode(periode = Periode(YearMonth.of(2020, 2),
                                                                             YearMonth.of(2020, 2)),
                                                           foreldelsesvurderingstype = foreldelsesvurderingstyper[1],
                                                           begrunnelse = "foreldelse begrunnelse 2"))
        foreldelseRepository.update(vurdertForeldelse.copy(foreldelsesperioder = foreldelsesperioder))
    }

}
