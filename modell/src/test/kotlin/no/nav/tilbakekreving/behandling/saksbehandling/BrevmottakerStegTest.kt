package no.nav.tilbakekreving.behandling.saksbehandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.kontrakter.brev.ManuellAdresseInfo
import no.nav.tilbakekreving.kontrakter.verge.Vergetype
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
    fun `registrertBrevmottaker skal være DefaultMottaker ved behandlingopprettelse`() {
        val navn = "test bruker"
        val ident = "12312312312"
        val brevmottakerSteg = BrevmottakerSteg.opprett(navn, ident)

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.DefaultMottaker> {
            it.navn shouldBe "test bruker"
        }
    }

    @Test
    fun `registrertBrevmottaker skal være dødsbo etter at dødsbo er lagt til som brevmottaker`() {
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
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.DødsboMottaker> {
            it.navn shouldBe "dødsbo adresse"
        }
    }

    @Test
    fun `registrertBrevmottaker skal være utenlandskadresse etter at utenlandskadresse er lagt til som brevmottaker`() {
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
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.UtenlandskAdresseMottaker> {
            it.navn shouldBe "Navn"
        }
    }

    @Test
    fun `skal kunne oppdatere utenlandskadresse når brevmottakeren er allerede utenlandskadresse`() {
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
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
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
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.UtenlandskAdresseMottaker> {
            it.navn shouldBe "New Mottaker"
            it.manuellAdresseInfo?.adresselinje1 shouldBe "Ny Adresse"
        }
    }

    @Test
    fun `registrertBrevmottaker skal være VergeMottaker etter at VergeMottaker er lagt til som brevmottaker`() {
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
                vergeType = Vergetype.VERGE_FOR_BARN,
            ),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.VergeMottaker> {
            it.navn shouldBe "Verge"
            it.manuellAdresseInfo?.adresselinje1 shouldBe "Adresse"
        }
    }

    @Test
    fun `skal kunne oppdatere VergeMottaker når brevmottakeren er allerede VergeMottaker`() {
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
                vergeType = Vergetype.VERGE_FOR_BARN,
            ),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.VergeMottaker(
                id = UUID.randomUUID(),
                personIdent = "23232323231",
                navn = "Ny Verge",
                manuellAdresseInfo = adresseInfo,
                vergeType = Vergetype.VERGE_FOR_BARN,
            ),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.VergeMottaker> {
            it.navn shouldBe "Ny Verge"
            it.personIdent shouldBe "23232323231"
        }
    }

    @Test
    fun `registrertBrevmottaker skal være FullmektigMottaker etter at FullmektigMottaker er lagt til som brevmottaker`() {
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
                vergeType = Vergetype.ADVOKAT,
            ),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.FullmektigMottaker> {
            it.navn shouldBe "fullmektig"
            it.manuellAdresseInfo?.adresselinje1 shouldBe "Adresse"
        }
    }

    @Test
    fun `skal kunne oppdatere FullmektigMottaker når brevmottakeren er allerede FullmektigMottaker`() {
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
                vergeType = Vergetype.ADVOKAT,
            ),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.FullmektigMottaker(
                id = UUID.randomUUID(),
                personIdent = "11111111111",
                navn = "ny fullmektig",
                manuellAdresseInfo = adresseInfo,
                vergeType = Vergetype.ADVOKAT,
            ),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.FullmektigMottaker> {
            it.navn shouldBe "ny fullmektig"
            it.personIdent shouldBe "11111111111"
        }
    }

    @Test
    fun `registrertBrevmottaker skal være UtenlandskAdresseOgVergeMottaker når Utenlandskaddresse legges til VergeMottaker som brevmottakere`() {
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
                vergeType = Vergetype.VERGE_FOR_BARN,
            ),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.UtenlandskAdresseMottaker(
                id = UUID.randomUUID(),
                navn = "Navn",
                manuellAdresseInfo = adresseInfo,
            ),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.UtenlandskAdresseOgVergeMottaker> {
            it.utenlandskAdresse.navn shouldBe "Navn"
            it.verge.navn shouldBe "Verge"
        }
    }

    @Test
    fun `skal kunne oppdatere Utenlandskaddresse når brevmottakeren er UtenlandskAdresseOgVergeMottaker`() {
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
                vergeType = Vergetype.VERGE_FOR_BARN,
            ),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.UtenlandskAdresseMottaker(
                id = UUID.randomUUID(),
                navn = "Navn",
                manuellAdresseInfo = adresseInfo,
            ),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.UtenlandskAdresseOgVergeMottaker> {
            it.utenlandskAdresse.navn shouldBe "Navn"
            it.utenlandskAdresse.manuellAdresseInfo?.poststed shouldBe "Poststed"
            it.verge.navn shouldBe "Verge"
        }

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
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.UtenlandskAdresseOgVergeMottaker> {
            it.utenlandskAdresse.navn shouldBe "Ny Navn"
            it.utenlandskAdresse.manuellAdresseInfo?.poststed shouldBe "Ny Poststed"
            it.verge.navn shouldBe "Verge"
        }
    }

    @Test
    fun `skal kunne oppdatere VergeMottaker når brevmottakeren er UtenlandskAdresseOgVergeMottake`() {
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
                vergeType = Vergetype.VERGE_FOR_BARN,
            ),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.UtenlandskAdresseMottaker(
                id = UUID.randomUUID(),
                navn = "Navn",
                manuellAdresseInfo = adresseInfo,
            ),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.VergeMottaker(
                id = UUID.randomUUID(),
                personIdent = "11122233345",
                navn = "Verge",
                manuellAdresseInfo = adresseInfo,
                vergeType = Vergetype.VERGE_FOR_BARN,
            ),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.UtenlandskAdresseOgVergeMottaker> {
            it.utenlandskAdresse.navn shouldBe "Navn"
            it.verge.personIdent shouldBe "11122233345"
        }
    }

    @Test
    fun `registrertBrevmottaker skal være UtenlandskAdresseOgFullmektigMottaker når Utenlandskaddresse legges til FullmektigMottaker som brevmottakere`() {
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
                vergeType = Vergetype.ADVOKAT,
            ),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.UtenlandskAdresseMottaker(
                id = UUID.randomUUID(),
                navn = "Navn",
                manuellAdresseInfo = adresseInfo,
            ),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.UtenlandskAdresseOgFullmektigMottaker> {
            it.utenlandskAdresse.navn shouldBe "Navn"
            it.fullmektig.navn shouldBe "Fullmektig"
        }
    }

    @Test
    fun `skal kunne oppdatere Utenlandskaddresse når brevmottakeren er UtenlandskAdresseOgFullmektigMottaker`() {
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
                vergeType = Vergetype.ADVOKAT,
            ),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.UtenlandskAdresseMottaker(
                id = UUID.randomUUID(),
                navn = "Navn",
                manuellAdresseInfo = adresseInfo,
            ),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
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
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.UtenlandskAdresseOgFullmektigMottaker> {
            it.utenlandskAdresse.navn shouldBe "Ny Navn"
            it.utenlandskAdresse.manuellAdresseInfo?.poststed shouldBe "Ny Poststed"
            it.fullmektig.navn shouldBe "Fullmektig"
        }
    }

    @Test
    fun `skal kunne oppdatere FullmektigMottaker når brevmottakeren er UtenlandskAdresseOgFullmektigMottaker`() {
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
                vergeType = Vergetype.ADVOKAT,
            ),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.UtenlandskAdresseMottaker(
                id = UUID.randomUUID(),
                navn = "Navn",
                manuellAdresseInfo = adresseInfo,
            ),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.FullmektigMottaker(
                id = UUID.randomUUID(),
                personIdent = "33333333311",
                navn = "Fullmektig",
                manuellAdresseInfo = adresseInfo,
                vergeType = Vergetype.ADVOKAT,
            ),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.UtenlandskAdresseOgFullmektigMottaker> {
            it.utenlandskAdresse.navn shouldBe "Navn"
            it.fullmektig.personIdent shouldBe "33333333311"
        }
    }

    @Test
    fun `endre UtenlandskAdresseOgFullmektigMottaker til UtenlandskAdresseOgVergeMottaker`() {
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
                vergeType = Vergetype.ADVOKAT,
            ),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.UtenlandskAdresseMottaker(
                id = UUID.randomUUID(),
                navn = "Navn",
                manuellAdresseInfo = adresseInfo,
            ),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.VergeMottaker(
                id = UUID.randomUUID(),
                navn = "Verge",
                manuellAdresseInfo = adresseInfo,
                vergeType = Vergetype.VERGE_FOR_BARN,
            ),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.UtenlandskAdresseOgVergeMottaker> {
            it.utenlandskAdresse.navn shouldBe "Navn"
            it.verge.navn shouldBe "Verge"
        }
    }

    @Test
    fun `endre UtenlandskAdresseOgVergeMottaker til UtenlandskAdresseOgFullmektigMottaker`() {
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
                vergeType = Vergetype.VERGE_FOR_BARN,
            ),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.UtenlandskAdresseMottaker(
                id = UUID.randomUUID(),
                navn = "Navn",
                manuellAdresseInfo = adresseInfo,
            ),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.FullmektigMottaker(
                id = UUID.randomUUID(),
                navn = "Fullmektig",
                manuellAdresseInfo = adresseInfo,
                vergeType = Vergetype.ADVOKAT,
            ),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.UtenlandskAdresseOgFullmektigMottaker> {
            it.utenlandskAdresse.navn shouldBe "Navn"
            it.fullmektig.navn shouldBe "Fullmektig"
        }
    }

    @Test
    fun `kan ikke endre UtenlandskAdresseOgVergeMottaker til Dødsbo`() {
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
                vergeType = Vergetype.VERGE_FOR_BARN,
            ),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.UtenlandskAdresseMottaker(
                id = UUID.randomUUID(),
                navn = "Navn",
                manuellAdresseInfo = adresseInfo,
            ),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        val exception = shouldThrow<ModellFeil.UgyldigOperasjonException> {
            brevmottakerSteg.håndter(
                RegistrertBrevmottaker.DødsboMottaker(
                    id = UUID.randomUUID(),
                    navn = "Dødsbo",
                    manuellAdresseInfo = adresseInfo,
                ),
                Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
            )
        }
        exception.message shouldContain "Ugyldig mottaker"
    }

    @Test
    fun `må kunne ikke fjerne når brevmottakerSteg er deaktivert`() {
        val navn = "test bruker"
        val ident = "12312312312"
        val brevmottakerSteg = BrevmottakerSteg.opprett(navn, ident)

        val exception = shouldThrow<Exception> {
            brevmottakerSteg.fjernManuellBrevmottaker(UUID.randomUUID(), Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
        }
        exception.message shouldContain "BrevmottakerSteg er ikke aktivert."
    }

    @Test
    fun `må kunne ikke fjerne defaultBruker`() {
        val navn = "test bruker"
        val ident = "12312312312"
        val brevmottakerSteg = BrevmottakerSteg.opprett(navn, ident)
        brevmottakerSteg.aktiverSteg()

        val exception = shouldThrow<Exception> {
            brevmottakerSteg.fjernManuellBrevmottaker(UUID.randomUUID(), Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
        }
        exception.message shouldContain "Kan ikke fjerne defaultMotatker."
    }

    @Test
    fun `registrertBrevmottaker skal være DefaultMottaker når dødsboMottaker fjernes`() {
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
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.fjernManuellBrevmottaker(dødsboId, Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()))

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.DefaultMottaker>()
    }

    @Test
    fun `registrertBrevmottaker skal være DefaultMottaker når utenlandskadresse fjernes`() {
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
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.fjernManuellBrevmottaker(utenlandskAdresseId, Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()))

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.DefaultMottaker>()
    }

    @Test
    fun `registrertBrevmottaker skal være DefaultMottaker når VergeMottaker fjernes`() {
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
                vergeType = Vergetype.VERGE_FOR_BARN,
            ),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.fjernManuellBrevmottaker(vergeId, Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()))

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.DefaultMottaker> {
            it.navn shouldBe "test bruker"
        }
    }

    @Test
    fun `registrertBrevmottaker skal være UtenlandskAdresseMottaker når VergeMottaker fjernes fra UtenlandskAdresseOgVergeMottaker`() {
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
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.VergeMottaker(
                id = vergeId,
                navn = "Verge Navn",
                manuellAdresseInfo = adresseInfo,
                vergeType = Vergetype.VERGE_FOR_BARN,
            ),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.fjernManuellBrevmottaker(vergeId, Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()))

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.UtenlandskAdresseMottaker> {
            it.id shouldBe utenlandskAdresseId
        }
    }

    @Test
    fun `registrertBrevmottaker skal være VergeMottaker når UtenlandskAdresseMottaker fjernes fra UtenlandskAdresseOgVergeMottaker`() {
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
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.VergeMottaker(
                id = vergeId,
                navn = "Verge Navn",
                manuellAdresseInfo = adresseInfo,
                vergeType = Vergetype.VERGE_FOR_BARN,
            ),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.fjernManuellBrevmottaker(utenlandskAdresseId, Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()))

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.VergeMottaker> {
            it.navn shouldBe "Verge Navn"
        }
    }

    @Test
    fun `registrertBrevmottaker skal være DefaultMottaker når Fullmektig fjernes`() {
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
                vergeType = Vergetype.ADVOKAT,
            ),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.fjernManuellBrevmottaker(fullmektigId, Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()))

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.DefaultMottaker> {
            it.navn shouldBe "test bruker"
        }
    }

    @Test
    fun `registrertBrevmottaker skal være UtenlandskAdresseMottaker når FullmektigMottaker fjernes fra UtenlandskAdresseOgFullmektigMottaker`() {
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
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.FullmektigMottaker(
                id = fullmektigId,
                navn = "Fullmektig Navn",
                manuellAdresseInfo = adresseInfo,
                vergeType = Vergetype.ADVOKAT,
            ),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.fjernManuellBrevmottaker(fullmektigId, Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()))

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.UtenlandskAdresseMottaker> {
            it.id shouldBe utenlandskAdresseId
        }
    }

    @Test
    fun `registrertBrevmottaker skal være FullmektigMottaker når UtenlandskAdresseMottaker fjernes fra UtenlandskAdresseOgFullmektigMottaker`() {
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
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.håndter(
            RegistrertBrevmottaker.FullmektigMottaker(
                id = fullmektigId,
                navn = "Fullmektig Navn",
                manuellAdresseInfo = adresseInfo,
                vergeType = Vergetype.ADVOKAT,
            ),
            Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
        )

        brevmottakerSteg.fjernManuellBrevmottaker(utenlandskAdresseId, Sporing(UUID.randomUUID().toString(), UUID.randomUUID().toString()))

        brevmottakerSteg.hentRegistrertBrevmottaker().shouldBeInstanceOf<RegistrertBrevmottaker.FullmektigMottaker> {
            it.navn shouldBe "Fullmektig Navn"
        }
    }
}
