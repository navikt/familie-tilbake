package no.nav.tilbakekreving.brev.informasjonsbrev

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.breeeev.InfobrevData
import no.nav.tilbakekreving.breeeev.Infobrevavsnitt
import no.nav.tilbakekreving.kontrakter.frontend.models.BrevmottakerDto
import no.nav.tilbakekreving.kontrakter.frontend.models.HovedavsnittInformasjonsbrevDto
import no.nav.tilbakekreving.kontrakter.frontend.models.InformasjonsbrevDataDto
import no.nav.tilbakekreving.kontrakter.frontend.models.RentekstElementDto
import no.nav.tilbakekreving.kontrakter.frontend.models.SignaturAutomatiskDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UnderavsnittElementDto
import no.nav.tilbakekreving.test.mai
import org.junit.jupiter.api.Test

class InformasjonsbrevServiceTest {
    @Test
    fun `henter data for informasjonsbrev`() {
        val brukerdata = BrevmottakerDto(navn = "Ola Nordmann", personIdent = "12345678901")
        val infobrevData = InfobrevData(
            brukerdata = brukerdata,
            tittel = "Nav har avsluttet saken din om tilbakebetaling",
            innhold = listOf("Første avsnitt", "Andre avsnitt"),
            underavsnitt = listOf(
                Infobrevavsnitt(
                    tittel = "Har du spørsmål?",
                    avsnitt = listOf("Kontakt oss", "Ring oss"),
                ),
            ),
            enhetNavn = "Nav Arbeid og ytelser",
            sendtDato = 7.mai(2026),
            tilbakekrevingId = "tilbakekreving-1",
        )

        InformasjonsbrevService.hentInformasjonsbrevData(infobrevData) shouldBe InformasjonsbrevDataDto(
            hovedavsnitt = HovedavsnittInformasjonsbrevDto(
                tittel = "Nav har avsluttet saken din om tilbakebetaling",
                innhold = listOf("Første avsnitt", "Andre avsnitt"),
            ),
            underavsnitt = listOf(
                UnderavsnittElementDto(
                    tittel = "Har du spørsmål?",
                    underavsnitt = listOf(RentekstElementDto("Kontakt oss"), RentekstElementDto("Ring oss")),
                ),
            ),
            brevGjelder = brukerdata,
            signatur = SignaturAutomatiskDto(enhetNavn = "Nav Arbeid og ytelser"),
            sendtDato = "7. mai 2026",
            saksnummer = "tilbakekreving-1",
        )
    }
}
