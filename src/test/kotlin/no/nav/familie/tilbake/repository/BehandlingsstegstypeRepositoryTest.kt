package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class BehandlingsstegstypeRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var behandlingsstegstypeRepository: BehandlingsstegstypeRepository

    private val behandlingsstegstype = Testdata.behandlingsstegstype

    @Test
    fun insertPersistererEnForekomstAvBehandlingsstegstypeTilBasen() {
        behandlingsstegstypeRepository.insert(behandlingsstegstype)

        val lagretBehandlingsstegstype = behandlingsstegstypeRepository.findByIdOrThrow(behandlingsstegstype.id)

        Assertions.assertThat(lagretBehandlingsstegstype).isEqualToIgnoringGivenFields(behandlingsstegstype, "sporbar")
    }

    @Test
    fun updateOppdatererEnForekomstAvBehandlingsstegstypeIBasen() {
        behandlingsstegstypeRepository.insert(behandlingsstegstype)
        val oppdatertBehandlingsstegstype = behandlingsstegstype.copy(beskrivelse = "bob")

        behandlingsstegstypeRepository.update(oppdatertBehandlingsstegstype)

        val lagretBehandlingsstegstype = behandlingsstegstypeRepository.findByIdOrThrow(behandlingsstegstype.id)
        Assertions.assertThat(lagretBehandlingsstegstype).isEqualToIgnoringGivenFields(oppdatertBehandlingsstegstype, "sporbar")
    }

}