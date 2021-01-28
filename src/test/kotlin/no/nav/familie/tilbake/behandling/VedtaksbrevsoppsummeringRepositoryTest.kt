package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class VedtaksbrevsoppsummeringRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var vedtaksbrevsoppsummeringRepository: VedtaksbrevsoppsummeringRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private val vedtaksbrevsoppsummering = Testdata.vedtaksbrevsoppsummering

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(Testdata.behandling)
    }

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av Vedtaksbrevsoppsummering til basen`() {
        vedtaksbrevsoppsummeringRepository.insert(vedtaksbrevsoppsummering)

        val lagretVedtaksbrevsoppsummering = vedtaksbrevsoppsummeringRepository.findByIdOrThrow(vedtaksbrevsoppsummering.id)

        Assertions.assertThat(lagretVedtaksbrevsoppsummering).isEqualToIgnoringGivenFields(vedtaksbrevsoppsummering, "sporbar")
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av Vedtaksbrevsoppsummering i basen`() {
        vedtaksbrevsoppsummeringRepository.insert(vedtaksbrevsoppsummering)
        val oppdatertVedtaksbrevsoppsummering = vedtaksbrevsoppsummering.copy(fritekst = "bob")

        vedtaksbrevsoppsummeringRepository.update(oppdatertVedtaksbrevsoppsummering)

        val lagretVedtaksbrevsoppsummering = vedtaksbrevsoppsummeringRepository.findByIdOrThrow(vedtaksbrevsoppsummering.id)
        Assertions.assertThat(lagretVedtaksbrevsoppsummering)
                .isEqualToIgnoringGivenFields(oppdatertVedtaksbrevsoppsummering, "sporbar")
    }

}