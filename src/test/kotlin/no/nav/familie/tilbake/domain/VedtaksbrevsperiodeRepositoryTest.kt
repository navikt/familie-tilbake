package no.nav.familie.tilbake.domain

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.repository.tbd.VedtaksbrevsperiodeRepository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow

internal class VedtaksbrevsperiodeRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var vedtaksbrevsperiodeRepository: VedtaksbrevsperiodeRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private val vedtaksbrevsperiode = Testdata.vedtaksbrevsperiode

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(Testdata.behandling)
    }

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av Vedtaksbrevsperiode til basen`() {
        vedtaksbrevsperiodeRepository.insert(vedtaksbrevsperiode)

        val lagretVedtaksbrevsperiode = vedtaksbrevsperiodeRepository.findByIdOrThrow(vedtaksbrevsperiode.id)

        Assertions.assertThat(lagretVedtaksbrevsperiode).isEqualToIgnoringGivenFields(vedtaksbrevsperiode, "sporbar")
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av Vedtaksbrevsperiode i basen`() {
        vedtaksbrevsperiodeRepository.insert(vedtaksbrevsperiode)
        val oppdatertVedtaksbrevsperiode = vedtaksbrevsperiode.copy(fritekst = "bob")

        vedtaksbrevsperiodeRepository.update(oppdatertVedtaksbrevsperiode)

        val lagretVedtaksbrevsperiode = vedtaksbrevsperiodeRepository.findByIdOrThrow(vedtaksbrevsperiode.id)
        Assertions.assertThat(lagretVedtaksbrevsperiode).isEqualToIgnoringGivenFields(oppdatertVedtaksbrevsperiode, "sporbar")
    }

}