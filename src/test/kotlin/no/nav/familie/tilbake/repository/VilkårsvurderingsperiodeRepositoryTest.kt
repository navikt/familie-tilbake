package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.domain.Navoppfulgt
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class VilkårsvurderingsperiodeRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var vilkårsperiodeRepository: VilkårsperiodeRepository

    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    private val vilkårsperiode = Testdata.vilkårsperiode

    @BeforeEach
    fun init() {
        vilkårsvurderingRepository.insert(Testdata.vilkår)
    }

    @Test
    fun insertPersistererEnForekomstAvVilkårsperiodeTilBasen() {
        vilkårsperiodeRepository.insert(vilkårsperiode)

        val lagretVilkårsperiode = vilkårsperiodeRepository.findByIdOrThrow(vilkårsperiode.id)

        Assertions.assertThat(lagretVilkårsperiode).isEqualToIgnoringGivenFields(vilkårsperiode, "sporbar")
    }

    @Test
    fun updateOppdatererEnForekomstAvVilkårsperiodeIBasen() {
        vilkårsperiodeRepository.insert(vilkårsperiode)
        val oppdatertVilkårsperiode = vilkårsperiode.copy(navoppfulgt = Navoppfulgt.BEREGNINGSFEIL)

        vilkårsperiodeRepository.update(oppdatertVilkårsperiode)

        val lagretVilkårsperiode = vilkårsperiodeRepository.findByIdOrThrow(vilkårsperiode.id)
        Assertions.assertThat(lagretVilkårsperiode).isEqualToIgnoringGivenFields(oppdatertVilkårsperiode, "sporbar")
    }

}