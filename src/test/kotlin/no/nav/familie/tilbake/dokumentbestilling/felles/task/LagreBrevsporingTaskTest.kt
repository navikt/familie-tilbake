package no.nav.familie.tilbake.dokumentbestilling.felles.task

import io.kotest.inspectors.forOne
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager.MANUELL_TILLEGGSMOTTAKER
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager.VERGE
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.historikkinnslag.Aktør
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.Properties
import java.util.UUID

internal class LagreBrevsporingTaskTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var brevsporingRepository: BrevsporingRepository

    @Autowired
    private lateinit var lagreBrevsporingTask: LagreBrevsporingTask

    @Autowired
    private lateinit var historikkService: HistorikkService

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    private lateinit var behandling: Behandling

    private val dokumentId: String = "testverdi"
    private val journalpostId: String = "testverdi"

    @BeforeEach
    fun init() {
        behandling =
            Testdata.lagBehandling(
                behandlingStatus = Behandlingsstatus.IVERKSETTER_VEDTAK,
            )
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(behandling)
        behandlingsstegstilstandRepository.insert(Behandlingsstegstilstand(behandlingId = behandling.id, behandlingssteg = Behandlingssteg.AVSLUTTET, behandlingsstegsstatus = Behandlingsstegstatus.KLAR))
    }

    @Test
    fun `doTask skal lagre brevsporing for varselbrev`() {
        lagreBrevsporingTask.doTask(opprettTask(behandling.id, Brevtype.VARSEL))

        assertBrevsporing(Brevtype.VARSEL)
    }

    @Test
    fun `onCompletion skal lage historikk task for varselbrev`() {
        lagreBrevsporingTask.onCompletion(opprettTask(behandling.id, Brevtype.VARSEL))

        assertHistorikkinnslag(TilbakekrevingHistorikkinnslagstype.VARSELBREV_SENDT, Aktør.Vedtaksløsning)
    }

    @Test
    fun `onCompletion skal lage historikk task for manuelt varselbrev`() {
        lagreBrevsporingTask.onCompletion(opprettTask(behandling.id, Brevtype.VARSEL, "Z0000"))

        assertHistorikkinnslag(TilbakekrevingHistorikkinnslagstype.VARSELBREV_SENDT, Aktør.Saksbehandler(behandling.ansvarligSaksbehandler))
    }

    @Test
    fun `doTask skal lagre brevsporing for korrigert varselbrev`() {
        lagreBrevsporingTask.doTask(opprettTask(behandling.id, Brevtype.KORRIGERT_VARSEL))

        assertBrevsporing(Brevtype.KORRIGERT_VARSEL)
    }

    @Test
    fun `onCompletion skal lage historikk task for korrigert varselbrev`() {
        lagreBrevsporingTask.onCompletion(opprettTask(behandling.id, Brevtype.KORRIGERT_VARSEL))

        assertHistorikkinnslag(
            TilbakekrevingHistorikkinnslagstype.KORRIGERT_VARSELBREV_SENDT,
            Aktør.Saksbehandler(behandling.ansvarligSaksbehandler),
        )
    }

    @Test
    fun `doTask skal lagre brevsporing for henleggelsesbrev`() {
        lagreBrevsporingTask.doTask(opprettTask(behandling.id, Brevtype.HENLEGGELSE))

        assertBrevsporing(Brevtype.HENLEGGELSE)
    }

    @Test
    fun `onCompletion skal lage historikk task for henleggelsesbrev`() {
        lagreBrevsporingTask.onCompletion(opprettTask(behandling.id, Brevtype.HENLEGGELSE))

        assertHistorikkinnslag(
            TilbakekrevingHistorikkinnslagstype.HENLEGGELSESBREV_SENDT,
            Aktør.Vedtaksløsning,
        )
    }

    @Test
    fun `doTask skal lagre brevsporing for innhent dokumentasjon`() {
        lagreBrevsporingTask.doTask(opprettTask(behandling.id, Brevtype.INNHENT_DOKUMENTASJON))

        assertBrevsporing(Brevtype.INNHENT_DOKUMENTASJON)
    }

    @Test
    fun `onCompletion skal lage historikk task for innhent dokumentasjon`() {
        lagreBrevsporingTask.onCompletion(opprettTask(behandling.id, Brevtype.INNHENT_DOKUMENTASJON))

        assertHistorikkinnslag(
            TilbakekrevingHistorikkinnslagstype.INNHENT_DOKUMENTASJON_BREV_SENDT,
            Aktør.Saksbehandler(behandling.ansvarligSaksbehandler),
        )
    }

    @Test
    fun `doTask skal lagre brevsporing for vedtaksbrev`() {
        lagreBrevsporingTask.doTask(opprettTask(behandling.id, Brevtype.VEDTAK))

        assertBrevsporing(Brevtype.VEDTAK)
    }

    @Test
    fun `onCompletion skal lage historikk task for vedtaksbrev`() {
        lagreBrevsporingTask.onCompletion(opprettTask(behandling.id, Brevtype.VEDTAK))

        assertHistorikkinnslag(TilbakekrevingHistorikkinnslagstype.VEDTAKSBREV_SENDT, Aktør.Vedtaksløsning)
    }

    @Test
    fun `onCompletion skal lage historikk task for vedtaksbrev når mottaker adresse er ukjent`() {
        lagreBrevsporingTask.onCompletion(
            opprettTask(behandling.id, Brevtype.VEDTAK).also { task ->
                task.metadata.also { it["ukjentAdresse"] = "true" }
            },
        )

        assertHistorikkinnslag(
            TilbakekrevingHistorikkinnslagstype.BREV_IKKE_SENDT_UKJENT_ADRESSE,
            Aktør.Vedtaksløsning,
            "Vedtak om tilbakebetaling er ikke sendt",
        )
    }

    @Test
    fun `onCompletion skal lage historikk task for vedtaksbrev når adresse til dødsbo er ukjent`() {
        lagreBrevsporingTask.onCompletion(
            opprettTask(behandling.id, Brevtype.VEDTAK).also { task ->
                task.metadata.also { it["dødsboUkjentAdresse"] = "true" }
            },
        )

        assertHistorikkinnslag(
            TilbakekrevingHistorikkinnslagstype.BREV_IKKE_SENDT_DØDSBO_UKJENT_ADRESSE,
            Aktør.Vedtaksløsning,
            "Vedtak om tilbakebetaling er ikke sendt",
        )
    }

    @Test
    fun `onCompletion skal lage AvsluttBehandlingTask ved brevtype VEDTAK, men kun når mottakeren ikke er en tilleggsmottaker`() {
        lagreBrevsporingTask.onCompletion(opprettTask(behandling.id, Brevtype.VEDTAK))
        lagreBrevsporingTask.onCompletion(opprettTask(behandling.id, Brevtype.VEDTAK, brevmottager = MANUELL_TILLEGGSMOTTAKER))
        lagreBrevsporingTask.onCompletion(opprettTask(behandling.id, Brevtype.VEDTAK, brevmottager = VERGE))

        behandlingsstegstilstandRepository.findByBehandlingIdAndBehandlingssteg(behandling.id, Behandlingssteg.AVSLUTTET)?.behandlingsstegsstatus shouldBe Behandlingsstegstatus.UTFØRT
        assertHistorikkinnslag(TilbakekrevingHistorikkinnslagstype.BEHANDLING_AVSLUTTET, Aktør.Vedtaksløsning)
    }

    private fun opprettTask(
        behandlingId: UUID,
        brevtype: Brevtype,
        ansvarligSaksbehandler: String? = Constants.BRUKER_ID_VEDTAKSLØSNINGEN,
        brevmottager: Brevmottager = Brevmottager.BRUKER,
    ): Task =
        Task(
            type = LagreBrevsporingTask.TYPE,
            payload = behandlingId.toString(),
            properties =
                Properties().apply {
                    this["dokumentId"] = dokumentId
                    this["journalpostId"] = journalpostId
                    this["brevtype"] = brevtype.name
                    this["mottager"] = brevmottager.name
                    this["ansvarligSaksbehandler"] = ansvarligSaksbehandler
                },
        )

    private fun assertBrevsporing(brevtype: Brevtype) {
        val brevsporing =
            brevsporingRepository.findFirstByBehandlingIdAndBrevtypeOrderBySporbarOpprettetTidDesc(
                behandling.id,
                brevtype,
            )
        brevsporing.shouldNotBeNull()
        brevsporing.dokumentId shouldBe dokumentId
        brevsporing.journalpostId shouldBe journalpostId
    }

    private fun assertHistorikkinnslag(
        historikkinnslagstype: TilbakekrevingHistorikkinnslagstype,
        aktør: Aktør,
        tekst: String? = historikkinnslagstype.tekst,
    ) {
        historikkService
            .hentHistorikkinnslag(behandling.id)
            .forOne {
                it.type shouldBe historikkinnslagstype.type
                it.tittel shouldBe historikkinnslagstype.tittel
                it.tekst shouldBe tekst
                it.aktør shouldBe aktør.type
                it.opprettetAv shouldBe aktør.ident
            }
    }
}
