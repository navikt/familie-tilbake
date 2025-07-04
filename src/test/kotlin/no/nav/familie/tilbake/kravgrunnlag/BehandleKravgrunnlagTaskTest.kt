package no.nav.familie.tilbake.kravgrunnlag

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forExactly
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.Ytelsestype
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Fagsystemsbehandling
import no.nav.familie.tilbake.behandling.steg.StegService
import no.nav.familie.tilbake.behandling.task.OppdaterFaktainfoTask
import no.nav.familie.tilbake.behandling.task.TracableTaskService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.vedtak.VedtaksbrevsoppsummeringRepository
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingRepository
import no.nav.familie.tilbake.foreldelse.VurdertForeldelseRepository
import no.nav.familie.tilbake.historikkinnslag.Aktør
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravstatuskode
import no.nav.familie.tilbake.kravgrunnlag.domain.ØkonomiXmlMottatt
import no.nav.familie.tilbake.kravgrunnlag.task.BehandleKravgrunnlagTask
import no.nav.familie.tilbake.lagDatoIkkeForeldet
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.oppgave.OppdaterOppgaveTask
import no.nav.familie.tilbake.oppgave.OppdaterPrioritetTask
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingRepository
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegFaktaDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegForeldelseDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegForeslåVedtaksstegDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegVilkårsvurderingDto
import no.nav.tilbakekreving.api.v1.dto.FaktaFeilutbetalingsperiodeDto
import no.nav.tilbakekreving.api.v1.dto.ForeldelsesperiodeDto
import no.nav.tilbakekreving.api.v1.dto.FritekstavsnittDto
import no.nav.tilbakekreving.api.v1.dto.GodTroDto
import no.nav.tilbakekreving.api.v1.dto.VilkårsvurderingsperiodeDto
import no.nav.tilbakekreving.kontrakter.Tilbakekrevingsvalg
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsesundertype
import no.nav.tilbakekreving.kontrakter.foreldelse.Foreldelsesvurderingstype
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class BehandleKravgrunnlagTaskTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var taskService: TracableTaskService

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var mottattXmlRepository: ØkonomiXmlMottattRepository

    @Autowired
    private lateinit var mottattXmlArkivRepository: ØkonomiXmlMottattArkivRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var faktaFeilutbetalingRepository: FaktaFeilutbetalingRepository

    @Autowired
    private lateinit var foreldelseRepository: VurdertForeldelseRepository

    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @Autowired
    private lateinit var vedtaksbrevsoppsummeringRepository: VedtaksbrevsoppsummeringRepository

    @Autowired
    private lateinit var behandlingskontrollService: BehandlingskontrollService

    @Autowired
    private lateinit var stegService: StegService

    @Autowired
    private lateinit var behandleKravgrunnlagTask: BehandleKravgrunnlagTask

    @Autowired
    private lateinit var historikkService: HistorikkService

    private lateinit var fagsak: Fagsak
    private lateinit var behandling: Behandling

    @BeforeEach
    fun init() {
        fagsak = Testdata.fagsak
        behandling = Testdata.lagBehandling()
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(behandling)
    }

    @Test
    fun `doTask skal lagre mottatt kravgrunnlag i Kravgrunnlag431 når behandling finnes`() {
        lagGrunnlagssteg()
        val kravgrunnlagXml = readKravgrunnlagXmlMedIkkeForeldetDato("/kravgrunnlagxml/kravgrunnlag_BA_riktig_eksternfagsakId_ytelsestype.xml")
        val task = opprettTask(kravgrunnlagXml)

        behandleKravgrunnlagTask.doTask(task)

        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
        kravgrunnlag.shouldNotBeNull()
        kravgrunnlag.kravstatuskode shouldBe Kravstatuskode.NYTT
        kravgrunnlag.fagsystemId shouldBe fagsak.eksternFagsakId
        KravgrunnlagUtil.tilYtelsestype(kravgrunnlag.fagområdekode.name) shouldBe Ytelsestype.BARNETRYGD

        assertPerioder(kravgrunnlag)
        assertBeløp(kravgrunnlag)

        mottattXmlRepository
            .findByEksternKravgrunnlagIdAndVedtakId(
                kravgrunnlag.eksternKravgrunnlagId,
                kravgrunnlag.vedtakId,
            ).shouldBeEmpty()
        mottattXmlArkivRepository.findAll().toList().shouldNotBeEmpty()

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)

        assertHistorikkinnslag(TilbakekrevingHistorikkinnslagstype.KRAVGRUNNLAG_MOTTATT, Aktør.Vedtaksløsning)
        assertOppgaveTask(
            "Behandling er tatt av vent, " +
                "men revurderingsvedtaksdato er mindre enn 10 dager fra dagens dato. " +
                "Fristen settes derfor 10 dager fra revurderingsvedtaksdato " +
                "for å sikre at behandlingen har mottatt oppdatert kravgrunnlag",
            behandling.aktivFagsystemsbehandling.revurderingsvedtaksdato.plusDays(10),
        )
        assertIkkeOpprettOppdaterPrioritetTask()
    }

    @Test
    fun `doTask skal lagre mottatt kravgrunnlag i Kravgrunnlag431 når behandling finnes med revurdering gamle enn 10 dager`() {
        val fagsystemsbehandling =
            Fagsystemsbehandling(
                eksternId = UUID.randomUUID().toString(),
                tilbakekrevingsvalg =
                    Tilbakekrevingsvalg
                        .OPPRETT_TILBAKEKREVING_UTEN_VARSEL,
                revurderingsvedtaksdato = LocalDate.now().minusDays(10),
                resultat = "OPPHØR",
                årsak = "testverdi",
            )
        behandlingRepository.update(
            behandlingRepository
                .findByIdOrThrow(behandling.id)
                .copy(fagsystemsbehandling = setOf(fagsystemsbehandling)),
        )
        lagGrunnlagssteg()
        val kravgrunnlagXml = readKravgrunnlagXmlMedIkkeForeldetDato("/kravgrunnlagxml/kravgrunnlag_BA_riktig_eksternfagsakId_ytelsestype.xml")
        val task = opprettTask(kravgrunnlagXml)

        behandleKravgrunnlagTask.doTask(task)

        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
        kravgrunnlag.shouldNotBeNull()
        kravgrunnlag.kravstatuskode shouldBe Kravstatuskode.NYTT
        kravgrunnlag.fagsystemId shouldBe fagsak.eksternFagsakId
        KravgrunnlagUtil.tilYtelsestype(kravgrunnlag.fagområdekode.name) shouldBe Ytelsestype.BARNETRYGD

        assertPerioder(kravgrunnlag)
        assertBeløp(kravgrunnlag)

        mottattXmlRepository
            .findByEksternKravgrunnlagIdAndVedtakId(
                kravgrunnlag.eksternKravgrunnlagId,
                kravgrunnlag.vedtakId,
            ).shouldBeEmpty()
        mottattXmlArkivRepository.findAll().toList().shouldNotBeEmpty()

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)

        assertHistorikkinnslag(TilbakekrevingHistorikkinnslagstype.KRAVGRUNNLAG_MOTTATT, Aktør.Vedtaksløsning)
        assertOppgaveTask(
            "Behandling er tatt av vent, pga mottatt kravgrunnlag",
            LocalDate.now().plusDays(1),
        )
        assertIkkeOpprettOppdaterPrioritetTask()
    }

    @Test
    fun `doTask skal lagre mottatt kravgrunnlag i Kravgrunnlag431 mens behandling venter på brukerstilbakemelding`() {
        behandlingsstegstilstandRepository.insert(
            Behandlingsstegstilstand(
                behandlingId = behandling.id,
                behandlingssteg = Behandlingssteg.VARSEL,
                behandlingsstegsstatus = Behandlingsstegstatus.VENTER,
                venteårsak = Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
                tidsfrist = LocalDate.now().plusWeeks(3),
            ),
        )

        val kravgrunnlagXml = readKravgrunnlagXmlMedIkkeForeldetDato("/kravgrunnlagxml/kravgrunnlag_BA_riktig_eksternfagsakId_ytelsestype.xml")
        val task = opprettTask(kravgrunnlagXml)

        behandleKravgrunnlagTask.doTask(task)

        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
        kravgrunnlag.shouldNotBeNull()
        kravgrunnlag.kravstatuskode shouldBe Kravstatuskode.NYTT
        kravgrunnlag.fagsystemId shouldBe fagsak.eksternFagsakId
        KravgrunnlagUtil.tilYtelsestype(kravgrunnlag.fagområdekode.name) shouldBe Ytelsestype.BARNETRYGD

        assertPerioder(kravgrunnlag)
        assertBeløp(kravgrunnlag)

        mottattXmlRepository
            .findByEksternKravgrunnlagIdAndVedtakId(
                kravgrunnlag.eksternKravgrunnlagId,
                kravgrunnlag.vedtakId,
            ).shouldBeEmpty()
        mottattXmlArkivRepository.findAll().toList().shouldNotBeEmpty()

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        // behandling venter fortsatt på brukerstilbakemelding, oppretter ikke grunnlagssteg
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.VARSEL, Behandlingsstegstatus.VENTER)
        behandlingsstegstilstand.any { it.behandlingssteg == Behandlingssteg.GRUNNLAG }.shouldBeFalse()

        assertHistorikkinnslag(TilbakekrevingHistorikkinnslagstype.KRAVGRUNNLAG_MOTTATT, Aktør.Vedtaksløsning)
        taskService.findAll().none { it.type == OppdaterOppgaveTask.TYPE }.shouldBeTrue()
        assertOpprettOppdaterPrioritetTask()
    }

    @Test
    fun `doTask skal lagre mottatt kravgrunnlag i Kravgrunnlag431 med en YTEL postering som er større enn differansen`() {
        behandlingsstegstilstandRepository.insert(
            Behandlingsstegstilstand(
                behandlingId = behandling.id,
                behandlingssteg = Behandlingssteg.VARSEL,
                behandlingsstegsstatus = Behandlingsstegstatus.VENTER,
                venteårsak = Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
                tidsfrist = LocalDate.now().plusWeeks(3),
            ),
        )

        val kravgrunnlagXml = readKravgrunnlagXmlMedIkkeForeldetDato("/kravgrunnlagxml/kravgrunnlag_BA_belop_storre_enn_diff.xml")
        val task = opprettTask(kravgrunnlagXml)

        behandleKravgrunnlagTask.doTask(task)

        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
        kravgrunnlag.shouldNotBeNull()
        kravgrunnlag.kravstatuskode shouldBe Kravstatuskode.NYTT
        kravgrunnlag.fagsystemId shouldBe fagsak.eksternFagsakId
        KravgrunnlagUtil.tilYtelsestype(kravgrunnlag.fagområdekode.name) shouldBe Ytelsestype.BARNETRYGD

        assertPerioder(kravgrunnlag)

        val kravgrunnlagsbeløp = kravgrunnlag.perioder.toList()[0].beløp
        kravgrunnlagsbeløp.size shouldBe 3
        kravgrunnlagsbeløp.any { Klassetype.YTEL == it.klassetype }.shouldBeTrue()
        kravgrunnlagsbeløp.any { Klassetype.FEIL == it.klassetype }.shouldBeTrue()

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        // behandling venter fortsatt på brukerstilbakemelding, oppretter ikke grunnlagssteg
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.VARSEL, Behandlingsstegstatus.VENTER)
        behandlingsstegstilstand.any { it.behandlingssteg == Behandlingssteg.GRUNNLAG }.shouldBeFalse()

        assertHistorikkinnslag(TilbakekrevingHistorikkinnslagstype.KRAVGRUNNLAG_MOTTATT, Aktør.Vedtaksløsning)
        taskService.findAll().none { it.type == OppdaterOppgaveTask.TYPE }.shouldBeTrue()
        assertOpprettOppdaterPrioritetTask()
    }

    @Test
    fun `doTask skal lagre mottatt ENDR kravgrunnlag i Kravgrunnlag431 når behandling finnes`() {
        lagGrunnlagssteg()
        val kravgrunnlagXml = readKravgrunnlagXmlMedIkkeForeldetDato("/kravgrunnlagxml/kravgrunnlag_BA_riktig_eksternfagsakId_ytelsestype.xml")
        behandleKravgrunnlagTask.doTask(opprettTask(kravgrunnlagXml))

        behandlingskontrollService
            .tilbakehoppBehandlingssteg(
                behandlingId = behandling.id,
                behandlingsstegsinfo =
                    Behandlingsstegsinfo(
                        Behandlingssteg.GRUNNLAG,
                        Behandlingsstegstatus.VENTER,
                        Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG,
                        LocalDate.now().plusWeeks(4),
                    ),
                logContext = SecureLog.Context.tom(),
            )
        val endretKravgrunnlagXml = readKravgrunnlagXmlMedIkkeForeldetDato("/kravgrunnlagxml/kravgrunnlag_BA_ENDR.xml")

        behandleKravgrunnlagTask.doTask(opprettTask(endretKravgrunnlagXml))

        val alleKravgrunnlag = kravgrunnlagRepository.findByBehandlingId(behandling.id)
        alleKravgrunnlag.size shouldBe 2
        alleKravgrunnlag.any { Kravstatuskode.NYTT == it.kravstatuskode && !it.aktiv }.shouldBeTrue()

        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
        kravgrunnlag.shouldNotBeNull()
        kravgrunnlag.kravstatuskode shouldBe Kravstatuskode.ENDRET
        kravgrunnlag.fagsystemId shouldBe fagsak.eksternFagsakId
        KravgrunnlagUtil.tilYtelsestype(kravgrunnlag.fagområdekode.name) shouldBe Ytelsestype.BARNETRYGD

        assertPerioder(kravgrunnlag)
        assertBeløp(kravgrunnlag)

        mottattXmlRepository
            .findByEksternKravgrunnlagIdAndVedtakId(
                kravgrunnlag.eksternKravgrunnlagId,
                kravgrunnlag.vedtakId,
            ).shouldBeEmpty()
        mottattXmlArkivRepository.findAll().toList().shouldNotBeEmpty()
        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)

        assertHistorikkinnslag(TilbakekrevingHistorikkinnslagstype.KRAVGRUNNLAG_MOTTATT, Aktør.Vedtaksløsning, times = 2)
        assertOppdaterFaktainfoTask(kravgrunnlag.referanse)
        assertOppgaveTask(
            "Behandling er tatt av vent, " +
                "men revurderingsvedtaksdato er mindre enn 10 dager fra dagens dato. " +
                "Fristen settes derfor 10 dager fra revurderingsvedtaksdato " +
                "for å sikre at behandlingen har mottatt oppdatert kravgrunnlag",
            behandling.aktivFagsystemsbehandling.revurderingsvedtaksdato.plusDays(10),
        )
        assertIkkeOpprettOppdaterPrioritetTask()
    }

    @Test
    fun `doTask skal lagre mottatt ENDR kravgrunnlag og slette behandlet data når behandling er på vilkårsvurdering steg`() {
        lagGrunnlagssteg()
        val kravgrunnlagXml = readKravgrunnlagXmlMedIkkeForeldetDato("/kravgrunnlagxml/kravgrunnlag_BA_riktig_eksternfagsakId_ytelsestype.xml")
        behandleKravgrunnlagTask.doTask(opprettTask(kravgrunnlagXml))
        // Håndter fakta steg
        stegService.håndterSteg(
            behandling.id,
            BehandlingsstegFaktaDto(
                listOf(
                    FaktaFeilutbetalingsperiodeDto(
                        Datoperiode(
                            YearMonth.from(LocalDate.of(2020, 8, 1).lagDatoIkkeForeldet()),
                            YearMonth.from(LocalDate.of(2020, 8, 1).lagDatoIkkeForeldet()),
                        ),
                        Hendelsestype.ANNET,
                        Hendelsesundertype.ANNET_FRITEKST,
                    ),
                ),
                "Fakta begrunnelse",
            ),
            SecureLog.Context.tom(),
        )
        // Håndter foreldelse steg
        stegService.håndterSteg(
            behandling.id,
            BehandlingsstegForeldelseDto(
                listOf(
                    ForeldelsesperiodeDto(
                        Datoperiode(
                            YearMonth.from(LocalDate.of(2020, 8, 1).lagDatoIkkeForeldet()),
                            YearMonth.from(LocalDate.of(2020, 8, 1).lagDatoIkkeForeldet()),
                        ),
                        "Foreldelse begrunnelse",
                        Foreldelsesvurderingstype
                            .IKKE_FORELDET,
                    ),
                ),
            ),
            SecureLog.Context.tom(),
        )

        faktaFeilutbetalingRepository.findByBehandlingIdAndAktivIsTrue(behandling.id).shouldNotBeNull()
        foreldelseRepository.findByBehandlingIdAndAktivIsTrue(behandling.id).shouldNotBeNull()

        var behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        assertBehandlingsstegstilstand(
            behandlingsstegstilstand,
            Behandlingssteg.FORELDELSE,
            Behandlingsstegstatus.UTFØRT,
        )
        assertBehandlingsstegstilstand(
            behandlingsstegstilstand,
            Behandlingssteg.VILKÅRSVURDERING,
            Behandlingsstegstatus.KLAR,
        )

        val endretKravgrunnlagXml = readKravgrunnlagXmlMedIkkeForeldetDato("/kravgrunnlagxml/kravgrunnlag_BA_ENDR.xml")
        behandleKravgrunnlagTask.doTask(opprettTask(endretKravgrunnlagXml))

        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
        kravgrunnlag.shouldNotBeNull()
        kravgrunnlag.kravstatuskode shouldBe Kravstatuskode.ENDRET
        kravgrunnlag.fagsystemId shouldBe fagsak.eksternFagsakId
        KravgrunnlagUtil.tilYtelsestype(kravgrunnlag.fagområdekode.name) shouldBe Ytelsestype.BARNETRYGD

        assertPerioder(kravgrunnlag)
        assertBeløp(kravgrunnlag)

        mottattXmlRepository
            .findByEksternKravgrunnlagIdAndVedtakId(
                kravgrunnlag.eksternKravgrunnlagId,
                kravgrunnlag.vedtakId,
            ).shouldBeEmpty()
        mottattXmlArkivRepository.findAll().toList().shouldNotBeEmpty()

        behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)
        assertBehandlingsstegstilstand(
            behandlingsstegstilstand,
            Behandlingssteg.FORELDELSE,
            Behandlingsstegstatus.TILBAKEFØRT,
        )
        assertBehandlingsstegstilstand(
            behandlingsstegstilstand,
            Behandlingssteg.VILKÅRSVURDERING,
            Behandlingsstegstatus.TILBAKEFØRT,
        )

        faktaFeilutbetalingRepository.findByBehandlingIdAndAktivIsTrue(behandling.id).shouldBeNull()
        foreldelseRepository.findByBehandlingIdAndAktivIsTrue(behandling.id).shouldBeEmpty()

        assertHistorikkinnslag(TilbakekrevingHistorikkinnslagstype.KRAVGRUNNLAG_MOTTATT, Aktør.Vedtaksløsning, times = 2)
        assertHistorikkinnslag(TilbakekrevingHistorikkinnslagstype.FAKTA_VURDERT, Aktør.Saksbehandler(behandling.ansvarligSaksbehandler))
        assertHistorikkinnslag(TilbakekrevingHistorikkinnslagstype.FORELDELSE_VURDERT, Aktør.Saksbehandler(behandling.ansvarligSaksbehandler))

        assertOppdaterFaktainfoTask(kravgrunnlag.referanse)
        assertOpprettOppdaterPrioritetTask()
    }

    @Test
    fun `doTask skal lagre mottatt ENDR kravgrunnlag og slette behandlet data når behandling er på fatte vedtak steg`() {
        lagGrunnlagssteg()
        val kravgrunnlagXml = readKravgrunnlagXmlMedIkkeForeldetDato("/kravgrunnlagxml/kravgrunnlag_BA_riktig_eksternfagsakId_ytelsestype.xml")
        behandleKravgrunnlagTask.doTask(opprettTask(kravgrunnlagXml))

        val periode = Datoperiode(YearMonth.from(LocalDate.of(2020, 8, 1).lagDatoIkkeForeldet()), YearMonth.from(LocalDate.of(2020, 8, 1).lagDatoIkkeForeldet()))
        // Håndter fakta steg
        stegService.håndterSteg(
            behandling.id,
            BehandlingsstegFaktaDto(
                listOf(
                    FaktaFeilutbetalingsperiodeDto(
                        periode,
                        Hendelsestype.BARNS_ALDER,
                        Hendelsesundertype.BARN_DØD,
                    ),
                ),
                "Fakta begrunnelse",
            ),
            SecureLog.Context.tom(),
        )
        // Håndter foreldelse steg
        stegService.håndterSteg(
            behandling.id,
            BehandlingsstegForeldelseDto(
                listOf(
                    ForeldelsesperiodeDto(
                        periode,
                        "Foreldelse begrunnelse",
                        Foreldelsesvurderingstype
                            .IKKE_FORELDET,
                    ),
                ),
            ),
            SecureLog.Context.tom(),
        )

        // Håndter Vilkårsvurdering steg
        val periodeMedGodTro =
            VilkårsvurderingsperiodeDto(
                periode,
                Vilkårsvurderingsresultat.GOD_TRO,
                "Vilkårs begrunnelse",
                GodTroDto(begrunnelse = "god tro", beløpErIBehold = false),
            )
        stegService.håndterSteg(
            behandling.id,
            BehandlingsstegVilkårsvurderingDto(listOf(periodeMedGodTro)),
            SecureLog.Context.tom(),
        )

        // Håndter Foreslå Vedtak steg
        stegService.håndterSteg(
            behandling.id,
            BehandlingsstegForeslåVedtaksstegDto(
                FritekstavsnittDto(
                    "oppsummeringstekst",
                    emptyList(),
                ),
            ),
            SecureLog.Context.tom(),
        )

        faktaFeilutbetalingRepository.findByBehandlingIdAndAktivIsTrue(behandling.id).shouldNotBeNull()
        foreldelseRepository.findByBehandlingIdAndAktivIsTrue(behandling.id).shouldNotBeNull()
        vilkårsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandling.id).shouldNotBeNull()
        vedtaksbrevsoppsummeringRepository.findByBehandlingId(behandling.id).shouldNotBeNull()

        var behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.FORELDELSE, Behandlingsstegstatus.UTFØRT)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.UTFØRT)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.FATTE_VEDTAK, Behandlingsstegstatus.KLAR)

        val endretKravgrunnlagXml = readKravgrunnlagXmlMedIkkeForeldetDato("/kravgrunnlagxml/kravgrunnlag_BA_ENDR.xml")
        behandleKravgrunnlagTask.doTask(opprettTask(endretKravgrunnlagXml))

        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
        kravgrunnlag.shouldNotBeNull()
        kravgrunnlag.kravstatuskode shouldBe Kravstatuskode.ENDRET
        kravgrunnlag.fagsystemId shouldBe fagsak.eksternFagsakId
        KravgrunnlagUtil.tilYtelsestype(kravgrunnlag.fagområdekode.name) shouldBe Ytelsestype.BARNETRYGD

        assertPerioder(kravgrunnlag)
        assertBeløp(kravgrunnlag)

        mottattXmlRepository
            .findByEksternKravgrunnlagIdAndVedtakId(
                kravgrunnlag.eksternKravgrunnlagId,
                kravgrunnlag.vedtakId,
            ).shouldBeEmpty()
        mottattXmlArkivRepository.findAll().toList().shouldNotBeEmpty()

        behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.FORELDELSE, Behandlingsstegstatus.TILBAKEFØRT)
        assertBehandlingsstegstilstand(
            behandlingsstegstilstand,
            Behandlingssteg.VILKÅRSVURDERING,
            Behandlingsstegstatus.TILBAKEFØRT,
        )
        assertBehandlingsstegstilstand(
            behandlingsstegstilstand,
            Behandlingssteg.FORESLÅ_VEDTAK,
            Behandlingsstegstatus.TILBAKEFØRT,
        )
        assertBehandlingsstegstilstand(
            behandlingsstegstilstand,
            Behandlingssteg.FATTE_VEDTAK,
            Behandlingsstegstatus.TILBAKEFØRT,
        )

        faktaFeilutbetalingRepository.findByBehandlingIdAndAktivIsTrue(behandling.id).shouldBeNull()
        foreldelseRepository.findByBehandlingIdAndAktivIsTrue(behandling.id).shouldBeEmpty()
        vilkårsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandling.id).shouldBeEmpty()
        vedtaksbrevsoppsummeringRepository.findByBehandlingId(behandling.id).shouldBeNull()

        assertHistorikkinnslag(TilbakekrevingHistorikkinnslagstype.KRAVGRUNNLAG_MOTTATT, Aktør.Vedtaksløsning, times = 2)
        assertHistorikkinnslag(TilbakekrevingHistorikkinnslagstype.FAKTA_VURDERT, Aktør.Saksbehandler(behandling.ansvarligSaksbehandler))
        assertHistorikkinnslag(TilbakekrevingHistorikkinnslagstype.FORELDELSE_VURDERT, Aktør.Saksbehandler(behandling.ansvarligSaksbehandler))

        assertOppdaterFaktainfoTask(kravgrunnlag.referanse)
        assertOpprettOppdaterPrioritetTask()
    }

    @Test
    fun `doTask skal ikke lagre mottatt kravgrunnlag når mottatt xml er ugyldig`() {
        val kravgrunnlagXml = readXml("/kravgrunnlagxml/kravgrunnlag_ugyldig_struktur.xml")

        val exception = shouldThrow<RuntimeException> { behandleKravgrunnlagTask.doTask(opprettTask(kravgrunnlagXml)) }
        exception.message shouldBe "Mottatt kravgrunnlagXML er ugyldig! Den feiler med jakarta.xml.bind.UnmarshalException\n" +
            " - with linked exception:\n" +
            "[org.xml.sax.SAXParseException; lineNumber: 21; columnNumber: 33; " +
            "cvc-complex-type.2.4.b: The content of element 'urn:detaljertKravgrunnlag' " +
            "is not complete. One of " +
            "'{\"urn:no:nav:tilbakekreving:kravgrunnlag:detalj:v1\":tilbakekrevingsPeriode}'" +
            " is expected.]"
    }

    @Test
    fun `doTask skal ikke lagre mottatt kravgrunnlag når mottatt xml ikke har referanse`() {
        val kravgrunnlagXml = readKravgrunnlagXmlMedIkkeForeldetDato("/kravgrunnlagxml/kravgrunnlag_tomt_referanse.xml")

        val exception = shouldThrow<RuntimeException> { behandleKravgrunnlagTask.doTask(opprettTask(kravgrunnlagXml)) }
        exception.message shouldBe "Ugyldig kravgrunnlag for kravgrunnlagId 0. Mangler referanse."
    }

    @Test
    fun `doTask skal ikke lagre mottatt kravgrunnlag når mottatt xml periode ikke er innenfor måned`() {
        val kravgrunnlagXml = readKravgrunnlagXmlMedIkkeForeldetDato("/kravgrunnlagxml/kravgrunnlag_periode_utenfor_kalendermåned.xml")

        val exception = shouldThrow<RuntimeException> { behandleKravgrunnlagTask.doTask(opprettTask(kravgrunnlagXml)) }
        exception.message shouldBe "Ugyldig kravgrunnlag for kravgrunnlagId 0. " +
            "Perioden ${LocalDate.of(2023, 8,1).lagDatoIkkeForeldet()} til ${LocalDate.of(2023, 9,30).lagDatoIkkeForeldet()} er ikke innenfor samme kalendermåned."
    }

    @Test
    fun `doTask skal ikke lagre mottatt kravgrunnlag når mottatt xml periode ikke starter første dag i måned`() {
        val kravgrunnlagXml = readKravgrunnlagXmlMedIkkeForeldetDato("/kravgrunnlagxml/kravgrunnlag_periode_starter_ikke_første_dag.xml")

        val exception = shouldThrow<RuntimeException> { behandleKravgrunnlagTask.doTask(opprettTask(kravgrunnlagXml)) }
        exception.message shouldBe "Ugyldig kravgrunnlag for kravgrunnlagId 0. " +
            "Perioden ${LocalDate.of(2020, 8,15).lagDatoIkkeForeldet()} til ${LocalDate.of(2020, 8,31).lagDatoIkkeForeldet()} starter ikke første dag i måned."
    }

    @Test
    fun `doTask skal ikke lagre mottatt kravgrunnlag når mottatt xml periode ikke slutter siste dag i måned`() {
        val kravgrunnlagXml = readKravgrunnlagXmlMedIkkeForeldetDato("/kravgrunnlagxml/kravgrunnlag_periode_slutter_ikke_siste_dag.xml")

        val exception = shouldThrow<RuntimeException> { behandleKravgrunnlagTask.doTask(opprettTask(kravgrunnlagXml)) }
        exception.message shouldBe "Ugyldig kravgrunnlag for kravgrunnlagId 0. " +
            "Perioden ${LocalDate.of(2020, 8,1).lagDatoIkkeForeldet()} til ${LocalDate.of(2020, 8,28).lagDatoIkkeForeldet()} slutter ikke siste dag i måned."
    }

    @Test
    fun `doTask skal ikke lagre mottatt kravgrunnlag når mottatt xml mangler FEIL postering`() {
        val kravgrunnlagXml = readKravgrunnlagXmlMedIkkeForeldetDato("/kravgrunnlagxml/kravgrunnlag_uten_FEIL_postering.xml")

        val exception = shouldThrow<RuntimeException> { behandleKravgrunnlagTask.doTask(opprettTask(kravgrunnlagXml)) }
        exception.message shouldBe "Ugyldig kravgrunnlag for kravgrunnlagId 0. " +
            "Perioden ${LocalDate.of(2020, 8,1).lagDatoIkkeForeldet()} til ${LocalDate.of(2020, 8,31).lagDatoIkkeForeldet()} mangler postering med klassetype=FEIL."
    }

    @Test
    fun `doTask skal ikke lagre mottatt kravgrunnlag når mottatt xml mangler YTEL postering`() {
        val kravgrunnlagXml = readKravgrunnlagXmlMedIkkeForeldetDato("/kravgrunnlagxml/kravgrunnlag_uten_YTEL_postering.xml")

        val exception = shouldThrow<RuntimeException> { behandleKravgrunnlagTask.doTask(opprettTask(kravgrunnlagXml)) }
        exception.message shouldBe "Ugyldig kravgrunnlag for kravgrunnlagId 0. " +
            "Perioden ${LocalDate.of(2020, 8,1).lagDatoIkkeForeldet()} til ${LocalDate.of(2020, 8,31).lagDatoIkkeForeldet()} mangler postering med klassetype=YTEL."
    }

    @Test
    fun `doTask skal ikke lagre mottatt kravgrunnlag når mottatt xml har overlappende perioder`() {
        val kravgrunnlagXml = readKravgrunnlagXmlMedIkkeForeldetDato("/kravgrunnlagxml/kravgrunnlag_overlappende_perioder.xml")

        val exception = shouldThrow<RuntimeException> { behandleKravgrunnlagTask.doTask(opprettTask(kravgrunnlagXml)) }
        exception.message shouldBe "Ugyldig kravgrunnlag for kravgrunnlagId 0. " +
            "Perioden ${LocalDate.of(2020,8,1).lagDatoIkkeForeldet().year}-08-01 til ${LocalDate.now().lagDatoIkkeForeldet().year}-08-31 overlapper med perioden ${LocalDate.now().lagDatoIkkeForeldet().year}-08-01 til ${LocalDate.of(2020,8,31).lagDatoIkkeForeldet().year}-08-31."
    }

    @Test
    fun `doTask skal ikke lagre mottatt kravgrunnlag når xml har posteringsskatt som ikke matcher månedlig skatt beløp`() {
        val kravgrunnlagXml = readKravgrunnlagXmlMedIkkeForeldetDato("/kravgrunnlagxml/kravgrunnlag_posteringsskatt_matcher_ikke_med_månedlig_skatt_beløp.xml")

        val exception = shouldThrow<RuntimeException> { behandleKravgrunnlagTask.doTask(opprettTask(kravgrunnlagXml)) }
        exception.message shouldBe "Ugyldig kravgrunnlag for kravgrunnlagId 0. " +
            "Maks skatt for perioden ${LocalDate.of(2020,8,1).lagDatoIkkeForeldet().year}-08-01 til ${LocalDate.of(2020,8,1).lagDatoIkkeForeldet().year}-08-31 er 0.00, men maks tilbakekreving ganget med skattesats blir 210."
    }

    @Test
    fun `doTask skal ikke lagre mottatt kravgrunnlag når mottatt xml har FEIL postering med negativt beløp`() {
        val kravgrunnlagXml = readKravgrunnlagXmlMedIkkeForeldetDato("/kravgrunnlagxml/kravgrunnlag_FEIL_postering_med_negativ_beløp.xml")

        val exception = shouldThrow<RuntimeException> { behandleKravgrunnlagTask.doTask(opprettTask(kravgrunnlagXml)) }
        exception.message shouldBe "Ugyldig kravgrunnlag for kravgrunnlagId 0. " +
            "Perioden ${LocalDate.of(2020, 8,1).lagDatoIkkeForeldet()} til ${LocalDate.of(2020, 8,31).lagDatoIkkeForeldet()} har feilpostering med negativt beløp."
    }

    @Test
    fun `doTask skal ikke lagre mottatt kravgrunnlag når mottatt xml har ulike total tilbakekrevesbeløp og total nybeløp`() {
        val kravgrunnlagXml = readKravgrunnlagXmlMedIkkeForeldetDato("/kravgrunnlagxml/kravgrunnlag_ulike_total_tilbakekrevesbeløp_total_nybeløp.xml")

        val exception = shouldThrow<RuntimeException> { behandleKravgrunnlagTask.doTask(opprettTask(kravgrunnlagXml)) }
        exception.message shouldBe "Ugyldig kravgrunnlag for kravgrunnlagId 0. " +
            "Perioden ${LocalDate.of(2020, 8, 1).lagDatoIkkeForeldet()} til ${LocalDate.of(2020, 8, 31).lagDatoIkkeForeldet()} " +
            "har ulikt summert tilbakekrevingsbeløp i YTEL postering(1500.00) " +
            "i forhold til summert beløpNy i FEIL postering(2108.00)."
    }

    @Test
    fun `doTask skal ikke lagre mottatt kravgrunnlag når mottatt xml har YTEL postering som ikke matcher beregning`() {
        val kravgrunnlagXml = readXml("/kravgrunnlagxml/kravgrunnlag_YTEL_postering_som_ikke_matcher_beregning.xml")

        val exception = shouldThrow<RuntimeException> { behandleKravgrunnlagTask.doTask(opprettTask(kravgrunnlagXml)) }
        exception.message shouldBe "Ugyldig kravgrunnlag for kravgrunnlagId 0. " +
            "Har en eller flere perioder med YTEL-postering med tilbakekrevesBeløp " +
            "som er større enn differanse mellom nyttBeløp og opprinneligBeløp."
    }

    @Test
    fun `doTask skal lagre mottatt kravgrunnlag i oko xml mottatt når behandling ikke finnes`() {
        val behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(behandling.copy(status = Behandlingsstatus.AVSLUTTET))

        val kravgrunnlagXml = readKravgrunnlagXmlMedIkkeForeldetDato("/kravgrunnlagxml/kravgrunnlag_BA_riktig_eksternfagsakId_ytelsestype.xml")
        val task = opprettTask(kravgrunnlagXml)

        behandleKravgrunnlagTask.doTask(task)
        kravgrunnlagRepository.existsByBehandlingIdAndAktivTrueAndSperretFalse(behandling.id).shouldBeFalse()

        val mottattKravgrunnlagListe =
            mottattXmlRepository.findByEksternKravgrunnlagIdAndVedtakId(
                BigInteger.ZERO,
                BigInteger.ZERO,
            )
        assertOkoXmlMottattData(mottattKravgrunnlagListe, kravgrunnlagXml, Kravstatuskode.NYTT, "0", "2021-03-02-18.50.15.236315")

        mottattXmlArkivRepository.findAll().toList().shouldBeEmpty()
    }

    @Test
    fun `doTask skal lagre mottatt ENDR kravgrunnlag i oko xml mottatt når tabellen allerede har NYTT kravgrunnlag`() {
        val behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(behandling.copy(status = Behandlingsstatus.AVSLUTTET))

        val kravgrunnlagXml = readKravgrunnlagXmlMedIkkeForeldetDato("/kravgrunnlagxml/kravgrunnlag_BA_riktig_eksternfagsakId_ytelsestype.xml")
        behandleKravgrunnlagTask.doTask(opprettTask(kravgrunnlagXml))
        val endretKravgrunnlagXml = readKravgrunnlagXmlMedIkkeForeldetDato("/kravgrunnlagxml/kravgrunnlag_BA_ENDR.xml")

        behandleKravgrunnlagTask.doTask(opprettTask(endretKravgrunnlagXml))

        val mottattKravgrunnlagListe =
            mottattXmlRepository.findByEksternKravgrunnlagIdAndVedtakId(
                BigInteger.ZERO,
                BigInteger.ZERO,
            )
        assertOkoXmlMottattData(mottattKravgrunnlagListe, endretKravgrunnlagXml, Kravstatuskode.ENDRET, "1")

        mottattXmlArkivRepository.findAll().toList().shouldNotBeEmpty()
    }

    private fun opprettTask(kravgrunnlagXml: String): Task =
        taskService.save(
            Task(
                type = BehandleKravgrunnlagTask.TYPE,
                payload = kravgrunnlagXml,
            ),
            SecureLog.Context.tom(),
        )

    private fun assertOkoXmlMottattData(
        mottattKravgrunnlagListe: List<ØkonomiXmlMottatt>,
        kravgrunnlagXml: String,
        kravstatuskode: Kravstatuskode,
        referanse: String,
        forventetKontrollfelt: String = "2021-03-02-18.50.15.236316",
    ) {
        mottattKravgrunnlagListe.shouldNotBeEmpty()
        mottattKravgrunnlagListe.size shouldBe 1
        val mottattKravgrunnlag = mottattKravgrunnlagListe[0]
        mottattKravgrunnlag.kravstatuskode shouldBe kravstatuskode
        mottattKravgrunnlag.eksternFagsakId shouldBe fagsak.eksternFagsakId
        mottattKravgrunnlag.referanse shouldBe referanse
        mottattKravgrunnlag.kontrollfelt shouldBe forventetKontrollfelt
        mottattKravgrunnlag.melding shouldBe kravgrunnlagXml
        mottattKravgrunnlag.eksternKravgrunnlagId shouldBe BigInteger.ZERO
        mottattKravgrunnlag.vedtakId shouldBe BigInteger.ZERO
        mottattKravgrunnlag.ytelsestype shouldBe Ytelsestype.BARNETRYGD
    }

    private fun assertPerioder(kravgrunnlag: Kravgrunnlag431) {
        val perioder = kravgrunnlag.perioder
        perioder.shouldNotBeNull()
        perioder.size shouldBe 1
        (perioder.toList()[0].månedligSkattebeløp == BigDecimal("0.00")).shouldBeTrue()
    }

    private fun assertBeløp(kravgrunnlag: Kravgrunnlag431) {
        val kravgrunnlagsbeløp = kravgrunnlag.perioder.toList()[0].beløp
        kravgrunnlagsbeløp.size shouldBe 2
        kravgrunnlagsbeløp.any { Klassetype.YTEL == it.klassetype }.shouldBeTrue()
        kravgrunnlagsbeløp.any { Klassetype.FEIL == it.klassetype }.shouldBeTrue()
    }

    private fun lagGrunnlagssteg() {
        behandlingsstegstilstandRepository
            .insert(
                Behandlingsstegstilstand(
                    behandlingId = behandling.id,
                    behandlingssteg = Behandlingssteg.GRUNNLAG,
                    behandlingsstegsstatus = Behandlingsstegstatus.VENTER,
                    venteårsak = Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG,
                    tidsfrist = LocalDate.now().plusWeeks(4),
                ),
            )
    }

    private fun assertBehandlingsstegstilstand(
        behandlingsstegstilstand: List<Behandlingsstegstilstand>,
        behandlingssteg: Behandlingssteg,
        behandlingsstegstatus: Behandlingsstegstatus,
    ) {
        behandlingsstegstilstand
            .any {
                behandlingssteg == it.behandlingssteg &&
                    behandlingsstegstatus == it.behandlingsstegsstatus
            }.shouldBeTrue()
    }

    private fun assertHistorikkinnslag(
        historikkinnslagstype: TilbakekrevingHistorikkinnslagstype,
        aktør: Aktør,
        times: Int = 1,
    ) {
        historikkService.hentHistorikkinnslag(behandling.id).forExactly(times) {
            it.type shouldBe historikkinnslagstype.type
            it.tittel shouldBe historikkinnslagstype.tittel
            it.tekst shouldBe historikkinnslagstype.tekst
            it.aktør shouldBe aktør.type
            it.opprettetAv shouldBe aktør.ident
        }
    }

    private fun assertOppdaterFaktainfoTask(referanse: String) {
        taskService
            .findAll()
            .any {
                OppdaterFaktainfoTask.TYPE == it.type &&
                    fagsak.eksternFagsakId == it.metadata["eksternFagsakId"] &&
                    fagsak.ytelsestype.name == it.metadata["ytelsestype"] &&
                    referanse == it.metadata["eksternId"]
            }.shouldBeTrue()
    }

    private fun assertOppgaveTask(
        beskrivelse: String,
        fristDato: LocalDate,
    ) {
        taskService
            .findAll()
            .any {
                OppdaterOppgaveTask.TYPE == it.type &&
                    behandling.id.toString() == it.payload
                beskrivelse == it.metadata["beskrivelse"] &&
                    fristDato.toString() == it.metadata["frist"]
            }.shouldBeTrue()
    }

    private fun assertOpprettOppdaterPrioritetTask() {
        taskService
            .findAll()
            .any {
                OppdaterPrioritetTask.TYPE == it.type &&
                    behandling.id.toString() == it.payload
            }.shouldBeTrue()
    }

    private fun assertIkkeOpprettOppdaterPrioritetTask() {
        taskService
            .findAll()
            .any {
                OppdaterPrioritetTask.TYPE == it.type &&
                    behandling.id.toString() == it.payload
            }.shouldBeFalse()
    }
}
