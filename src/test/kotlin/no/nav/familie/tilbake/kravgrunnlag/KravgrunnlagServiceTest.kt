package no.nav.familie.tilbake.kravgrunnlag

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingService
import no.nav.familie.tilbake.behandling.HentFagsystemsbehandlingService
import no.nav.familie.tilbake.behandling.steg.StegService
import no.nav.familie.tilbake.behandling.task.TracableTaskService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsperiode432
import no.nav.familie.tilbake.kravgrunnlag.event.EndretKravgrunnlagEventPublisher
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.micrometer.TellerService
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class KravgrunnlagServiceTest {
    private val kravgrunnlagRepository: KravgrunnlagRepository = mockk()
    private val behandlingRepository: BehandlingRepository = mockk()
    private val mottattXmlService: ØkonomiXmlMottattService = mockk()
    private val stegService: StegService = mockk()
    private val behandlingskontrollService: BehandlingskontrollService = mockk()
    private val taskService: TracableTaskService = mockk()
    private val tellerService: TellerService = mockk()
    private val oppgaveTaskService: OppgaveTaskService = mockk()
    private val historikkService: HistorikkService = mockk()
    private val hentFagsystemsbehandlingService: HentFagsystemsbehandlingService = mockk()
    private val endretKravgrunnlagEventPublisher: EndretKravgrunnlagEventPublisher = mockk()
    private val behandlingService: BehandlingService = mockk()

    private val kravgrunnlagService =
        KravgrunnlagService(
            kravgrunnlagRepository = kravgrunnlagRepository,
            behandlingRepository = behandlingRepository,
            mottattXmlService = mottattXmlService,
            stegService = stegService,
            behandlingskontrollService = behandlingskontrollService,
            taskService = taskService,
            tellerService = tellerService,
            oppgaveTaskService = oppgaveTaskService,
            historikkService = historikkService,
            hentFagsystemsbehandlingService = hentFagsystemsbehandlingService,
            endretKravgrunnlagEventPublisher = endretKravgrunnlagEventPublisher,
            behandlingService = behandlingService,
        )

    @Test
    fun `Skal finne år for feilutbetaling i kravgrunnlagsperioder `() {
        val fom = LocalDate.now().minusYears(10)
        val tom = LocalDate.now()

        val kravgrunnlagsperioder: Set<Kravgrunnlagsperiode432> =
            setOf(Testdata.lagKravgrunnlagsperiode(fom, tom))

        kravgrunnlagsperioder.finnÅrForNyesteFeilutbetalingsperiode() shouldBe tom.year
    }

    @Test
    fun `Skal finne riktig (nyeste) år med feilutbetaling i kravgrunnlagsperioder `() {
        val now = LocalDate.now()
        val forventetTom = now.minusYears(2)

        val eldstePeriode = Testdata.lagKravgrunnlagsperiode(now.minusYears(5), now.minusYears(5))
        val gammelPeriode = Testdata.lagKravgrunnlagsperiode(now.minusYears(4), now.minusYears(4))
        val nyestePeriode = Testdata.lagKravgrunnlagsperiode(now.minusYears(3), forventetTom)

        val kravgrunnlagsperioder: Set<Kravgrunnlagsperiode432> =
            setOf(eldstePeriode, nyestePeriode, gammelPeriode)

        kravgrunnlagsperioder.finnÅrForNyesteFeilutbetalingsperiode() shouldBe forventetTom.year
    }

    @Test
    fun `Er under fire rettsgebyr og refererer til samme fagsystembehandling `() {
        val fom = LocalDate.of(2022, 1, 1)
        val tom = LocalDate.of(2022, 1, 1)

        val kravgrunnlagsperioder: Set<Kravgrunnlagsperiode432> =
            setOf(Testdata.lagKravgrunnlagsperiode(fom = fom, tom = tom, beløp = 1000))

        val behandling = Testdata.lagBehandling()

        val kravgrunnlag =
            Testdata.lagKravgrunnlag(behandlingId = behandling.id, perioder = kravgrunnlagsperioder).copy(referanse = behandling.fagsystemsbehandling.first().eksternId)

        kravgrunnlagService.kanBehandlesAutomatiskBasertPåRettsgebyrOgFagsystemreferanse(
            kravgrunnlag,
            behandling,
        ) shouldBe true
    }

    @Test
    fun `Feiler fordi beløp er 1 kr over fire rettsgebyr`() {
        val fom = LocalDate.of(2022, 1, 1)
        val tom = LocalDate.of(2022, 1, 1)

        // 1223

        val kravgrunnlagsperioder: Set<Kravgrunnlagsperiode432> =
            setOf(Testdata.lagKravgrunnlagsperiode(fom = fom, tom = tom, beløp = 1223 * 4 + 1))

        val behandling = Testdata.lagBehandling()

        val kravgrunnlag =
            Testdata.lagKravgrunnlag(behandlingId = behandling.id, perioder = kravgrunnlagsperioder).copy(referanse = behandling.fagsystemsbehandling.first().eksternId)

        kravgrunnlagService.kanBehandlesAutomatiskBasertPåRettsgebyrOgFagsystemreferanse(
            kravgrunnlag,
            behandling,
        ) shouldBe false
    }

    @Test
    fun `Henter ut riktig rettsgebyr for 2022`() {
        val rettsgebyr2022 = 1223
        Constants.rettsgebyrForÅr(2022) shouldBe rettsgebyr2022
    }

    @Test
    fun `Skal ikke finne rettsgebyr for år vi ikke har registrert `() {
        val fom = LocalDate.of(2017, 1, 1)
        val tom = LocalDate.of(2017, 1, 1)

        val kravgrunnlagsperioder: Set<Kravgrunnlagsperiode432> =
            setOf(Testdata.lagKravgrunnlagsperiode(fom, tom))

        val kravgrunnlag =
            Testdata.lagKravgrunnlag(behandlingId = UUID.randomUUID(), perioder = kravgrunnlagsperioder)

        kravgrunnlagService.kanBehandlesAutomatiskBasertPåRettsgebyrOgFagsystemreferanse(
            kravgrunnlag,
            Testdata.lagBehandling(),
        ) shouldBe false
    }

    @Test
    fun `Skal ikke oppdatere aktiv på nytt kravgrunnlag med eldre dato i kontrollfelt`() {
        val behandling = Testdata.lagBehandling()
        val gammeltKravgrunnlag = Testdata.lagKravgrunnlag(behandling.id).copy(kontrollfelt = "2024-04-29-18.50.15.236317")
        val nyttKravgrunnlag = Testdata.lagKravgrunnlag(behandling.id).copy(kontrollfelt = "2024-04-29-18.50.15.236316")

        val nyttKravgrunnlagSlot = slot<Kravgrunnlag431>()
        val gammeltKravgrunnlagSlot = slot<Kravgrunnlag431>()

        assertThat(gammeltKravgrunnlag.aktiv).isTrue

        every { kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(any()) } returns true
        every { kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(any()) } returns gammeltKravgrunnlag
        every { kravgrunnlagRepository.insert(capture(nyttKravgrunnlagSlot)) } returns mockk()
        every { kravgrunnlagRepository.update(capture(gammeltKravgrunnlagSlot)) } returns mockk()

        kravgrunnlagService.lagreKravgrunnlag(nyttKravgrunnlag, Ytelsestype.BARNETRYGD, SecureLog.Context.tom())

        assertThat(gammeltKravgrunnlagSlot.captured.aktiv).isTrue
        assertThat(nyttKravgrunnlagSlot.captured.aktiv).isFalse
    }

    @Test
    fun `Skal oppdatere aktiv på nytt kravgrunnlag med nyere dato i kontrollfelt`() {
        val behandling = Testdata.lagBehandling()
        val gammeltKravgrunnlag = Testdata.lagKravgrunnlag(behandling.id).copy(kontrollfelt = "2024-04-29-18.50.15.236316")
        val nyttKravgrunnlag = Testdata.lagKravgrunnlag(behandling.id).copy(kontrollfelt = "2024-04-29-18.50.15.236317")

        val nyttKravgrunnlagSlot = slot<Kravgrunnlag431>()
        val gammeltKravgrunnlagSlot = slot<Kravgrunnlag431>()

        assertThat(gammeltKravgrunnlag.aktiv).isTrue

        every { kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(any()) } returns true
        every { kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(any()) } returns gammeltKravgrunnlag
        every { kravgrunnlagRepository.update(capture(gammeltKravgrunnlagSlot)) } returns mockk()
        every { kravgrunnlagRepository.insert(capture(nyttKravgrunnlagSlot)) } returns mockk()

        kravgrunnlagService.lagreKravgrunnlag(nyttKravgrunnlag, Ytelsestype.OVERGANGSSTØNAD, SecureLog.Context.tom())

        assertThat(gammeltKravgrunnlagSlot.captured.aktiv).isFalse
        assertThat(nyttKravgrunnlagSlot.captured.aktiv).isTrue
    }

    @Test
    fun `Skal lagre nytt kravgrunnlag dersom det ikke finnes et kravgrunnlag fra før`() {
        val behandling = Testdata.lagBehandling()
        val nyttKravgrunnlag = Testdata.lagKravgrunnlag(behandling.id).copy(kontrollfelt = "2024-04-29-18.50.15.236317")

        val nyttKravgrunnlagSlot2 = slot<Kravgrunnlag431>()

        every { kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(any()) } returns false
        every { kravgrunnlagRepository.insert(capture(nyttKravgrunnlagSlot2)) } returns mockk()

        kravgrunnlagService.lagreKravgrunnlag(nyttKravgrunnlag, Ytelsestype.OVERGANGSSTØNAD, SecureLog.Context.tom())

        assertThat(nyttKravgrunnlagSlot2.captured.aktiv).isTrue
    }
}
