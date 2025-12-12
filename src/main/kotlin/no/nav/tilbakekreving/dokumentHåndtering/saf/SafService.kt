package no.nav.tilbakekreving.dokumentHåndtering.saf

import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.kontrakter.journalpost.DokumentInfo
import no.nav.familie.tilbake.kontrakter.journalpost.Journalpost
import no.nav.familie.tilbake.kontrakter.journalpost.Journalposttype
import no.nav.familie.tilbake.kontrakter.journalpost.Journalstatus
import no.nav.familie.tilbake.kontrakter.journalpost.LogiskVedlegg
import no.nav.familie.tilbake.kontrakter.journalpost.RelevantDato
import no.nav.familie.tilbake.kontrakter.journalpost.Sak
import no.nav.familie.tilbake.log.SecureLog
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.integrasjoner.felles.graphqlQuery
import no.nav.tilbakekreving.kontrakter.ytelse.Tema
import no.tilbakekreving.integrasjoner.CallContext
import no.tilbakekreving.integrasjoner.dokument.kontrakter.Bruker
import no.tilbakekreving.integrasjoner.dokument.kontrakter.BrukerIdType
import no.tilbakekreving.integrasjoner.dokument.kontrakter.IntegrasjonTema
import no.tilbakekreving.integrasjoner.dokument.kontrakter.JournalpostResponse
import no.tilbakekreving.integrasjoner.dokument.saf.SafClient
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SafService(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    private val safClient: SafClient,
) {
    fun hentDokument(
        behandlingId: UUID,
        journalpostId: String,
        dokumentInfoId: String,
        fagsakId: String?,
    ): ByteArray {
        val logContext = SecureLog.Context.medBehandling(fagsakId, behandlingId.toString())
        val token = tokenValidationContextHolder.getTokenValidationContext().firstValidToken ?: throw Feil(
            message = "Finner ikke token",
            logContext = logContext,
            httpStatus = HttpStatus.UNAUTHORIZED,
        )

        return safClient.hentDokument(
            behandlingId = behandlingId,
            journalpostId = journalpostId,
            dokumentInfoId = dokumentInfoId,
            callContext = CallContext.Saksbehandler(
                logContext.behandlingId,
                logContext.fagsystemId,
                userToken = token.encodedToken,
            ),
        )
    }

    fun hentJournalposter(
        tilbakekreving: Tilbakekreving?,
        brukerId: String?,
        temaListe: List<Tema>?,
    ): List<Journalpost> {
        if (tilbakekreving != null) {
            val brukerinfo = tilbakekreving.bruker!!.hentBrukerinfo()
            val fagsak = tilbakekreving.eksternFagsak
            val temaList = listOf(fagsak.hentYtelse().tilTema())
            return mapTilJournalposter(
                safClient.hentJournalposterForBruker(
                    bruker = Bruker(brukerinfo.ident, BrukerIdType.FNR),
                    tema = temaList.map { IntegrasjonTema.valueOf(it.name) },
                    graphqlQuery = graphqlQuery("/saf/journalposterForBruker.graphql"),
                ),
            )
        }
        return mapTilJournalposter(
            safClient.hentJournalposterForBruker(
                bruker = Bruker(id = brukerId!!, type = BrukerIdType.FNR),
                tema = temaListe!!.map { IntegrasjonTema.valueOf(it.name) },
                graphqlQuery = graphqlQuery("/saf/journalposterForBruker.graphql"),
            ),
        )
    }

    private fun mapTilJournalposter(journalposter: List<JournalpostResponse>): List<Journalpost> {
        return journalposter.map { it ->
            Journalpost(
                it.journalpostId,
                Journalposttype.valueOf(it.journalposttype.name),
                Journalstatus.valueOf(it.journalstatus.name),
                it.tema,
                it.tittel,
                Sak(
                    it.sak?.arkivsaksystem,
                    it.sak?.arkivsaksystem,
                    it.sak?.fagsakId,
                    it.sak?.sakstype,
                    it.sak?.fagsaksystem,
                ),
                it.dokumenter?.map {
                    DokumentInfo(
                        it.dokumentInfoId,
                        it.tittel,
                        it.brevkode,
                        it.logiskeVedlegg?.map {
                            LogiskVedlegg(
                                it.logiskVedleggId,
                                it.tittel,
                            )
                        },
                    )
                },
                it.relevanteDatoer?.map {
                    RelevantDato(
                        it.dato,
                        it.datotype,
                    )
                },
                it.eksternReferanseId,
            )
        }
    }
}
