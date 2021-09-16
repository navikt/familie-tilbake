package no.nav.familie.tilbake.forvaltning

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravstatuskode
import no.nav.familie.tilbake.kravgrunnlag.domain.ØkonomiXmlMottatt
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattArkivRepository
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigInteger
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
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
    private lateinit var forvaltningService: ForvaltningService

    private val behandling = Testdata.behandling

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(behandling)
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
    }

    @Test
    fun `korrigerKravgrunnlag skal hente korrigert kravgrunnlag når behandling ikke har et kravgrunnlag`() {
        lagMottattXml()
        assertDoesNotThrow { forvaltningService.korrigerKravgrunnlag(behandling.id, BigInteger.ZERO) }

        val kravgrunnlagene = kravgrunnlagRepository.findByBehandlingId(behandling.id)
        assertEquals(1, kravgrunnlagene.size)
        assertTrue { kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(behandling.id) }
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

    private fun lagMottattXml(): ØkonomiXmlMottatt {
        val mottattXml = readXml("/kravgrunnlagxml/kravgrunnlag_BA_riktig_eksternfagsakId_ytelsestype.xml")
        val økonomiXmlMottatt = økonomiXmlMottattRepository.insert(ØkonomiXmlMottatt(melding = mottattXml,
                                                                                     kravstatuskode = Kravstatuskode.NYTT,
                                                                                     eksternFagsakId = "0",
                                                                                     ytelsestype = Ytelsestype.BARNETRYGD,
                                                                                     referanse = "0",
                                                                                     eksternKravgrunnlagId = BigInteger.ZERO,
                                                                                     vedtakId = BigInteger.ZERO,
                                                                                     kontrollfelt = "2021-03-02-18.50.15.236315",
                                                                                     sperret = false))
        return økonomiXmlMottatt
    }
}