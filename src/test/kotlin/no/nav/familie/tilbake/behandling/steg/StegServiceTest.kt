package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.api.dto.BehandlingsstegFaktaDto
import no.nav.familie.tilbake.api.dto.FaktaFeilutbetalingsperiodeDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingService
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsestype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsesundertype
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevsporingRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import kotlin.test.assertEquals
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
    private lateinit var brevsporingRepository: BrevsporingRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var behandlingskontrollService: BehandlingskontrollService

    @Autowired
    private lateinit var faktaFeilutbetalingService: FaktaFeilutbetalingService

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
    fun `håndterSteg skal vente på faktafeilutbetalingssteg for behandling`() {
        behandlingskontrollService.fortsettBehandling(behandlingId)
        brevsporingRepository.insert(Testdata.brevsporing)
        stegService.håndterSteg(behandlingId)

        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)

        assertDoesNotThrow { stegService.håndterSteg(behandlingId) }

        val behandlingsstegstilstander = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertEquals(3, behandlingsstegstilstander.size)
        val aktivtBehandlingssteg = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstander)
        assertEquals(Behandlingssteg.FAKTA, aktivtBehandlingssteg?.behandlingssteg)
        assertEquals(Behandlingsstegstatus.KLAR, aktivtBehandlingssteg?.behandlingsstegsstatus)

    }

    @Test
    fun `håndterSteg skal utføre faktafeilutbetalingssteg for behandling`() {
        behandlingskontrollService.fortsettBehandling(behandlingId)
        brevsporingRepository.insert(Testdata.brevsporing)
        stegService.håndterSteg(behandlingId)

        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)
        stegService.håndterSteg(behandlingId)

        val faktaFeilutbetaltePerioderDto = FaktaFeilutbetalingsperiodeDto(periode = Periode(LocalDate.now().minusMonths(1),
                                                                                             LocalDate.now()),
                                                                           hendelsestype = Hendelsestype.BA_ANNET,
                                                                           hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST)
        assertDoesNotThrow {
            stegService
                    .håndterSteg(behandlingId,
                                 BehandlingsstegFaktaDto(feilutbetaltePerioder = listOf(faktaFeilutbetaltePerioderDto),
                                                         begrunnelse = "testverdi"))
        }

        val behandlingsstegstilstander = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertEquals(5, behandlingsstegstilstander.size)
        val aktivtBehandlingssteg = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstander)
        assertEquals(Behandlingssteg.VILKÅRSVURDERING, aktivtBehandlingssteg?.behandlingssteg)
        assertEquals(Behandlingsstegstatus.KLAR, aktivtBehandlingssteg?.behandlingsstegsstatus)
        assertTrue {
            behandlingsstegstilstander.any {
                Behandlingssteg.FAKTA == it.behandlingssteg &&
                Behandlingsstegstatus.UTFØRT == it.behandlingsstegsstatus
            }
        }
        assertTrue {
            behandlingsstegstilstander.any {
                Behandlingssteg.FORELDELSE == it.behandlingssteg &&
                Behandlingsstegstatus.AUTOUTFØRT == it.behandlingsstegsstatus
            }
        }

        val faktaFeilutbetaling = faktaFeilutbetalingService.hentAktivFaktaOmFeilutbetaling(behandlingId)
        assertNotNull(faktaFeilutbetaling)
        val faktaFeilutbetalingsperioder = faktaFeilutbetaling.perioder.toList()
        assertEquals(1, faktaFeilutbetalingsperioder.size)
        assertEquals(faktaFeilutbetaltePerioderDto.periode, faktaFeilutbetalingsperioder[0].periode)
        assertEquals(faktaFeilutbetaltePerioderDto.hendelsestype, faktaFeilutbetalingsperioder[0].hendelsestype)
        assertEquals(faktaFeilutbetaltePerioderDto.hendelsesundertype, faktaFeilutbetalingsperioder[0].hendelsesundertype)
        assertEquals(faktaFeilutbetaling.begrunnelse, "testverdi")
    }

}
