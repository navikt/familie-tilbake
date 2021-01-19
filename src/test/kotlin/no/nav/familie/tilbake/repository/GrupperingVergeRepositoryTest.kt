package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class GrupperingVergeRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var grupperingVergeRepository: GrupperingVergeRepository

    @Autowired
    private lateinit var vergeRepository: VergeRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private val grupperingVerge = Testdata.grupperingVerge

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(Testdata.behandling)
        vergeRepository.insert(Testdata.verge)
    }

    @Test
    fun insertPersistererEnForekomstAvGrupperingVergeTilBasen() {
        grupperingVergeRepository.insert(grupperingVerge)

        val lagretGrupperingVerge = grupperingVergeRepository.findByIdOrThrow(grupperingVerge.id)

        assertThat(lagretGrupperingVerge).isEqualToIgnoringGivenFields(grupperingVerge, "sporbar")
    }

    @Test
    fun updateOppdatererEnForekomstAvGrupperingVergeIBasen() {
        grupperingVergeRepository.insert(grupperingVerge)
        val oppdatertGrupperingVerge = grupperingVerge.copy(aktiv = false)

        grupperingVergeRepository.update(oppdatertGrupperingVerge)

        val lagretGrupperingVerge = grupperingVergeRepository.findByIdOrThrow(grupperingVerge.id)
        assertThat(lagretGrupperingVerge).isEqualToIgnoringGivenFields(oppdatertGrupperingVerge, "sporbar")
    }

}

