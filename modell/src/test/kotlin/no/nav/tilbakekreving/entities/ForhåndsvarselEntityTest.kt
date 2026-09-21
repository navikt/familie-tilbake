package no.nav.tilbakekreving.entities

import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tilbakekreving.HistorikkStub.Companion.fakeReferanse
import no.nav.tilbakekreving.KlokkeStub
import no.nav.tilbakekreving.behandling.Forhåndsvarsel
import no.nav.tilbakekreving.behandling.UttalelseInfo
import no.nav.tilbakekreving.behandling.UttalelseVurdering
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.brev.Varselbrev
import no.nav.tilbakekreving.defaultFeatures
import no.nav.tilbakekreving.kontrakter.frontend.models.ArsakTilTilbakeforingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.IkkeVurdertDto
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagSammenligning.OverordnetSammendrag
import no.nav.tilbakekreving.test.februar
import no.nav.tilbakekreving.test.januar
import org.junit.jupiter.api.Test
import java.util.UUID

class ForhåndsvarselEntityTest {
    @Test
    fun `forhåndsvarsel som må vurderes på nytt beholder brukeruttalelse`() {
        val forhåndsvarsel = Forhåndsvarsel.opprett().also {
            it.lagreOpprinneligFrist(31.januar(2021))
            it.lagreUttalelse(
                uttalelseVurdering = UttalelseVurdering.JA_ETTER_FORHÅNDSVARSEL,
                uttalelseInfo = UttalelseInfo(
                    id = UUID.randomUUID(),
                    uttalelsesdato = 20.januar(2021),
                    hvorBrukerenUttalteSeg = "Telefon",
                    uttalelseBeskrivelse = "Brukeren har uttalt seg om forhåndsvarselet",
                ),
                kommentar = "Uttalelsen er vurdert",
            )
        }
        forhåndsvarsel.nyttKravgrunnlagMottatt(
            OverordnetSammendrag(
                fom = 1.januar(2021),
                tom = 31.januar(2021),
                nyttBeløp = 1500.kroner,
                gammeltBeløp = 1000.kroner,
            ),
        )

        val gjenopprettet = forhåndsvarsel.tilEntity(UUID.randomUUID()).fraEntity()

        gjenopprettet.nyForhåndsvarselTilFrontend(varselbrev(), KlokkeStub(1.februar(2021))).should {
            it.forhaandsvarselSteg.shouldBeInstanceOf<IkkeVurdertDto>()
            it.brukeruttalelse?.beskrivelse shouldBe "Brukeren har uttalt seg om forhåndsvarselet"
            it.tilbakeført shouldBe ArsakTilTilbakeforingDto.NyttKravgrunnlag
            it.ferdigvurdert shouldBe false
        }
        gjenopprettet.tilEntity(UUID.randomUUID()).vurderingstype shouldBe ForhåndsvarselVurderingstype.MÅ_VURDERES_PÅ_NYTT
    }

    @Test
    fun `forhåndsvarsel uten brukeruttalelse forblir i MÅ_VURDERES_PÅ_NYTT etter gjenopprettelse`() {
        val forhåndsvarsel = Forhåndsvarsel.opprett().also {
            it.lagreOpprinneligFrist(31.januar(2021))
        }
        forhåndsvarsel.nyttKravgrunnlagMottatt(
            OverordnetSammendrag(
                fom = 1.januar(2021),
                tom = 31.januar(2021),
                nyttBeløp = 1500.kroner,
                gammeltBeløp = 1000.kroner,
            ),
        )

        val gjenopprettet = forhåndsvarsel.tilEntity(UUID.randomUUID()).fraEntity()

        gjenopprettet.nyForhåndsvarselTilFrontend(varselbrev(), KlokkeStub(1.februar(2021))).should {
            it.forhaandsvarselSteg.shouldBeInstanceOf<IkkeVurdertDto>()
            it.brukeruttalelse shouldBe null
            it.tilbakeført shouldBe ArsakTilTilbakeforingDto.NyttKravgrunnlag
            it.ferdigvurdert shouldBe false
        }
        gjenopprettet.tilEntity(UUID.randomUUID()).vurderingstype shouldBe ForhåndsvarselVurderingstype.MÅ_VURDERES_PÅ_NYTT
    }

    private fun varselbrev() = Varselbrev.opprett(
        "",
        fakeReferanse(kravgrunnlag()),
        "",
        defaultFeatures(),
        KlokkeStub(1.februar(2021)),
    )
}
