package no.nav.familie.tilbake.behandling

import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.tilbake.api.dto.BehandlingDto
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultat
import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import no.nav.familie.tilbake.behandling.domain.EksternBehandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Fagsystem
import no.nav.familie.tilbake.behandling.domain.Varsel
import no.nav.familie.tilbake.behandling.domain.Varselsperiode
import no.nav.familie.tilbake.behandling.domain.Verge
import no.nav.familie.tilbake.behandling.domain.Vergetype
import no.nav.familie.tilbake.common.ContextService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import no.nav.familie.kontrakter.felles.tilbakekreving.Vergetype as Kontraksvergetype

object BehandlingMapper {

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)
    fun tilDomeneBehandling(opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest,
                            fagsystem: Fagsystem,
                            fagsak: Fagsak): Behandling {
        val eksternBehandling = EksternBehandling(eksternId = opprettTilbakekrevingRequest.eksternId)
        val varsler = tilDomeneVarsel(opprettTilbakekrevingRequest)
        val verger = tilDomeneVerge(fagsystem, opprettTilbakekrevingRequest)

        return Behandling(fagsakId = fagsak.id, type = Behandlingstype.TILBAKEKREVING,
                          ansvarligSaksbehandler = ContextService.hentSaksbehandler(),
                          behandlendeEnhet = opprettTilbakekrevingRequest.enhetId,
                          behandlendeEnhetsNavn = opprettTilbakekrevingRequest.enhetsnavn,
                          manueltOpprettet = opprettTilbakekrevingRequest.manueltOpprettet,
                          eksternBehandling = setOf(eksternBehandling),
                          varsler = varsler,
                          verger = verger)
    }

    fun tilRespons(behandling: Behandling,
               kanHenleggeBehandling: Boolean): BehandlingDto {

        val resultat: Behandlingsresultat? = behandling.resultater.maxByOrNull { behandlingsresultat ->
            behandlingsresultat.sporbar.endret.endretTid
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
                endretTidspunkt = behandling.endretTidspunkt,
                harVerge = behandling.verger.isNotEmpty(),
                kanHenleggeBehandling = kanHenleggeBehandling,
                erBehandlingPåVent = false) //hard-kodert til vente funksjonalitet er implementert

    }

    private fun tilDomeneVarsel(opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest): Set<Varsel> {
        return opprettTilbakekrevingRequest.varsel?.let {
            val varselsperioder =
                    it.perioder.map { periode ->
                        Varselsperiode(fom = periode.fom, tom = periode.tom)
                    }.toSet()
            return setOf(Varsel(varseltekst = it.varseltekst,
                                varselbeløp = it.sumFeilutbetaling.longValueExact(),
                                revurderingsvedtaksdato = opprettTilbakekrevingRequest.revurderingsvedtaksdato,
                                perioder = varselsperioder))
        } ?: emptySet()
    }

    private fun tilDomeneVerge(fagsystem: Fagsystem, opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest): Set<Verge> {
        opprettTilbakekrevingRequest.verge?.let {
            if (it.gyldigTom.isBefore(LocalDate.now())) {
                logger.info("Vergeinformasjon er utløpt.Så kopierer ikke fra fagsystem=$fagsystem")
            } else {
                return setOf(Verge(
                        type = tilDomeneVergetype(it.vergetype),
                        kilde = fagsystem.name,
                        gyldigFom = it.gyldigFom,
                        gyldigTom = it.gyldigTom,
                        navn = it.navn,
                        orgNr = it.organisasjonsnummer,
                        ident = it.personIdent?.ident
                ))
            }
        }
        return emptySet()
    }

    private fun tilDomeneVergetype(vergetype: Kontraksvergetype): Vergetype {
        return Vergetype.values().firstOrNull {
            it.name == vergetype.name
        } ?: throw IllegalArgumentException("Ukjent vergetype=$vergetype")
    }
}
