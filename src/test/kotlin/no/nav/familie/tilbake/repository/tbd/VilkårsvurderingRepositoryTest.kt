package no.nav.familie.tilbake.repository.tbd

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

internal class VilkårsvurderingRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    private val vilkår = Testdata.vilkår

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av Vilkårsvurdering til basen`() {
        vilkårsvurderingRepository.insert(vilkår)

        val lagretVilkår = vilkårsvurderingRepository.findByIdOrThrow(vilkår.id)
        assertThat(lagretVilkår).usingRecursiveComparison().ignoringFields("sporbar", "versjon").isEqualTo(vilkår)
        assertEquals(1, lagretVilkår.versjon)
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av Vilkårsvurdering i basen`() {
        vilkårsvurderingRepository.insert(vilkår)
        var lagretVilkår = vilkårsvurderingRepository.findByIdOrThrow(vilkår.id)
        val oppdatertVilkår = lagretVilkår.copy(aktiv = false)

        vilkårsvurderingRepository.update(oppdatertVilkår)

        lagretVilkår = vilkårsvurderingRepository.findByIdOrThrow(vilkår.id)
        assertThat(lagretVilkår).usingRecursiveComparison().ignoringFields("sporbar", "versjon").isEqualTo(oppdatertVilkår)
        assertEquals(2, lagretVilkår.versjon)
    }

}
