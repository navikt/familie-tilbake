package no.nav.familie.tilbake.kravgrunnlag

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Faktainfo
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingService
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravstatuskode
import no.nav.familie.tilbake.kravgrunnlag.domain.ØkonomiXmlMottatt
import no.nav.familie.tilbake.kravgrunnlag.task.FinnKravgrunnlagTask
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigInteger
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class FinnKravgrunnlagTaskTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var økonomiXmlMottattRepository: ØkonomiXmlMottattRepository

    @Autowired
    private lateinit var økonomiXmlMottattArkivRepository: ØkonomiXmlMottattArkivRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var finnKravgrunnlagTask: FinnKravgrunnlagTask

    private lateinit var behandling: Behandling
    private lateinit var behandlingId: UUID

    private val eksternFagsakId = "testverdi"

    @BeforeEach
    fun init() {
        behandling = opprettBehandling()
        behandlingId = behandling.id
    }

    @Test
    fun `doTask skal finne og koble grunnlag med behandling`() {
        val kravgrunnlagXml = readXml("/kravgrunnlagxml/kravgrunnlag_BA_riktig_eksternfagsakId_ytelsestype.xml")
        lagreMottattKravgrunnlag(kravgrunnlagXml)

        finnKravgrunnlagTask.doTask(Task(type = FinnKravgrunnlagTask.TYPE, payload = behandlingId.toString()))

        val arkivXmlene = økonomiXmlMottattArkivRepository.findByEksternFagsakIdAndYtelsestype(eksternFagsakId,
                                                                                               Ytelsestype.BARNETRYGD)
        assertTrue { arkivXmlene.isNotEmpty() }

        assertTrue { kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(behandlingId) }

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)
    }

    @Test
    fun `doTask skal finne og koble grunnlag med behandling når grunnlag er sperret`() {
        val kravgrunnlagXml = readXml("/kravgrunnlagxml/kravgrunnlag_BA_riktig_eksternfagsakId_ytelsestype.xml")
        lagreMottattKravgrunnlag(kravgrunnlagXml, true)

        finnKravgrunnlagTask.doTask(Task(type = FinnKravgrunnlagTask.TYPE, payload = behandlingId.toString()))

        val arkivXmlene = økonomiXmlMottattArkivRepository.findByEksternFagsakIdAndYtelsestype(eksternFagsakId,
                                                                                               Ytelsestype.BARNETRYGD)
        assertTrue { arkivXmlene.isNotEmpty() }

        assertTrue { kravgrunnlagRepository.existsByBehandlingIdAndAktivTrueAndSperretTrue(behandlingId) }

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.VENTER)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.AVBRUTT)
    }

    @Test
    fun `doTask skal finne og koble grunnlag med behandling når det finnes et NY og et ENDR grunnlag`() {
        val kravgrunnlagXml = readXml("/kravgrunnlagxml/kravgrunnlag_BA_riktig_eksternfagsakId_ytelsestype.xml")
        lagreMottattKravgrunnlag(kravgrunnlagXml, true)

        val endretKravgrunnlagXml = readXml("/kravgrunnlagxml/kravgrunnlag_BA_ENDR.xml")
        lagreMottattKravgrunnlag(endretKravgrunnlagXml)

        finnKravgrunnlagTask.doTask(Task(type = FinnKravgrunnlagTask.TYPE, payload = behandlingId.toString()))

        val arkivXmlene = økonomiXmlMottattArkivRepository.findByEksternFagsakIdAndYtelsestype(eksternFagsakId,
                                                                                               Ytelsestype.BARNETRYGD)
        assertTrue { arkivXmlene.isNotEmpty() }
        assertEquals(2, arkivXmlene.size)

        assertTrue { kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(behandlingId) }
        val kravgrunnlagene = kravgrunnlagRepository.findByBehandlingId(behandlingId)
        assertTrue { kravgrunnlagene.any { it.aktiv && it.kravstatuskode == Kravstatuskode.ENDRET } }
        assertTrue { kravgrunnlagene.any { !it.aktiv && it.sperret && it.kravstatuskode == Kravstatuskode.NYTT } }

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertBehandlingsstegstilstand(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)
    }

    private fun opprettBehandling(): Behandling {
        val faktainfo = Faktainfo(revurderingsårsak = "testverdi",
                                  revurderingsresultat = "testresultat",
                                  tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL)

        val request = OpprettTilbakekrevingRequest(ytelsestype = Ytelsestype.BARNETRYGD,
                                                   fagsystem = Fagsystem.BA,
                                                   eksternFagsakId = eksternFagsakId,
                                                   personIdent = "321321322",
                                                   eksternId = "0",
                                                   manueltOpprettet = false,
                                                   språkkode = Språkkode.NB,
                                                   enhetId = "8020",
                                                   enhetsnavn = "Oslo",
                                                   varsel = null,
                                                   revurderingsvedtaksdato = LocalDate.now(),
                                                   faktainfo = faktainfo,
                                                   saksbehandlerIdent = "Z0000")
        return behandlingService.opprettBehandlingAutomatisk(request)
    }

    private fun readXml(fileName: String): String {
        val url = requireNotNull(this::class.java.getResource(fileName)) { "fil med filnavn=$fileName finnes ikke" }
        return url.readText()
    }

    private fun lagreMottattKravgrunnlag(kravgrunnlagXml: String,
                                         sperret: Boolean = false) {
        økonomiXmlMottattRepository.insert(ØkonomiXmlMottatt(melding = kravgrunnlagXml,
                                                             kravstatuskode = Kravstatuskode.NYTT,
                                                             eksternFagsakId = eksternFagsakId,
                                                             ytelsestype = Ytelsestype.BARNETRYGD,
                                                             referanse = "0",
                                                             eksternKravgrunnlagId = BigInteger.ZERO,
                                                             vedtakId = BigInteger.ZERO,
                                                             kontrollfelt = "2021-03-02-18.50.15.236315",
                                                             sperret = sperret))
    }

    private fun assertBehandlingsstegstilstand(behandlingsstegstilstand: List<Behandlingsstegstilstand>,
                                               behandlingssteg: Behandlingssteg,
                                               behandlingsstegstatus: Behandlingsstegstatus) {
        assertTrue {
            behandlingsstegstilstand.any {
                it.behandlingssteg == behandlingssteg &&
                it.behandlingsstegsstatus == behandlingsstegstatus
            }
        }
    }
}
