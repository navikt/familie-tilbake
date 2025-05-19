package no.nav.familie.tilbake.behandling.batch

import io.kotest.inspectors.forAny
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.shouldBe
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.task.TracableTaskService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.kontrakter.Regelverk
import no.nav.tilbakekreving.kontrakter.Tilbakekrevingsvalg
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal class AutomatiskSaksbehandlingBatchTest : OppslagSpringRunnerTest() {
    override val tømDBEtterHverTest = false

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var taskService: TracableTaskService

    @Autowired
    private lateinit var brevsporingRepository: BrevsporingRepository

    @Autowired
    private lateinit var behandlingskontrollService: BehandlingskontrollService

    @Autowired
    private lateinit var automatiskSaksbehandlingBatch: AutomatiskSaksbehandlingBatch

    private lateinit var fagsak: Fagsak
    private lateinit var behandling: Behandling

    @BeforeEach
    fun init() {
        val fagsak = fagsakRepository.insert(Testdata.fagsak())
        behandling = Testdata.lagBehandling(fagsak.id)
        val fagsystemsbehandling =
            behandling.aktivFagsystemsbehandling.copy(
                tilbakekrevingsvalg =
                    Tilbakekrevingsvalg
                        .OPPRETT_TILBAKEKREVING_UTEN_VARSEL,
            )
        behandlingRepository.insert(
            behandling.copy(
                fagsystemsbehandling = setOf(fagsystemsbehandling),
                status = Behandlingsstatus.UTREDES,
            ),
        )
        val feilKravgrunnlagBeløp = Testdata.lagFeilKravgrunnlagsbeløp(nyttBeløp = BigDecimal("100"))
        val ytelKravgrunnlagsbeløp433 =
            Testdata.lagYtelKravgrunnlagsbeløp().copy(
                opprinneligUtbetalingsbeløp = BigDecimal("100"),
                tilbakekrevesBeløp = BigDecimal("100"),
            )

        val kravgrunnlag =
            Testdata
                .lagKravgrunnlag(behandling.id)
                .copy(
                    kontrollfelt = "2019-11-22-19.09.31.458065",
                    perioder =
                        setOf(
                            Testdata.lagKravgrunnlagsperiode().copy(
                                beløp =
                                    setOf(
                                        feilKravgrunnlagBeløp,
                                        ytelKravgrunnlagsbeløp433,
                                    ),
                            ),
                        ),
                )

        kravgrunnlagRepository.insert(kravgrunnlag)
        behandlingsstegstilstandRepository.insert(
            lagBehandlingsstegstilstand(
                Behandlingssteg.GRUNNLAG,
                Behandlingsstegstatus.UTFØRT,
            ),
        )
        behandlingsstegstilstandRepository.insert(
            lagBehandlingsstegstilstand(
                Behandlingssteg.FAKTA,
                Behandlingsstegstatus.KLAR,
            ),
        )
    }

    @Test
    fun `behandleAutomatisk skal opprette tasker når det finnes en behandling klar for automatisk saksbehandling`() {
        automatiskSaksbehandlingBatch.behandleAutomatisk()
        taskService.findAll().shouldHaveSingleElement {
            it.type == AutomatiskSaksbehandlingTask.TYPE &&
                it.payload == behandling.id.toString()
        }
    }

    @Test
    fun `behandleAutomatisk skal ikke opprette tasker for EØS-behandlinger`() {
        val behandling =
            behandlingRepository
                .findByIdOrThrow(behandling.id)
                .copy(regelverk = Regelverk.EØS)
                .also { behandlingRepository.update(it) }

        automatiskSaksbehandlingBatch.behandleAutomatisk()
        taskService
            .findAll()
            .any {
                it.type == AutomatiskSaksbehandlingTask.TYPE &&
                    it.payload == behandling.id.toString()
            }.shouldBeFalse()
    }

    @Test
    fun `behandleAutomatisk skal ikke opprette tasker når behandlingen allerede sendte varselsbrev`() {
        val behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        val fagsystemsbehandling =
            behandling.aktivFagsystemsbehandling.copy(
                tilbakekrevingsvalg =
                    Tilbakekrevingsvalg
                        .OPPRETT_TILBAKEKREVING_MED_VARSEL,
            )
        behandlingRepository.update(behandling.copy(fagsystemsbehandling = setOf(fagsystemsbehandling)))
        brevsporingRepository.insert(Testdata.lagBrevsporing(behandling.id))

        automatiskSaksbehandlingBatch.behandleAutomatisk()
        taskService
            .findAll()
            .any {
                it.type == AutomatiskSaksbehandlingTask.TYPE &&
                    it.payload == behandling.id.toString()
            }.shouldBeFalse()
    }

    @Test
    fun `behandleAutomatisk skal ikke opprette tasker når behandlingen er på vent`() {
        behandlingskontrollService.settBehandlingPåVent(
            behandling.id,
            Venteårsak.AVVENTER_DOKUMENTASJON,
            LocalDate.now().plusWeeks(2),
            SecureLog.Context.tom(),
        )

        automatiskSaksbehandlingBatch.behandleAutomatisk()
        taskService
            .findAll()
            .any {
                it.type == AutomatiskSaksbehandlingTask.TYPE &&
                    it.payload == behandling.id.toString()
            }.shouldBeFalse()
    }

    @Test
    fun `behandleAutomatisk skal ikke opprette tasker når behandlingens kravgrunnlag som ikke er gammel enn begrensning`() {
        kravgrunnlagRepository.update(
            kravgrunnlagRepository
                .findByBehandlingIdAndAktivIsTrue(behandling.id)
                .copy(
                    kontrollfelt =
                        LocalDateTime
                            .now()
                            .format(DateTimeFormatter.ofPattern("YYYY-MM-dd-HH.mm.ss.SSSSSS")),
                ),
        )

        automatiskSaksbehandlingBatch.behandleAutomatisk()
        taskService
            .findAll()
            .any {
                it.type == AutomatiskSaksbehandlingTask.TYPE &&
                    it.payload == behandling.id.toString()
            }.shouldBeFalse()
    }

    @Test
    fun `behandleAutomatisk skal ikke opprette tasker når behandlingens kravgrunnlag har feilbeløp mer enn begrensning`() {
        kravgrunnlagRepository.update(
            kravgrunnlagRepository
                .findByBehandlingIdAndAktivIsTrue(behandling.id)
                .copy(perioder = setOf(Testdata.lagKravgrunnlagsperiode())),
        )

        automatiskSaksbehandlingBatch.behandleAutomatisk()
        taskService
            .findAll()
            .any {
                it.type == AutomatiskSaksbehandlingTask.TYPE &&
                    it.payload == behandling.id.toString()
            }.shouldBeFalse()
    }

    @Test
    fun `behandleAutomatisk skal ikke opprette tasker når det allerede finnes en feilede tasker`() {
        val task = taskService.save(Task(type = AutomatiskSaksbehandlingTask.TYPE, payload = behandling.id.toString()), SecureLog.Context.tom())
        taskService.save(taskService.findById(task.id).copy(status = Status.FEILET), SecureLog.Context.tom())

        automatiskSaksbehandlingBatch.behandleAutomatisk()
        taskService
            .findAll()
            .forAny {
                it.type shouldBe AutomatiskSaksbehandlingTask.TYPE
                it.payload shouldBe behandling.id.toString()
                it.status shouldBe Status.FEILET
            }
    }

    private fun lagBehandlingsstegstilstand(
        behandlingssteg: Behandlingssteg,
        behandlingsstegstatus: Behandlingsstegstatus,
    ): Behandlingsstegstilstand =
        Behandlingsstegstilstand(
            behandlingId = behandling.id,
            behandlingssteg = behandlingssteg,
            behandlingsstegsstatus = behandlingsstegstatus,
        )
}
