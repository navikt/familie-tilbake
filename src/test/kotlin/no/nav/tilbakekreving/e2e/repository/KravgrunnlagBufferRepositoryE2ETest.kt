package no.nav.tilbakekreving.e2e.repository

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator
import no.nav.tilbakekreving.e2e.TilbakekrevingE2EBase
import no.nav.tilbakekreving.hendelse.Kravstatuskode
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagBufferRepository
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.query

class KravgrunnlagBufferRepositoryE2ETest : TilbakekrevingE2EBase() {
    @Test
    fun `konsumering av kravgrunnlag med exception - les ikke alt på nytt`() {
        val fagsystemId1 = KravgrunnlagGenerator.nextPaddedId(6)
        val fagsystemId2 = KravgrunnlagGenerator.nextPaddedId(6)
        kravgrunnlagBufferRepository.lagre(
            KravgrunnlagBufferRepository.Entity(
                kravgrunnlag = "",
                kravgrunnlagId = KravgrunnlagGenerator.nextPaddedId(8),
                fagsystemId = fagsystemId1,
            ),
        )
        kravgrunnlagBufferRepository.lagre(
            KravgrunnlagBufferRepository.Entity(
                kravgrunnlag = "",
                kravgrunnlagId = KravgrunnlagGenerator.nextPaddedId(8),
                fagsystemId = fagsystemId2,
            ),
        )

        val egneFagsystemIder = setOf(fagsystemId1, fagsystemId2)

        repeat(2) {
            kravgrunnlagBufferRepository.konsumerKravgrunnlag {
                if (it.fagsystemId !in egneFagsystemIder) throw Exception("Skal ikke konsumere kravgrunnlag kjørt av parallelle tester")
                if (it.fagsystemId == fagsystemId1) throw Exception("Feil under konsumering av kravgrunnlag")
            }
        }

        jdbcTemplate.query("SELECT * FROM kravgrunnlag_buffer WHERE fagsystem_id IN (?, ?) ORDER BY fagsystem_id", fagsystemId1, fagsystemId2) { resultSet, _ ->
            resultSet.getString("fagsystem_id") to resultSet.getBoolean("lest")
        } shouldBe listOf(
            fagsystemId1 to false,
            fagsystemId2 to true,
        )

        kravgrunnlagBufferRepository.konsumerKravgrunnlag {
            if (it.fagsystemId !in egneFagsystemIder) throw Exception("Skal ikke konsumere kravgrunnlag kjørt av parallelle tester")
        }

        jdbcTemplate.query("SELECT * FROM kravgrunnlag_buffer WHERE fagsystem_id IN (?, ?) ORDER BY fagsystem_id", fagsystemId1, fagsystemId2) { resultSet, _ ->
            resultSet.getString("fagsystem_id") to resultSet.getBoolean("lest")
        } shouldBe listOf(
            fagsystemId1 to true,
            fagsystemId2 to true,
        )
    }

    @Test
    fun `endret kravgrunnlag med samme kravgrunnlagId blir ikke markert som lest`() {
        val vedtakId = KravgrunnlagGenerator.nextPaddedId(6)
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        val kravgrunnlagId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlag(
            KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
                vedtakId = vedtakId,
                kravgrunnlagId = kravgrunnlagId,
            ),
        )
        // Fyller opp lesingen slik at det endrede kravgrunnlaget ikke blir lest sammen med det første
        repeat(4) {
            sendKravgrunnlag(KravgrunnlagGenerator.forTilleggsstønader())
        }
        sendKravgrunnlag(
            KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
                vedtakId = vedtakId,
                kravgrunnlagId = kravgrunnlagId,
                kontrollfelt = "2025-12-24-11.12.13.234567",
                kravStatusKode = "ENDR",
            ),
        )

        repeat(2) {
            kravgrunnlagMediator.lesKravgrunnlag()
        }

        tilbakekreving(FagsystemDTO.TS, fagsystemId)
            .shouldNotBeNull()
            .kravgrunnlagHistorikk
            .nåværendeKravgrunnlagReferanse()
            .entry
            .kravstatuskode shouldBe Kravstatuskode.ENDRET
    }
}
