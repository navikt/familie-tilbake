package no.nav.familie.tilbake.behandling.steg

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockkObject
import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.kontrakter.felles.tilbakekreving.Vergetype
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.api.dto.BehandlingsstegFaktaDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegFatteVedtaksstegDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegForeldelseDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegForeslåVedtaksstegDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegVergeDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegVilkårsvurderingDto
import no.nav.familie.tilbake.api.dto.FaktaFeilutbetalingsperiodeDto
import no.nav.familie.tilbake.api.dto.ForeldelsesperiodeDto
import no.nav.familie.tilbake.api.dto.FritekstavsnittDto
import no.nav.familie.tilbake.api.dto.GodTroDto
import no.nav.familie.tilbake.api.dto.PeriodeDto
import no.nav.familie.tilbake.api.dto.PeriodeMedTekstDto
import no.nav.familie.tilbake.api.dto.VergeDto
import no.nav.familie.tilbake.api.dto.VilkårsvurderingsperiodeDto
import no.nav.familie.tilbake.api.dto.VurdertTotrinnDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.VergeService
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandling.domain.Iverksettingsstatus
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingService
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsestype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsesundertype
import no.nav.familie.tilbake.foreldelse.ForeldelseService
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesvurderingstype
import no.nav.familie.tilbake.historikkinnslag.LagHistorikkinnslagTask
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.iverksettvedtak.task.SendØkonomiTilbakekrevingsvedtakTask
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.oppgave.FerdigstillOppgaveTask
import no.nav.familie.tilbake.oppgave.LagOppgaveTask
import no.nav.familie.tilbake.totrinn.TotrinnsvurderingRepository
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingRepository
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsresultat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class StegServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @Autowired
    private lateinit var totrinnsvurderingRepository: TotrinnsvurderingRepository

    @Autowired
    private lateinit var taskRepository: TaskRepository

    @Autowired
    private lateinit var behandlingskontrollService: BehandlingskontrollService

    @Autowired
    private lateinit var faktaFeilutbetalingService: FaktaFeilutbetalingService

    @Autowired
    private lateinit var foreldelseService: ForeldelseService

    @Autowired
    private lateinit var vergeService: VergeService

    @Autowired
    private lateinit var stegService: StegService

    private val fagsak = Testdata.fagsak
    private val behandling = Testdata.behandling
    private val behandlingId = behandling.id

    @BeforeEach
    fun init() {
        fagsakRepository.insert(fagsak)
        behandlingRepository.insert(behandling)

        mockkObject(ContextService)
        every { ContextService.hentSaksbehandler() }.returns("Z0000")
    }

    @AfterEach
    fun tearDown() {
        clearMocks(ContextService)
    }

    @Test
    fun `håndterSteg skal utføre grunnlagssteg og fortsette til Fakta steg når behandling ikke har verge og har fått grunnlag`() {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandlingRepository.update(behandling.copy(verger = emptySet()))

        lagBehandlingsstegstilstand(Behandlingssteg.GRUNNLAG,
                                    Behandlingsstegstatus.VENTER,
                                    Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG)
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)

        assertDoesNotThrow { stegService.håndterSteg(behandlingId) }
        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)
    }

    @Test
    fun `håndterSteg skal utføre grunnlagssteg,autoutføre verge steg og fortsette til Fakta steg når behandling har verge`() {
        lagBehandlingsstegstilstand(Behandlingssteg.GRUNNLAG,
                                    Behandlingsstegstatus.VENTER,
                                    Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG)
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)

        assertDoesNotThrow { stegService.håndterSteg(behandlingId) }
        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.VERGE, Behandlingsstegstatus.AUTOUTFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)
    }

    @Test
    fun `håndterSteg skal ikke utføre faktafeilutbetaling når behandling er avsluttet`() {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandlingRepository.update(behandling.copy(status = Behandlingsstatus.AVSLUTTET))

        val behandlingsstegFaktaDto = lagBehandlingsstegFaktaDto()

        val exception = assertFailsWith<RuntimeException>(block =
                                                          { stegService.håndterSteg(behandlingId, behandlingsstegFaktaDto) })
        assertEquals("Behandling med id=$behandlingId er allerede ferdig behandlet", exception.message)
    }

    @Test
    fun `håndterSteg skal ikke utføre faktafeilutbetaling når behandling er på vent`() {
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA,
                                    Behandlingsstegstatus.VENTER,
                                    Venteårsak.AVVENTER_DOKUMENTASJON)

        val behandlingsstegFaktaDto = lagBehandlingsstegFaktaDto()

        val exception = assertFailsWith<RuntimeException>(block =
                                                          { stegService.håndterSteg(behandlingId, behandlingsstegFaktaDto) })
        assertEquals("Behandling med id=$behandlingId er på vent, kan ikke behandle steg FAKTA", exception.message)
    }

    @Test
    fun `håndterSteg skal utføre faktafeilutbetalingssteg for behandling`() {
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)

        val behandlingsstegFaktaDto = lagBehandlingsstegFaktaDto()
        assertDoesNotThrow { stegService.håndterSteg(behandlingId, behandlingsstegFaktaDto) }

        val behandlingsstegstilstander = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertEquals(3, behandlingsstegstilstander.size)
        val aktivtBehandlingssteg = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstander)
        assertEquals(Behandlingssteg.VILKÅRSVURDERING, aktivtBehandlingssteg?.behandlingssteg)
        assertEquals(Behandlingsstegstatus.KLAR, aktivtBehandlingssteg?.behandlingsstegsstatus)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        assertBehandlingsstatus(behandlingId, Behandlingsstatus.UTREDES)

        assertFaktadata(behandlingsstegFaktaDto)

        //historikk
        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.FAKTA_VURDERT, Aktør.SAKSBEHANDLER)
        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.FORELDELSE_VURDERT, Aktør.VEDTAKSLØSNING)
    }

    @Test
    fun `håndterSteg skal utføre faktafeilutbetaling og fortsette til vilkårsvurdering når behandling er på foreslåvedtak`() {
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.KLAR)
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)

        val behandlingsstegFaktaDto = lagBehandlingsstegFaktaDto()

        assertDoesNotThrow { stegService.håndterSteg(behandlingId, behandlingsstegFaktaDto) }

        val behandlingsstegstilstander = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertEquals(4, behandlingsstegstilstander.size)
        val aktivtBehandlingssteg = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstander)
        assertEquals(Behandlingssteg.VILKÅRSVURDERING, aktivtBehandlingssteg?.behandlingssteg)
        assertEquals(Behandlingsstegstatus.KLAR, aktivtBehandlingssteg?.behandlingsstegsstatus)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        assertBehandlingssteg(behandlingsstegstilstander,
                              Behandlingssteg.FORESLÅ_VEDTAK,
                              Behandlingsstegstatus.TILBAKEFØRT)
        assertBehandlingsstatus(behandlingId, Behandlingsstatus.UTREDES)

        assertFaktadata(behandlingsstegFaktaDto)

        //historikk
        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.FAKTA_VURDERT, Aktør.SAKSBEHANDLER)
        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.FORELDELSE_VURDERT, Aktør.VEDTAKSLØSNING)
    }

    @Test
    fun `håndterSteg skal utføre faktafeilutbetaling og fortsette til foreldelse når foreldelse ikke er autoutført`() {
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.KLAR)

        var kravgrunnlag431 = Testdata.kravgrunnlag431
        for (grunnlagsperiode in kravgrunnlag431.perioder) {
            kravgrunnlag431 =
                    kravgrunnlag431.copy(perioder =
                                         setOf(grunnlagsperiode.copy(periode = Periode(fom = LocalDate.of(2010, 1, 1),
                                                                                       tom = LocalDate.of(2010, 1, 31)))))
        }
        kravgrunnlagRepository.insert(kravgrunnlag431)
        val behandlingsstegFaktaDto = lagBehandlingsstegFaktaDto()

        assertDoesNotThrow { stegService.håndterSteg(behandlingId, behandlingsstegFaktaDto) }

        val behandlingsstegstilstander = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertEquals(4, behandlingsstegstilstander.size)
        val aktivtBehandlingssteg = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstander)
        assertEquals(Behandlingssteg.FORELDELSE, aktivtBehandlingssteg?.behandlingssteg)
        assertEquals(Behandlingsstegstatus.KLAR, aktivtBehandlingssteg?.behandlingsstegsstatus)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.TILBAKEFØRT)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.TILBAKEFØRT)
        assertBehandlingsstatus(behandlingId, Behandlingsstatus.UTREDES)

        assertFaktadata(behandlingsstegFaktaDto)
        //historikk
        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.FAKTA_VURDERT, Aktør.SAKSBEHANDLER)
    }

    @Test
    fun `håndterSteg skal utføre foreldelse og fortsette til foreslå vedtak når alle perioder er foreldet`() {
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.KLAR)

        var kravgrunnlag431 = Testdata.kravgrunnlag431
        for (grunnlagsperiode in kravgrunnlag431.perioder) {
            kravgrunnlag431 =
                    kravgrunnlag431.copy(perioder =
                                         setOf(grunnlagsperiode.copy(periode = Periode(fom = LocalDate.of(2010, 1, 1),
                                                                                       tom = LocalDate.of(2010, 1, 31)))))
        }
        kravgrunnlagRepository.insert(kravgrunnlag431)
        val behandlingsstegForeldelseDto =
                BehandlingsstegForeldelseDto(listOf(ForeldelsesperiodeDto(PeriodeDto(LocalDate.of(2010, 1, 1),
                                                                                     LocalDate.of(2010, 1, 31)),
                                                                          "foreldelses begrunnelse",
                                                                          Foreldelsesvurderingstype.FORELDET)))
        assertDoesNotThrow { stegService.håndterSteg(behandlingId, behandlingsstegForeldelseDto) }

        val behandlingsstegstilstander = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertEquals(4, behandlingsstegstilstander.size)
        val aktivtBehandlingssteg = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstander)
        assertEquals(Behandlingssteg.FORESLÅ_VEDTAK, aktivtBehandlingssteg?.behandlingssteg)
        assertEquals(Behandlingsstegstatus.KLAR, aktivtBehandlingssteg?.behandlingsstegsstatus)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FORELDELSE, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.AUTOUTFØRT)
        assertBehandlingsstatus(behandlingId, Behandlingsstatus.UTREDES)

        assertForeldelsesdata(behandlingsstegForeldelseDto.foreldetPerioder[0])

        //historikk
        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.FORELDELSE_VURDERT, Aktør.SAKSBEHANDLER)
        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.VILKÅRSVURDERING_VURDERT, Aktør.VEDTAKSLØSNING)
    }

    @Test
    fun `håndterSteg skal utføre foreldelse og fortsette til vilkårsvurdering når minst en periode ikke er foreldet`() {
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.KLAR)

        val førstePeriode = Testdata.kravgrunnlagsperiode432
                .copy(id = UUID.randomUUID(),
                      periode = Periode(fom = LocalDate.of(2018, 1, 1),
                                        tom = LocalDate.of(2018, 1, 31)),
                      beløp = setOf(Testdata.feilKravgrunnlagsbeløp433.copy(id = UUID.randomUUID()),
                                    Testdata.ytelKravgrunnlagsbeløp433.copy(id = UUID.randomUUID())))
        val andrePeriode = Testdata.kravgrunnlagsperiode432
                .copy(id = UUID.randomUUID(),
                      periode = Periode(fom = LocalDate.of(2018, 2, 1),
                                        tom = LocalDate.of(2018, 2, 28)),
                      beløp = setOf(Testdata.feilKravgrunnlagsbeløp433.copy(id = UUID.randomUUID()),
                                    Testdata.ytelKravgrunnlagsbeløp433.copy(id = UUID.randomUUID())))

        val kravgrunnlag431 = Testdata.kravgrunnlag431.copy(perioder = setOf(førstePeriode, andrePeriode))
        kravgrunnlagRepository.insert(kravgrunnlag431)
        val behandlingsstegForeldelseDto =
                BehandlingsstegForeldelseDto(listOf(ForeldelsesperiodeDto(PeriodeDto(førstePeriode.periode),
                                                                          "foreldelses begrunnelse",
                                                                          Foreldelsesvurderingstype.FORELDET),
                                                    ForeldelsesperiodeDto(PeriodeDto(andrePeriode.periode),
                                                                          "foreldelses begrunnelse",
                                                                          Foreldelsesvurderingstype.IKKE_FORELDET)))
        assertDoesNotThrow { stegService.håndterSteg(behandlingId, behandlingsstegForeldelseDto) }

        val behandlingsstegstilstander = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertEquals(3, behandlingsstegstilstander.size)
        val aktivtBehandlingssteg = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstander)
        assertEquals(Behandlingssteg.VILKÅRSVURDERING, aktivtBehandlingssteg?.behandlingssteg)
        assertEquals(Behandlingsstegstatus.KLAR, aktivtBehandlingssteg?.behandlingsstegsstatus)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FORELDELSE, Behandlingsstegstatus.UTFØRT)
        assertBehandlingsstatus(behandlingId, Behandlingsstatus.UTREDES)

        //historikk
        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.FORELDELSE_VURDERT, Aktør.SAKSBEHANDLER)
    }

    @Test
    fun `håndterSteg skal utføre foreldelse og fortsette til foreslå vedtak når alle perioder endret til foreldet`() {
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.KLAR)

        var kravgrunnlag431 = Testdata.kravgrunnlag431
        for (grunnlagsperiode in kravgrunnlag431.perioder) {
            kravgrunnlag431 =
                    kravgrunnlag431.copy(perioder = setOf(grunnlagsperiode.copy(periode = Periode(LocalDate.of(2010, 1, 1),
                                                                                                  LocalDate.of(2010, 1, 31)))))
        }
        kravgrunnlagRepository.insert(kravgrunnlag431)
        // foreldelsesteg vurderte som IKKE_FORELDET med første omgang
        var behandlingsstegForeldelseDto =
                BehandlingsstegForeldelseDto(listOf(ForeldelsesperiodeDto(PeriodeDto(LocalDate.of(2010, 1, 1),
                                                                                     LocalDate.of(2010, 1, 31)),
                                                                          "foreldelses begrunnelse",
                                                                          Foreldelsesvurderingstype.IKKE_FORELDET)))
        assertDoesNotThrow { stegService.håndterSteg(behandlingId, behandlingsstegForeldelseDto) }
        var behandlingsstegstilstander = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.KLAR)

        // behandle vilkårsvurderingssteg
        val behandlingsstegVilkårsvurderingDto = lagBehandlingsstegVilkårsvurderingDto(PeriodeDto(LocalDate.of(2010, 1, 1),
                                                                                                  LocalDate.of(2010, 1, 31)))
        stegService.håndterSteg(behandlingId, behandlingsstegVilkårsvurderingDto)
        behandlingsstegstilstander = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.KLAR)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        assertBehandlingsstatus(behandlingId, Behandlingsstatus.UTREDES)

        // behandler foreldelse steg på nytt og endrer periode til foreldet
        behandlingsstegForeldelseDto =
                BehandlingsstegForeldelseDto(listOf(ForeldelsesperiodeDto(PeriodeDto(LocalDate.of(2010, 1, 1),
                                                                                     LocalDate.of(2010, 1, 31)),
                                                                          "foreldelses begrunnelse",
                                                                          Foreldelsesvurderingstype.FORELDET)))
        stegService.håndterSteg(behandlingId, behandlingsstegForeldelseDto)
        behandlingsstegstilstander = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.KLAR)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.AUTOUTFØRT)
        assertBehandlingsstatus(behandlingId, Behandlingsstatus.UTREDES)

        // deaktiverte tildligere behandlet vilkårsvurdering når alle perioder er foreldet
        assertNull(vilkårsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandlingId))

        //historikk
        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.FORELDELSE_VURDERT, Aktør.SAKSBEHANDLER)
        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.VILKÅRSVURDERING_VURDERT, Aktør.SAKSBEHANDLER)
        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.VILKÅRSVURDERING_VURDERT, Aktør.VEDTAKSLØSNING)
    }

    @Test
    fun `håndterSteg skal utføre foreslå vedtak og forsette til fatte vedtak`() {
        // behandle fakta steg
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)
        val behandlingsstegFaktaDto = lagBehandlingsstegFaktaDto()
        stegService.håndterSteg(behandlingId, lagBehandlingsstegFaktaDto())

        // behandle vilkårsvurderingssteg
        stegService.håndterSteg(behandlingId, lagBehandlingsstegVilkårsvurderingDto(PeriodeDto(LocalDate.of(2021, 1, 1),
                                                                                               LocalDate.of(2021, 1, 31))))

        val fritekstavsnitt =
                FritekstavsnittDto(perioderMedTekst = listOf(PeriodeMedTekstDto(periode = PeriodeDto(LocalDate.of(2021, 1, 1),
                                                                                                     LocalDate.of(2021, 1, 31)),
                                                                                faktaAvsnitt = "fakta tekst")))
        stegService.håndterSteg(behandlingId, BehandlingsstegForeslåVedtaksstegDto(fritekstavsnitt))
        val behandlingsstegstilstander = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FATTE_VEDTAK, Behandlingsstegstatus.KLAR)
        assertBehandlingsstatus(behandlingId, Behandlingsstatus.FATTER_VEDTAK)
        assertFaktadata(behandlingsstegFaktaDto)

        assertOppgave(FerdigstillOppgaveTask.TYPE)
        assertOppgave(LagOppgaveTask.TYPE)

        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.FORESLÅ_VEDTAK_VURDERT, Aktør.SAKSBEHANDLER)
        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.BEHANDLING_SENDT_TIL_BESLUTTER, Aktør.SAKSBEHANDLER)
    }

    @Test
    fun `håndterSteg skal utføre foreslå vedtak på nytt når beslutter underkjente steg og forsette til fatte vedtak`() {
        // behandle fakta steg
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)
        stegService.håndterSteg(behandlingId, lagBehandlingsstegFaktaDto())

        // behandle vilkårsvurderingssteg
        stegService.håndterSteg(behandlingId, lagBehandlingsstegVilkårsvurderingDto(PeriodeDto(LocalDate.of(2021, 1, 1),
                                                                                               LocalDate.of(2021, 1, 31))))

        val fritekstavsnitt =
                FritekstavsnittDto(perioderMedTekst = listOf(PeriodeMedTekstDto(periode = PeriodeDto(LocalDate.of(2021, 1, 1),
                                                                                                     LocalDate.of(2021, 1, 31)),
                                                                                faktaAvsnitt = "fakta tekst")))
        stegService.håndterSteg(behandlingId, BehandlingsstegForeslåVedtaksstegDto(fritekstavsnitt = fritekstavsnitt))

        assertOppgave(FerdigstillOppgaveTask.TYPE)
        assertOppgave(LagOppgaveTask.TYPE)

        stegService.håndterSteg(behandlingId, lagBehandlingsstegFatteVedtaksstegDto(godkjent = false))

        assertOppgave(FerdigstillOppgaveTask.TYPE, 2)
        assertOppgave(LagOppgaveTask.TYPE, 2)

        val behandlingsstegstilstander = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.KLAR)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FATTE_VEDTAK, Behandlingsstegstatus.TILBAKEFØRT)

        stegService.håndterSteg(behandlingId, BehandlingsstegForeslåVedtaksstegDto(fritekstavsnitt = fritekstavsnitt))

        assertOppgave(FerdigstillOppgaveTask.TYPE, 3)
        assertOppgave(LagOppgaveTask.TYPE, 3)

        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.BEHANDLING_SENDT_TILBAKE_TIL_SAKSBEHANDLER, Aktør.BESLUTTER)
    }

    @Test
    fun `håndterSteg skal utføre fatte vedtak og forsette til iverksette vedtak når beslutter godkjenner alt`() {
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)

        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FATTE_VEDTAK, Behandlingsstegstatus.KLAR)

        stegService.håndterSteg(behandlingId, lagBehandlingsstegFatteVedtaksstegDto(godkjent = true))

        val behandlingsstegstilstander = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.IVERKSETT_VEDTAK, Behandlingsstegstatus.KLAR)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FATTE_VEDTAK, Behandlingsstegstatus.UTFØRT)

        assertAnsvarligBeslutter()
        assertBehandlingsstatus(behandlingId, Behandlingsstatus.IVERKSETTER_VEDTAK)

        val totrinnsvurderinger = totrinnsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        assertTrue { totrinnsvurderinger.isNotEmpty() }
        assertTrue { totrinnsvurderinger.any { it.behandlingssteg == Behandlingssteg.FAKTA && it.godkjent } }
        assertFalse { totrinnsvurderinger.any { it.behandlingssteg == Behandlingssteg.FORELDELSE } }
        assertTrue { totrinnsvurderinger.any { it.behandlingssteg == Behandlingssteg.VILKÅRSVURDERING && it.godkjent } }
        assertTrue { totrinnsvurderinger.any { it.behandlingssteg == Behandlingssteg.FORESLÅ_VEDTAK && it.godkjent } }

        assertOppgave(FerdigstillOppgaveTask.TYPE)
        assertHistorikkTask(TilbakekrevingHistorikkinnslagstype.VEDTAK_FATTET, Aktør.BESLUTTER)

        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val behandlingsresultat = behandling.sisteResultat
        assertNotNull(behandlingsresultat)
        assertEquals(Behandlingsresultatstype.INGEN_TILBAKEBETALING, behandlingsresultat.type)
        val behandlingsvedtak = behandlingsresultat.behandlingsvedtak
        assertNotNull(behandlingsvedtak)
        assertEquals(Iverksettingsstatus.UNDER_IVERKSETTING, behandlingsvedtak.iverksettingsstatus)
        assertTrue { taskRepository.findByStatus(Status.UBEHANDLET).any { it.type == SendØkonomiTilbakekrevingsvedtakTask.TYPE } }
    }

    @Test
    fun `håndterSteg skal tilbakeføre fatte vedtak og flytte til foreslå vedtak når beslutter underkjente steg`() {
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FATTE_VEDTAK, Behandlingsstegstatus.KLAR)

        stegService.håndterSteg(behandlingId, lagBehandlingsstegFatteVedtaksstegDto(godkjent = false))

        val behandlingsstegstilstander = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.KLAR)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FATTE_VEDTAK, Behandlingsstegstatus.TILBAKEFØRT)

        assertAnsvarligBeslutter()
        assertBehandlingsstatus(behandlingId, Behandlingsstatus.UTREDES)

        val totrinnsvurderinger = totrinnsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        assertTrue { totrinnsvurderinger.isNotEmpty() }
        assertTrue { totrinnsvurderinger.any { it.behandlingssteg == Behandlingssteg.FAKTA && !it.godkjent } }
        assertFalse { totrinnsvurderinger.any { it.behandlingssteg == Behandlingssteg.FORELDELSE } }
        assertTrue { totrinnsvurderinger.any { it.behandlingssteg == Behandlingssteg.VILKÅRSVURDERING && !it.godkjent } }
        assertTrue { totrinnsvurderinger.any { it.behandlingssteg == Behandlingssteg.FORESLÅ_VEDTAK && !it.godkjent } }

        assertOppgave(FerdigstillOppgaveTask.TYPE)
        assertOppgave(LagOppgaveTask.TYPE)
    }

    @Test
    fun `håndterSteg skal ikke utføre fakta steg når behandling er på fatte vedtak steg`() {
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FATTE_VEDTAK, Behandlingsstegstatus.KLAR)

        stegService.håndterSteg(behandlingId, lagBehandlingsstegFaktaDto())

        val behandlingsstegstilstander = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FATTE_VEDTAK, Behandlingsstegstatus.KLAR)

        assertTrue { totrinnsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandlingId).isEmpty() }
    }

    @Test
    fun `håndterSteg skal ikke utføre fatte vedtak steg når beslutter er samme som saksbehandler`() {
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FATTE_VEDTAK, Behandlingsstegstatus.KLAR)

        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandlingRepository.update(behandling.copy(status = Behandlingsstatus.FATTER_VEDTAK,
                                                    ansvarligSaksbehandler = "Z0000"))

        val exception = assertFailsWith<RuntimeException> {
            stegService.håndterSteg(behandlingId,
                                    lagBehandlingsstegFatteVedtaksstegDto(godkjent = true))
        }

        assertEquals("ansvarlig beslutter kan ikke være samme som ansvarlig saksbehandler", exception.message)
    }

    @Test
    fun `håndterSteg skal opprette og utføre verge steg når behandling er på foreslå vedtak`() {
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.UTFØRT)

        vergeService.opprettVergeSteg(behandlingId)

        var behandlingsstegstilstander = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.TILBAKEFØRT)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.TILBAKEFØRT)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FORELDELSE, Behandlingsstegstatus.TILBAKEFØRT)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FAKTA, Behandlingsstegstatus.TILBAKEFØRT)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.VERGE, Behandlingsstegstatus.KLAR)

        val vergeData = BehandlingsstegVergeDto(verge = VergeDto(ident = "testverdi",
                                                                 type = Vergetype.VERGE_FOR_BARN,
                                                                 navn = "testverdi",
                                                                 begrunnelse = "testverdi"))
        stegService.håndterSteg(behandlingId, vergeData)
        behandlingsstegstilstander = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.TILBAKEFØRT)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.TILBAKEFØRT)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FORELDELSE, Behandlingsstegstatus.TILBAKEFØRT)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.VERGE, Behandlingsstegstatus.UTFØRT)
    }


    @Test
    fun `gjenopptaSteg skal gjenoppta behandling og fortsette til grunnlag når behandling er i varselssteg uten grunnlag`() {
        lagBehandlingsstegstilstand(Behandlingssteg.VARSEL,
                                    Behandlingsstegstatus.VENTER,
                                    Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING)

        stegService.gjenopptaSteg(behandlingId)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        val aktivtBehandlingsstegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingId)
        assertNotNull(aktivtBehandlingsstegstilstand)
        assertEquals(Behandlingssteg.GRUNNLAG, aktivtBehandlingsstegstilstand.behandlingssteg)
        assertEquals(Behandlingsstegstatus.VENTER, aktivtBehandlingsstegstilstand.behandlingsstegsstatus)
        assertBehandlingsstatus(behandlingId, Behandlingsstatus.UTREDES)
        assertEquals(Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG, aktivtBehandlingsstegstilstand.venteårsak)
        assertEquals(LocalDate.now().plusWeeks(Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG.defaultVenteTidIUker),
                     aktivtBehandlingsstegstilstand.tidsfrist)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.VARSEL, Behandlingsstegstatus.UTFØRT)
    }

    @Test
    fun `gjenopptaSteg skal gjenoppta behandling og fortsette til fakta når behandling er i varselssteg med grunnlag`() {
        lagBehandlingsstegstilstand(Behandlingssteg.VARSEL,
                                    Behandlingsstegstatus.VENTER,
                                    Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING)

        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)

        stegService.gjenopptaSteg(behandlingId)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        val aktivtBehandlingsstegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingId)
        assertNotNull(aktivtBehandlingsstegstilstand)
        assertEquals(Behandlingssteg.FAKTA, aktivtBehandlingsstegstilstand.behandlingssteg)
        assertEquals(Behandlingsstegstatus.KLAR, aktivtBehandlingsstegstilstand.behandlingsstegsstatus)
        assertBehandlingsstatus(behandlingId, Behandlingsstatus.UTREDES)

        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.VARSEL, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.VERGE, Behandlingsstegstatus.AUTOUTFØRT)
    }

    @Test
    fun `gjenopptaSteg skal ikke gjenoppta behandling når behandling er i grunnlagssteg uten grunnlag`() {
        lagBehandlingsstegstilstand(Behandlingssteg.GRUNNLAG,
                                    Behandlingsstegstatus.VENTER,
                                    Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG)

        stegService.gjenopptaSteg(behandlingId)

        val aktivtBehandlingsstegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingId)
        assertNotNull(aktivtBehandlingsstegstilstand)
        assertEquals(Behandlingssteg.GRUNNLAG, aktivtBehandlingsstegstilstand.behandlingssteg)
        assertEquals(Behandlingsstegstatus.VENTER, aktivtBehandlingsstegstilstand.behandlingsstegsstatus)
        assertEquals(Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG, aktivtBehandlingsstegstilstand.venteårsak)
        assertEquals(LocalDate.now().plusWeeks(Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG.defaultVenteTidIUker),
                     aktivtBehandlingsstegstilstand.tidsfrist)
    }

    @Test
    fun `gjenopptaSteg skal gjenoppta behandling når behandling er i grunnlagssteg med grunnlag`() {
        lagBehandlingsstegstilstand(Behandlingssteg.GRUNNLAG,
                                    Behandlingsstegstatus.VENTER,
                                    Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG)
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)

        stegService.gjenopptaSteg(behandlingId)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        val aktivtBehandlingsstegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingId)
        assertNotNull(aktivtBehandlingsstegstilstand)
        assertEquals(Behandlingssteg.FAKTA, aktivtBehandlingsstegstilstand.behandlingssteg)
        assertEquals(Behandlingsstegstatus.KLAR, aktivtBehandlingsstegstilstand.behandlingsstegsstatus)
        assertBehandlingsstatus(behandlingId, Behandlingsstatus.UTREDES)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.VERGE, Behandlingsstegstatus.AUTOUTFØRT)
    }

    @Test
    fun `gjenopptaSteg skal gjenoppta behandling når behandling er i vilkårsvurderingssteg`() {
        lagBehandlingsstegstilstand(Behandlingssteg.VILKÅRSVURDERING,
                                    Behandlingsstegstatus.VENTER,
                                    Venteårsak.AVVENTER_DOKUMENTASJON)

        stegService.gjenopptaSteg(behandlingId)

        val aktivtBehandlingsstegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingId)
        assertNotNull(aktivtBehandlingsstegstilstand)
        assertEquals(Behandlingssteg.VILKÅRSVURDERING, aktivtBehandlingsstegstilstand.behandlingssteg)
        assertEquals(Behandlingsstegstatus.KLAR, aktivtBehandlingsstegstilstand.behandlingsstegsstatus)
        assertBehandlingsstatus(behandlingId, Behandlingsstatus.UTREDES)
    }

    private fun lagBehandlingsstegstilstand(behandlingssteg: Behandlingssteg,
                                            behandlingsstegstatus: Behandlingsstegstatus,
                                            venteårsak: Venteårsak? = null) {
        val tidsfrist: LocalDate? = venteårsak?.let { LocalDate.now().plusWeeks(it.defaultVenteTidIUker) }
        behandlingsstegstilstandRepository.insert(Behandlingsstegstilstand(behandlingssteg = behandlingssteg,
                                                                           behandlingsstegsstatus = behandlingsstegstatus,
                                                                           venteårsak = venteårsak,
                                                                           tidsfrist = tidsfrist,
                                                                           behandlingId = behandlingId))
    }

    private fun lagBehandlingsstegFaktaDto(): BehandlingsstegFaktaDto {
        val faktaFeilutbetaltePerioderDto = FaktaFeilutbetalingsperiodeDto(periode = PeriodeDto(LocalDate.of(2021, 1, 1),
                                                                                                LocalDate.of(2021, 1, 31)),
                                                                           hendelsestype = Hendelsestype.ANNET,
                                                                           hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST)
        return BehandlingsstegFaktaDto(feilutbetaltePerioder = listOf(faktaFeilutbetaltePerioderDto),
                                       begrunnelse = "testverdi")
    }

    private fun lagBehandlingsstegVilkårsvurderingDto(periode: PeriodeDto): BehandlingsstegVilkårsvurderingDto {
        return BehandlingsstegVilkårsvurderingDto(listOf(VilkårsvurderingsperiodeDto(periode,
                                                                                     Vilkårsvurderingsresultat.GOD_TRO,
                                                                                     "Vilkårsvurdering begrunnelse",
                                                                                     GodTroDto(false,
                                                                                               null,
                                                                                               "God tro begrunnelse"))))
    }

    private fun lagBehandlingsstegFatteVedtaksstegDto(godkjent: Boolean): BehandlingsstegFatteVedtaksstegDto {
        return BehandlingsstegFatteVedtaksstegDto(listOf(VurdertTotrinnDto(behandlingssteg = Behandlingssteg.FAKTA,
                                                                           godkjent = godkjent,
                                                                           begrunnelse = "fakta totrinn begrunnelse"),
                                                         VurdertTotrinnDto(behandlingssteg = Behandlingssteg.FORELDELSE,
                                                                           godkjent = godkjent,
                                                                           begrunnelse = "foreldelse totrinn begrunnelse"),
                                                         VurdertTotrinnDto(behandlingssteg = Behandlingssteg.VILKÅRSVURDERING,
                                                                           godkjent = godkjent,
                                                                           begrunnelse = "vilkårsvurdering totrinn begrunnelse"),
                                                         VurdertTotrinnDto(behandlingssteg = Behandlingssteg.FORESLÅ_VEDTAK,
                                                                           godkjent = godkjent,
                                                                           begrunnelse = "foreslåvedtak totrinn begrunnelse")))
    }

    private fun assertBehandlingssteg(behandlingsstegstilstand: List<Behandlingsstegstilstand>,
                                      behandlingssteg: Behandlingssteg,
                                      behandlingsstegstatus: Behandlingsstegstatus) {

        assertTrue {
            behandlingsstegstilstand.any {
                behandlingssteg == it.behandlingssteg &&
                behandlingsstegstatus == it.behandlingsstegsstatus
            }
        }
    }

    private fun assertFaktadata(behandlingsstegFaktaDto: BehandlingsstegFaktaDto) {
        val faktaFeilutbetaling = faktaFeilutbetalingService.hentAktivFaktaOmFeilutbetaling(behandlingId)
        assertNotNull(faktaFeilutbetaling)
        val faktaFeilutbetalingsperioder = faktaFeilutbetaling.perioder.toList()
        assertEquals(1, faktaFeilutbetalingsperioder.size)
        val faktaFeilutbetaltePerioderDto = behandlingsstegFaktaDto.feilutbetaltePerioder[0]
        assertEquals(faktaFeilutbetaltePerioderDto.periode, PeriodeDto(faktaFeilutbetalingsperioder[0].periode))
        assertEquals(faktaFeilutbetaltePerioderDto.hendelsestype, faktaFeilutbetalingsperioder[0].hendelsestype)
        assertEquals(faktaFeilutbetaltePerioderDto.hendelsesundertype, faktaFeilutbetalingsperioder[0].hendelsesundertype)
        assertEquals(faktaFeilutbetaling.begrunnelse, "testverdi")
    }

    private fun assertForeldelsesdata(foreldelsesperiodeDto: ForeldelsesperiodeDto) {
        val vurdertForeldelsesdata = foreldelseService.hentVurdertForeldelse(behandlingId)
        assertEquals(1, vurdertForeldelsesdata.foreldetPerioder.size)
        val vurdertForeldetData = vurdertForeldelsesdata.foreldetPerioder[0]
        assertEquals(foreldelsesperiodeDto.begrunnelse, vurdertForeldetData.begrunnelse)
        assertEquals(foreldelsesperiodeDto.foreldelsesvurderingstype, vurdertForeldetData.foreldelsesvurderingstype)
        assertEquals(BigDecimal("10000"), vurdertForeldetData.feilutbetaltBeløp)
        assertEquals(foreldelsesperiodeDto.periode, vurdertForeldetData.periode)
    }

    private fun assertBehandlingsstatus(behandlingId: UUID, behandlingsstatus: Behandlingsstatus) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        assertEquals(behandlingsstatus, behandling.status)
    }

    private fun assertAnsvarligBeslutter() {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        assertEquals("Z0000", behandling.ansvarligBeslutter)
    }

    private fun assertOppgave(tasktype: String, forventet: Int = 1) {
        val taskene = taskRepository.findByStatusIn(status = listOf(Status.KLAR_TIL_PLUKK, Status.UBEHANDLET,
                                                                    Status.BEHANDLER, Status.FERDIG), page = Pageable.unpaged())
                .filter { tasktype == it.type }

        assertEquals(forventet, taskene.size)
    }

    private fun assertHistorikkTask(historikkinnslagstype: TilbakekrevingHistorikkinnslagstype,
                                    aktør: Aktør) {
        assertTrue {
            taskRepository.findByStatus(Status.UBEHANDLET).any {
                LagHistorikkinnslagTask.TYPE == it.type &&
                historikkinnslagstype.name == it.metadata["historikkinnslagstype"] &&
                aktør.name == it.metadata["aktør"] &&
                behandlingId.toString() == it.payload
            }
        }
    }

}
