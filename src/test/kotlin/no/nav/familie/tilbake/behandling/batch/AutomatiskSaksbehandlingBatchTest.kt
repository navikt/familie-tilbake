package no.nav.familie.tilbake.behandling.batch

import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class AutomatiskSaksbehandlingBatchTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var taskRepository: TaskRepository

    @Autowired
    private lateinit var brevsporingRepository: BrevsporingRepository

    @Autowired
    private lateinit var behandlingskontrollService: BehandlingskontrollService

    @Autowired
    private lateinit var automatiskSaksbehandlingBatch: AutomatiskSaksbehandlingBatch

    private val fagsak: Fagsak = Testdata.fagsak
    private val behandling: Behandling = Testdata.behandling

    @BeforeEach
    fun init() {
        fagsakRepository.insert(fagsak)
        val fagsystemsbehandling = behandling.aktivFagsystemsbehandling.copy(tilbakekrevingsvalg = Tilbakekrevingsvalg
                .OPPRETT_TILBAKEKREVING_UTEN_VARSEL)
        behandlingRepository.insert(behandling.copy(fagsystemsbehandling = setOf(fagsystemsbehandling),
                                                    status = Behandlingsstatus.UTREDES))
        val feilKravgrunnlagBeløp = Testdata.feilKravgrunnlagsbeløp433.copy(nyttBeløp = BigDecimal("100"))
        val ytelKravgrunnlagsbeløp433 =
                Testdata.ytelKravgrunnlagsbeløp433.copy(opprinneligUtbetalingsbeløp = BigDecimal("100"),
                                                        tilbakekrevesBeløp = BigDecimal("100"))

        val kravgrunnlag = Testdata.kravgrunnlag431
                .copy(kontrollfelt = "2019-11-22-19.09.31.458065",
                      perioder = setOf(Testdata.kravgrunnlagsperiode432.copy(beløp = setOf(feilKravgrunnlagBeløp,
                                                                                           ytelKravgrunnlagsbeløp433))))

        kravgrunnlagRepository.insert(kravgrunnlag)
        behandlingsstegstilstandRepository.insert(lagBehandlingsstegstilstand(Behandlingssteg.GRUNNLAG,
                                                                              Behandlingsstegstatus.UTFØRT))
        behandlingsstegstilstandRepository.insert(lagBehandlingsstegstilstand(Behandlingssteg.FAKTA,
                                                                              Behandlingsstegstatus.KLAR))

    }

    @Test
    fun `behandleAutomatisk skal opprette tasker når det finnes en behandling klar for automatisk saksbehandling`() {
        assertDoesNotThrow { automatiskSaksbehandlingBatch.behandleAutomatisk() }
        assertTrue {
            taskRepository.findAll().any {
                it.type == AutomatiskSaksbehandlingTask.TYPE &&
                it.payload == behandling.id.toString()
            }
        }
    }

    @Test
    fun `behandleAutomatisk skal ikke opprette tasker når behandlingen allerede sendte varselsbrev`() {
        val behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        val fagsystemsbehandling = behandling.aktivFagsystemsbehandling.copy(tilbakekrevingsvalg = Tilbakekrevingsvalg
                .OPPRETT_TILBAKEKREVING_MED_VARSEL)
        behandlingRepository.update(behandling.copy(fagsystemsbehandling = setOf(fagsystemsbehandling)))
        brevsporingRepository.insert(Testdata.brevsporing)

        assertDoesNotThrow { automatiskSaksbehandlingBatch.behandleAutomatisk() }
        assertFalse {
            taskRepository.findAll().any {
                it.type == AutomatiskSaksbehandlingTask.TYPE &&
                it.payload == behandling.id.toString()
            }
        }
    }

    @Test
    fun `behandleAutomatisk skal ikke opprette tasker når behandlingen er på vent`() {
        behandlingskontrollService.settBehandlingPåVent(behandling.id,
                                                        Venteårsak.AVVENTER_DOKUMENTASJON,
                                                        LocalDate.now().plusWeeks(2))

        assertDoesNotThrow { automatiskSaksbehandlingBatch.behandleAutomatisk() }
        assertFalse {
            taskRepository.findAll().any {
                it.type == AutomatiskSaksbehandlingTask.TYPE &&
                it.payload == behandling.id.toString()
            }
        }
    }

    @Test
    fun `behandleAutomatisk skal ikke opprette tasker når behandlingens kravgrunnlag som ikke er gammel enn begrensning`() {
        kravgrunnlagRepository.update(kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
                                              .copy(kontrollfelt = LocalDateTime.now()
                                                      .format(DateTimeFormatter.ofPattern("YYYY-MM-dd-HH.mm.ss.SSSSSS"))))

        assertDoesNotThrow { automatiskSaksbehandlingBatch.behandleAutomatisk() }
        assertFalse {
            taskRepository.findAll().any {
                it.type == AutomatiskSaksbehandlingTask.TYPE &&
                it.payload == behandling.id.toString()
            }
        }
    }

    @Test
    fun `behandleAutomatisk skal ikke opprette tasker når behandlingens kravgrunnlag har feilbeløp mer enn begrensning`() {
        kravgrunnlagRepository.update(kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
                                              .copy(perioder = setOf(Testdata.kravgrunnlagsperiode432)))

        assertDoesNotThrow { automatiskSaksbehandlingBatch.behandleAutomatisk() }
        assertFalse {
            taskRepository.findAll().any {
                it.type == AutomatiskSaksbehandlingTask.TYPE &&
                it.payload == behandling.id.toString()
            }
        }
    }

    @Test
    fun `behandleAutomatisk skal ikke opprette tasker når det allerede finnes en feilede tasker`() {
        val task = taskRepository.save(Task(type = AutomatiskSaksbehandlingTask.TYPE, payload = behandling.id.toString()))
        taskRepository.save(taskRepository.findById(task.id).get().copy(status = Status.FEILET))

        assertDoesNotThrow { automatiskSaksbehandlingBatch.behandleAutomatisk() }
        assertFalse {
            taskRepository.findAll().any {
                it.type == AutomatiskSaksbehandlingTask.TYPE &&
                it.payload == behandling.id.toString()
                it.status != Status.FEILET
            }
        }
    }

    private fun lagBehandlingsstegstilstand(behandlingssteg: Behandlingssteg,
                                            behandlingsstegstatus: Behandlingsstegstatus): Behandlingsstegstilstand {
        return Behandlingsstegstilstand(behandlingId = behandling.id,
                                        behandlingssteg = behandlingssteg,
                                        behandlingsstegsstatus = behandlingsstegstatus)
    }
}