package no.nav.familie.tilbake.behandling.batch

import io.kotest.assertions.throwables.shouldNotThrow
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.kontrakter.felles.tilbakekreving.Vergetype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingService
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
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevsporing
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.oppgave.LagOppgaveTask
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

internal class AutomatiskBehandlingRydderBatchTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var brevsporingRepository: BrevsporingRepository

    @Autowired
    private lateinit var automatiskBehandlingRydderBatch: AutomatiskBehandlingRydderBatch

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var automatiskBehandlingRydderService: AutomatiskBehandlingRydderService

    @Test
    fun `skal henlegge behandlinger eldre enn 8 uker som ikke har en kravgrunnlag og ikke er avsluttet og ikke har sendt brev`() {
        val fagsak = Testdata.fagsak
        fagsakRepository.insert(fagsak)


        val behandlingEldreEnn8UkerUtenBrev = behandlingRepository.insert(unikBehandling((Behandlingsstatus.UTREDES), 10))
        behandlingRepository.insert(unikBehandling((Behandlingsstatus.AVSLUTTET), 10))
        behandlingRepository.insert(unikBehandling((Behandlingsstatus.UTREDES), 7))

        Assertions.assertEquals(automatiskBehandlingRydderService.hentGammelBehandlingerUtenKravgrunnlag().size, 1)

        // rydder behandlinger eldre enn 8 uker med status ikke avslutning
        shouldNotThrow<RuntimeException> { automatiskBehandlingRydderBatch.automatiskFjerningAvGammelBehandlingerUtenKravgrunnlag() }

        // Verifiser rydding
        val behandlingEldreEnn8UkerUtenBrevEtterRydding = behandlingService.hentBehandling(behandlingEldreEnn8UkerUtenBrev.id)

        Assertions.assertEquals(behandlingEldreEnn8UkerUtenBrevEtterRydding.status, Behandlingsstatus.AVSLUTTET)
    }

    @Test
    fun `skal lage task for manulet henlegging av behandlinger eldre enn 8 uker som ikke har en kravgrunnlag og ikke er avsluttet og har sendt brev`() {
        val fagsak = Testdata.fagsak
        fagsakRepository.insert(fagsak)

        val behandlingEldreEnn8UkerMedBrev = behandlingRepository.insert(unikBehandling((Behandlingsstatus.UTREDES), 9))
        val behandlingEldreEnn8UkerUtenBrev = behandlingRepository.insert(unikBehandling((Behandlingsstatus.UTREDES), 10))
        behandlingRepository.insert(unikBehandling((Behandlingsstatus.AVSLUTTET), 10))
        behandlingRepository.insert(unikBehandling((Behandlingsstatus.UTREDES), 7))

        brevsporingRepository.insert(
            Brevsporing(
                behandlingId = behandlingEldreEnn8UkerMedBrev.id,
                journalpostId = "",
                dokumentId = "",
                brevtype = Brevtype.VARSEL,
            ),
        )

        Assertions.assertNull(taskService.finnTaskMedPayloadOgType(behandlingEldreEnn8UkerMedBrev.id.toString(), Oppgavetype.VurderHenvendelse.toString()))
        Assertions.assertEquals(automatiskBehandlingRydderService.hentGammelBehandlingerUtenKravgrunnlag().size, 2)

        // rydder behandlinger eldre enn 8 uker med status ikke avslutning
        shouldNotThrow<RuntimeException> { automatiskBehandlingRydderBatch.automatiskFjerningAvGammelBehandlingerUtenKravgrunnlag() }

        // Verifiser rydding
        val behandlingEldreEnn8UkerMedBrevEtterRydding = behandlingService.hentBehandling(behandlingEldreEnn8UkerMedBrev.id)
        val behandlingEldreEnn8UkerUtenBrevEtterRydding = behandlingService.hentBehandling(behandlingEldreEnn8UkerUtenBrev.id)
        Assertions.assertEquals(behandlingEldreEnn8UkerMedBrevEtterRydding.status, Behandlingsstatus.UTREDES)

        val opprettetTask: Task? = taskService.finnTaskMedPayloadOgType(behandlingEldreEnn8UkerMedBrev.id.toString(), LagOppgaveTask.TYPE)

        Assertions.assertNotNull(opprettetTask)
        Assertions.assertEquals(opprettetTask?.metadata?.getProperty("oppgavetype"), Oppgavetype.VurderHenvendelse.toString())
        Assertions.assertEquals(opprettetTask?.type, LagOppgaveTask.TYPE)

        Assertions.assertEquals(behandlingEldreEnn8UkerUtenBrevEtterRydding.status, Behandlingsstatus.AVSLUTTET)
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
