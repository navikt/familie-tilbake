package no.nav.familie.tilbake.behandlingskontroll

import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Fagsystemsbehandling
import no.nav.familie.tilbake.behandling.domain.Varsel
import no.nav.familie.tilbake.behandling.domain.Varselsperiode
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg.AVSLUTTET
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
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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
        val lagretBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(lagretBehandling.copy(fagsystemsbehandling = setOf(fagsystemsbehandling),
                                                          varsler = setOf(varsel)))

        behandlingskontrollService.fortsettBehandling(behandlingId = behandling.id)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertEquals(1, behandlingsstegstilstand.size)
        val sisteStegstilstand = behandlingsstegstilstand[0]
        assertEquals(VARSEL, sisteStegstilstand.behandlingssteg)
        assertEquals(VENTER, sisteStegstilstand.behandlingsstegsstatus)
        assertEquals(Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING, sisteStegstilstand.venteårsak)
        assertEquals(behandling.opprettetDato.plusWeeks(4), sisteStegstilstand.tidsfrist)
    }

    @Test
    fun `fortsettBehandling skal ikke fortsette til grunnlag steg når behandling venter på varsel steg`() {
        lagBehandlingsstegstilstand(setOf(lagBehandlingsstegsinfo(VARSEL, Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING)))

        behandlingskontrollService.fortsettBehandling(behandlingId = behandling.id)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertEquals(1, behandlingsstegstilstand.size)
        val sisteStegstilstand = behandlingsstegstilstand[0]
        assertEquals(VARSEL, sisteStegstilstand.behandlingssteg)
        assertEquals(VENTER, sisteStegstilstand.behandlingsstegsstatus)
        assertEquals(Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING, sisteStegstilstand.venteårsak)
        assertEquals(behandling.opprettetDato.plusWeeks(4), sisteStegstilstand.tidsfrist)
    }

    @Test
    fun `fortsettBehandling skal oppdatere til grunnlag steg etter behandling er opprettet uten varsel`() {
        val fagsystemsbehandling = lagFagsystemsbehandling(Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL)
        val lagretBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(lagretBehandling.copy(fagsystemsbehandling = setOf(fagsystemsbehandling),
                                                          varsler = emptySet()))

        behandlingskontrollService.fortsettBehandling(behandlingId = behandling.id)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertEquals(1, behandlingsstegstilstand.size)
        val sisteStegstilstand = behandlingsstegstilstand[0]
        assertEquals(GRUNNLAG, sisteStegstilstand.behandlingssteg)
        assertEquals(VENTER, sisteStegstilstand.behandlingsstegsstatus)
        assertEquals(Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG, sisteStegstilstand.venteårsak)
        assertEquals(behandling.opprettetDato.plusWeeks(4), sisteStegstilstand.tidsfrist)
    }

    @Test
    fun `fortsettBehandling skal oppdatere til fakta steg etter behandling er opprettet uten varsel og mottok kravgrunnlag`() {
        val fagsystemsbehandling = lagFagsystemsbehandling(Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL)
        val lagretBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandlingRepository.update(lagretBehandling.copy(fagsystemsbehandling = setOf(fagsystemsbehandling),
                                                          varsler = emptySet()))
        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)

        behandlingskontrollService.fortsettBehandling(behandlingId = behandling.id)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertEquals(1, behandlingsstegstilstand.size)
        val sisteStegstilstand = behandlingsstegstilstand[0]
        assertEquals(FAKTA, sisteStegstilstand.behandlingssteg)
        assertEquals(KLAR, sisteStegstilstand.behandlingsstegsstatus)
        assertNull(sisteStegstilstand.venteårsak)
        assertNull(sisteStegstilstand.tidsfrist)
    }

    @Test
    fun `fortsettBehandling skal oppdatere til foreldelse steg etter fakta steg er utført`() {
        lagBehandlingsstegstilstand(setOf(Behandlingsstegsinfo(FAKTA, UTFØRT)))
        behandlingskontrollService.fortsettBehandling(behandlingId = behandling.id)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertEquals(2, behandlingsstegstilstand.size)
        val sisteStegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstand)
        assertNotNull(sisteStegstilstand)
        assertEquals(FORELDELSE, sisteStegstilstand.behandlingssteg)
        assertEquals(KLAR, sisteStegstilstand.behandlingsstegsstatus)
        assertNull(sisteStegstilstand.venteårsak)
        assertNull(sisteStegstilstand.tidsfrist)
    }

    @Test
    fun `fortsettBehandling skal oppdatere til vilkårsvurdering steg etter foreldelse steg er utført`() {
        lagBehandlingsstegstilstand(setOf(Behandlingsstegsinfo(FAKTA, UTFØRT),
                                          Behandlingsstegsinfo(FORELDELSE, UTFØRT)))

        behandlingskontrollService.fortsettBehandling(behandlingId = behandling.id)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertEquals(3, behandlingsstegstilstand.size)
        val sisteStegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstand)
        assertNotNull(sisteStegstilstand)
        assertEquals(VILKÅRSVURDERING, sisteStegstilstand.behandlingssteg)
        assertEquals(KLAR, sisteStegstilstand.behandlingsstegsstatus)
        assertNull(sisteStegstilstand.venteårsak)
        assertNull(sisteStegstilstand.tidsfrist)
    }

    @Test
    fun `fortsettBehandling skal ikke oppdatere til foreldelse steg når fakta steg ikke er utført`() {
        lagBehandlingsstegstilstand(setOf(Behandlingsstegsinfo(VARSEL, UTFØRT),
                                          Behandlingsstegsinfo(GRUNNLAG, UTFØRT),
                                          Behandlingsstegsinfo(FAKTA, KLAR)))

        behandlingskontrollService.fortsettBehandling(behandlingId = behandling.id)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertEquals(3, behandlingsstegstilstand.size)
        val sisteStegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstand)
        assertNotNull(sisteStegstilstand)
        assertEquals(FAKTA, sisteStegstilstand.behandlingssteg)
        assertEquals(KLAR, sisteStegstilstand.behandlingsstegsstatus)
        assertNull(sisteStegstilstand.venteårsak)
        assertNull(sisteStegstilstand.tidsfrist)
    }

    @Test
    fun `fortsettBehandling skal oppdatere til fakta steg etter mottok endr melding`() {
        lagBehandlingsstegstilstand(setOf(Behandlingsstegsinfo(VARSEL, UTFØRT),
                                          Behandlingsstegsinfo(GRUNNLAG, UTFØRT),
                                          Behandlingsstegsinfo(FAKTA, AVBRUTT),
                                          Behandlingsstegsinfo(FORELDELSE, AVBRUTT),
                                          Behandlingsstegsinfo(VILKÅRSVURDERING, AVBRUTT)))

        kravgrunnlagRepository.insert(Testdata.kravgrunnlag431)

        behandlingskontrollService.fortsettBehandling(behandlingId = behandling.id)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertEquals(5, behandlingsstegstilstand.size)
        val sisteStegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstand)
        assertNotNull(sisteStegstilstand)
        assertEquals(FAKTA, sisteStegstilstand.behandlingssteg)
        assertEquals(KLAR, sisteStegstilstand.behandlingsstegsstatus)
        assertNull(sisteStegstilstand.venteårsak)
        assertNull(sisteStegstilstand.tidsfrist)
    }

    @Test
    fun `tilbakehoppBehandlingssteg skal oppdatere til varsel steg når manuelt varsel sendt og behandling er i vilkår steg `() {
        lagBehandlingsstegstilstand(setOf(Behandlingsstegsinfo(VARSEL, UTFØRT),
                                          Behandlingsstegsinfo(GRUNNLAG, UTFØRT),
                                          Behandlingsstegsinfo(FAKTA, UTFØRT),
                                          Behandlingsstegsinfo(FORELDELSE, AUTOUTFØRT),
                                          Behandlingsstegsinfo(VILKÅRSVURDERING, KLAR)))

        behandlingskontrollService
                .tilbakehoppBehandlingssteg(behandlingId = behandling.id,
                                            behandlingsstegsinfo =
                                            lagBehandlingsstegsinfo(VARSEL, Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING))
        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertEquals(5, behandlingsstegstilstand.size)

        val sisteStegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstand)
        assertNotNull(sisteStegstilstand)
        assertEquals(VARSEL, sisteStegstilstand.behandlingssteg)
        assertEquals(VENTER, sisteStegstilstand.behandlingsstegsstatus)
        assertEquals(Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING, sisteStegstilstand.venteårsak)
        assertEquals(behandling.opprettetDato.plusWeeks(4), sisteStegstilstand.tidsfrist)

        assertEquals(UTFØRT, behandlingsstegstilstand.first { GRUNNLAG == it.behandlingssteg }.behandlingsstegsstatus)
        assertEquals(UTFØRT, behandlingsstegstilstand.first { FAKTA == it.behandlingssteg }.behandlingsstegsstatus)
        assertEquals(AUTOUTFØRT, behandlingsstegstilstand.first { FORELDELSE == it.behandlingssteg }.behandlingsstegsstatus)
        assertEquals(AVBRUTT, behandlingsstegstilstand.first { VILKÅRSVURDERING == it.behandlingssteg }.behandlingsstegsstatus)
    }

    @Test
    fun `tilbakehoppBehandlingssteg skal oppdatere til varsel steg når mottok sper melding og behandling er i vilkår steg `() {
        lagBehandlingsstegstilstand(setOf(Behandlingsstegsinfo(VARSEL, UTFØRT),
                                          Behandlingsstegsinfo(GRUNNLAG, UTFØRT),
                                          Behandlingsstegsinfo(FAKTA, UTFØRT),
                                          Behandlingsstegsinfo(FORELDELSE, AUTOUTFØRT),
                                          Behandlingsstegsinfo(VILKÅRSVURDERING, KLAR)))

        behandlingskontrollService
                .tilbakehoppBehandlingssteg(behandlingId = behandling.id,
                                            behandlingsstegsinfo =
                                            lagBehandlingsstegsinfo(GRUNNLAG, Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG))

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertEquals(5, behandlingsstegstilstand.size)

        val sisteStegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstand)
        assertNotNull(sisteStegstilstand)
        assertEquals(GRUNNLAG, sisteStegstilstand.behandlingssteg)
        assertEquals(VENTER, sisteStegstilstand.behandlingsstegsstatus)
        assertEquals(Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG, sisteStegstilstand.venteårsak)
        assertEquals(behandling.opprettetDato.plusWeeks(4), sisteStegstilstand.tidsfrist)

        assertEquals(UTFØRT, behandlingsstegstilstand.first { VARSEL == it.behandlingssteg }.behandlingsstegsstatus)
        assertEquals(UTFØRT, behandlingsstegstilstand.first { FAKTA == it.behandlingssteg }.behandlingsstegsstatus)
        assertEquals(AUTOUTFØRT, behandlingsstegstilstand.first { FORELDELSE == it.behandlingssteg }.behandlingsstegsstatus)
        assertEquals(AVBRUTT, behandlingsstegstilstand.first { VILKÅRSVURDERING == it.behandlingssteg }.behandlingsstegsstatus)
    }

    @Test
    fun `settBehandlingPåVent skal sette behandling på vent med avventer dokumentasjon når behandling er i fakta steg`() {
        val tidsfrist: LocalDate = LocalDate.now().plusWeeks(2)
        lagBehandlingsstegstilstand(setOf(Behandlingsstegsinfo(VARSEL, UTFØRT),
                                          Behandlingsstegsinfo(GRUNNLAG, UTFØRT),
                                          Behandlingsstegsinfo(FAKTA, KLAR)))

        behandlingskontrollService.settBehandlingPåVent(behandlingId = behandling.id,
                                                        venteårsak = Venteårsak.AVVENTER_DOKUMENTASJON,
                                                        tidsfrist = tidsfrist)

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertEquals(3, behandlingsstegstilstand.size)

        val sisteStegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstand)
        assertNotNull(sisteStegstilstand)
        assertEquals(FAKTA, sisteStegstilstand.behandlingssteg)
        assertEquals(VENTER, sisteStegstilstand.behandlingsstegsstatus)
        assertEquals(Venteårsak.AVVENTER_DOKUMENTASJON, sisteStegstilstand.venteårsak)
        assertEquals(tidsfrist, sisteStegstilstand.tidsfrist)
    }

    @Test
    fun `settBehandlingPåVent skal ikke sette behandling på vent med avventer dokumentasjon når behandling er avsluttet`() {
        val tidsfrist: LocalDate = LocalDate.now().plusWeeks(2)
        lagBehandlingsstegstilstand(setOf(Behandlingsstegsinfo(VARSEL, AVBRUTT),
                                          Behandlingsstegsinfo(AVSLUTTET, UTFØRT)))

        val exception = assertFailsWith<RuntimeException>(block = {
            behandlingskontrollService.settBehandlingPåVent(behandlingId = behandling.id,
                                                            venteårsak = Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
                                                            tidsfrist = tidsfrist.minusDays(5))
        })
        assertEquals("Behandling ${behandling.id} har ikke aktivt steg", exception.message)
    }

    @Test
    fun `settBehandlingPåVent skal utvide fristen med brukers tilbakemelding når behandling er i varsel steg`() {
        val tidsfrist: LocalDate =
                behandling.opprettetDato.plusWeeks(Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING.defaultVenteTidIUker)
        lagBehandlingsstegstilstand(setOf(Behandlingsstegsinfo(VARSEL, VENTER,
                                                               venteårsak = Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
                                                               tidsfrist = tidsfrist)))

        behandlingskontrollService.settBehandlingPåVent(behandlingId = behandling.id,
                                                        venteårsak = Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
                                                        tidsfrist = tidsfrist.plusWeeks(2))

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertEquals(1, behandlingsstegstilstand.size)

        val sisteStegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingsstegstilstand)
        assertNotNull(sisteStegstilstand)
        assertEquals(VARSEL, sisteStegstilstand.behandlingssteg)
        assertEquals(VENTER, sisteStegstilstand.behandlingsstegsstatus)
        assertEquals(Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING, sisteStegstilstand.venteårsak)
        assertEquals(tidsfrist.plusWeeks(2), sisteStegstilstand.tidsfrist)
    }

    private fun lagBehandlingsstegstilstand(stegMetadata: Set<Behandlingsstegsinfo>) {
        stegMetadata.map {
            behandlingsstegstilstandRepository.insert(Behandlingsstegstilstand(behandlingId = behandling.id,
                                                                               behandlingssteg = it.behandlingssteg,
                                                                               behandlingsstegsstatus = it.behandlingsstegstatus,
                                                                               venteårsak = it.venteårsak,
                                                                               tidsfrist = it.tidsfrist))
        }

    }


    private fun lagFagsystemsbehandling(tilbakekrevingsvalg: Tilbakekrevingsvalg): Fagsystemsbehandling {
        return Fagsystemsbehandling(eksternId = "123",
                                    tilbakekrevingsvalg = tilbakekrevingsvalg,
                                    resultat = "testverdi",
                                    årsak = "testverdi",
                                    revurderingsvedtaksdato = LocalDate.now().minusDays(1))
    }

    private fun lagBehandlingsstegsinfo(behandlingssteg: Behandlingssteg,
                                        venteårsak: Venteårsak): Behandlingsstegsinfo {

        return Behandlingsstegsinfo(behandlingssteg = behandlingssteg,
                                    behandlingsstegstatus = VENTER,
                                    venteårsak = venteårsak,
                                    tidsfrist = behandling.opprettetDato.plusWeeks(venteårsak.defaultVenteTidIUker))
    }
}
