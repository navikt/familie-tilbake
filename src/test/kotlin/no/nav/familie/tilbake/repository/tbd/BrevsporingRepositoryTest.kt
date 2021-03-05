package no.nav.familie.tilbake.repository.tbd

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.domain.tbd.Brevtype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

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
    fun `insert med gyldige verdier skal persistere en forekomst av Brevsporing til basen`() {
        brevsporingRepository.insert(brevsporing)

        val lagretBrevsporing = brevsporingRepository.findByIdOrThrow(brevsporing.id)

        assertThat(lagretBrevsporing).isEqualToIgnoringGivenFields(brevsporing, "sporbar", "versjon")
        assertEquals(1, lagretBrevsporing.versjon)
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av Brevsporing i basen`() {
        brevsporingRepository.insert(brevsporing)
        var lagretBrevsporing = brevsporingRepository.findByIdOrThrow(brevsporing.id)
        val oppdatertBrevsporing = lagretBrevsporing.copy(brevtype = Brevtype.HENLEGGELSE)

        brevsporingRepository.update(oppdatertBrevsporing)

        lagretBrevsporing = brevsporingRepository.findByIdOrThrow(brevsporing.id)
        assertThat(lagretBrevsporing).isEqualToIgnoringGivenFields(oppdatertBrevsporing, "sporbar", "versjon")
        assertEquals(2, lagretBrevsporing.versjon)
    }

}
