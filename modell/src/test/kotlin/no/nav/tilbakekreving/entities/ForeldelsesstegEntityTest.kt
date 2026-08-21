package no.nav.tilbakekreving.entities

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.test.januar
import org.junit.jupiter.api.Test
import java.util.UUID

class ForeldelsesstegEntityTest {
    @Test
    fun `feil rekkefølge på perioder i db blir gjenopprettet i riktig rekkefølge`() {
        val behandlingRef = UUID.randomUUID()
        val periode1 = DatoperiodeEntity(1.januar(2021), 10.januar(2021))
        val periode2 = DatoperiodeEntity(15.januar(2021), 20.januar(2021))
        val entity = ForeldelsesstegEntity(
            id = UUID.randomUUID(),
            behandlingRef = behandlingRef,
            vurdertePerioder = listOf(
                ForeldelseperiodeEntity(
                    id = UUID.randomUUID(),
                    foreldelsesvurderingRef = UUID.randomUUID(),
                    periode = periode2,
                    foreldelsesvurdering = ForeldelsesvurderingEntity(
                        type = ForeldelsesvurderingType.IKKE_VURDERT,
                        begrunnelse = null,
                        frist = null,
                        oppdaget = null,
                    ),
                    endringIKravgrunnlag = null,
                ),
                ForeldelseperiodeEntity(
                    id = UUID.randomUUID(),
                    foreldelsesvurderingRef = UUID.randomUUID(),
                    periode = periode1,
                    foreldelsesvurdering = ForeldelsesvurderingEntity(
                        type = ForeldelsesvurderingType.IKKE_VURDERT,
                        begrunnelse = null,
                        frist = null,
                        oppdaget = null,
                    ),
                    endringIKravgrunnlag = null,
                ),
            ),
            tilbakeført = null,
        )

        val gjenopprettetEntity = entity.fraEntity().tilEntity(behandlingRef)

        gjenopprettetEntity.vurdertePerioder[0].periode shouldBe periode1
        gjenopprettetEntity.vurdertePerioder[1].periode shouldBe periode2
    }
}
