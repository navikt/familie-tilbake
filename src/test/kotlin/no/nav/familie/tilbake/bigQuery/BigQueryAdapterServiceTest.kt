package no.nav.familie.tilbake.bigQuery

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forSingle
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingRepository
import no.nav.tilbakekreving.api.v1.dto.BigQueryVilkårsvurderingsperiodeDto
import no.nav.tilbakekreving.api.v1.dto.RettsligGrunnlag
import no.nav.tilbakekreving.bigquery.BigQueryOppsamler
import no.nav.tilbakekreving.bigquery.grensesnittStub
import no.nav.tilbakekreving.kontrakter.periode.Månedsperiode.Companion.til
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import no.nav.tilbakekreving.test.februar
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.test.mars
import org.junit.jupiter.api.Test

class BigQueryAdapterServiceTest {
    @Test
    fun `aktive vurderinger med uvurdert periode og tidligere inaktiv vurdering`() {
        val fagsak = Testdata.fagsak()
        val behandling = Testdata.lagBehandling(fagsak.id)
        val første = Testdata.vilkårsperiode(januar(2021) til januar(2021)).copy(
            vilkårsvurderingsresultat = Vilkårsvurderingsresultat.GOD_TRO,
            aktsomhet = null,
        )
        val andre = Testdata.vilkårsperiode(februar(2021) til februar(2021))
        val uvurdert = Testdata.vilkårsperiode(mars(2021) til mars(2021)).copy(
            vilkårsvurderingsresultat = Vilkårsvurderingsresultat.UDEFINERT,
            aktsomhet = null,
        )
        val aktiv = Testdata.lagVilkårsvurdering(behandling.id, linkedSetOf(andre, uvurdert, første))
        val inaktiv = Testdata.lagVilkårsvurdering(behandling.id).copy(aktiv = false)
        val alle = listOf(inaktiv, aktiv)
        val oppsamler = BigQueryOppsamler()
        val repository = grensesnittStub<VilkårsvurderingRepository> { metode, argumenter ->
            argumenter.single() shouldBe behandling.id
            when (metode) {
                "findByBehandlingIdAndAktivIsTrue" -> alle.filter { it.aktiv }
                "findByBehandlingId" -> alle
                else -> error("Uventet repository-kall: $metode")
            }
        }
        val fagsaker = grensesnittStub<FagsakRepository> { metode, argumenter ->
            check(metode == "finnFagsakForBehandlingId")
            argumenter.single() shouldBe behandling.id
            fagsak
        }
        val kravgrunnlag = grensesnittStub<KravgrunnlagRepository> { metode, _ -> error("Uventet kravgrunnlag-kall: $metode") }

        BigQueryAdapterService(kravgrunnlag, oppsamler, fagsaker, repository).lagreVilkårsvurdering(behandling)

        oppsamler.vurderinger.forSingle {
            it.behandlingId shouldBe behandling.id.toString()
            it.ytelse shouldBe fagsak.fagsystem.navn
            it.perioder shouldBe listOf(
                BigQueryVilkårsvurderingsperiodeDto(første.id, første.periode.toDatoperiode(), RettsligGrunnlag.GOD_TRO),
                BigQueryVilkårsvurderingsperiodeDto(andre.id, andre.periode.toDatoperiode(), RettsligGrunnlag.BURDE_FORSTÅTT),
            )
        }
        oppsamler.behandlinger.shouldBeEmpty()
    }

    @Test
    fun `ingen aktive vurderinger`() {
        val fagsak = Testdata.fagsak()
        val behandling = Testdata.lagBehandling(fagsak.id)
        val oppsamler = BigQueryOppsamler()
        val repository = grensesnittStub<VilkårsvurderingRepository> { metode, argumenter ->
            check(metode == "findByBehandlingIdAndAktivIsTrue")
            argumenter.single() shouldBe behandling.id
            emptyList<Any>()
        }
        val fagsaker = grensesnittStub<FagsakRepository> { metode, _ ->
            check(metode == "finnFagsakForBehandlingId")
            fagsak
        }
        val kravgrunnlag = grensesnittStub<KravgrunnlagRepository> { metode, _ -> error("Uventet kall: $metode") }

        BigQueryAdapterService(kravgrunnlag, oppsamler, fagsaker, repository).lagreVilkårsvurdering(behandling)

        oppsamler.vurderinger.forSingle { it.perioder.shouldBeEmpty() }
    }

    @Test
    fun `aktiv vurdering mangler aktsomhet`() {
        val fagsak = Testdata.fagsak()
        val behandling = Testdata.lagBehandling(fagsak.id)
        val oppsamler = BigQueryOppsamler()
        val vurdering = Testdata.lagVilkårsvurdering(
            behandling.id,
            setOf(Testdata.vilkårsperiode().copy(aktsomhet = null)),
        )
        val repository = grensesnittStub<VilkårsvurderingRepository> { metode, _ ->
            check(metode == "findByBehandlingIdAndAktivIsTrue")
            listOf(vurdering)
        }
        val fagsaker = grensesnittStub<FagsakRepository> { metode, _ ->
            check(metode == "finnFagsakForBehandlingId")
            fagsak
        }
        val kravgrunnlag = grensesnittStub<KravgrunnlagRepository> { metode, _ -> error("Uventet kall: $metode") }

        shouldThrow<IllegalArgumentException> {
            BigQueryAdapterService(kravgrunnlag, oppsamler, fagsaker, repository).lagreVilkårsvurdering(behandling)
        }
        oppsamler.vurderinger.shouldBeEmpty()
    }
}
