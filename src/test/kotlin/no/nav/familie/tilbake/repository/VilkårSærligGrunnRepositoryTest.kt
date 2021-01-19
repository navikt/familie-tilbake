package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class VilkårSærligGrunnRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var vilkårSærligGrunnRepository: VilkårSærligGrunnRepository

    @Autowired
    private lateinit var vilkårAktsomhetRepository: VilkårAktsomhetRepository

    @Autowired
    private lateinit var vilkårsperiodeRepository: VilkårsperiodeRepository

    @Autowired
    private lateinit var vilkårRepository: VilkårRepository

    private val vilkårSærligGrunn = Testdata.vilkårSærligGrunn

    @BeforeEach
    fun init() {
        vilkårRepository.insert(Testdata.vilkår)
        vilkårsperiodeRepository.insert(Testdata.vilkårsperiode)
        vilkårAktsomhetRepository.insert(Testdata.vilkårAktsomhet)
    }

    @Test
    fun insertPersistererEnForekomstAvVilkårSærligGrunnTilBasen() {
        vilkårSærligGrunnRepository.insert(vilkårSærligGrunn)

        val lagretVilkårSærligGrunn = vilkårSærligGrunnRepository.findByIdOrThrow(vilkårSærligGrunn.id)

        Assertions.assertThat(lagretVilkårSærligGrunn).isEqualToIgnoringGivenFields(vilkårSærligGrunn, "sporbar")
    }

    @Test
    fun updateOppdatererEnForekomstAvVilkårSærligGrunnIBasen() {
        vilkårSærligGrunnRepository.insert(vilkårSærligGrunn)
        val oppdatertVilkårSærligGrunn = vilkårSærligGrunn.copy(begrunnelse = "bob")

        vilkårSærligGrunnRepository.update(oppdatertVilkårSærligGrunn)

        val lagretVilkårSærligGrunn = vilkårSærligGrunnRepository.findByIdOrThrow(vilkårSærligGrunn.id)
        Assertions.assertThat(lagretVilkårSærligGrunn).isEqualToIgnoringGivenFields(oppdatertVilkårSærligGrunn, "sporbar")
    }
}