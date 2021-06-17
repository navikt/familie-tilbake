package no.nav.familie.tilbake.behandling

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsresultatstype.DELVIS_TILBAKEBETALING
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsresultatstype.FULL_TILBAKEBETALING
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsresultatstype.HENLAGT
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsresultatstype.INGEN_TILBAKEBETALING
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingstype.REVURDERING_TILBAKEKREVING
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingstype.TILBAKEKREVING
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsårsakstype.REVURDERING_FEILUTBETALT_BELØP_HELT_ELLER_DELVIS_BORTFALT
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsårsakstype.REVURDERING_KLAGE_KA
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsårsakstype.REVURDERING_KLAGE_NFP
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsårsakstype.REVURDERING_OPPLYSNINGER_OM_FORELDELSE
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsårsakstype.REVURDERING_OPPLYSNINGER_OM_VILKÅR
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.tilbake.api.dto.BehandlingDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegsinfoDto
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultat
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import no.nav.familie.tilbake.behandling.domain.Behandlingsårsakstype
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Fagsystemsbehandling
import no.nav.familie.tilbake.behandling.domain.Fagsystemskonsekvens
import no.nav.familie.tilbake.behandling.domain.Varsel
import no.nav.familie.tilbake.behandling.domain.Varselsperiode
import no.nav.familie.tilbake.behandling.domain.Verge
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate

object BehandlingMapper {

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    fun tilDomeneBehandling(opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest,
                            fagsystem: Fagsystem,
                            fagsak: Fagsak): Behandling {
        val faktainfo = opprettTilbakekrevingRequest.faktainfo
        val fagsystemskonsekvenser = faktainfo.konsekvensForYtelser.map { Fagsystemskonsekvens(konsekvens = it) }.toSet()
        val fagsystemsbehandling =
                Fagsystemsbehandling(eksternId = opprettTilbakekrevingRequest.eksternId,
                                     tilbakekrevingsvalg = faktainfo.tilbakekrevingsvalg,
                                     revurderingsvedtaksdato = opprettTilbakekrevingRequest.revurderingsvedtaksdato,
                                     resultat = faktainfo.revurderingsresultat,
                                     årsak = faktainfo.revurderingsårsak,
                                     konsekvenser = fagsystemskonsekvenser)
        val varsler = tilDomeneVarsel(opprettTilbakekrevingRequest)
        val verger = tilDomeneVerge(fagsystem, opprettTilbakekrevingRequest)

        return Behandling(fagsakId = fagsak.id,
                          type = Behandlingstype.TILBAKEKREVING,
                          ansvarligSaksbehandler = opprettTilbakekrevingRequest.saksbehandlerIdent,
                          behandlendeEnhet = opprettTilbakekrevingRequest.enhetId,
                          behandlendeEnhetsNavn = opprettTilbakekrevingRequest.enhetsnavn,
                          manueltOpprettet = opprettTilbakekrevingRequest.manueltOpprettet,
                          fagsystemsbehandling = setOf(fagsystemsbehandling),
                          varsler = varsler,
                          verger = verger)
    }

    fun tilRespons(behandling: Behandling,
                   erBehandlingPåVent: Boolean,
                   kanHenleggeBehandling: Boolean,
                   kanEndres: Boolean,
                   behandlingsstegsinfoer: List<Behandlingsstegsinfo>,
                   varselSendt: Boolean): BehandlingDto {

        val resultat: Behandlingsresultat? = behandling.resultater.maxByOrNull {
            it.sporbar.endret.endretTid
        }

        return BehandlingDto(eksternBrukId = behandling.eksternBrukId,
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
                             erBehandlingPåVent = erBehandlingPåVent,
                             kanEndres = kanEndres,
                             varselSendt = varselSendt,
                             behandlingsstegsinfo = tilBehandlingstegsinfoDto(behandlingsstegsinfoer),
                             fagsystemsbehandlingId = behandling.aktivFagsystemsbehandling.eksternId)

    }

    private fun tilBehandlingstegsinfoDto(behandlingsstegsinfoListe: List<Behandlingsstegsinfo>): List<BehandlingsstegsinfoDto> {
        return behandlingsstegsinfoListe.map {
            BehandlingsstegsinfoDto(behandlingssteg = it.behandlingssteg,
                                    behandlingsstegstatus = it.behandlingsstegstatus,
                                    venteårsak = it.venteårsak,
                                    tidsfrist = it.tidsfrist)
        }
    }

    private fun tilDomeneVarsel(opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest): Set<Varsel> {
        return opprettTilbakekrevingRequest.varsel?.let {
            val varselsperioder =
                    it.perioder.map { periode ->
                        Varselsperiode(fom = periode.fom, tom = periode.tom)
                    }.toSet()
            return setOf(Varsel(varseltekst = it.varseltekst,
                                varselbeløp = it.sumFeilutbetaling.longValueExact(),
                                perioder = varselsperioder))
        } ?: emptySet()
    }

    private fun tilDomeneVerge(fagsystem: Fagsystem, opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest): Set<Verge> {
        opprettTilbakekrevingRequest.verge?.let {
            if (it.gyldigTom < LocalDate.now()) {
                logger.info("Vergeinformasjon er utløpt.Så kopierer ikke fra fagsystem=$fagsystem")
            } else {
                return setOf(Verge(type = it.vergetype,
                                   kilde = fagsystem.name,
                                   gyldigFom = it.gyldigFom,
                                   gyldigTom = it.gyldigTom,
                                   navn = it.navn,
                                   orgNr = it.organisasjonsnummer,
                                   ident = it.personIdent))
            }
        }
        return emptySet()
    }

    fun tilBehandlingerForFagsystem(behandling: Behandling): no.nav.familie.kontrakter.felles.tilbakekreving.Behandling {
        val resultat: Behandlingsresultat? = behandling.resultater.maxByOrNull {
            it.sporbar.endret.endretTid
        }
        return no.nav.familie.kontrakter.felles.tilbakekreving.Behandling(behandlingId = behandling.eksternBrukId,
                                                                          opprettetTidspunkt = behandling.opprettetTidspunkt,
                                                                          aktiv = !behandling.erAvsluttet,
                                                                          type = mapType(behandling),
                                                                          status = mapStatus(behandling),
                                                                          årsak = mapÅrsak(behandling),
                                                                          vedtaksdato = behandling.avsluttetDato?.atStartOfDay(),
                                                                          resultat = mapResultat(resultat))
    }

    private fun mapType(behandling: Behandling): no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingstype {
        return when (behandling.type) {
            Behandlingstype.TILBAKEKREVING -> TILBAKEKREVING
            Behandlingstype.REVURDERING_TILBAKEKREVING -> REVURDERING_TILBAKEKREVING
        }
    }

    private fun mapStatus(behandling: Behandling): no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsstatus {
        return when (behandling.status) {
            Behandlingsstatus.AVSLUTTET -> no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsstatus.AVSLUTTET
            Behandlingsstatus.UTREDES -> no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsstatus.UTREDES
            Behandlingsstatus.FATTER_VEDTAK -> no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsstatus.FATTER_VEDTAK
            Behandlingsstatus.IVERKSETTER_VEDTAK -> no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsstatus.IVERKSETTER_VEDTAK
            Behandlingsstatus.OPPRETTET -> no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsstatus.OPPRETTET
        }
    }

    private fun mapÅrsak(behandling: Behandling): no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsårsakstype? {
        if (behandling.årsaker.isEmpty()) return null
        return when (behandling.årsaker.firstOrNull()?.type) {
            Behandlingsårsakstype.REVURDERING_KLAGE_KA -> REVURDERING_KLAGE_KA
            Behandlingsårsakstype.REVURDERING_KLAGE_NFP -> REVURDERING_KLAGE_NFP
            Behandlingsårsakstype.REVURDERING_OPPLYSNINGER_OM_VILKÅR -> REVURDERING_OPPLYSNINGER_OM_VILKÅR
            Behandlingsårsakstype.REVURDERING_OPPLYSNINGER_OM_FORELDELSE -> REVURDERING_OPPLYSNINGER_OM_FORELDELSE
            Behandlingsårsakstype.REVURDERING_FEILUTBETALT_BELØP_HELT_ELLER_DELVIS_BORTFALT -> REVURDERING_FEILUTBETALT_BELØP_HELT_ELLER_DELVIS_BORTFALT
            else -> null
        }
    }

    private fun mapResultat(resultat: Behandlingsresultat?): no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsresultatstype? {
        return when (resultat?.type) {
            Behandlingsresultatstype.DELVIS_TILBAKEBETALING -> DELVIS_TILBAKEBETALING
            Behandlingsresultatstype.FULL_TILBAKEBETALING -> FULL_TILBAKEBETALING
            Behandlingsresultatstype.INGEN_TILBAKEBETALING -> INGEN_TILBAKEBETALING
            Behandlingsresultatstype.HENLAGT,
            Behandlingsresultatstype.HENLAGT_FEILOPPRETTET,
            Behandlingsresultatstype.HENLAGT_FEILOPPRETTET_MED_BREV,
            Behandlingsresultatstype.HENLAGT_FEILOPPRETTET_UTEN_BREV,
            Behandlingsresultatstype.HENLAGT_KRAVGRUNNLAG_NULLSTILT,
            Behandlingsresultatstype.HENLAGT_TEKNISK_VEDLIKEHOLD -> HENLAGT
            else -> null
        }
    }
}
