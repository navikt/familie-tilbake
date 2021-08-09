package no.nav.familie.tilbake.integration.pdl

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.tilbake.config.PdlConfig
import no.nav.familie.tilbake.integration.pdl.internal.Kjønn
import no.nav.familie.webflux.sts.StsTokenClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import java.net.URI
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class PdlClientTest {

    companion object {

        private val webClient = WebClient.builder().build()
        lateinit var pdlClient: PdlClient
        lateinit var wiremockServerItem: WireMockServer

        @BeforeAll
        @JvmStatic
        fun initClass() {
            wiremockServerItem = WireMockServer(wireMockConfig().dynamicPort())
            wiremockServerItem.start()
            val stsRestClient = mockk<StsTokenClient>()
            every { stsRestClient.systemOIDCToken } returns "token"
            pdlClient = PdlClient(PdlConfig(URI.create(wiremockServerItem.baseUrl())), webClient, stsRestClient)

        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            wiremockServerItem.stop()
        }
    }

    @AfterEach
    fun tearDownEachTest() {
        wiremockServerItem.resetAll()
    }

    @Test
    fun `hentPersoninfo skal hente person info for barnetrygd med ok respons fra PDL`() {
        wiremockServerItem.stubFor(post(urlEqualTo("/${PdlConfig.PATH_GRAPHQL}"))
                                           .willReturn(okJson(readFile("pdlOkResponseEnkel.json"))))

        val respons = pdlClient.hentPersoninfo("11111122222", Fagsystem.BA)

        assertNotNull(respons)
        assertEquals("ENGASJERT FYR", respons.navn)
        assertEquals(Kjønn.MANN, respons.kjønn)
        assertEquals(LocalDate.of(1955, 9, 13), respons.fødselsdato)
    }

    @Test
    fun `hentPersoninfo skal ikke hente person info når person ikke finnes`() {
        wiremockServerItem.stubFor(post(urlEqualTo("/${PdlConfig.PATH_GRAPHQL}"))
                                           .willReturn(okJson(readFile("pdlPersonIkkeFunnetResponse.json"))))


        val exception = assertFailsWith<RuntimeException>(block =
                                                          { pdlClient.hentPersoninfo("11111122222", Fagsystem.BA) })
        assertEquals("Feil ved oppslag på person: Person ikke funnet", exception.message)
    }


    private fun readFile(filnavn: String): String {
        return this::class.java.getResource("/pdl/json/$filnavn").readText()
    }
}
