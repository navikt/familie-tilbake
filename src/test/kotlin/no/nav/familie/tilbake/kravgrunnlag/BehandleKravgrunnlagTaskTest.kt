package no.nav.familie.tilbake.kravgrunnlag

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravstatuskode
import no.nav.familie.tilbake.kravgrunnlag.domain.ØkonomiXmlMottatt
import no.nav.familie.tilbake.kravgrunnlag.task.BehandleKravgrunnlagTask
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class BehandleKravgrunnlagTaskTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var taskRepository: TaskRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var mottattXmlRepository: ØkonomiXmlMottattRepository

    @Autowired
    private lateinit var mottattXmlArkivRepository: ØkonomiXmlMottattArkivRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var behandlingskontrollService: BehandlingskontrollService

    @Autowired
    private lateinit var behandleKravgrunnlagTask: BehandleKravgrunnlagTask

    private val fagsak = Testdata.fagsak
    private val behandling = Testdata.behandling

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(behandling)
    }

    @Test
    fun `doTask skal lagre mottatt kravgrunnlag i Kravgrunnlag431 når behandling finnes`() {
        lagGrunnlagssteg()
        val kravgrunnlagXml = readXml("/kravgrunnlagxml/kravgrunnlag_BA_riktig_eksternfagsakId_ytelsestype.xml")
        val task = opprettTask(kravgrunnlagXml)

        assertDoesNotThrow { behandleKravgrunnlagTask.doTask(task) }

        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
        assertNotNull(kravgrunnlag)
        assertEquals(Kravstatuskode.NYTT, kravgrunnlag.kravstatuskode)
        assertEquals(fagsak.eksternFagsakId, kravgrunnlag.fagsystemId)
        assertEquals(Ytelsestype.BARNETRYGD, KravgrunnlagUtil.tilYtelsestype(kravgrunnlag.fagområdekode.name))

        assertPerioder(kravgrunnlag)
        assertBeløp(kravgrunnlag)

        assertTrue {
            mottattXmlRepository.findByEksternKravgrunnlagIdAndVedtakId(kravgrunnlag.eksternKravgrunnlagId,
                                                                        kravgrunnlag.vedtakId).isEmpty()
        }
        assertTrue { mottattXmlArkivRepository.findAll().toList().isNotEmpty() }

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertTrue {
            behandlingsstegstilstand.any {
                Behandlingssteg.GRUNNLAG == it.behandlingssteg
                && Behandlingsstegstatus.UTFØRT == it.behandlingsstegsstatus
            }
        }
    }

    @Test
    fun `doTask skal lagre mottatt ENDR kravgrunnlag i Kravgrunnlag431 når behandling finnes`() {
        lagGrunnlagssteg()
        val kravgrunnlagXml = readXml("/kravgrunnlagxml/kravgrunnlag_BA_riktig_eksternfagsakId_ytelsestype.xml")
        behandleKravgrunnlagTask.doTask(opprettTask(kravgrunnlagXml))

        behandlingskontrollService
                .tilbakehoppBehandlingssteg(behandlingId = behandling.id,
                                            behandlingsstegsinfo =
                                            Behandlingsstegsinfo(Behandlingssteg.GRUNNLAG,
                                                                 Behandlingsstegstatus.VENTER,
                                                                 Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG,
                                                                 LocalDate.now().plusWeeks(4)))
        val endretKravgrunnlagXml = readXml("/kravgrunnlagxml/kravgrunnlag_BA_ENDR.xml")

        assertDoesNotThrow { behandleKravgrunnlagTask.doTask(opprettTask(endretKravgrunnlagXml)) }

        val alleKravgrunnlag = kravgrunnlagRepository.findByBehandlingId(behandling.id)
        assertEquals(2, alleKravgrunnlag.size)
        assertEquals(Kravstatuskode.NYTT, alleKravgrunnlag[0].kravstatuskode)
        assertFalse { alleKravgrunnlag[0].aktiv }

        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
        assertNotNull(kravgrunnlag)
        assertEquals(Kravstatuskode.ENDRET, kravgrunnlag.kravstatuskode)
        assertEquals(fagsak.eksternFagsakId, kravgrunnlag.fagsystemId)
        assertEquals(Ytelsestype.BARNETRYGD, KravgrunnlagUtil.tilYtelsestype(kravgrunnlag.fagområdekode.name))

        assertPerioder(kravgrunnlag)
        assertBeløp(kravgrunnlag)

        assertTrue {
            mottattXmlRepository.findByEksternKravgrunnlagIdAndVedtakId(kravgrunnlag.eksternKravgrunnlagId,
                                                                        kravgrunnlag.vedtakId).isEmpty()
        }
        assertTrue { mottattXmlArkivRepository.findAll().toList().isNotEmpty() }
        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertTrue {
            behandlingsstegstilstand.any {
                Behandlingssteg.GRUNNLAG == it.behandlingssteg
                && Behandlingsstegstatus.UTFØRT == it.behandlingsstegsstatus
            }
        }
    }

    @Test
    fun `doTask skal ikke lagre mottatt kravgrunnlag når mottatt xml er ugyldig`() {
        val kravgrunnlagXml = readXml("/kravgrunnlagxml/kravgrunnlag_ugyldig_struktur.xml")

        val exception = assertFailsWith<RuntimeException> { behandleKravgrunnlagTask.doTask(opprettTask(kravgrunnlagXml)) }
        assertEquals("Mottatt kravgrunnlagXML er ugyldig! Den feiler med javax.xml.bind.UnmarshalException\n" +
                     " - with linked exception:\n" +
                     "[org.xml.sax.SAXParseException; lineNumber: 21; columnNumber: 33; " +
                     "cvc-complex-type.2.4.b: The content of element 'urn:detaljertKravgrunnlag' " +
                     "is not complete. One of " +
                     "'{\"urn:no:nav:tilbakekreving:kravgrunnlag:detalj:v1\":tilbakekrevingsPeriode}'" +
                     " is expected.]", exception.message)
    }

    @Test
    fun `doTask skal ikke lagre mottatt kravgrunnlag når mottatt xml ikke har referanse`() {
        val kravgrunnlagXml = readXml("/kravgrunnlagxml/kravgrunnlag_tomt_referanse.xml")

        val exception = assertFailsWith<RuntimeException> { behandleKravgrunnlagTask.doTask(opprettTask(kravgrunnlagXml)) }
        assertEquals("Ugyldig kravgrunnlag for kravgrunnlagId 0. Mangler referanse.", exception.message)
    }

    @Test
    fun `doTask skal ikke lagre mottatt kravgrunnlag når mottatt xml periode ikke er innenfor måned`() {
        val kravgrunnlagXml = readXml("/kravgrunnlagxml/kravgrunnlag_periode_utenfor_kalendermåned.xml")

        val exception = assertFailsWith<RuntimeException> { behandleKravgrunnlagTask.doTask(opprettTask(kravgrunnlagXml)) }
        assertEquals("Ugyldig kravgrunnlag for kravgrunnlagId 0. " +
                     "Perioden 2020-08-01-2020-09-30 er ikke innenfor en kalendermåned.",
                     exception.message)
    }

    @Test
    fun `doTask skal ikke lagre mottatt kravgrunnlag når mottatt xml periode ikke starter første dag i måned`() {
        val kravgrunnlagXml = readXml("/kravgrunnlagxml/kravgrunnlag_periode_starter_ikke_første_dag.xml")

        val exception = assertFailsWith<RuntimeException> { behandleKravgrunnlagTask.doTask(opprettTask(kravgrunnlagXml)) }
        assertEquals("Ugyldig kravgrunnlag for kravgrunnlagId 0. " +
                     "Perioden 2020-08-15-2020-08-31 starter ikke første dag i måned.",
                     exception.message)
    }

    @Test
    fun `doTask skal ikke lagre mottatt kravgrunnlag når mottatt xml periode ikke slutter siste dag i måned`() {
        val kravgrunnlagXml = readXml("/kravgrunnlagxml/kravgrunnlag_periode_slutter_ikke_siste_dag.xml")

        val exception = assertFailsWith<RuntimeException> { behandleKravgrunnlagTask.doTask(opprettTask(kravgrunnlagXml)) }
        assertEquals("Ugyldig kravgrunnlag for kravgrunnlagId 0. " +
                     "Perioden 2020-08-01-2020-08-28 slutter ikke siste dag i måned.",
                     exception.message)
    }

    @Test
    fun `doTask skal ikke lagre mottatt kravgrunnlag når mottatt xml mangler FEIL postering`() {
        val kravgrunnlagXml = readXml("/kravgrunnlagxml/kravgrunnlag_uten_FEIL_postering.xml")

        val exception = assertFailsWith<RuntimeException> { behandleKravgrunnlagTask.doTask(opprettTask(kravgrunnlagXml)) }
        assertEquals("Ugyldig kravgrunnlag for kravgrunnlagId 0. " +
                     "Perioden 2020-08-01-2020-08-31 mangler postering med klasseType=FEIL.",
                     exception.message)
    }

    @Test
    fun `doTask skal ikke lagre mottatt kravgrunnlag når mottatt xml mangler YTEL postering`() {
        val kravgrunnlagXml = readXml("/kravgrunnlagxml/kravgrunnlag_uten_YTEL_postering.xml")

        val exception = assertFailsWith<RuntimeException> { behandleKravgrunnlagTask.doTask(opprettTask(kravgrunnlagXml)) }
        assertEquals("Ugyldig kravgrunnlag for kravgrunnlagId 0. " +
                     "Perioden 2020-08-01-2020-08-31 mangler postering med klasseType=YTEL.",
                     exception.message)
    }

    @Test
    fun `doTask skal ikke lagre mottatt kravgrunnlag når mottatt xml har overlappende perioder`() {
        val kravgrunnlagXml = readXml("/kravgrunnlagxml/kravgrunnlag_overlappende_perioder.xml")

        val exception = assertFailsWith<RuntimeException> { behandleKravgrunnlagTask.doTask(opprettTask(kravgrunnlagXml)) }
        assertEquals("Ugyldig kravgrunnlag for kravgrunnlagId 0. " +
                     "Overlappende perioder Periode(fom=2020-08, tom=2020-08) og Periode(fom=2020-08, tom=2020-08).",
                     exception.message)
    }

    @Test
    fun `doTask skal ikke lagre mottatt kravgrunnlag når xml har posteringsskatt som ikke matcher månedlig skatt beløp`() {
        val kravgrunnlagXml = readXml("/kravgrunnlagxml/kravgrunnlag_posteringsskatt_matcher_ikke_med_månedlig_skatt_beløp.xml")

        val exception = assertFailsWith<RuntimeException> { behandleKravgrunnlagTask.doTask(opprettTask(kravgrunnlagXml)) }
        assertEquals("Ugyldig kravgrunnlag for kravgrunnlagId 0. " +
                     "For måned 2020-08 er maks skatt 0.00, men maks tilbakekreving ganget med skattesats blir 210",
                     exception.message)
    }

    @Test
    fun `doTask skal ikke lagre mottatt kravgrunnlag når mottatt xml har FEIL postering med negativt beløp`() {
        val kravgrunnlagXml = readXml("/kravgrunnlagxml/kravgrunnlag_FEIL_postering_med_negativ_beløp.xml")

        val exception = assertFailsWith<RuntimeException> { behandleKravgrunnlagTask.doTask(opprettTask(kravgrunnlagXml)) }
        assertEquals("Ugyldig kravgrunnlag for kravgrunnlagId 0. " +
                     "Perioden 2020-08-01-2020-08-31 har FEIL postering med negativ beløp",
                     exception.message)
    }

    @Test
    fun `doTask skal ikke lagre mottatt kravgrunnlag når mottatt xml har ulike total tilbakekrevesbeløp og total nybeløp`() {
        val kravgrunnlagXml = readXml("/kravgrunnlagxml/kravgrunnlag_ulike_total_tilbakekrevesbeløp_total_nybeløp.xml")

        val exception = assertFailsWith<RuntimeException> { behandleKravgrunnlagTask.doTask(opprettTask(kravgrunnlagXml)) }
        assertEquals("Ugyldig kravgrunnlag for kravgrunnlagId 0. " +
                     "For perioden 2020-08-01-2020-08-31 " +
                     "total tilkakekrevesBeløp i YTEL posteringer er 1500.00," +
                     "mens total nytt beløp i FEIL posteringer er 2108.00. " +
                     "Det er forventet at disse er like.",
                     exception.message)
    }

    @Test
    fun `doTask skal ikke lagre mottatt kravgrunnlag når mottatt xml har YTEL postering som ikke matcher beregning`() {
        val kravgrunnlagXml = readXml("/kravgrunnlagxml/" +
                                      "kravgrunnlag_YTEL_postering_som_ikke_matcher_beregning.xml")

        val exception = assertFailsWith<RuntimeException> { behandleKravgrunnlagTask.doTask(opprettTask(kravgrunnlagXml)) }
        assertEquals("Ugyldig kravgrunnlag for kravgrunnlagId 0. " +
                     "For perioden 2020-08-01-2020-08-31 " +
                     "finnes YTEL-postering med tilbakekrevesBeløp 2108.00 " +
                     "som er større enn differanse mellom nyttBeløp 0.00 " +
                     "og opprinneligBeløp 1000.00",
                     exception.message)
    }

    @Test
    fun `doTask skal lagre mottatt kravgrunnlag i oko xml mottatt når behandling ikke finnes`() {
        val behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(behandling.copy(status = Behandlingsstatus.AVSLUTTET))

        val kravgrunnlagXml = readXml("/kravgrunnlagxml/kravgrunnlag_BA_riktig_eksternfagsakId_ytelsestype.xml")
        val task = opprettTask(kravgrunnlagXml)

        assertDoesNotThrow { behandleKravgrunnlagTask.doTask(task) }
        assertFalse { kravgrunnlagRepository.existsByBehandlingIdAndAktivTrueAndSperretFalse(behandling.id) }

        val mottattKravgrunnlagListe = mottattXmlRepository.findByEksternKravgrunnlagIdAndVedtakId(BigInteger.ZERO,
                                                                                                   BigInteger.ZERO)
        assertOkoXmlMottattData(mottattKravgrunnlagListe, kravgrunnlagXml, Kravstatuskode.NYTT)

        assertTrue { mottattXmlArkivRepository.findAll().toList().isEmpty() }
    }

    @Test
    fun `doTask skal lagre mottatt ENDR kravgrunnlag i oko xml mottatt når tabellen allerede har NYTT kravgrunnlag`() {
        val behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(behandling.copy(status = Behandlingsstatus.AVSLUTTET))

        val kravgrunnlagXml = readXml("/kravgrunnlagxml/kravgrunnlag_BA_riktig_eksternfagsakId_ytelsestype.xml")
        behandleKravgrunnlagTask.doTask(opprettTask(kravgrunnlagXml))
        val endretKravgrunnlagXml = readXml("/kravgrunnlagxml/kravgrunnlag_BA_ENDR.xml")

        assertDoesNotThrow { behandleKravgrunnlagTask.doTask(opprettTask(endretKravgrunnlagXml)) }

        val mottattKravgrunnlagListe = mottattXmlRepository.findByEksternKravgrunnlagIdAndVedtakId(BigInteger.ZERO,
                                                                                                   BigInteger.ZERO)
        assertOkoXmlMottattData(mottattKravgrunnlagListe,
                                endretKravgrunnlagXml, Kravstatuskode.ENDRET)

        assertTrue { mottattXmlArkivRepository.findAll().toList().isNotEmpty() }
    }

    private fun readXml(fileName: String): String {
        return this::class.java.getResource(fileName).readText()
    }

    private fun opprettTask(kravgrunnlagXml: String): Task {
        return taskRepository.save(Task(type = BehandleKravgrunnlagTask.TYPE,
                                        payload = kravgrunnlagXml))
    }

    private fun assertOkoXmlMottattData(mottattKravgrunnlagListe: List<ØkonomiXmlMottatt>,
                                        kravgrunnlagXml: String,
                                        kravstatuskode: Kravstatuskode) {
        assertTrue { mottattKravgrunnlagListe.isNotEmpty() }
        assertEquals(1, mottattKravgrunnlagListe.size)
        val mottattKravgrunnlag = mottattKravgrunnlagListe[0]
        assertEquals(kravstatuskode, mottattKravgrunnlag.kravstatuskode)
        assertEquals(fagsak.eksternFagsakId, mottattKravgrunnlag.eksternFagsakId)
        assertEquals("0", mottattKravgrunnlag.referanse)
        assertEquals("2021-03-02-18.50.15.236315", mottattKravgrunnlag.kontrollfelt)
        assertEquals(kravgrunnlagXml, mottattKravgrunnlag.melding)
        assertEquals(BigInteger.ZERO, mottattKravgrunnlag.eksternKravgrunnlagId)
        assertEquals(BigInteger.ZERO, mottattKravgrunnlag.vedtakId)
        assertEquals(Ytelsestype.BARNETRYGD, mottattKravgrunnlag.ytelsestype)
    }

    private fun assertPerioder(kravgrunnlag: Kravgrunnlag431) {
        val perioder = kravgrunnlag.perioder
        assertNotNull(perioder)
        assertEquals(1, perioder.size)
        assertTrue { perioder.toList()[0].månedligSkattebeløp == BigDecimal("0.00") }
    }

    private fun assertBeløp(kravgrunnlag: Kravgrunnlag431) {
        val kravgrunnlagsbeløp = kravgrunnlag.perioder.toList()[0].beløp
        assertEquals(2, kravgrunnlagsbeløp.size)
        assertTrue { kravgrunnlagsbeløp.any { Klassetype.YTEL == it.klassetype } }
        assertTrue { kravgrunnlagsbeløp.any { Klassetype.FEIL == it.klassetype } }
    }

    private fun lagGrunnlagssteg() {
        behandlingsstegstilstandRepository.insert(
                Behandlingsstegstilstand(behandlingId = behandling.id,
                                         behandlingssteg = Behandlingssteg.GRUNNLAG,
                                         behandlingsstegsstatus = Behandlingsstegstatus.VENTER,
                                         venteårsak = Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG,
                                         tidsfrist = LocalDate.now().plusWeeks(4)))
    }

}
