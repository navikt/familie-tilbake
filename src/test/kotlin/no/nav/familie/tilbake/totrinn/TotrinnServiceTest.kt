package no.nav.familie.tilbake.totrinn

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.api.dto.VurdertTotrinnDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.data.Testdata
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class TotrinnServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var totrinnService: TotrinnService

    private val fagsak = Testdata.fagsak
    private val behandling = Testdata.behandling
    private val behandlingId = behandling.id

    @BeforeEach
    fun init() {
        fagsakRepository.insert(fagsak)
        behandlingRepository.insert(behandling)

    }

    @Test
    fun `hentTotrinnsvurderinger skal hente totrinnsvurdering for det første gang`() {
        lagBehandlingsstegstilstand(Behandlingssteg.VARSEL, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FATTE_VEDTAK, Behandlingsstegstatus.KLAR)

        val totrinnsvurderingDto = totrinnService.hentTotrinnsvurderinger(behandlingId)
        assertTrue { totrinnsvurderingDto.totrinnsstegsinfo.isNotEmpty() }
        assertEquals(4, totrinnsvurderingDto.totrinnsstegsinfo.size)

        val totrinnsstegsinfo = totrinnsvurderingDto.totrinnsstegsinfo
        assertTrue {
            totrinnsstegsinfo.any {
                Behandlingssteg.FAKTA == it.behandlingssteg
                && it.godkjent == null && it.begrunnelse == null
            }
        }

        assertTrue {
            totrinnsstegsinfo.any {
                Behandlingssteg.FORELDELSE == it.behandlingssteg
                && it.godkjent == null && it.begrunnelse == null
            }
        }

        assertTrue {
            totrinnsstegsinfo.any {
                Behandlingssteg.VILKÅRSVURDERING == it.behandlingssteg
                && it.godkjent == null && it.begrunnelse == null
            }
        }

        assertTrue {
            totrinnsstegsinfo.any {
                Behandlingssteg.FORESLÅ_VEDTAK == it.behandlingssteg
                && it.godkjent == null && it.begrunnelse == null
            }
        }

    }

    @Test
    fun `hentTotrinnsvurderinger skal hente totrinnsvurdering etter beslutters vurdering`() {
        lagBehandlingsstegstilstand(Behandlingssteg.VARSEL, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FATTE_VEDTAK, Behandlingsstegstatus.KLAR)

        totrinnService.lagreTotrinnsvurderinger(behandlingId = behandlingId,
                                                totrinnsvurderinger = listOf(
                                                        VurdertTotrinnDto(behandlingssteg = Behandlingssteg.FAKTA,
                                                                          godkjent = true, begrunnelse = "testverdi"),
                                                        VurdertTotrinnDto(behandlingssteg = Behandlingssteg.FORELDELSE,
                                                                          godkjent = true, begrunnelse = "testverdi"),
                                                        VurdertTotrinnDto(behandlingssteg = Behandlingssteg.VILKÅRSVURDERING,
                                                                          godkjent = false, begrunnelse = "testverdi"),
                                                        VurdertTotrinnDto(behandlingssteg = Behandlingssteg.FORESLÅ_VEDTAK,
                                                                          godkjent = false, begrunnelse = "testverdi")))

        val totrinnsvurderingDto = totrinnService.hentTotrinnsvurderinger(behandlingId)
        assertTrue { totrinnsvurderingDto.totrinnsstegsinfo.isNotEmpty() }
        assertEquals(3, totrinnsvurderingDto.totrinnsstegsinfo.size)

        val totrinnsstegsinfo = totrinnsvurderingDto.totrinnsstegsinfo
        assertTrue {
            totrinnsstegsinfo.any {
                Behandlingssteg.FAKTA == it.behandlingssteg
                && it.godkjent == true && it.begrunnelse == "testverdi"
            }
        }

        //Autoutførte steg lagres ikke
        assertFalse {
            totrinnsstegsinfo.any {
                Behandlingssteg.FORELDELSE == it.behandlingssteg
            }
        }

        assertTrue {
            totrinnsstegsinfo.any {
                Behandlingssteg.VILKÅRSVURDERING == it.behandlingssteg
                && it.godkjent == false && it.begrunnelse == "testverdi"
            }
        }

        assertTrue {
            totrinnsstegsinfo.any {
                Behandlingssteg.FORESLÅ_VEDTAK == it.behandlingssteg
                && it.godkjent == false && it.begrunnelse == "testverdi"
            }
        }

    }

    @Test
    fun `lagreTotrinnsvurderinger skal ikke lagre når det mangler steg i request som kan besluttes`() {
        lagBehandlingsstegstilstand(Behandlingssteg.VARSEL, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FATTE_VEDTAK, Behandlingsstegstatus.KLAR)

        val exception = assertFailsWith<RuntimeException> {
            totrinnService
                    .lagreTotrinnsvurderinger(behandlingId = behandlingId,
                                              totrinnsvurderinger = listOf(
                                                      VurdertTotrinnDto(behandlingssteg = Behandlingssteg.FAKTA,
                                                                        godkjent = true, begrunnelse = "testverdi"),
                                                      VurdertTotrinnDto(behandlingssteg = Behandlingssteg.FORESLÅ_VEDTAK,
                                                                        godkjent = false, begrunnelse = "testverdi")))
        }

        assertEquals("Stegene [FORELDELSE, VILKÅRSVURDERING] mangler totrinnsvurdering", exception.message)
    }

    private fun lagBehandlingsstegstilstand(behandlingssteg: Behandlingssteg,
                                            behandlingsstegstatus: Behandlingsstegstatus) {
        behandlingsstegstilstandRepository.insert(Behandlingsstegstilstand(behandlingId = behandlingId,
                                                                           behandlingssteg = behandlingssteg,
                                                                           behandlingsstegsstatus = behandlingsstegstatus)
        )
    }


}
