package no.nav.familie.tilbake.dokumentbestilling.vedtak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsårsak
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Verge
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.DistribusjonshåndteringService
import no.nav.familie.tilbake.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevmetadataUtil
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.Brevdata
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.PdfBrevService
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.ManuellBrevmottakerRepository
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.domene.ManuellBrevmottaker
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingRepository
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingService
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetaling
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetalingsperiode
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.log.LogService
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.pdfgen.validering.PdfaValidator
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingRepository
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurdering
import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingAktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingSærligGrunn
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsperiode
import no.nav.tilbakekreving.api.v1.dto.FritekstavsnittDto
import no.nav.tilbakekreving.api.v1.dto.HentForhåndvisningVedtaksbrevPdfDto
import no.nav.tilbakekreving.api.v1.dto.PeriodeMedTekstDto
import no.nav.tilbakekreving.april
import no.nav.tilbakekreving.februar
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsårsakstype
import no.nav.tilbakekreving.kontrakter.behandling.Saksbehandlingstype
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.kontrakter.brev.MottakerType
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsesundertype
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.periode.Månedsperiode
import no.nav.tilbakekreving.kontrakter.periode.Månedsperiode.Companion.til
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunn
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import no.nav.tilbakekreving.kontrakter.ytelse.Fagsystem
import no.nav.tilbakekreving.kontrakter.ytelse.Ytelsestype
import no.nav.tilbakekreving.mars
import no.nav.tilbakekreving.oktober
import org.apache.commons.lang3.RandomStringUtils
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

internal class VedtaksbrevServiceTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var vedtaksbrevgeneratorService: VedtaksbrevgeneratorService

    @Autowired
    private lateinit var vedtaksbrevgrunnlagService: VedtaksbrevgunnlagService

    @Autowired
    private lateinit var faktaRepository: FaktaFeilutbetalingRepository

    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var vedtaksbrevsoppsummeringRepository: VedtaksbrevsoppsummeringRepository

    @Autowired
    private lateinit var vedtaksbrevsperiodeRepository: VedtaksbrevsperiodeRepository

    @Autowired
    private lateinit var vilkårsvurderingService: VilkårsvurderingService

    @Autowired
    private lateinit var faktaFeilutbetalingService: FaktaFeilutbetalingService

    @Autowired
    private lateinit var pdfBrevService: PdfBrevService

    private lateinit var spyPdfBrevService: PdfBrevService

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var logService: LogService

    private val eksterneDataForBrevService: EksterneDataForBrevService = mockk()

    private lateinit var vedtaksbrevService: VedtaksbrevService

    private lateinit var sendBrevService: DistribusjonshåndteringService

    @Autowired
    private lateinit var brevmetadataUtil: BrevmetadataUtil

    private lateinit var manuellBrevmottakerRepository: ManuellBrevmottakerRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var periodeService: PeriodeService

    @BeforeEach
    fun init() {
        spyPdfBrevService = spyk(pdfBrevService)
        manuellBrevmottakerRepository = mockk(relaxed = true)
        sendBrevService = DistribusjonshåndteringService(
            fagsakRepository = fagsakRepository,
            pdfBrevService = spyPdfBrevService,
            vedtaksbrevgrunnlagService = vedtaksbrevgrunnlagService,
            brevmetadataUtil = brevmetadataUtil,
            manuelleBrevmottakerRepository = manuellBrevmottakerRepository,
        )
        vedtaksbrevService = VedtaksbrevService(
            behandlingRepository,
            vedtaksbrevgeneratorService,
            vedtaksbrevgrunnlagService,
            faktaRepository,
            vilkårsvurderingRepository,
            vedtaksbrevsoppsummeringRepository,
            vedtaksbrevsperiodeRepository,
            spyPdfBrevService,
            sendBrevService,
            periodeService,
            logService,
        )
    }

    @Test
    fun `sendVedtaksbrev skal kalle pfdBrevService med behandling, fagsak og genererte brevdata`() {
        val behandlingSlot = slot<Behandling>()
        val fagsakSlot = slot<Fagsak>()
        val brevtypeSlot = slot<Brevtype>()
        val brevdataSlot = slot<Brevdata>()

        val (fagsak, behandling) = nyBehandling()
        val behandlingUtenVerge = behandling.copy(verger = emptySet())
        vedtaksbrevService.sendVedtaksbrev(behandlingUtenVerge)

        verify {
            spyPdfBrevService.sendBrev(
                behandling = capture(behandlingSlot),
                fagsak = capture(fagsakSlot),
                brevtype = capture(brevtypeSlot),
                data = capture(brevdataSlot),
            )
        }
        behandlingSlot.captured shouldBe behandlingUtenVerge
        fagsakSlot.captured shouldBe fagsak
        brevtypeSlot.captured shouldBe Brevtype.VEDTAK
        brevdataSlot.captured.overskrift shouldBe "Du må betale tilbake barnetrygden"
    }

    @Test
    fun `sendVedtaksbrev skal ikke ha besluttersignatur for behandling uten beslutter (under 4 rettsgebyr)`() {
        val behandlingSlot = slot<Behandling>()
        val brevdataSlot = slot<Brevdata>()

        val (fagsak, behandling) = nyBehandling()
        val efFagsak = fagsak.copy(fagsystem = Fagsystem.EF, ytelsestype = Ytelsestype.OVERGANGSSTØNAD)
        fagsakRepository.update(efFagsak)

        val under4rettsgebyrbehandling = behandling.copy(saksbehandlingstype = Saksbehandlingstype.AUTOMATISK_IKKE_INNKREVING_UNDER_4X_RETTSGEBYR, verger = emptySet())
        behandlingRepository.update(under4rettsgebyrbehandling)

        // setter behandlingsstegstilstand for ikke få falsk positiv. Et brev som ikke er besluttet vil heller ikke ha besluttersignatur (f.eks. forhåndsvisning)
        behandlingsstegstilstandRepository.insert(lagBehandlingstegtilstandIverksetter(under4rettsgebyrbehandling))

        vedtaksbrevService.sendVedtaksbrev(under4rettsgebyrbehandling)

        verify {
            spyPdfBrevService.sendBrev(
                behandling = capture(behandlingSlot),
                fagsak = any(),
                brevtype = any(),
                data = capture(brevdataSlot),
            )
        }

        behandlingSlot.captured shouldBe under4rettsgebyrbehandling
        brevdataSlot.captured.brevtekst shouldNotContain "{venstrejustert}Bob Burger{høyrejustert}Bob Burger"
        brevdataSlot.captured.brevtekst shouldContain "{venstrejustert}Bob Burger{høyrejustert}"
    }

    private fun lagBehandlingstegtilstandIverksetter(under4rettsgebyrbehandling: Behandling) = Behandlingsstegstilstand(
        behandlingId = under4rettsgebyrbehandling.id,
        behandlingssteg = Behandlingssteg.IVERKSETT_VEDTAK,
        behandlingsstegsstatus = Behandlingsstegstatus.KLAR,
    )

    @Test
    fun `sendVedtaksbrev til organisasjon skal sette orgnr som mottakerident i brevdata`() {
        val orgNr = "123456789"
        val brevdataSlot = mutableListOf<Brevdata>()

        val (_, behandling) = nyBehandling()

        every { manuellBrevmottakerRepository.findByBehandlingId(any()) } returns listOf(
            ManuellBrevmottaker(
                type = MottakerType.FULLMEKTIG,
                behandlingId = behandling.id,
                navn = "Organisasjonen v/ advokatfullmektig",
                orgNr = orgNr,
            ),
        )
        vedtaksbrevService.sendVedtaksbrev(behandling.copy(verger = emptySet()))

        verify { spyPdfBrevService.sendBrev(any(), any(), any(), capture(brevdataSlot)) }
        brevdataSlot.last().mottager shouldBe Brevmottager.MANUELL_TILLEGGSMOTTAKER
        brevdataSlot.last().metadata.mottageradresse.ident shouldBe orgNr
    }

    @Test
    fun `hentForhåndsvisningVedtaksbrevMedVedleggSomPdf skal generere en gyldig pdf`() {
        val (_, behandling) = nyBehandling()
        val dto = HentForhåndvisningVedtaksbrevPdfDto(
            behandling.id,
            "Dette er en stor og gild oppsummeringstekst",
            listOf(
                PeriodeMedTekstDto(
                    1.januar(2021) til 31.januar(2021),
                    "Friktekst om fakta",
                    "Friktekst om foreldelse",
                    "Friktekst om vilkår",
                    """Friktekst & > < ' "særligeGrunner""",
                    "Friktekst om særligeGrunnerAnnet",
                ),
            ),
        )

        val bytes = vedtaksbrevService.hentForhåndsvisningVedtaksbrevMedVedleggSomPdf(dto)
        //   File("test.pdf").writeBytes(bytes)

        PdfaValidator.validatePdf(bytes)
    }

    @Test
    fun `hentForhåndsvisningVedtaksbrevMedVedleggSomPdf skal generere en gyldig pdf med xml-spesialtegn`() {
        val (_, behandling) = nyBehandling()
        val bytes = vedtaksbrevService.hentForhåndsvisningVedtaksbrevMedVedleggSomPdf(forhåndvisningDto(behandling.id))

//        File("test.pdf").writeBytes(bytes)
        PdfaValidator.validatePdf(bytes)
    }

    @Test
    fun `hentForhåndsvisningVedtaksbrevSomTekst genererer avsnitt med tekst for forhåndsvisning av vedtaksbrev`() {
        val (_, behandling) = nyBehandling()
        val avsnitt = vedtaksbrevService.hentVedtaksbrevSomTekst(behandling.id)

        avsnitt.shouldHaveSize(3)
        avsnitt.first().overskrift shouldBe "Du må betale tilbake barnetrygden"
    }

    @Test
    fun `lagreFriteksterFraSaksbehandler skal ikke lagre fritekster når en av de periodene er ugyldig`() {
        val (_, behandling) = nyBehandling(
            lagFaktaVurdering = false,
            kravgrunnlagPerioder = listOf(
                januar(2021) til januar(2021),
                februar(2021) til februar(2021),
                mars(2021) til mars(2021),
            ),
        )
        lagFakta(behandling.id, januar(2021) til mars(2021))
        val perioderMedTekst = listOf(
            PeriodeMedTekstDto(
                periode = 1.januar(2021) til 31.mars(2021),
                faktaAvsnitt = "fakta fritekst",
                vilkårAvsnitt = "vilkår fritekst",
            ),
            PeriodeMedTekstDto(
                periode = 1.oktober(2021) til 31.oktober(2021),
                faktaAvsnitt = "ugyldig",
                vilkårAvsnitt = "ugyldig",
            ),
        )
        val fritekstAvsnittDto = FritekstavsnittDto(
            oppsummeringstekst = "oppsummeringstekst",
            perioderMedTekst = perioderMedTekst,
        )

        val exception = shouldThrow<RuntimeException> {
            vedtaksbrevService.lagreFriteksterFraSaksbehandler(
                behandling.id,
                fritekstAvsnittDto,
                SecureLog.Context.tom(),
            )
        }
        exception.message shouldBe "Periode 2021-10-01-2021-10-31 er ugyldig for behandling ${behandling.id}"
    }

    @Test
    fun `lagreFriteksterFraSaksbehandler skal ikke lagre fritekster når oppsummeringstekst er for lang`() {
        val (_, behandling) = nyBehandling(lagFaktaVurdering = false)
        lagFakta(behandling.id, januar(2021) til januar(2021))
        val exception = shouldThrow<RuntimeException> {
            vedtaksbrevService.lagreFriteksterFraSaksbehandler(
                behandling.id,
                lagFritekstAvsnittDto(
                    faktaFritekst = "fakta",
                    oppsummeringstekst = RandomStringUtils.random(5000),
                    periode = 1.januar(2021) til 31.januar(2021),
                ),
                SecureLog.Context.tom(),
            )
        }
        exception.message shouldBe "Oppsummeringstekst er for lang for behandling ${behandling.id}"
    }

    @Test
    fun `lagreFriteksterFraSaksbehandler skal ikke lagre når fritekst mangler for ANNET særliggrunner begrunnelse`() {
        val (_, behandling) = nyBehandling(
            lagFaktaVurdering = false,
            lagVilkårsvurdering = false,
            kravgrunnlagPerioder = listOf(
                januar(2021) til januar(2021),
                februar(2021) til februar(2021),
                mars(2021) til mars(2021),
            ),
        )
        lagFakta(behandling.id, januar(2021) til mars(2021))
        lagVilkårsvurdering(behandling.id, januar(2021) til mars(2021))
        val exception = shouldThrow<RuntimeException> {
            vedtaksbrevService.lagreFriteksterFraSaksbehandler(
                behandling.id,
                lagFritekstAvsnittDto(
                    faktaFritekst = "fakta",
                    oppsummeringstekst = "fakta data",
                    periode = 1.januar(2021) til 31.mars(2021),
                ),
                SecureLog.Context.tom(),
            )
        }
        exception.message shouldBe "Mangler ANNET Særliggrunner fritekst for ${januar(2021) til mars(2021)}"
    }

    @Test
    fun `lagreFriteksterFraSaksbehandler skal ikke lagre når fritekst mangler for alle fakta perioder`() {
        val (_, behandling) = nyBehandling(
            lagFaktaVurdering = false,
            lagVilkårsvurdering = false,
            kravgrunnlagPerioder = listOf(
                januar(2021) til januar(2021),
                februar(2021) til februar(2021),
            ),
        )
        lagFakta(
            behandling.id,
            januar(2021) til januar(2021),
            februar(2021) til februar(2021),
        )
        val exception = shouldThrow<RuntimeException> {
            vedtaksbrevService.lagreFriteksterFraSaksbehandler(
                behandling.id,
                lagFritekstAvsnittDto(periode = 1.januar(2021) til 31.januar(2021)),
                SecureLog.Context.tom(),
            )
        }
        exception.message shouldBe "Mangler fakta fritekst for alle fakta perioder"
    }

    @Test
    fun `lagreFriteksterFraSaksbehandler skal ikke lagre når fritekst mangler for en av fakta perioder`() {
        val (_, behandling) = nyBehandling(
            lagFaktaVurdering = false,
            kravgrunnlagPerioder = listOf(
                januar(2021) til januar(2021),
                februar(2021) til februar(2021),
                mars(2021) til mars(2021),
            ),
        )
        lagFakta(behandling.id, januar(2021) til mars(2021))
        val perioderMedTekst = listOf(
            PeriodeMedTekstDto(
                periode = 1.januar(2021) til 31.januar(2021),
                faktaAvsnitt = "fakta fritekst",
                vilkårAvsnitt = "vilkår fritekst",
            ),
            PeriodeMedTekstDto(
                periode = 1.februar(2021) til 28.februar(2021),
                faktaAvsnitt = "fakta fritekst",
                vilkårAvsnitt = "vilkår fritekst",
            ),
            PeriodeMedTekstDto(
                periode = 1.mars(2021) til 31.mars(2021),
                vilkårAvsnitt = "vilkår fritekst",
            ),
        )
        val fritekstAvsnittDto =
            FritekstavsnittDto(
                oppsummeringstekst = "oppsummeringstekst",
                perioderMedTekst = perioderMedTekst,
            )

        val exception = shouldThrow<RuntimeException> {
            vedtaksbrevService.lagreFriteksterFraSaksbehandler(
                behandlingId = behandling.id,
                fritekstavsnittDto = fritekstAvsnittDto,
                logContext = SecureLog.Context.tom(),
            )
        }
        exception.message shouldBe "Mangler fakta fritekst for ${1.mars(2021)}-${31.mars(2021)}"
    }

    @Test
    fun `lagreFriteksterFraSaksbehandler skal lagre fritekst`() {
        val (_, behandling) = nyBehandling(
            kravgrunnlagPerioder = listOf(
                januar(2021) til januar(2021),
                februar(2021) til februar(2021),
                mars(2021) til mars(2021),
            ),
            lagFaktaVurdering = false,
            lagVilkårsvurdering = false,
        )
        lagFakta(behandling.id, januar(2021) til mars(2021))
        lagVilkårsvurdering(behandling.id, januar(2021) til mars(2021))

        val fritekstAvsnittDto = lagFritekstAvsnittDto(
            faktaFritekst = "fakta fritekst",
            oppsummeringstekst = "oppsummering fritekst",
            særligGrunnerAnnetFritekst = "særliggrunner annet fritekst",
            periode = 1.januar(2021) til 31.mars(2021),
        )

        vedtaksbrevService.lagreFriteksterFraSaksbehandler(
            behandlingId = behandling.id,
            fritekstavsnittDto = fritekstAvsnittDto,
            logContext = SecureLog.Context.tom(),
        )

        val avsnittene = vedtaksbrevService.hentVedtaksbrevSomTekst(behandling.id)
        avsnittene.shouldNotBeEmpty()
        avsnittene.size shouldBe 3

        val oppsummeringsavsnitt = avsnittene.firstOrNull { Avsnittstype.OPPSUMMERING == it.avsnittstype }
        oppsummeringsavsnitt.shouldNotBeNull()
        oppsummeringsavsnitt.underavsnittsliste.size shouldBe 3
        val oppsummeringsunderavsnitt0 = oppsummeringsavsnitt.underavsnittsliste[0]
        assertUnderavsnitt(
            underavsnitt = oppsummeringsunderavsnitt0,
            fritekst = "",
            fritekstTillatt = false,
            fritekstPåkrevet = false,
        )
        val oppsummeringsunderavsnitt1 = oppsummeringsavsnitt.underavsnittsliste[1]
        assertUnderavsnitt(
            underavsnitt = oppsummeringsunderavsnitt1,
            fritekst = "",
            fritekstTillatt = false,
            fritekstPåkrevet = false,
        )
        val oppsummeringsunderavsnitt2 = oppsummeringsavsnitt.underavsnittsliste[2]
        assertUnderavsnitt(
            underavsnitt = oppsummeringsunderavsnitt2,
            fritekst = "oppsummering fritekst",
            fritekstTillatt = true,
            fritekstPåkrevet = false,
        )

        val periodeAvsnitter = avsnittene.firstOrNull { Avsnittstype.PERIODE == it.avsnittstype }
        periodeAvsnitter.shouldNotBeNull()
        periodeAvsnitter.fom shouldBe 1.januar(2021)
        periodeAvsnitter.tom shouldBe 31.mars(2021)

        periodeAvsnitter.underavsnittsliste.size shouldBe 7
        val faktaUnderavsnitt = periodeAvsnitter.underavsnittsliste
            .firstOrNull { Underavsnittstype.FAKTA == it.underavsnittstype }
        faktaUnderavsnitt.shouldNotBeNull()
        assertUnderavsnitt(
            underavsnitt = faktaUnderavsnitt,
            fritekst = "fakta fritekst",
            fritekstTillatt = true,
            fritekstPåkrevet = true,
        )

        val foreldelseUnderavsnitt = periodeAvsnitter.underavsnittsliste
            .firstOrNull { Underavsnittstype.FORELDELSE == it.underavsnittstype }
        foreldelseUnderavsnitt.shouldBeNull() // periodene er ikke foreldet

        val vilkårUnderavsnitter = periodeAvsnitter.underavsnittsliste.filter { Underavsnittstype.VILKÅR == it.underavsnittstype }
        vilkårUnderavsnitter.size shouldBe 2
        val vilkårUnderavsnitt1 = vilkårUnderavsnitter[0]
        assertUnderavsnitt(
            underavsnitt = vilkårUnderavsnitt1,
            fritekst = "",
            fritekstTillatt = false,
            fritekstPåkrevet = false,
        )
        val vilkårUnderavsnitt2 = vilkårUnderavsnitter[1]
        assertUnderavsnitt(
            underavsnitt = vilkårUnderavsnitt2,
            fritekst = "vilkår fritekst",
            fritekstTillatt = true,
            fritekstPåkrevet = false,
        )

        val særligGrunnerUnderavsnitt = periodeAvsnitter.underavsnittsliste
            .firstOrNull { Underavsnittstype.SÆRLIGEGRUNNER == it.underavsnittstype }
        særligGrunnerUnderavsnitt.shouldNotBeNull()
        assertUnderavsnitt(
            underavsnitt = særligGrunnerUnderavsnitt,
            fritekst = "særliggrunner fritekst",
            fritekstTillatt = true,
            fritekstPåkrevet = false,
        )

        val særligGrunnerAnnetUnderavsnitt = periodeAvsnitter.underavsnittsliste
            .firstOrNull { Underavsnittstype.SÆRLIGEGRUNNER_ANNET == it.underavsnittstype }
        særligGrunnerAnnetUnderavsnitt.shouldNotBeNull()
        assertUnderavsnitt(
            underavsnitt = særligGrunnerAnnetUnderavsnitt,
            fritekst = "særliggrunner annet fritekst",
            fritekstTillatt = true,
            fritekstPåkrevet = true,
        )

        val tilleggsavsnitt = avsnittene.firstOrNull { Avsnittstype.TILLEGGSINFORMASJON == it.avsnittstype }
        tilleggsavsnitt.shouldNotBeNull()
    }

    @Test
    fun `lagreUtkastAvFriteksterFraSaksbehandler skal lagre selv når påkrevet fritekst mangler for alle fakta perioder`() {
        val (_, behandling) = nyBehandling(lagFaktaVurdering = false, lagVilkårsvurdering = false)
        lagFakta(behandling.id, januar(2021) til januar(2021))
        lagVilkårsvurdering(behandling.id, januar(2021) til januar(2021))

        val fritekstAvsnittDto = lagFritekstAvsnittDto(
            oppsummeringstekst = "oppsummering fritekst",
            særligGrunnerAnnetFritekst = "særliggrunner annet fritekst",
            periode = 1.januar(2021) til 31.januar(2021),
        )
        vedtaksbrevService.lagreUtkastAvFritekster(
            behandling.id,
            fritekstAvsnittDto,
            SecureLog.Context.tom(),
        )

        val avsnittene = vedtaksbrevService.hentVedtaksbrevSomTekst(behandling.id)
        avsnittene.shouldNotBeEmpty()
        avsnittene.size shouldBe 3
    }

    @Test
    fun `lagreUtkastAvFriteksterFraSaksbehandler skal lagre selv når påkrevet fritekst mangler for ANNET særliggrunner begrunnelse`() {
        val (_, behandling) = nyBehandling(lagFaktaVurdering = false, lagVilkårsvurdering = false)
        lagFakta(behandling.id, januar(2021) til januar(2021))
        lagVilkårsvurdering(behandling.id, januar(2021) til januar(2021))

        val fritekstAvsnittDto = lagFritekstAvsnittDto(
            faktaFritekst = "fakta fritekst",
            oppsummeringstekst = "oppsummering fritekst",
            periode = 1.januar(2021) til 31.januar(2021),
        )

        vedtaksbrevService.lagreUtkastAvFritekster(
            behandlingId = behandling.id,
            fritekstavsnittDto = fritekstAvsnittDto,
            logContext = SecureLog.Context.tom(),
        )

        val avsnittene = vedtaksbrevService.hentVedtaksbrevSomTekst(behandling.id)
        avsnittene.shouldNotBeEmpty()
        avsnittene.size shouldBe 3
    }

    @Test
    fun `lagreUtkastAvFriteksterFraSaksbehandler skal lagre selv når påkrevet fritekst mangler for oppsummering`() {
        val (fagsak, behandling) = nyBehandling()
        val revurdering = behandlingRepository.insert(
            Testdata.lagRevurdering(behandling.id, fagsak.id).copy(
                id = UUID.randomUUID(),
                eksternBrukId = UUID.randomUUID(),
                avsluttetDato = null,
                årsaker =
                    setOf(
                        Behandlingsårsak(
                            originalBehandlingId = behandling.id,
                            type = Behandlingsårsakstype.REVURDERING_OPPLYSNINGER_OM_VILKÅR,
                        ),
                    ),
            ),
        )

        // Er nødt til å opprette kravgrunnlag for revurderingen
        kravgrunnlagRepository.insert(Testdata.lagKravgrunnlag(behandling.id).copy(id = UUID.randomUUID(), behandlingId = revurdering.id, perioder = emptySet()))

        lagFakta(revurdering.id, januar(2021) til januar(2021))
        lagVilkårsvurdering(revurdering.id, januar(2021) til januar(2021))

        val fritekstAvsnittDto = lagFritekstAvsnittDto(
            faktaFritekst = "fakta fritekst",
            særligGrunnerAnnetFritekst = "særliggrunner annet fritekst",
            periode = 1.januar(2021) til 31.januar(2021),
        )

        vedtaksbrevService.lagreUtkastAvFritekster(
            behandlingId = revurdering.id,
            fritekstavsnittDto = fritekstAvsnittDto,
            logContext = SecureLog.Context.tom(),
        )

        val avsnittene = vedtaksbrevService.hentVedtaksbrevSomTekst(revurdering.id)
        avsnittene.shouldNotBeEmpty()
        avsnittene.size shouldBe 3
    }

    @Test
    fun `lagreFriteksterFraSaksbehandler skal ikke lagre fritekster når påkrevet oppsummeringstekst mangler`() {
        val (fagsak, behandling) = nyBehandling()
        val revurdering = behandlingRepository.insert(
            Testdata.lagRevurdering(behandling.id, fagsak.id).copy(
                id = UUID.randomUUID(),
                eksternBrukId = UUID.randomUUID(),
                årsaker =
                    setOf(
                        Behandlingsårsak(
                            originalBehandlingId = behandling.id,
                            type = Behandlingsårsakstype.REVURDERING_OPPLYSNINGER_OM_VILKÅR,
                        ),
                    ),
            ),
        )
        lagFakta(revurdering.id, januar(2021) til januar(2021))
        lagVilkårsvurdering(revurdering.id, januar(2021) til januar(2021))

        val exception = shouldThrow<RuntimeException> {
            vedtaksbrevService.lagreFriteksterFraSaksbehandler(
                revurdering.id,
                lagFritekstAvsnittDto(
                    faktaFritekst = "fakta",
                    særligGrunnerAnnetFritekst = "test",
                    periode = 1.januar(2021) til 31.januar(2021),
                ),
                SecureLog.Context.tom(),
            )
        }
        exception.message shouldBe "oppsummering fritekst påkrevet for revurdering ${revurdering.id}"
    }

    private fun lagFritekstAvsnittDto(
        faktaFritekst: String? = null,
        oppsummeringstekst: String? = null,
        særligGrunnerAnnetFritekst: String? = null,
        periode: Datoperiode,
    ): FritekstavsnittDto {
        return FritekstavsnittDto(
            oppsummeringstekst = oppsummeringstekst,
            perioderMedTekst = listOf(
                PeriodeMedTekstDto(
                    periode = periode,
                    faktaAvsnitt = faktaFritekst,
                    vilkårAvsnitt = "vilkår fritekst",
                    foreldelseAvsnitt = "foreldelse fritekst",
                    særligeGrunnerAvsnitt = "særliggrunner fritekst",
                    særligeGrunnerAnnetAvsnitt = særligGrunnerAnnetFritekst,
                ),
            ),
        )
    }

    private fun lagFakta(
        behandlingId: UUID,
        vararg perioder: Månedsperiode,
    ) {
        faktaRepository.insert(
            FaktaFeilutbetaling(
                behandlingId = behandlingId,
                begrunnelse = "fakta begrrunnelse",
                perioder = perioder.map {
                    FaktaFeilutbetalingsperiode(
                        periode = it,
                        hendelsestype = Hendelsestype.ANNET,
                        hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
                    )
                }.toSet(),
            ),
        )
    }

    private fun lagVilkårsvurdering(
        behandlingId: UUID,
        periode: Månedsperiode,
    ) {
        vilkårsvurderingRepository.insert(
            Vilkårsvurdering(
                behandlingId = behandlingId,
                perioder = setOf(
                    Vilkårsvurderingsperiode(
                        periode = periode,
                        vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
                        begrunnelse = "Vilkårsvurdering begrunnelse",
                        aktsomhet = VilkårsvurderingAktsomhet(
                            aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                            særligeGrunnerBegrunnelse = "Særlig grunner begrunnelse",
                            særligeGrunnerTilReduksjon = false,
                            vilkårsvurderingSærligeGrunner = setOf(
                                VilkårsvurderingSærligGrunn(
                                    særligGrunn = SærligGrunn.ANNET,
                                    begrunnelse = "Annet begrunnelse",
                                ),
                            ),
                            begrunnelse = "aktsomhet begrunnelse",
                        ),
                    ),
                ),
            ),
        )
    }

    fun nyBehandling(
        lagFaktaVurdering: Boolean = true,
        lagVilkårsvurdering: Boolean = true,
        kravgrunnlagPerioder: List<Månedsperiode> = listOf(mars(2021) til mars(2021)),
    ): Pair<Fagsak, Behandling> {
        val fagsak = fagsakRepository.insert(Testdata.fagsak())
        val behandling = behandlingRepository.insert(Testdata.lagBehandling(fagsakId = fagsak.id).copy(avsluttetDato = 20.april(2024)))
        kravgrunnlagRepository.insert(
            Testdata.lagKravgrunnlag(
                behandlingId = behandling.id,
                perioder = kravgrunnlagPerioder.map { Testdata.lagKravgrunnlagsperiode(it) }.toSet(),
            ),
        )
        if (lagVilkårsvurdering) {
            vilkårsvurderingRepository.insert(
                Testdata.lagVilkårsvurdering(
                    behandlingId = behandling.id,
                    perioder = setOf(Testdata.vilkårsperiode(mars(2021) til april(2021), godTro = null)),
                ),
            )
        }
        if (lagFaktaVurdering) {
            faktaRepository.insert(
                Testdata.lagFaktaFeilutbetaling(behandling.id).copy(
                    perioder = setOf(
                        FaktaFeilutbetalingsperiode(
                            periode = mars(2021) til april(2021),
                            hendelsestype = Hendelsestype.ANNET,
                            hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
                        ),
                    ),
                ),
            )
        }

        val personinfo = Personinfo("28056325874", 1.januar(2024), "Fiona")

        every { eksterneDataForBrevService.hentPerson(fagsak.bruker.ident, any(), any()) }.returns(personinfo)
        every { eksterneDataForBrevService.hentSaksbehandlernavn(behandling.ansvarligSaksbehandler) }
            .returns("Ansvarlig O'Saksbehandler")
        every { eksterneDataForBrevService.hentSaksbehandlernavn(behandling.ansvarligBeslutter!!) }
            .returns("Ansvarlig O'Beslutter")
        every { eksterneDataForBrevService.hentAdresse(any(), any(), any<Verge>(), any(), any()) }
            .returns(Adresseinfo("12345678901", "Test"))

        return fagsak to behandling
    }

    private fun assertUnderavsnitt(
        underavsnitt: Underavsnitt,
        fritekst: String,
        fritekstTillatt: Boolean,
        fritekstPåkrevet: Boolean,
    ) {
        underavsnitt.fritekst shouldBe fritekst
        underavsnitt.fritekstTillatt shouldBe fritekstTillatt
        underavsnitt.fritekstPåkrevet shouldBe fritekstPåkrevet
    }

    fun forhåndvisningDto(behandlingId: UUID) = HentForhåndvisningVedtaksbrevPdfDto(
        behandlingId,
        "Dette er en stor og gild oppsummeringstekst",
        listOf(
            PeriodeMedTekstDto(
                1.januar(2024) til 31.januar(2024),
                faktaAvsnitt = "&bob",
                vilkårAvsnitt = "<bob>",
                særligeGrunnerAnnetAvsnitt = "'bob' \"bob\"",
            ),
        ),
    )
}
