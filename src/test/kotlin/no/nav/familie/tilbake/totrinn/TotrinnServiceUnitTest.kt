package no.nav.familie.tilbake.totrinn

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.common.repository.Endret
import no.nav.familie.tilbake.common.repository.Sporbar
import no.nav.familie.tilbake.totrinn.domain.Totrinnsvurdering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class TotrinnServiceUnitTest {
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val behandlingsstegstilstandRepository = mockk<BehandlingsstegstilstandRepository>()
    private val totrinnsvurderingRepository = mockk<TotrinnsvurderingRepository>()

    private val totrinnService = TotrinnService(behandlingRepository, behandlingsstegstilstandRepository, totrinnsvurderingRepository)

    val behandlingId = UUID.randomUUID()

    @Test
    fun `skal returnere tidligere beslutter lik null når underkjenningen er over 1 mnd tilbake i tid  `() {
        val toMndTilbakeITid = LocalDateTime.now().minusMonths(2)
        val totrinnsvurdering =
            Totrinnsvurdering(
                behandlingId = behandlingId,
                behandlingssteg = mockk(),
                godkjent = false,
                begrunnelse = "begrunnelse",
                sporbar = Sporbar(endret = Endret(endretTid = toMndTilbakeITid, endretAv = "endretAv")),
            )
        every { totrinnsvurderingRepository.findByBehandlingIdAndAktivIsTrue(any()) } returns listOf(totrinnsvurdering)

        val beslutter = totrinnService.finnForrigeBeslutterMedNyVurderingEllerNull(behandlingId)

        assertThat(beslutter).isNull()
    }

    @Test
    fun `skal returnere tidligere beslutter hvis en vurdering er underkjent og det er under 1 mnd siden`() {
        val treUkerTilbakeITid = LocalDateTime.now().minusWeeks(3)
        val totrinnsvurdering =
            Totrinnsvurdering(
                behandlingId = behandlingId,
                behandlingssteg = mockk(),
                godkjent = false,
                begrunnelse = "begrunnelse",
                sporbar = Sporbar(endret = Endret(endretTid = treUkerTilbakeITid, endretAv = "endretAv")),
            )
        every { totrinnsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandlingId) } returns listOf(totrinnsvurdering)

        val result = totrinnService.finnForrigeBeslutterMedNyVurderingEllerNull(behandlingId)

        assertEquals("endretAv", result)
    }
}
