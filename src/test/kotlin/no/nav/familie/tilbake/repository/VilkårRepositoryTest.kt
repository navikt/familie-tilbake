package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class VilkårRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var vilkårRepository: VilkårRepository

    private val vilkår = Testdata.vilkår

    @Test
    fun insertPersistererEnForekomstAvVilkårTilBasen() {
        vilkårRepository.insert(vilkår)

        val lagretVilkår = vilkårRepository.findByIdOrThrow(vilkår.id)

        Assertions.assertThat(lagretVilkår).isEqualToIgnoringGivenFields(vilkår, "sporbar")
    }

    @Test
    fun updateOppdatererEnForekomstAvVilkårIBasen() {
        vilkårRepository.insert(vilkår)
        val oppdatertVilkår = vilkår.copy(aktiv = false)

        vilkårRepository.update(oppdatertVilkår)

        val lagretVilkår = vilkårRepository.findByIdOrThrow(vilkår.id)
        Assertions.assertThat(lagretVilkår).isEqualToIgnoringGivenFields(oppdatertVilkår, "sporbar")
    }

}