package no.nav.familie.tilbake.integration.familie

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.http.sts.StsRestClient
import no.nav.familie.kontrakter.felles.Ressurs.Companion.failure
import no.nav.familie.kontrakter.felles.Ressurs.Companion.success
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.tilbake.config.IntegrasjonerConfig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.client.RestOperations
import java.net.URI
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

internal class IntegrasjonerClientTest {

    private val wireMockServer = WireMockServer(wireMockConfig().dynamicPort())
    private val restOperations: RestOperations = RestTemplateBuilder().build()

    private lateinit var integrasjonerClient: IntegrasjonerClient
    private val arkiverDokumentRequest = ArkiverDokumentRequest("123456789", true, listOf())

    @BeforeEach
    fun setUp() {
        wireMockServer.start()
        val stsRestClient = mockk<StsRestClient>()
        every { stsRestClient.systemOIDCToken } returns "token"
        integrasjonerClient = IntegrasjonerClient(restOperations,
                                                  IntegrasjonerConfig(URI.create(wireMockServer.baseUrl()), "tilbake"))
    }

    @AfterEach
    fun tearDown() {
        wireMockServer.resetAll()
        wireMockServer.stop()
    }

    @Test
    fun `arkiver skal gi vellykket respons hvis integrasjoner gir gyldig svar`() {
        val arkiverDokumentResponse = ArkiverDokumentResponse("wer", true)

        wireMockServer.stubFor(post(urlEqualTo("/${IntegrasjonerConfig.PATH_ARKIVER}"))
                                       .willReturn(okJson(success(arkiverDokumentResponse).toJson())))

        assertNotNull(integrasjonerClient.arkiver(arkiverDokumentRequest))
    }

    @Test
    fun `arkiver skal kaste feil hvis hvis integrasjoner gir ugyldig svar`() {
        wireMockServer.stubFor(post(urlEqualTo("/${IntegrasjonerConfig.PATH_ARKIVER}"))
                                       .willReturn(okJson(failure<Any>("error").toJson())))

        assertFailsWith(IllegalStateException::class) {
            integrasjonerClient.arkiver(arkiverDokumentRequest)
        }
    }

    @Test
    fun `distribuerJournalpost skal gi vellykket respons hvis integrasjoner gir gyldig svar`() {
        // Gitt
        wireMockServer.stubFor(post(urlEqualTo("/${IntegrasjonerConfig.PATH_DISTRIBUER}"))
                                       .willReturn(okJson(success("id").toJson())))
        // Vil gi resultat
        assertNotNull(integrasjonerClient.distribuerJournalpost("3216354", Fagsystem.EF))
    }

    @Test
    fun `distribuerJournalpost skal kaste feil hvis hvis integrasjoner gir ugyldig svar`() {
        wireMockServer.stubFor(post(urlEqualTo("/${IntegrasjonerConfig.PATH_DISTRIBUER}"))
                                       .willReturn(okJson(failure<Any>("error").toJson())))

        assertFailsWith(IllegalStateException::class) {
            integrasjonerClient.distribuerJournalpost("3216354", Fagsystem.EF)
        }
    }

    @Test
    fun `hentOrganisasjon skal gi vellykket respons hvis integrasjoner gir gyldig svar`() {
        // Gitt
        wireMockServer.stubFor(get(urlEqualTo("/${IntegrasjonerConfig.PATH_ORGANISASJON}/987654321"))
                                       .willReturn(okJson(success(Organisasjon("Bob AS", "987654321")).toJson())))
        // Vil gi resultat
        assertNotNull(integrasjonerClient.hentOrganisasjon("987654321"))
    }

    @Test
    fun `hentOrganisasjon skal kaste feil hvis hvis integrasjoner gir ugyldig svar`() {
        wireMockServer.stubFor(get(urlEqualTo("/${IntegrasjonerConfig.PATH_ORGANISASJON}/987654321"))
                                       .willReturn(okJson(failure<Any>("error").toJson())))

        assertFailsWith(IllegalStateException::class) {
            integrasjonerClient.hentOrganisasjon("987654321")
        }
    }

}



