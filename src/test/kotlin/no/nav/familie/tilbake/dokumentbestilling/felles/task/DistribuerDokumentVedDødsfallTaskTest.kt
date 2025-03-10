package no.nav.familie.tilbake.dokumentbestilling.felles.task

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forOne
import io.kotest.matchers.shouldBe
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.historikkinnslag.Aktør
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstidspunkt
import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstype
import no.nav.tilbakekreving.kontrakter.Fagsystem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

internal class DistribuerDokumentVedDødsfallTaskTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var historikkService: HistorikkService

    @Autowired
    private lateinit var distribuerDokumentVedDødsfallTask: DistribuerDokumentVedDødsfallTask

    private lateinit var behandling: Behandling
    private lateinit var behandlingId: UUID

    @BeforeEach
    fun init() {
        behandling = Testdata.lagBehandling()
        behandlingId = behandling.id
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(behandling)
    }

    @Test
    fun `skal kjøre ferdig når adressen er blitt oppdatert`() {
        distribuerDokumentVedDødsfallTask.doTask(opprettTask("jp1"))

        assertHistorikkinnslag(
            TilbakekrevingHistorikkinnslagstype.DISTRIBUSJON_BREV_DØDSBO_SUKSESS,
            "Vedtak om tilbakebetaling er sendt",
        )
    }

    @Test
    fun `skal feile når adressen ikke har blitt oppdatert`() {
        val exception =
            shouldThrow<java.lang.RuntimeException> {
                distribuerDokumentVedDødsfallTask.doTask(opprettTask("jpUkjentDødsbo"))
            }

        exception.message shouldBe "org.springframework.web.client.RestClientResponseException: Ukjent adresse dødsbo"
    }

    @Test
    fun `skal opprette historikkinnslag når tasken er for gammel`() {
        distribuerDokumentVedDødsfallTask.doTask(
            opprettTask("jpUkjentDødsbo").copy(
                opprettetTid =
                    LocalDateTime
                        .now()
                        .minusMonths(7),
            ),
        )

        assertHistorikkinnslag(
            TilbakekrevingHistorikkinnslagstype.DISTRIBUSJON_BREV_DØDSBO_FEILET_6_MND,
            "Mottaker har ikke fått dødsboadresse etter 6 måneder. Vedtak om tilbakebetaling er ikke sendt",
        )
    }

    private fun opprettTask(journalpostId: String): Task =
        Task(
            type = DistribuerDokumentVedDødsfallTask.TYPE,
            payload = behandling.id.toString(),
            properties =
                Properties().apply {
                    this["journalpostId"] = journalpostId
                    this["fagsystem"] = Fagsystem.BA.name
                    this["distribusjonstype"] = Distribusjonstype.VIKTIG.name
                    this["distribusjonstidspunkt"] = Distribusjonstidspunkt.KJERNETID.name
                    this["mottager"] = Brevmottager.BRUKER.name
                    this["brevtype"] = Brevtype.VEDTAK.name
                    this["ansvarligSaksbehandler"] = Constants.BRUKER_ID_VEDTAKSLØSNINGEN
                },
        )

    private fun assertHistorikkinnslag(
        historikkinnslagstype: TilbakekrevingHistorikkinnslagstype,
        tekst: String,
    ) {
        historikkService.hentHistorikkinnslag(behandlingId).forOne {
            it.type shouldBe historikkinnslagstype.type
            it.tittel shouldBe historikkinnslagstype.tittel
            it.tekst shouldBe tekst
            it.aktør shouldBe Aktør.Vedtaksløsning.type
            it.opprettetAv shouldBe Aktør.Vedtaksløsning.ident
        }
    }
}
