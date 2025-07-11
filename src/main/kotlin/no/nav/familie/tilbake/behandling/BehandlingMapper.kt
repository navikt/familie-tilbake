package no.nav.familie.tilbake.behandling

import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultat
import no.nav.familie.tilbake.behandling.domain.Behandlingsårsak
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Fagsystemsbehandling
import no.nav.familie.tilbake.behandling.domain.Fagsystemskonsekvens
import no.nav.familie.tilbake.behandling.domain.Varsel
import no.nav.familie.tilbake.behandling.domain.Varselsperiode
import no.nav.familie.tilbake.behandling.domain.Verge
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.ManuellBrevmottakerMapper
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.domene.ManuellBrevmottaker
import no.nav.familie.tilbake.kontrakter.klage.FagsystemType
import no.nav.familie.tilbake.kontrakter.klage.FagsystemVedtak
import no.nav.familie.tilbake.kontrakter.saksbehandler.Saksbehandler
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.api.v1.dto.BehandlingDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegsinfoDto
import no.nav.tilbakekreving.kontrakter.Behandlingsresultatstype.DELVIS_TILBAKEBETALING
import no.nav.tilbakekreving.kontrakter.Behandlingsresultatstype.FULL_TILBAKEBETALING
import no.nav.tilbakekreving.kontrakter.Behandlingsresultatstype.HENLAGT
import no.nav.tilbakekreving.kontrakter.Behandlingsresultatstype.INGEN_TILBAKEBETALING
import no.nav.tilbakekreving.kontrakter.Behandlingstype.REVURDERING_TILBAKEKREVING
import no.nav.tilbakekreving.kontrakter.Behandlingstype.TILBAKEKREVING
import no.nav.tilbakekreving.kontrakter.Behandlingsårsakstype.REVURDERING_FEILUTBETALT_BELØP_HELT_ELLER_DELVIS_BORTFALT
import no.nav.tilbakekreving.kontrakter.Behandlingsårsakstype.REVURDERING_KLAGE_KA
import no.nav.tilbakekreving.kontrakter.Behandlingsårsakstype.REVURDERING_KLAGE_NFP
import no.nav.tilbakekreving.kontrakter.Behandlingsårsakstype.REVURDERING_OPPLYSNINGER_OM_FORELDELSE
import no.nav.tilbakekreving.kontrakter.Behandlingsårsakstype.REVURDERING_OPPLYSNINGER_OM_VILKÅR
import no.nav.tilbakekreving.kontrakter.OpprettTilbakekrevingRequest
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsresultatstype
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsårsakstype
import no.nav.tilbakekreving.kontrakter.behandling.Saksbehandlingstype.AUTOMATISK_IKKE_INNKREVING_UNDER_4X_RETTSGEBYR
import no.nav.tilbakekreving.kontrakter.behandling.Saksbehandlingstype.ORDINÆR
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO

object BehandlingMapper {
    fun tilDomeneBehandling(
        opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest,
        fagsystem: FagsystemDTO,
        fagsak: Fagsak,
        ansvarligSaksbehandler: Saksbehandler,
        erAutomatiskOgFeatureTogglePå: Boolean,
    ): Behandling {
        val faktainfo = opprettTilbakekrevingRequest.faktainfo
        val fagsystemskonsekvenser = faktainfo.konsekvensForYtelser.map { Fagsystemskonsekvens(konsekvens = it) }.toSet()
        val fagsystemsbehandling =
            Fagsystemsbehandling(
                eksternId = opprettTilbakekrevingRequest.eksternId,
                tilbakekrevingsvalg = faktainfo.tilbakekrevingsvalg,
                revurderingsvedtaksdato = opprettTilbakekrevingRequest.revurderingsvedtaksdato,
                resultat = faktainfo.revurderingsresultat,
                årsak = faktainfo.revurderingsårsak,
                konsekvenser = fagsystemskonsekvenser,
            )
        val varsler = tilDomeneVarsel(opprettTilbakekrevingRequest)
        val verger = tilDomeneVerge(Fagsystem.forDTO(fagsystem), opprettTilbakekrevingRequest)

        return Behandling(
            fagsakId = fagsak.id,
            type = Behandlingstype.TILBAKEKREVING,
            saksbehandlingstype = if (erAutomatiskOgFeatureTogglePå) AUTOMATISK_IKKE_INNKREVING_UNDER_4X_RETTSGEBYR else ORDINÆR,
            ansvarligSaksbehandler = ansvarligSaksbehandler.navIdent,
            behandlendeEnhet = opprettTilbakekrevingRequest.enhetId,
            behandlendeEnhetsNavn = opprettTilbakekrevingRequest.enhetsnavn,
            manueltOpprettet = opprettTilbakekrevingRequest.manueltOpprettet,
            fagsystemsbehandling = setOf(fagsystemsbehandling),
            varsler = varsler,
            verger = verger,
            regelverk = opprettTilbakekrevingRequest.regelverk,
            begrunnelseForTilbakekreving = opprettTilbakekrevingRequest.begrunnelseForTilbakekreving,
        )
    }

    fun tilRespons(
        behandling: Behandling,
        erBehandlingPåVent: Boolean,
        kanHenleggeBehandling: Boolean,
        kanEndres: Boolean,
        kanSetteBehandlingTilbakeTilFakta: Boolean,
        kanRevurderingOpprettes: Boolean,
        behandlingsstegsinfoer: List<Behandlingsstegsinfo>,
        varselSendt: Boolean,
        eksternFagsakId: String,
        manuelleBrevmottakere: List<ManuellBrevmottaker>,
        støtterManuelleBrevmottakere: Boolean,
    ): BehandlingDto {
        val resultat: Behandlingsresultat? =
            behandling.resultater.maxByOrNull {
                it.sporbar.endret.endretTid
            }

        return BehandlingDto(
            eksternBrukId = behandling.eksternBrukId,
            behandlingId = behandling.id,
            type = behandling.type,
            status = behandling.status,
            erBehandlingHenlagt = resultat?.erBehandlingHenlagt() ?: false,
            resultatstype = resultat?.resultatstypeTilFrontend(),
            enhetskode = behandling.behandlendeEnhet,
            enhetsnavn = behandling.behandlendeEnhetsNavn,
            ansvarligSaksbehandler = behandling.ansvarligSaksbehandler,
            ansvarligBeslutter = behandling.ansvarligBeslutter,
            opprettetDato = behandling.opprettetDato,
            avsluttetDato = behandling.avsluttetDato,
            vedtaksdato = behandling.sisteResultat?.behandlingsvedtak?.vedtaksdato,
            endretTidspunkt = behandling.endretTidspunkt,
            harVerge = behandling.harVerge,
            kanHenleggeBehandling = kanHenleggeBehandling,
            kanRevurderingOpprettes = kanRevurderingOpprettes,
            erBehandlingPåVent = erBehandlingPåVent,
            kanEndres = kanEndres,
            kanSetteTilbakeTilFakta = kanSetteBehandlingTilbakeTilFakta,
            varselSendt = varselSendt,
            behandlingsstegsinfo = tilBehandlingstegsinfoDto(behandlingsstegsinfoer),
            fagsystemsbehandlingId = behandling.aktivFagsystemsbehandling.eksternId,
            eksternFagsakId = eksternFagsakId,
            behandlingsårsakstype = behandling.sisteÅrsak?.type,
            harManuelleBrevmottakere = manuelleBrevmottakere.isNotEmpty(),
            støtterManuelleBrevmottakere = støtterManuelleBrevmottakere,
            manuelleBrevmottakere = manuelleBrevmottakere.map { ManuellBrevmottakerMapper.tilRespons(it) },
            begrunnelseForTilbakekreving = behandling.begrunnelseForTilbakekreving,
            saksbehandlingstype = behandling.saksbehandlingstype,
        )
    }

    private fun tilBehandlingstegsinfoDto(behandlingsstegsinfoListe: List<Behandlingsstegsinfo>): List<BehandlingsstegsinfoDto> =
        behandlingsstegsinfoListe.map {
            BehandlingsstegsinfoDto(
                behandlingssteg = it.behandlingssteg,
                behandlingsstegstatus = it.behandlingsstegstatus,
                venteårsak = it.venteårsak,
                tidsfrist = it.tidsfrist,
            )
        }

    private fun tilDomeneVarsel(opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest): Set<Varsel> {
        return opprettTilbakekrevingRequest.varsel?.let {
            val varselsperioder =
                it.perioder
                    .map { periode ->
                        Varselsperiode(fom = periode.fom, tom = periode.tom)
                    }.toSet()
            return setOf(
                Varsel(
                    varseltekst = it.varseltekst,
                    varselbeløp = it.sumFeilutbetaling.longValueExact(),
                    perioder = varselsperioder,
                ),
            )
        } ?: emptySet()
    }

    private fun tilDomeneVerge(
        fagsystem: Fagsystem,
        opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest,
    ): Set<Verge> {
        opprettTilbakekrevingRequest.verge?.let {
            return setOf(
                Verge(
                    type = it.vergetype,
                    kilde = fagsystem.name,
                    navn = it.navn,
                    orgNr = it.organisasjonsnummer,
                    ident = it.personIdent,
                ),
            )
        }
        return emptySet()
    }

    fun tilBehandlingerForFagsystem(behandling: Behandling): no.nav.tilbakekreving.kontrakter.Behandling {
        val resultat: Behandlingsresultat? =
            behandling.resultater.maxByOrNull {
                it.sporbar.endret.endretTid
            }
        return no.nav.tilbakekreving.kontrakter.Behandling(
            behandlingId = behandling.eksternBrukId,
            opprettetTidspunkt = behandling.opprettetTidspunkt,
            aktiv = !behandling.erAvsluttet,
            type = mapType(behandling),
            status = mapStatus(behandling),
            årsak = mapÅrsak(behandling),
            vedtaksdato = behandling.avsluttetDato?.atStartOfDay(),
            resultat = mapResultat(resultat),
        )
    }

    fun tilVedtakForFagsystem(behandlinger: List<Behandling>): List<FagsystemVedtak> =
        behandlinger
            .filter { it.erAvsluttet }
            .filter { it.sisteResultat?.erBehandlingFastsatt() ?: false }
            .map {
                val avsluttetDato = it.avsluttetDato ?: error("Mangler avsluttet dato på behandling=${it.id}")
                val sisteResultat = it.sisteResultat ?: error("Mangler resultat på behandling=${it.id}")

                FagsystemVedtak(
                    eksternBehandlingId = it.eksternBrukId.toString(),
                    behandlingstype = mapType(it).visningsnavn,
                    resultat = sisteResultat.type.navn,
                    vedtakstidspunkt = avsluttetDato.atStartOfDay(),
                    fagsystemType = FagsystemType.TILBAKEKREVING,
                    regelverk = it.regelverk,
                )
            }

    private fun mapType(behandling: Behandling): no.nav.tilbakekreving.kontrakter.Behandlingstype =
        when (behandling.type) {
            Behandlingstype.TILBAKEKREVING -> TILBAKEKREVING
            Behandlingstype.REVURDERING_TILBAKEKREVING -> REVURDERING_TILBAKEKREVING
        }

    private fun mapStatus(behandling: Behandling): no.nav.tilbakekreving.kontrakter.Behandlingsstatus =
        when (behandling.status) {
            Behandlingsstatus.AVSLUTTET -> no.nav.tilbakekreving.kontrakter.Behandlingsstatus.AVSLUTTET
            Behandlingsstatus.UTREDES -> no.nav.tilbakekreving.kontrakter.Behandlingsstatus.UTREDES
            Behandlingsstatus.FATTER_VEDTAK -> no.nav.tilbakekreving.kontrakter.Behandlingsstatus.FATTER_VEDTAK
            Behandlingsstatus.IVERKSETTER_VEDTAK ->
                no.nav.tilbakekreving.kontrakter.Behandlingsstatus.IVERKSETTER_VEDTAK
            Behandlingsstatus.OPPRETTET -> no.nav.tilbakekreving.kontrakter.Behandlingsstatus.OPPRETTET
        }

    private fun mapÅrsak(behandling: Behandling): no.nav.tilbakekreving.kontrakter.Behandlingsårsakstype? {
        if (behandling.årsaker.isEmpty()) return null
        return when (behandling.årsaker.firstOrNull()?.type) {
            Behandlingsårsakstype.REVURDERING_KLAGE_KA -> REVURDERING_KLAGE_KA
            Behandlingsårsakstype.REVURDERING_KLAGE_NFP -> REVURDERING_KLAGE_NFP
            Behandlingsårsakstype.REVURDERING_OPPLYSNINGER_OM_VILKÅR -> REVURDERING_OPPLYSNINGER_OM_VILKÅR
            Behandlingsårsakstype.REVURDERING_OPPLYSNINGER_OM_FORELDELSE -> REVURDERING_OPPLYSNINGER_OM_FORELDELSE
            Behandlingsårsakstype.REVURDERING_FEILUTBETALT_BELØP_HELT_ELLER_DELVIS_BORTFALT ->
                REVURDERING_FEILUTBETALT_BELØP_HELT_ELLER_DELVIS_BORTFALT
            else -> null
        }
    }

    private fun mapResultat(resultat: Behandlingsresultat?): no.nav.tilbakekreving.kontrakter.Behandlingsresultatstype? =
        when (resultat?.type) {
            Behandlingsresultatstype.DELVIS_TILBAKEBETALING -> DELVIS_TILBAKEBETALING
            Behandlingsresultatstype.FULL_TILBAKEBETALING -> FULL_TILBAKEBETALING
            Behandlingsresultatstype.INGEN_TILBAKEBETALING -> INGEN_TILBAKEBETALING
            Behandlingsresultatstype.HENLAGT,
            Behandlingsresultatstype.HENLAGT_FEILOPPRETTET,
            Behandlingsresultatstype.HENLAGT_FEILOPPRETTET_MED_BREV,
            Behandlingsresultatstype.HENLAGT_FEILOPPRETTET_UTEN_BREV,
            Behandlingsresultatstype.HENLAGT_KRAVGRUNNLAG_NULLSTILT,
            Behandlingsresultatstype.HENLAGT_TEKNISK_VEDLIKEHOLD,
            Behandlingsresultatstype.HENLAGT_MANGLENDE_KRAVGRUNNLAG,
            -> HENLAGT
            Behandlingsresultatstype.IKKE_FASTSATT,
            null,
            -> null
        }

    fun tilDomeneBehandlingRevurdering(
        originalBehandling: Behandling,
        behandlingsårsakstype: Behandlingsårsakstype,
        logContext: SecureLog.Context,
    ): Behandling {
        val verger: Set<Verge> = kopiVerge(originalBehandling)?.let { setOf(it) } ?: emptySet()
        return Behandling(
            fagsakId = originalBehandling.fagsakId,
            type = Behandlingstype.REVURDERING_TILBAKEKREVING,
            ansvarligSaksbehandler = ContextService.hentSaksbehandler(logContext),
            behandlendeEnhet = originalBehandling.behandlendeEnhet,
            behandlendeEnhetsNavn = originalBehandling.behandlendeEnhetsNavn,
            manueltOpprettet = false,
            årsaker =
                setOf(
                    Behandlingsårsak(
                        type = behandlingsårsakstype,
                        originalBehandlingId = originalBehandling.id,
                    ),
                ),
            fagsystemsbehandling = setOf(kopiFagsystemsbehandling(originalBehandling)),
            verger = verger,
            regelverk = originalBehandling.regelverk,
            begrunnelseForTilbakekreving = originalBehandling.begrunnelseForTilbakekreving,
        )
    }

    private fun kopiFagsystemsbehandling(originalBehandling: Behandling): Fagsystemsbehandling {
        val fagsystemsbehandling = originalBehandling.aktivFagsystemsbehandling
        return Fagsystemsbehandling(
            eksternId = fagsystemsbehandling.eksternId,
            årsak = fagsystemsbehandling.årsak,
            resultat = fagsystemsbehandling.resultat,
            tilbakekrevingsvalg = fagsystemsbehandling.tilbakekrevingsvalg,
            revurderingsvedtaksdato = fagsystemsbehandling.revurderingsvedtaksdato,
            konsekvenser = kopiFagsystemskonsekvenser(fagsystemsbehandling.konsekvenser),
        )
    }

    private fun kopiFagsystemskonsekvenser(originalKonsekvenser: Set<Fagsystemskonsekvens>): Set<Fagsystemskonsekvens> = originalKonsekvenser.map { Fagsystemskonsekvens(konsekvens = it.konsekvens) }.toSet()

    private fun kopiVerge(originalBehandling: Behandling): Verge? {
        val verge = originalBehandling.aktivVerge
        return verge?.let {
            Verge(
                type = it.type,
                ident = it.ident,
                orgNr = it.orgNr,
                navn = it.navn,
                begrunnelse = it.begrunnelse,
                kilde = it.kilde,
            )
        }
    }
}
