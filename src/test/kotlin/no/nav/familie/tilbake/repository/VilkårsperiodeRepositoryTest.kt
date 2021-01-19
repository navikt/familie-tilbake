package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class VilkårsperiodeRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var vilkårsperiodeRepository: VilkårsperiodeRepository

    @Autowired
    private lateinit var vilkårRepository: VilkårRepository

    private val vilkårsperiode = Testdata.vilkårsperiode

    @BeforeEach
    fun init() {
        vilkårRepository.insert(Testdata.vilkår)
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
        val oppdatertVilkårsperiode = vilkårsperiode.copy(fulgtOppNav = "bob")

        vilkårsperiodeRepository.update(oppdatertVilkårsperiode)

        val lagretVilkårsperiode = vilkårsperiodeRepository.findByIdOrThrow(vilkårsperiode.id)
        Assertions.assertThat(lagretVilkårsperiode).isEqualToIgnoringGivenFields(oppdatertVilkårsperiode, "sporbar")
    }

}