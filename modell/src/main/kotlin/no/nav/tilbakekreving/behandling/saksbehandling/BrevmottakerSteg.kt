package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.api.v1.dto.ManuellBrevmottakerResponsDto
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import java.util.UUID

class BrevmottakerSteg(
    private var aktivert: Boolean,
    private val defaultMottaker: RegistrertBrevmottaker,
) : Saksbehandlingsteg<List<ManuellBrevmottakerResponsDto>> {
    override val type = Behandlingssteg.BREVMOTTAKER
    var registrertBrevmottaker: RegistrertBrevmottaker = defaultMottaker

    override fun erFullstending(): Boolean {
        return true
    }

    internal fun håndter(nyBrevmottaker: RegistrertBrevmottaker) {
        if (!aktivert) {
            throw Exception("BrevmottakerSteg er ikke aktivert.")
        }
        registrertBrevmottaker = registrertBrevmottaker.oppdaterRegistrertBrevmottaker(nyBrevmottaker)
        /*if (registrertBrevmottaker == defaultMottaker) {
            registrertBrevmottaker = håndterNyMottaker(nyBrevmottaker)
        } else {
            registrertBrevmottaker = registrertBrevmottaker.oppdaterRegistrertBrevmottaker(nyBrevmottaker)
        }
    }

    private fun håndterNyMottaker(nyBrevmottaker: RegistrertBrevmottaker): RegistrertBrevmottaker =
        when (nyBrevmottaker) {
            is RegistrertBrevmottaker.FullmektigMottaker -> RegistrertBrevmottaker.DefaultBrukerAdresseOgFullmektigMottaker(
                id = UUID.randomUUID(),
                defaultMottaker = defaultMottaker as RegistrertBrevmottaker.DefaultMottaker,
                fullmektig = nyBrevmottaker,
            )

            is RegistrertBrevmottaker.VergeMottaker -> RegistrertBrevmottaker.DefaultBrukerAdresseOgVergeMottaker(
                id = UUID.randomUUID(),
                defaultMottaker = defaultMottaker as RegistrertBrevmottaker.DefaultMottaker,
                verge = nyBrevmottaker,
            )

            else -> nyBrevmottaker

         */
    }

    override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
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

    fun fjernManuellBrevmottaker(manuellBrevmottakerId: UUID) {
        if (!aktivert) {
            throw Exception("BrevmottakerSteg er ikke aktivert.")
        }

        if (registrertBrevmottaker == defaultMottaker) {
            return
        }

        val eksisterende = registrertBrevmottaker

        registrertBrevmottaker = when (eksisterende) {
            is RegistrertBrevmottaker.UtenlandskAdresseOgVergeMottaker ->
                when (manuellBrevmottakerId) {
                    eksisterende.utenlandskAdresse.id -> lagDefaultBrevmottakerOgVergeMottaker(eksisterende.verge)
                    eksisterende.verge.id -> eksisterende.utenlandskAdresse
                    else -> throw IllegalArgumentException("Ugyldig brevmottaker-id: $manuellBrevmottakerId")
                }

            is RegistrertBrevmottaker.UtenlandskAdresseOgFullmektigMottaker ->
                when (manuellBrevmottakerId) {
                    eksisterende.utenlandskAdresse.id -> lagDefaultBrevmottakerOgFullmektigMottaker(eksisterende.fullmektig)
                    eksisterende.fullmektig.id -> eksisterende.utenlandskAdresse
                    else -> throw IllegalArgumentException("Ugyldig brevmottaker-id: $manuellBrevmottakerId")
                }
            is RegistrertBrevmottaker.DefaultBrukerAdresseOgVergeMottaker ->
                when (manuellBrevmottakerId) {
                    eksisterende.verge.id -> eksisterende.defaultMottaker
                    else -> throw IllegalArgumentException("Ugyldig brevmottaker-id: $manuellBrevmottakerId")
                }

            is RegistrertBrevmottaker.DefaultBrukerAdresseOgFullmektigMottaker ->
                when (manuellBrevmottakerId) {
                    eksisterende.fullmektig.id -> eksisterende.defaultMottaker
                    else -> throw IllegalArgumentException("Ugyldig brevmottaker-id: $manuellBrevmottakerId")
                }

            else -> defaultMottaker
        }
    }

    private fun lagDefaultBrevmottakerOgVergeMottaker(
        verge: RegistrertBrevmottaker.VergeMottaker,
    ): RegistrertBrevmottaker.DefaultBrukerAdresseOgVergeMottaker {
        return RegistrertBrevmottaker.DefaultBrukerAdresseOgVergeMottaker(
            id = UUID.randomUUID(),
            defaultMottaker = defaultMottaker as RegistrertBrevmottaker.DefaultMottaker,
            verge = verge,
        )
    }

    private fun lagDefaultBrevmottakerOgFullmektigMottaker(
        fullmektig: RegistrertBrevmottaker.FullmektigMottaker,
    ): RegistrertBrevmottaker.DefaultBrukerAdresseOgFullmektigMottaker {
        return RegistrertBrevmottaker.DefaultBrukerAdresseOgFullmektigMottaker(
            id = UUID.randomUUID(),
            defaultMottaker = defaultMottaker as RegistrertBrevmottaker.DefaultMottaker,
            fullmektig = fullmektig,
        )
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
