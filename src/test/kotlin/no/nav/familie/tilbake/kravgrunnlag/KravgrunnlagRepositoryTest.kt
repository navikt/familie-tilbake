package no.nav.familie.tilbake.kravgrunnlag

import io.kotest.matchers.equality.shouldBeEqualToComparingFieldsExcept
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class KravgrunnlagRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    private lateinit var fagsak: Fagsak
    private lateinit var kravgrunnlag431: Kravgrunnlag431
    private lateinit var behandling: Behandling

    @BeforeEach
    fun init() {
        fagsak = Testdata.fagsak
        kravgrunnlag431 = Testdata.kravgrunnlag431
        behandling = Testdata.behandling
        fagsakRepository.insert(fagsak)
        behandlingRepository.insert(behandling)
    }

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av Kravgrunnlag431 til basen`() {
        kravgrunnlagRepository.insert(kravgrunnlag431)

        val lagretKravgrunnlag431 = kravgrunnlagRepository.findByIdOrThrow(kravgrunnlag431.id)

        lagretKravgrunnlag431.shouldBeEqualToComparingFieldsExcept(
            kravgrunnlag431,
            Kravgrunnlag431::sporbar,
            Kravgrunnlag431::perioder,
            Kravgrunnlag431::versjon,
        )
        lagretKravgrunnlag431.versjon shouldBe 1
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av Kravgrunnlag431 i basen`() {
        kravgrunnlagRepository.insert(kravgrunnlag431)
        var lagretKravgrunnlag431 = kravgrunnlagRepository.findByIdOrThrow(kravgrunnlag431.id)
        val oppdatertKravgrunnlag431 = lagretKravgrunnlag431.copy(sperret = true)

        kravgrunnlagRepository.update(oppdatertKravgrunnlag431)

        lagretKravgrunnlag431 = kravgrunnlagRepository.findByIdOrThrow(kravgrunnlag431.id)
        lagretKravgrunnlag431.shouldBeEqualToComparingFieldsExcept(
            oppdatertKravgrunnlag431,
            Kravgrunnlag431::sporbar,
            Kravgrunnlag431::perioder,
            Kravgrunnlag431::versjon,
        )
        lagretKravgrunnlag431.versjon shouldBe 2
    }

    @Test
    fun `findByBehandlingId returnerer resultat når det finnes en forekomst`() {
        kravgrunnlagRepository.insert(kravgrunnlag431)

        val findByBehandlingId = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)

        kravgrunnlag431.shouldBeEqualToComparingFieldsExcept(
            findByBehandlingId,
            Kravgrunnlag431::sporbar,
            Kravgrunnlag431::perioder,
            Kravgrunnlag431::versjon,
        )
    }
}
