package no.nav.tilbakekreving.e2e

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Testdata
import no.nav.tilbakekreving.UtenforScope
import no.nav.tilbakekreving.fagsystem.FagsystemIntegrasjonService
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.kravgrunnlag.StatusmeldingBufferRepository
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

        sendStatusmelding(statusmelding = StatusmeldingGenerator.forTilleggsstønader(fagsystemId = fagsystemId, kodeStatusKrav = "AVSL"))

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

        sendStatusmelding(statusmelding = StatusmeldingGenerator.forTilleggsstønader(fagsystemId = fagsystemId, kodeStatusKrav = "AVSL"))

        val exception = shouldThrow<ModellFeil.UtenforScopeException> {
            behandlingController.hentBehandling(behandlingId)
        }
        exception.utenforScope shouldBe UtenforScope.KravgrunnlagBortfalt
    }
}
