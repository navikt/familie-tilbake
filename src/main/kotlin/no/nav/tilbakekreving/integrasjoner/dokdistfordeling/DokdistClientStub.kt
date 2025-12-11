package no.nav.tilbakekreving.integrasjoner.dokdistfordeling

import no.nav.familie.tilbake.http.RessursException
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstidspunkt
import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstype
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.domain.AdresseTo
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.domain.DistribuerJournalpostResponse
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestClientResponseException
import java.util.UUID

class DokdistClientStub : DokdistClient {
    override fun brevTilUtsending(
        behandlingId: UUID,
        journalpostId: String,
        fagsystem: FagsystemDTO,
        distribusjonstype: Distribusjonstype,
        distribusjonstidspunkt: Distribusjonstidspunkt,
        adresse: AdresseTo?,
        logContext: SecureLog.Context,
    ): DistribuerJournalpostResponse {
        when (journalpostId) {
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
            else -> return DistribuerJournalpostResponse("42")
        }
    }
}
