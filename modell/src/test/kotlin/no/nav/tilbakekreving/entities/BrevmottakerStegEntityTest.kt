package no.nav.tilbakekreving.entities

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.behandling.BehandlingObservatørOppsamler
import no.nav.tilbakekreving.behandling.Enhet
import no.nav.tilbakekreving.behandling.saksbehandling.BrevmottakerSteg
import no.nav.tilbakekreving.behandling.saksbehandling.RegistrertBrevmottaker
import no.nav.tilbakekreving.brev.BrevHistorikk
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandlingHistorikk
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakRevurdering
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsårsakstype
import no.nav.tilbakekreving.kontrakter.brev.ManuellAdresseInfo
import no.nav.tilbakekreving.kontrakter.verge.Vergetype
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagHistorikk
import no.nav.tilbakekreving.saksbehandler.Behandler
import no.nav.tilbakekreving.tilstand.TilBehandling
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class BrevmottakerStegEntityTest {
    @Test
    fun `gjennopptat brevmottakersteg med DefaultMottaker`() {
        val fagsakBehandlingHistorikk = EksternFagsakBehandlingHistorikk(mutableListOf())
        val kravgrunnlagHistorikk = KravgrunnlagHistorikk(mutableListOf())
        val brevHistorikk = BrevHistorikk(mutableListOf())
        val behandlingFørLagring = hentBehandling(fagsakBehandlingHistorikk, kravgrunnlagHistorikk, brevHistorikk)

        behandlingFørLagring.brevmottakerSteg = BrevmottakerSteg(
            id = UUID.randomUUID(),
            aktivert = true,
            defaultMottaker = RegistrertBrevmottaker.DefaultMottaker(
                navn = "default navn",
                personIdent = "123456789",
            ),
        )

        val behandlingEtterLagring = behandlingFørLagring.tilEntity("not_needed").fraEntity(fagsakBehandlingHistorikk, kravgrunnlagHistorikk, brevHistorikk)
        behandlingEtterLagring.brevmottakerSteg?.tilFrontendDto()?.size shouldBe 0
    }

    @Test
    fun `gjennopptat brevmottakersteg med UtenlandskAdresseMottaker`() {
        val fagsakBehandlingHistorikk = EksternFagsakBehandlingHistorikk(mutableListOf())
        val kravgrunnlagHistorikk = KravgrunnlagHistorikk(mutableListOf())
        val brevHistorikk = BrevHistorikk(mutableListOf())
        val behandlingFørLagring = hentBehandling(fagsakBehandlingHistorikk, kravgrunnlagHistorikk, brevHistorikk)

        behandlingFørLagring.brevmottakerSteg = BrevmottakerSteg(
            id = UUID.randomUUID(),
            aktivert = true,
            RegistrertBrevmottaker.UtenlandskAdresseMottaker(
                id = UUID.randomUUID(),
                navn = "Utenlandsk Mottaker",
                manuellAdresseInfo = ManuellAdresseInfo(
                    adresselinje1 = "Adresselinje1",
                    poststed = "Oslo",
                    postnummer = "123456789",
                    landkode = "Norge",
                ),
            ),
        )

        val behandlingEtterLagring = behandlingFørLagring.tilEntity("not_needed").fraEntity(fagsakBehandlingHistorikk, kravgrunnlagHistorikk, brevHistorikk)

        behandlingEtterLagring.brevmottakerSteg?.tilFrontendDto()?.size shouldBe 1
        behandlingEtterLagring.brevmottakerSteg?.tilFrontendDto()?.first()?.brevmottaker?.navn shouldBe "Utenlandsk Mottaker"
    }

    @Test
    fun `gjennopptat brevmottakersteg med VergeMottaker`() {
        val fagsakBehandlingHistorikk = EksternFagsakBehandlingHistorikk(mutableListOf())
        val kravgrunnlagHistorikk = KravgrunnlagHistorikk(mutableListOf())
        val brevHistorikk = BrevHistorikk(mutableListOf())
        val behandlingFørLagring = hentBehandling(fagsakBehandlingHistorikk, kravgrunnlagHistorikk, brevHistorikk)

        behandlingFørLagring.brevmottakerSteg = BrevmottakerSteg(
            id = UUID.randomUUID(),
            aktivert = true,
            RegistrertBrevmottaker.VergeMottaker(
                id = UUID.randomUUID(),
                navn = "Verge Mottaker",
                vergeType = Vergetype.VERGE_FOR_BARN,
                manuellAdresseInfo = ManuellAdresseInfo(
                    adresselinje1 = "Adresselinje1",
                    poststed = "Oslo",
                    postnummer = "123456789",
                    landkode = "Norge",
                ),
            ),
        )

        val behandlingEtterLagring = behandlingFørLagring.tilEntity("not_needed").fraEntity(fagsakBehandlingHistorikk, kravgrunnlagHistorikk, brevHistorikk)

        behandlingEtterLagring.brevmottakerSteg?.tilFrontendDto()?.size shouldBe 1
        behandlingEtterLagring.brevmottakerSteg?.tilFrontendDto()?.first()?.brevmottaker?.navn shouldBe "Verge Mottaker"
    }

    @Test
    fun `gjennopptat brevmottakersteg med FullmektigMottaker`() {
        val fagsakBehandlingHistorikk = EksternFagsakBehandlingHistorikk(mutableListOf())
        val kravgrunnlagHistorikk = KravgrunnlagHistorikk(mutableListOf())
        val brevHistorikk = BrevHistorikk(mutableListOf())
        val behandlingFørLagring = hentBehandling(fagsakBehandlingHistorikk, kravgrunnlagHistorikk, brevHistorikk)

        behandlingFørLagring.brevmottakerSteg = BrevmottakerSteg(
            id = UUID.randomUUID(),
            aktivert = true,
            RegistrertBrevmottaker.FullmektigMottaker(
                id = UUID.randomUUID(),
                navn = "Verge Mottaker",
                vergeType = Vergetype.ADVOKAT,
                manuellAdresseInfo = ManuellAdresseInfo(
                    adresselinje1 = "Adresselinje1",
                    poststed = "Oslo",
                    postnummer = "123456789",
                    landkode = "Norge",
                ),
            ),
        )

        val behandlingEtterLagring = behandlingFørLagring.tilEntity("not_needed").fraEntity(fagsakBehandlingHistorikk, kravgrunnlagHistorikk, brevHistorikk)

        behandlingEtterLagring.brevmottakerSteg?.tilFrontendDto()?.size shouldBe 1
        behandlingEtterLagring.brevmottakerSteg?.tilFrontendDto()?.first()?.brevmottaker?.navn shouldBe "Verge Mottaker"
    }

    @Test
    fun `gjennopptat brevmottakersteg med DødsboMottaker`() {
        val fagsakBehandlingHistorikk = EksternFagsakBehandlingHistorikk(mutableListOf())
        val kravgrunnlagHistorikk = KravgrunnlagHistorikk(mutableListOf())
        val brevHistorikk = BrevHistorikk(mutableListOf())
        val behandlingFørLagring = hentBehandling(fagsakBehandlingHistorikk, kravgrunnlagHistorikk, brevHistorikk)

        behandlingFørLagring.brevmottakerSteg = BrevmottakerSteg(
            id = UUID.randomUUID(),
            aktivert = true,
            RegistrertBrevmottaker.DødsboMottaker(
                id = UUID.randomUUID(),
                navn = "Døds Mottaker",
                manuellAdresseInfo = ManuellAdresseInfo(
                    adresselinje1 = "Adresselinje1",
                    poststed = "Oslo",
                    postnummer = "123456789",
                    landkode = "Norge",
                ),
            ),
        )

        val behandlingEtterLagring = behandlingFørLagring.tilEntity("not_needed").fraEntity(fagsakBehandlingHistorikk, kravgrunnlagHistorikk, brevHistorikk)

        behandlingEtterLagring.brevmottakerSteg?.tilFrontendDto()?.size shouldBe 1
        behandlingEtterLagring.brevmottakerSteg?.tilFrontendDto()?.first()?.brevmottaker?.navn shouldBe "Døds Mottaker"
    }

    @Test
    fun `gjennopptat brevmottakersteg med UtenlandskAdresseOgVergeMottaker`() {
        val fagsakBehandlingHistorikk = EksternFagsakBehandlingHistorikk(mutableListOf())
        val kravgrunnlagHistorikk = KravgrunnlagHistorikk(mutableListOf())
        val brevHistorikk = BrevHistorikk(mutableListOf())
        val behandlingFørLagring = hentBehandling(fagsakBehandlingHistorikk, kravgrunnlagHistorikk, brevHistorikk)

        behandlingFørLagring.brevmottakerSteg = BrevmottakerSteg(
            id = UUID.randomUUID(),
            aktivert = true,
            RegistrertBrevmottaker.UtenlandskAdresseOgVergeMottaker(
                id = UUID.randomUUID(),
                utenlandskAdresse = RegistrertBrevmottaker.UtenlandskAdresseMottaker(
                    id = UUID.randomUUID(),
                    navn = "Person i utlandet",
                    manuellAdresseInfo = ManuellAdresseInfo(
                        adresselinje1 = "melkeveien 5",
                        poststed = "jupiter",
                        postnummer = "123456789",
                        landkode = "jup",
                    ),
                ),
                verge = RegistrertBrevmottaker.VergeMottaker(
                    id = UUID.randomUUID(),
                    navn = "Verge",
                    personIdent = "43214321321",
                    vergeType = Vergetype.ADVOKAT,
                ),
            ),
        )

        val behandlingEtterLagring = behandlingFørLagring.tilEntity("not_needed").fraEntity(fagsakBehandlingHistorikk, kravgrunnlagHistorikk, brevHistorikk)
        behandlingEtterLagring.brevmottakerSteg?.tilFrontendDto()?.size shouldBe 2
        behandlingEtterLagring.brevmottakerSteg?.tilFrontendDto()?.first()?.brevmottaker?.navn shouldBe "Person i utlandet"
        behandlingEtterLagring.brevmottakerSteg?.tilFrontendDto()?.get(1)?.brevmottaker?.navn shouldBe "Verge"
    }

    @Test
    fun `gjennopptat brevmottakersteg med UtenlandskAdresseOgFullmektigMottaker`() {
        val fagsakBehandlingHistorikk = EksternFagsakBehandlingHistorikk(mutableListOf())
        val kravgrunnlagHistorikk = KravgrunnlagHistorikk(mutableListOf())
        val brevHistorikk = BrevHistorikk(mutableListOf())
        val behandlingFørLagring = hentBehandling(fagsakBehandlingHistorikk, kravgrunnlagHistorikk, brevHistorikk)

        behandlingFørLagring.brevmottakerSteg = BrevmottakerSteg(
            id = UUID.randomUUID(),
            aktivert = true,
            RegistrertBrevmottaker.UtenlandskAdresseOgFullmektigMottaker(
                id = UUID.randomUUID(),
                utenlandskAdresse = RegistrertBrevmottaker.UtenlandskAdresseMottaker(
                    id = UUID.randomUUID(),
                    navn = "Person i utlandet",
                    manuellAdresseInfo = ManuellAdresseInfo(
                        adresselinje1 = "melkeveien 5",
                        poststed = "jupiter",
                        postnummer = "123456789",
                        landkode = "jup",
                    ),
                ),
                fullmektig = RegistrertBrevmottaker.FullmektigMottaker(
                    id = UUID.randomUUID(),
                    navn = "Fullmekting",
                    organisasjonsnummer = "123456789",
                    vergeType = Vergetype.ADVOKAT,
                ),
            ),
        )

        val behandlingEtterLagring = behandlingFørLagring.tilEntity("not_needed").fraEntity(fagsakBehandlingHistorikk, kravgrunnlagHistorikk, brevHistorikk)
        behandlingEtterLagring.brevmottakerSteg?.tilFrontendDto()?.size shouldBe 2
        behandlingEtterLagring.brevmottakerSteg?.tilFrontendDto()?.first()?.brevmottaker?.navn shouldBe "Person i utlandet"
        behandlingEtterLagring.brevmottakerSteg?.tilFrontendDto()?.get(1)?.brevmottaker?.navn shouldBe "Fullmekting"
    }

    @Test
    fun `gjennopptat brevmottakersteg med DefaultAdresseOgVergeMottaker`() {
        val fagsakBehandlingHistorikk = EksternFagsakBehandlingHistorikk(mutableListOf())
        val kravgrunnlagHistorikk = KravgrunnlagHistorikk(mutableListOf())
        val brevHistorikk = BrevHistorikk(mutableListOf())
        val behandlingFørLagring = hentBehandling(fagsakBehandlingHistorikk, kravgrunnlagHistorikk, brevHistorikk)

        behandlingFørLagring.brevmottakerSteg = BrevmottakerSteg(
            id = UUID.randomUUID(),
            aktivert = true,
            defaultMottaker = RegistrertBrevmottaker.DefaultMottaker(
                id = UUID.randomUUID(),
                navn = "Default navn",
            ),
        ).apply {
            registrertBrevmottaker = RegistrertBrevmottaker.VergeMottaker(
                id = UUID.randomUUID(),
                navn = "Verge",
                personIdent = "43214321321",
                vergeType = Vergetype.ADVOKAT,
            )
        }
        val behandlingEtterLagring = behandlingFørLagring.tilEntity("not_needed").fraEntity(fagsakBehandlingHistorikk, kravgrunnlagHistorikk, brevHistorikk)
        behandlingEtterLagring.brevmottakerSteg?.tilFrontendDto()?.size shouldBe 1
        behandlingEtterLagring.brevmottakerSteg?.tilFrontendDto()?.first()?.brevmottaker?.navn shouldBe "Verge"
    }

    @Test
    fun `gjennopptat brevmottakersteg med DefaultAdresseOgFullmektigMottaker`() {
        val fagsakBehandlingHistorikk = EksternFagsakBehandlingHistorikk(mutableListOf())
        val kravgrunnlagHistorikk = KravgrunnlagHistorikk(mutableListOf())
        val brevHistorikk = BrevHistorikk(mutableListOf())
        val behandlingFørLagring = hentBehandling(fagsakBehandlingHistorikk, kravgrunnlagHistorikk, brevHistorikk)

        behandlingFørLagring.brevmottakerSteg = BrevmottakerSteg(
            id = UUID.randomUUID(),
            aktivert = true,
            defaultMottaker = RegistrertBrevmottaker.DefaultMottaker(
                id = UUID.randomUUID(),
                navn = "Default navn",
            ),
        ).apply {
            registrertBrevmottaker = RegistrertBrevmottaker.FullmektigMottaker(
                id = UUID.randomUUID(),
                navn = "Fullmektig",
                vergeType = Vergetype.ADVOKAT,
                organisasjonsnummer = "123456789",
            )
        }

        val behandlingEtterLagring = behandlingFørLagring.tilEntity("not_needed").fraEntity(fagsakBehandlingHistorikk, kravgrunnlagHistorikk, brevHistorikk)
        behandlingEtterLagring.brevmottakerSteg?.tilFrontendDto()?.size shouldBe 1
        behandlingEtterLagring.brevmottakerSteg?.tilFrontendDto()?.first()?.brevmottaker?.navn shouldBe "Fullmektig"
    }

    private fun hentBehandling(
        fagsakBehandlingHistorikk: EksternFagsakBehandlingHistorikk,
        kravgrunnlagHistorikk: KravgrunnlagHistorikk,
        brevHistorikk: BrevHistorikk,
    ): Behandling {
        val behandlingId = UUID.randomUUID()

        val revurderingInnslag = fagsakBehandlingHistorikk.lagre(
            EksternFagsakRevurdering.Revurdering(
                id = UUID.randomUUID(),
                eksternId = UUID.randomUUID().toString(),
                revurderingsårsak = EksternFagsakRevurdering.Revurderingsårsak.NYE_OPPLYSNINGER,
                årsakTilFeilutbetaling = "abc",
                vedtaksdato = LocalDate.now(),
                utvidedePerioder = emptyList(),
            ),
        )
        // Lagre et nytt innslag så vi er sikker på at det riktige plukkes opp, ikke det nyeste
        fagsakBehandlingHistorikk.lagre(EksternFagsakRevurdering.Ukjent(UUID.randomUUID(), UUID.randomUUID().toString(), null))

        val kravgrunnlag = kravgrunnlagHistorikk.lagre(kravgrunnlag())
        kravgrunnlagHistorikk.lagre(kravgrunnlag())
        val behandler = Behandler.Saksbehandler("A123456")
        return Behandling.nyBehandling(
            id = behandlingId,
            behandlingstype = Behandlingstype.TILBAKEKREVING,
            enhet = Enhet("0425", "NAV Solør"),
            årsak = Behandlingsårsakstype.REVURDERING_KLAGE_KA,
            ansvarligSaksbehandler = behandler,
            eksternFagsakRevurdering = revurderingInnslag,
            kravgrunnlag = kravgrunnlag,
            brevHistorikk = brevHistorikk,
            behandlingObservatør = BehandlingObservatørOppsamler(),
            tilstand = TilBehandling,
        )
    }
}
