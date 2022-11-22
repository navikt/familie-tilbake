package no.nav.familie.tilbake.behandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.familie.kontrakter.felles.klage.FagsystemType
import no.nav.familie.tilbake.behandling.BehandlingMapper.tilVedtakForFagsystem
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultat
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import no.nav.familie.tilbake.config.Constants
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class BehandlingMapperTest {

    @Nested
    inner class TilVedtakForFagsystem {

        @Test
        internal fun `mapper avsluttet behandling`() {
            val behandling = avsluttetBehandling()
            val resultat = tilVedtakForFagsystem(listOf(behandling))
            resultat.shouldHaveSize(1)

            resultat[0].resultat shouldBe "Full tilbakebetaling"
            resultat[0].behandlingstype shouldBe "Tilbakekreving"
            resultat[0].eksternBehandlingId shouldBe behandling.eksternBrukId.toString()
            resultat[0].vedtakstidspunkt shouldBe LocalDate.of(2021, 7, 13).atStartOfDay()
            resultat[0].fagsystemType shouldBe FagsystemType.TILBAKEKREVING
        }

        @Test
        internal fun `mapper ikke behandlinger er henlagt`() {
            val behandling = avsluttetBehandling(behandlingsresultatstype = Behandlingsresultatstype.HENLAGT_FEILOPPRETTET)
            tilVedtakForFagsystem(listOf(behandling)).shouldBeEmpty()
        }

        @Test
        internal fun `mapper ikke behandlinger som ikke er avsluttet`() {
            val behandling = behandling().copy(status = Behandlingsstatus.FATTER_VEDTAK)
            tilVedtakForFagsystem(listOf(behandling)).shouldBeEmpty()
        }

        @Test
        internal fun `forventer at behandling inneholder avsluttet dato`() {
            val behandling = behandling().copy(status = Behandlingsstatus.AVSLUTTET, avsluttetDato = null)
            val exception = shouldThrow<IllegalStateException> {
                tilVedtakForFagsystem(listOf(behandling))
            }
            exception.message shouldContain "Mangler avsluttet dato på behandling="
        }

        @Test
        internal fun `forventer at behandling inneholder sisteResultat`() {
            val behandling = behandling().copy(status = Behandlingsstatus.AVSLUTTET, avsluttetDato = LocalDate.now())
            val exception = shouldThrow<IllegalStateException> {
                tilVedtakForFagsystem(listOf(behandling))
            }
            exception.message shouldContain "Mangler resultat på behandling="
        }

    }

    private fun avsluttetBehandling(
        behandlingsresultatstype: Behandlingsresultatstype = Behandlingsresultatstype.FULL_TILBAKEBETALING
    ) = behandling().copy(
        status = Behandlingsstatus.AVSLUTTET,
        avsluttetDato = LocalDate.of(2021, 7, 13),
        resultater = setOf(Behandlingsresultat(type = behandlingsresultatstype))
    )

    private fun behandling() = Behandling(
        fagsakId = UUID.randomUUID(),
        type = Behandlingstype.TILBAKEKREVING,
        ansvarligSaksbehandler = Constants.BRUKER_ID_VEDTAKSLØSNINGEN,
        behandlendeEnhet = "8020",
        behandlendeEnhetsNavn = "Oslo",
        manueltOpprettet = false
    )
}