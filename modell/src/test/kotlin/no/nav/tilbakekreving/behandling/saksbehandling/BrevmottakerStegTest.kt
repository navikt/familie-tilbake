package no.nav.tilbakekreving.behandling.saksbehandling

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tilbakekreving.kontrakter.brev.ManuellAdresseInfo
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

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.DefaultMottaker> {
            it.navn shouldBe "test bruker"
        }
    }

    @Test
    fun `erstatter bruker addresse med utenlandsk adresse`() {
        val navn = "test bruker"
        val ident = "12312312312"
        val brevmottakerSteg = BrevmottakerSteg.opprett(navn, ident)
        brevmottakerSteg.aktiverSteg()

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.UtenlandskAdresseMottaker(
                id = UUID.randomUUID(),
                navn = "Navn",
                manuellAdresseInfo = adresseInfo,
            ),
        )

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.UtenlandskAdresseMottaker> {
            it.navn shouldBe "Navn"
        }
    }

    @Test
    fun `legge til Verge som brevmottaker`() {
        val navn = "test bruker"
        val ident = "12312312312"
        val brevmottakerSteg = BrevmottakerSteg.opprett(navn, ident)
        brevmottakerSteg.aktiverSteg()

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.VergeMottaker(
                id = UUID.randomUUID(),
                personIdent = "43214321321",
                navn = "Verge",
                manuellAdresseInfo = adresseInfo,
            ),
        )

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.DefaultBrukerAdresseOgVergeMottaker> {
            it.defaultMottaker.navn shouldBe "test bruker"
            it.verge.navn shouldBe "Verge"
        }
    }

    @Test
    fun `legge til fullmektig som brevmottaker`() {
        val navn = "test bruker"
        val ident = "12312312312"
        val brevmottakerSteg = BrevmottakerSteg.opprett(navn, ident)
        brevmottakerSteg.aktiverSteg()

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.FullmektigMottaker(
                id = UUID.randomUUID(),
                personIdent = "43214321321",
                navn = "fullmektig",
                manuellAdresseInfo = adresseInfo,
            ),
        )

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.DefaultBrukerAdresseOgFullmektigMottaker> {
            it.defaultMottaker.navn shouldBe "test bruker"
            it.fullmektig.navn shouldBe "fullmektig"
        }
    }

    @Test
    fun `legge til dødsbo som brevmottaker`() {
        val navn = "dødsbo adresse"
        val ident = "12312312312"
        val brevmottakerSteg = BrevmottakerSteg.opprett(navn, ident)
        brevmottakerSteg.aktiverSteg()

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.DødsboMottaker(
                id = UUID.randomUUID(),
                navn = navn,
                manuellAdresseInfo = adresseInfo,
            ),
        )

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.DødsboMottaker> {
            it.navn shouldBe "dødsbo adresse"
        }
    }

    @Test
    fun `legge til Utenlandskaddresse til Bruker_Og_Verge som brevmottakere`() {
        val navn = "test bruker"
        val ident = "12312312312"
        val brevmottakerSteg = BrevmottakerSteg.opprett(navn, ident)
        brevmottakerSteg.aktiverSteg()

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

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.UtenlandskAdresseOgVergeMottaker> {
            it.utenlandskAdresse.navn shouldBe "Navn"
            it.verge.navn shouldBe "Verge"
        }
    }

    @Test
    fun `legge til Utenlandskaddresse til Bruker_Og_Fullmektig som brevmottakere`() {
        val navn = "test bruker"
        val ident = "12312312312"
        val brevmottakerSteg = BrevmottakerSteg.opprett(navn, ident)
        brevmottakerSteg.aktiverSteg()

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

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.UtenlandskAdresseOgFullmektigMottaker> {
            it.utenlandskAdresse.navn shouldBe "Navn"
            it.fullmektig.navn shouldBe "Fullmektig"
        }
    }

    @Test
    fun `endre UTENLANDSK_ADRESSE_OG_FULLMEKTIG til UTENLANDSK_ADRESSE_OG_VERGE`() {
        val navn = "test bruker"
        val ident = "12312312312"
        val brevmottakerSteg = BrevmottakerSteg.opprett(navn, ident)
        brevmottakerSteg.aktiverSteg()

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

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.UtenlandskAdresseOgFullmektigMottaker>()

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.VergeMottaker(
                id = UUID.randomUUID(),
                navn = "Verge",
                manuellAdresseInfo = adresseInfo,
            ),
        )

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.UtenlandskAdresseOgVergeMottaker> {
            it.utenlandskAdresse.navn shouldBe "Navn"
            it.verge.navn shouldBe "Verge"
        }
    }

    @Test
    fun `endre UTENLANDSK_ADRESSE_OG_VERGE til UTENLANDSK_ADRESSE_OG_FULLMEKTIG`() {
        val navn = "test bruker"
        val ident = "12312312312"
        val brevmottakerSteg = BrevmottakerSteg.opprett(navn, ident)
        brevmottakerSteg.aktiverSteg()

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

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.UtenlandskAdresseOgVergeMottaker>()

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.FullmektigMottaker(
                id = UUID.randomUUID(),
                navn = "Fullmektig",
                manuellAdresseInfo = adresseInfo,
            ),
        )

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.UtenlandskAdresseOgFullmektigMottaker> {
            it.utenlandskAdresse.navn shouldBe "Navn"
            it.fullmektig.navn shouldBe "Fullmektig"
        }
    }

    @Test
    fun `endre UTENLANDSK_ADRESSE_OG_VERGE til Dødsbo`() {
        val navn = "test bruker"
        val ident = "12312312312"
        val brevmottakerSteg = BrevmottakerSteg.opprett(navn, ident)
        brevmottakerSteg.aktiverSteg()

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

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.UtenlandskAdresseOgVergeMottaker>()

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.DødsboMottaker(
                id = UUID.randomUUID(),
                navn = "Dødsbo",
                manuellAdresseInfo = adresseInfo,
            ),
        )

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.DødsboMottaker> {
            it.navn shouldBe "Dødsbo"
        }
    }

    @Test
    fun `fjerner fullmektig adresse fra utenlandskaddresse_og_fullmektig`() {
        val navn = "test bruker"
        val ident = "12312312312"
        val brevmottakerSteg = BrevmottakerSteg.opprett(navn, ident)
        val fullmektigId = UUID.randomUUID()
        val utenlandskAdresseId = UUID.randomUUID()
        brevmottakerSteg.aktiverSteg()

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

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.UtenlandskAdresseMottaker> {
            it.id shouldBe utenlandskAdresseId
        }
    }

    @Test
    fun `fjerner verge adresse fra defaultbruker_og_verge`() {
        val navn = "test bruker"
        val ident = "12312312312"
        val brevmottakerSteg = BrevmottakerSteg.opprett(navn, ident)
        val vergeId = UUID.randomUUID()
        brevmottakerSteg.aktiverSteg()

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.VergeMottaker(
                id = vergeId,
                personIdent = "43214321321",
                navn = "Verge",
                manuellAdresseInfo = adresseInfo,
            ),
        )

        brevmottakerSteg.fjernManuellBrevmottaker(vergeId)

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.DefaultMottaker> {
            it.navn shouldBe "test bruker"
        }
    }
}
