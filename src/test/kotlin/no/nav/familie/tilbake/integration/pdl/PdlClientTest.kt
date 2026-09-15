package no.nav.familie.tilbake.integration.pdl

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.config.PdlConfig
import no.nav.familie.tilbake.integration.pdl.internal.PdlKjønnType
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import org.junit.jupiter.api.Test
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import java.net.URI
import java.time.LocalDate

class PdlClientTest {
    @Test
    fun `hentPersoninfo skal hente person info for barnetrygd med ok respons fra PDL`() {
        val (pdlClient, mockServer) = opprettMockClient("pdlOkResponseEnkel.json")

        val respons = pdlClient.hentPersoninfo("11111122222", FagsystemDTO.BA, SecureLog.Context.tom())

        respons.shouldNotBeNull()
        respons.navn shouldBe "ENGASJERT FYR"
        respons.kjønn shouldBe PdlKjønnType.MANN
        respons.fødselsdato shouldBe LocalDate.of(1955, 9, 13)
        respons.dødsdato shouldBe null
        mockServer.verify()
    }

    @Test
    fun `hentPersoninfo skal hente info for en død person`() {
        val (pdlClient, mockServer) = opprettMockClient("pdlOkResponseDødPerson.json")

        val respons = pdlClient.hentPersoninfo("11111122222", FagsystemDTO.BA, SecureLog.Context.tom())

        respons.shouldNotBeNull()
        respons.navn shouldBe "ENGASJERT FYR"
        respons.kjønn shouldBe PdlKjønnType.MANN
        respons.fødselsdato shouldBe LocalDate.of(1955, 9, 13)
        respons.dødsdato shouldBe LocalDate.of(2022, 4, 1)
        mockServer.verify()
    }

    @Test
    fun `hentPersoninfo skal ikke hente person info når person ikke finnes`() {
        val (pdlClient, mockServer) = opprettMockClient("pdlPersonIkkeFunnetResponse.json")

        val exception =
            shouldThrow<RuntimeException>(
                block =
                    { pdlClient.hentPersoninfo("11111122222", FagsystemDTO.BA, SecureLog.Context.tom()) },
            )
        exception.message shouldBe "Feil ved oppslag på person: Person ikke funnet"
        mockServer.verify()
    }

    private fun opprettMockClient(responsfil: String): Pair<PdlClient, MockRestServiceServer> {
        val restTemplate = RestTemplateBuilder().build()
        val mockServer = MockRestServiceServer.bindTo(restTemplate).build()
        mockServer
            .expect(requestTo(URI.create("http://localhost/${PdlConfig.PATH_GRAPHQL}")))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(readFile(responsfil), MediaType.APPLICATION_JSON))
        val pdlClient = PdlClientImpl(PdlConfig(URI.create("http://localhost")), restTemplate)
        return pdlClient to mockServer
    }

    private fun readFile(filnavn: String): String = this::class.java.getResource("/pdl/json/$filnavn").readText()
}
