package no.nav.tilbakekreving.entities

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.instanceOf
import no.nav.tilbakekreving.behandling.saksbehandling.SærligGrunn
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.KanUnnlates4xRettsgebyr
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.NivåAvForståelse
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ReduksjonSærligeGrunner
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.Vilkårsvurderingsteg
import no.nav.tilbakekreving.eksternFagsakBehandling
import no.nav.tilbakekreving.kontrakter.frontend.models.ForstoDto
import no.nav.tilbakekreving.kontrakter.frontend.models.ForstoEllerBurdeForstaattDto
import no.nav.tilbakekreving.kontrakter.frontend.models.IkkeAktueltDto
import no.nav.tilbakekreving.kontrakter.frontend.models.NeiSaerligeGrunnerDto
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.test.januar
import org.junit.jupiter.api.Test
import java.util.UUID

class VilkårsvurderingstegEntityTest {
    @Test
    fun `vilkårsvurdering med feil rekkefølge på perioder i db blir gjenopprettet i riktig rekkefølge`() {
        val behandlingId = UUID.randomUUID()
        val vilkårsvurderingFør = Vilkårsvurderingsteg.opprett(
            eksternFagsakBehandling(),
            kravgrunnlag(
                perioder = listOf(
                    kravgrunnlagPeriode(1.januar(2021) til 12.januar(2021)),
                    kravgrunnlagPeriode(15.januar(2021) til 28.januar(2021)),
                ),
            ),
        )
        val perioder = vilkårsvurderingFør.hentVilkårsvurderingsperioder()
        vilkårsvurderingFør.splittVilkårsvurdering(perioder[1].periodeId)
        val entity = vilkårsvurderingFør.tilEntity(behandlingId).let {
            // Simuler at periodene endrer rekkefølge i DB
            it.copy(vurderinger = it.vurderinger.reversed())
        }

        vilkårsvurderingFør.vurdertePerioderForBrev(emptySet()) shouldBe entity.fraEntity().vurdertePerioderForBrev(emptySet())
    }

    @Test
    fun `vilkårsvurdering Forstod med Unnlatelse og særligGrunner`() {
        val behandlingId = UUID.randomUUID()
        val vilkårsvurderingFør = Vilkårsvurderingsteg.opprett(
            eksternFagsakBehandling(),
            kravgrunnlag(
                perioder = listOf(
                    kravgrunnlagPeriode(1.januar(2021) til 12.januar(2021)),
                    kravgrunnlagPeriode(15.januar(2021) til 28.januar(2021)),
                ),
            ),
        )

        val perioder = vilkårsvurderingFør.hentVilkårsvurderingsperioder()
        vilkårsvurderingFør.vurder(
            perioder.first().periodeId,
            NivåAvForståelse.Forstod(
                begrunnelseMottakersForståelse = "begrunnelse mottakers forståelse",
                begrunnelse = "Begrunnelse",
                kanUnnlates4XRettsgebyr = KanUnnlates4xRettsgebyr.ErOver4xRettsgebyr(
                    ReduksjonSærligeGrunner(
                        begrunnelse = "Begrunnelse",
                        grunner = setOf(SærligGrunn.Annet("Annet begrunnelse")),
                        skalReduseres = ReduksjonSærligeGrunner.SkalReduseres.Nei,
                    ),
                ),
            ),
        )

        val entity = vilkårsvurderingFør.tilEntity(behandlingId)
        val vurdering = entity.fraEntity().tilFrontendDto().firstOrNull {
            it.id == perioder.first().periodeId
        }

        vurdering shouldNotBeNull {
            val valgDto = valg as ForstoEllerBurdeForstaattDto
            val forsto = valgDto.forståelse as ForstoDto
            val unnlatelse = forsto.unnlatelse as IkkeAktueltDto
            val særligeGrunner = unnlatelse.erDetSærligeGrunner as NeiSaerligeGrunnerDto

            valg shouldBe instanceOf<ForstoEllerBurdeForstaattDto>()
            valgDto.forståelse shouldBe instanceOf<ForstoDto>()
            forsto.unnlatelse shouldBe instanceOf<IkkeAktueltDto>()
            unnlatelse.erDetSærligeGrunner shouldBe instanceOf<NeiSaerligeGrunnerDto>()

            særligeGrunner.annetBegrunnelse shouldBe "Annet begrunnelse"
            særligeGrunner.særligeGrunnerMot.size shouldBe 1
            særligeGrunner.særligeGrunnerMot.first().beskrivelse shouldBe "Annet"
            særligeGrunner.særligeGrunnerMot.first().moment shouldBe "ANNET"
            særligeGrunner.begrunnelse shouldBe "Begrunnelse"
        }
    }
}
