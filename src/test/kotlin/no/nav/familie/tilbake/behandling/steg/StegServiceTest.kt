package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.api.dto.BehandlingsstegFaktaDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegForeldelseDto
import no.nav.familie.tilbake.api.dto.FaktaFeilutbetalingsperiodeDto
import no.nav.familie.tilbake.api.dto.ForeldelsesperiodeDto
import no.nav.familie.tilbake.api.dto.PeriodeDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingService
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsestype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsesundertype
import no.nav.familie.tilbake.foreldelse.ForeldelseService
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesvurderingstype
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
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
    private lateinit var behandlingskontrollService: BehandlingskontrollService

    @Autowired
    private lateinit var faktaFeilutbetalingService: FaktaFeilutbetalingService

    @Autowired
    private lateinit var foreldelseService: ForeldelseService

    @Autowired
    private lateinit var stegService: StegService

    private val fagsak = Testdata.fagsak
    private val behandling = Testdata.behandling
    private val behandlingId = behandling.id

    @BeforeEach
    fun init() {
        fagsakRepository.insert(fagsak)
        behandlingRepository.insert(behandling)
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

        assertFaktadata(behandlingsstegFaktaDto)
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
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.TILBAKEFØRT)

        assertFaktadata(behandlingsstegFaktaDto)
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
                    kravgrunnlag431.copy(perioder = setOf(grunnlagsperiode.copy(
                            periode = Periode(fom = LocalDate.of(2010, 1, 1),
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

        assertFaktadata(behandlingsstegFaktaDto)
    }

    @Test
    fun `håndterSteg skal utføre foreldelse og fortsette til vilkårsvurdering`() {
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.KLAR)

        var kravgrunnlag431 = Testdata.kravgrunnlag431
        for (grunnlagsperiode in kravgrunnlag431.perioder) {
            kravgrunnlag431 =
                    kravgrunnlag431.copy(perioder = setOf(grunnlagsperiode.copy(
                            periode = Periode(fom = LocalDate.of(2010, 1, 1),
                                              tom = LocalDate.of(2010, 1, 31)))))
        }
        kravgrunnlagRepository.insert(kravgrunnlag431)
        val behandlingsstegForeldelseDto = BehandlingsstegForeldelseDto(
                foreldetPerioder = listOf(ForeldelsesperiodeDto(periode = PeriodeDto(LocalDate.of(2010, 1, 1),
                                                                                     LocalDate.of(2010, 1, 31)),
                                                                begrunnelse = "foreldelses begrunnelse",
                                                                foreldelsesvurderingstype = Foreldelsesvurderingstype.FORELDET)))
        assertDoesNotThrow { stegService.håndterSteg(behandlingId, behandlingsstegForeldelseDto) }

        val behandlingsstegstilstander = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertEquals(3, behandlingsstegstilstander.size)
        val aktivtBehandlingssteg = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstander)
        assertEquals(Behandlingssteg.VILKÅRSVURDERING, aktivtBehandlingssteg?.behandlingssteg)
        assertEquals(Behandlingsstegstatus.KLAR, aktivtBehandlingssteg?.behandlingsstegsstatus)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstander, Behandlingssteg.FORELDELSE, Behandlingsstegstatus.UTFØRT)

        assertForeldelsesdata(behandlingsstegForeldelseDto.foreldetPerioder[0])
    }

    @Test
    fun `gjenopptaSteg skal gjenoppta behandling når behandling er i varselssteg`() {
        lagBehandlingsstegstilstand(Behandlingssteg.VARSEL,
                                    Behandlingsstegstatus.VENTER,
                                    Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING)

        stegService.gjenopptaSteg(behandlingId)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        val aktivtBehandlingsstegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingId)
        assertNotNull(aktivtBehandlingsstegstilstand)
        assertEquals(Behandlingssteg.GRUNNLAG, aktivtBehandlingsstegstilstand.behandlingssteg)
        assertEquals(Behandlingsstegstatus.VENTER, aktivtBehandlingsstegstilstand.behandlingsstegsstatus)
        assertEquals(Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG, aktivtBehandlingsstegstilstand.venteårsak)
        assertEquals(LocalDate.now().plusWeeks(Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG.defaultVenteTidIUker),
                     aktivtBehandlingsstegstilstand.tidsfrist)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.VARSEL, Behandlingsstegstatus.UTFØRT)
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
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
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
                                                                           hendelsestype = Hendelsestype.BA_ANNET,
                                                                           hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST)
        return BehandlingsstegFaktaDto(feilutbetaltePerioder = listOf(faktaFeilutbetaltePerioderDto),
                                       begrunnelse = "testverdi")
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

}
