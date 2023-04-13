package no.nav.familie.tilbake.dokumentbestilling.felles

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.tilbakekreving.MottakerType
import no.nav.familie.kontrakter.felles.tilbakekreving.Vergetype
import no.nav.familie.tilbake.behandling.domain.Verge
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.config.FeatureToggleConfig
import no.nav.familie.tilbake.config.FeatureToggleService
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.ManuellBrevmottakerService
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.domene.ManuellBrevmottaker
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.toManuellAdresse
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import no.nav.familie.tilbake.person.PersonService
import org.springframework.stereotype.Service
import java.util.UUID
import no.nav.familie.kontrakter.felles.tilbakekreving.Verge as VergeDto

@Service
class EksterneDataForBrevService(
    private val personService: PersonService,
    private val integrasjonerClient: IntegrasjonerClient,
    private val featureToggleService: FeatureToggleService,
    private val manuellBrevmottakerService: ManuellBrevmottakerService
) {

    fun hentPerson(ident: String, fagsystem: Fagsystem): Personinfo {
        return personService.hentPersoninfo(ident, fagsystem)
    }

    fun hentSaksbehandlernavn(id: String): String {
        val saksbehandler = integrasjonerClient.hentSaksbehandler(id)
        return saksbehandler.fornavn + " " + saksbehandler.etternavn
    }

    fun hentPåloggetSaksbehandlernavnMedDefault(defaultId: String?): String {
        val saksbehandlerId = ContextService.hentPåloggetSaksbehandler(defaultId)
        val saksbehandler = integrasjonerClient.hentSaksbehandler(saksbehandlerId)
        return saksbehandler.fornavn + " " + saksbehandler.etternavn
    }

    private fun hentAdresse(personinfo: Personinfo): Adresseinfo {
        return Adresseinfo(personinfo.ident, personinfo.navn)
    }

    fun hentAdresse(
        personinfo: Personinfo,
        brevmottager: Brevmottager,
        verge: Verge?,
        fagsystem: Fagsystem,
        behandlingId: UUID
    ): Adresseinfo {
        if (featureToggleService.isEnabled(FeatureToggleConfig.DSITRIBUER_TIL_MANUELLE_BREVMOTTAKERE)) {
            val manuelleBrevmottakere = manuellBrevmottakerService.hentBrevmottakere(behandlingId)
            if (manuelleBrevmottakere.isNotEmpty()) {
                val (manuellVerge, manuellBruker) = manuelleBrevmottakere
                    .partition { it.type == MottakerType.VERGE }
                    .let { it.first.singleOrNull() to it.second.singleOrNull() }

                val brukerNavn = manuellBruker?.navn ?: personinfo.navn
                val vergeNavn: String? = hentVergeNavn(manuellVerge, verge)

                if (brevmottager == Brevmottager.VERGE) {
                    if (manuellVerge == null && verge == null) {
                        throw Feil("Brevmottager for behandling $behandlingId was ${Brevmottager.VERGE} but no aktivVerge was found")
                    }
                    return hentAdresseForVerge(manuellVerge, vergeNavn, brukerNavn, verge)
                } else {
                    return hentAdresseForBruker(manuellBruker, brukerNavn, vergeNavn, personinfo)
                }
            }
        }

        return verge?.let {
            hentAdresse(
                vergeType = it.type,
                organisasjonsnummer = it.orgNr,
                vergeNavn = it.navn,
                personinfo = personinfo,
                brevmottager = brevmottager,
                vergeIdent = it.ident,
                fagsystem = fagsystem
            )
        }
            ?: hentAdresse(personinfo)
    }

    private fun hentAdresseForBruker(
        manuellBruker: ManuellBrevmottaker?,
        brukerNavn: String,
        vergeNavn: String?,
        personinfo: Personinfo
    ): Adresseinfo {
        if (manuellBruker != null) {
            val manuellBrukerAdresse = manuellBruker.toManuellAdresse()
            return Adresseinfo(manuellBruker.ident, brukerNavn, vergeNavn, manuellBrukerAdresse)
        } else {
            return Adresseinfo(personinfo.ident, personinfo.navn, vergeNavn)
        }
    }

    private fun hentAdresseForVerge(
        manuellVerge: ManuellBrevmottaker?,
        vergeNavn: String?,
        brukerNavn: String,
        verge: Verge?
    ): Adresseinfo {
        if (manuellVerge != null) {
            val manuellVergeAdresse = manuellVerge.toManuellAdresse()
            return Adresseinfo(manuellVerge.ident, vergeNavn!!, brukerNavn, manuellVergeAdresse)
        } else {
            return Adresseinfo(verge!!.ident ?: verge.orgNr, vergeNavn!!, brukerNavn)
        }
    }

    private fun hentVergeNavn(manuellVerge: ManuellBrevmottaker?, verge: Verge?): String? {
        if (manuellVerge == null && verge == null) return null
        if ((manuellVerge?.vergetype ?: verge?.type) == Vergetype.ADVOKAT) {
            val organisasjon = integrasjonerClient.hentOrganisasjon(manuellVerge?.orgNr ?: verge?.orgNr!!)
            return "${organisasjon.navn} v/ ${manuellVerge?.navn ?: verge?.navn}"
        } else {
            return manuellVerge?.navn ?: verge?.navn
        }
    }

    fun hentAdresse(
        personinfo: Personinfo,
        brevmottager: Brevmottager,
        vergeDto: VergeDto?,
        fagsystem: Fagsystem
    ): Adresseinfo {
        return vergeDto?.let {
            hentAdresse(
                it.vergetype,
                it.organisasjonsnummer,
                it.navn,
                personinfo,
                brevmottager,
                it.personIdent,
                fagsystem
            )
        } ?: hentAdresse(personinfo)
    }

    private fun hentAdresse(
        vergeType: Vergetype,
        organisasjonsnummer: String?,
        vergeNavn: String,
        personinfo: Personinfo,
        brevmottager: Brevmottager,
        vergeIdent: String?,
        fagsystem: Fagsystem
    ): Adresseinfo {
        if (Vergetype.ADVOKAT == vergeType) {
            return hentOrganisasjonsadresse(
                organisasjonsnummer ?: error("organisasjonsnummer er påkrevd for $vergeType"),
                vergeNavn,
                personinfo,
                brevmottager
            )
        } else if (Brevmottager.VERGE == brevmottager) {
            val verge = hentPerson(vergeIdent ?: error("personIdent er påkrevd for $vergeType"), fagsystem)
            if (featureToggleService.isEnabled(FeatureToggleConfig.DSITRIBUER_TIL_MANUELLE_BREVMOTTAKERE)) {
                return Adresseinfo(vergeIdent, verge.navn, personinfo.navn)
            } else {
                return hentAdresse(verge)
            }
        }
        if (featureToggleService.isEnabled(FeatureToggleConfig.DSITRIBUER_TIL_MANUELLE_BREVMOTTAKERE)) {
            return Adresseinfo(personinfo.ident, personinfo.navn, vergeNavn)
        } else {
            return hentAdresse(personinfo)
        }
    }

    private fun hentOrganisasjonsadresse(
        organisasjonsnummer: String,
        vergenavn: String,
        personinfo: Personinfo,
        brevmottager: Brevmottager
    ): Adresseinfo {
        val organisasjon = integrasjonerClient.hentOrganisasjon(organisasjonsnummer)
        if (featureToggleService.isEnabled(FeatureToggleConfig.DSITRIBUER_TIL_MANUELLE_BREVMOTTAKERE)) {
            val organisasjonsMottakerNavn = "${organisasjon.navn} v/ $vergenavn"
            return if (Brevmottager.VERGE == brevmottager) {
                Adresseinfo(organisasjon.organisasjonsnummer, organisasjonsMottakerNavn, personinfo.navn)
            } else {
                Adresseinfo(personinfo.ident, personinfo.navn, organisasjonsMottakerNavn)
            }
        } else {
            return if (Brevmottager.VERGE == brevmottager) {
                Adresseinfo(organisasjon.organisasjonsnummer, organisasjon.navn, vergenavn)
            } else {
                Adresseinfo(personinfo.ident, personinfo.navn)
            }
        }
    }
}
