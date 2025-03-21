package no.nav.familie.tilbake.dokumentbestilling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.task.TracableTaskService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.innhentdokumentasjon.InnhentDokumentasjonbrevService
import no.nav.familie.tilbake.dokumentbestilling.innhentdokumentasjon.InnhentDokumentasjonbrevTask
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.ManuellBrevmottakerRepository
import no.nav.familie.tilbake.dokumentbestilling.varsel.manuelt.ManueltVarselbrevService
import no.nav.familie.tilbake.dokumentbestilling.varsel.manuelt.SendManueltVarselbrevTask
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.Fagområdekode
import no.nav.familie.tilbake.kravgrunnlag.domain.GjelderType
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassekode
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsbeløp433
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsperiode432
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravstatuskode
import no.nav.familie.tilbake.log.LogService
import no.nav.tilbakekreving.kontrakter.brev.Dokumentmalstype
import no.nav.tilbakekreving.kontrakter.periode.Månedsperiode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.util.UUID

class DokumentBehandlingServiceTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var behandlingskontrollService: BehandlingskontrollService

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var taskService: TracableTaskService

    @Autowired
    private lateinit var logService: LogService

    private lateinit var fagsak: Fagsak

    private lateinit var behandling: Behandling

    private val mockManueltVarselBrevService: ManueltVarselbrevService = mockk()
    private val mockInnhentDokumentasjonbrevService: InnhentDokumentasjonbrevService = mockk()
    private val mockManuellBrevmottakerRepository: ManuellBrevmottakerRepository = mockk()

    private lateinit var dokumentBehandlingService: DokumentbehandlingService

    @BeforeEach
    fun init() {
        fagsak = fagsakRepository.insert(Testdata.fagsak())
        behandling = behandlingRepository.insert(Testdata.lagBehandling(fagsakId = fagsak.id))
        behandlingsstegstilstandRepository.insert(Testdata.lagBehandlingsstegstilstand(behandling.id))
        dokumentBehandlingService =
            DokumentbehandlingService(
                behandlingRepository,
                fagsakRepository,
                behandlingskontrollService,
                kravgrunnlagRepository,
                taskService,
                mockManueltVarselBrevService,
                mockInnhentDokumentasjonbrevService,
                mockManuellBrevmottakerRepository,
                logService,
            )

        every { mockManuellBrevmottakerRepository.findByBehandlingId(behandling.id) } returns emptyList()
    }

    @Test
    fun `bestillBrev skal kunne bestille varselbrev når grunnlag finnes`() {
        val behandlingId = opprettOgLagreKravgrunnlagPåBehandling()

        dokumentBehandlingService.bestillBrev(behandlingId, Dokumentmalstype.VARSEL, "Bestilt varselbrev")

        val tasks = taskService.finnTasksMedStatus(listOf(Status.UBEHANDLET), Pageable.unpaged())
        tasks.first().type shouldBe SendManueltVarselbrevTask.TYPE
    }

    @Test
    fun `bestillBrev skal ikke kunne bestille varselbrev når grunnlag ikke finnes`() {
        shouldThrow<IllegalStateException> {
            dokumentBehandlingService.bestillBrev(behandling.id, Dokumentmalstype.VARSEL, "Bestilt varselbrev")
        }.message shouldBe "Kan ikke sende varselbrev fordi grunnlag finnes ikke for behandlingId = ${behandling.id}"
    }

    @Test
    fun `bestillBrev skal kunne bestille innhent dokumentasjon brev når grunnlag finnes`() {
        val behandlingId = opprettOgLagreKravgrunnlagPåBehandling()

        dokumentBehandlingService.bestillBrev(
            behandlingId,
            Dokumentmalstype.INNHENT_DOKUMENTASJON,
            "Bestilt innhent dokumentasjon",
        )

        val tasks = taskService.finnTasksMedStatus(listOf(Status.UBEHANDLET), Pageable.unpaged())
        tasks.first().type shouldBe InnhentDokumentasjonbrevTask.TYPE
    }

    @Test
    fun `bestillBrev skal ikke kunne bestille innhent dokumentasjonbrev når grunnlag ikke finnes`() {
        shouldThrow<java.lang.IllegalStateException> {
            dokumentBehandlingService.bestillBrev(
                behandling.id,
                Dokumentmalstype.INNHENT_DOKUMENTASJON,
                "Bestilt innhent dokumentasjon",
            )
        }.message shouldBe "Kan ikke sende innhent dokumentasjonsbrev fordi grunnlag finnes ikke for behandlingId = " +
            "${behandling.id}"
    }

    private fun opprettOgLagreKravgrunnlagPåBehandling(): UUID {
        val ytelBeløp =
            Kravgrunnlagsbeløp433(
                klassetype = Klassetype.YTEL,
                klassekode = Klassekode.BATR,
                nyttBeløp = BigDecimal.ZERO,
                tilbakekrevesBeløp = BigDecimal.valueOf(1000),
                opprinneligUtbetalingsbeløp = BigDecimal.valueOf(1000),
                skatteprosent = BigDecimal.ZERO,
            )
        val feilBeløp =
            Kravgrunnlagsbeløp433(
                klassetype = Klassetype.FEIL,
                klassekode = Klassekode.BATR,
                nyttBeløp = BigDecimal.valueOf(1000),
                tilbakekrevesBeløp = BigDecimal.ZERO,
                opprinneligUtbetalingsbeløp = BigDecimal.ZERO,
                skatteprosent = BigDecimal.ZERO,
            )
        val periode =
            Kravgrunnlagsperiode432(
                periode = Månedsperiode(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 31)),
                månedligSkattebeløp = BigDecimal.ZERO,
                beløp = setOf(ytelBeløp, feilBeløp),
            )
        val kravgrunnlag431 =
            Kravgrunnlag431(
                behandlingId = behandling.id,
                fagområdekode = Fagområdekode.BA,
                vedtakId = BigInteger.valueOf(12342L),
                eksternKravgrunnlagId = BigInteger.valueOf(1234),
                kravstatuskode = Kravstatuskode.NYTT,
                fagsystemId = "1234",
                utbetalesTilId = "11323432111",
                utbetIdType = GjelderType.PERSON,
                gjelderVedtakId = "11323432111",
                gjelderType = GjelderType.PERSON,
                ansvarligEnhet = "enhet",
                bostedsenhet = "enhet",
                behandlingsenhet = "enhet",
                kontrollfelt = "132323",
                saksbehandlerId = "23454334",
                referanse = "testverdi",
                perioder = setOf(periode),
            )
        kravgrunnlagRepository.insert(kravgrunnlag431)
        return kravgrunnlag431.behandlingId
    }
}
