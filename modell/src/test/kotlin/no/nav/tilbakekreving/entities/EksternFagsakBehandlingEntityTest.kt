package no.nav.tilbakekreving.entities

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.test.januar
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class EksternFagsakBehandlingEntityTest {
    @Test
    fun `feil rekkefølge på utvidede perioder i db blir gjenopprettet i riktig rekkefølge`() {
        val eksternFagsakRef = UUID.randomUUID()
        val periode1 = DatoperiodeEntity(1.januar(2021), 10.januar(2021))
        val periode2 = DatoperiodeEntity(15.januar(2021), 20.januar(2021))
        val entity = EksternFagsakBehandlingEntity(
            id = UUID.randomUUID(),
            eksternFagsakRef = eksternFagsakRef,
            type = EksternFagsakBehandlingType.BEHANDLING,
            eksternId = "1",
            revurderingsårsak = RevurderingsårsakType.NYE_OPPLYSNINGER,
            årsakTilFeilutbetaling = "Årsak",
            vedtaksdato = LocalDate.now(),
            utvidedePerioder = listOf(
                UtvidetPeriodeEntity(
                    id = UUID.randomUUID(),
                    eksternFagsakBehandlingRef = UUID.randomUUID(),
                    kravgrunnlagPeriode = periode2,
                    vedtaksperiode = periode2,
                ),
                UtvidetPeriodeEntity(
                    id = UUID.randomUUID(),
                    eksternFagsakBehandlingRef = UUID.randomUUID(),
                    kravgrunnlagPeriode = periode1,
                    vedtaksperiode = periode1,
                ),
            ),
            url = null,
            opprettet = LocalDateTime.now(),
        )

        val gjenopprettetEntity = entity.fraEntity().tilEntity(eksternFagsakRef)

        gjenopprettetEntity.utvidedePerioder?.get(0)?.kravgrunnlagPeriode shouldBe periode1
        gjenopprettetEntity.utvidedePerioder?.get(0)?.vedtaksperiode shouldBe periode1
        gjenopprettetEntity.utvidedePerioder?.get(1)?.kravgrunnlagPeriode shouldBe periode2
        gjenopprettetEntity.utvidedePerioder?.get(1)?.vedtaksperiode shouldBe periode2
    }
}
