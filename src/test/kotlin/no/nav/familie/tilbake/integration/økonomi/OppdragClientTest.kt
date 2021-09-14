package no.nav.familie.tilbake.integration.økonomi

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.exceptionhandler.IntegrasjonException
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.iverksettvedtak.TilbakekrevingsvedtakMarshaller
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagUtil
import no.nav.familie.tilbake.kravgrunnlag.domain.KodeAksjon
import no.nav.okonomi.tilbakekrevingservice.KravgrunnlagHentDetaljRequest
import no.nav.okonomi.tilbakekrevingservice.KravgrunnlagHentDetaljResponse
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakRequest
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakResponse
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.HentKravgrunnlagDetaljDto
import no.nav.tilbakekreving.typer.v1.MmelDto
import org.eclipse.jetty.http.HttpStatus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.client.RestOperations
import java.math.BigInteger
import java.net.URI
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class OppdragClientTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    private lateinit var oppdragClient: OppdragClient

    private val restOperations: RestOperations = RestTemplateBuilder().build()
    private val wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())

    private val fagsak = Testdata.fagsak
    private val behandling = Testdata.behandling
    private lateinit var tilbakekrevingsvedtakRequest: TilbakekrevingsvedtakRequest
    private lateinit var hentKravgrunnlagRequest: KravgrunnlagHentDetaljRequest
    private val kravgrunnlagId: BigInteger = BigInteger.ZERO

    @BeforeEach
    fun init() {
        wireMockServer.start()

        fagsakRepository.insert(fagsak)
        behandlingRepository.insert(behandling)
        oppdragClient = DefaultOppdragClient(restOperations, URI.create(wireMockServer.baseUrl()))

        val tilbakekrevingsvedtakRequestXml = readXml("/tilbakekrevingsvedtak/tilbakekrevingsvedtak.xml")
        tilbakekrevingsvedtakRequest = TilbakekrevingsvedtakMarshaller.unmarshall(tilbakekrevingsvedtakRequestXml,
                                                                                  behandling.id,
                                                                                  UUID.randomUUID())
        hentKravgrunnlagRequest = KravgrunnlagHentDetaljRequest().apply {
            hentkravgrunnlag = HentKravgrunnlagDetaljDto().apply {
                kravgrunnlagId = kravgrunnlagId
                kodeAksjon = KodeAksjon.HENT_KORRIGERT_KRAVGRUNNLAG.kode
                saksbehId = "testverdi"
                enhetAnsvarlig = "testverdi"
            }
        }
    }

    @AfterEach
    fun tearDown() {
        wireMockServer.resetAll()
        wireMockServer.stop()
    }

    @Test
    fun `iverksettVedtak skal sende iverksettelse request til oppdrag`() {
        wireMockServer.stubFor(WireMock.post(WireMock.urlEqualTo(DefaultOppdragClient.IVERKSETTELSE_URI + behandling.id))
                                       .willReturn(WireMock.okJson(Ressurs.success(lagIverksettelseRespons()).toJson())))
        assertDoesNotThrow { oppdragClient.iverksettVedtak(behandling.id, tilbakekrevingsvedtakRequest) }
    }

    @Test
    fun `iverksettVedtak skal ikke sende iverksettelse request til oppdrag når oppdrag har nedetid`() {
        wireMockServer.stubFor(WireMock.post(WireMock.urlEqualTo(DefaultOppdragClient.IVERKSETTELSE_URI + behandling.id))
                                       .willReturn(WireMock.status(HttpStatus.REQUEST_TIMEOUT_408)))

        val exception = assertFailsWith<RuntimeException> {
            oppdragClient.iverksettVedtak(behandling.id,
                                          tilbakekrevingsvedtakRequest)
        }
        assertNotNull(exception)
        assertTrue { exception is IntegrasjonException }
        assertEquals("Noe gikk galt ved iverksetting av behandling=${behandling.id}", exception.message)
    }

    @Test
    fun `iverksettVedtak skal ikke iverksette behandling til oppdrag når økonomi ikke svarer`() {
        wireMockServer.stubFor(WireMock.post(WireMock.urlEqualTo(DefaultOppdragClient.IVERKSETTELSE_URI + behandling.id))
                                       .willReturn(WireMock.serviceUnavailable().withStatusMessage("Couldn't send message")))

        val exception = assertFailsWith<RuntimeException> {
            oppdragClient.iverksettVedtak(behandling.id,
                                          tilbakekrevingsvedtakRequest)
        }
        assertNotNull(exception)
        assertTrue { exception is IntegrasjonException }
        assertEquals("Noe gikk galt ved iverksetting av behandling=${behandling.id}", exception.message)
        assertEquals("503 Couldn't send message: [no body]", exception.cause?.message)
    }

    @Test
    fun `hentKravgrunnlag skal hente kravgrunnlag fra oppdrag`() {

        wireMockServer.stubFor(WireMock.post(WireMock.urlEqualTo(DefaultOppdragClient.HENT_KRAVGRUNNLAG_URI +
                                                                 kravgrunnlagId))
                                       .willReturn(WireMock.okJson(Ressurs.success(lagHentKravgrunnlagRespons("00",
                                                                                                              "OK"))
                                                                           .toJson())))
        assertDoesNotThrow { oppdragClient.hentKravgrunnlag(kravgrunnlagId, hentKravgrunnlagRequest) }
    }

    @Test
    fun `hentKravgrunnlag skal ikke hente kravgrunnlag fra oppdrag når kravgrunnlag ikke finnes i økonomi`() {

        wireMockServer.stubFor(WireMock.post(WireMock.urlEqualTo(DefaultOppdragClient.HENT_KRAVGRUNNLAG_URI +
                                                                 kravgrunnlagId))
                                       .willReturn(WireMock.okJson(Ressurs.success(lagHentKravgrunnlagRespons("00",
                                                                                                              "B420010I"))
                                                                           .toJson())))
        val exception = assertFailsWith<RuntimeException> {
            oppdragClient.hentKravgrunnlag(kravgrunnlagId, hentKravgrunnlagRequest)
        }
        assertNotNull(exception)
        assertTrue { exception is IntegrasjonException }
        assertEquals("Noe gikk galt ved henting av kravgrunnlag for kravgrunnlagId=$kravgrunnlagId", exception.message)
        assertEquals("Fikk feil respons:{\"systemId\":null,\"kodeMelding\":\"B420010I\",\"alvorlighetsgrad\":\"00\"," +
                     "\"beskrMelding\":null,\"sqlKode\":null,\"sqlState\":null,\"sqlMelding\":null,\"mqCompletionKode\":null," +
                     "\"mqReasonKode\":null,\"programId\":null,\"sectionNavn\":null} fra økonomi ved henting av kravgrunnlag " +
                     "for kravgrunnlagId=$kravgrunnlagId.", exception.cause?.message)
    }

    @Test
    fun `hentKravgrunnlag skal ikke hente kravgrunnlag fra oppdrag når kravgrunnlag er sperret i økonomi`() {

        wireMockServer.stubFor(WireMock.post(WireMock.urlEqualTo(DefaultOppdragClient.HENT_KRAVGRUNNLAG_URI +
                                                                 kravgrunnlagId))
                                       .willReturn(WireMock.okJson(Ressurs.success(lagHentKravgrunnlagRespons("00",
                                                                                                              "B420012I"))
                                                                           .toJson())))
        val exception = assertFailsWith<RuntimeException> {
            oppdragClient.hentKravgrunnlag(kravgrunnlagId, hentKravgrunnlagRequest)
        }
        assertNotNull(exception)
        assertEquals("Noe gikk galt ved henting av kravgrunnlag for kravgrunnlagId=$kravgrunnlagId", exception.message)
        assertEquals("Hentet kravgrunnlag for kravgrunnlagId=$kravgrunnlagId er sperret", exception.cause?.message)
    }

    @Test
    fun `hentKravgrunnlag skal ikke hente kravgrunnlag fra oppdrag når økonomi ikke svarer`() {

        wireMockServer.stubFor(WireMock.post(WireMock.urlEqualTo(DefaultOppdragClient.HENT_KRAVGRUNNLAG_URI +
                                                                 kravgrunnlagId))
                                       .willReturn(WireMock.serviceUnavailable().withStatusMessage("Couldn't send message")))
        val exception = assertFailsWith<RuntimeException> {
            oppdragClient.hentKravgrunnlag(kravgrunnlagId, hentKravgrunnlagRequest)
        }
        assertNotNull(exception)
        assertTrue { exception is IntegrasjonException }
        assertEquals("Noe gikk galt ved henting av kravgrunnlag for kravgrunnlagId=$kravgrunnlagId", exception.message)
        assertEquals("503 Couldn't send message: [no body]", exception.cause?.message)
    }

    private fun lagIverksettelseRespons(): TilbakekrevingsvedtakResponse {
        val mmelDto = lagMmmelDto("00", "OK")

        val respons = TilbakekrevingsvedtakResponse()
        respons.mmel = mmelDto
        respons.tilbakekrevingsvedtak = tilbakekrevingsvedtakRequest.tilbakekrevingsvedtak

        return respons
    }

    private fun lagHentKravgrunnlagRespons(alvorlighetsgrad: String,
                                           kodeMelding: String): KravgrunnlagHentDetaljResponse {
        val mmelDto = lagMmmelDto(alvorlighetsgrad, kodeMelding)

        val respons = KravgrunnlagHentDetaljResponse()
        respons.mmel = mmelDto
        respons.detaljertkravgrunnlag = DetaljertKravgrunnlagDto()
        respons.detaljertkravgrunnlag = KravgrunnlagUtil
                .unmarshalKravgrunnlag(readXml("/kravgrunnlagxml/kravgrunnlag_BA_riktig_eksternfagsakId_ytelsestype.xml"))
        return respons
    }

    private fun lagMmmelDto(alvorlighetsgrad: String, kodeMelding: String): MmelDto {
        val mmelDto = MmelDto()
        mmelDto.alvorlighetsgrad = alvorlighetsgrad
        mmelDto.kodeMelding = kodeMelding
        return mmelDto
    }

}