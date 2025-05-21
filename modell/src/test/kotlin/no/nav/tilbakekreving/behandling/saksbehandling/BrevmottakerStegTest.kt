package no.nav.tilbakekreving.behandling.saksbehandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
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
    fun `endre bruker mottaker til dødsbo som brevmottaker`() {
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
    fun `oppdatere utenlandskadresse i utenlandskadresse`() {
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

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.UtenlandskAdresseMottaker(
                id = UUID.randomUUID(),
                navn = "New Mottaker",
                manuellAdresseInfo = ManuellAdresseInfo(
                    adresselinje1 = "Ny Adresse",
                    adresselinje2 = "Ny Adresse",
                    postnummer = "4321",
                    poststed = "Ny Poststed",
                    landkode = "Ny Land",
                ),
            ),
        )

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.UtenlandskAdresseMottaker> {
            it.navn shouldBe "New Mottaker"
            it.manuellAdresseInfo?.adresselinje1 shouldBe "Ny Adresse"
        }
    }

    @Test
    fun `legge til Verge til bruker som brevmottaker`() {
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
    fun `oppdater Verge i Defaultbruker_og_verge`() {
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
            RegistrertBrevmottaker.VergeMottaker(
                id = UUID.randomUUID(),
                personIdent = "23232323231",
                navn = "Ny Verge",
                manuellAdresseInfo = adresseInfo,
            ),
        )

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.DefaultBrukerAdresseOgVergeMottaker> {
            it.defaultMottaker.navn shouldBe "test bruker"
            it.verge.navn shouldBe "Ny Verge"
            it.verge.personIdent shouldBe "23232323231"
        }
    }

    @Test
    fun `legge til fullmektig til bruker som brevmottaker`() {
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
    fun `oppdater Fullmektig i Defaultbruker_og_fullmektig`() {
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

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.FullmektigMottaker(
                id = UUID.randomUUID(),
                personIdent = "11111111111",
                navn = "ny fullmektig",
                manuellAdresseInfo = adresseInfo,
            ),
        )

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.DefaultBrukerAdresseOgFullmektigMottaker> {
            it.defaultMottaker.navn shouldBe "test bruker"
            it.fullmektig.navn shouldBe "ny fullmektig"
            it.fullmektig.personIdent shouldBe "11111111111"
        }
    }

    @Test
    fun `legge til Utenlandskaddresse til Defaultbruker_Og_Verge som brevmottakere`() {
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
    fun `oppdatere Utenlandskaddresse i Utenlandskadresse_Og_Verge`() {
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

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.UtenlandskAdresseMottaker(
                id = UUID.randomUUID(),
                navn = "Ny Navn",
                manuellAdresseInfo = ManuellAdresseInfo(
                    adresselinje1 = "Ny Adresse",
                    adresselinje2 = "Adresse",
                    postnummer = "1222",
                    poststed = "Ny Poststed",
                    landkode = "Ny Land",
                ),
            ),
        )

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.UtenlandskAdresseOgVergeMottaker> {
            it.utenlandskAdresse.navn shouldBe "Ny Navn"
            it.utenlandskAdresse.manuellAdresseInfo?.poststed shouldBe "Ny Poststed"
            it.verge.navn shouldBe "Verge"
        }
    }

    @Test
    fun `oppdatere Verge i Utenlandskadresse_Og_Verge`() {
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

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.VergeMottaker(
                id = UUID.randomUUID(),
                personIdent = "11122233345",
                navn = "Verge",
                manuellAdresseInfo = adresseInfo,
            ),
        )

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.UtenlandskAdresseOgVergeMottaker> {
            it.utenlandskAdresse.navn shouldBe "Navn"
            it.verge.personIdent shouldBe "11122233345"
        }
    }

    @Test
    fun `legge til Utenlandskaddresse til Defaultbruker_Og_Fullmektig`() {
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
    fun `oppdatere Utenlandskaddresse i Utenlandskadresse_Og_Fullmektig`() {
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

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.UtenlandskAdresseMottaker(
                id = UUID.randomUUID(),
                navn = "Ny Navn",
                manuellAdresseInfo = ManuellAdresseInfo(
                    adresselinje1 = "Ny Adresse",
                    adresselinje2 = "Adresse",
                    postnummer = "1222",
                    poststed = "Ny Poststed",
                    landkode = "Ny Land",
                ),
            ),
        )

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.UtenlandskAdresseOgFullmektigMottaker> {
            it.utenlandskAdresse.navn shouldBe "Ny Navn"
            it.utenlandskAdresse.manuellAdresseInfo?.poststed shouldBe "Ny Poststed"
            it.fullmektig.navn shouldBe "Fullmektig"
        }
    }

    @Test
    fun `oppdatere Fullmektig i Utenlandskadresse_Og_Fullmektig`() {
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

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.FullmektigMottaker(
                id = UUID.randomUUID(),
                personIdent = "33333333311",
                navn = "Fullmektig",
                manuellAdresseInfo = adresseInfo,
            ),
        )

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.UtenlandskAdresseOgFullmektigMottaker> {
            it.utenlandskAdresse.navn shouldBe "Navn"
            it.fullmektig.personIdent shouldBe "33333333311"
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
    fun `kan ikke endre UTENLANDSK_ADRESSE_OG_VERGE til Dødsbo`() {
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

        val exception = shouldThrow<IllegalArgumentException> {
            brevmottakerSteg.håndter(
                RegistrertBrevmottaker.DødsboMottaker(
                    id = UUID.randomUUID(),
                    navn = "Dødsbo",
                    manuellAdresseInfo = adresseInfo,
                ),
            )
        }
        exception.message shouldContain "Ugyldig mottaker"
    }

    @Test
    fun `kan ikke fjerne brevmottakerSteg er deaktivert`() {
        val navn = "test bruker"
        val ident = "12312312312"
        val brevmottakerSteg = BrevmottakerSteg.opprett(navn, ident)

        val exception = shouldThrow<Exception> {
            brevmottakerSteg.fjernManuellBrevmottaker(UUID.randomUUID())
        }
        exception.message shouldContain "BrevmottakerSteg er ikke aktivert."
    }

    @Test
    fun `kan ikke fjerne defaultBruker`() {
        val navn = "test bruker"
        val ident = "12312312312"
        val brevmottakerSteg = BrevmottakerSteg.opprett(navn, ident)
        brevmottakerSteg.aktiverSteg()

        val exception = shouldThrow<Exception> {
            brevmottakerSteg.fjernManuellBrevmottaker(UUID.randomUUID())
        }
        exception.message shouldContain "Kan ikke fjerne defaultMotatker."
    }

    @Test
    fun `fjerner dødsboMottaker`() {
        val navn = "test bruker"
        val ident = "12312312312"
        val brevmottakerSteg = BrevmottakerSteg.opprett(navn, ident)
        brevmottakerSteg.aktiverSteg()

        val dødsboId = UUID.randomUUID()
        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.DødsboMottaker(
                id = dødsboId,
                navn = "Navn",
                manuellAdresseInfo = adresseInfo,
            ),
        )

        brevmottakerSteg.fjernManuellBrevmottaker(dødsboId)

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.DefaultMottaker>()
    }

    @Test
    fun `fjerner utenlandskadresse`() {
        val navn = "test bruker"
        val ident = "12312312312"
        val brevmottakerSteg = BrevmottakerSteg.opprett(navn, ident)
        brevmottakerSteg.aktiverSteg()

        val utenlandskAdresseId = UUID.randomUUID()
        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.UtenlandskAdresseMottaker(
                id = utenlandskAdresseId,
                navn = "Navn",
                manuellAdresseInfo = adresseInfo,
            ),
        )

        brevmottakerSteg.fjernManuellBrevmottaker(utenlandskAdresseId)

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.DefaultMottaker>()
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

    @Test
    fun `fjerner verge adresse fra utenlandskaddresse_og_verge`() {
        val navn = "test bruker"
        val ident = "12312312312"
        val brevmottakerSteg = BrevmottakerSteg.opprett(navn, ident)
        val vergeId = UUID.randomUUID()
        val utenlandskAdresseId = UUID.randomUUID()
        brevmottakerSteg.aktiverSteg()

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.UtenlandskAdresseMottaker(
                id = utenlandskAdresseId,
                navn = "Navn",
                manuellAdresseInfo = adresseInfo,
            ),
        )

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.VergeMottaker(
                id = vergeId,
                navn = "Fullmektig Navn",
                manuellAdresseInfo = adresseInfo,
            ),
        )

        brevmottakerSteg.fjernManuellBrevmottaker(vergeId)

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.UtenlandskAdresseMottaker> {
            it.id shouldBe utenlandskAdresseId
        }
    }

    @Test
    fun `fjerner utenlandskadresse fra utenlandskaddresse_og_verge`() {
        val navn = "test bruker"
        val ident = "12312312312"
        val brevmottakerSteg = BrevmottakerSteg.opprett(navn, ident)
        val vergeId = UUID.randomUUID()
        val utenlandskAdresseId = UUID.randomUUID()
        brevmottakerSteg.aktiverSteg()

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.UtenlandskAdresseMottaker(
                id = utenlandskAdresseId,
                navn = "Navn",
                manuellAdresseInfo = adresseInfo,
            ),
        )

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.VergeMottaker(
                id = vergeId,
                navn = "Verge Navn",
                manuellAdresseInfo = adresseInfo,
            ),
        )

        brevmottakerSteg.fjernManuellBrevmottaker(utenlandskAdresseId)

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.DefaultBrukerAdresseOgVergeMottaker> {
            it.defaultMottaker.navn shouldBe "test bruker"
            it.verge.navn shouldBe "Verge Navn"
        }
    }

    @Test
    fun `fjerner fullmektig adresse fra defaultbruker_og_fullmektig`() {
        val navn = "test bruker"
        val ident = "12312312312"
        val brevmottakerSteg = BrevmottakerSteg.opprett(navn, ident)
        val fullmektigId = UUID.randomUUID()
        brevmottakerSteg.aktiverSteg()

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.FullmektigMottaker(
                id = fullmektigId,
                personIdent = "43214321321",
                navn = "Verge",
                manuellAdresseInfo = adresseInfo,
            ),
        )

        brevmottakerSteg.fjernManuellBrevmottaker(fullmektigId)

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.DefaultMottaker> {
            it.navn shouldBe "test bruker"
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
            RegistrertBrevmottaker.UtenlandskAdresseMottaker(
                id = utenlandskAdresseId,
                navn = "Navn",
                manuellAdresseInfo = adresseInfo,
            ),
        )

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.FullmektigMottaker(
                id = fullmektigId,
                navn = "Fullmektig Navn",
                manuellAdresseInfo = adresseInfo,
            ),
        )

        brevmottakerSteg.fjernManuellBrevmottaker(fullmektigId)

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.UtenlandskAdresseMottaker> {
            it.id shouldBe utenlandskAdresseId
        }
    }

    @Test
    fun `fjerner utenlandskadresse fra utenlandskaddresse_og_fullmektig`() {
        val navn = "test bruker"
        val ident = "12312312312"
        val brevmottakerSteg = BrevmottakerSteg.opprett(navn, ident)
        val fullmektigId = UUID.randomUUID()
        val utenlandskAdresseId = UUID.randomUUID()
        brevmottakerSteg.aktiverSteg()

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.UtenlandskAdresseMottaker(
                id = utenlandskAdresseId,
                navn = "Navn",
                manuellAdresseInfo = adresseInfo,
            ),
        )

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.FullmektigMottaker(
                id = fullmektigId,
                navn = "Fullmektig Navn",
                manuellAdresseInfo = adresseInfo,
            ),
        )

        brevmottakerSteg.fjernManuellBrevmottaker(utenlandskAdresseId)

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.DefaultBrukerAdresseOgFullmektigMottaker> {
            it.defaultMottaker.navn shouldBe "test bruker"
            it.fullmektig.navn shouldBe "Fullmektig Navn"
        }
    }
}
