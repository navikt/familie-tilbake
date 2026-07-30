package no.nav.tilbakekreving.entities

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class HistorikkEntityTest {
    @Test
    fun `historikk med feil rekkefølge fra database blir riktig sortert`() {
        val entry1 = TestEntry(UUID.randomUUID(), LocalDateTime.now().minusDays(1))
        val entry2 = TestEntry(UUID.randomUUID(), LocalDateTime.now())
        val historikk = HistorikkEntity<UUID, TestEntry, TestEntry>(listOf(entry2, entry1))

        historikk.fraEntity { it } shouldBe listOf(entry1, entry2)
    }

    data class TestEntry(override val id: UUID, override val opprettet: LocalDateTime) : HistorikkInnslagEntity<UUID>
}
