package no.nav.familie.tilbake.config

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.tilbake.http.RessursException
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.familie.tilbake.kontrakter.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.tilbake.kontrakter.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.tilbake.kontrakter.journalpost.DokumentInfo
import no.nav.familie.tilbake.kontrakter.journalpost.Journalpost
import no.nav.familie.tilbake.kontrakter.journalpost.Journalposttype
import no.nav.familie.tilbake.kontrakter.journalpost.Journalstatus
import no.nav.familie.tilbake.kontrakter.journalpost.RelevantDato
import no.nav.familie.tilbake.kontrakter.navkontor.NavKontorEnhet
import no.nav.familie.tilbake.kontrakter.oppgave.FinnOppgaveResponseDto
import no.nav.familie.tilbake.kontrakter.oppgave.Oppgave
import no.nav.familie.tilbake.kontrakter.oppgave.Oppgavetype
import no.nav.familie.tilbake.kontrakter.organisasjon.Organisasjon
import no.nav.familie.tilbake.kontrakter.saksbehandler.Saksbehandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestClientResponseException
import java.time.LocalDateTime
import java.util.UUID

@Configuration
@Profile("mock-integrasjoner")
class IntegrasjonerClientConfig {
    @Bean
    @Primary
    fun integrasjonerClient(): IntegrasjonerClient {
        val integrasjonerClient: IntegrasjonerClient = mockk(relaxed = true)

        val arkiverRequest = slot<ArkiverDokumentRequest>()
        every { integrasjonerClient.arkiver(capture(arkiverRequest)) } answers {
            when (arkiverRequest.captured.fnr) {
                "12345678901" ->
                    ArkiverDokumentResponse(
                        "jpUkjentDødsbo",
                        false,
                        listOf(
                            no.nav.familie.tilbake.kontrakter.dokarkiv
                                .DokumentInfo("id"),
                        ),
                    )
                "04098203010" ->
                    ArkiverDokumentResponse(
                        "jpUkjentDødsbo",
                        false,
                        listOf(
                            no.nav.familie.tilbake.kontrakter.dokarkiv
                                .DokumentInfo("id"),
                        ),
                    )
                else ->
                    ArkiverDokumentResponse(
                        "jpId",
                        false,
                        listOf(
                            no.nav.familie.tilbake.kontrakter.dokarkiv
                                .DokumentInfo("id"),
                        ),
                    )
            }
        }

        val journalpostId = slot<String>()
        every { integrasjonerClient.distribuerJournalpost(capture(journalpostId), any(), any(), any()) } answers {
            when (
                journalpostId.captured
            ) {
                "jpUkjentDødsbo" ->
                    throw RessursException(
                        httpStatus = HttpStatus.GONE,
                        ressurs = Ressurs.failure("Ukjent adresse dødsbo"),
                        cause = RestClientResponseException("Ukjent adresse dødsbo", 410, "gone", null, null, null),
                    )
                "jpUkjentAdresse" ->
                    throw RessursException(
                        httpStatus = HttpStatus.BAD_REQUEST,
                        ressurs = Ressurs.failure("Mottaker har ukjent adresse"),
                        cause = RestClientResponseException("Mottaker har ukjent adresse", 401, "not there", null, null, null),
                    )
                "jpDuplikatDistribusjon" ->
                    throw RessursException(
                        httpStatus = HttpStatus.CONFLICT,
                        ressurs = Ressurs.failure("Dokumentet er allerede distribuert"),
                        cause = RestClientResponseException("Dokumentet er allerede distribuert", 409, "conflict", null, null, null),
                    )
                else -> "42"
            }
        }

        every { integrasjonerClient.hentDokument(any(), any()) } returns readMockfileFromResources()

        every { integrasjonerClient.hentJournalposterForBruker(any(), any()) }
            .returns(
                listOf(
                    Journalpost(
                        journalpostId = "jpId1",
                        journalposttype = Journalposttype.I,
                        journalstatus = Journalstatus.FERDIGSTILT,
                        tittel = "Journalførte dokumenter 1",
                        relevanteDatoer =
                            listOf(
                                RelevantDato(
                                    dato = LocalDateTime.now().minusDays(7),
                                    datotype = "DATO_REGISTRERT",
                                ),
                                RelevantDato(
                                    dato = LocalDateTime.now().minusDays(7),
                                    datotype = "DATO_JOURNALFOERT",
                                ),
                            ),
                        dokumenter =
                            listOf(
                                DokumentInfo(
                                    dokumentInfoId = "dokId1",
                                    tittel = "Dokument 1.1",
                                ),
                                DokumentInfo(
                                    dokumentInfoId = "dokId2",
                                    tittel = "Dokument 1.2",
                                ),
                            ),
                    ),
                    Journalpost(
                        journalpostId = "jpId2",
                        journalposttype = Journalposttype.U,
                        journalstatus = Journalstatus.FERDIGSTILT,
                        tittel = "Journalførte dokumenter 2",
                        relevanteDatoer =
                            listOf(
                                RelevantDato(
                                    dato = LocalDateTime.now().minusDays(4),
                                    datotype = "DATO_EKSPEDERT",
                                ),
                                RelevantDato(
                                    dato = LocalDateTime.now().minusDays(4),
                                    datotype = "DATO_JOURNALFOERT",
                                ),
                            ),
                        dokumenter =
                            listOf(
                                DokumentInfo(
                                    dokumentInfoId = "dokId1",
                                    tittel = "Dokument 2.1",
                                ),
                                DokumentInfo(
                                    dokumentInfoId = "dokId2",
                                    tittel = "Dokument 2.2",
                                ),
                            ),
                    ),
                    Journalpost(
                        journalpostId = "jpId3",
                        journalposttype = Journalposttype.N,
                        journalstatus = Journalstatus.FERDIGSTILT,
                        tittel = "Journalførte dokumenter 3",
                        relevanteDatoer =
                            listOf(
                                RelevantDato(
                                    dato = LocalDateTime.now().minusDays(2),
                                    datotype = "DATO_JOURNALFOERT",
                                ),
                            ),
                        dokumenter =
                            listOf(
                                DokumentInfo(
                                    dokumentInfoId = "dokId1",
                                    tittel = "Dokument 3.1",
                                ),
                                DokumentInfo(
                                    dokumentInfoId = "dokId2",
                                    tittel = "Dokument 3.2",
                                ),
                            ),
                    ),
                    Journalpost(
                        journalpostId = "jpId4",
                        journalposttype = Journalposttype.I,
                        journalstatus = Journalstatus.FERDIGSTILT,
                        tittel = "Journalførte dokumenter 4",
                        relevanteDatoer =
                            listOf(
                                RelevantDato(
                                    dato = LocalDateTime.now().minusDays(3),
                                    datotype = "DATO_JOURNALFOERT",
                                ),
                            ),
                        dokumenter =
                            listOf(
                                DokumentInfo(
                                    dokumentInfoId = "dokId1",
                                    tittel = "Dokument 4.1",
                                ),
                            ),
                    ),
                    Journalpost(
                        journalpostId = "jpId5",
                        journalposttype = Journalposttype.U,
                        journalstatus = Journalstatus.FERDIGSTILT,
                        tittel = "Journalførte dokumenter 5",
                        relevanteDatoer =
                            listOf(
                                RelevantDato(
                                    dato = LocalDateTime.now().minusDays(1),
                                    datotype = "DATO_JOURNALFOERT",
                                ),
                            ),
                        dokumenter =
                            listOf(
                                DokumentInfo(
                                    dokumentInfoId = "dokId1",
                                    tittel = "Dokument 5.1",
                                ),
                            ),
                    ),
                    Journalpost(
                        journalpostId = "jpId6",
                        journalposttype = Journalposttype.N,
                        journalstatus = Journalstatus.UNDER_ARBEID,
                        tittel = "Journalførte dokumenter 6",
                        relevanteDatoer =
                            listOf(
                                RelevantDato(
                                    dato = LocalDateTime.now().minusDays(6),
                                    datotype = "DATO_DOKUMENT",
                                ),
                            ),
                        dokumenter =
                            listOf(
                                DokumentInfo(
                                    dokumentInfoId = "dokId1",
                                    tittel = "Dokument 6.1",
                                ),
                            ),
                    ),
                ),
            )

        val organisasjonsnummer = slot<String>()
        every { integrasjonerClient.hentOrganisasjon(capture(organisasjonsnummer)) } answers {
            when (organisasjonsnummer.captured) {
                "998765432" ->
                    Organisasjon(
                        "998765432",
                        "Testinstitusjon",
                    )
                "999876543" ->
                    Organisasjon(
                        "999876543",
                        "Testinstitusjon med langt navn for test i frontend",
                    )
                else ->
                    Organisasjon(
                        "987654321",
                        "Bobs Burgers",
                    )
            }
        }

        every { integrasjonerClient.validerOrganisasjon(any()) } returns true

        every { integrasjonerClient.hentSaksbehandler(any()) } returns
            Saksbehandler(
                UUID.randomUUID(),
                "bb1234",
                "Bob",
                "Burger",
                "enhet",
            )

        every { integrasjonerClient.finnOppgaver(any()) } answers
            {
                if (Thread.currentThread().stackTrace.any { it.methodName == "opprettOppgave" }) {
                    FinnOppgaveResponseDto(
                        antallTreffTotalt = 0,
                        oppgaver = emptyList(),
                    )
                } else {
                    FinnOppgaveResponseDto(
                        antallTreffTotalt = 1,
                        oppgaver = listOf(Oppgave(id = 1, oppgavetype = Oppgavetype.BehandleSak.value)),
                    )
                }
            }

        every { integrasjonerClient.ferdigstillOppgave(any()) } just Runs

        every { integrasjonerClient.hentNavkontor(any()) } returns
            NavKontorEnhet(
                enhetId = 4806,
                navn = "Mock Nav Drammen",
                enhetNr = "mock",
                status = "mock",
            )

        return integrasjonerClient
    }

    fun readMockfileFromResources(): ByteArray = javaClass.getResource("/mockpdf/mocktest.pdf").readBytes()
}
