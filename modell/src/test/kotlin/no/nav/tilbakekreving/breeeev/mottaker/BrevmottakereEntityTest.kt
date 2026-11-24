package no.nav.tilbakekreving.breeeev.mottaker

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.kontrakter.frontend.models.BrevmottakereDto
import no.nav.tilbakekreving.kontrakter.frontend.models.FregAdresseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.FullmektigDto
import no.nav.tilbakekreving.kontrakter.frontend.models.ManuellAdresseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.NorskAdresseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.OrganisasjonDto
import no.nav.tilbakekreving.kontrakter.frontend.models.PersonDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UtenlandskAdresseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VergeDto
import org.junit.jupiter.api.Test
import java.util.UUID

class BrevmottakereEntityTest {
    @Test
    fun `folkeregistrert hovedmottaker uten tilleggsmottaker`() {
        val brevmottakere = Brevmottakere.opprett()

        brevmottakere.tilEntity("1").fraEntity().tilFrontendDto() shouldBe BrevmottakereDto(
            hovedmottaker = FregAdresseDto,
            tilleggsmottaker = null,
        )
    }

    @Test
    fun `norsk hovedmottaker`() {
        val brevmottakere = Brevmottakere(
            hovedmottaker = Addresse.Norsk(
                id = UUID.randomUUID(),
                addresselinje = "Testgata 1",
                postnummer = "0661",
            ),
            tilleggsmottaker = null,
        )

        brevmottakere.tilEntity("1").fraEntity().tilFrontendDto() shouldBe BrevmottakereDto(
            hovedmottaker = NorskAdresseDto(gate = "Testgata 1", postnummer = "0661"),
            tilleggsmottaker = null,
        )
    }

    @Test
    fun `utenlandsk hovedmottaker`() {
        val brevmottakere = Brevmottakere(
            hovedmottaker = Addresse.Utenlandsk(
                id = UUID.randomUUID(),
                land = "SE",
                addresselinje = "Testvägen 1, 111 22 Stockholm",
            ),
            tilleggsmottaker = null,
        )

        brevmottakere.tilEntity("1").fraEntity().tilFrontendDto() shouldBe BrevmottakereDto(
            hovedmottaker = UtenlandskAdresseDto(land = "SE", adresse = "Testvägen 1, 111 22 Stockholm"),
            tilleggsmottaker = null,
        )
    }

    @Test
    fun `verge med manuell adresse`() {
        val vergeId = UUID.randomUUID()
        val brevmottakere = Brevmottakere(
            hovedmottaker = Hovedmottaker.FolkeregistrertAddresse,
            tilleggsmottaker = Tilleggsmottaker.VergeMedManuellAddresse(
                navn = "Verge Vergesen",
                addresse = Addresse.Norsk(
                    id = vergeId,
                    addresselinje = "Vergeveien 2",
                    postnummer = "5000",
                ),
            ),
        )

        brevmottakere.tilEntity("1").fraEntity().tilFrontendDto() shouldBe BrevmottakereDto(
            hovedmottaker = FregAdresseDto,
            tilleggsmottaker = VergeDto(
                id = vergeId,
                mottaker = ManuellAdresseDto(
                    navn = "Verge Vergesen",
                    mottaker = NorskAdresseDto(gate = "Vergeveien 2", postnummer = "5000"),
                ),
            ),
        )
    }

    @Test
    fun `verge med folkeregistrert adresse`() {
        val vergeId = UUID.randomUUID()
        val brevmottakere = Brevmottakere(
            hovedmottaker = Hovedmottaker.FolkeregistrertAddresse,
            tilleggsmottaker = Tilleggsmottaker.VergeMedFolkeregistrertAddresse(
                id = vergeId,
                fødselsnummer = "12345678901",
            ),
        )

        brevmottakere.tilEntity("1").fraEntity().tilFrontendDto() shouldBe BrevmottakereDto(
            hovedmottaker = FregAdresseDto,
            tilleggsmottaker = VergeDto(
                id = vergeId,
                mottaker = PersonDto(navn = "", ident = "12345678901"),
            ),
        )
    }

    @Test
    fun `fullmektig med manuell adresse`() {
        val fullmektigId = UUID.randomUUID()
        val brevmottakere = Brevmottakere(
            hovedmottaker = Hovedmottaker.FolkeregistrertAddresse,
            tilleggsmottaker = Tilleggsmottaker.FullmektigMedManuellAddresse(
                navn = "Fullmektig Fullmektigsen",
                addresse = Addresse.Utenlandsk(
                    id = fullmektigId,
                    land = "DK",
                    addresselinje = "Testvej 3, 1000 København",
                ),
            ),
        )

        brevmottakere.tilEntity("1").fraEntity().tilFrontendDto() shouldBe BrevmottakereDto(
            hovedmottaker = FregAdresseDto,
            tilleggsmottaker = FullmektigDto(
                id = fullmektigId,
                mottaker = ManuellAdresseDto(
                    navn = "Fullmektig Fullmektigsen",
                    mottaker = UtenlandskAdresseDto(land = "DK", adresse = "Testvej 3, 1000 København"),
                ),
            ),
        )
    }

    @Test
    fun `fullmektig med organisasjonsnummer`() {
        val fullmektigId = UUID.randomUUID()
        val brevmottakere = Brevmottakere(
            hovedmottaker = Hovedmottaker.FolkeregistrertAddresse,
            tilleggsmottaker = Tilleggsmottaker.FullmektigMedOrganisasjonsnummer(
                id = fullmektigId,
                navn = "Kontaktperson Kontaktpersonsen",
                organisasjonsnummer = "999999999",
            ),
        )

        brevmottakere.tilEntity("1").fraEntity().tilFrontendDto() shouldBe BrevmottakereDto(
            hovedmottaker = FregAdresseDto,
            tilleggsmottaker = FullmektigDto(
                id = fullmektigId,
                mottaker = OrganisasjonDto(
                    navn = "Kontaktperson Kontaktpersonsen",
                    organisasjonsnavn = "",
                    organisasjonsnummer = "999999999",
                ),
            ),
        )
    }

    @Test
    fun `fullmektig med fødselsnummer`() {
        val fullmektigId = UUID.randomUUID()
        val brevmottakere = Brevmottakere(
            hovedmottaker = Hovedmottaker.FolkeregistrertAddresse,
            tilleggsmottaker = Tilleggsmottaker.FullmektigPerson(
                id = fullmektigId,
                fødselsnummer = "20046912345",
            ),
        )

        brevmottakere.tilEntity("1").fraEntity().tilFrontendDto() shouldBe BrevmottakereDto(
            hovedmottaker = FregAdresseDto,
            tilleggsmottaker = FullmektigDto(
                id = fullmektigId,
                mottaker = PersonDto(
                    navn = "",
                    ident = "20046912345",
                ),
            ),
        )
    }
}
