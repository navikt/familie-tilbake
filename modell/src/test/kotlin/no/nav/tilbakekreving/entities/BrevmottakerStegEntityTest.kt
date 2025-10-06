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
    fun `gjennopptat brevmottakersteg med en mottakerregistrert`() {
        val behandlingId = UUID.randomUUID()
        val fagsakBehandlingHistorikk = EksternFagsakBehandlingHistorikk(mutableListOf())
        val kravgrunnlagHistorikk = KravgrunnlagHistorikk(mutableListOf())
        val brevHistorikk = BrevHistorikk(mutableListOf())

        val revurderingInnslag = fagsakBehandlingHistorikk.lagre(
            EksternFagsakRevurdering.Revurdering(
                internId = UUID.randomUUID(),
                eksternId = UUID.randomUUID().toString(),
                revurderingsårsak = EksternFagsakRevurdering.Revurderingsårsak.NYE_OPPLYSNINGER,
                årsakTilFeilutbetaling = "abc",
                vedtaksdato = LocalDate.now(),
                utvidedePerioder = emptyList(),
            ),
        )
        // Lagre et nytt innslag så vi er sikker på at det riktige plukkes opp, ikke det nyeste
        fagsakBehandlingHistorikk.lagre(EksternFagsakRevurdering.Ukjent(UUID.randomUUID(), null))

        val kravgrunnlag = kravgrunnlagHistorikk.lagre(kravgrunnlag())
        kravgrunnlagHistorikk.lagre(kravgrunnlag())
        val behandler = Behandler.Saksbehandler("A123456")
        val behandlingFørLagring = Behandling.nyBehandling(
            internId = behandlingId,
            eksternId = behandlingId,
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

        behandlingFørLagring.brevmottakerSteg = BrevmottakerSteg(
            aktivert = true,
            RegistrertBrevmottaker.DefaultMottaker(
                navn = "default navn",
                personIdent = "123456789",
            ),
        )

        var behandlingEtterLagring = behandlingFørLagring.tilEntity().fraEntity(fagsakBehandlingHistorikk, kravgrunnlagHistorikk, brevHistorikk)
        behandlingEtterLagring.brevmottakerSteg?.tilFrontendDto()?.size shouldBe 0

        behandlingFørLagring.brevmottakerSteg = BrevmottakerSteg(
            aktivert = true,
            RegistrertBrevmottaker.UtenlandskAdresseMottaker(
                id = UUID.randomUUID(),
                navn = "default navn",
                manuellAdresseInfo = ManuellAdresseInfo(
                    adresselinje1 = "Adresselinje1",
                    poststed = "Oslo",
                    postnummer = "123456789",
                    landkode = "Norge",
                ),
            ),
        )

        behandlingEtterLagring = behandlingFørLagring.tilEntity().fraEntity(fagsakBehandlingHistorikk, kravgrunnlagHistorikk, brevHistorikk)

        behandlingEtterLagring.brevmottakerSteg?.tilFrontendDto()?.size shouldBe 1
        behandlingEtterLagring.brevmottakerSteg?.tilFrontendDto()?.first()?.brevmottaker?.navn shouldBe "default navn"
    }

    @Test
    fun `gjennopptat brevmottakersteg med en flere mottakere`() {
        val behandlingId = UUID.randomUUID()
        val fagsakBehandlingHistorikk = EksternFagsakBehandlingHistorikk(mutableListOf())
        val kravgrunnlagHistorikk = KravgrunnlagHistorikk(mutableListOf())
        val brevHistorikk = BrevHistorikk(mutableListOf())

        val revurderingInnslag = fagsakBehandlingHistorikk.lagre(
            EksternFagsakRevurdering.Revurdering(
                internId = UUID.randomUUID(),
                eksternId = UUID.randomUUID().toString(),
                revurderingsårsak = EksternFagsakRevurdering.Revurderingsårsak.NYE_OPPLYSNINGER,
                årsakTilFeilutbetaling = "abc",
                vedtaksdato = LocalDate.now(),
                utvidedePerioder = emptyList(),
            ),
        )
        // Lagre et nytt innslag så vi er sikker på at det riktige plukkes opp, ikke det nyeste
        fagsakBehandlingHistorikk.lagre(EksternFagsakRevurdering.Ukjent(UUID.randomUUID(), null))

        val kravgrunnlag = kravgrunnlagHistorikk.lagre(kravgrunnlag())
        kravgrunnlagHistorikk.lagre(kravgrunnlag())
        val behandler = Behandler.Saksbehandler("A123456")
        val behandlingFørLagring = Behandling.nyBehandling(
            internId = behandlingId,
            eksternId = behandlingId,
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

        behandlingFørLagring.brevmottakerSteg = BrevmottakerSteg(
            aktivert = true,
            RegistrertBrevmottaker.DefaultMottaker(
                navn = "default navn",
                personIdent = "123456789",
            ),
        )

        behandlingFørLagring.brevmottakerSteg = BrevmottakerSteg(
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

        val behandlingEtterLagring = behandlingFørLagring.tilEntity().fraEntity(fagsakBehandlingHistorikk, kravgrunnlagHistorikk, brevHistorikk)
        behandlingEtterLagring.brevmottakerSteg?.tilFrontendDto()?.size shouldBe 2
        behandlingEtterLagring.brevmottakerSteg?.tilFrontendDto()?.first()?.brevmottaker?.navn shouldBe "Person i utlandet"
        behandlingEtterLagring.brevmottakerSteg?.tilFrontendDto()?.get(1)?.brevmottaker?.navn shouldBe "Fullmekting"
    }
}
