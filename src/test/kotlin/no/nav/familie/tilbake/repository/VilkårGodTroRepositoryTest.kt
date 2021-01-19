package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class VilkårGodTroRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var vilkårGodTroRepository: VilkårGodTroRepository

    @Autowired
    private lateinit var vilkårsperiodeRepository: VilkårsperiodeRepository

    @Autowired
    private lateinit var vilkårRepository: VilkårRepository

    private val vilkårGodTro = Testdata.vilkårGodTro

    @BeforeEach
    fun init() {
        vilkårRepository.insert(Testdata.vilkår)
        vilkårsperiodeRepository.insert(Testdata.vilkårsperiode)
    }

    @Test
    fun insertPersistererEnForekomstAvVilkårGodTroTilBasen() {
        vilkårGodTroRepository.insert(vilkårGodTro)

        val lagretVilkårGodTro = vilkårGodTroRepository.findByIdOrThrow(vilkårGodTro.id)

        Assertions.assertThat(lagretVilkårGodTro).isEqualToIgnoringGivenFields(vilkårGodTro, "sporbar")
    }

    @Test
    fun updateOppdatererEnForekomstAvVilkårGodTroIBasen() {
        vilkårGodTroRepository.insert(vilkårGodTro)
        val oppdatertVilkårGodTro = vilkårGodTro.copy(begrunnelse = "bob")

        vilkårGodTroRepository.update(oppdatertVilkårGodTro)

        val lagretVilkårGodTro = vilkårGodTroRepository.findByIdOrThrow(vilkårGodTro.id)
        Assertions.assertThat(lagretVilkårGodTro).isEqualToIgnoringGivenFields(oppdatertVilkårGodTro, "sporbar")
    }

}