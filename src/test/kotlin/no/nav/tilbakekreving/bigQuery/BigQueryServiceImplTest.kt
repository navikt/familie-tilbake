package no.nav.tilbakekreving.bigQuery

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.InsertAllRequest
import com.google.cloud.bigquery.InsertAllResponse
import com.google.cloud.bigquery.TableId
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.behandling.Behandlingsinformasjon
import no.nav.tilbakekreving.behandling.Enhet
import no.nav.tilbakekreving.bigquery.BigQueryServiceImpl
import no.nav.tilbakekreving.config.ApplicationProperties
import no.nav.tilbakekreving.e2e.TilbakekrevingE2EBase
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.core.io.support.ResourcePatternResolver
import java.time.LocalDateTime
import java.util.UUID

class BigQueryServiceImplTest : TilbakekrevingE2EBase() {
    private val environment: Environment = mockk(relaxed = true)

    @Autowired
    private lateinit var applicationProperties: ApplicationProperties

    val resolver = mockk<ResourcePatternResolver>(relaxed = true)

    @Test
    fun `leggeTilBehanlingInfo legger til bigquery tabell`() {
        val bigQuery = mockk<BigQuery>()
        val response = mockk<InsertAllResponse> { every { hasErrors() } returns false }
        val requestSlot = slot<InsertAllRequest>()
        every { bigQuery.insertAll(capture(requestSlot)) } returns response

        val service = BigQueryServiceImpl(environment, applicationProperties, bigQuery, resolver)

        val behandling = mockk<Behandling>()
        val info = Behandlingsinformasjon(
            behandlingId = UUID.randomUUID(),
            kravgrunnlagReferanse = "1",
            opprettetTid = LocalDateTime.of(2024, 8, 8, 12, 34, 56),
            behandlingstype = Behandlingstype.TILBAKEKREVING,
            enhet = Enhet("123", "EnhetTest"),
        )
        every { behandling.hentBehandlingsinformasjon() } returns info

        service.leggeTilBehanlingInfo("behandlinger", behandling)

        val req = requestSlot.captured
        req.table.shouldBe(TableId.of("Dummy", "Dummy", "behandlinger"))

        val row = req.rows.first().content
        row["behandlingstype"] shouldBe Behandlingstype.TILBAKEKREVING.name
        row["behandlende_enhet"] shouldBe "EnhetTest"
        row["opprettet_tid"] shouldBe "2024-08-08 12:34:56"

        verify(exactly = 1) { bigQuery.insertAll(any()) }
    }
}
