package no.nav.tilbakekreving.dokument.saf

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.dokumentHåndtering.saf.SafService
import no.nav.tilbakekreving.eksternfagsak.EksternFagsak
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kontrakter.ytelse.Tema
import no.tilbakekreving.integrasjoner.dokument.kontrakter.JournalpostResponse
import no.tilbakekreving.integrasjoner.dokument.kontrakter.Journalposttype
import no.tilbakekreving.integrasjoner.dokument.kontrakter.Journalstatus
import no.tilbakekreving.integrasjoner.dokument.saf.SafClient
import org.junit.jupiter.api.Test
import java.util.UUID

class SafServiceTest() {
    private val tokenValidationContextHolder = mockk<TokenValidationContextHolder>()
    private val safClient = mockk<SafClient>()
    private val safService = SafService(
        tokenValidationContextHolder = tokenValidationContextHolder,
        safClient = safClient,
    )

    @Test
    fun `hentDokumenter skal ikke finne token`() {
        val ctx = mockk<TokenValidationContext>()
        every { tokenValidationContextHolder.getTokenValidationContext() } returns ctx
        every { ctx.firstValidToken } returns null

        shouldThrow<Feil> {
            safService.hentDokument(UUID.randomUUID(), "", "", null)
        }.message shouldContain "Finner ikke token"
    }

    @Test
    fun `hentDokumenter skal returnere ByteArray`() {
        val ctx = mockk<TokenValidationContext>()
        val jwtToken = mockk<JwtToken>()
        every { tokenValidationContextHolder.getTokenValidationContext() } returns ctx
        every { ctx.firstValidToken } returns jwtToken
        every { jwtToken.encodedToken } returns "jwt-abc"
        every { safClient.hentDokument(any(), any(), any(), any()) } returns ByteArray(0)

        val response = safService.hentDokument(UUID.randomUUID(), "", "", null)
        response shouldBe ByteArray(0)
    }

    @Test
    fun `hentJournalposter skal returnere List av Journalpost med tilbakekreving som argument`() {
        val tilbakekreving = mockk<Tilbakekreving> {
            every { eksternFagsak } returns mockk<EksternFagsak> {
                every { hentYtelse() } returns mockk<Ytelse> {
                    every { tilTema() } returns Tema.TSO
                }
            }
            every { bruker } returns mockk {
                every { hentBrukerinfo() } returns mockk {
                    every { ident } returns "12312312312"
                }
            }
        }
        every { safClient.hentJournalposterForBruker(any(), any(), any()) } returns listOf(
            JournalpostResponse(journalpostId = "123", journalposttype = Journalposttype.I, Journalstatus.UKJENT),
        )

        val response = safService.hentJournalposter(tilbakekreving, null, null)

        response.size shouldBe 1
        response[0].journalpostId shouldBe "123"
    }

    @Test
    fun `hentJournalposter skal returnere List av Journalpost uten tilbakekreving som argument`() {
        every { safClient.hentJournalposterForBruker(any(), any(), any()) } returns listOf(
            JournalpostResponse(journalpostId = "123", journalposttype = Journalposttype.I, Journalstatus.UKJENT),
        )

        val response = safService.hentJournalposter(null, "1231231231", listOf(Tema.TSO))

        response.size shouldBe 1
        response[0].journalpostId shouldBe "123"
    }
}
