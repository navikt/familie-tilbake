package no.nav.familie.tilbake.repository.tbd

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class GrupperingKravGrunnlagRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var grupperingKravGrunnlagRepository: GrupperingKravGrunnlagRepository

    @Autowired
    private lateinit var kravgrunnlag431Repository: Kravgrunnlag431Repository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private val grupperingKravGrunnlag = Testdata.grupperingKravGrunnlag

    @BeforeEach
    fun init() {
        kravgrunnlag431Repository.insert(Testdata.kravgrunnlag431)
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(Testdata.behandling)
    }

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av GrupperingKravGrunnlag til basen`() {
        grupperingKravGrunnlagRepository.insert(grupperingKravGrunnlag)

        val lagretGrupperingKravGrunnlag = grupperingKravGrunnlagRepository.findByIdOrThrow(grupperingKravGrunnlag.id)

        Assertions.assertThat(lagretGrupperingKravGrunnlag).isEqualToIgnoringGivenFields(grupperingKravGrunnlag, "sporbar")
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av GrupperingKravGrunnlag i basen`() {
        grupperingKravGrunnlagRepository.insert(grupperingKravGrunnlag)
        val oppdatertGrupperingKravGrunnlag = grupperingKravGrunnlag.copy(sperret = true)

        grupperingKravGrunnlagRepository.update(oppdatertGrupperingKravGrunnlag)

        val lagretGrupperingKravGrunnlag = grupperingKravGrunnlagRepository.findByIdOrThrow(grupperingKravGrunnlag.id)
        Assertions.assertThat(lagretGrupperingKravGrunnlag)
                .isEqualToIgnoringGivenFields(oppdatertGrupperingKravGrunnlag, "sporbar")
    }

}
