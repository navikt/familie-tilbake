package no.nav.familie.tilbake.historikkinnslag

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.Applikasjon
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.kontrakter.felles.historikkinnslag.Historikkinnslagstype
import no.nav.familie.kontrakter.felles.historikkinnslag.OpprettHistorikkinnslagRequest
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultat
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.behandling.domain.Behandlingsvedtak
import no.nav.familie.tilbake.behandling.domain.Iverksettingsstatus
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevsporing
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.integration.kafka.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.util.concurrent.SettableListenableFuture
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals

internal class HistorikkServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var brevsporingRepository: BrevsporingRepository

    @Autowired
    private lateinit var behandlingskontrollService: BehandlingskontrollService

    private val mockKafkaTemplate: KafkaTemplate<String, String> = mockk()
    private lateinit var spyKafkaProducer: KafkaProducer
    private lateinit var historikkService: HistorikkService

    private val fagsak = Testdata.fagsak
    private val behandling = Testdata.behandling
    private val behandlingId = behandling.id
    private val opprettetTidspunkt = LocalDateTime.now()

    private val behandlingIdSlot = slot<UUID>()
    private val keySlot = slot<String>()
    private val historikkinnslagRecordSlot = slot<OpprettHistorikkinnslagRequest>()

    @BeforeEach
    fun init() {
        fagsakRepository.insert(fagsak)
        behandlingRepository.insert(behandling)

        spyKafkaProducer = spyk(KafkaProducer(mockKafkaTemplate))
        historikkService = HistorikkService(behandlingRepository,
                                            fagsakRepository,
                                            brevsporingRepository,
                                            behandlingskontrollService,
                                            spyKafkaProducer)
        val future = SettableListenableFuture<SendResult<String, String>>()
        every { mockKafkaTemplate.send(any<ProducerRecord<String, String>>()) }.returns(future)
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når behandling oppretter automatisk`() {
        historikkService.lagHistorikkinnslag(behandlingId,
                                             TilbakekrevingHistorikkinnslagstype.BEHANDLING_OPPRETTET,
                                             Aktør.VEDTAKSLØSNING,
                                             opprettetTidspunkt)
        verify {
            spyKafkaProducer.sendHistorikkinnslag(capture(behandlingIdSlot),
                                                  capture(keySlot),
                                                  capture(historikkinnslagRecordSlot))
        }
        assertHistorikkinnslagRequest(Aktør.VEDTAKSLØSNING, "VL", TilbakekrevingHistorikkinnslagstype.BEHANDLING_OPPRETTET.tittel,
                                      Historikkinnslagstype.HENDELSE)
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når behandling setter på vent automatisk`() {
        behandlingskontrollService.fortsettBehandling(behandlingId)
        behandlingskontrollService.settBehandlingPåVent(behandlingId,
                                                        Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
                                                        LocalDate.now().plusDays(20))
        historikkService.lagHistorikkinnslag(behandlingId,
                                             TilbakekrevingHistorikkinnslagstype.BEHANDLING_PÅ_VENT,
                                             Aktør.VEDTAKSLØSNING,
                                             opprettetTidspunkt)
        verify {
            spyKafkaProducer.sendHistorikkinnslag(capture(behandlingIdSlot),
                                                  capture(keySlot),
                                                  capture(historikkinnslagRecordSlot))
        }
        assertHistorikkinnslagRequest(aktør = Aktør.VEDTAKSLØSNING,
                                      aktørIdent = "VL",
                                      tittel = TilbakekrevingHistorikkinnslagstype.BEHANDLING_PÅ_VENT.tittel,
                                      tekst = "Årsak: Venter på tilbakemelding fra bruker",
                                      type = Historikkinnslagstype.HENDELSE)
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når behandling setter på vent manuelt`() {
        behandlingskontrollService.fortsettBehandling(behandlingId)
        behandlingskontrollService.settBehandlingPåVent(behandlingId,
                                                        Venteårsak.AVVENTER_DOKUMENTASJON,
                                                        LocalDate.now().plusDays(20))
        historikkService.lagHistorikkinnslag(behandlingId,
                                             TilbakekrevingHistorikkinnslagstype.BEHANDLING_PÅ_VENT,
                                             Aktør.SAKSBEHANDLER,
                                             opprettetTidspunkt)
        verify {
            spyKafkaProducer.sendHistorikkinnslag(capture(behandlingIdSlot),
                                                  capture(keySlot),
                                                  capture(historikkinnslagRecordSlot))
        }
        assertHistorikkinnslagRequest(aktør = Aktør.SAKSBEHANDLER,
                                      aktørIdent = behandling.ansvarligSaksbehandler,
                                      tittel = TilbakekrevingHistorikkinnslagstype.BEHANDLING_PÅ_VENT.tittel,
                                      tekst = "Årsak: Avventer dokumentasjon",
                                      type = Historikkinnslagstype.HENDELSE)
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når behandling tar av vent manuelt`() {
        historikkService.lagHistorikkinnslag(behandlingId,
                                             TilbakekrevingHistorikkinnslagstype.BEHANDLING_GJENOPPTATT,
                                             Aktør.SAKSBEHANDLER,
                                             opprettetTidspunkt)
        verify {
            spyKafkaProducer.sendHistorikkinnslag(capture(behandlingIdSlot),
                                                  capture(keySlot),
                                                  capture(historikkinnslagRecordSlot))
        }
        assertHistorikkinnslagRequest(aktør = Aktør.SAKSBEHANDLER,
                                      aktørIdent = behandling.ansvarligSaksbehandler,
                                      tittel = TilbakekrevingHistorikkinnslagstype.BEHANDLING_GJENOPPTATT.tittel,
                                      type = Historikkinnslagstype.HENDELSE)
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når behandling mottar et kravgrunnlag`() {
        historikkService.lagHistorikkinnslag(behandlingId,
                                             TilbakekrevingHistorikkinnslagstype.KRAVGRUNNLAG_MOTTATT,
                                             Aktør.VEDTAKSLØSNING,
                                             opprettetTidspunkt)
        verify {
            spyKafkaProducer.sendHistorikkinnslag(capture(behandlingIdSlot),
                                                  capture(keySlot),
                                                  capture(historikkinnslagRecordSlot))
        }
        assertHistorikkinnslagRequest(aktør = Aktør.VEDTAKSLØSNING,
                                      aktørIdent = "VL",
                                      tittel = TilbakekrevingHistorikkinnslagstype.KRAVGRUNNLAG_MOTTATT.tittel,
                                      type = Historikkinnslagstype.HENDELSE)
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når behandling sender varselbrev`() {
        brevsporingRepository.insert(Brevsporing(behandlingId = behandlingId,
                                                 brevtype = Brevtype.VARSEL,
                                                 journalpostId = "testverdi",
                                                 dokumentId = "testverdi"))
        historikkService.lagHistorikkinnslag(behandlingId,
                                             TilbakekrevingHistorikkinnslagstype.VARSELBREV_SENDT,
                                             Aktør.VEDTAKSLØSNING,
                                             opprettetTidspunkt)
        verify {
            spyKafkaProducer.sendHistorikkinnslag(capture(behandlingIdSlot),
                                                  capture(keySlot),
                                                  capture(historikkinnslagRecordSlot))
        }
        assertHistorikkinnslagRequest(aktør = Aktør.VEDTAKSLØSNING,
                                      aktørIdent = "VL",
                                      tittel = TilbakekrevingHistorikkinnslagstype.VARSELBREV_SENDT.tittel,
                                      tekst = TilbakekrevingHistorikkinnslagstype.VARSELBREV_SENDT.tekst,
                                      type = Historikkinnslagstype.BREV,
                                      dokumentId = "testverdi",
                                      journalpostId = "testverdi")
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når behandling er automatisk henlagt`() {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandlingRepository.update(
                behandling.copy(resultater =
                                setOf(Behandlingsresultat(type = Behandlingsresultatstype.HENLAGT_KRAVGRUNNLAG_NULLSTILT))))
        historikkService.lagHistorikkinnslag(behandlingId,
                                             TilbakekrevingHistorikkinnslagstype.BEHANDLING_HENLAGT,
                                             Aktør.VEDTAKSLØSNING,
                                             opprettetTidspunkt)

        verify {
            spyKafkaProducer.sendHistorikkinnslag(capture(behandlingIdSlot),
                                                  capture(keySlot),
                                                  capture(historikkinnslagRecordSlot))
        }

        assertHistorikkinnslagRequest(aktør = Aktør.VEDTAKSLØSNING,
                                      aktørIdent = "VL",
                                      tittel = TilbakekrevingHistorikkinnslagstype.BEHANDLING_HENLAGT.tittel,
                                      tekst = "Årsak: Kravgrunnlaget er nullstilt",
                                      type = Historikkinnslagstype.HENDELSE)
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når behandling er manuelt henlagt`() {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandlingRepository.update(
                behandling.copy(resultater =
                                setOf(Behandlingsresultat(type = Behandlingsresultatstype.HENLAGT_FEILOPPRETTET))))
        historikkService.lagHistorikkinnslag(behandlingId,
                                             TilbakekrevingHistorikkinnslagstype.BEHANDLING_HENLAGT,
                                             Aktør.VEDTAKSLØSNING,
                                             opprettetTidspunkt,
                                             "testverdi")

        verify {
            spyKafkaProducer.sendHistorikkinnslag(capture(behandlingIdSlot),
                                                  capture(keySlot),
                                                  capture(historikkinnslagRecordSlot))
        }

        assertHistorikkinnslagRequest(aktør = Aktør.VEDTAKSLØSNING,
                                      aktørIdent = "VL",
                                      tittel = TilbakekrevingHistorikkinnslagstype.BEHANDLING_HENLAGT.tittel,
                                      tekst = "Årsak: Henlagt, søknaden er feilopprettet, Begrunnelse: testverdi",
                                      type = Historikkinnslagstype.HENDELSE)
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når behandling sender henleggelsesbrev`() {
        brevsporingRepository.insert(Brevsporing(behandlingId = behandlingId,
                                                 brevtype = Brevtype.HENLEGGELSE,
                                                 journalpostId = "testverdi",
                                                 dokumentId = "testverdi"))
        historikkService.lagHistorikkinnslag(behandlingId,
                                             TilbakekrevingHistorikkinnslagstype.HENLEGGELSESBREV_SENDT,
                                             Aktør.VEDTAKSLØSNING,
                                             opprettetTidspunkt)
        verify {
            spyKafkaProducer.sendHistorikkinnslag(capture(behandlingIdSlot),
                                                  capture(keySlot),
                                                  capture(historikkinnslagRecordSlot))
        }
        assertHistorikkinnslagRequest(aktør = Aktør.VEDTAKSLØSNING,
                                      aktørIdent = "VL",
                                      tittel = TilbakekrevingHistorikkinnslagstype.HENLEGGELSESBREV_SENDT.tittel,
                                      tekst = TilbakekrevingHistorikkinnslagstype.HENLEGGELSESBREV_SENDT.tekst,
                                      type = Historikkinnslagstype.BREV,
                                      dokumentId = "testverdi",
                                      journalpostId = "testverdi")
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når fakta steg er utført for behandling`() {
        historikkService.lagHistorikkinnslag(behandlingId,
                                             TilbakekrevingHistorikkinnslagstype.FAKTA_VURDERT,
                                             Aktør.SAKSBEHANDLER,
                                             opprettetTidspunkt)
        verify {
            spyKafkaProducer.sendHistorikkinnslag(capture(behandlingIdSlot),
                                                  capture(keySlot),
                                                  capture(historikkinnslagRecordSlot))
        }
        assertHistorikkinnslagRequest(aktør = Aktør.SAKSBEHANDLER,
                                      aktørIdent = behandling.ansvarligSaksbehandler,
                                      tittel = TilbakekrevingHistorikkinnslagstype.FAKTA_VURDERT.tittel,
                                      type = Historikkinnslagstype.SKJERMLENKE,
                                      steg = Behandlingssteg.FAKTA.name)
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når foreldelse steg er utført for behandling`() {
        historikkService.lagHistorikkinnslag(behandlingId,
                                             TilbakekrevingHistorikkinnslagstype.FORELDELSE_VURDERT,
                                             Aktør.SAKSBEHANDLER,
                                             opprettetTidspunkt)
        verify {
            spyKafkaProducer.sendHistorikkinnslag(capture(behandlingIdSlot),
                                                  capture(keySlot),
                                                  capture(historikkinnslagRecordSlot))
        }
        assertHistorikkinnslagRequest(aktør = Aktør.SAKSBEHANDLER,
                                      aktørIdent = behandling.ansvarligSaksbehandler,
                                      tittel = TilbakekrevingHistorikkinnslagstype.FORELDELSE_VURDERT.tittel,
                                      type = Historikkinnslagstype.SKJERMLENKE,
                                      steg = Behandlingssteg.FORELDELSE.name)
    }

    @Test
    fun `lagHistorikkinnslag skal lage historikkinnslag når behandling er fattet`() {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        behandlingRepository.update(
                behandling.copy(resultater =
                                setOf(Behandlingsresultat(type = Behandlingsresultatstype.FULL_TILBAKEBETALING,
                                                          behandlingsvedtak = Behandlingsvedtak(vedtaksdato = LocalDate.now(),
                                                                                                iverksettingsstatus =
                                                                                                Iverksettingsstatus.IVERKSATT)))))
        historikkService.lagHistorikkinnslag(behandlingId,
                                             TilbakekrevingHistorikkinnslagstype.VEDTAK_FATTET,
                                             Aktør.BESLUTTER,
                                             opprettetTidspunkt)
        verify {
            spyKafkaProducer.sendHistorikkinnslag(capture(behandlingIdSlot),
                                                  capture(keySlot),
                                                  capture(historikkinnslagRecordSlot))
        }
        assertHistorikkinnslagRequest(aktør = Aktør.BESLUTTER,
                                      aktørIdent = requireNotNull(behandling.ansvarligBeslutter),
                                      tittel = TilbakekrevingHistorikkinnslagstype.VEDTAK_FATTET.tittel,
                                      tekst = "Resultat: Full tilbakebetaling",
                                      type = Historikkinnslagstype.HENDELSE)
    }


    private fun assertHistorikkinnslagRequest(aktør: Aktør, aktørIdent: String,
                                              tittel: String, type: Historikkinnslagstype,
                                              tekst: String? = null, steg: String? = null,
                                              dokumentId: String? = null, journalpostId: String? = null) {
        assertEquals(behandlingId, behandlingIdSlot.captured)
        val request = historikkinnslagRecordSlot.captured
        assertEquals(request.behandlingId, keySlot.captured)

        assertEquals(fagsak.eksternFagsakId, request.eksternFagsakId)
        assertEquals(aktør, request.aktør)
        assertEquals(aktørIdent, request.aktørIdent)
        assertEquals(opprettetTidspunkt, request.opprettetTidspunkt)
        assertEquals(Fagsystem.BA, request.fagsystem)
        assertEquals(Applikasjon.FAMILIE_TILBAKE, request.applikasjon)
        assertEquals(tittel, request.tittel)
        assertEquals(type, request.type)
        assertEquals(tekst, request.tekst)
        assertEquals(steg, request.steg)
        assertEquals(dokumentId, request.dokumentId)
        assertEquals(journalpostId, request.journalpostId)
    }

}
