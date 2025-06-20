package no.nav.familie.tilbake.person

import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.Fagsystem
import no.nav.familie.tilbake.behandling.event.EndretPersonIdentEventPublisher
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.integration.pdl.PdlClient
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import no.nav.familie.tilbake.kontrakter.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.tilbake.log.SecureLog
import org.springframework.stereotype.Service

@Service
class PersonService(
    private val pdlClient: PdlClient,
    private val fagsakRepository: FagsakRepository,
    private val endretPersonIdentEventPublisher: EndretPersonIdentEventPublisher,
) {
    fun hentPersoninfo(
        personIdent: String,
        fagsystem: Fagsystem,
        logContext: SecureLog.Context,
    ): Personinfo {
        val personInfo = pdlClient.hentPersoninfo(personIdent, fagsystem.tilDTO(), logContext)
        // fire event for å oppdatere personIdent når lagret personIdent ikke matcher med PDL.
        if (personIdent != personInfo.ident) {
            val fagsak =
                fagsakRepository.finnFagsakForFagsystemAndIdent(fagsystem, personIdent)
                    ?: throw Feil("Finnes ikke fagsak", logContext = SecureLog.Context.tom())
            endretPersonIdentEventPublisher.fireEvent(personInfo.ident, fagsak.id)
        }
        return personInfo
    }

    fun hentIdenterMedStrengtFortroligAdressebeskyttelse(
        personIdenter: List<String>,
        fagsystem: Fagsystem,
        logContext: SecureLog.Context,
    ): List<String> {
        val adresseBeskyttelseBolk = pdlClient.hentAdressebeskyttelseBolk(personIdenter, fagsystem.tilDTO(), logContext)
        return adresseBeskyttelseBolk
            .filter { (_, person) ->
                person.adressebeskyttelse.any { adressebeskyttelse ->
                    adressebeskyttelse.gradering == ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG ||
                        adressebeskyttelse.gradering == ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG_UTLAND
                }
            }.map { it.key }
    }

    fun hentAktørId(
        personIdent: String,
        fagsystem: Fagsystem,
        logContext: SecureLog.Context,
    ): List<String> {
        val hentIdenter = pdlClient.hentIdenter(personIdent, fagsystem.tilDTO(), logContext)
        return hentIdenter.data.pdlIdenter!!
            .identer
            .filter { it.gruppe == "AKTORID" }
            .map { it.ident }
    }

    fun hentAktivAktørId(
        ident: String,
        fagsystem: Fagsystem,
        logContext: SecureLog.Context,
    ): String {
        val aktørId = hentAktørId(ident, fagsystem, logContext)
        if (aktørId.isEmpty()) error("Finner ingen aktiv aktørId for ident")
        return aktørId.first()
    }
}
