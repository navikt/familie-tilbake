package no.nav.familie.tilbake.forvaltning

import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.HentFagsystemsbehandlingRequestSendtRepository
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandling.domain.HentFagsystemsbehandlingRequestSendt
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.datavarehus.saksstatistikk.SendSakshendelseTilDvhTask
import no.nav.familie.tilbake.historikkinnslag.LagHistorikkinnslagTask
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravstatuskode
import no.nav.familie.tilbake.kravgrunnlag.domain.ØkonomiXmlMottatt
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattArkivRepository
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattRepository
import no.nav.familie.tilbake.oppgave.FerdigstillOppgaveTask
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import java.math.BigInteger
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class ForvaltningServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var økonomiXmlMottattRepository: ØkonomiXmlMottattRepository

    @Autowired
    private lateinit var økonomiXmlMottattArkivRepository: ØkonomiXmlMottattArkivRepository

    @Autowired
    private lateinit var taskRepository: TaskRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var requestSendtRepository: HentFagsystemsbehandlingRequestSendtRepository

    @Autowired
    private lateinit var forvaltningService: ForvaltningService

    private val behandling = Testdata.behandling

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(behandling)
        behandlingsstegstilstandRepository
                .insert(Behandlingsstegstilstand(behandlingId = behandling.id,
                                                 behandlingssteg = Behandlingssteg.GRUNNLAG,
                                                 behandlingsstegsstatus = Behandlingsstegstatus.VENTER,
                                                 tidsfrist = LocalDate.now().plusWeeks(3),
                                                 venteårsak = Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG))
    }

    @Test
    fun `korrigerKravgrunnlag skal ikke hente korrigert kravgrunnlag når behandling er avsluttet`() {
        behandlingRepository.update(behandlingRepository.findByIdOrThrow(behandling.id)
                                            .copy(status = Behandlingsstatus.AVSLUTTET))

        val exception = assertFailsWith<RuntimeException> {
            forvaltningService.korrigerKravgrunnlag(behandling.id,
                                                    BigInteger.ZERO)
        }
        assertEquals("Behandling med id=${behandling.id} er allerede ferdig behandlet.", exception.message)
    }

    @Test
    fun `korrigerKravgrunnlag skal hente korrigert kravgrunnlag når behandling allerede har et kravgrunnlag`() {
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)

        assertDoesNotThrow {
            forvaltningService.korrigerKravgrunnlag(behandling.id,
                                                    Testdata.kravgrunnlag431.eksternKravgrunnlagId)
        }

        val kravgrunnlagene = kravgrunnlagRepository.findByBehandlingId(behandling.id)
        assertEquals(2, kravgrunnlagene.size)
        assertTrue { kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(behandling.id) }

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)
    }

    @Test
    fun `korrigerKravgrunnlag skal hente korrigert kravgrunnlag når behandling ikke har et kravgrunnlag`() {
        lagMottattXml()
        assertDoesNotThrow { forvaltningService.korrigerKravgrunnlag(behandling.id, BigInteger.ZERO) }

        val kravgrunnlagene = kravgrunnlagRepository.findByBehandlingId(behandling.id)
        assertEquals(1, kravgrunnlagene.size)
        assertTrue { kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(behandling.id) }

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)
    }


    @Test
    fun `arkiverMottattKravgrunnlag skal arkivere mottatt xml`() {
        val økonomiXmlMottatt = lagMottattXml()
        assertDoesNotThrow { forvaltningService.arkiverMottattKravgrunnlag(økonomiXmlMottatt.id) }

        assertFalse { økonomiXmlMottattRepository.existsById(økonomiXmlMottatt.id) }
        assertTrue {
            økonomiXmlMottattArkivRepository.findByEksternFagsakIdAndYtelsestype(økonomiXmlMottatt.eksternFagsakId,
                                                                                 økonomiXmlMottatt.ytelsestype).isNotEmpty()
        }
    }

    @Test
    fun `tvingHenleggBehandling skal ikke henlegge behandling når behandling er avsluttet`() {
        behandlingRepository.update(behandlingRepository.findByIdOrThrow(behandling.id)
                                            .copy(status = Behandlingsstatus.AVSLUTTET))

        val exception = assertFailsWith<RuntimeException> {
            forvaltningService.tvingHenleggBehandling(behandling.id)
        }
        assertEquals("Behandling med id=${behandling.id} er allerede ferdig behandlet.", exception.message)
    }

    @Test
    fun `tvingHenleggBehandling skal henlegge behandling når behandling ikke er avsluttet`() {
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)
        forvaltningService.korrigerKravgrunnlag(behandling.id, Testdata.kravgrunnlag431.eksternKravgrunnlagId)

        var behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)

        assertDoesNotThrow { forvaltningService.tvingHenleggBehandling(behandling.id) }

        behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.AVBRUTT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.AVBRUTT)

        val oppdatertBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        assertTrue { oppdatertBehandling.erAvsluttet }
        assertEquals(ContextService.hentSaksbehandler(), oppdatertBehandling.ansvarligSaksbehandler)
        assertEquals(LocalDate.now(), oppdatertBehandling.avsluttetDato)
        assertEquals(Behandlingsresultatstype.HENLAGT_TEKNISK_VEDLIKEHOLD, oppdatertBehandling.sisteResultat!!.type)

        val tasker = taskRepository.findAll()
        assertTrue {
            tasker.any {
                LagHistorikkinnslagTask.TYPE == it.type &&
                behandling.id.toString() == it.payload &&
                Aktør.SAKSBEHANDLER.name == it.metadata.getProperty("aktør") &&
                TilbakekrevingHistorikkinnslagstype.BEHANDLING_HENLAGT.name == it.metadata.getProperty("historikkinnslagstype")
            }
        }
        assertTrue {
            tasker.any {
                SendSakshendelseTilDvhTask.TASK_TYPE == it.type &&
                behandling.id.toString() == it.payload
            }
        }
        assertTrue {
            tasker.any {
                FerdigstillOppgaveTask.TYPE == it.type &&
                behandling.id.toString() == it.payload
            }
        }
    }

    @Test
    fun `hentFagsystemsbehandling skal ikke hente fagsystemsbehandling når behandling er avsluttet`() {
        behandlingRepository.update(behandlingRepository.findByIdOrThrow(behandling.id)
                                            .copy(status = Behandlingsstatus.AVSLUTTET))

        val exception = assertFailsWith<RuntimeException> {
            forvaltningService.hentFagsystemsbehandling(behandling.id)
        }
        assertEquals("Behandling med id=${behandling.id} er allerede ferdig behandlet.", exception.message)
    }

    @Test
    fun `hentFagsystemsbehandling skal sende request til fagsystem for å hente fagsystemsbehandling`() {
        val eksternFagsakId = Testdata.fagsak.eksternFagsakId
        val ytelsestype = Testdata.fagsak.ytelsestype
        val eksternId = behandling.aktivFagsystemsbehandling.eksternId
        // finnes en eksisterende request
        val requestSendt = requestSendtRepository.insert(HentFagsystemsbehandlingRequestSendt(eksternFagsakId = eksternFagsakId,
                                                                                              ytelsestype = ytelsestype,
                                                                                              eksternId = eksternId))
        assertDoesNotThrow { forvaltningService.hentFagsystemsbehandling(behandling.id) }
        assertNull(requestSendtRepository.findByIdOrNull(requestSendt.id))
        assertNotNull(requestSendtRepository.findByEksternFagsakIdAndYtelsestypeAndEksternId(eksternFagsakId,
                                                                                             ytelsestype,
                                                                                             eksternId))
    }

    private fun lagMottattXml(): ØkonomiXmlMottatt {
        val mottattXml = readXml("/kravgrunnlagxml/kravgrunnlag_BA_riktig_eksternfagsakId_ytelsestype.xml")
        return økonomiXmlMottattRepository.insert(ØkonomiXmlMottatt(melding = mottattXml,
                                                                    kravstatuskode = Kravstatuskode.NYTT,
                                                                    eksternFagsakId = "0",
                                                                    ytelsestype = Ytelsestype.BARNETRYGD,
                                                                    referanse = "0",
                                                                    eksternKravgrunnlagId = BigInteger.ZERO,
                                                                    vedtakId = BigInteger.ZERO,
                                                                    kontrollfelt = "2021-03-02-18.50.15.236315",
                                                                    sperret = false))
    }

    private fun assertBehandlingssteg(behandlingsstegstilstand: List<Behandlingsstegstilstand>,
                                      behandlingssteg: Behandlingssteg,
                                      behandlingsstegstatus: Behandlingsstegstatus) {
        assertTrue {
            behandlingsstegstilstand.any {
                behandlingssteg == it.behandlingssteg &&
                behandlingsstegstatus == it.behandlingsstegsstatus
            }
        }
    }
}