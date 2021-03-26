package no.nav.familie.tilbake.kravgrunnlag

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

internal class KravgrunnlagRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    private val fagsak = Testdata.fagsak
    private val kravgrunnlag431 = Testdata.kravgrunnlag431
    private val behandling = Testdata.behandling

    @BeforeEach
    fun init() {
        fagsakRepository.insert(fagsak)
        behandlingRepository.insert(behandling)
    }

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av Kravgrunnlag431 til basen`() {
        kravgrunnlagRepository.insert(kravgrunnlag431)

        val lagretKravgrunnlag431 = kravgrunnlagRepository.findByIdOrThrow(kravgrunnlag431.id)

        assertThat(lagretKravgrunnlag431).usingRecursiveComparison()
                .ignoringFields("sporbar", "perioder", "versjon")
                .isEqualTo(kravgrunnlag431)
        assertEquals(1, lagretKravgrunnlag431.versjon)
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av Kravgrunnlag431 i basen`() {
        kravgrunnlagRepository.insert(kravgrunnlag431)
        var lagretKravgrunnlag431 = kravgrunnlagRepository.findByIdOrThrow(kravgrunnlag431.id)
        val oppdatertKravgrunnlag431 = lagretKravgrunnlag431.copy(sperret = true)

        kravgrunnlagRepository.update(oppdatertKravgrunnlag431)

        lagretKravgrunnlag431 = kravgrunnlagRepository.findByIdOrThrow(kravgrunnlag431.id)
        assertThat(lagretKravgrunnlag431).usingRecursiveComparison()
                .ignoringFields("sporbar", "perioder", "versjon")
                .isEqualTo(oppdatertKravgrunnlag431)
        assertEquals(2, lagretKravgrunnlag431.versjon)
    }

    @Test
    fun `findByBehandlingId returnerer resultat når det finnes en forekomst`() {
        kravgrunnlagRepository.insert(kravgrunnlag431)

        val findByBehandlingId = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)

        assertThat(kravgrunnlag431).usingRecursiveComparison()
                .ignoringFields("sporbar", "perioder", "versjon")
                .isEqualTo(findByBehandlingId)
    }

}

