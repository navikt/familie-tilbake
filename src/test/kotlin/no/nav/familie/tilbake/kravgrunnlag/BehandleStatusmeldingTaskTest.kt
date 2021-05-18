package no.nav.familie.tilbake.kravgrunnlag

import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg.FAKTA
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg.FORELDELSE
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg.FORESLÅ_VEDTAK
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg.GRUNNLAG
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg.VARSEL
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg.VILKÅRSVURDERING
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravstatuskode
import no.nav.familie.tilbake.kravgrunnlag.task.BehandleKravgrunnlagTask
import no.nav.familie.tilbake.kravgrunnlag.task.BehandleStatusmeldingTask
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class BehandleStatusmeldingTaskTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var mottattXmlRepository: ØkonomiXmlMottattRepository

    @Autowired
    private lateinit var mottattXmlArkivRepository: ØkonomiXmlMottattArkivRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var taskRepository: TaskRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var behandleStatusmeldingTask: BehandleStatusmeldingTask

    @Autowired
    private lateinit var behandleKravgrunnlagTask: BehandleKravgrunnlagTask

    private val fagsak = Testdata.fagsak
    private val behandling = Testdata.behandling

    @BeforeEach
    fun init() {
        fagsakRepository.insert(fagsak)
    }

    @Test
    fun `doTask skal ikke prosessere SPER melding når det ikke finnes et kravgrunnlag`() {
        val statusmeldingXml = readXml("/kravvedtakstatusxml/statusmelding_SPER_BA.xml")
        val task = opprettTask(statusmeldingXml, BehandleStatusmeldingTask.TYPE)

        val exception = assertFails { behandleStatusmeldingTask.doTask(task) }
        assertEquals("Det finnes intet kravgrunnlag for fagsystemId=${fagsak.eksternFagsakId} " +
                     "og ytelsestype=${fagsak.ytelsestype}",
                     exception.message)
    }

    @Test
    fun `doTask skal ikke prosessere ugyldig SPER melding`() {
        val statusmeldingXml = readXml("/kravvedtakstatusxml/statusmelding_SPER_ugyldig.xml")
        val task = opprettTask(statusmeldingXml, BehandleStatusmeldingTask.TYPE)

        val exception = assertFails { behandleStatusmeldingTask.doTask(task) }
        assertEquals("Ugyldig statusmelding for vedtakId=0, Mangler referanse.",
                     exception.message)
    }

    @Test
    fun `doTask skal prosessere SPER melding uten behandling`() {
        opprettGrunnlag()

        val statusmeldingXml = readXml("/kravvedtakstatusxml/statusmelding_SPER_BA.xml")
        val task = opprettTask(statusmeldingXml, BehandleStatusmeldingTask.TYPE)

        assertDoesNotThrow { behandleStatusmeldingTask.doTask(task) }
        assertTrue { kravgrunnlagRepository.findAll().toList().isEmpty() }

        val mottattXmlListe = mottattXmlRepository.findByEksternFagsakIdAndYtelsestype(fagsak.eksternFagsakId, fagsak.ytelsestype)
        assertEquals(1, mottattXmlListe.size)
        val mottattXml = mottattXmlListe[0]
        assertTrue { mottattXml.melding.contains(Constants.kravgrunnlagXmlRootElement) }
        assertTrue { mottattXml.sperret }

        assertArkivertXml(1, false, Kravstatuskode.SPERRET)
    }

    @Test
    fun `doTask skal prosessere ENDR melding uten behandling`() {
        opprettGrunnlag()

        var statusmeldingXml = readXml("/kravvedtakstatusxml/statusmelding_SPER_BA.xml")
        var task = opprettTask(statusmeldingXml, BehandleStatusmeldingTask.TYPE)
        behandleStatusmeldingTask.doTask(task)

        statusmeldingXml = readXml("/kravvedtakstatusxml/statusmelding_ENDR_BA.xml")
        task = opprettTask(statusmeldingXml, BehandleStatusmeldingTask.TYPE)

        assertDoesNotThrow { behandleStatusmeldingTask.doTask(task) }
        assertTrue { kravgrunnlagRepository.findAll().toList().isEmpty() }

        val mottattXmlListe = mottattXmlRepository.findByEksternFagsakIdAndYtelsestype(fagsak.eksternFagsakId, fagsak.ytelsestype)
        assertEquals(1, mottattXmlListe.size)
        val mottattXml = mottattXmlListe[0]
        assertTrue { mottattXml.melding.contains(Constants.kravgrunnlagXmlRootElement) }
        assertFalse { mottattXml.sperret }

        assertArkivertXml(2, false, Kravstatuskode.SPERRET, Kravstatuskode.ENDRET)
    }

    @Test
    fun `doTask skal prosessere AVSL melding uten behandling`() {
        opprettGrunnlag()

        val statusmeldingXml = readXml("/kravvedtakstatusxml/statusmelding_AVSL_BA.xml")
        val task = opprettTask(statusmeldingXml, BehandleStatusmeldingTask.TYPE)

        assertDoesNotThrow { behandleStatusmeldingTask.doTask(task) }
        assertTrue { kravgrunnlagRepository.findAll().toList().isEmpty() }
        assertTrue {
            mottattXmlRepository.findByEksternFagsakIdAndYtelsestype(fagsak.eksternFagsakId, fagsak.ytelsestype)
                    .isEmpty()
        }

        assertArkivertXml(2, true, Kravstatuskode.AVSLUTTET)
    }

    @Test
    fun `doTask skal prosessere SPER melding med behandling på VARSEL steg`() {
        behandlingRepository.insert(behandling)
        lagBehandlingsstegstilstand(VARSEL, Behandlingsstegstatus.VENTER)

        opprettGrunnlag()

        val statusmeldingXml = readXml("/kravvedtakstatusxml/statusmelding_SPER_BA.xml")
        val task = opprettTask(statusmeldingXml, BehandleStatusmeldingTask.TYPE)

        assertDoesNotThrow { behandleStatusmeldingTask.doTask(task) }
        assertTrue { kravgrunnlagRepository.findByBehandlingId(behandling.id).isNotEmpty() }
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
        assertTrue { kravgrunnlag.sperret }

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingstegstilstand(behandlingsstegstilstand, VARSEL, Behandlingsstegstatus.VENTER)
    }

    @Test
    fun `doTask skal prosessere SPER melding med behandling på FAKTA steg`() {
        behandlingRepository.insert(behandling)
        lagBehandlingsstegstilstand(VARSEL, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(GRUNNLAG, Behandlingsstegstatus.VENTER)

        opprettGrunnlag()
        var behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingstegstilstand(behandlingsstegstilstand, FAKTA, Behandlingsstegstatus.KLAR)

        val statusmeldingXml = readXml("/kravvedtakstatusxml/statusmelding_SPER_BA.xml")
        val task = opprettTask(statusmeldingXml, BehandleStatusmeldingTask.TYPE)

        assertDoesNotThrow { behandleStatusmeldingTask.doTask(task) }
        assertTrue { kravgrunnlagRepository.findByBehandlingId(behandling.id).isNotEmpty() }
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
        assertTrue { kravgrunnlag.sperret }

        behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingstegstilstand(behandlingsstegstilstand, FAKTA, Behandlingsstegstatus.AVBRUTT)
        assertBehandlingstegstilstand(behandlingsstegstilstand, GRUNNLAG, Behandlingsstegstatus.VENTER)
        assertBehandlingstegstilstand(behandlingsstegstilstand, VARSEL, Behandlingsstegstatus.UTFØRT)

        assertArkivertXml(2, true, Kravstatuskode.SPERRET)
        assertTrue {
            mottattXmlRepository.findByEksternFagsakIdAndYtelsestype(fagsak.eksternFagsakId, fagsak.ytelsestype)
                    .isEmpty()
        }
    }

    @Test
    fun `doTask skal prosessere SPER melding med behandling på FORESLÅ VEDTAK steg`() {
        behandlingRepository.insert(behandling)
        settBehandlingTilForeslåVedtakSteg()

        val statusmeldingXml = readXml("/kravvedtakstatusxml/statusmelding_SPER_BA.xml")
        val task = opprettTask(statusmeldingXml, BehandleStatusmeldingTask.TYPE)

        assertDoesNotThrow { behandleStatusmeldingTask.doTask(task) }
        assertTrue { kravgrunnlagRepository.findByBehandlingId(behandling.id).isNotEmpty() }
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
        assertTrue { kravgrunnlag.sperret }

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingstegstilstand(behandlingsstegstilstand, FAKTA, Behandlingsstegstatus.UTFØRT)
        assertBehandlingstegstilstand(behandlingsstegstilstand, GRUNNLAG, Behandlingsstegstatus.VENTER)
        assertBehandlingstegstilstand(behandlingsstegstilstand, VARSEL, Behandlingsstegstatus.UTFØRT)
        assertBehandlingstegstilstand(behandlingsstegstilstand, FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        assertBehandlingstegstilstand(behandlingsstegstilstand, VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        assertBehandlingstegstilstand(behandlingsstegstilstand, FORESLÅ_VEDTAK, Behandlingsstegstatus.AVBRUTT)

        assertArkivertXml(2, true, Kravstatuskode.SPERRET)
        assertTrue {
            mottattXmlRepository.findByEksternFagsakIdAndYtelsestype(fagsak.eksternFagsakId, fagsak.ytelsestype)
                    .isEmpty()
        }
    }

    @Test
    fun `doTask skal prosessere ENDR melding med behandling på FORESLÅ VEDTAK steg`() {
        behandlingRepository.insert(behandling)
        settBehandlingTilForeslåVedtakSteg()

        var statusmeldingXml = readXml("/kravvedtakstatusxml/statusmelding_SPER_BA.xml")
        var task = opprettTask(statusmeldingXml, BehandleStatusmeldingTask.TYPE)
        behandleStatusmeldingTask.doTask(task)

        statusmeldingXml = readXml("/kravvedtakstatusxml/statusmelding_ENDR_BA.xml")
        task = opprettTask(statusmeldingXml, BehandleStatusmeldingTask.TYPE)

        assertDoesNotThrow { behandleStatusmeldingTask.doTask(task) }
        assertTrue { kravgrunnlagRepository.findByBehandlingId(behandling.id).isNotEmpty() }
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
        assertFalse { kravgrunnlag.sperret }

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingstegstilstand(behandlingsstegstilstand, FAKTA, Behandlingsstegstatus.UTFØRT)
        assertBehandlingstegstilstand(behandlingsstegstilstand, GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertBehandlingstegstilstand(behandlingsstegstilstand, VARSEL, Behandlingsstegstatus.UTFØRT)
        assertBehandlingstegstilstand(behandlingsstegstilstand, FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        assertBehandlingstegstilstand(behandlingsstegstilstand, VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        assertBehandlingstegstilstand(behandlingsstegstilstand, FORESLÅ_VEDTAK, Behandlingsstegstatus.KLAR)

        assertArkivertXml(3, true, Kravstatuskode.SPERRET, Kravstatuskode.ENDRET)

        assertTrue {
            mottattXmlRepository.findByEksternFagsakIdAndYtelsestype(fagsak.eksternFagsakId, fagsak.ytelsestype)
                    .isEmpty()
        }
    }

    @Test
    fun `doTask skal prosessere AVSL melding med behandling på FORESLÅ VEDTAK steg`() {
        behandlingRepository.insert(behandling)
        settBehandlingTilForeslåVedtakSteg()

        val statusmeldingXml = readXml("/kravvedtakstatusxml/statusmelding_AVSL_BA.xml")
        val task = opprettTask(statusmeldingXml, BehandleStatusmeldingTask.TYPE)

        assertDoesNotThrow { behandleStatusmeldingTask.doTask(task) }
        assertTrue { kravgrunnlagRepository.findByBehandlingId(behandling.id).isNotEmpty() }
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
        assertFalse { kravgrunnlag.sperret }
        assertTrue { kravgrunnlag.avsluttet }

        assertArkivertXml(2, true, Kravstatuskode.AVSLUTTET)

        assertTrue {
            mottattXmlRepository.findByEksternFagsakIdAndYtelsestype(fagsak.eksternFagsakId, fagsak.ytelsestype)
                    .isEmpty()
        }

        val behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        assertEquals(Behandlingsstatus.AVSLUTTET, behandling.status)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertTrue {
            behandlingsstegstilstand.filter { VARSEL != it.behandlingssteg }
                    .all { Behandlingsstegstatus.AVBRUTT == it.behandlingsstegsstatus }
        }
        assertBehandlingstegstilstand(behandlingsstegstilstand, VARSEL, Behandlingsstegstatus.UTFØRT)
    }

    private fun settBehandlingTilForeslåVedtakSteg() {
        lagBehandlingsstegstilstand(VARSEL, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(GRUNNLAG, Behandlingsstegstatus.VENTER)

        opprettGrunnlag()

        //oppdater FAKTA steg manuelt til UTFØRT
        val aktivtBehandlingsstegstilstand = behandlingsstegstilstandRepository
                .findByBehandlingIdAndBehandlingssteg(behandling.id, FAKTA)
        assertTrue { aktivtBehandlingsstegstilstand != null }
        aktivtBehandlingsstegstilstand?.let {
            behandlingsstegstilstandRepository.update(it.copy(behandlingsstegsstatus = Behandlingsstegstatus.UTFØRT))
        }
        //sett aktivt behandlingssteg til FORESLÅ_VEDTAK
        lagBehandlingsstegstilstand(FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        lagBehandlingsstegstilstand(VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(FORESLÅ_VEDTAK, Behandlingsstegstatus.KLAR)
    }

    private fun opprettGrunnlag() {
        val kravgrunnlagXml = readXml("/kravgrunnlagxml/kravgrunnlag_BA_riktig_eksternfagsakId_ytelsestype.xml")
        val task = opprettTask(kravgrunnlagXml, BehandleKravgrunnlagTask.TYPE)
        behandleKravgrunnlagTask.doTask(task)
    }

    private fun lagBehandlingsstegstilstand(behandlingssteg: Behandlingssteg,
                                            behandlingsstegstatus: Behandlingsstegstatus,
                                            venteårsak: Venteårsak? = null,
                                            tidsfrist: LocalDate? = null) {
        behandlingsstegstilstandRepository.insert(Behandlingsstegstilstand(behandlingId = behandling.id,
                                                                           behandlingssteg = behandlingssteg,
                                                                           behandlingsstegsstatus = behandlingsstegstatus,
                                                                           venteårsak = venteårsak,
                                                                           tidsfrist = tidsfrist))
    }

    private fun assertBehandlingstegstilstand(behandlingsstegstilstand: List<Behandlingsstegstilstand>,
                                              behandlingssteg: Behandlingssteg,
                                              behandlingsstegstatus: Behandlingsstegstatus) {
        assertTrue {
            behandlingsstegstilstand.any {
                behandlingssteg == it.behandlingssteg &&
                behandlingsstegstatus == it.behandlingsstegsstatus
            }
        }
    }

    private fun assertArkivertXml(size: Int,
                                  finnesKravgrunnlag: Boolean,
                                  vararg statusmeldingKravstatuskode: Kravstatuskode) {
        val arkivertXmlListe = mottattXmlArkivRepository.findByEksternFagsakIdAndYtelsestype(fagsak.eksternFagsakId,
                                                                                             fagsak.ytelsestype)
        assertEquals(size, arkivertXmlListe.size)

        if (finnesKravgrunnlag) {
            assertTrue { arkivertXmlListe.any { it.melding.contains(Constants.kravgrunnlagXmlRootElement) } }
        }
        statusmeldingKravstatuskode.forEach { kravstatuskode ->
            assertTrue {
                arkivertXmlListe.any {
                    it.melding.contains(Constants.statusmeldingXmlRootElement) &&
                    it.melding.contains(kravstatuskode.kode)
                }
            }
        }
    }

    private fun readXml(fileName: String): String {
        return this::class.java.getResource(fileName).readText()
    }

    private fun opprettTask(xml: String, taskType: String): Task {
        return taskRepository.save(Task(type = taskType,
                                        payload = xml))
    }
}
