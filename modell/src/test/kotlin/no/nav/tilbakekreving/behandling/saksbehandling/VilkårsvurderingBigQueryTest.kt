package no.nav.tilbakekreving.behandling.saksbehandling

import io.kotest.inspectors.forAll
import io.kotest.inspectors.forSingle
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvNav
import no.nav.tilbakekreving.api.v1.dto.RettsligGrunnlag
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ForårsaketAvBruker
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.Vilkårsvurderingsteg
import no.nav.tilbakekreving.eksternFagsakBehandling
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.test.februar
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.test.mars
import org.junit.jupiter.api.Test

class VilkårsvurderingBigQueryTest {
    @Test
    fun `uvurderte perioder med kopier`() {
        val steg = opprettSteg()
        steg.bigqueryPerioder().shouldBeEmpty()
    }

    @Test
    fun `kjede av kopierte vurderinger beholder egne perioder`() {
        val steg = opprettSteg()
        val perioderFør = steg.tilFrontendDto().flatMap { it.delbarePerioder }
        steg.vurder(1.januar(2021) til 31.januar(2021), forårsaketAvNav().godTro())

        steg.bigqueryPerioder().shouldHaveSize(3).forAll { eksportert ->
            perioderFør.filter { it.periodeId == eksportert.periodeId }.forSingle {
                eksportert.periode.fom shouldBe it.periode.fom
                eksportert.periode.tom shouldBe it.periode.tom
            }
            eksportert.rettsligGrunnlag shouldBe RettsligGrunnlag.GOD_TRO
        }
    }

    @Test
    fun `selvstendig uvurdert periode og kopiert vurdert periode`() {
        val steg = opprettSteg()
        val perioderFør = steg.tilFrontendDto().flatMap { it.delbarePerioder }
        val sisteId = perioderFør.single { it.periode.fom == 1.mars(2021) }.periodeId
        steg.vurder(sisteId, ForårsaketAvBruker.IkkeVurdert())
        steg.vurder(1.januar(2021) til 31.januar(2021), forårsaketAvNav().godTro())

        steg.bigqueryPerioder().shouldHaveSize(2).forAll { eksportert ->
            perioderFør.filter { it.periodeId == eksportert.periodeId }.forSingle {
                eksportert.periode.fom shouldBe it.periode.fom
                eksportert.periode.tom shouldBe it.periode.tom
            }
            eksportert.rettsligGrunnlag shouldBe RettsligGrunnlag.GOD_TRO
        }
        steg.bigqueryPerioder().any { it.periodeId == sisteId } shouldBe false
    }

    @Test
    fun `kopierte perioder følger endret underliggende vurdering`() {
        val steg = opprettSteg()
        steg.vurder(1.januar(2021) til 31.januar(2021), forårsaketAvNav().godTro())
        steg.vurder(1.januar(2021) til 31.januar(2021), forårsaketAvNav().forstod())
        steg.bigqueryPerioder().shouldHaveSize(3).forAll {
            it.rettsligGrunnlag shouldBe RettsligGrunnlag.FORSTOD
        }
    }

    private fun opprettSteg() = Vilkårsvurderingsteg.opprett(
        eksternFagsakBehandling(),
        kravgrunnlag(
            perioder = listOf(
                kravgrunnlagPeriode(1.januar(2021) til 31.januar(2021)),
                kravgrunnlagPeriode(1.februar(2021) til 28.februar(2021)),
                kravgrunnlagPeriode(1.mars(2021) til 31.mars(2021)),
            ),
        ),
    )
}
