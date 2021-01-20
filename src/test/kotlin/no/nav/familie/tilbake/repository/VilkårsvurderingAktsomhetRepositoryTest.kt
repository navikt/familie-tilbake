package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class VilkårsvurderingAktsomhetRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var vilkårsvurderingAktsomhetRepository: VilkårsvurderingAktsomhetRepository

    @Autowired
    private lateinit var vilkårsperiodeRepository: VilkårsperiodeRepository

    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    private val vilkårsvurderingAktsomhet = Testdata.vilkårsvurderingAktsomhet

    @BeforeEach
    fun init() {
        vilkårsvurderingRepository.insert(Testdata.vilkår)
        vilkårsperiodeRepository.insert(Testdata.vilkårsperiode)
    }

    @Test
    fun insertPersistererEnForekomstAvVilkårsvurderingAktsomhetTilBasen() {
        vilkårsvurderingAktsomhetRepository.insert(vilkårsvurderingAktsomhet)

        val lagretVilkårsvurderingAktsomhet = vilkårsvurderingAktsomhetRepository.findByIdOrThrow(vilkårsvurderingAktsomhet.id)

        Assertions.assertThat(lagretVilkårsvurderingAktsomhet).isEqualToIgnoringGivenFields(vilkårsvurderingAktsomhet, "sporbar")
    }

    @Test
    fun updateOppdatererEnForekomstAvVilkårsvurderingAktsomhetIBasen() {
        vilkårsvurderingAktsomhetRepository.insert(vilkårsvurderingAktsomhet)
        val oppdatertVilkårsvurderingAktsomhet = vilkårsvurderingAktsomhet.copy(begrunnelse = "bob")

        vilkårsvurderingAktsomhetRepository.update(oppdatertVilkårsvurderingAktsomhet)

        val lagretVilkårsvurderingAktsomhet = vilkårsvurderingAktsomhetRepository.findByIdOrThrow(vilkårsvurderingAktsomhet.id)
        Assertions.assertThat(lagretVilkårsvurderingAktsomhet)
                .isEqualToIgnoringGivenFields(oppdatertVilkårsvurderingAktsomhet, "sporbar")
    }

}