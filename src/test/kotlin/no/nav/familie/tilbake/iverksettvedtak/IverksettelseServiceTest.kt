package no.nav.familie.tilbake.iverksettvedtak

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingsvedtakService
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.beregning.TilbakekrevingsberegningService
import no.nav.familie.tilbake.common.exceptionhandler.IntegrasjonException
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.integration.økonomi.DefaultOppdragClient
import no.nav.familie.tilbake.integration.økonomi.OppdragClient
import no.nav.familie.tilbake.iverksettvedtak.domain.KodeResultat
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.familie.tilbake.kontrakter.objectMapper
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.Fagområdekode
import no.nav.familie.tilbake.kravgrunnlag.domain.GjelderType
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassekode
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.familie.tilbake.kravgrunnlag.domain.KodeAksjon
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsbeløp433
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsperiode432
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravstatuskode
import no.nav.familie.tilbake.log.LogService
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingService
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakResponse
import no.nav.tilbakekreving.api.v1.dto.AktsomhetDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegVilkårsvurderingDto
import no.nav.tilbakekreving.api.v1.dto.SærligGrunnDto
import no.nav.tilbakekreving.api.v1.dto.VilkårsvurderingsperiodeDto
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsresultatstype
import no.nav.tilbakekreving.kontrakter.periode.Månedsperiode
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunn
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import no.nav.tilbakekreving.tilbakekrevingsvedtak.vedtak.v1.TilbakekrevingsbelopDto
import no.nav.tilbakekreving.tilbakekrevingsvedtak.vedtak.v1.TilbakekrevingsvedtakDto
import no.nav.tilbakekreving.typer.v1.MmelDto
import no.nav.tilbakekreving.vedtak.IverksettRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.client.RestOperations
import java.math.BigDecimal
import java.math.BigInteger
import java.net.URI
import java.time.YearMonth
import java.util.UUID

internal class IverksettelseServiceTest : OppslagSpringRunnerTest() {
    override val tømDBEtterHverTest = false

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var iverksettRepository: IverksettRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var økonomiXmlSendtRepository: ØkonomiXmlSendtRepository

    @Autowired
    private lateinit var vilkårsvurderingService: VilkårsvurderingService

    @Autowired
    private lateinit var tilbakekrevingsvedtakBeregningService: TilbakekrevingsvedtakBeregningService

    @Autowired
    private lateinit var behandlingVedtakService: BehandlingsvedtakService

    @Autowired
    private lateinit var beregningService: TilbakekrevingsberegningService

    @Autowired
    private lateinit var logService: LogService

    private lateinit var iverksettelseService: IverksettelseService
    private lateinit var oppdragClient: OppdragClient

    private val restOperations: RestOperations = RestTemplateBuilder().build()
    private val wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())

    private lateinit var fagsak: Fagsak
    private lateinit var behandling: Behandling
    private lateinit var behandlingId: UUID
    private val perioder =
        listOf(
            Månedsperiode(YearMonth.of(2021, 1), YearMonth.of(2021, 1)),
            Månedsperiode(YearMonth.of(2021, 2), YearMonth.of(2021, 2)),
        )
    private lateinit var kravgrunnlag431: Kravgrunnlag431

    @BeforeEach
    fun init() {
        fagsak = Testdata.fagsak()
        behandling = Testdata.lagBehandling(fagsakId = fagsak.id)
        behandlingId = behandling.id
        fagsakRepository.insert(fagsak)
        behandlingRepository.insert(behandling)

        kravgrunnlag431 = lagKravgrunnlag()
        lagVilkårsvurdering()

        behandlingVedtakService.opprettBehandlingsvedtak(behandlingId)

        wireMockServer.start()
        oppdragClient = DefaultOppdragClient(restOperations, URI.create(wireMockServer.baseUrl()))

        iverksettelseService =
            IverksettelseService(
                behandlingRepository,
                kravgrunnlagRepository,
                fagsakRepository,
                iverksettRepository,
                tilbakekrevingsvedtakBeregningService,
                behandlingVedtakService,
                oppdragClient,
                logService,
            )
    }

    @AfterEach
    fun tearDown() {
        wireMockServer.resetAll()
        wireMockServer.stop()
    }

    @Test
    fun `sendIverksettVedtak skal sende iverksettvedtak til økonomi for suksess respons`() {
        mockIverksettelseResponse("00", "OK")

        iverksettelseService.sendIverksettVedtak(behandlingId)
        val iverksattVedtak = iverksettRepository.findByBehandlingId(behandlingId)
        iverksattVedtak.shouldNotBeNull()
        assertRequestXml(iverksattVedtak.tilbakekrevingsvedtak)
        assertRespons(iverksattVedtak.kvittering, "00")

        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val aktivBehandlingsresultat = behandling.sisteResultat
        aktivBehandlingsresultat.shouldNotBeNull()
        aktivBehandlingsresultat.type shouldBe Behandlingsresultatstype.FULL_TILBAKEBETALING
    }

    @Test
    fun `sendIverksettVedtak skal sende iverksettvedtak til økonomi for feil respons`() {
        mockIverksettelseResponse("10", "feil")

        val exception = shouldThrow<RuntimeException> { iverksettelseService.sendIverksettVedtak(behandlingId) }

        exception.shouldBeInstanceOf<IntegrasjonException>()
        exception.message shouldBe "Noe gikk galt ved iverksetting av behandling=$behandlingId"
        exception.cause!!.message shouldBe "Fikk feil respons fra økonomi ved iverksetting av behandling=$behandlingId." +
            "Mottatt respons:${objectMapper.writeValueAsString(lagMmmelDto("10", "feil"))}"

        // val økonomiXmlSendt = økonomiXmlSendtRepository.findByBehandlingId(behandlingId)
        // økonomiXmlSendt.shouldBeNull()
        val iverksattVedtak = iverksettRepository.findByBehandlingId(behandlingId)
        iverksattVedtak.shouldBeNull()
    }

    @Test
    fun `sendIverksettVedtak for allerede iverksatt behandling - skal returnere KVITTERING_OK og ikke iverksette`() {
        mockIverksettelseResponse("08", "B441012F") // Denne kan håndteres dersom oppdrag skiller på om vedtak finnes eller om det er feil status

        val exception = shouldThrow<RuntimeException> { iverksettelseService.sendIverksettVedtak(behandlingId) }
        exception.shouldBeInstanceOf<IntegrasjonException>()
    }

    private fun mockIverksettelseResponse(
        alvorlighetsgrad: String,
        kodeMelding: String,
    ) {
        wireMockServer.stubFor(
            WireMock
                .post(WireMock.urlEqualTo("/${DefaultOppdragClient.IVERKSETTELSE_PATH}/$behandlingId"))
                .willReturn(
                    WireMock.okJson(
                        Ressurs
                            .success(
                                lagRespons(
                                    alvorlighetsgrad,
                                    kodeMelding,
                                ),
                            ).toJson(),
                    ),
                ),
        )
    }

    private fun lagKravgrunnlag(): Kravgrunnlag431 {
        val feilPostering =
            lagKravgrunnlagsbeløp(
                klassetype = Klassetype.FEIL,
                klassekode = Klassekode.KL_KODE_FEIL_BA,
                nyttBeløp = BigDecimal(5000),
            )

        val ytelPostering =
            lagKravgrunnlagsbeløp(
                klassetype = Klassetype.YTEL,
                klassekode = Klassekode.BATR,
                utbetaltBeløp = BigDecimal(5000),
                tilbakekrevesBeløp = BigDecimal(5000),
            )

        val kravgrunnlagsperioder =
            perioder
                .map {
                    Kravgrunnlagsperiode432(
                        periode = it,
                        månedligSkattebeløp = BigDecimal.ZERO,
                        beløp =
                            setOf(
                                feilPostering.copy(id = UUID.randomUUID()),
                                ytelPostering.copy(id = UUID.randomUUID()),
                            ),
                    )
                }.toSet()

        val kravgrunnlag =
            Kravgrunnlag431(
                behandlingId = behandlingId,
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
                perioder = kravgrunnlagsperioder,
            )
        kravgrunnlagRepository.insert(kravgrunnlag)

        return kravgrunnlag
    }

    private fun lagKravgrunnlagsbeløp(
        klassetype: Klassetype,
        klassekode: Klassekode,
        nyttBeløp: BigDecimal = BigDecimal.ZERO,
        utbetaltBeløp: BigDecimal = BigDecimal.ZERO,
        tilbakekrevesBeløp: BigDecimal = BigDecimal.ZERO,
    ): Kravgrunnlagsbeløp433 =
        Kravgrunnlagsbeløp433(
            klassetype = klassetype,
            klassekode = klassekode,
            nyttBeløp = nyttBeløp,
            opprinneligUtbetalingsbeløp = utbetaltBeløp,
            tilbakekrevesBeløp = tilbakekrevesBeløp,
            skatteprosent = BigDecimal.ZERO,
        )

    private fun lagVilkårsvurdering() {
        val vilkårsperioder =
            perioder.map {
                VilkårsvurderingsperiodeDto(
                    periode = it.toDatoperiode(),
                    begrunnelse = "testverdi",
                    aktsomhetDto =
                        AktsomhetDto(
                            aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                            begrunnelse = "testverdi",
                            særligeGrunnerTilReduksjon = false,
                            tilbakekrevSmåbeløp = true,
                            særligeGrunnerBegrunnelse = "testverdi",
                            særligeGrunner =
                                listOf(
                                    SærligGrunnDto(
                                        særligGrunn = SærligGrunn.ANNET,
                                        begrunnelse = "testverdi",
                                    ),
                                ),
                        ),
                    vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
                )
            }
        vilkårsvurderingService.lagreVilkårsvurdering(behandling.id, BehandlingsstegVilkårsvurderingDto(vilkårsperioder))
    }

    private fun lagRespons(
        alvorlighetsgrad: String,
        kodeMelding: String,
    ): TilbakekrevingsvedtakResponse {
        val mmelDto = lagMmmelDto(alvorlighetsgrad, kodeMelding)

        val respons = TilbakekrevingsvedtakResponse()
        respons.mmel = mmelDto
        respons.tilbakekrevingsvedtak = TilbakekrevingsvedtakDto()

        return respons
    }

    private fun lagMmmelDto(
        alvorlighetsgrad: String,
        kodeMelding: String,
    ): MmelDto {
        val mmelDto = MmelDto()
        mmelDto.alvorlighetsgrad = alvorlighetsgrad
        mmelDto.kodeMelding = kodeMelding
        return mmelDto
    }

    private fun assertRespons(
        kvittering: String?,
        alvorlighetsgrad: String,
    ) {
        kvittering.shouldNotBeEmpty()
        kvittering shouldBe alvorlighetsgrad
    }

    private fun assertRequestXml(
        tilbakekrevingsvedtak: TilbakekrevingsvedtakDto,
    ) {
        tilbakekrevingsvedtak.kodeAksjon shouldBe KodeAksjon.FATTE_VEDTAK.kode
        tilbakekrevingsvedtak.datoVedtakFagsystem.shouldNotBeNull()
        tilbakekrevingsvedtak.vedtakId shouldBe BigInteger.ZERO
        tilbakekrevingsvedtak.kodeHjemmel shouldBe "22-15"
        tilbakekrevingsvedtak.enhetAnsvarlig shouldBe kravgrunnlag431.ansvarligEnhet

        val førstePeriode = tilbakekrevingsvedtak.tilbakekrevingsperiode[0]
        førstePeriode.periode.shouldNotBeNull()
        førstePeriode.belopRenter shouldBe BigDecimal.ZERO
        førstePeriode.tilbakekrevingsbelop.size shouldBe 2
        assertBeløp(
            beløpene = førstePeriode.tilbakekrevingsbelop,
            klassekode = Klassekode.KL_KODE_FEIL_BA,
            nyttBeløp = BigDecimal(5000),
        )
        assertBeløp(
            beløpene = førstePeriode.tilbakekrevingsbelop,
            klassekode = Klassekode.BATR,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(5000),
            kodeResultat = KodeResultat.FULL_TILBAKEKREVING,
        )

        val andrePeriode = tilbakekrevingsvedtak.tilbakekrevingsperiode[0]
        andrePeriode.periode.shouldNotBeNull()
        andrePeriode.belopRenter shouldBe BigDecimal.ZERO
        andrePeriode.tilbakekrevingsbelop.size shouldBe 2
        assertBeløp(
            beløpene = andrePeriode.tilbakekrevingsbelop,
            klassekode = Klassekode.KL_KODE_FEIL_BA,
            nyttBeløp = BigDecimal(5000),
        )
        assertBeløp(
            beløpene = andrePeriode.tilbakekrevingsbelop,
            klassekode = Klassekode.BATR,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(5000),
            kodeResultat = KodeResultat.FULL_TILBAKEKREVING,
        )
    }

    private fun assertBeløp(
        beløpene: List<TilbakekrevingsbelopDto>,
        klassekode: Klassekode,
        nyttBeløp: BigDecimal = BigDecimal.ZERO,
        utbetaltBeløp: BigDecimal = BigDecimal.ZERO,
        tilbakekrevesBeløp: BigDecimal = BigDecimal.ZERO,
        uinnkrevdBeløp: BigDecimal = BigDecimal.ZERO,
        skattBeløp: BigDecimal = BigDecimal.ZERO,
        kodeResultat: KodeResultat? = null,
    ) {
        beløpene
            .any {
                klassekode.name == it.kodeKlasse &&
                    nyttBeløp == it.belopNy &&
                    utbetaltBeløp == it.belopOpprUtbet &&
                    tilbakekrevesBeløp == it.belopTilbakekreves &&
                    uinnkrevdBeløp == it.belopUinnkrevd
                skattBeløp == it.belopSkatt
            }.shouldBeTrue()

        beløpene.any {
            kodeResultat?.kode == it.kodeResultat &&
                "ANNET" == it.kodeAarsak &&
                "IKKE_FORDELT" == it.kodeSkyld
        }
    }
}
