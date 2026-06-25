package no.nav.familie.tilbake.config

import no.nav.tilbakekreving.integrasjoner.CallContext
import no.nav.tilbakekreving.integrasjoner.dokument.kontrakter.Bruker
import no.nav.tilbakekreving.integrasjoner.dokument.kontrakter.IntegrasjonTema
import no.nav.tilbakekreving.integrasjoner.dokument.kontrakter.JournalpostResponse
import no.nav.tilbakekreving.integrasjoner.dokument.saf.SafClient
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@Primary
class SafClientMock : SafClient {
    override fun hentDokument(behandlingId: UUID, journalpostId: String, dokumentInfoId: String, callContext: CallContext.Saksbehandler): ByteArray {
        return ByteArray(0)
    }

    override fun hentJournalposterForBruker(bruker: Bruker, tema: List<IntegrasjonTema>, graphqlQuery: String): List<JournalpostResponse> {
        return emptyList()
    }
}
