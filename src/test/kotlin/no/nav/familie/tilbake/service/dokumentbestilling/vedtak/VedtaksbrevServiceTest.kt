package no.nav.familie.tilbake.service.dokumentbestilling.vedtak

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.api.dto.HentForhåndvisningVedtaksbrevPdfDto
import no.nav.familie.tilbake.api.dto.PeriodeDto
import no.nav.familie.tilbake.api.dto.PeriodeMedTekstDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Verge
import no.nav.familie.tilbake.beregning.TilbakekrevingsberegningService
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingRepository
import no.nav.familie.tilbake.foreldelse.VurdertForeldelseRepository
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.repository.tbd.VedtaksbrevsoppsummeringRepository
import no.nav.familie.tilbake.repository.tbd.VedtaksbrevsperiodeRepository
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.service.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.service.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.service.dokumentbestilling.felles.pdf.Brevdata
import no.nav.familie.tilbake.service.dokumentbestilling.felles.pdf.PdfBrevService
import no.nav.familie.tilbake.service.pdfgen.validering.PdfaValidator
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

internal class VedtaksbrevServiceTest : OppslagSpringRunnerTest() {


    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var faktaRepository: FaktaFeilutbetalingRepository

    @Autowired
    private lateinit var foreldelseRepository: VurdertForeldelseRepository

    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var vedtaksbrevsoppsummeringRepository: VedtaksbrevsoppsummeringRepository

    @Autowired
    private lateinit var vedtaksbrevsperiodeRepository: VedtaksbrevsperiodeRepository

    @Autowired
    private lateinit var brevSporingRepository: BrevsporingRepository

    @Autowired
    private lateinit var tilbakekrevingBeregningService: TilbakekrevingsberegningService

    @Autowired
    private lateinit var pdfBrevService: PdfBrevService

    private lateinit var spyPdfBrevService: PdfBrevService

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    private val eksterneDataForBrevService: EksterneDataForBrevService = mockk(relaxed = true)

    private lateinit var vedtaksbrevService: VedtaksbrevService

    private lateinit var behandling: Behandling
    private lateinit var fagsak: Fagsak

    @BeforeEach
    fun init() {
        spyPdfBrevService = spyk(pdfBrevService)
        vedtaksbrevService = VedtaksbrevService(behandlingRepository,
                                                faktaRepository,
                                                foreldelseRepository,
                                                vilkårsvurderingRepository,
                                                fagsakRepository,
                                                vedtaksbrevsoppsummeringRepository,
                                                vedtaksbrevsperiodeRepository,
                                                brevSporingRepository,
                                                tilbakekrevingBeregningService,
                                                eksterneDataForBrevService,
                                                spyPdfBrevService)

        fagsak = fagsakRepository.insert(Testdata.fagsak)
        behandling = behandlingRepository.insert(Testdata.behandling)
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)
        vilkårsvurderingRepository.insert(Testdata.vilkår.copy(perioder = setOf(Testdata.vilkårsperiode.copy(godTro = null))))
        faktaRepository.insert(Testdata.faktaFeilutbetaling)
        brevSporingRepository.insert(Testdata.brevsporing)

        val personinfo = Personinfo("28056325874", LocalDate.now(), "Fiona")

        every { eksterneDataForBrevService.hentPerson(Testdata.fagsak.bruker.ident, any()) }.returns(personinfo)
        every {
            eksterneDataForBrevService.hentAdresse(any(), any(), any<Verge>(), any())
        }.returns(Adresseinfo("Test", "12345678901"))
    }

    @Test
    fun `sendVedtaksbrev skal kalle pfdBrevService med behandling, fagsak og genererte brevdata`() {
        val behandlingSlot = slot<Behandling>()
        val fagsakSlot = slot<Fagsak>()
        val brevtypeSlot = slot<Brevtype>()
        val brevdataSlot = slot<Brevdata>()

        vedtaksbrevService.sendVedtaksbrev(Testdata.behandling.id, Brevmottager.BRUKER)

        verify {
            spyPdfBrevService.sendBrev(capture(behandlingSlot),
                                       capture(fagsakSlot),
                                       capture(brevtypeSlot),
                                       capture(brevdataSlot))
        }
        assertThat(behandlingSlot.captured).isEqualTo(behandling)
        assertThat(fagsakSlot.captured).isEqualTo(fagsak)
        assertThat(brevtypeSlot.captured).isEqualTo(Brevtype.VEDTAK)
        assertThat(brevdataSlot.captured.overskrift).isEqualTo("Du må betale tilbake barnetrygden")
    }

    @Test
    fun `hentForhåndsvisningVedtaksbrevMedVedleggSomPdf skal generere en gyldig pdf`() {
        val dto = HentForhåndvisningVedtaksbrevPdfDto(Testdata.behandling.id,
                                                      "Dette er en stor og gild oppsummeringstekst",
                                                      listOf(PeriodeMedTekstDto(PeriodeDto(LocalDate.now().minusDays(1),
                                                                                           LocalDate.now()),
                                                                                faktaAvsnitt = "fakta")))

        val bytes = vedtaksbrevService.hentForhåndsvisningVedtaksbrevMedVedleggSomPdf(dto)

        PdfaValidator.validatePdf(bytes)
    }

    @Test
    fun `hentForhåndsvisningVedtaksbrevSomTekst genererer avsnitt med tekst for forhåndsvisning av vedtaksbrev`() {

        val avsnitt = vedtaksbrevService.hentForhåndsvisningVedtaksbrevSomTekst(Testdata.behandling.id)

        assertThat(avsnitt).hasSize(3)
        assertThat(avsnitt.first().overskrift).isEqualTo("Du må betale tilbake barnetrygden")
    }
}
