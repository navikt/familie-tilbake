package no.nav.familie.tilbake.behandling

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.tilbake.api.dto.BehandlingDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegsinfoDto
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultat
import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Fagsystemsbehandling
import no.nav.familie.tilbake.behandling.domain.Fagsystemskonsekvens
import no.nav.familie.tilbake.behandling.domain.Varsel
import no.nav.familie.tilbake.behandling.domain.Varselsperiode
import no.nav.familie.tilbake.behandling.domain.Verge
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.common.ContextService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate

object BehandlingMapper {

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    fun tilDomeneBehandling(opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest,
                            fagsystem: Fagsystem,
                            fagsak: Fagsak): Behandling {
        val faktainfo = opprettTilbakekrevingRequest.faktainfo
        val fagsystemskonsekvenser = faktainfo.konsekvensForYtelser
                .map { Fagsystemskonsekvens(konsekvens = it) }.toSet()
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
                          ansvarligSaksbehandler = ContextService.hentSaksbehandler(),
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
                   behandlingsstegsinfoer: List<Behandlingsstegsinfo>): BehandlingDto {

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
                             endretTidspunkt = behandling.endretTidspunkt,
                             harVerge = behandling.harVerge,
                             kanHenleggeBehandling = kanHenleggeBehandling,
                             erBehandlingPåVent = erBehandlingPåVent,
                             kanEndres = kanEndres,
                             behandlingsstegsinfo = tilBehandlingstegsinfoDto(behandlingsstegsinfoer))

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
}
