package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class VilkårsvurderingRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    private val vilkår = Testdata.vilkår

    @Test
    fun insertPersistererEnForekomstAvVilkårsvurderingTilBasen() {
        vilkårsvurderingRepository.insert(vilkår)

        val lagretVilkår = vilkårsvurderingRepository.findByIdOrThrow(vilkår.id)

        Assertions.assertThat(lagretVilkår).isEqualToIgnoringGivenFields(vilkår, "sporbar")
    }

    @Test
    fun updateOppdatererEnForekomstAvVilkårsvurderingIBasen() {
        vilkårsvurderingRepository.insert(vilkår)
        val oppdatertVilkår = vilkår.copy(aktiv = false)

        vilkårsvurderingRepository.update(oppdatertVilkår)

        val lagretVilkår = vilkårsvurderingRepository.findByIdOrThrow(vilkår.id)
        Assertions.assertThat(lagretVilkår).isEqualToIgnoringGivenFields(oppdatertVilkår, "sporbar")
    }

}