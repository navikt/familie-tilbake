package no.nav.familie.tilbake.datavarehus.saksstatistikk

import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import no.nav.familie.kontrakter.felles.Datoperiode
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultat
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import no.nav.familie.tilbake.behandling.domain.Behandlingsvedtak
import no.nav.familie.tilbake.behandling.domain.Iverksettingsstatus
import no.nav.familie.tilbake.beregning.TilbakekrevingsberegningService
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.datavarehus.saksstatistikk.vedtak.UtvidetVilkårsresultat
import no.nav.familie.tilbake.datavarehus.saksstatistikk.vedtak.VedtakPeriode
import no.nav.familie.tilbake.datavarehus.saksstatistikk.vedtak.Vedtaksoppsummering
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
import no.nav.familie.tilbake.kravgrunnlag.domain.Fagområdekode
import no.nav.familie.tilbake.kravgrunnlag.domain.GjelderType
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassekode
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsbeløp433
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsperiode432
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravstatuskode
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingRepository
import no.nav.familie.tilbake.vilkårsvurdering.domain.Aktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingAktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingGodTro
import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingSærligGrunn
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsperiode
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsresultat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class VedtaksoppsummeringServiceTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @Autowired
    private lateinit var foreldelseRepository: VurdertForeldelseRepository

    @Autowired
    private lateinit var faktaFeilutbetalingRepository: FaktaFeilutbetalingRepository

    @Autowired
    private lateinit var beregningService: TilbakekrevingsberegningService

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    private lateinit var vedtaksoppsummeringService: VedtaksoppsummeringService

    private lateinit var behandling: Behandling
    private lateinit var saksnummer: String

    private val periode: Datoperiode = Datoperiode(YearMonth.of(2020, 1), YearMonth.of(2020, 1))

    @BeforeEach
    fun setup() {
        vedtaksoppsummeringService =
            VedtaksoppsummeringService(
                behandlingRepository,
                fagsakRepository,
                vilkårsvurderingRepository,
                foreldelseRepository,
                faktaFeilutbetalingRepository,
                beregningService,
            )

        behandling =
            Testdata.behandling.copy(
                ansvarligSaksbehandler = ANSVARLIG_SAKSBEHANDLER,
                ansvarligBeslutter = ANSVARLIG_BESLUTTER,
                behandlendeEnhet = "8020",
            )
        fagsakRepository.insert(Testdata.fagsak.copy(fagsystem = Fagsystem.EF, ytelsestype = Ytelsestype.OVERGANGSSTØNAD))
        behandling = behandlingRepository.insert(behandling)
        saksnummer = Testdata.fagsak.eksternFagsakId
        lagKravgrunnlag()
        lagFakta()
    }

    @Test
    fun `hentVedtaksoppsummering skal lage oppsummering for foreldelse perioder`() {
        lagForeldelse()
        lagBehandlingVedtak()

        val vedtaksoppsummering: Vedtaksoppsummering = vedtaksoppsummeringService.hentVedtaksoppsummering(behandling.id)

        fellesAssertVedtaksoppsummering(vedtaksoppsummering)
        val vedtakPerioder: List<VedtakPeriode> = vedtaksoppsummering.perioder
        val vedtakPeriode: VedtakPeriode = fellesAssertVedtakPeriode(vedtakPerioder)
        vedtakPeriode.feilutbetaltBeløp shouldBe BigDecimal.valueOf(1000)
        vedtakPeriode.rentebeløp shouldBe BigDecimal.ZERO
        vedtakPeriode.bruttoTilbakekrevingsbeløp shouldBe BigDecimal.ZERO
        vedtakPeriode.aktsomhet shouldBe null
        vedtakPeriode.vilkårsresultat shouldBe UtvidetVilkårsresultat.FORELDET
        vedtakPeriode.harBruktSjetteLedd shouldBe false
        vedtakPeriode.særligeGrunner shouldBe null
    }

    @Test
    fun `hentVedtaksoppsummering skal lage oppsummering for perioder med god tro`() {
        lagVilkårMedGodTro()
        lagBehandlingVedtak()

        val vedtaksoppsummering: Vedtaksoppsummering = vedtaksoppsummeringService.hentVedtaksoppsummering(behandling.id)

        fellesAssertVedtaksoppsummering(vedtaksoppsummering)
        val vedtakPerioder: List<VedtakPeriode> = vedtaksoppsummering.perioder
        val vedtakPeriode: VedtakPeriode = fellesAssertVedtakPeriode(vedtakPerioder)
        vedtakPeriode.feilutbetaltBeløp shouldBe BigDecimal.valueOf(1000)
        vedtakPeriode.rentebeløp shouldBe BigDecimal.ZERO
        vedtakPeriode.bruttoTilbakekrevingsbeløp shouldBe BigDecimal.valueOf(1000)
        vedtakPeriode.aktsomhet shouldBe null
        vedtakPeriode.vilkårsresultat shouldBe UtvidetVilkårsresultat.GOD_TRO
        vedtakPeriode.harBruktSjetteLedd shouldBe false
        vedtakPeriode.særligeGrunner shouldBe null
    }

    @Test
    fun `hentVedtaksoppsummering skal lage oppsummering for perioder med aktsomhet`() {
        lagVilkårMedAktsomhet()
        lagBehandlingVedtak()
        val vedtaksoppsummering: Vedtaksoppsummering = vedtaksoppsummeringService.hentVedtaksoppsummering(behandling.id)
        fellesAssertVedtaksoppsummering(vedtaksoppsummering)
        val vedtakPerioder: List<VedtakPeriode> = vedtaksoppsummering.perioder
        val vedtakPeriode: VedtakPeriode = fellesAssertVedtakPeriode(vedtakPerioder)
        vedtakPeriode.feilutbetaltBeløp shouldBe BigDecimal.valueOf(1000)
        vedtakPeriode.rentebeløp shouldBe BigDecimal.valueOf(100)
        vedtakPeriode.bruttoTilbakekrevingsbeløp shouldBe BigDecimal.valueOf(1100)
        vedtakPeriode.aktsomhet shouldBe Aktsomhet.SIMPEL_UAKTSOMHET
        vedtakPeriode.vilkårsresultat shouldBe UtvidetVilkårsresultat.FORSTO_BURDE_FORSTÅTT
        vedtakPeriode.harBruktSjetteLedd shouldBe false
        vedtakPeriode.særligeGrunner.shouldNotBeNull()
        vedtakPeriode.særligeGrunner?.erSærligeGrunnerTilReduksjon shouldBe false
        vedtakPeriode.særligeGrunner?.særligeGrunner.shouldNotBeEmpty()
    }

    private fun fellesAssertVedtaksoppsummering(vedtaksoppsummering: Vedtaksoppsummering) {
        vedtaksoppsummering.behandlingUuid.shouldNotBeNull()
        vedtaksoppsummering.ansvarligBeslutter shouldBe ANSVARLIG_BESLUTTER
        vedtaksoppsummering.ansvarligSaksbehandler shouldBe ANSVARLIG_SAKSBEHANDLER
        vedtaksoppsummering.behandlendeEnhet.shouldNotBeEmpty()
        vedtaksoppsummering.behandlingOpprettetTidspunkt.shouldNotBeNull()
        vedtaksoppsummering.behandlingOpprettetTidspunkt
        vedtaksoppsummering.behandlingstype shouldBe Behandlingstype.TILBAKEKREVING
        vedtaksoppsummering.erBehandlingManueltOpprettet shouldBe false
        vedtaksoppsummering.referertFagsaksbehandling.shouldNotBeNull()
        vedtaksoppsummering.saksnummer shouldBe saksnummer
        vedtaksoppsummering.vedtakFattetTidspunkt.shouldNotBeNull()
        vedtaksoppsummering.ytelsestype shouldBe Ytelsestype.OVERGANGSSTØNAD
        vedtaksoppsummering.forrigeBehandling shouldBe null
    }

    private fun fellesAssertVedtakPeriode(vedtakPerioder: List<VedtakPeriode>): VedtakPeriode {
        vedtakPerioder.size shouldBe 1
        val vedtakPeriode: VedtakPeriode = vedtakPerioder[0]
        vedtakPeriode.fom shouldBe periode.fom
        vedtakPeriode.tom shouldBe periode.tom
        vedtakPeriode.hendelsestype shouldBe "BOSATT_I_RIKET"
        vedtakPeriode.hendelsesundertype shouldBe "BRUKER_BOR_IKKE_I_NORGE"
        return vedtakPeriode
    }

    private fun lagFakta() {
        val faktaFeilutbetalingPeriode =
            FaktaFeilutbetalingsperiode(
                periode = periode,
                hendelsestype = Hendelsestype.BOSATT_I_RIKET,
                hendelsesundertype = Hendelsesundertype.BRUKER_BOR_IKKE_I_NORGE,
            )
        val faktaFeilutbetaling =
            FaktaFeilutbetaling(
                behandlingId = behandling.id,
                perioder = setOf(faktaFeilutbetalingPeriode),
                begrunnelse = "fakta begrunnelse",
            )

        faktaFeilutbetalingRepository.insert(faktaFeilutbetaling)
    }

    private fun lagForeldelse() {
        val foreldelsePeriode =
            Foreldelsesperiode(
                periode = periode,
                foreldelsesvurderingstype = Foreldelsesvurderingstype.FORELDET,
                begrunnelse = "foreldelse begrunnelse",
                foreldelsesfrist = periode.fom.plusMonths(8),
            )
        val vurdertForeldelse =
            VurdertForeldelse(
                behandlingId = behandling.id,
                foreldelsesperioder = setOf(foreldelsePeriode),
            )

        foreldelseRepository.insert(vurdertForeldelse)
    }

    private fun lagVilkårMedAktsomhet() {
        val særligGrunn =
            VilkårsvurderingSærligGrunn(
                særligGrunn = no.nav.familie.tilbake.vilkårsvurdering.domain.SærligGrunn.STØRRELSE_BELØP,
                begrunnelse = "særlig grunner begrunnelse",
            )
        val vilkårVurderingAktsomhet =
            VilkårsvurderingAktsomhet(
                aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
                ileggRenter = true,
                særligeGrunnerTilReduksjon = false,
                begrunnelse = "aktsomhet begrunnelse",
                vilkårsvurderingSærligeGrunner = setOf(særligGrunn),
            )
        val vilkårVurderingPeriode =
            Vilkårsvurderingsperiode(
                periode = periode,
                vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
                begrunnelse = "vilkår begrunnelse",
                aktsomhet = vilkårVurderingAktsomhet,
            )
        val vilkårVurdering = Testdata.vilkårsvurdering.copy(perioder = setOf(vilkårVurderingPeriode))

        vilkårsvurderingRepository.insert(vilkårVurdering)
    }

    private fun lagVilkårMedGodTro() {
        val vilkårVurderingGodTro =
            VilkårsvurderingGodTro(
                beløpTilbakekreves = BigDecimal.valueOf(1000),
                beløpErIBehold = false,
                begrunnelse = "god tro begrunnelse",
            )
        val vilkårVurderingPeriode =
            Vilkårsvurderingsperiode(
                periode = periode,
                vilkårsvurderingsresultat = Vilkårsvurderingsresultat.GOD_TRO,
                begrunnelse = "vilkår begrunnelse",
                godTro = vilkårVurderingGodTro,
            )
        val vilkårsvurdering = Testdata.vilkårsvurdering.copy(perioder = setOf(vilkårVurderingPeriode))
        vilkårsvurderingRepository.insert(vilkårsvurdering)
    }

    private fun lagBehandlingVedtak() {
        val behandlingVedtak =
            Behandlingsvedtak(
                iverksettingsstatus = Iverksettingsstatus.IVERKSATT,
                vedtaksdato = LocalDate.now(),
            )
        val behandlingsresultat =
            Behandlingsresultat(
                type = Behandlingsresultatstype.FULL_TILBAKEBETALING,
                behandlingsvedtak = behandlingVedtak,
            )

        val behandling = behandling.copy(resultater = setOf(behandlingsresultat))
        behandlingRepository.update(behandling)
    }

    private fun lagKravgrunnlag() {
        val ytelPostering =
            Kravgrunnlagsbeløp433(
                klassekode = Klassekode.EFOG,
                klassetype = Klassetype.YTEL,
                tilbakekrevesBeløp = BigDecimal.valueOf(1000),
                opprinneligUtbetalingsbeløp = BigDecimal.valueOf(1000),
                nyttBeløp = BigDecimal.ZERO,
                skatteprosent = BigDecimal.valueOf(10),
            )
        val feilPostering =
            Kravgrunnlagsbeløp433(
                klassekode = Klassekode.EFOG,
                klassetype = Klassetype.FEIL,
                nyttBeløp = BigDecimal.valueOf(1000),
                skatteprosent = BigDecimal.valueOf(10),
                tilbakekrevesBeløp = BigDecimal.valueOf(1000),
                opprinneligUtbetalingsbeløp = BigDecimal.valueOf(1000),
            )
        val kravgrunnlagPeriode432 =
            Kravgrunnlagsperiode432(
                periode = periode,
                månedligSkattebeløp = BigDecimal.valueOf(100),
                beløp = setOf(feilPostering, ytelPostering),
            )
        val kravgrunnlag431 =
            Kravgrunnlag431(
                behandlingId = behandling.id,
                eksternKravgrunnlagId = 12345L.toBigInteger(),
                vedtakId = 12345L.toBigInteger(),
                behandlingsenhet = "8020",
                bostedsenhet = "8020",
                ansvarligEnhet = "8020",
                fagområdekode = Fagområdekode.EFOG,
                kravstatuskode = Kravstatuskode.NYTT,
                utbetalesTilId = "1234567890",
                utbetIdType = GjelderType.PERSON,
                gjelderVedtakId = "1234567890",
                gjelderType = GjelderType.PERSON,
                kontrollfelt = "2020",
                saksbehandlerId = ANSVARLIG_SAKSBEHANDLER,
                fagsystemId = saksnummer + "100",
                referanse = "1",
                perioder = setOf(kravgrunnlagPeriode432),
            )
        kravgrunnlagRepository.insert(kravgrunnlag431)
    }

    companion object {
        private const val ANSVARLIG_SAKSBEHANDLER = "Z13456"
        private const val ANSVARLIG_BESLUTTER = "Z12456"
    }
}
