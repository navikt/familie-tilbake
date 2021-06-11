package no.nav.familie.tilbake.iverksettvedtak

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import kotlin.test.assertEquals

internal class ØkonomiXmlSendtRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var økonomiXmlSendtRepository: ØkonomiXmlSendtRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private val økonomiXmlSendt = Testdata.økonomiXmlSendt

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(Testdata.behandling)
    }

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av ØkonomiXmlSendt til basen`() {
        økonomiXmlSendtRepository.insert(økonomiXmlSendt)

        val lagretØkonomiXmlSendt = økonomiXmlSendtRepository.findByIdOrThrow(økonomiXmlSendt.id)

        assertThat(lagretØkonomiXmlSendt).usingRecursiveComparison()
                .ignoringFields("sporbar", "versjon")
                .isEqualTo(økonomiXmlSendt)
        assertEquals(1, lagretØkonomiXmlSendt.versjon)
    }


    @Test
    fun `findByMeldingstypeAndSporbarOpprettetTidAfter skal finne forekomster hvis det finnes for søkekriterier`() {
        økonomiXmlSendtRepository.insert(økonomiXmlSendt)

        val lagretØkonomiXmlSendt =
                økonomiXmlSendtRepository.findByOpprettetPåDato(LocalDate.now())


        assertThat(lagretØkonomiXmlSendt).isNotEmpty
    }

    @Test
    fun `findByMeldingstypeAndSporbarOpprettetTidAfter skal ikke finne forekomster hvis det ikke finnes for søkekriterier`() {
        økonomiXmlSendtRepository.insert(økonomiXmlSendt)

        val lagretØkonomiXmlSendt =
                økonomiXmlSendtRepository.findByOpprettetPåDato(LocalDate.now().plusDays(1))


        assertThat(lagretØkonomiXmlSendt).isEmpty()
    }


    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av ØkonomiXmlSendt i basen`() {
        økonomiXmlSendtRepository.insert(økonomiXmlSendt)
        var lagretØkonomiXmlSendt = økonomiXmlSendtRepository.findByIdOrThrow(økonomiXmlSendt.id)
        val oppdatertØkonomiXmlSendt = lagretØkonomiXmlSendt.copy(melding = "bob")

        økonomiXmlSendtRepository.update(oppdatertØkonomiXmlSendt)

        lagretØkonomiXmlSendt = økonomiXmlSendtRepository.findByIdOrThrow(økonomiXmlSendt.id)
        assertThat(lagretØkonomiXmlSendt).usingRecursiveComparison()
                .ignoringFields("sporbar", "versjon")
                .isEqualTo(oppdatertØkonomiXmlSendt)
        assertEquals(2, lagretØkonomiXmlSendt.versjon)
    }

}
