package no.nav.familie.tilbake.api.forvaltning

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.Fagsystem
import no.nav.familie.tilbake.behandling.Ytelsestype
import no.nav.familie.tilbake.behandling.domain.Bruker
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.common.exceptionhandler.ForbiddenError
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.tilbakekreving.e2e.ContextServiceHelpers.somSaksbehandler
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
import no.nav.tilbakekreving.test.FellesTestdata.SAKSBEHANDLER_IDENT
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class ForvaltningControllerTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var forvaltningController: ForvaltningController

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Test
    fun `Forvalter kan sette behandling på vent tilbake til fakta`() {
        shouldNotThrowAny {
            flyttBehandlingTilFakta(opprettTestdata(), grupper = listOf("familie123"))
        }
    }

    @Test
    fun `Beslutter skal ikke kunne kalle på forvalterendepunkt`() {
        shouldThrow<ForbiddenError> {
            flyttBehandlingTilFakta(opprettTestdata(), grupper = listOf("eb123"))
        }
    }

    @Test
    fun `Saksbehandler og forvalter som ikke er ansvarlig saksbehandler skal kunne bruke forvaltningsendepunkt`() {
        shouldNotThrowAny {
            flyttBehandlingTilFakta(opprettTestdata(), ident = "ikke ansvarlig", grupper = listOf("familie123", "es123"))
        }
    }

    @Test
    fun `Veileder skal ikke kunne sette behandling tilbake til faktasteg`() {
        shouldThrow<ForbiddenError> {
            flyttBehandlingTilFakta(opprettTestdata(), grupper = listOf("ev123"))
        }
    }

    @Test
    fun `Forvalter kan sette behandling tilbake til fakta når behandling ikke er under utredning`() {
        shouldNotThrowAny {
            flyttBehandlingTilFakta(opprettTestdata(behandlingStatus = Behandlingsstatus.FATTER_VEDTAK), grupper = listOf("familie123"))
        }
    }

    private fun flyttBehandlingTilFakta(
        behandlingId: UUID,
        ident: String = SAKSBEHANDLER_IDENT,
        grupper: List<String>,
    ) = somSaksbehandler(ident = ident, grupper = grupper) {
        forvaltningController.flyttBehandlingTilFakta(behandlingId)
    }

    private fun opprettTestdata(behandlingStatus: Behandlingsstatus = Behandlingsstatus.UTREDES): UUID {
        val fagsak = Fagsak(
            ytelsestype = Ytelsestype.BARNETRYGD,
            fagsystem = Fagsystem.EF,
            eksternFagsakId = UUID.randomUUID().toString(),
            bruker = Bruker(ident = "32132132111"),
        )
        val behandling = Testdata.lagBehandling(fagsakId = fagsak.id, ansvarligSaksbehandler = SAKSBEHANDLER_IDENT, behandlingStatus = behandlingStatus)
        fagsakRepository.insert(fagsak)
        behandlingRepository.insert(behandling)
        kravgrunnlagRepository.insert(Testdata.lagKravgrunnlag(behandling.id))
        behandlingsstegstilstandRepository.insert(
            Behandlingsstegstilstand(
                behandlingId = behandling.id,
                behandlingssteg = Behandlingssteg.FAKTA,
                behandlingsstegsstatus = Behandlingsstegstatus.KLAR,
                tidsfrist = LocalDate.now().plusWeeks(3),
                venteårsak = Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG,
            ),
        )
        return behandling.id
    }
}
