package no.nav.familie.tilbake.config

import no.nav.familie.tilbake.integration.pdl.PdlClient
import no.nav.familie.tilbake.integration.pdl.internal.Data
import no.nav.familie.tilbake.integration.pdl.internal.IdentInformasjon
import no.nav.familie.tilbake.integration.pdl.internal.PdlAdressebeskyttelsePerson
import no.nav.familie.tilbake.integration.pdl.internal.PdlHentIdenterResponse
import no.nav.familie.tilbake.integration.pdl.internal.PdlIdenter
import no.nav.familie.tilbake.integration.pdl.internal.PdlKjønnType
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.time.LocalDate

@Primary
@Service
@Profile("mock-pdl")
class PdlClientMock : PdlClient {
    private var hentPersoninfoHitsInternal = mutableListOf<PersoninfoHit>()
    val hentPersoninfoHits: List<PersoninfoHit> get() = hentPersoninfoHitsInternal

    fun reset() {
        hentPersoninfoHitsInternal.clear()
    }

    override fun hentPersoninfo(
        ident: String,
        fagsystem: FagsystemDTO,
        logContext: SecureLog.Context,
    ): Personinfo {
        hentPersoninfoHitsInternal.add(PersoninfoHit(ident, fagsystem))
        val identerDødePersoner = listOf("doed1234")
        val dødsdato = if (identerDødePersoner.contains(ident)) {
            LocalDate.of(2022, 4, 1)
        } else {
            null
        }
        return Personinfo(
            ident = ident,
            fødselsdato = LocalDate.now().minusYears(20),
            navn = "testverdi",
            kjønn = PdlKjønnType.MANN,
            dødsdato = dødsdato,
        )
    }

    override fun hentIdenter(
        personIdent: String,
        fagsystem: FagsystemDTO,
        logContext: SecureLog.Context,
    ): PdlHentIdenterResponse = PdlHentIdenterResponse(
        data = Data(PdlIdenter(identer = listOf(IdentInformasjon("123", "AKTORID")))),
        extensions = null,
        errors = listOf(),
    )

    override fun hentAdressebeskyttelseBolk(
        personIdentList: List<String>,
        fagsystem: FagsystemDTO,
        logContext: SecureLog.Context,
    ): Map<String, PdlAdressebeskyttelsePerson> = emptyMap()

    data class PersoninfoHit(val ident: String, val fagsystem: FagsystemDTO)
}
