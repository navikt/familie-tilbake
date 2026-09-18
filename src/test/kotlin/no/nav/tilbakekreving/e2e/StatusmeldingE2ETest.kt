package no.nav.tilbakekreving.e2e

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagUtil
import no.nav.tilbakekreving.Testdata
import no.nav.tilbakekreving.UtenforScope
import no.nav.tilbakekreving.fagsystem.FagsystemIntegrasjonService
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.kravgrunnlag.StatusmeldingBufferRepository
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.query

class StatusmeldingE2ETest : TilbakekrevingE2EBase() {
    @Autowired
    private lateinit var fagsystemIntegrasjonService: FagsystemIntegrasjonService

    @Autowired
    private lateinit var statusmeldingBufferRepository: StatusmeldingBufferRepository

    @Test
    fun `statusmelding lagres i buffer for senere behandling`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)

        sendStatusmelding(statusmelding = statusmelding(fagsystemId = fagsystemId, kodeStatusKrav = "AVSL"))

        val antallKravgrunnlag = jdbcTemplate.query(
            "SELECT count(1) AS antall FROM kravgrunnlag_buffer WHERE fagsystem_id=?;",
            fagsystemId,
        ) { rs, _ -> rs.getInt("antall") }.single()

        antallKravgrunnlag shouldBe 0
    }

    @Test
    fun `annullert kravgrunnlag blokkerer behandling`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)

        sendKravgrunnlagOgAvventLesing(KravgrunnlagGenerator.forTilleggsstønader(fagsystemId = fagsystemId))
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId))
        val behandlingId = behandlingIdFor(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()

        sendStatusmelding(statusmelding = statusmelding(fagsystemId = fagsystemId, kodeStatusKrav = "AVSL"))

        val exception = shouldThrow<ModellFeil.UtenforScopeException> {
            behandlingController.hentBehandling(behandlingId)
        }
        exception.utenforScope shouldBe UtenforScope.KravgrunnlagBortfalt
    }

    private fun sendStatusmelding(
        statusmelding: String,
        forventetAntallUleste: Int = 1,
    ) {
        val dto = KravgrunnlagUtil.unmarshalStatusmelding(statusmelding)
        statusmeldingBufferRepository.lagre(
            StatusmeldingBufferRepository.Entity(
                statusmelding = statusmelding,
                fagsystemId = dto.fagsystemId,
                vedtakId = dto.vedtakId.toString(),
                status = dto.kodeStatusKrav,
            ),
        )
        statusmeldingBufferRepository
        tellUlesteStatusmeldinger(dto.fagsystemId) shouldBe forventetAntallUleste
    }

    private fun tellUlesteStatusmeldinger(fagsystemId: String): Int {
        return jdbcTemplate.query("SELECT count(1) AS antall FROM statusmelding_buffer WHERE lest=false AND fagsystem_id=?;", fagsystemId) { rs, _ ->
            rs.getInt("antall")
        }.single()
    }

    fun statusmelding(
        vedtakId: String = KravgrunnlagGenerator.nextPaddedId(6),
        fagsystemId: String = KravgrunnlagGenerator.nextPaddedId(6),
        referanse: String = KravgrunnlagGenerator.nextPaddedId(4),
        fødselsnummer: String = "40026912345",
        kodeStatusKrav: String,
        fagområde: String = "TILLST",
    ): String {
        @Language("XML")
        val xml = """<?xml version="1.0" encoding="utf-8"?>
        <urn:endringKravOgVedtakstatus xmlns:urn="urn:no:nav:tilbakekreving:status:v1">
            <urn:kravOgVedtakstatus>
                <urn:vedtakId>$vedtakId</urn:vedtakId>
                <urn:kodeStatusKrav>$kodeStatusKrav</urn:kodeStatusKrav>
                <urn:kodeFagomraade>$fagområde</urn:kodeFagomraade>
                <urn:fagsystemId>$fagsystemId</urn:fagsystemId>
                <urn:vedtakGjelderId>$fødselsnummer</urn:vedtakGjelderId>
                <urn:typeGjelderId>PERSON</urn:typeGjelderId>
                <urn:referanse>$referanse</urn:referanse>
            </urn:kravOgVedtakstatus>
        </urn:endringKravOgVedtakstatus>
            """
        return xml.replace(Regex("\n *"), "")
    }
}
