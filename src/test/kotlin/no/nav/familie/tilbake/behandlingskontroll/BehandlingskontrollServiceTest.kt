package no.nav.familie.tilbake.behandlingskontroll

import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Fagsystemsbehandling
import no.nav.familie.tilbake.behandling.domain.Varsel
import no.nav.familie.tilbake.behandling.domain.Varselsperiode
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg.FAKTA
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg.FORELDELSE
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg.GRUNNLAG
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg.VARSEL
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg.VILKÅRSVURDERING
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus.AUTOUTFØRT
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus.AVBRUTT
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus.KLAR
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus.UTFØRT
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus.VENTER
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class BehandlingskontrollServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var behandlingskontrollService: BehandlingskontrollService


    private val behandling = Testdata.behandling

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(behandling)
    }

    @Test
    fun `fortsettBehandling skal oppdatere til varsel steg etter behandling er opprettet med varsel`() {
        val fagsystemsbehandling = lagFagsystemsbehandling(Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL)
        val varsel = Varsel(varseltekst = "testverdi",
                            varselbeløp = 1000L,
                            perioder = setOf(Varselsperiode(fom = LocalDate.now().minusMonths(2),
                                                            tom = LocalDate.now())))
        behandlingRepository.update(behandling.copy(fagsystemsbehandling = setOf(fagsystemsbehandling), varsler = setOf(varsel)))

        behandlingskontrollService.fortsettBehandling(behandlingId = behandling.id)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertEquals(1, behandlingsstegstilstand.size)
        val sisteStegstilstand = behandlingsstegstilstand[0]
        assertEquals(VARSEL, sisteStegstilstand.behandlingssteg)
        assertEquals(VENTER, sisteStegstilstand.behandlingsstegsstatus)
    }

    @Test
    fun `fortsettBehandling skal ikke fortsette til grunnlag steg når behandling venter på varsel steg`() {
        lagBehandlingsstegstilstand(setOf(BehandlingsstegMedStatus(VARSEL, VENTER)))

        behandlingskontrollService.fortsettBehandling(behandlingId = behandling.id)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertEquals(1, behandlingsstegstilstand.size)
        val sisteStegstilstand = behandlingsstegstilstand[0]
        assertEquals(VARSEL, sisteStegstilstand.behandlingssteg)
        assertEquals(VENTER, sisteStegstilstand.behandlingsstegsstatus)
    }

    @Test
    fun `fortsettBehandling skal oppdatere til grunnlag steg etter behandling er opprettet uten varsel`() {
        val fagsystemsbehandling = lagFagsystemsbehandling(Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL)
        behandlingRepository.update(behandling.copy(fagsystemsbehandling = setOf(fagsystemsbehandling), varsler = emptySet()))

        behandlingskontrollService.fortsettBehandling(behandlingId = behandling.id)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertEquals(1, behandlingsstegstilstand.size)
        val sisteStegstilstand = behandlingsstegstilstand[0]
        assertEquals(GRUNNLAG, sisteStegstilstand.behandlingssteg)
        assertEquals(VENTER, sisteStegstilstand.behandlingsstegsstatus)
    }

    @Test
    fun `fortsettBehandling skal oppdatere til fakta steg etter behandling er opprettet uten varsel og mottok kravgrunnlag`() {
        val fagsystemsbehandling = lagFagsystemsbehandling(Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL)
        behandlingRepository.update(behandling.copy(fagsystemsbehandling = setOf(fagsystemsbehandling), varsler = emptySet()))
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)

        behandlingskontrollService.fortsettBehandling(behandlingId = behandling.id)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertEquals(1, behandlingsstegstilstand.size)
        val sisteStegstilstand = behandlingsstegstilstand[0]
        assertEquals(FAKTA, sisteStegstilstand.behandlingssteg)
        assertEquals(KLAR, sisteStegstilstand.behandlingsstegsstatus)
    }

    @Test
    fun `fortsettBehandling skal oppdatere til foreldelse steg etter fakta steg er utført`() {
        lagBehandlingsstegstilstand(setOf(BehandlingsstegMedStatus(FAKTA, UTFØRT)))
        behandlingskontrollService.fortsettBehandling(behandlingId = behandling.id)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertEquals(2, behandlingsstegstilstand.size)
        val sisteStegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstand)
        assertNotNull(sisteStegstilstand)
        assertEquals(FORELDELSE, sisteStegstilstand.behandlingssteg)
        assertEquals(KLAR, sisteStegstilstand.behandlingsstegsstatus)
    }

    @Test
    fun `fortsettBehandling skal oppdatere til vilkårsvurdering steg etter foreldelse steg er utført`() {
        lagBehandlingsstegstilstand(setOf(BehandlingsstegMedStatus(FAKTA, UTFØRT),
                                          BehandlingsstegMedStatus(FORELDELSE, UTFØRT)))

        behandlingskontrollService.fortsettBehandling(behandlingId = behandling.id)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertEquals(3, behandlingsstegstilstand.size)
        val sisteStegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstand)
        assertNotNull(sisteStegstilstand)
        assertEquals(VILKÅRSVURDERING, sisteStegstilstand.behandlingssteg)
        assertEquals(KLAR, sisteStegstilstand.behandlingsstegsstatus)
    }

    @Test
    fun `fortsettBehandling skal ikke oppdatere til foreldelse steg når fakta steg ikke er utført`() {
        lagBehandlingsstegstilstand(setOf(BehandlingsstegMedStatus(VARSEL, UTFØRT),
                                          BehandlingsstegMedStatus(GRUNNLAG, UTFØRT),
                                          BehandlingsstegMedStatus(FAKTA, KLAR)))

        behandlingskontrollService.fortsettBehandling(behandlingId = behandling.id)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertEquals(3, behandlingsstegstilstand.size)
        val sisteStegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstand)
        assertNotNull(sisteStegstilstand)
        assertEquals(FAKTA, sisteStegstilstand.behandlingssteg)
        assertEquals(KLAR, sisteStegstilstand.behandlingsstegsstatus)
    }

    @Test
    fun `fortsettBehandling skal oppdatere til fakta steg etter mottok endr melding`() {
        lagBehandlingsstegstilstand(setOf(BehandlingsstegMedStatus(VARSEL, UTFØRT),
                                          BehandlingsstegMedStatus(GRUNNLAG, UTFØRT),
                                          BehandlingsstegMedStatus(FAKTA, AVBRUTT),
                                          BehandlingsstegMedStatus(FORELDELSE, AVBRUTT),
                                          BehandlingsstegMedStatus(VILKÅRSVURDERING, AVBRUTT)))

        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)

        behandlingskontrollService.fortsettBehandling(behandlingId = behandling.id)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertEquals(5, behandlingsstegstilstand.size)
        val sisteStegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstand)
        assertNotNull(sisteStegstilstand)
        assertEquals(FAKTA, sisteStegstilstand.behandlingssteg)
        assertEquals(KLAR, sisteStegstilstand.behandlingsstegsstatus)
    }

    @Test
    fun `tilbakehoppBehandlingssteg skal oppdatere til varsel steg når manuelt varsel sendt og behandling er i vilkår steg `() {
        lagBehandlingsstegstilstand(setOf(BehandlingsstegMedStatus(VARSEL, UTFØRT),
                                          BehandlingsstegMedStatus(GRUNNLAG, UTFØRT),
                                          BehandlingsstegMedStatus(FAKTA, UTFØRT),
                                          BehandlingsstegMedStatus(FORELDELSE, AUTOUTFØRT),
                                          BehandlingsstegMedStatus(VILKÅRSVURDERING, KLAR)))

        behandlingskontrollService.tilbakehoppBehandlingssteg(behandlingId = behandling.id,
                                                              behandlingsstegMedStatus = BehandlingsstegMedStatus(VARSEL, VENTER))
        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertEquals(5, behandlingsstegstilstand.size)

        val sisteStegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstand)
        assertNotNull(sisteStegstilstand)
        assertEquals(VARSEL, sisteStegstilstand.behandlingssteg)
        assertEquals(VENTER, sisteStegstilstand.behandlingsstegsstatus)

        assertEquals(UTFØRT, behandlingsstegstilstand.first { GRUNNLAG == it.behandlingssteg }.behandlingsstegsstatus)
        assertEquals(AVBRUTT, behandlingsstegstilstand.first { FAKTA == it.behandlingssteg }.behandlingsstegsstatus)
        assertEquals(AVBRUTT, behandlingsstegstilstand.first { FORELDELSE == it.behandlingssteg }.behandlingsstegsstatus)
        assertEquals(AVBRUTT, behandlingsstegstilstand.first { VILKÅRSVURDERING == it.behandlingssteg }.behandlingsstegsstatus)
    }

    @Test
    fun `tilbakehoppBehandlingssteg skal oppdatere til varsel steg når mottok sper melding og behandling er i vilkår steg `() {
        lagBehandlingsstegstilstand(setOf(BehandlingsstegMedStatus(VARSEL, UTFØRT),
                                          BehandlingsstegMedStatus(GRUNNLAG, UTFØRT),
                                          BehandlingsstegMedStatus(FAKTA, UTFØRT),
                                          BehandlingsstegMedStatus(FORELDELSE, AUTOUTFØRT),
                                          BehandlingsstegMedStatus(VILKÅRSVURDERING, KLAR)))

        behandlingskontrollService.tilbakehoppBehandlingssteg(behandlingId = behandling.id,
                                                              behandlingsstegMedStatus = BehandlingsstegMedStatus(GRUNNLAG, VENTER))

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertEquals(5, behandlingsstegstilstand.size)

        val sisteStegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstand)
        assertNotNull(sisteStegstilstand)
        assertEquals(GRUNNLAG, sisteStegstilstand.behandlingssteg)
        assertEquals(VENTER, sisteStegstilstand.behandlingsstegsstatus)

        assertEquals(UTFØRT, behandlingsstegstilstand.first { VARSEL == it.behandlingssteg }.behandlingsstegsstatus)
        assertEquals(AVBRUTT, behandlingsstegstilstand.first { FAKTA == it.behandlingssteg }.behandlingsstegsstatus)
        assertEquals(AVBRUTT, behandlingsstegstilstand.first { FORELDELSE == it.behandlingssteg }.behandlingsstegsstatus)
        assertEquals(AVBRUTT, behandlingsstegstilstand.first { VILKÅRSVURDERING == it.behandlingssteg }.behandlingsstegsstatus)
    }

    private fun lagBehandlingsstegstilstand(stegMetadata: Set<BehandlingsstegMedStatus>) {
        stegMetadata.map {
            behandlingsstegstilstandRepository.insert(Behandlingsstegstilstand(behandlingId = behandling.id,
                                                                               behandlingssteg = it.behandlingssteg,
                                                                               behandlingsstegsstatus = it.behandlingsstegstatus))
        }

    }


    private fun lagFagsystemsbehandling(tilbakekrevingsvalg: Tilbakekrevingsvalg): Fagsystemsbehandling {
        return Fagsystemsbehandling(eksternId = "123",
                                    tilbakekrevingsvalg = tilbakekrevingsvalg,
                                    resultat = "testverdi",
                                    årsak = "testverdi",
                                    revurderingsvedtaksdato = LocalDate.now().minusDays(1))
    }
}
