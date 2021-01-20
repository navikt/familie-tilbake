package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class VilkårsvurderingSærligGrunnRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var vilkårsvurderingSærligGrunnRepository: VilkårsvurderingSærligGrunnRepository

    @Autowired
    private lateinit var vilkårsvurderingAktsomhetRepository: VilkårsvurderingAktsomhetRepository

    @Autowired
    private lateinit var vilkårsperiodeRepository: VilkårsperiodeRepository

    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    private val vilkårsvurderingSærligGrunn = Testdata.vilkårsvurderingSærligGrunn

    @BeforeEach
    fun init() {
        vilkårsvurderingRepository.insert(Testdata.vilkår)
        vilkårsperiodeRepository.insert(Testdata.vilkårsperiode)
        vilkårsvurderingAktsomhetRepository.insert(Testdata.vilkårsvurderingAktsomhet)
    }

    @Test
    fun insertPersistererEnForekomstAvVilkårsvurderingSærligGrunnTilBasen() {
        vilkårsvurderingSærligGrunnRepository.insert(vilkårsvurderingSærligGrunn)

        val lagretVilkårsvurderingSærligGrunn =
                vilkårsvurderingSærligGrunnRepository.findByIdOrThrow(vilkårsvurderingSærligGrunn.id)

        Assertions.assertThat(lagretVilkårsvurderingSærligGrunn)
                .isEqualToIgnoringGivenFields(vilkårsvurderingSærligGrunn, "sporbar")
    }

    @Test
    fun updateOppdatererEnForekomstAvVilkårsvurderingSærligGrunnIBasen() {
        vilkårsvurderingSærligGrunnRepository.insert(vilkårsvurderingSærligGrunn)
        val oppdatertVilkårsvurderingSærligGrunn = vilkårsvurderingSærligGrunn.copy(begrunnelse = "bob")

        vilkårsvurderingSærligGrunnRepository.update(oppdatertVilkårsvurderingSærligGrunn)

        val lagretVilkårsvurderingSærligGrunn =
                vilkårsvurderingSærligGrunnRepository.findByIdOrThrow(vilkårsvurderingSærligGrunn.id)
        Assertions.assertThat(lagretVilkårsvurderingSærligGrunn)
                .isEqualToIgnoringGivenFields(oppdatertVilkårsvurderingSærligGrunn, "sporbar")
    }
}