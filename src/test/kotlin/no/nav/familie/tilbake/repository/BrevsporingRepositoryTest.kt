package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.domain.Brevtype
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class BrevsporingRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var brevsporingRepository: BrevsporingRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private val brevsporing = Testdata.brevsporing

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(Testdata.behandling)
    }

    @Test
    fun insertPersistererEnForekomstAvBrevsporingTilBasen() {
        brevsporingRepository.insert(brevsporing)

        val lagretBrevsporing = brevsporingRepository.findByIdOrThrow(brevsporing.id)

        Assertions.assertThat(lagretBrevsporing).isEqualToIgnoringGivenFields(brevsporing, "sporbar")
    }

    @Test
    fun updateOppdatererEnForekomstAvBrevsporingIBasen() {
        brevsporingRepository.insert(brevsporing)
        val oppdatertBrevsporing = brevsporing.copy(brevtype = Brevtype.HENLEGGELSE_BREV)

        brevsporingRepository.update(oppdatertBrevsporing)

        val lagretBrevsporing = brevsporingRepository.findByIdOrThrow(brevsporing.id)
        Assertions.assertThat(lagretBrevsporing).isEqualToIgnoringGivenFields(oppdatertBrevsporing, "sporbar")
    }

}