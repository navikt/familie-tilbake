package no.nav.familie.tilbake.behandling.batch

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.historikkinnslag.LagHistorikkinnslagTask
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.oppgave.OppdaterOppgaveTask
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

internal class AutomatiskGjenopptaBehandlingBatchTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var taskRepository: TaskRepository

    @Autowired
    private lateinit var automatiskGjenopptaBehandlingBatch: AutomatiskGjenopptaBehandlingBatch

    @Test
    fun `skal gjenoppta behandling som venter på varsel og har allerede fått kravgrunnlag til FAKTA steg`() {
        fagsakRepository.insert(Testdata.fagsak)
        val behandling = behandlingRepository.insert(Testdata.behandling.copy(status = Behandlingsstatus.UTREDES))
        behandlingsstegstilstandRepository.insert(
            Testdata.behandlingsstegstilstand.copy(
                behandlingssteg = Behandlingssteg.VARSEL,
                behandlingsstegsstatus = Behandlingsstegstatus.VENTER,
                venteårsak = Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
                tidsfrist = LocalDate.now().minusWeeks(4)
            )
        )
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)
        shouldNotThrow<RuntimeException> { automatiskGjenopptaBehandlingBatch.automatiskGjenopptaBehandling() }

        behandlingRepository.findByIdOrThrow(behandling.id).ansvarligSaksbehandler.shouldBe("VL")

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        behandlingsstegstilstand.any {
            it.behandlingssteg == Behandlingssteg.VARSEL &&
                it.behandlingsstegsstatus == Behandlingsstegstatus.UTFØRT
        }.shouldBeTrue()
        behandlingsstegstilstand.any {
            it.behandlingssteg == Behandlingssteg.FAKTA &&
                it.behandlingsstegsstatus == Behandlingsstegstatus.KLAR
        }.shouldBeTrue()

        taskRepository.findAll().any {
            it.type == LagHistorikkinnslagTask.TYPE &&
                it.payload == behandling.id.toString() &&
                it.metadata["historikkinnslagstype"] == TilbakekrevingHistorikkinnslagstype.BEHANDLING_GJENOPPTATT.name &&
                it.metadata["aktør"] == Aktør.VEDTAKSLØSNING.name
        }.shouldBeTrue()

        taskRepository.findAll().any {
            it.type == OppdaterOppgaveTask.TYPE &&
                it.payload == behandling.id.toString() &&
                it.metadata["beskrivelse"] == "Behandling er tatt av vent" &&
                it.metadata["frist"] == LocalDate.now().toString() &&
                it.metadata["saksbehandler"] == "VL"
        }.shouldBeTrue()
    }

    @Test
    fun `skal gjenoppta behandling som venter på varsel og har ikke fått kravgrunnlag til GRUNNLAG steg`() {
        fagsakRepository.insert(Testdata.fagsak)
        val behandling = behandlingRepository.insert(Testdata.behandling.copy(status = Behandlingsstatus.UTREDES))
        behandlingsstegstilstandRepository.insert(
            Testdata.behandlingsstegstilstand.copy(
                behandlingssteg = Behandlingssteg.VARSEL,
                behandlingsstegsstatus = Behandlingsstegstatus.VENTER,
                venteårsak = Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
                tidsfrist = LocalDate.now().minusWeeks(4)
            )
        )
        shouldNotThrow<RuntimeException> { automatiskGjenopptaBehandlingBatch.automatiskGjenopptaBehandling() }

        behandlingRepository.findByIdOrThrow(behandling.id).ansvarligSaksbehandler.shouldBe("VL")

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        behandlingsstegstilstand.any {
            it.behandlingssteg == Behandlingssteg.VARSEL &&
                it.behandlingsstegsstatus == Behandlingsstegstatus.UTFØRT
        }.shouldBeTrue()
        behandlingsstegstilstand.any {
            it.behandlingssteg == Behandlingssteg.GRUNNLAG &&
                it.behandlingsstegsstatus == Behandlingsstegstatus.VENTER &&
                it.venteårsak == Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG
        }.shouldBeTrue()

        taskRepository.findAll().any {
            it.type == LagHistorikkinnslagTask.TYPE &&
                it.payload == behandling.id.toString() &&
                it.metadata["historikkinnslagstype"] == TilbakekrevingHistorikkinnslagstype.BEHANDLING_GJENOPPTATT.name &&
                it.metadata["aktør"] == Aktør.VEDTAKSLØSNING.name
        }.shouldBeTrue()

        taskRepository.findAll().any {
            it.type == LagHistorikkinnslagTask.TYPE &&
                it.payload == behandling.id.toString() &&
                it.metadata["historikkinnslagstype"] == TilbakekrevingHistorikkinnslagstype.BEHANDLING_PÅ_VENT.name &&
                it.metadata["aktør"] == Aktør.VEDTAKSLØSNING.name
        }.shouldBeTrue()

        taskRepository.findAll().any {
            it.type == OppdaterOppgaveTask.TYPE &&
                it.payload == behandling.id.toString() &&
                it.metadata["beskrivelse"] == "Behandling er tatt av vent" &&
                it.metadata["frist"] == LocalDate.now().toString() &&
                it.metadata["saksbehandler"] == "VL"
        }.shouldBeTrue()

        taskRepository.findAll().any {
            it.type == OppdaterOppgaveTask.TYPE &&
                it.payload == behandling.id.toString() &&
                it.metadata["beskrivelse"] == Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG.beskrivelse
        }.shouldBeTrue()
    }

    @Test
    fun `skal gjenoppta behandling som venter på avvent dokumentasjon`() {
        fagsakRepository.insert(Testdata.fagsak)
        val behandling = behandlingRepository.insert(Testdata.behandling.copy(status = Behandlingsstatus.UTREDES))
        behandlingsstegstilstandRepository.insert(
            Testdata.behandlingsstegstilstand.copy(
                behandlingssteg = Behandlingssteg.VILKÅRSVURDERING,
                behandlingsstegsstatus = Behandlingsstegstatus.VENTER,
                venteårsak = Venteårsak.AVVENTER_DOKUMENTASJON,
                tidsfrist = LocalDate.now().minusWeeks(1)
            )
        )
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)
        shouldNotThrow<RuntimeException> { automatiskGjenopptaBehandlingBatch.automatiskGjenopptaBehandling() }

        behandlingRepository.findByIdOrThrow(behandling.id).ansvarligSaksbehandler.shouldBe("VL")

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        behandlingsstegstilstand.any {
            it.behandlingssteg == Behandlingssteg.VILKÅRSVURDERING &&
                it.behandlingsstegsstatus == Behandlingsstegstatus.KLAR
            it.venteårsak == null && it.tidsfrist == null
        }.shouldBeTrue()

        taskRepository.findAll().any {
            it.type == LagHistorikkinnslagTask.TYPE &&
                it.payload == behandling.id.toString() &&
                it.metadata["historikkinnslagstype"] == TilbakekrevingHistorikkinnslagstype.BEHANDLING_GJENOPPTATT.name &&
                it.metadata["aktør"] == Aktør.VEDTAKSLØSNING.name
        }.shouldBeTrue()

        taskRepository.findAll().any {
            it.type == OppdaterOppgaveTask.TYPE &&
                it.payload == behandling.id.toString() &&
                it.metadata["beskrivelse"] == "Behandling er tatt av vent" &&
                it.metadata["frist"] == LocalDate.now().toString() &&
                it.metadata["saksbehandler"] == "VL"
        }.shouldBeTrue()
    }
}
