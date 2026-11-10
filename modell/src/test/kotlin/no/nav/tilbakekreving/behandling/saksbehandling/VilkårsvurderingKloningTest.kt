package no.nav.tilbakekreving.behandling.saksbehandling

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvBruker
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ForårsaketAvBruker
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.Skyldgrad
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.Vilkårsvurderingsteg
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.Vilkårsvurderingsteg.Vilkårsvurderingsperiode
import no.nav.tilbakekreving.eksternFagsakBehandling
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.test.februar
import no.nav.tilbakekreving.test.januar
import org.junit.jupiter.api.Test

class VilkårsvurderingKloningTest {
    @Test
    fun `kloning av kopierte vurderinger`() {
        val original = Vilkårsvurderingsteg.opprett(
            eksternFagsakRevurdering = eksternFagsakBehandling(),
            kravgrunnlagHendelse = kravgrunnlag(
                perioder = listOf(
                    kravgrunnlagPeriode(1.januar(2021) til 31.januar(2021)),
                    kravgrunnlagPeriode(1.februar(2021) til 28.februar(2021)),
                ),
            ),
        )

        val originalePerioder = original.perioder().toList().map { it.shouldBeInstanceOf<Vilkårsvurderingsperiode>() }
        original.vurder(originalePerioder[0].id, forårsaketAvBruker().uaktsomt())

        val klon = original.klon()
        val klonedePerioder = klon.perioder().toList().map { it.shouldBeInstanceOf<Vilkårsvurderingsperiode>() }

        originalePerioder shouldHaveSize 2
        klonedePerioder shouldHaveSize 2
        klonedePerioder[0].vurdering.shouldBeInstanceOf<Skyldgrad.Uaktsomt>()
        klonedePerioder[1].vurdering.shouldBeInstanceOf<ForårsaketAvBruker.KopiertVurdering>().forrigePeriodeId shouldBe klonedePerioder[0].id
        klonedePerioder[0].id shouldNotBe originalePerioder[0].id
        klonedePerioder[1].id shouldNotBe originalePerioder[1].id
    }
}
