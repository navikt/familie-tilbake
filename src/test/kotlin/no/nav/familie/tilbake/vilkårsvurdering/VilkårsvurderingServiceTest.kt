package no.nav.familie.tilbake.vilkårsvurdering

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingRepository
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetaling
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetalingsperiode
import no.nav.familie.tilbake.foreldelse.ForeldelseService
import no.nav.familie.tilbake.iverksettvedtak.VilkårsvurderingsPeriodeDomainUtil.lagVilkårsvurderingAktsomhet
import no.nav.familie.tilbake.iverksettvedtak.VilkårsvurderingsPeriodeDomainUtil.lagVilkårsvurderingGodTro
import no.nav.familie.tilbake.iverksettvedtak.VilkårsvurderingsPeriodeDomainUtil.lagVilkårsvurderingsperiode
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassekode
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsbeløp433
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurdering
import no.nav.tilbakekreving.Rettsgebyr
import no.nav.tilbakekreving.api.v1.dto.AktivitetDto
import no.nav.tilbakekreving.api.v1.dto.AktsomhetDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegForeldelseDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegVilkårsvurderingDto
import no.nav.tilbakekreving.api.v1.dto.ForeldelsesperiodeDto
import no.nav.tilbakekreving.api.v1.dto.GodTroDto
import no.nav.tilbakekreving.api.v1.dto.SærligGrunnDto
import no.nav.tilbakekreving.api.v1.dto.VilkårsvurderingsperiodeDto
import no.nav.tilbakekreving.februar
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsesundertype
import no.nav.tilbakekreving.kontrakter.foreldelse.Foreldelsesvurderingstype
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.periode.Månedsperiode
import no.nav.tilbakekreving.kontrakter.periode.Månedsperiode.Companion.til
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunnType
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import no.nav.tilbakekreving.mars
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class VilkårsvurderingServiceTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var foreldelseService: ForeldelseService
    override val tømDBEtterHverTest = false

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
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @Autowired
    private lateinit var vilkårsvurderingService: VilkårsvurderingService

    private lateinit var behandling: Behandling

    private val førstePeriode = januar(2020) til januar(2020)
    private val andrePeriode = februar(2020) til februar(2020)

    @BeforeEach
    fun init() {
        behandling = lagBehandling(
            kravgrunnlagPerioder = listOf(førstePeriode, andrePeriode),
            faktaperioder = listOf(førstePeriode.fom til andrePeriode.tom),
        )
    }

    @Test
    fun `hentVilkårsvurdering skal hente vilkårsvurdering fra fakta perioder`() {
        lagBehandlingsstegstilstand(behandling.id, Behandlingssteg.FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        lagBehandlingsstegstilstand(behandling.id, Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.KLAR)

        val vurdertVilkårsvurderingDto = vilkårsvurderingService.hentVilkårsvurdering(behandling.id)
        vurdertVilkårsvurderingDto.rettsgebyr shouldBe Rettsgebyr.rettsgebyr
        vurdertVilkårsvurderingDto.perioder.shouldNotBeEmpty()
        vurdertVilkårsvurderingDto.perioder.size shouldBe 1
        val vurdertPeriode = vurdertVilkårsvurderingDto.perioder[0]
        vurdertPeriode.periode shouldBe Datoperiode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 2, 29))
        vurdertPeriode.hendelsestype shouldBe Hendelsestype.ANNET
        vurdertPeriode.feilutbetaltBeløp shouldBe BigDecimal("20000")
        vurdertPeriode.reduserteBeløper.shouldBeEmpty()
        assertAktiviteter(vurdertPeriode.aktiviteter)
        vurdertPeriode.aktiviteter[0].beløp shouldBe BigDecimal(20000)
        vurdertPeriode.foreldet.shouldBeFalse()
        vurdertPeriode.foreldet.shouldBeFalse()
        vurdertPeriode.begrunnelse.shouldBeNull()
        vurdertPeriode.vilkårsvurderingsresultatInfo.shouldBeNull()
    }

    @Test
    fun `hentVilkårsvurdering skal hente vilkårsvurdering fra foreldelse perioder som ikke er foreldet`() {
        lagForeldelse(førstePeriode to Foreldelsesvurderingstype.FORELDET, andrePeriode to Foreldelsesvurderingstype.IKKE_FORELDET)
        lagBehandlingsstegstilstand(behandling.id, Behandlingssteg.FORELDELSE, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(behandling.id, Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.KLAR)

        val vurdertVilkårsvurderingDto = vilkårsvurderingService.hentVilkårsvurdering(behandling.id)
        vurdertVilkårsvurderingDto.rettsgebyr shouldBe Rettsgebyr.rettsgebyr
        vurdertVilkårsvurderingDto.perioder.shouldNotBeEmpty()
        vurdertVilkårsvurderingDto.perioder.size shouldBe 2

        val foreldetPeriode = vurdertVilkårsvurderingDto.perioder[0]
        foreldetPeriode.periode shouldBe Datoperiode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31))
        foreldetPeriode.hendelsestype shouldBe Hendelsestype.ANNET
        foreldetPeriode.feilutbetaltBeløp shouldBe BigDecimal("10000")
        foreldetPeriode.foreldet.shouldBeTrue()
        foreldetPeriode.reduserteBeløper.shouldBeEmpty()
        assertAktiviteter(foreldetPeriode.aktiviteter)
        foreldetPeriode.aktiviteter[0].beløp shouldBe BigDecimal(10000)
        foreldetPeriode.begrunnelse shouldBe "begrunnelse"
        foreldetPeriode.vilkårsvurderingsresultatInfo.shouldBeNull()

        val ikkeForeldetPeriode = vurdertVilkårsvurderingDto.perioder[1]
        ikkeForeldetPeriode.periode shouldBe Datoperiode(LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 29))
        ikkeForeldetPeriode.hendelsestype shouldBe Hendelsestype.ANNET
        ikkeForeldetPeriode.foreldet.shouldBeFalse()
        ikkeForeldetPeriode.feilutbetaltBeløp shouldBe BigDecimal("10000")
        ikkeForeldetPeriode.aktiviteter[0].beløp shouldBe BigDecimal(10000)
        ikkeForeldetPeriode.reduserteBeløper.shouldBeEmpty()
        assertAktiviteter(ikkeForeldetPeriode.aktiviteter)
        ikkeForeldetPeriode.begrunnelse.shouldBeNull()
        ikkeForeldetPeriode.vilkårsvurderingsresultatInfo.shouldBeNull()
    }

    @Test
    fun `hentVilkårsvurdering skal hente vilkårsvurdering når perioder er delt opp`() {
        // delt opp i to perioder
        val periode1 = Datoperiode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31))
        val periode2 = Datoperiode(LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 29))
        val behandlingsstegVilkårsvurderingDto = lagVilkårsvurderingMedGodTro(perioder = listOf(periode1, periode2))
        vilkårsvurderingService.lagreVilkårsvurdering(behandling.id, behandlingsstegVilkårsvurderingDto)

        val vurdertVilkårsvurderingDto = vilkårsvurderingService.hentVilkårsvurdering(behandling.id)
        vurdertVilkårsvurderingDto.rettsgebyr shouldBe Rettsgebyr.rettsgebyr
        vurdertVilkårsvurderingDto.perioder.shouldNotBeEmpty()
        vurdertVilkårsvurderingDto.perioder.size shouldBe 2

        val førstePeriode = vurdertVilkårsvurderingDto.perioder[0]
        førstePeriode.periode shouldBe periode1
        førstePeriode.hendelsestype shouldBe Hendelsestype.ANNET
        førstePeriode.feilutbetaltBeløp shouldBe BigDecimal(10000)
        førstePeriode.foreldet.shouldBeFalse()
        assertAktiviteter(førstePeriode.aktiviteter)
        førstePeriode.aktiviteter[0].beløp shouldBe BigDecimal(10000)
        var vilkårsvurderingsresultatDto = førstePeriode.vilkårsvurderingsresultatInfo
        vilkårsvurderingsresultatDto.shouldNotBeNull()
        vilkårsvurderingsresultatDto.vilkårsvurderingsresultat shouldBe Vilkårsvurderingsresultat.GOD_TRO

        val andrePeriode = vurdertVilkårsvurderingDto.perioder[1]
        andrePeriode.periode shouldBe periode2
        andrePeriode.hendelsestype shouldBe Hendelsestype.ANNET
        andrePeriode.feilutbetaltBeløp shouldBe BigDecimal(10000)
        andrePeriode.foreldet.shouldBeFalse()
        assertAktiviteter(andrePeriode.aktiviteter)
        andrePeriode.aktiviteter[0].beløp shouldBe BigDecimal(10000)
        vilkårsvurderingsresultatDto = andrePeriode.vilkårsvurderingsresultatInfo
        vilkårsvurderingsresultatDto.shouldNotBeNull()
        vilkårsvurderingsresultatDto.vilkårsvurderingsresultat shouldBe Vilkårsvurderingsresultat.GOD_TRO
    }

    @Test
    fun `hentVilkårsvurdering skal hente vilkårsvurdering med reduserte beløper`() {
        lagBehandlingsstegstilstand(behandling.id, Behandlingssteg.FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        lagBehandlingsstegstilstand(behandling.id, Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.KLAR)

        val kravgrunnlag431 = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
        val justBeløp = lagKravgrunnlagsbeløp(
            klassetype = Klassetype.JUST,
            nyttBeløp = BigDecimal(5000),
            opprinneligUtbetalingsbeløp = BigDecimal.ZERO,
        )
        val trekBeløp = lagKravgrunnlagsbeløp(
            klassetype = Klassetype.TREK,
            nyttBeløp = BigDecimal.ZERO,
            opprinneligUtbetalingsbeløp = BigDecimal(-2000),
        )
        val skatBeløp = lagKravgrunnlagsbeløp(
            klassetype = Klassetype.SKAT,
            nyttBeløp = BigDecimal.ZERO,
            opprinneligUtbetalingsbeløp = BigDecimal(-2000),
        )
        val førstePeriode = kravgrunnlag431.perioder
            .toList()[0]
            .copy(
                beløp =
                    setOf(
                        Testdata.feilKravgrunnlagsbeløp433.copy(id = UUID.randomUUID()),
                        Testdata.ytelKravgrunnlagsbeløp433.copy(id = UUID.randomUUID()),
                        justBeløp,
                    ),
            )
        val andrePeriode = kravgrunnlag431.perioder
            .toList()[1]
            .copy(
                beløp =
                    setOf(
                        Testdata.feilKravgrunnlagsbeløp433.copy(id = UUID.randomUUID()),
                        Testdata.ytelKravgrunnlagsbeløp433.copy(id = UUID.randomUUID()),
                        trekBeløp,
                        skatBeløp,
                    ),
            )
        kravgrunnlagRepository.update(kravgrunnlag431.copy(perioder = setOf(førstePeriode, andrePeriode)))

        val vurdertVilkårsvurderingDto = vilkårsvurderingService.hentVilkårsvurdering(behandling.id)
        vurdertVilkårsvurderingDto.rettsgebyr shouldBe Rettsgebyr.rettsgebyr
        vurdertVilkårsvurderingDto.perioder.shouldNotBeEmpty()
        vurdertVilkårsvurderingDto.perioder.size shouldBe 1
        val vurdertPeriode = vurdertVilkårsvurderingDto.perioder[0]
        vurdertPeriode.periode shouldBe Datoperiode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 2, 29))
        vurdertPeriode.hendelsestype shouldBe Hendelsestype.ANNET
        vurdertPeriode.feilutbetaltBeløp shouldBe BigDecimal("20000")
        assertAktiviteter(vurdertPeriode.aktiviteter)
        vurdertPeriode.aktiviteter[0].beløp shouldBe BigDecimal(20000)
        vurdertPeriode.foreldet.shouldBeFalse()

        vurdertPeriode.reduserteBeløper.shouldNotBeEmpty()
        vurdertPeriode.reduserteBeløper.size shouldBe 3
        var redusertBeløp = vurdertPeriode.reduserteBeløper[0]
        redusertBeløp.trekk.shouldBeTrue()
        redusertBeløp.beløp shouldBe BigDecimal("2000.00")
        redusertBeløp = vurdertPeriode.reduserteBeløper[1]
        redusertBeløp.trekk.shouldBeTrue()
        redusertBeløp.beløp shouldBe BigDecimal("2000.00")
        redusertBeløp = vurdertPeriode.reduserteBeløper[2]
        redusertBeløp.trekk.shouldBeFalse()
        redusertBeløp.beløp shouldBe BigDecimal("5000.00")

        vurdertPeriode.vilkårsvurderingsresultatInfo.shouldBeNull()
        vurdertPeriode.begrunnelse.shouldBeNull()
    }

    @Test
    fun `hentVilkårsvurdering skal hente allerede lagret simpel aktsomhet vilkårsvurdering`() {
        val behandlingsstegVilkårsvurderingDto =
            lagVilkårsvurderingMedSimpelAktsomhet(særligGrunn = SærligGrunnDto(SærligGrunnType.GRAD_AV_UAKTSOMHET))
        vilkårsvurderingService.lagreVilkårsvurdering(
            behandlingId = behandling.id,
            behandlingsstegVilkårsvurderingDto = behandlingsstegVilkårsvurderingDto,
        )

        val vurdertVilkårsvurderingDto = vilkårsvurderingService.hentVilkårsvurdering(behandling.id)
        vurdertVilkårsvurderingDto.rettsgebyr shouldBe Rettsgebyr.rettsgebyr
        vurdertVilkårsvurderingDto.perioder.shouldNotBeEmpty()
        vurdertVilkårsvurderingDto.perioder.size shouldBe 1
        val vurdertPeriode = vurdertVilkårsvurderingDto.perioder[0]
        vurdertPeriode.periode shouldBe Datoperiode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 2, 29))
        vurdertPeriode.hendelsestype shouldBe Hendelsestype.ANNET
        vurdertPeriode.feilutbetaltBeløp shouldBe BigDecimal("20000")
        assertAktiviteter(vurdertPeriode.aktiviteter)
        vurdertPeriode.aktiviteter[0].beløp shouldBe BigDecimal(20000)
        vurdertPeriode.foreldet.shouldBeFalse()
        vurdertPeriode.begrunnelse shouldBe "Vilkårsvurdering begrunnelse"

        val vilkårsvurderingsresultatDto = vurdertPeriode.vilkårsvurderingsresultatInfo
        vilkårsvurderingsresultatDto.shouldNotBeNull()
        vilkårsvurderingsresultatDto.godTro.shouldBeNull()
        vilkårsvurderingsresultatDto.vilkårsvurderingsresultat shouldBe Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT
        val aktsomhetDto = vilkårsvurderingsresultatDto.aktsomhet
        aktsomhetDto.shouldNotBeNull()
        aktsomhetDto.aktsomhet shouldBe Aktsomhet.SIMPEL_UAKTSOMHET
        aktsomhetDto.begrunnelse shouldBe "Aktsomhet begrunnelse"
        aktsomhetDto.tilbakekrevSmåbeløp.shouldBeFalse()
        aktsomhetDto.særligeGrunnerTilReduksjon.shouldBeFalse()
        aktsomhetDto.andelTilbakekreves.shouldBeNull()
        aktsomhetDto.ileggRenter.shouldBeNull()
        aktsomhetDto.beløpTilbakekreves.shouldBeNull()
        aktsomhetDto.særligeGrunnerBegrunnelse shouldBe "Særlig grunner begrunnelse"
        val særligGrunner = aktsomhetDto.særligeGrunner
        særligGrunner.shouldNotBeNull()
        særligGrunner.any { SærligGrunnType.GRAD_AV_UAKTSOMHET == it.særligGrunn }.shouldBeTrue()
        særligGrunner.all { it.begrunnelse == null }.shouldBeTrue()
    }

    @Test
    fun `hentVilkårsvurdering skal hente allerede lagret god tro vilkårsvurdering`() {
        val behandlingsstegVilkårsvurderingDto =
            lagVilkårsvurderingMedGodTro(
                perioder = listOf(Datoperiode(YearMonth.of(2020, 1), YearMonth.of(2020, 2))),
            )
        vilkårsvurderingService.lagreVilkårsvurdering(
            behandlingId = behandling.id,
            behandlingsstegVilkårsvurderingDto = behandlingsstegVilkårsvurderingDto,
        )

        val vurdertVilkårsvurderingDto = vilkårsvurderingService.hentVilkårsvurdering(behandling.id)
        vurdertVilkårsvurderingDto.rettsgebyr shouldBe Rettsgebyr.rettsgebyr
        vurdertVilkårsvurderingDto.perioder.shouldNotBeEmpty()
        vurdertVilkårsvurderingDto.perioder.size shouldBe 1
        val vurdertPeriode = vurdertVilkårsvurderingDto.perioder[0]
        vurdertPeriode.periode shouldBe Datoperiode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 2, 29))
        vurdertPeriode.hendelsestype shouldBe Hendelsestype.ANNET
        vurdertPeriode.feilutbetaltBeløp shouldBe BigDecimal("20000")
        assertAktiviteter(vurdertPeriode.aktiviteter)
        vurdertPeriode.aktiviteter[0].beløp shouldBe BigDecimal(20000)
        vurdertPeriode.foreldet.shouldBeFalse()
        vurdertPeriode.begrunnelse shouldBe "Vilkårsvurdering begrunnelse"

        val vilkårsvurderingsresultatDto = vurdertPeriode.vilkårsvurderingsresultatInfo
        vilkårsvurderingsresultatDto.shouldNotBeNull()
        vilkårsvurderingsresultatDto.aktsomhet.shouldBeNull()
        vilkårsvurderingsresultatDto.vilkårsvurderingsresultat shouldBe Vilkårsvurderingsresultat.GOD_TRO
        val godTroDto = vilkårsvurderingsresultatDto.godTro
        godTroDto.shouldNotBeNull()
        godTroDto.beløpErIBehold.shouldBeTrue()
        godTroDto.begrunnelse shouldBe "God tro begrunnelse"
        godTroDto.beløpTilbakekreves.shouldBeNull()
    }

    @Test
    fun `skal hente inaktiv vilkårsvurdering`() {
        val behandlingsstegVilkårsvurderingDto =
            lagVilkårsvurderingMedGodTro(
                perioder = listOf(Datoperiode(YearMonth.of(2020, 1), YearMonth.of(2020, 2))),
            )
        vilkårsvurderingService.lagreVilkårsvurdering(
            behandlingId = behandling.id,
            behandlingsstegVilkårsvurderingDto = behandlingsstegVilkårsvurderingDto,
        )

        val oppdatertVilkårsvurderingDto = lagVilkårsvurderingMedSimpelAktsomhet(særligGrunn = SærligGrunnDto(SærligGrunnType.GRAD_AV_UAKTSOMHET))
        val oppdatertVilkårsvurderingDtoNavsFeil = lagVilkårsvurderingMedSimpelAktsomhet(særligGrunn = SærligGrunnDto(SærligGrunnType.HELT_ELLER_DELVIS_NAVS_FEIL))
        vilkårsvurderingService.lagreVilkårsvurdering(
            behandlingId = behandling.id,
            behandlingsstegVilkårsvurderingDto = oppdatertVilkårsvurderingDto,
        )
        vilkårsvurderingService.lagreVilkårsvurdering(
            behandlingId = behandling.id,
            behandlingsstegVilkårsvurderingDto = oppdatertVilkårsvurderingDtoNavsFeil,
        )

        val inaktiveVilkårsvurderinger = vilkårsvurderingService.hentInaktivVilkårsvurdering(behandling.id)
        inaktiveVilkårsvurderinger.size shouldBe 2
        inaktiveVilkårsvurderinger
            .first()
            .perioder
            .first()
            .vilkårsvurderingsresultatInfo
            ?.vilkårsvurderingsresultat shouldBe Vilkårsvurderingsresultat.GOD_TRO
        inaktiveVilkårsvurderinger
            .first()
            .perioder
            .first()
            .vilkårsvurderingsresultatInfo
            ?.godTro
            ?.begrunnelse shouldBe "God tro begrunnelse"

        inaktiveVilkårsvurderinger
            .last()
            .perioder
            .first()
            .vilkårsvurderingsresultatInfo
            ?.vilkårsvurderingsresultat shouldBe Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT
        inaktiveVilkårsvurderinger
            .last()
            .perioder
            .first()
            .vilkårsvurderingsresultatInfo
            ?.aktsomhet
            ?.begrunnelse shouldBe "Aktsomhet begrunnelse"
        inaktiveVilkårsvurderinger
            .last()
            .perioder
            .first()
            .vilkårsvurderingsresultatInfo
            ?.aktsomhet
            ?.særligeGrunner
            ?.shouldHaveSize(1)
        inaktiveVilkårsvurderinger
            .last()
            .perioder
            .first()
            .vilkårsvurderingsresultatInfo
            ?.aktsomhet
            ?.særligeGrunner
            ?.first()
            ?.særligGrunn shouldBe SærligGrunnType.GRAD_AV_UAKTSOMHET
    }

    @Test
    fun `hentVilkårsvurdering skal hente foreldelse perioder som endret til IKKE_FORELDET`() {
        // en periode med FORELDET og andre er IKKE_FORELDET
        lagForeldelse(førstePeriode to Foreldelsesvurderingstype.FORELDET, andrePeriode to Foreldelsesvurderingstype.IKKE_FORELDET)
        lagBehandlingsstegstilstand(behandling.id, Behandlingssteg.FORELDELSE, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(behandling.id, Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.KLAR)

        var vurdertVilkårsvurderingDto = vilkårsvurderingService.hentVilkårsvurdering(behandling.id)
        vurdertVilkårsvurderingDto.perioder.shouldNotBeEmpty()
        vurdertVilkårsvurderingDto.perioder.size shouldBe 2
        vurdertVilkårsvurderingDto.perioder.count { it.foreldet } shouldBe 1
        vurdertVilkårsvurderingDto.perioder.count { !it.foreldet } shouldBe 1

        // behandle vilkårsvurdering
        vilkårsvurderingService.lagreVilkårsvurdering(
            behandling.id,
            lagVilkårsvurderingMedGodTro(
                perioder = listOf(Datoperiode(YearMonth.of(2020, 2), YearMonth.of(2020, 2))),
            ),
        )
        lagBehandlingsstegstilstand(behandling.id, Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(behandling.id, Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.KLAR)

        lagForeldelse(
            førstePeriode to Foreldelsesvurderingstype.IKKE_FORELDET,
            andrePeriode to Foreldelsesvurderingstype.IKKE_FORELDET,
        )

        vurdertVilkårsvurderingDto = vilkårsvurderingService.hentVilkårsvurdering(behandling.id)
        vurdertVilkårsvurderingDto.perioder.shouldNotBeEmpty()
        vurdertVilkårsvurderingDto.perioder.size shouldBe 2
        vurdertVilkårsvurderingDto.perioder.count { !it.foreldet } shouldBe 2

        val ikkeVurdertPeriode = vurdertVilkårsvurderingDto.perioder[0]
        ikkeVurdertPeriode.periode shouldBe Datoperiode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31))
        ikkeVurdertPeriode.hendelsestype shouldBe Hendelsestype.ANNET
        ikkeVurdertPeriode.feilutbetaltBeløp shouldBe BigDecimal("10000")
        ikkeVurdertPeriode.reduserteBeløper.shouldBeEmpty()
        assertAktiviteter(ikkeVurdertPeriode.aktiviteter)
        ikkeVurdertPeriode.aktiviteter[0].beløp shouldBe BigDecimal(10000)
        ikkeVurdertPeriode.vilkårsvurderingsresultatInfo.shouldBeNull()
        ikkeVurdertPeriode.begrunnelse.shouldBeNull()

        val vurdertPeriode = vurdertVilkårsvurderingDto.perioder[1]
        vurdertPeriode.periode shouldBe Datoperiode(LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 29))
        vurdertPeriode.hendelsestype shouldBe Hendelsestype.ANNET
        vurdertPeriode.feilutbetaltBeløp shouldBe BigDecimal("10000")
        assertAktiviteter(vurdertPeriode.aktiviteter)
        vurdertPeriode.aktiviteter[0].beløp shouldBe BigDecimal(10000)
        vurdertPeriode.begrunnelse shouldBe "Vilkårsvurdering begrunnelse"

        val vilkårsvurderingsresultatDto = vurdertPeriode.vilkårsvurderingsresultatInfo
        vilkårsvurderingsresultatDto.shouldNotBeNull()
        vilkårsvurderingsresultatDto.aktsomhet.shouldBeNull()
        vilkårsvurderingsresultatDto.vilkårsvurderingsresultat shouldBe Vilkårsvurderingsresultat.GOD_TRO
        val godTroDto = vilkårsvurderingsresultatDto.godTro
        godTroDto.shouldNotBeNull()
        godTroDto.beløpErIBehold.shouldBeTrue()
        godTroDto.begrunnelse shouldBe "God tro begrunnelse"
        godTroDto.beløpTilbakekreves.shouldBeNull()
    }

    @Test
    fun `hentVilkårsvurdering skal hente perioder som endret til FORELDET`() {
        // en periode med FORELDET og andre er IKKE_FORELDET
        lagForeldelse(førstePeriode to Foreldelsesvurderingstype.FORELDET, andrePeriode to Foreldelsesvurderingstype.IKKE_FORELDET)
        lagBehandlingsstegstilstand(behandling.id, Behandlingssteg.FORELDELSE, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(behandling.id, Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.KLAR)

        var vurdertVilkårsvurderingDto = vilkårsvurderingService.hentVilkårsvurdering(behandling.id)
        vurdertVilkårsvurderingDto.perioder.shouldNotBeEmpty()
        vurdertVilkårsvurderingDto.perioder.size shouldBe 2
        vurdertVilkårsvurderingDto.perioder.count { it.foreldet } shouldBe 1
        vurdertVilkårsvurderingDto.perioder.count { !it.foreldet } shouldBe 1

        // behandle vilkårsvurdering
        vilkårsvurderingService.lagreVilkårsvurdering(
            behandling.id,
            lagVilkårsvurderingMedGodTro(
                perioder = listOf(Datoperiode(YearMonth.of(2020, 2), YearMonth.of(2020, 2))),
            ),
        )
        lagBehandlingsstegstilstand(behandling.id, Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(behandling.id, Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.KLAR)

        lagForeldelse(
            førstePeriode to Foreldelsesvurderingstype.FORELDET,
            andrePeriode to Foreldelsesvurderingstype.FORELDET,
        )

        vurdertVilkårsvurderingDto = vilkårsvurderingService.hentVilkårsvurdering(behandling.id)
        vurdertVilkårsvurderingDto.perioder.shouldNotBeEmpty()
        vurdertVilkårsvurderingDto.perioder.size shouldBe 2
        vurdertVilkårsvurderingDto.perioder.count { it.foreldet } shouldBe 2

        val førsteForeldetPeriode = vurdertVilkårsvurderingDto.perioder[0]
        førsteForeldetPeriode.periode shouldBe (1.januar(2020) til 31.januar(2020))
        førsteForeldetPeriode.hendelsestype shouldBe Hendelsestype.ANNET
        førsteForeldetPeriode.feilutbetaltBeløp shouldBe BigDecimal("10000")
        førsteForeldetPeriode.reduserteBeløper.shouldBeEmpty()
        assertAktiviteter(førsteForeldetPeriode.aktiviteter)
        førsteForeldetPeriode.aktiviteter[0].beløp shouldBe BigDecimal(10000)
        førsteForeldetPeriode.vilkårsvurderingsresultatInfo.shouldBeNull()
        førsteForeldetPeriode.begrunnelse shouldBe "begrunnelse"

        val andreForeldetPeriode = vurdertVilkårsvurderingDto.perioder[1]
        andreForeldetPeriode.periode shouldBe (1.februar(2020) til 29.februar(2020))
        andreForeldetPeriode.hendelsestype shouldBe Hendelsestype.ANNET
        andreForeldetPeriode.feilutbetaltBeløp shouldBe BigDecimal("10000")
        andreForeldetPeriode.reduserteBeløper.shouldBeEmpty()
        assertAktiviteter(andreForeldetPeriode.aktiviteter)
        andreForeldetPeriode.aktiviteter[0].beløp shouldBe BigDecimal(10000)
        andreForeldetPeriode.vilkårsvurderingsresultatInfo.shouldBeNull()
        andreForeldetPeriode.begrunnelse shouldBe "begrunnelse"
    }

    @Test
    fun `lagreVilkårsvurdering skal ikke lagre vilkårsvurdering når andelTilbakekreves er mer enn 100 prosent `() {
        val exception = shouldThrow<RuntimeException> {
            vilkårsvurderingService.lagreVilkårsvurdering(
                behandling.id,
                lagVilkårsvurderingMedSimpelAktsomhet(
                    andelTilbakekreves = BigDecimal(120),
                    særligGrunn =
                        SærligGrunnDto(SærligGrunnType.GRAD_AV_UAKTSOMHET),
                ),
            )
        }
        exception.message shouldBe "Andel som skal tilbakekreves kan ikke være mer enn 100 prosent"
    }

    @Test
    fun `lagreVilkårsvurdering skal ikke lagre vilkårsvurdering når ANNET særlig grunner mangler ANNET begrunnelse`() {
        val exception = shouldThrow<RuntimeException> {
            vilkårsvurderingService.lagreVilkårsvurdering(
                behandling.id,
                lagVilkårsvurderingMedSimpelAktsomhet(særligGrunn = SærligGrunnDto(SærligGrunnType.ANNET)),
            )
        }
        exception.message shouldBe "ANNET særlig grunner må ha ANNET begrunnelse"
    }

    @Test
    fun `lagreVilkårsvurdering skal ikke lagre vilkårsvurdering når manueltSattBeløp er mer enn feilutbetalt beløp`() {
        // forutsetter at kravgrunnlag har 20000 som feilutbetalt beløp fra Testdata
        val behandlingsstegVilkårsvurderingDto = lagVilkårsvurderingMedSimpelAktsomhet(
            manueltSattBeløp = BigDecimal(30000),
            særligGrunn = SærligGrunnDto(SærligGrunnType.GRAD_AV_UAKTSOMHET),
        )
        val exception = shouldThrow<RuntimeException> {
            vilkårsvurderingService.lagreVilkårsvurdering(behandling.id, behandlingsstegVilkårsvurderingDto)
        }
        exception.message shouldBe "Beløp som skal tilbakekreves kan ikke være mer enn feilutbetalt beløp"
    }

    @Test
    fun `lagreVilkårsvurdering skal ikke lagre vilkårsvurdering når tilbakekrevesBeløp er mer enn feilutbetalt beløp`() {
        // forutsetter at kravgrunnlag har 20000 som feilutbetalt beløp fra Testdata
        val exception = shouldThrow<RuntimeException> {
            vilkårsvurderingService.lagreVilkårsvurdering(
                behandling.id,
                lagVilkårsvurderingMedGodTro(
                    listOf(Datoperiode(YearMonth.of(2020, 1), YearMonth.of(2020, 2))),
                    BigDecimal(30000),
                ),
            )
        }
        exception.message shouldBe "Beløp som skal tilbakekreves kan ikke være mer enn feilutbetalt beløp"
    }

    @Test
    fun `lagreVilkårsvurdering skal lagre vilkårsvurdering med false ileggRenter for barnetrygd behandling`() {
        // forutsetter at behandling opprettet for barnetrygd fra Testdata
        vilkårsvurderingService.lagreVilkårsvurdering(
            behandling.id,
            lagVilkårsvurderingMedSimpelAktsomhet(
                ileggRenter = true,
                særligGrunn =
                    SærligGrunnDto(SærligGrunnType.GRAD_AV_UAKTSOMHET),
            ),
        )

        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
            .singleOrNull()
            .shouldNotBeNull()

        vilkårsvurdering.perioder.shouldNotBeEmpty()
        vilkårsvurdering.perioder.size shouldBe 1
        val vurdertPeriode = vilkårsvurdering.perioder.toList()[0]
        vurdertPeriode.periode shouldBe Månedsperiode(YearMonth.of(2020, 1), YearMonth.of(2020, 2))
        vurdertPeriode.begrunnelse shouldBe "Vilkårsvurdering begrunnelse"

        vurdertPeriode.aktsomhet.shouldNotBeNull()
        vurdertPeriode.godTro.shouldBeNull()
        vurdertPeriode.vilkårsvurderingsresultat shouldBe Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT

        val aktsomhet = vurdertPeriode.aktsomhet
        aktsomhet.shouldNotBeNull()
        aktsomhet.aktsomhet shouldBe Aktsomhet.SIMPEL_UAKTSOMHET
        aktsomhet.begrunnelse shouldBe "Aktsomhet begrunnelse"
        aktsomhet.tilbakekrevSmåbeløp.shouldBeFalse()
        aktsomhet.særligeGrunnerTilReduksjon.shouldBeFalse()
        aktsomhet.andelTilbakekreves.shouldBeNull()
        aktsomhet.andelTilbakekreves.shouldBeNull()
        aktsomhet.ileggRenter shouldBe false
        aktsomhet.manueltSattBeløp.shouldBeNull()
        aktsomhet.særligeGrunnerBegrunnelse shouldBe "Særlig grunner begrunnelse"

        val særligGrunner = aktsomhet.vilkårsvurderingSærligeGrunner
        særligGrunner.shouldNotBeNull()
        særligGrunner.any { SærligGrunnType.GRAD_AV_UAKTSOMHET == it.særligGrunn }.shouldBeTrue()
        særligGrunner.all { it.begrunnelse == null }.shouldBeTrue()
    }

    @Test
    fun `er vilkårsperioder like - skal være ulike og returnere false`() {
        val vilkårsvurdering = Vilkårsvurdering(
            id = UUID.randomUUID(),
            behandlingId = behandling.id,
            aktiv = true,
            perioder = setOf(
                lagVilkårsvurderingsperiode(
                    periode = Månedsperiode(YearMonth.of(2024, 1), YearMonth.of(2024, 5)),
                ),
                lagVilkårsvurderingsperiode(
                    periode = Månedsperiode(YearMonth.of(2024, 1), YearMonth.of(2024, 5)),
                    begrunnelse = "annen begrunnelse",
                ),
            ),
        )

        vilkårsvurderingRepository.insert(vilkårsvurdering)

        val erLike = vilkårsvurderingService.sjekkOmVilkårsvurderingPerioderErLike(behandling.id, SecureLog.Context.tom())
        Assertions.assertFalse(erLike)
    }

    @Test
    fun `sjekk er vilkårsperioder like utenom dato og beløp som skal tilbakekreves - skal være like og returnere true`() {
        val vilkårsvurdering = Vilkårsvurdering(
            id = UUID.randomUUID(),
            behandlingId = behandling.id,
            aktiv = true,
            perioder = setOf(
                lagVilkårsvurderingsperiode(
                    periode = Månedsperiode(YearMonth.of(2024, 1), YearMonth.of(2024, 3)),
                    godTro = lagVilkårsvurderingGodTro(beløpTilbakekreves = BigDecimal(1234)),
                ),
                lagVilkårsvurderingsperiode(
                    periode = Månedsperiode(YearMonth.of(2024, 4), YearMonth.of(2024, 5)),
                    godTro = lagVilkårsvurderingGodTro(beløpTilbakekreves = BigDecimal(4321)),
                ),
            ),
        )

        vilkårsvurderingRepository.insert(vilkårsvurdering)

        val erLike = vilkårsvurderingService.sjekkOmVilkårsvurderingPerioderErLike(behandling.id, SecureLog.Context.tom())
        Assertions.assertTrue(erLike)
    }

    @Test
    fun `sjekk at vilkårsperioder ikke er like når det er ulik prosent andel som skal tilbakekreves`() {
        val vilkårsvurdering = Vilkårsvurdering(
            id = UUID.randomUUID(),
            behandlingId = behandling.id,
            aktiv = true,
            perioder = setOf(
                lagVilkårsvurderingsperiode(
                    periode = Månedsperiode(YearMonth.of(2024, 1), YearMonth.of(2024, 3)),
                    aktsomhet = lagVilkårsvurderingAktsomhet(andelTilbakekreves = BigDecimal(100)),
                    godTro = lagVilkårsvurderingGodTro(beløpTilbakekreves = BigDecimal(1234)),
                ),
                lagVilkårsvurderingsperiode(
                    periode = Månedsperiode(YearMonth.of(2024, 4), YearMonth.of(2024, 5)),
                    aktsomhet = lagVilkårsvurderingAktsomhet(andelTilbakekreves = BigDecimal(50)),
                    godTro = lagVilkårsvurderingGodTro(beløpTilbakekreves = BigDecimal(4321)),
                ),
            ),
        )

        vilkårsvurderingRepository.insert(vilkårsvurdering)

        val erLike = vilkårsvurderingService.sjekkOmVilkårsvurderingPerioderErLike(behandling.id, SecureLog.Context.tom())
        Assertions.assertFalse(erLike)
    }

    @Test
    fun `ved splitt og vurdering av 1 av 2 perioder skal den uvurderte perioden fortsatt eksistere etter henting av vilkårsvurderingen`() {
        // Splitter på første periode og lagrer vurderingen kun den første perioden
        val splittetPeriode = 1.januar(2020) til 31.januar(2020)

        vilkårsvurderingService.lagreVilkårsvurdering(behandling.id, godTroVurdering(splittetPeriode))

        val oppdatertVilkårsvurdering = vilkårsvurderingService.hentVilkårsvurdering(behandling.id)
        oppdatertVilkårsvurdering.perioder[0].periode shouldBe splittetPeriode
        oppdatertVilkårsvurdering.perioder[1].periode shouldBe (1.februar(2020) til 29.februar(2020))
    }

    @Test
    fun `ved splitt og vurdering av 1 av 2 perioder skal den uvurderte perioden fortsatt eksistere etter henting av vilkårsvurderingen om foreldelse er vurdert`() {
        lagForeldelse(januar(2020) til februar(2020) to Foreldelsesvurderingstype.IKKE_FORELDET)
        // Splitter på første periode og lagrer vurderingen kun den første perioden
        val splittetPeriode = 1.januar(2020) til 31.januar(2020)

        vilkårsvurderingService.lagreVilkårsvurdering(behandling.id, godTroVurdering(splittetPeriode))

        val oppdatertVilkårsvurdering = vilkårsvurderingService.hentVilkårsvurdering(behandling.id)
        oppdatertVilkårsvurdering.perioder[0].periode shouldBe splittetPeriode
        oppdatertVilkårsvurdering.perioder[1].periode shouldBe (1.februar(2020) til 29.februar(2020))
    }

    @Test
    fun `splittet periode overskrives av en foreldelse`() {
        val foreldetPeriode = (1.januar(2020) til 29.februar(2020))
        lagForeldelse(januar(2020) til februar(2020) to Foreldelsesvurderingstype.IKKE_FORELDET)

        vilkårsvurderingService.lagreVilkårsvurdering(behandling.id, godTroVurdering(1.januar(2020) til 31.januar(2020)))
        lagForeldelse(januar(2020) til februar(2020) to Foreldelsesvurderingstype.FORELDET)

        val oppdatertVilkårsvurdering = vilkårsvurderingService.hentVilkårsvurdering(behandling.id)
        oppdatertVilkårsvurdering.perioder[0].foreldet shouldBe true
        oppdatertVilkårsvurdering.perioder[0].periode shouldBe foreldetPeriode
        oppdatertVilkårsvurdering.perioder.size shouldBe 1
    }

    @Test
    fun `to perioder der kun en er vilkårsvurdert`() {
        behandling = lagBehandling(
            kravgrunnlagPerioder = listOf(januar(2020) til januar(2020), mars(2020) til mars(2020)),
            faktaperioder = listOf(januar(2020) til januar(2020), mars(2020) til mars(2020)),
        )

        lagForeldelse(
            januar(2020) til januar(2020) to Foreldelsesvurderingstype.IKKE_FORELDET,
            mars(2020) til mars(2020) to Foreldelsesvurderingstype.IKKE_FORELDET,
        )

        vilkårsvurderingService.lagreVilkårsvurdering(behandling.id, godTroVurdering(1.januar(2020) til 31.januar(2020)))
        val oppdatertVilkårsvurdering = vilkårsvurderingService.hentVilkårsvurdering(behandling.id)

        oppdatertVilkårsvurdering.perioder[0].periode shouldBe (1.januar(2020) til 31.januar(2020))
        oppdatertVilkårsvurdering.perioder[0].vilkårsvurderingsresultatInfo shouldNotBe null
        oppdatertVilkårsvurdering.perioder[1].periode shouldBe (1.mars(2020) til 31.mars(2020))
        oppdatertVilkårsvurdering.perioder[1].vilkårsvurderingsresultatInfo shouldBe null
        oppdatertVilkårsvurdering.perioder.size shouldBe 2
    }

    private fun godTroVurdering(periode: Datoperiode): BehandlingsstegVilkårsvurderingDto {
        return BehandlingsstegVilkårsvurderingDto(
            listOf(
                VilkårsvurderingsperiodeDto(
                    periode = periode,
                    vilkårsvurderingsresultat = Vilkårsvurderingsresultat.GOD_TRO,
                    begrunnelse = "kekw",
                    godTroDto = GodTroDto(
                        beløpErIBehold = false,
                        begrunnelse = "lol xD",
                    ),
                    aktsomhetDto = null,
                ),
            ),
        )
    }

    private fun lagBehandlingsstegstilstand(
        behandlingId: UUID,
        behandlingssteg: Behandlingssteg,
        behandlingsstegstatus: Behandlingsstegstatus,
    ) {
        behandlingsstegstilstandRepository.insert(
            Behandlingsstegstilstand(
                behandlingssteg = behandlingssteg,
                behandlingsstegsstatus = behandlingsstegstatus,
                behandlingId = behandlingId,
            ),
        )
    }

    private fun lagKravgrunnlagsbeløp(
        klassetype: Klassetype,
        nyttBeløp: BigDecimal,
        opprinneligUtbetalingsbeløp: BigDecimal,
    ): Kravgrunnlagsbeløp433 = Kravgrunnlagsbeløp433(
        id = UUID.randomUUID(),
        klassetype = klassetype,
        klassekode = Klassekode.BATR,
        opprinneligUtbetalingsbeløp = opprinneligUtbetalingsbeløp,
        nyttBeløp = nyttBeløp,
        tilbakekrevesBeløp = BigDecimal.ZERO,
        skatteprosent = BigDecimal.ZERO,
        uinnkrevdBeløp = BigDecimal.ZERO,
        resultatkode = "testverdi",
        årsakskode = "testverdi",
        skyldkode = "testverdi",
    )

    private fun assertAktiviteter(aktiviteter: List<AktivitetDto>) {
        aktiviteter.shouldNotBeEmpty()
        aktiviteter.size shouldBe 1
        aktiviteter[0].aktivitet shouldBe Klassekode.BATR.aktivitet
    }

    private fun lagVilkårsvurderingMedSimpelAktsomhet(
        andelTilbakekreves: BigDecimal? = null,
        manueltSattBeløp: BigDecimal? = null,
        ileggRenter: Boolean? = null,
        særligGrunn: SærligGrunnDto,
    ): BehandlingsstegVilkårsvurderingDto {
        val periode = VilkårsvurderingsperiodeDto(
            periode = Datoperiode(YearMonth.of(2020, 1), YearMonth.of(2020, 2)),
            vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
            begrunnelse = "Vilkårsvurdering begrunnelse",
            aktsomhetDto =
                AktsomhetDto(
                    aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
                    andelTilbakekreves = andelTilbakekreves,
                    beløpTilbakekreves = manueltSattBeløp,
                    ileggRenter = ileggRenter,
                    begrunnelse = "Aktsomhet begrunnelse",
                    særligeGrunner = listOf(særligGrunn),
                    tilbakekrevSmåbeløp = false,
                    særligeGrunnerBegrunnelse =
                        "Særlig grunner begrunnelse",
                ),
        )
        return BehandlingsstegVilkårsvurderingDto(listOf(periode))
    }

    private fun lagVilkårsvurderingMedGodTro(
        perioder: List<Datoperiode>,
        beløpTilbakekreves: BigDecimal? = null,
    ): BehandlingsstegVilkårsvurderingDto =
        BehandlingsstegVilkårsvurderingDto(
            vilkårsvurderingsperioder = perioder.map {
                VilkårsvurderingsperiodeDto(
                    periode = it,
                    vilkårsvurderingsresultat = Vilkårsvurderingsresultat.GOD_TRO,
                    begrunnelse = "Vilkårsvurdering begrunnelse",
                    godTroDto =
                        GodTroDto(
                            begrunnelse = "God tro begrunnelse",
                            beløpErIBehold = true,
                            beløpTilbakekreves = beløpTilbakekreves,
                        ),
                )
            },
        )

    private fun lagForeldelse(vararg perioder: Pair<Månedsperiode, Foreldelsesvurderingstype>) {
        foreldelseService.lagreVurdertForeldelse(
            behandlingId = behandling.id,
            BehandlingsstegForeldelseDto(
                foreldetPerioder = perioder.map { (periode, vurdering) ->
                    ForeldelsesperiodeDto(
                        periode = periode.toDatoperiode(),
                        begrunnelse = "begrunnelse",
                        foreldelsesvurderingstype = vurdering,
                        foreldelsesfrist = LocalDate.now().takeIf { vurdering == Foreldelsesvurderingstype.FORELDET },
                        oppdagelsesdato = null,
                    )
                },
            ),
            SecureLog.Context.tom(),
        )
    }

    private fun lagBehandling(
        kravgrunnlagPerioder: List<Månedsperiode>,
        faktaperioder: List<Månedsperiode>,
    ): Behandling {
        val fagsak = fagsakRepository.insert(Testdata.fagsak())
        val behandling = behandlingRepository.insert(Testdata.lagBehandling(fagsak.id))

        kravgrunnlagRepository.insert(
            Testdata.lagKravgrunnlag(behandling.id).copy(
                perioder = kravgrunnlagPerioder.map {
                    Testdata.lagKravgrunnlagsperiode(it).copy(
                        beløp = setOf(
                            Testdata.feilKravgrunnlagsbeløp433.copy(id = UUID.randomUUID()),
                            Testdata.ytelKravgrunnlagsbeløp433.copy(id = UUID.randomUUID()),
                        ),
                    )
                }.toSet(),
            ),
        )

        faktaFeilutbetalingRepository.insert(
            FaktaFeilutbetaling(
                behandlingId = behandling.id,
                begrunnelse = "fakta begrunnelse",
                perioder = faktaperioder.map {
                    FaktaFeilutbetalingsperiode(
                        periode = Månedsperiode(it.fom, it.tom),
                        hendelsestype = Hendelsestype.ANNET,
                        hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
                    )
                }.toSet(),
            ),
        )

        lagBehandlingsstegstilstand(behandling.id, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(behandling.id, Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)

        return behandling
    }
}
