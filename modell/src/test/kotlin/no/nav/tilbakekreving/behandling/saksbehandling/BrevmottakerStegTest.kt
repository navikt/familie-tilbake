package no.nav.tilbakekreving.behandling.saksbehandling

import io.kotest.assertions.fail
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.kontrakter.brev.ManuellAdresseInfo
import no.nav.tilbakekreving.kontrakter.brev.MottakerType
import org.junit.jupiter.api.Test
import java.util.UUID

class BrevmottakerStegTest {
    val adresseInfo = ManuellAdresseInfo(
        adresselinje1 = "Adresse",
        adresselinje2 = "Adresse",
        postnummer = "1234",
        poststed = "Poststed",
        landkode = "Land",
    )

    @Test
    fun `brevmottakerSteg er deaktivert ved behandlingopprettelse`() {
        val brevmottakerSteg = BrevmottakerSteg.opprett("navn", "12312312312")
        brevmottakerSteg.erStegetAktivert() shouldBe false
    }

    @Test
    fun `brevmottaker skal være brukeren ved behandlingopprettelse`() {
        val navn = "test bruker"
        val ident = "12312312312"
        val brevmottakerSteg = BrevmottakerSteg.opprett(navn, ident)

        brevmottakerSteg.hentRegistrertBrevmottaker().type shouldBe MottakerType.BRUKER
    }

    @Test
    fun `erstatter bruker addresse med utenlandsk adresse`() {
        val navn = "test bruker"
        val ident = "12312312312"
        val brevmottakerSteg = BrevmottakerSteg.opprett(navn, ident)

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.UtenlandskAdresseMottaker(
                id = UUID.randomUUID(),
                navn = "Navn",
                manuellAdresseInfo = adresseInfo,
            ),
        )
        brevmottakerSteg.hentRegistrertBrevmottaker().type shouldBe MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE

        when (val mottaker = brevmottakerSteg.hentRegistrertBrevmottaker()) {
            is RegistrertBrevmottaker.UtenlandskAdresseMottaker -> {
                mottaker.navn shouldBe "Navn"
            }
            else -> fail("Forventet UtenlandskAdresseMottaker, men fikk ${mottaker::class}")
        }
    }

    @Test
    fun `legge til Verge som brevmottaker`() {
        val navn = "test bruker"
        val ident = "12312312312"
        val brevmottakerSteg = BrevmottakerSteg.opprett(navn, ident)

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.VergeMottaker(
                id = UUID.randomUUID(),
                personIdent = "43214321321",
                navn = "Verge",
                manuellAdresseInfo = adresseInfo,
            ),
        )
        brevmottakerSteg.hentRegistrertBrevmottaker().type shouldBe MottakerType.BRUKER_OG_VERGE

        when (val mottaker = brevmottakerSteg.hentRegistrertBrevmottaker()) {
            is RegistrertBrevmottaker.DefaultBrukerAdresseOgVergeMottaker -> {
                mottaker.defaultMottaker.navn shouldBe "test bruker"
                mottaker.verge.navn shouldBe "Verge"
            }
            else -> fail("Forventet DefaultBrukerAdresseOgVergeMottaker, men fikk ${mottaker::class}")
        }
    }

    @Test
    fun `legge til fullmektig som brevmottaker`() {
        val navn = "test bruker"
        val ident = "12312312312"
        val brevmottakerSteg = BrevmottakerSteg.opprett(navn, ident)

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.FullmektigMottaker(
                id = UUID.randomUUID(),
                personIdent = "43214321321",
                navn = "fullmektig",
                manuellAdresseInfo = adresseInfo,
            ),
        )
        brevmottakerSteg.hentRegistrertBrevmottaker().type shouldBe MottakerType.BRUKER_OG_FULLMEKTIG

        when (val mottaker = brevmottakerSteg.hentRegistrertBrevmottaker()) {
            is RegistrertBrevmottaker.DefaultBrukerAdresseOgFullmektigMottaker -> {
                mottaker.defaultMottaker.navn shouldBe "test bruker"
                mottaker.fullmektig.navn shouldBe "fullmektig"
            }
            else -> fail("Forventet DefaultBrukerAdresseOgFullmektigMottaker, men fikk ${mottaker::class}")
        }
    }

    @Test
    fun `legge til dødsbo som brevmottaker`() {
        val navn = "dødsbo adresse"
        val ident = "12312312312"
        val brevmottakerSteg = BrevmottakerSteg.opprett(navn, ident)

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.DødsboMottaker(
                id = UUID.randomUUID(),
                navn = navn,
                manuellAdresseInfo = adresseInfo,
            ),
        )
        brevmottakerSteg.hentRegistrertBrevmottaker().type shouldBe MottakerType.DØDSBO

        when (val mottaker = brevmottakerSteg.hentRegistrertBrevmottaker()) {
            is RegistrertBrevmottaker.DødsboMottaker -> {
                mottaker.navn shouldBe "dødsbo adresse"
            }
            else -> fail("Forventet DødsboMottaker, men fikk ${mottaker::class}")
        }
    }

    @Test
    fun `legge til Utenlandskaddresse til Bruker_Og_Verge som brevmottakere`() {
        val navn = "test bruker"
        val ident = "12312312312"
        val brevmottakerSteg = BrevmottakerSteg.opprett(navn, ident)

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.VergeMottaker(
                id = UUID.randomUUID(),
                personIdent = "43214321321",
                navn = "Verge",
                manuellAdresseInfo = adresseInfo,
            ),
        )

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.UtenlandskAdresseMottaker(
                id = UUID.randomUUID(),
                navn = "Navn",
                manuellAdresseInfo = adresseInfo,
            ),
        )

        brevmottakerSteg.hentRegistrertBrevmottaker().type shouldBe MottakerType.UTENLANDSK_ADRESSE_OG_VERGE

        when (val mottaker = brevmottakerSteg.hentRegistrertBrevmottaker()) {
            is RegistrertBrevmottaker.UtenlandskAdresseOgVergeMottaker -> {
                mottaker.utenlandskAdresse.navn shouldBe "Navn"
                mottaker.verge.navn shouldBe "Verge"
            }
            else -> fail("Forventet UtenlandskAdresseOgVergeMottaker, men fikk ${mottaker::class}")
        }
    }

    @Test
    fun `legge til Utenlandskaddresse til Bruker_Og_Fullmektig som brevmottakere`() {
        val navn = "test bruker"
        val ident = "12312312312"
        val brevmottakerSteg = BrevmottakerSteg.opprett(navn, ident)

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.FullmektigMottaker(
                id = UUID.randomUUID(),
                personIdent = "43214321321",
                navn = "Fullmektig",
                manuellAdresseInfo = adresseInfo,
            ),
        )

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.UtenlandskAdresseMottaker(
                id = UUID.randomUUID(),
                navn = "Navn",
                manuellAdresseInfo = adresseInfo,
            ),
        )

        brevmottakerSteg.hentRegistrertBrevmottaker().type shouldBe MottakerType.UTENLANDSK_ADRESSE_OG_FULLMEKTIG

        when (val mottaker = brevmottakerSteg.hentRegistrertBrevmottaker()) {
            is RegistrertBrevmottaker.UtenlandskAdresseOgFullmektigMottaker -> {
                mottaker.utenlandskAdresse.navn shouldBe "Navn"
                mottaker.fullmektig.navn shouldBe "Fullmektig"
            }
            else -> fail("Forventet UtenlandskAdresseOgFullmektigMottaker, men fikk ${mottaker::class}")
        }
    }

    @Test
    fun `fjerner fullmektig adresse fra utenlandskaddresse_og_fullmektig`() {
        val navn = "test bruker"
        val ident = "12312312312"
        val brevmottakerSteg = BrevmottakerSteg.opprett(navn, ident)
        val fullmektigId = UUID.randomUUID()
        val utenlandskAdresseId = UUID.randomUUID()

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.FullmektigMottaker(
                id = fullmektigId,
                personIdent = "43214321321",
                navn = "Fullmektig",
                manuellAdresseInfo = adresseInfo,
            ),
        )

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.UtenlandskAdresseMottaker(
                id = utenlandskAdresseId,
                navn = "Navn",
                manuellAdresseInfo = adresseInfo,
            ),
        )

        brevmottakerSteg.fjernManuellBrevmottaker(fullmektigId)

        brevmottakerSteg.hentRegistrertBrevmottaker().type shouldBe MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE

        when (val mottaker = brevmottakerSteg.hentRegistrertBrevmottaker()) {
            is RegistrertBrevmottaker.UtenlandskAdresseMottaker -> {
                mottaker.id shouldBe utenlandskAdresseId
            }
            else -> fail("Forventet BRUKER_MED_UTENLANDSK_ADRESSE, men fikk ${mottaker::class}")
        }
    }

    @Test
    fun `fjerner verge adresse fra defaultbruker_og_verge`() {
        val navn = "test bruker"
        val ident = "12312312312"
        val brevmottakerSteg = BrevmottakerSteg.opprett(navn, ident)
        val vergeId = UUID.randomUUID()

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.VergeMottaker(
                id = vergeId,
                personIdent = "43214321321",
                navn = "Verge",
                manuellAdresseInfo = adresseInfo,
            ),
        )

        brevmottakerSteg.fjernManuellBrevmottaker(vergeId)

        brevmottakerSteg.hentRegistrertBrevmottaker().type shouldBe MottakerType.BRUKER

        when (val mottaker = brevmottakerSteg.hentRegistrertBrevmottaker()) {
            is RegistrertBrevmottaker.DefaultMottaker -> {
                mottaker.navn shouldBe "test bruker"
            }
            else -> fail("Forventet DefaultMottaker, men fikk ${mottaker::class}")
        }
    }
}
