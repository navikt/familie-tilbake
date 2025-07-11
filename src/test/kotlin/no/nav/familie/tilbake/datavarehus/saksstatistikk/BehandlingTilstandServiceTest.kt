package no.nav.familie.tilbake.datavarehus.saksstatistikk

import io.kotest.matchers.date.shouldBeBetween
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingService
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.Fagsystem
import no.nav.familie.tilbake.behandling.Ytelsestype
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultat
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.task.TracableTaskService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingService
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.log.LogService
import no.nav.tilbakekreving.api.v1.dto.BehandlingPåVentDto
import no.nav.tilbakekreving.kontrakter.Faktainfo
import no.nav.tilbakekreving.kontrakter.OpprettTilbakekrevingRequest
import no.nav.tilbakekreving.kontrakter.Periode
import no.nav.tilbakekreving.kontrakter.Tilbakekrevingsvalg
import no.nav.tilbakekreving.kontrakter.Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL
import no.nav.tilbakekreving.kontrakter.Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL
import no.nav.tilbakekreving.kontrakter.Varsel
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsresultatstype
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.kontrakter.ytelse.YtelsestypeDTO
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.util.UUID

class BehandlingTilstandServiceTest : OppslagSpringRunnerTest() {
    override val tømDBEtterHverTest = false

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var taskService: TracableTaskService

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var faktaFeilutbetalingService: FaktaFeilutbetalingService

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var logService: LogService

    private lateinit var service: BehandlingTilstandService

    private lateinit var behandling: Behandling
    private lateinit var fagsak: Fagsak

    @BeforeEach
    fun setup() {
        service =
            BehandlingTilstandService(
                behandlingRepository,
                behandlingsstegstilstandRepository,
                fagsakRepository,
                taskService,
                faktaFeilutbetalingService,
                logService,
            )
        fagsak = fagsakRepository.insert(Testdata.fagsak())
        behandling = behandlingRepository.insert(Testdata.lagBehandling(fagsakId = fagsak.id))
    }

    @Test
    fun `hentBehandlingensTilstand skal utlede behandlingtilstand for nyopprettet behandling`() {
        val fagsak = fagsakRepository.insert(Testdata.fagsak())
        val behandling =
            behandlingService.opprettBehandling(
                lagOpprettTilbakekrevingRequest(
                    true,
                    OPPRETT_TILBAKEKREVING_MED_VARSEL,
                    fagsak,
                ),
            )
        val tilstand = service.hentBehandlingensTilstand(behandling.id)

        tilstand.ytelsestype shouldBe Ytelsestype.BARNETRYGD
        tilstand.fagsystem shouldBe Fagsystem.BA
        tilstand.saksnummer shouldBe fagsak.eksternFagsakId
        tilstand.behandlingUuid shouldBe behandling.eksternBrukId
        tilstand.referertFagsaksbehandling shouldBe behandling.aktivFagsystemsbehandling.eksternId
        tilstand.behandlingstype shouldBe Behandlingstype.TILBAKEKREVING
        tilstand.behandlingsstatus shouldBe Behandlingsstatus.UTREDES
        tilstand.behandlingsresultat shouldBe Behandlingsresultatstype.IKKE_FASTSATT

        tilstand.venterPåBruker shouldBe true
        tilstand.venterPåØkonomi shouldBe false
        tilstand.behandlingErManueltOpprettet shouldBe false
        tilstand.funksjoneltTidspunkt.shouldBeBetween(OffsetDateTime.now().minusMinutes(1), OffsetDateTime.now().plusSeconds(1))
        tilstand.tekniskTidspunkt shouldBe null
        tilstand.ansvarligBeslutter shouldBe behandling.ansvarligBeslutter
        tilstand.ansvarligSaksbehandler shouldBe behandling.ansvarligSaksbehandler
        tilstand.ansvarligEnhet shouldBe behandling.behandlendeEnhet
        tilstand.totalFeilutbetaltBeløp shouldBe BigDecimal("1500")
        tilstand.totalFeilutbetaltPeriode.shouldNotBeNull()
        tilstand.totalFeilutbetaltPeriode!!.should {
            it.fom == YearMonth.now().minusMonths(1).atDay(1) &&
                it.tom == YearMonth.now().atEndOfMonth()
        }
    }

    @Test
    fun `hentBehandlingensTilstand skal utlede behandlingtilstand for nyopprettet behandling uten varsel`() {
        val fagsak = fagsakRepository.insert(Testdata.fagsak())
        val behandling =
            behandlingService.opprettBehandling(
                lagOpprettTilbakekrevingRequest(
                    false,
                    OPPRETT_TILBAKEKREVING_UTEN_VARSEL,
                    fagsak,
                ),
            )
        val tilstand = service.hentBehandlingensTilstand(behandling.id)

        tilstand.ytelsestype shouldBe Ytelsestype.BARNETRYGD
        tilstand.fagsystem shouldBe Fagsystem.BA
        tilstand.saksnummer shouldBe fagsak.eksternFagsakId
        tilstand.behandlingUuid shouldBe behandling.eksternBrukId
        tilstand.referertFagsaksbehandling shouldBe behandling.aktivFagsystemsbehandling.eksternId
        tilstand.behandlingstype shouldBe Behandlingstype.TILBAKEKREVING
        tilstand.behandlingsstatus shouldBe Behandlingsstatus.UTREDES
        tilstand.behandlingsresultat shouldBe Behandlingsresultatstype.IKKE_FASTSATT

        tilstand.venterPåBruker shouldBe false
        tilstand.venterPåØkonomi shouldBe true
        tilstand.behandlingErManueltOpprettet shouldBe false
        tilstand.funksjoneltTidspunkt.shouldBeBetween(OffsetDateTime.now().minusMinutes(1), OffsetDateTime.now().plusSeconds(1))
        tilstand.tekniskTidspunkt shouldBe null
        tilstand.ansvarligBeslutter shouldBe behandling.ansvarligBeslutter
        tilstand.ansvarligSaksbehandler shouldBe behandling.ansvarligSaksbehandler
        tilstand.ansvarligEnhet shouldBe behandling.behandlendeEnhet
        tilstand.totalFeilutbetaltBeløp.shouldBeNull()
        tilstand.totalFeilutbetaltPeriode.shouldBeNull()
    }

    @Test
    fun `hentBehandlingensTilstand skal utlede behandlingtilstand for fattet behandling`() {
        val behandlingsresultat = Behandlingsresultat(type = Behandlingsresultatstype.FULL_TILBAKEBETALING)
        val fattetBehandling =
            behandling.copy(
                behandlendeEnhet = "1234",
                behandlendeEnhetsNavn = "foo bar",
                ansvarligSaksbehandler = "Z111111",
                ansvarligBeslutter = "Z111112",
                resultater = setOf(behandlingsresultat),
            )
        behandlingRepository.update(fattetBehandling)
        behandlingsstegstilstandRepository.insert(Testdata.lagBehandlingsstegstilstand(behandling.id).copy(behandlingssteg = Behandlingssteg.FATTE_VEDTAK))
        kravgrunnlagRepository.insert(Testdata.lagKravgrunnlag(behandling.id))

        val tilstand = service.hentBehandlingensTilstand(behandling.id)

        tilstand.ytelsestype shouldBe Ytelsestype.BARNETRYGD
        tilstand.fagsystem shouldBe Fagsystem.BA
        tilstand.saksnummer shouldBe fagsak.eksternFagsakId
        tilstand.behandlingUuid shouldBe behandling.eksternBrukId
        tilstand.referertFagsaksbehandling shouldBe behandling.aktivFagsystemsbehandling.eksternId
        tilstand.behandlingstype shouldBe behandling.type
        tilstand.behandlingsstatus shouldBe behandling.status
        tilstand.behandlingsresultat shouldBe behandlingsresultat.type
        tilstand.venterPåBruker shouldBe false
        tilstand.venterPåØkonomi shouldBe false
        tilstand.behandlingErManueltOpprettet shouldBe false
        tilstand.funksjoneltTidspunkt.shouldBeBetween(OffsetDateTime.now().minusMinutes(1), OffsetDateTime.now().plusSeconds(1))
        tilstand.tekniskTidspunkt shouldBe null
        tilstand.ansvarligBeslutter shouldBe "Z111112"
        tilstand.ansvarligSaksbehandler shouldBe "Z111111"
        tilstand.ansvarligEnhet shouldBe "1234"
        tilstand.totalFeilutbetaltBeløp shouldBe BigDecimal("10000.00")
        tilstand.totalFeilutbetaltPeriode.shouldNotBeNull()
        tilstand.totalFeilutbetaltPeriode!!.should {
            it.fom == YearMonth.now().minusMonths(1).atDay(1) &&
                it.tom == YearMonth.now().atEndOfMonth()
        }
    }

    @Test
    fun `hentBehandlingensTilstand skal utlede behandlingstilstand for behandling på vent`() {
        behandlingsstegstilstandRepository.insert(Testdata.lagBehandlingsstegstilstand(behandling.id))
        kravgrunnlagRepository.insert(Testdata.lagKravgrunnlag(behandling.id))
        behandlingService.settBehandlingPåVent(
            behandling.id,
            BehandlingPåVentDto(
                venteårsak = Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
                tidsfrist = LocalDate.now().plusDays(1),
                begrunnelse = null,
            ),
        )
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)

        val tilstand = service.hentBehandlingensTilstand(behandling.id)

        tilstand.ytelsestype shouldBe Ytelsestype.BARNETRYGD
        tilstand.fagsystem shouldBe Fagsystem.BA
        tilstand.saksnummer shouldBe fagsak.eksternFagsakId
        tilstand.behandlingUuid shouldBe behandling.eksternBrukId
        tilstand.referertFagsaksbehandling shouldBe behandling.aktivFagsystemsbehandling.eksternId
        tilstand.behandlingstype shouldBe behandling.type
        tilstand.behandlingsstatus shouldBe behandling.status
        tilstand.behandlingsresultat shouldBe Behandlingsresultatstype.IKKE_FASTSATT
        tilstand.venterPåBruker shouldBe true
        tilstand.venterPåØkonomi shouldBe false
        tilstand.funksjoneltTidspunkt.shouldBeBetween(
            OffsetDateTime.now().minusMinutes(1),
            OffsetDateTime.now().plusSeconds(1),
        )

        tilstand.totalFeilutbetaltBeløp shouldBe BigDecimal("10000.00")
        tilstand.totalFeilutbetaltPeriode.shouldNotBeNull()
        tilstand.totalFeilutbetaltPeriode!!.should {
            it.fom == YearMonth.now().minusMonths(1).atDay(1) &&
                it.tom == YearMonth.now().atEndOfMonth()
        }
    }

    private fun lagOpprettTilbakekrevingRequest(
        finnesVarsel: Boolean,
        tilbakekrevingsvalg: Tilbakekrevingsvalg,
        fagsak: Fagsak,
    ): OpprettTilbakekrevingRequest {
        val fom = YearMonth.now().minusMonths(1).atDay(1)
        val tom = YearMonth.now().atEndOfMonth()

        val varsel =
            if (finnesVarsel) {
                Varsel(
                    varseltekst = "testverdi",
                    sumFeilutbetaling = BigDecimal.valueOf(1500L),
                    perioder = listOf(Periode(fom, tom)),
                )
            } else {
                null
            }

        val faktainfo =
            Faktainfo(
                revurderingsårsak = "testverdi",
                revurderingsresultat = "testresultat",
                tilbakekrevingsvalg = tilbakekrevingsvalg,
            )

        return OpprettTilbakekrevingRequest(
            ytelsestype = YtelsestypeDTO.BARNETRYGD,
            fagsystem = FagsystemDTO.BA,
            eksternFagsakId = fagsak.eksternFagsakId,
            personIdent = "321321322",
            eksternId = UUID.randomUUID().toString(),
            manueltOpprettet = false,
            språkkode = Språkkode.NN,
            enhetId = "8020",
            enhetsnavn = "Oslo",
            varsel = varsel,
            revurderingsvedtaksdato = fom,
            faktainfo = faktainfo,
            saksbehandlerIdent = "Z0000",
            begrunnelseForTilbakekreving = null,
        )
    }
}
