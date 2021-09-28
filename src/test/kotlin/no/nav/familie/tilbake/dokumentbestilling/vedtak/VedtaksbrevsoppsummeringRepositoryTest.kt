package no.nav.familie.tilbake.dokumentbestilling.vedtak

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

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
        assertThat(lagretVedtaksbrevsoppsummering).usingRecursiveComparison()
                .ignoringFields("sporbar", "versjon")
                .isEqualTo(vedtaksbrevsoppsummering)
        assertEquals(1, lagretVedtaksbrevsoppsummering.versjon)
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av Vedtaksbrevsoppsummering i basen`() {
        vedtaksbrevsoppsummeringRepository.insert(vedtaksbrevsoppsummering)
        var lagretVedtaksbrevsoppsummering = vedtaksbrevsoppsummeringRepository.findByIdOrThrow(vedtaksbrevsoppsummering.id)
        val oppdatertVedtaksbrevsoppsummering = lagretVedtaksbrevsoppsummering.copy(oppsummeringFritekst = "bob")

        vedtaksbrevsoppsummeringRepository.update(oppdatertVedtaksbrevsoppsummering)

        lagretVedtaksbrevsoppsummering = vedtaksbrevsoppsummeringRepository.findByIdOrThrow(vedtaksbrevsoppsummering.id)
        assertThat(lagretVedtaksbrevsoppsummering)
                .usingRecursiveComparison().ignoringFields("sporbar", "versjon").isEqualTo(oppdatertVedtaksbrevsoppsummering)
        assertEquals(2, lagretVedtaksbrevsoppsummering.versjon)
    }

}
