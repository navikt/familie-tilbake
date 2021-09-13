package no.nav.familie.tilbake.iverksettvedtak

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.api.dto.AktsomhetDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegVilkårsvurderingDto
import no.nav.familie.tilbake.api.dto.PeriodeDto
import no.nav.familie.tilbake.api.dto.SærligGrunnDto
import no.nav.familie.tilbake.api.dto.VilkårsvurderingsperiodeDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingsvedtakService
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.common.exceptionhandler.IntegrasjonException
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.integration.økonomi.DefaultOppdragClient
import no.nav.familie.tilbake.integration.økonomi.OppdragClient
import no.nav.familie.tilbake.iverksettvedtak.domain.KodeResultat
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
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.tilbake.vilkårsvurdering.domain.Aktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.SærligGrunn
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsresultat
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakResponse
import no.nav.tilbakekreving.tilbakekrevingsvedtak.vedtak.v1.TilbakekrevingsbelopDto
import no.nav.tilbakekreving.tilbakekrevingsvedtak.vedtak.v1.TilbakekrevingsvedtakDto
import no.nav.tilbakekreving.typer.v1.MmelDto
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.client.RestOperations
import java.math.BigDecimal
import java.math.BigInteger
import java.net.URI
import java.time.YearMonth
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class IverksettelseServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

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

    private lateinit var iverksettelseService: IverksettelseService
    private lateinit var oppdragClient: OppdragClient

    private val restOperations: RestOperations = RestTemplateBuilder().build()
    private val wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())

    private val fagsak = Testdata.fagsak
    private val behandling = Testdata.behandling
    private val behandlingId = behandling.id
    private val perioder = listOf(Periode(YearMonth.of(2021, 1), YearMonth.of(2021, 1)),
                                  Periode(YearMonth.of(2021, 2), YearMonth.of(2021, 2)))
    private lateinit var kravgrunnlag431: Kravgrunnlag431

    @BeforeEach
    fun init() {
        fagsakRepository.insert(fagsak)
        behandlingRepository.insert(behandling)

        kravgrunnlag431 = lagKravgrunnlag()
        lagVilkårsvurdering()

        behandlingVedtakService.opprettBehandlingsvedtak(behandlingId)

        wireMockServer.start()
        oppdragClient = DefaultOppdragClient(restOperations, URI.create(wireMockServer.baseUrl()))

        iverksettelseService = IverksettelseService(behandlingRepository,
                                                    kravgrunnlagRepository,
                                                    økonomiXmlSendtRepository,
                                                    tilbakekrevingsvedtakBeregningService,
                                                    behandlingVedtakService,
                                                    oppdragClient)
    }

    @AfterEach
    fun tearDown() {
        wireMockServer.resetAll()
        wireMockServer.stop()
    }

    @Test
    fun `sendIverksettVedtak skal sende iverksettvedtak til økonomi for suksess respons`() {
        wireMockServer.stubFor(WireMock.post(WireMock.urlEqualTo(DefaultOppdragClient.IVERKSETTELSE_URI))
                                       .willReturn(WireMock.okJson(Ressurs.success(lagRespons("00",
                                                                                              "OK")).toJson())))

        assertDoesNotThrow { iverksettelseService.sendIverksettVedtak(behandlingId) }

        val økonomiXmlSendt = økonomiXmlSendtRepository.findByBehandlingId(behandlingId)
        assertNotNull(økonomiXmlSendt)
        assertRequestXml(økonomiXmlSendt.melding, behandlingId, økonomiXmlSendt.id)
        assertRespons(økonomiXmlSendt.kvittering, "00", "OK")

        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val aktivBehandlingsresultat = behandling.sisteResultat
        assertNotNull(aktivBehandlingsresultat)
        assertEquals(Behandlingsresultatstype.FULL_TILBAKEBETALING, aktivBehandlingsresultat.type)
    }

    @Test
    fun `sendIverksettVedtak skal sende iverksettvedtak til økonomi for feil respons`() {
        wireMockServer.stubFor(WireMock.post(WireMock.urlEqualTo(DefaultOppdragClient.IVERKSETTELSE_URI))
                                       .willReturn(WireMock.okJson(Ressurs.success(lagRespons("10",
                                                                                              "feil")).toJson())))

        val exception = assertFailsWith<RuntimeException> { iverksettelseService.sendIverksettVedtak(behandlingId) }
        assertTrue { exception is IntegrasjonException }
        assertEquals("Noe gikk galt ved iverksetting av behandling=$behandlingId", exception.message)
        assertEquals("Fikk feil respons fra økonomi ved iverksetting av behandling=$behandlingId." +
                     "Mottatt respons:${objectMapper.writeValueAsString(lagMmmelDto("10", "feil"))}",
                     exception.cause!!.message)

        val økonomiXmlSendt = økonomiXmlSendtRepository.findByBehandlingId(behandlingId)
        assertNotNull(økonomiXmlSendt)
        assertRequestXml(økonomiXmlSendt.melding, behandlingId, økonomiXmlSendt.id)
        assertNull(økonomiXmlSendt.kvittering)
    }

    private fun lagKravgrunnlag(): Kravgrunnlag431 {
        val feilPostering = lagKravgrunnlagsbeløp(klassetype = Klassetype.FEIL,
                                                  klassekode = Klassekode.KL_KODE_FEIL_BA,
                                                  nyttBeløp = BigDecimal(5000))

        val ytelPostering = lagKravgrunnlagsbeløp(klassetype = Klassetype.YTEL,
                                                  klassekode = Klassekode.BATR,
                                                  utbetaltBeløp = BigDecimal(5000),
                                                  tilbakekrevesBeløp = BigDecimal(5000))

        val kravgrunnlagsperioder = perioder.map {
            Kravgrunnlagsperiode432(periode = it,
                                    månedligSkattebeløp = BigDecimal.ZERO,
                                    beløp = setOf(feilPostering.copy(id = UUID.randomUUID()),
                                                  ytelPostering.copy(id = UUID.randomUUID())))
        }.toSet()

        val kravgrunnlag = Kravgrunnlag431(behandlingId = behandlingId,
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
                                           perioder = kravgrunnlagsperioder)
        kravgrunnlagRepository.insert(kravgrunnlag)

        return kravgrunnlag
    }

    private fun lagKravgrunnlagsbeløp(klassetype: Klassetype,
                                      klassekode: Klassekode,
                                      nyttBeløp: BigDecimal = BigDecimal.ZERO,
                                      utbetaltBeløp: BigDecimal = BigDecimal.ZERO,
                                      tilbakekrevesBeløp: BigDecimal = BigDecimal.ZERO): Kravgrunnlagsbeløp433 {
        return Kravgrunnlagsbeløp433(klassetype = klassetype,
                                     klassekode = klassekode,
                                     nyttBeløp = nyttBeløp,
                                     opprinneligUtbetalingsbeløp = utbetaltBeløp,
                                     tilbakekrevesBeløp = tilbakekrevesBeløp,
                                     skatteprosent = BigDecimal.ZERO)
    }

    private fun lagVilkårsvurdering() {
        val vilkårsperioder = perioder.map {
            VilkårsvurderingsperiodeDto(periode = PeriodeDto(it),
                                        begrunnelse = "testverdi",
                                        aktsomhetDto = AktsomhetDto(
                                                aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                                                begrunnelse = "testverdi",
                                                særligeGrunnerTilReduksjon = false,
                                                tilbakekrevSmåbeløp = true,
                                                særligeGrunnerBegrunnelse = "testverdi",
                                                særligeGrunner = listOf(SærligGrunnDto(særligGrunn = SærligGrunn.ANNET,
                                                                                       begrunnelse = "testverdi"))),
                                        vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER)
        }
        vilkårsvurderingService.lagreVilkårsvurdering(behandling.id, BehandlingsstegVilkårsvurderingDto(vilkårsperioder))
    }

    private fun lagRespons(alvorlighetsgrad: String,
                           kodeMelding: String): TilbakekrevingsvedtakResponse {
        val mmelDto = lagMmmelDto(alvorlighetsgrad, kodeMelding)

        val respons = TilbakekrevingsvedtakResponse()
        respons.mmel = mmelDto
        respons.tilbakekrevingsvedtak = TilbakekrevingsvedtakDto()

        return respons
    }

    private fun lagMmmelDto(alvorlighetsgrad: String, kodeMelding: String): MmelDto {
        val mmelDto = MmelDto()
        mmelDto.alvorlighetsgrad = alvorlighetsgrad
        mmelDto.kodeMelding = kodeMelding
        return mmelDto
    }

    private fun assertRespons(kvittering: String?,
                              alvorlighetsgrad: String,
                              kodeMelding: String) {
        assertFalse { kvittering.isNullOrEmpty() }
        val mmelDto = objectMapper.readValue(kvittering, MmelDto::class.java)
        assertEquals(alvorlighetsgrad, mmelDto.alvorlighetsgrad)
        assertEquals(kodeMelding, mmelDto.kodeMelding)
    }

    private fun assertRequestXml(melding: String, behandlingId: UUID, xmlId: UUID) {
        val request = TilbakekrevingsvedtakMarshaller.unmarshall(melding, behandlingId, xmlId)
        assertNotNull(request)

        val tilbakekrevingsvedtak = request.tilbakekrevingsvedtak
        assertEquals(KodeAksjon.FATTE_VEDTAK.kode, tilbakekrevingsvedtak.kodeAksjon)
        assertNotNull(tilbakekrevingsvedtak.datoVedtakFagsystem)
        assertEquals(BigInteger.ZERO, tilbakekrevingsvedtak.vedtakId)
        assertEquals("22-15", tilbakekrevingsvedtak.kodeHjemmel)
        assertEquals(kravgrunnlag431.ansvarligEnhet, tilbakekrevingsvedtak.enhetAnsvarlig)

        val førstePeriode = tilbakekrevingsvedtak.tilbakekrevingsperiode[0]
        assertNotNull(førstePeriode.periode)
        assertEquals(BigDecimal.ZERO, førstePeriode.belopRenter)
        assertEquals(2, førstePeriode.tilbakekrevingsbelop.size)
        assertBeløp(beløpene = førstePeriode.tilbakekrevingsbelop,
                    klassekode = Klassekode.KL_KODE_FEIL_BA,
                    nyttBeløp = BigDecimal(5000))
        assertBeløp(beløpene = førstePeriode.tilbakekrevingsbelop,
                    klassekode = Klassekode.BATR,
                    utbetaltBeløp = BigDecimal(5000),
                    tilbakekrevesBeløp = BigDecimal(5000),
                    kodeResultat = KodeResultat.FULL_TILBAKEKREVING)

        val andrePeriode = tilbakekrevingsvedtak.tilbakekrevingsperiode[0]
        assertNotNull(andrePeriode.periode)
        assertEquals(BigDecimal.ZERO, andrePeriode.belopRenter)
        assertEquals(2, andrePeriode.tilbakekrevingsbelop.size)
        assertBeløp(beløpene = andrePeriode.tilbakekrevingsbelop,
                    klassekode = Klassekode.KL_KODE_FEIL_BA,
                    nyttBeløp = BigDecimal(5000))
        assertBeløp(beløpene = andrePeriode.tilbakekrevingsbelop,
                    klassekode = Klassekode.BATR,
                    utbetaltBeløp = BigDecimal(5000),
                    tilbakekrevesBeløp = BigDecimal(5000),
                    kodeResultat = KodeResultat.FULL_TILBAKEKREVING)
    }

    private fun assertBeløp(beløpene: List<TilbakekrevingsbelopDto>,
                            klassekode: Klassekode,
                            nyttBeløp: BigDecimal = BigDecimal.ZERO,
                            utbetaltBeløp: BigDecimal = BigDecimal.ZERO,
                            tilbakekrevesBeløp: BigDecimal = BigDecimal.ZERO,
                            uinnkrevdBeløp: BigDecimal = BigDecimal.ZERO,
                            skattBeløp: BigDecimal = BigDecimal.ZERO,
                            kodeResultat: KodeResultat? = null) {
        assertTrue {
            beløpene.any {
                klassekode.name == it.kodeKlasse &&
                nyttBeløp == it.belopNy &&
                utbetaltBeløp == it.belopOpprUtbet &&
                tilbakekrevesBeløp == it.belopTilbakekreves &&
                uinnkrevdBeløp == it.belopUinnkrevd
                skattBeløp == it.belopSkatt
            }
        }

        beløpene.any {
            kodeResultat?.kode == it.kodeResultat &&
            "ANNET" == it.kodeAarsak &&
            "IKKE_FORDELT" == it.kodeSkyld
        }
    }
}
