package no.nav.familie.tilbake.integration.pdl

import no.nav.familie.tilbake.integration.pdl.internal.PdlAdressebeskyttelsePerson
import no.nav.familie.tilbake.integration.pdl.internal.PdlHentIdenterResponse
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO

interface PdlClient {
    fun hentPersoninfo(
        ident: String,
        fagsystem: FagsystemDTO,
        logContext: SecureLog.Context,
    ): Personinfo

    fun hentIdenter(
        personIdent: String,
        fagsystem: FagsystemDTO,
        logContext: SecureLog.Context,
    ): PdlHentIdenterResponse

    fun hentAdressebeskyttelseBolk(
        personIdentList: List<String>,
        fagsystem: FagsystemDTO,
        logContext: SecureLog.Context,
    ): Map<String, PdlAdressebeskyttelsePerson>
}
