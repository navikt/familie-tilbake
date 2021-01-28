package no.nav.familie.tilbake.behandling

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.domain.Fagsaksstatus
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class FagsakRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private val fagsak = Testdata.fagsak

    @Test
    fun `skal persistere en fagsak`() {
        fagsakRepository.insert(fagsak)

        val lagretFagsak = fagsakRepository.findByIdOrThrow(fagsak.id)
        Assertions.assertThat(lagretFagsak).isEqualToIgnoringGivenFields(fagsak, "sporbar")
    }

    @Test
    fun `skal oppdatere fagsak status`() {
        fagsakRepository.insert(fagsak)
        val oppdatertFagsak = fagsak.copy(status = Fagsaksstatus.UNDER_BEHANDLING)

        fagsakRepository.update(oppdatertFagsak)

        val lagretFagsak = fagsakRepository.findByIdOrThrow(fagsak.id)
        Assertions.assertThat(lagretFagsak).isEqualToIgnoringGivenFields(oppdatertFagsak, "sporbar")
    }

}
