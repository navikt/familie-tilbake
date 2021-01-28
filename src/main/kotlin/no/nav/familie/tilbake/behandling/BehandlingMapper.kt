package no.nav.familie.tilbake.behandling

import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.VergeType
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import no.nav.familie.tilbake.behandling.domain.EksternBehandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Fagsystem
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.varsel.Varsel
import no.nav.familie.tilbake.varsel.Varselsperiode
import no.nav.familie.tilbake.verge.Verge
import no.nav.familie.tilbake.verge.Vergetype
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate

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

    private fun tilDomeneVarsel(opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest): Set<Varsel> {
        opprettTilbakekrevingRequest.varsel?.let {
            val varselsperioder =
                    it.perioder.map {
                        Varselsperiode(fom = it.fom, tom = it.tom)
                    }.toSet()
            return setOf(Varsel(varseltekst = it.varselTekst,
                                varselbeløp = it.sumFeilutbetaling.longValueExact(),
                                revurderingsvedtaksdato = opprettTilbakekrevingRequest.revurderingVedtakDato,
                                perioder = varselsperioder))
        }
        return emptySet()
    }

    private fun tilDomeneVerge(fagsystem: Fagsystem, opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest): Set<Verge> {
        opprettTilbakekrevingRequest.verge?.let {
            val fagsystemVerge = it
            if (fagsystemVerge.gyldigTom.isBefore(LocalDate.now())) {
                logger.info("Verge informasjon er utløpt.Så kopierer ikke fra fagsystem=$fagsystem")
            } else {
                return setOf(Verge(
                        type = tilDomeneVergetype(fagsystemVerge.vergeType),
                        kilde = fagsystem.name,
                        gyldigFom = fagsystemVerge.gyldigFom,
                        gyldigTom = fagsystemVerge.gyldigTom,
                        navn = fagsystemVerge.navn,
                        orgNr = fagsystemVerge.organisasjonsnummer,
                        ident = fagsystemVerge.personIdent?.ident
                ))
            }
        }
        return emptySet()
    }

    private fun tilDomeneVergetype(vergetype: VergeType): Vergetype {
        for (type in Vergetype.values()) {
            if (type.name == vergetype.name) {
                return type
            }
        }
        throw IllegalArgumentException("Ukjent vergeType=$vergetype")
    }
}
