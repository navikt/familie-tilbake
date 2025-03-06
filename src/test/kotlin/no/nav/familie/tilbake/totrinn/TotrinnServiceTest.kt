package no.nav.familie.tilbake.totrinn

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.api.v1.dto.Totrinnsstegsinfo
import no.nav.tilbakekreving.api.v1.dto.VurdertTotrinnDto
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

internal class TotrinnServiceTest : OppslagSpringRunnerTest() {
    override val tømDBEtterHverTest = false

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var totrinnService: TotrinnService

    private lateinit var fagsak: Fagsak
    private lateinit var behandling: Behandling
    private lateinit var behandlingId: UUID

    @BeforeEach
    fun init() {
        fagsak = Testdata.fagsak()
        behandling = Testdata.lagBehandling(fagsakId = fagsak.id)
        behandlingId = behandling.id
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

        val totrinnsstegsinfo = totrinnsvurderingDto.totrinnsstegsinfo
        totrinnsstegsinfo.shouldContainExactly(
            Totrinnsstegsinfo(Behandlingssteg.FAKTA, null, null),
            Totrinnsstegsinfo(Behandlingssteg.FORELDELSE, null, null),
            Totrinnsstegsinfo(Behandlingssteg.VILKÅRSVURDERING, null, null),
            Totrinnsstegsinfo(Behandlingssteg.FORESLÅ_VEDTAK, null, null),
        )
    }

    @Test
    fun `hentTotrinnsvurderinger skal hente totrinnsvurdering etter beslutters vurdering`() {
        lagBehandlingKlarForFatteVedtak()

        totrinnService.lagreTotrinnsvurderinger(
            behandlingId,
            listOf(
                VurdertTotrinnDto(
                    behandlingssteg = Behandlingssteg.FAKTA,
                    godkjent = true,
                    begrunnelse = "testverdi",
                ),
                VurdertTotrinnDto(
                    behandlingssteg = Behandlingssteg.FORELDELSE,
                    godkjent = true,
                    begrunnelse = "testverdi",
                ),
                VurdertTotrinnDto(
                    behandlingssteg = Behandlingssteg.VILKÅRSVURDERING,
                    godkjent = false,
                    begrunnelse = "testverdi",
                ),
                VurdertTotrinnDto(
                    behandlingssteg = Behandlingssteg.FORESLÅ_VEDTAK,
                    godkjent = false,
                    begrunnelse = "testverdi",
                ),
            ),
            SecureLog.Context.tom(),
        )

        val totrinnsvurderingDto = totrinnService.hentTotrinnsvurderinger(behandlingId)

        val totrinnsstegsinfo = totrinnsvurderingDto.totrinnsstegsinfo
        totrinnsstegsinfo.shouldContainExactly(
            Totrinnsstegsinfo(Behandlingssteg.FAKTA, true, "testverdi"),
            Totrinnsstegsinfo(Behandlingssteg.VILKÅRSVURDERING, false, "testverdi"),
            Totrinnsstegsinfo(Behandlingssteg.FORESLÅ_VEDTAK, false, "testverdi"),
        )
    }

    @Test
    fun `hentTotrinnsvurderinger skal hente totrinnsvurdering etter beslutters vurdering med nytt behandlingssteg`() {
        lagBehandlingKlarForFatteVedtak()

        totrinnService.lagreTotrinnsvurderinger(
            behandlingId,
            listOf(
                VurdertTotrinnDto(
                    behandlingssteg = Behandlingssteg.FAKTA,
                    godkjent = true,
                    begrunnelse = "testverdi",
                ),
                VurdertTotrinnDto(
                    behandlingssteg = Behandlingssteg.VILKÅRSVURDERING,
                    godkjent = false,
                    begrunnelse = "testverdi",
                ),
                VurdertTotrinnDto(
                    behandlingssteg = Behandlingssteg.FORESLÅ_VEDTAK,
                    godkjent = false,
                    begrunnelse = "testverdi",
                ),
            ),
            SecureLog.Context.tom(),
        )

        // Dette steget var ikke behandlet med første omgang
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.UTFØRT)

        val totrinnsvurderingDto = totrinnService.hentTotrinnsvurderinger(behandlingId)

        val totrinnsstegsinfo = totrinnsvurderingDto.totrinnsstegsinfo
        totrinnsstegsinfo.shouldContainExactly(
            Totrinnsstegsinfo(Behandlingssteg.FAKTA, true, "testverdi"),
            Totrinnsstegsinfo(Behandlingssteg.FORELDELSE, null, null),
            Totrinnsstegsinfo(Behandlingssteg.VILKÅRSVURDERING, false, "testverdi"),
            Totrinnsstegsinfo(Behandlingssteg.FORESLÅ_VEDTAK, false, "testverdi"),
        )
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

        val exception =
            shouldThrow<RuntimeException> {
                totrinnService.lagreTotrinnsvurderinger(
                    behandlingId,
                    listOf(
                        VurdertTotrinnDto(
                            behandlingssteg = Behandlingssteg.FAKTA,
                            godkjent = true,
                            begrunnelse = "testverdi",
                        ),
                        VurdertTotrinnDto(
                            behandlingssteg = Behandlingssteg.FORESLÅ_VEDTAK,
                            godkjent = false,
                            begrunnelse = "testverdi",
                        ),
                    ),
                    SecureLog.Context.tom(),
                )
            }

        exception.message shouldBe "Stegene [FORELDELSE, VILKÅRSVURDERING] mangler totrinnsvurdering"
    }

    @Test
    fun `tidligere beslutter skal bli satt hvis en vurdering er underkjent og har blitt fattet tidlig nok`() {
        lagBehandlingKlarForFatteVedtak()

        totrinnService.lagreTotrinnsvurderinger(
            behandlingId,
            listOf(
                VurdertTotrinnDto(
                    behandlingssteg = Behandlingssteg.FAKTA,
                    godkjent = false,
                    begrunnelse = "testverdi",
                ),
                VurdertTotrinnDto(
                    behandlingssteg = Behandlingssteg.VILKÅRSVURDERING,
                    godkjent = true,
                    begrunnelse = "testverdi",
                ),
                VurdertTotrinnDto(
                    behandlingssteg = Behandlingssteg.FORESLÅ_VEDTAK,
                    godkjent = true,
                    begrunnelse = "testverdi",
                ),
            ),
            SecureLog.Context.tom(),
        )

        val tidligerebeslutter = totrinnService.finnForrigeBeslutterMedNyVurderingEllerNull(behandlingId)

        assertThat(tidligerebeslutter).isNotNull
    }

    @Test
    fun `tidligere beslutter skal bli lik null hvis alle vurderinger er godkjente`() {
        lagBehandlingKlarForFatteVedtak()

        totrinnService.lagreTotrinnsvurderinger(
            behandlingId,
            listOf(
                VurdertTotrinnDto(
                    behandlingssteg = Behandlingssteg.FAKTA,
                    godkjent = true,
                    begrunnelse = "testverdi",
                ),
                VurdertTotrinnDto(
                    behandlingssteg = Behandlingssteg.VILKÅRSVURDERING,
                    godkjent = true,
                    begrunnelse = "testverdi",
                ),
                VurdertTotrinnDto(
                    behandlingssteg = Behandlingssteg.FORESLÅ_VEDTAK,
                    godkjent = true,
                    begrunnelse = "testverdi",
                ),
            ),
            SecureLog.Context.tom(),
        )

        val tidligerebeslutter = totrinnService.finnForrigeBeslutterMedNyVurderingEllerNull(behandlingId)

        assertThat(tidligerebeslutter).isNull()
    }

    @Test
    fun `tidligere beslutter skal bli null hvis det ikke finnes totrinnsvurderinger`() {
        lagBehandlingKlarForFatteVedtak()

        val tidligerebeslutter = totrinnService.finnForrigeBeslutterMedNyVurderingEllerNull(behandlingId)

        assertThat(tidligerebeslutter).isNull()
    }

    private fun lagBehandlingKlarForFatteVedtak() {
        lagBehandlingsstegstilstand(Behandlingssteg.VARSEL, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FAKTA, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORELDELSE, Behandlingsstegstatus.AUTOUTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.VILKÅRSVURDERING, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FORESLÅ_VEDTAK, Behandlingsstegstatus.UTFØRT)
        lagBehandlingsstegstilstand(Behandlingssteg.FATTE_VEDTAK, Behandlingsstegstatus.KLAR)
    }

    private fun lagBehandlingsstegstilstand(
        behandlingssteg: Behandlingssteg,
        behandlingsstegstatus: Behandlingsstegstatus,
    ) {
        behandlingsstegstilstandRepository.insert(
            Behandlingsstegstilstand(
                behandlingId = behandlingId,
                behandlingssteg = behandlingssteg,
                behandlingsstegsstatus = behandlingsstegstatus,
            ),
        )
    }
}
