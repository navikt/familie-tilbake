package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.api.v1.dto.ManuellBrevmottakerResponsDto
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import java.util.UUID

class BrevmottakerSteg(
    private var aktivert: Boolean,
    private val defaultMottaker: RegistrertBrevmottaker,
) : Saksbehandlingsteg {
    override val type = Behandlingssteg.BREVMOTTAKER
    var registrertBrevmottaker: RegistrertBrevmottaker = defaultMottaker

    override fun erFullstendig(): Boolean {
        return true
    }

    internal fun håndter(nyBrevmottaker: RegistrertBrevmottaker, sporing: Sporing) {
        if (!aktivert) {
            throw ModellFeil.UgyldigOperasjonException("BrevmottakerSteg er ikke aktivert.", sporing)
        }
        registrertBrevmottaker = when (nyBrevmottaker) {
            is RegistrertBrevmottaker.DødsboMottaker -> registrertBrevmottaker.kombiner(nyBrevmottaker, sporing)
            is RegistrertBrevmottaker.FullmektigMottaker -> registrertBrevmottaker.kombiner(nyBrevmottaker, sporing)
            is RegistrertBrevmottaker.UtenlandskAdresseMottaker -> registrertBrevmottaker.kombiner(nyBrevmottaker, sporing)
            is RegistrertBrevmottaker.VergeMottaker -> registrertBrevmottaker.kombiner(nyBrevmottaker, sporing)
            else -> throw ModellFeil.UgyldigOperasjonException(
                "Kan ikke motta brevmottaker av type ${nyBrevmottaker.javaClass.simpleName}.",
                sporing,
            )
        }
    }

    fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
        return registrertBrevmottaker.tilFrontendDto()
    }

    fun aktiverSteg() {
        aktivert = true
    }

    fun deaktiverSteg() {
        registrertBrevmottaker = defaultMottaker
        aktivert = false
    }

    fun erStegetAktivert() = aktivert

    fun hentRegistrertBrevmottaker() = registrertBrevmottaker

    fun fjernManuellBrevmottaker(brevmottakerId: UUID, sporing: Sporing) {
        if (!aktivert) {
            throw ModellFeil.UgyldigOperasjonException("BrevmottakerSteg er ikke aktivert.", sporing)
        }

        if (registrertBrevmottaker == defaultMottaker) {
            throw ModellFeil.UgyldigOperasjonException("Kan ikke fjerne defaultMotatker.", sporing)
        }

        registrertBrevmottaker = registrertBrevmottaker.fjernBrevmottaker(brevmottakerId, defaultMottaker, sporing)
    }

    companion object {
        fun opprett(
            navn: String,
            ident: String,
        ) = BrevmottakerSteg(
            aktivert = false,
            RegistrertBrevmottaker.DefaultMottaker(navn = navn, personIdent = ident),
        )
    }
}
