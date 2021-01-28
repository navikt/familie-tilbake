package no.nav.familie.tilbake.domain

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.repository.tbd.VilkårsvurderingRepository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import no.nav.familie.tilbake.common.repository.findByIdOrThrow

internal class VilkårsvurderingRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    private val vilkår = Testdata.vilkår

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av Vilkårsvurdering til basen`() {
        vilkårsvurderingRepository.insert(vilkår)

        val lagretVilkår = vilkårsvurderingRepository.findByIdOrThrow(vilkår.id)

        Assertions.assertThat(lagretVilkår).isEqualToIgnoringGivenFields(vilkår, "sporbar")
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av Vilkårsvurdering i basen`() {
        vilkårsvurderingRepository.insert(vilkår)
        val oppdatertVilkår = vilkår.copy(aktiv = false)

        vilkårsvurderingRepository.update(oppdatertVilkår)

        val lagretVilkår = vilkårsvurderingRepository.findByIdOrThrow(vilkår.id)
        Assertions.assertThat(lagretVilkår).isEqualToIgnoringGivenFields(oppdatertVilkår, "sporbar")
    }

}