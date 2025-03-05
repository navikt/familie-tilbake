package no.nav.familie.tilbake.historikkinnslag

import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultat
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.behandling.domain.Behandlingsvedtak
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Iverksettingsstatus
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevsporing
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.log.SecureLog
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime

internal class HistorikkServiceTest : OppslagSpringRunnerTest() {
    override val tømDBEtterHverTest = false

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var brevsporingRepository: BrevsporingRepository

    @Autowired
    private lateinit var behandlingskontrollService: BehandlingskontrollService

    @Autowired
    private lateinit var historikkinnslagRepository: HistorikkinnslagRepository

    private lateinit var historikkService: HistorikkService

    private lateinit var fagsak: Fagsak
    private lateinit var behandling: Behandling
    private val opprettetTidspunkt = LocalDateTime.now()

    @BeforeEach
    fun init() {
        fagsak = fagsakRepository.insert(Testdata.fagsak())
        behandling = behandlingRepository.insert(Testdata.lagBehandling(fagsak.id))

        historikkService =
            HistorikkService(
                behandlingRepository,
                brevsporingRepository,
                historikkinnslagRepository,
            )
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når behandling oppretter automatisk`() {
        val opprettetHistorikkInnslag =
            historikkService.lagHistorikkinnslag(
                behandling.id,
                TilbakekrevingHistorikkinnslagstype.BEHANDLING_OPPRETTET,
                Aktør.Vedtaksløsning,
                opprettetTidspunkt,
            )

        assertOpprettetHistorikkInnslag(
            opprettetHistorikkInnslag = opprettetHistorikkInnslag,
            aktør = Aktør.Vedtaksløsning,
            tittel = TilbakekrevingHistorikkinnslagstype.BEHANDLING_OPPRETTET.tittel,
            type = Historikkinnslagstype.HENDELSE,
        )
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når behandling setter på vent automatisk`() {
        behandlingskontrollService.fortsettBehandling(behandling.id, SecureLog.Context.tom())
        behandlingskontrollService.settBehandlingPåVent(
            behandling.id,
            Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
            LocalDate.now().plusDays(20),
            SecureLog.Context.tom(),
        )

        val opprettetHistorikkInnslag =
            historikkService.lagHistorikkinnslag(
                behandling.id,
                TilbakekrevingHistorikkinnslagstype.BEHANDLING_PÅ_VENT,
                Aktør.Vedtaksløsning,
                opprettetTidspunkt,
                Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING.beskrivelse,
            )

        assertOpprettetHistorikkInnslag(
            aktør = Aktør.Vedtaksløsning,
            tittel = TilbakekrevingHistorikkinnslagstype.BEHANDLING_PÅ_VENT.tittel,
            type = Historikkinnslagstype.HENDELSE,
            tekst = "Årsak: Venter på tilbakemelding fra bruker",
            opprettetHistorikkInnslag = opprettetHistorikkInnslag,
        )
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når behandling setter på vent manuelt`() {
        behandlingskontrollService.fortsettBehandling(behandling.id, SecureLog.Context.tom())
        behandlingskontrollService.settBehandlingPåVent(
            behandling.id,
            Venteårsak.AVVENTER_DOKUMENTASJON,
            LocalDate.now().plusDays(20),
            SecureLog.Context.tom(),
        )

        val opprettetHistorikkInnslag =
            historikkService.lagHistorikkinnslag(
                behandling.id,
                TilbakekrevingHistorikkinnslagstype.BEHANDLING_PÅ_VENT,
                Aktør.Saksbehandler(behandling.ansvarligSaksbehandler),
                opprettetTidspunkt,
                Venteårsak.AVVENTER_DOKUMENTASJON.beskrivelse,
            )

        assertOpprettetHistorikkInnslag(
            aktør = Aktør.Saksbehandler(behandling.ansvarligSaksbehandler),
            tittel = TilbakekrevingHistorikkinnslagstype.BEHANDLING_PÅ_VENT.tittel,
            type = Historikkinnslagstype.HENDELSE,
            tekst = "Årsak: Avventer dokumentasjon",
            opprettetHistorikkInnslag = opprettetHistorikkInnslag,
        )
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når behandling tar av vent manuelt`() {
        val opprettetHistorikkInnslag =
            historikkService.lagHistorikkinnslag(
                behandling.id,
                TilbakekrevingHistorikkinnslagstype.BEHANDLING_GJENOPPTATT,
                Aktør.Saksbehandler(behandling.ansvarligSaksbehandler),
                opprettetTidspunkt,
            )

        assertOpprettetHistorikkInnslag(
            aktør = Aktør.Saksbehandler(behandling.ansvarligSaksbehandler),
            tittel = TilbakekrevingHistorikkinnslagstype.BEHANDLING_GJENOPPTATT.tittel,
            type = Historikkinnslagstype.HENDELSE,
            opprettetHistorikkInnslag = opprettetHistorikkInnslag,
        )
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når behandling mottar et kravgrunnlag`() {
        val opprettetHistorikkInnslag =
            historikkService.lagHistorikkinnslag(
                behandling.id,
                TilbakekrevingHistorikkinnslagstype.KRAVGRUNNLAG_MOTTATT,
                Aktør.Vedtaksløsning,
                opprettetTidspunkt,
            )

        assertOpprettetHistorikkInnslag(
            aktør = Aktør.Vedtaksløsning,
            tittel = TilbakekrevingHistorikkinnslagstype.KRAVGRUNNLAG_MOTTATT.tittel,
            type = Historikkinnslagstype.HENDELSE,
            opprettetHistorikkInnslag = opprettetHistorikkInnslag,
        )
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når behandling sender varselbrev`() {
        brevsporingRepository.insert(
            Brevsporing(
                behandlingId = behandling.id,
                brevtype = Brevtype.VARSEL,
                journalpostId = "testverdi",
                dokumentId = "testverdi",
            ),
        )

        val opprettetHistorikkInnslag =
            historikkService.lagHistorikkinnslag(
                behandlingId = behandling.id,
                historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.VARSELBREV_SENDT,
                aktør = Aktør.Vedtaksløsning,
                opprettetTidspunkt = opprettetTidspunkt,
                brevtype = Brevtype.VARSEL,
            )

        assertOpprettetHistorikkInnslag(
            aktør = Aktør.Vedtaksløsning,
            tittel = TilbakekrevingHistorikkinnslagstype.VARSELBREV_SENDT.tittel,
            type = Historikkinnslagstype.BREV,
            tekst = TilbakekrevingHistorikkinnslagstype.VARSELBREV_SENDT.tekst,
            dokumentId = "testverdi",
            journalpostId = "testverdi",
            opprettetHistorikkInnslag = opprettetHistorikkInnslag,
        )
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når behandling er automatisk henlagt`() {
        val behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(
            behandling.copy(
                resultater =
                    setOf(Behandlingsresultat(type = Behandlingsresultatstype.HENLAGT_KRAVGRUNNLAG_NULLSTILT)),
            ),
        )
        val opprettetHistorikkInnslag =
            historikkService.lagHistorikkinnslag(
                behandling.id,
                TilbakekrevingHistorikkinnslagstype.BEHANDLING_HENLAGT,
                Aktør.Vedtaksløsning,
                opprettetTidspunkt,
            )

        assertOpprettetHistorikkInnslag(
            aktør = Aktør.Vedtaksløsning,
            tittel = TilbakekrevingHistorikkinnslagstype.BEHANDLING_HENLAGT.tittel,
            type = Historikkinnslagstype.HENDELSE,
            tekst = "Årsak: Kravgrunnlaget er nullstilt",
            opprettetHistorikkInnslag = opprettetHistorikkInnslag,
        )
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når behandling er manuelt henlagt`() {
        val behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(
            behandling.copy(
                resultater =
                    setOf(Behandlingsresultat(type = Behandlingsresultatstype.HENLAGT_FEILOPPRETTET)),
            ),
        )

        val opprettetHistorikkInnslag =
            historikkService.lagHistorikkinnslag(
                behandling.id,
                TilbakekrevingHistorikkinnslagstype.BEHANDLING_HENLAGT,
                Aktør.Vedtaksløsning,
                opprettetTidspunkt,
                "testverdi",
            )

        assertOpprettetHistorikkInnslag(
            aktør = Aktør.Vedtaksløsning,
            tittel = TilbakekrevingHistorikkinnslagstype.BEHANDLING_HENLAGT.tittel,
            type = Historikkinnslagstype.HENDELSE,
            tekst = "Årsak: Henlagt, søknaden er feilopprettet, Begrunnelse: testverdi",
            opprettetHistorikkInnslag = opprettetHistorikkInnslag,
        )
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når behandling sender henleggelsesbrev`() {
        brevsporingRepository.insert(
            Brevsporing(
                behandlingId = behandling.id,
                brevtype = Brevtype.HENLEGGELSE,
                journalpostId = "testverdi",
                dokumentId = "testverdi",
            ),
        )

        val opprettetHistorikkInnslag =
            historikkService.lagHistorikkinnslag(
                behandlingId = behandling.id,
                historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.HENLEGGELSESBREV_SENDT,
                aktør = Aktør.Vedtaksløsning,
                opprettetTidspunkt = opprettetTidspunkt,
                brevtype = Brevtype.HENLEGGELSE,
            )

        assertOpprettetHistorikkInnslag(
            aktør = Aktør.Vedtaksløsning,
            tittel = TilbakekrevingHistorikkinnslagstype.HENLEGGELSESBREV_SENDT.tittel,
            type = Historikkinnslagstype.BREV,
            tekst = TilbakekrevingHistorikkinnslagstype.HENLEGGELSESBREV_SENDT.tekst,
            dokumentId = "testverdi",
            journalpostId = "testverdi",
            opprettetHistorikkInnslag = opprettetHistorikkInnslag,
        )
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når behandling ikke sender henleggelsesbrev for ukjent adresse`() {
        brevsporingRepository.insert(
            Brevsporing(
                behandlingId = behandling.id,
                brevtype = Brevtype.HENLEGGELSE,
                journalpostId = "testverdi",
                dokumentId = "testverdi",
            ),
        )

        val opprettetHistorikkInnslag =
            historikkService.lagHistorikkinnslag(
                behandlingId = behandling.id,
                historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.BREV_IKKE_SENDT_UKJENT_ADRESSE,
                aktør = Aktør.Vedtaksløsning,
                opprettetTidspunkt = opprettetTidspunkt,
                brevtype = Brevtype.HENLEGGELSE,
                beskrivelse = TilbakekrevingHistorikkinnslagstype.HENLEGGELSESBREV_SENDT.tekst,
            )

        assertOpprettetHistorikkInnslag(
            aktør = Aktør.Vedtaksløsning,
            tittel = TilbakekrevingHistorikkinnslagstype.BREV_IKKE_SENDT_UKJENT_ADRESSE.tittel,
            type = Historikkinnslagstype.BREV,
            tekst = TilbakekrevingHistorikkinnslagstype.HENLEGGELSESBREV_SENDT.tekst + " er ikke sendt",
            dokumentId = "testverdi",
            journalpostId = "testverdi",
            opprettetHistorikkInnslag = opprettetHistorikkInnslag,
        )
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når fakta steg er utført for behandling`() {
        val opprettetHistorikkInnslag =
            historikkService.lagHistorikkinnslag(
                behandling.id,
                TilbakekrevingHistorikkinnslagstype.FAKTA_VURDERT,
                Aktør.Saksbehandler(behandling.ansvarligSaksbehandler),
                opprettetTidspunkt,
            )

        assertOpprettetHistorikkInnslag(
            aktør = Aktør.Saksbehandler(behandling.ansvarligSaksbehandler),
            tittel = TilbakekrevingHistorikkinnslagstype.FAKTA_VURDERT.tittel,
            type = Historikkinnslagstype.SKJERMLENKE,
            steg = Behandlingssteg.FAKTA.name,
            opprettetHistorikkInnslag = opprettetHistorikkInnslag,
        )
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når foreldelse steg er utført for behandling`() {
        val opprettetHistorikkInnslag =
            historikkService.lagHistorikkinnslag(
                behandling.id,
                TilbakekrevingHistorikkinnslagstype.FORELDELSE_VURDERT,
                Aktør.Saksbehandler(behandling.ansvarligSaksbehandler),
                opprettetTidspunkt,
            )

        assertOpprettetHistorikkInnslag(
            aktør = Aktør.Saksbehandler(behandling.ansvarligSaksbehandler),
            tittel = TilbakekrevingHistorikkinnslagstype.FORELDELSE_VURDERT.tittel,
            type = Historikkinnslagstype.SKJERMLENKE,
            steg = Behandlingssteg.FORELDELSE.name,
            opprettetHistorikkInnslag = opprettetHistorikkInnslag,
        )
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når behandling er fattet`() {
        val behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(
            behandling.copy(
                resultater =
                    setOf(
                        Behandlingsresultat(
                            type = Behandlingsresultatstype.FULL_TILBAKEBETALING,
                            behandlingsvedtak =
                                Behandlingsvedtak(
                                    vedtaksdato = LocalDate.now(),
                                    iverksettingsstatus =
                                        Iverksettingsstatus.IVERKSATT,
                                ),
                        ),
                    ),
            ),
        )
        val opprettetHistorikkInnslag =
            historikkService.lagHistorikkinnslag(
                behandling.id,
                TilbakekrevingHistorikkinnslagstype.VEDTAK_FATTET,
                Aktør.Beslutter(behandling.ansvarligSaksbehandler),
                opprettetTidspunkt,
            )

        assertOpprettetHistorikkInnslag(
            aktør = Aktør.Beslutter(behandling.ansvarligSaksbehandler),
            tittel = TilbakekrevingHistorikkinnslagstype.VEDTAK_FATTET.tittel,
            type = Historikkinnslagstype.HENDELSE,
            tekst = "Resultat: Full tilbakebetaling",
            opprettetHistorikkInnslag = opprettetHistorikkInnslag,
        )
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når man bytter enhet på behandling`() {
        val behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(behandling.copy(behandlendeEnhet = "3434"))

        val opprettetHistorikkInnslag =
            historikkService.lagHistorikkinnslag(
                behandling.id,
                TilbakekrevingHistorikkinnslagstype.ENDRET_ENHET,
                Aktør.Saksbehandler(behandling.ansvarligSaksbehandler),
                opprettetTidspunkt,
                "begrunnelse for endring",
            )

        assertOpprettetHistorikkInnslag(
            aktør = Aktør.Saksbehandler(behandling.ansvarligSaksbehandler),
            tittel = TilbakekrevingHistorikkinnslagstype.ENDRET_ENHET.tittel,
            type = Historikkinnslagstype.HENDELSE,
            tekst = "Ny enhet: 3434, Begrunnelse: begrunnelse for endring",
            opprettetHistorikkInnslag = opprettetHistorikkInnslag,
        )
    }

    private fun assertOpprettetHistorikkInnslag(
        aktør: Aktør,
        tittel: String,
        type: Historikkinnslagstype,
        tekst: String? = null,
        steg: String? = null,
        dokumentId: String? = null,
        journalpostId: String? = null,
        opprettetHistorikkInnslag: Historikkinnslag,
    ) {
        opprettetHistorikkInnslag.behandlingId shouldBe behandling.id
        opprettetHistorikkInnslag.aktør shouldBe aktør.type
        opprettetHistorikkInnslag.opprettetAv shouldBe aktør.ident
        opprettetHistorikkInnslag.opprettetTid shouldBe opprettetTidspunkt
        opprettetHistorikkInnslag.tittel shouldBe tittel
        opprettetHistorikkInnslag.type shouldBe type
        opprettetHistorikkInnslag.tekst shouldBe tekst
        opprettetHistorikkInnslag.steg shouldBe steg
        opprettetHistorikkInnslag.dokumentId shouldBe dokumentId
        opprettetHistorikkInnslag.journalpostId shouldBe journalpostId
    }
}
