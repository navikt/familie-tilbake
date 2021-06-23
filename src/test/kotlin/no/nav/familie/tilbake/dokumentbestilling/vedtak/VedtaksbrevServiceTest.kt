package no.nav.familie.tilbake.dokumentbestilling.vedtak

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.api.dto.FritekstavsnittDto
import no.nav.familie.tilbake.api.dto.HentForhåndvisningVedtaksbrevPdfDto
import no.nav.familie.tilbake.api.dto.PeriodeDto
import no.nav.familie.tilbake.api.dto.PeriodeMedTekstDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Verge
import no.nav.familie.tilbake.beregning.TilbakekrevingsberegningService
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.Brevdata
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.PdfBrevService
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingRepository
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingService
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetaling
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetalingsperiode
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsestype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsesundertype
import no.nav.familie.tilbake.foreldelse.VurdertForeldelseRepository
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.pdfgen.validering.PdfaValidator
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingRepository
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.tilbake.vilkårsvurdering.domain.Aktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.SærligGrunn
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurdering
import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingAktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingSærligGrunn
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsperiode
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsresultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
    private lateinit var vilkårsvurderingService: VilkårsvurderingService

    @Autowired
    private lateinit var faktaFeilutbetalingService: FaktaFeilutbetalingService

    @Autowired
    private lateinit var pdfBrevService: PdfBrevService

    private lateinit var spyPdfBrevService: PdfBrevService

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    private val eksterneDataForBrevService: EksterneDataForBrevService = mockk()

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

        val personinfo = Personinfo("28056325874", LocalDate.now(), "Fiona")

        every { eksterneDataForBrevService.hentPerson(Testdata.fagsak.bruker.ident, any()) }.returns(personinfo)
        every { eksterneDataForBrevService.hentSaksbehandlernavn(Testdata.behandling.ansvarligSaksbehandler) }
                .returns("Ansvarlig Saksbehandler")
        every { eksterneDataForBrevService.hentSaksbehandlernavn(Testdata.behandling.ansvarligBeslutter!!) }
                .returns("Ansvarlig Beslutter")
        every {
            eksterneDataForBrevService.hentAdresse(any(), any(), any<Verge>(), any())
        }.returns(Adresseinfo("12345678901", "Test"))
    }

    @Test
    fun `sendVedtaksbrev skal kalle pfdBrevService med behandling, fagsak og genererte brevdata`() {
        val behandlingSlot = slot<Behandling>()
        val fagsakSlot = slot<Fagsak>()
        val brevtypeSlot = slot<Brevtype>()
        val brevdataSlot = slot<Brevdata>()

        vedtaksbrevService.sendVedtaksbrev(Testdata.behandling, Brevmottager.BRUKER)

        verify {
            spyPdfBrevService.sendBrev(capture(behandlingSlot),
                                       capture(fagsakSlot),
                                       capture(brevtypeSlot),
                                       capture(brevdataSlot))
        }
        assertThat(behandlingSlot.captured).isEqualTo(Testdata.behandling)
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
                                                                                "Friktekst om fakta",
                                                                                "Friktekst om foreldelse",
                                                                                "Friktekst om vilkår",
                                                                                "Friktekst om særligeGrunner",
                                                                                "Friktekst om særligeGrunnerAnnet")))

        val bytes = vedtaksbrevService.hentForhåndsvisningVedtaksbrevMedVedleggSomPdf(dto)

        PdfaValidator.validatePdf(bytes)
    }

    @Test
    fun `hentForhåndsvisningVedtaksbrevMedVedleggSomPdf skal generere en gyldig pdf med xml-spesialtegn`() {
        val dto = HentForhåndvisningVedtaksbrevPdfDto(Testdata.behandling.id,
                                                      "Dette er en stor og gild oppsummeringstekst",
                                                      listOf(PeriodeMedTekstDto(PeriodeDto(LocalDate.now().minusDays(1),
                                                                                           LocalDate.now()),
                                                                                faktaAvsnitt = "&bob",
                                                                                vilkårAvsnitt = "<bob>",
                                                                                særligeGrunnerAnnetAvsnitt = "'bob' \"bob\"")))

        val bytes = vedtaksbrevService.hentForhåndsvisningVedtaksbrevMedVedleggSomPdf(dto)

        PdfaValidator.validatePdf(bytes)
    }

    @Test
    fun `hentForhåndsvisningVedtaksbrevSomTekst genererer avsnitt med tekst for forhåndsvisning av vedtaksbrev`() {

        val avsnitt = vedtaksbrevService.hentVedtaksbrevSomTekst(Testdata.behandling.id)

        assertThat(avsnitt).hasSize(3)
        assertThat(avsnitt.first().overskrift).isEqualTo("Du må betale tilbake barnetrygden")
    }

    @Test
    fun `lagreFriteksterFraSaksbehandler skal ikke lagre fritekster når en av de periodene er ugyldig`() {
        lagFakta()
        val perioderMedTekst = listOf(PeriodeMedTekstDto(periode = PeriodeDto(YearMonth.of(2021, 1), YearMonth.of(2021, 3)),
                                                         faktaAvsnitt = "fakta fritekst",
                                                         vilkårAvsnitt = "vilkår fritekst"),
                                      PeriodeMedTekstDto(periode = PeriodeDto(YearMonth.of(2021, 10), YearMonth.of(2021, 10)),
                                                         faktaAvsnitt = "ugyldig",
                                                         vilkårAvsnitt = "ugyldig"))
        val fritekstAvsnittDto = FritekstavsnittDto(oppsummeringstekst = "oppsummeringstekst",
                                                    perioderMedTekst = perioderMedTekst)

        val exception = assertFailsWith<RuntimeException> {
            vedtaksbrevService.lagreFriteksterFraSaksbehandler(behandlingId = behandling.id,
                                                               fritekstAvsnittDto)
        }
        assertEquals("Periode 2021-10-01-2021-10-31 er ugyldig for behandling ${behandling.id}", exception.message)
    }

    @Test
    fun `lagreFriteksterFraSaksbehandler skal ikke lagre fritekster når oppsummeringstekst er for lang`() {
        lagFakta()
        val exception = assertFailsWith<RuntimeException> {
            vedtaksbrevService.lagreFriteksterFraSaksbehandler(behandlingId = behandling.id,
                                                               lagFritekstAvsnittDto("fakta",
                                                                                     RandomStringUtils.random(5000)))
        }
        assertEquals("Oppsummeringstekst er for lang for behandling ${behandling.id}", exception.message)
    }

    @Test
    fun `lagreFriteksterFraSaksbehandler skal ikke lagre når fritekst mangler for ANNET særliggrunner begrunnelse`() {
        lagFakta()
        lagVilkårsvurdering()
        val exception = assertFailsWith<RuntimeException> {
            vedtaksbrevService.lagreFriteksterFraSaksbehandler(behandlingId = behandling.id,
                                                               lagFritekstAvsnittDto("fakta", "fakta data"))
        }
        assertEquals("Mangler ANNET Særliggrunner fritekst for " +
                     "${Periode(YearMonth.of(2021, 1), YearMonth.of(2021, 3))}", exception.message)
    }

    @Test
    fun `lagreFriteksterFraSaksbehandler skal ikke lagre når fritekst mangler for alle fakta perioder`() {
        lagFakta()
        val exception = assertFailsWith<RuntimeException> {
            vedtaksbrevService.lagreFriteksterFraSaksbehandler(behandlingId = behandling.id,
                                                               lagFritekstAvsnittDto())
        }
        assertEquals("Mangler fakta fritekst for alle fakta perioder", exception.message)
    }

    @Test
    fun `lagreFriteksterFraSaksbehandler skal ikke lagre når fritekst mangler for en av fakta perioder`() {
        lagFakta()
        val perioderMedTekst = listOf(PeriodeMedTekstDto(periode = PeriodeDto(YearMonth.of(2021, 1), YearMonth.of(2021, 1)),
                                                         faktaAvsnitt = "fakta fritekst",
                                                         vilkårAvsnitt = "vilkår fritekst"),
                                      PeriodeMedTekstDto(periode = PeriodeDto(YearMonth.of(2021, 2), YearMonth.of(2021, 2)),
                                                         faktaAvsnitt = "fakta fritekst",
                                                         vilkårAvsnitt = "vilkår fritekst"),
                                      PeriodeMedTekstDto(periode = PeriodeDto(YearMonth.of(2021, 3), YearMonth.of(2021, 3)),
                                                         vilkårAvsnitt = "vilkår fritekst"))
        val fritekstAvsnittDto = FritekstavsnittDto(oppsummeringstekst = "oppsummeringstekst",
                                                    perioderMedTekst = perioderMedTekst)

        val exception = assertFailsWith<RuntimeException> {
            vedtaksbrevService.lagreFriteksterFraSaksbehandler(behandlingId = behandling.id,
                                                               fritekstavsnittDto = fritekstAvsnittDto)
        }
        assertEquals("Mangler fakta fritekst for ${LocalDate.of(2021, 3, 1)}-" +
                     "${LocalDate.of(2021, 3, 31)}", exception.message)
    }

    @Test
    fun `lagreFriteksterFraSaksbehandler skal lagre fritekst`() {
        lagFakta()
        lagVilkårsvurdering()

        val fritekstAvsnittDto = lagFritekstAvsnittDto(faktaFritekst = "fakta fritekst",
                                                       oppsummeringstekst = "oppsummering fritekst",
                                                       særligGrunnerAnnetFritekst = "særliggrunner annet fritekst")

        assertDoesNotThrow {
            vedtaksbrevService.lagreFriteksterFraSaksbehandler(behandlingId = behandling.id,
                                                               fritekstavsnittDto = fritekstAvsnittDto)
        }

        val avsnittene = vedtaksbrevService.hentVedtaksbrevSomTekst(behandling.id)
        assertTrue { avsnittene.isNotEmpty() }
        assertEquals(3, avsnittene.size)

        val oppsummeringsavsnitt = avsnittene.firstOrNull { Avsnittstype.OPPSUMMERING == it.avsnittstype }
        assertNotNull(oppsummeringsavsnitt)
        assertEquals(1, oppsummeringsavsnitt.underavsnittsliste.size)
        val oppsummeringsunderavsnitt = oppsummeringsavsnitt.underavsnittsliste[0]
        assertUnderavsnitt(underavsnitt = oppsummeringsunderavsnitt,
                           fritekst = "oppsummering fritekst",
                           fritekstTillatt = true,
                           fritekstPåkrevet = false)

        val periodeAvsnitter = avsnittene.firstOrNull { Avsnittstype.PERIODE == it.avsnittstype }
        assertNotNull(periodeAvsnitter)
        assertEquals(LocalDate.of(2021, 1, 1), periodeAvsnitter.fom)
        assertEquals(LocalDate.of(2021, 3, 31), periodeAvsnitter.tom)

        assertEquals(6, periodeAvsnitter.underavsnittsliste.size)
        val faktaUnderavsnitt = periodeAvsnitter.underavsnittsliste
                .firstOrNull { Underavsnittstype.FAKTA == it.underavsnittstype }
        assertNotNull(faktaUnderavsnitt)
        assertUnderavsnitt(underavsnitt = faktaUnderavsnitt,
                           fritekst = "fakta fritekst",
                           fritekstTillatt = true,
                           fritekstPåkrevet = true)

        val foreldelseUnderavsnitt = periodeAvsnitter.underavsnittsliste
                .firstOrNull { Underavsnittstype.FORELDELSE == it.underavsnittstype }
        assertNull(foreldelseUnderavsnitt) // periodene er ikke foreldet

        val vilkårUnderavsnitt = periodeAvsnitter.underavsnittsliste
                .firstOrNull { Underavsnittstype.VILKÅR == it.underavsnittstype }
        assertNotNull(vilkårUnderavsnitt)
        assertUnderavsnitt(underavsnitt = vilkårUnderavsnitt,
                           fritekst = "vilkår fritekst",
                           fritekstTillatt = true,
                           fritekstPåkrevet = false)

        val særligGrunnerUnderavsnitt = periodeAvsnitter.underavsnittsliste
                .firstOrNull { Underavsnittstype.SÆRLIGEGRUNNER == it.underavsnittstype }
        assertNotNull(særligGrunnerUnderavsnitt)
        assertUnderavsnitt(underavsnitt = særligGrunnerUnderavsnitt,
                           fritekst = "særliggrunner fritekst",
                           fritekstTillatt = true,
                           fritekstPåkrevet = false)

        val særligGrunnerAnnetUnderavsnitt = periodeAvsnitter.underavsnittsliste
                .firstOrNull { Underavsnittstype.SÆRLIGEGRUNNER_ANNET == it.underavsnittstype }
        assertNotNull(særligGrunnerAnnetUnderavsnitt)
        assertUnderavsnitt(underavsnitt = særligGrunnerAnnetUnderavsnitt,
                           fritekst = "særliggrunner annet fritekst",
                           fritekstTillatt = true,
                           fritekstPåkrevet = true)

        val tilleggsavsnitt = avsnittene.firstOrNull { Avsnittstype.TILLEGGSINFORMASJON == it.avsnittstype }
        assertNotNull(tilleggsavsnitt)
    }

    private fun lagFritekstAvsnittDto(faktaFritekst: String? = null,
                                      oppsummeringstekst: String? = null,
                                      særligGrunnerAnnetFritekst: String? = null): FritekstavsnittDto {
        val perioderMedTekst = listOf(PeriodeMedTekstDto(periode = PeriodeDto(YearMonth.of(2021, 1), YearMonth.of(2021, 3)),
                                                         faktaAvsnitt = faktaFritekst,
                                                         vilkårAvsnitt = "vilkår fritekst",
                                                         foreldelseAvsnitt = "foreldelse fritekst",
                                                         særligeGrunnerAvsnitt = "særliggrunner fritekst",
                                                         særligeGrunnerAnnetAvsnitt = særligGrunnerAnnetFritekst))
        return FritekstavsnittDto(oppsummeringstekst = oppsummeringstekst,
                                  perioderMedTekst = perioderMedTekst)
    }

    private fun lagFakta() {

        val faktaFeilutbetaltePerioder =
                setOf(FaktaFeilutbetalingsperiode(periode = Periode(YearMonth.of(2021, 1), YearMonth.of(2021, 3)),
                                                  hendelsestype = Hendelsestype.BA_ANNET,
                                                  hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST))
        faktaFeilutbetalingService.deaktiverEksisterendeFaktaOmFeilutbetaling(behandling.id)
        faktaRepository.insert(FaktaFeilutbetaling(behandlingId = behandling.id,
                                                   begrunnelse = "fakta begrrunnelse",
                                                   perioder = faktaFeilutbetaltePerioder))
    }

    private fun lagVilkårsvurdering() {
        val aktsomhet =
                VilkårsvurderingAktsomhet(aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                                          særligeGrunnerBegrunnelse = "Særlig grunner begrunnelse",
                                          særligeGrunnerTilReduksjon = false,
                                          vilkårsvurderingSærligeGrunner =
                                          setOf(VilkårsvurderingSærligGrunn(særligGrunn = SærligGrunn.ANNET,
                                                                            begrunnelse = "Annet begrunnelse")),
                                          begrunnelse = "aktsomhet begrunnelse")
        val vilkårsvurderingPeriode =
                Vilkårsvurderingsperiode(periode = Periode(YearMonth.of(2021, 1), YearMonth.of(2021, 3)),
                                         vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
                                         begrunnelse = "Vilkårsvurdering begrunnelse",
                                         aktsomhet = aktsomhet)
        vilkårsvurderingService.deaktiverEksisterendeVilkårsvurdering(behandling.id)
        vilkårsvurderingRepository.insert(Vilkårsvurdering(behandlingId = behandling.id,
                                                           perioder = setOf(vilkårsvurderingPeriode)))
    }

    private fun assertUnderavsnitt(underavsnitt: Underavsnitt,
                                   fritekst: String,
                                   fritekstTillatt: Boolean,
                                   fritekstPåkrevet: Boolean) {
        assertEquals(fritekst, underavsnitt.fritekst)
        assertEquals(fritekstTillatt, underavsnitt.fritekstTillatt)
        assertEquals(fritekstPåkrevet, underavsnitt.fritekstPåkrevet)
    }
}
