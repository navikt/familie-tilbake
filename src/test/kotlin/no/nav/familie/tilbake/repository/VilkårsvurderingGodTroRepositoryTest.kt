package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class VilkårsvurderingGodTroRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var vilkårsvurderingGodTroRepository: VilkårsvurderingGodTroRepository

    @Autowired
    private lateinit var vilkårsperiodeRepository: VilkårsperiodeRepository

    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    private val vilkårsvurderingGodTro = Testdata.vilkårsvurderingGodTro

    @BeforeEach
    fun init() {
        vilkårsvurderingRepository.insert(Testdata.vilkår)
        vilkårsperiodeRepository.insert(Testdata.vilkårsperiode)
    }

    @Test
    fun insertPersistererEnForekomstAvVilkårsvurderingGodTroTilBasen() {
        vilkårsvurderingGodTroRepository.insert(vilkårsvurderingGodTro)

        val lagretVilkårsvurderingGodTro = vilkårsvurderingGodTroRepository.findByIdOrThrow(vilkårsvurderingGodTro.id)

        Assertions.assertThat(lagretVilkårsvurderingGodTro).isEqualToIgnoringGivenFields(vilkårsvurderingGodTro, "sporbar")
    }

    @Test
    fun updateOppdatererEnForekomstAvVilkårsvurderingGodTroIBasen() {
        vilkårsvurderingGodTroRepository.insert(vilkårsvurderingGodTro)
        val oppdatertVilkårsvurderingGodTro = vilkårsvurderingGodTro.copy(begrunnelse = "bob")

        vilkårsvurderingGodTroRepository.update(oppdatertVilkårsvurderingGodTro)

        val lagretVilkårsvurderingGodTro = vilkårsvurderingGodTroRepository.findByIdOrThrow(vilkårsvurderingGodTro.id)
        Assertions.assertThat(lagretVilkårsvurderingGodTro)
                .isEqualToIgnoringGivenFields(oppdatertVilkårsvurderingGodTro, "sporbar")
    }

}