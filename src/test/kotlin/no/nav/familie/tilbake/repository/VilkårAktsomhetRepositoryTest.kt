package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class VilkårAktsomhetRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var vilkårAktsomhetRepository: VilkårAktsomhetRepository

    @Autowired
    private lateinit var vilkårsperiodeRepository: VilkårsperiodeRepository

    @Autowired
    private lateinit var vilkårRepository: VilkårRepository

    private val vilkårAktsomhet = Testdata.vilkårAktsomhet

    @BeforeEach
    fun init() {
        vilkårRepository.insert(Testdata.vilkår)
        vilkårsperiodeRepository.insert(Testdata.vilkårsperiode)
    }

    @Test
    fun insertPersistererEnForekomstAvVilkårAktsomhetTilBasen() {
        vilkårAktsomhetRepository.insert(vilkårAktsomhet)

        val lagretVilkårAktsomhet = vilkårAktsomhetRepository.findByIdOrThrow(vilkårAktsomhet.id)

        Assertions.assertThat(lagretVilkårAktsomhet).isEqualToIgnoringGivenFields(vilkårAktsomhet, "sporbar")
    }

    @Test
    fun updateOppdatererEnForekomstAvVilkårAktsomhetIBasen() {
        vilkårAktsomhetRepository.insert(vilkårAktsomhet)
        val oppdatertVilkårAktsomhet = vilkårAktsomhet.copy(begrunnelse = "bob")

        vilkårAktsomhetRepository.update(oppdatertVilkårAktsomhet)

        val lagretVilkårAktsomhet = vilkårAktsomhetRepository.findByIdOrThrow(vilkårAktsomhet.id)
        Assertions.assertThat(lagretVilkårAktsomhet).isEqualToIgnoringGivenFields(oppdatertVilkårAktsomhet, "sporbar")
    }

}