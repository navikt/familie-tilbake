package no.nav.familie.tilbake.behandling.batch

import io.kotest.assertions.throwables.shouldNotThrow
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.kontrakter.felles.tilbakekreving.Vergetype
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultat
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandling.domain.Behandlingsvedtak
import no.nav.familie.tilbake.behandling.domain.Fagsystemsbehandling
import no.nav.familie.tilbake.behandling.domain.Varsel
import no.nav.familie.tilbake.behandling.domain.Varselsperiode
import no.nav.familie.tilbake.behandling.domain.Verge
import no.nav.familie.tilbake.data.Testdata
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

internal class AutomatiskBehandlingRydderServiceTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var automatiskBehandlingRydderBatch: AutomatiskBehandlingRydderBatch

    @Test
    fun `skal fjerne behandlinger eldre enn 8 uker som ikke har en kravgrunnlag og ikke er avsluttet`() {
        val fagsak = Testdata.fagsak
        fagsakRepository.insert(fagsak)
        val behandlingEldreEnn8Uker =
            behandlingRepository.insert(
                Testdata
                    .lagBehandling()
                    .copy(status = Behandlingsstatus.UTREDES, opprettetDato = LocalDate.now().minusWeeks(9)),
            )
        val behandlingYngreEnn8Uker = behandlingRepository.insert(unikBehandling((Behandlingsstatus.UTREDES), 7))
        val behandlingEldreEnn8UkerOgAvsluttet = behandlingRepository.insert(unikBehandling((Behandlingsstatus.AVSLUTTET), 10))

        // Verifiser om begge behandlingene finnes i DB
        val behandlingerFoerRydding = behandlingRepository.findAll()
        assert(behandlingerFoerRydding.contains(behandlingEldreEnn8Uker)) {
            "Behandling eldre enn 8 må være i DB før rydding"
        }
        assert(behandlingerFoerRydding.contains(behandlingYngreEnn8Uker)) {
            "Behandling yngre enn 8 uker må være i DB før rydding"
        }
        assert(behandlingerFoerRydding.contains(behandlingEldreEnn8UkerOgAvsluttet)) {
            "Behandling eldre enn 8 uker med status Avsluttet må være i DB før rydding"
        }

        // fjerner behandlinger eldre enn 8 uker med status ikke avslutning
        shouldNotThrow<RuntimeException> { automatiskBehandlingRydderBatch.automatiskFjerningAvGammelBehandlingerUtenKravgrunnlag() }

        // Verifiser at det er bare behandlingen eldre enn 8 uker og status ikke er avsluttet fjernet etter rydding
        val behandligerEtterRydding = behandlingRepository.findAll()
        assert(!behandligerEtterRydding.contains(behandlingEldreEnn8Uker)) {
            "Behandling eldre enn 8 uker burde være fjernet etter rydding"
        }
        assert(behandligerEtterRydding.contains(behandlingYngreEnn8Uker)) {
            "Behandling yngre enn 8 uker burde være i DB etter rydding"
        }
        assert(behandligerEtterRydding.contains(behandlingEldreEnn8UkerOgAvsluttet)) {
            "Behandling eldre enn 8 uker med status Avsluttet burde være i DB etter rydding"
        }
    }

    private fun unikBehandling(
        behandlingStatus: Behandlingsstatus,
        alder: Long,
    ): Behandling =
        Testdata
            .lagBehandling()
            .copy(
                status = behandlingStatus,
                opprettetDato = LocalDate.now().minusWeeks(alder),
                verger =
                    setOf(
                        Verge(
                            ident = "32132132112",
                            type = Vergetype.VERGE_FOR_BARN,
                            orgNr = "testverdi",
                            navn = "testverdi",
                            kilde = "testverdi",
                            begrunnelse = "testverdi",
                        ),
                    ),
                fagsystemsbehandling =
                    setOf(
                        Fagsystemsbehandling(
                            eksternId = UUID.randomUUID().toString(),
                            tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL,
                            revurderingsvedtaksdato = LocalDate.now().minusDays(1),
                            resultat = "OPPHØR",
                            årsak = "testverdi",
                        ),
                    ),
                resultater =
                    setOf(
                        Behandlingsresultat(behandlingsvedtak = Behandlingsvedtak(vedtaksdato = LocalDate.now())),
                    ),
                varsler =
                    setOf(
                        Varsel(
                            varseltekst = "testverdi",
                            varselbeløp = 123,
                            perioder = setOf(Varselsperiode(fom = LocalDate.now().minusMonths(2), tom = LocalDate.now())),
                        ),
                    ),
            )
}
