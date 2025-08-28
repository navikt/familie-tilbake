package no.nav.familie.tilbake.vilkårsvurdering

import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurdering
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class VilkårsvurderingRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av Vilkårsvurdering til basen`() {
        val vilkår = Testdata.lagVilkårsvurdering(Testdata.lagBehandling(Testdata.fagsak().id).id)
        vilkårsvurderingRepository.insert(vilkår)

        val lagretVilkår = vilkårsvurderingRepository.findByIdOrThrow(vilkår.id)
        lagretVilkår.shouldBeEqualToIgnoringFields(vilkår, Vilkårsvurdering::sporbar, Vilkårsvurdering::versjon)
        lagretVilkår.versjon shouldBe 1
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av Vilkårsvurdering i basen`() {
        val vilkår = Testdata.lagVilkårsvurdering(Testdata.lagBehandling(Testdata.fagsak().id).id)
        vilkårsvurderingRepository.insert(vilkår)
        var lagretVilkår = vilkårsvurderingRepository.findByIdOrThrow(vilkår.id)
        val oppdatertVilkår = lagretVilkår.copy(aktiv = false)

        vilkårsvurderingRepository.update(oppdatertVilkår)

        lagretVilkår = vilkårsvurderingRepository.findByIdOrThrow(vilkår.id)
        lagretVilkår.shouldBeEqualToIgnoringFields(oppdatertVilkår, Vilkårsvurdering::sporbar, Vilkårsvurdering::versjon)
        lagretVilkår.versjon shouldBe 2
    }
}
