package no.tilbakekreving.integrasjoner.persontilgang

import no.tilbakekreving.integrasjoner.CallContext
import no.tilbakekreving.integrasjoner.tilgangsmaskinen.TilgangsmaskinenClient
import no.tilbakekreving.integrasjoner.tilgangsmaskinen.kontrakter.AvvisningskodeDTO

internal class PersontilgangServiceImpl internal constructor(
    private val client: TilgangsmaskinenClient,
) : PersontilgangService {
    override suspend fun sjekkPersontilgang(
        callContext: CallContext.Saksbehandler,
        personIdent: String,
    ): Persontilgang {
        return when (val tilgang = client.hentPersontilgang(callContext, personIdent)) {
            null -> Persontilgang.Ok
            else -> Persontilgang.IkkeTilgang(
                begrunnelse = tilgang.begrunnelse,
                begrunnelseType = when (tilgang.title) {
                    AvvisningskodeDTO.AVVIST_STRENGT_FORTROLIG_ADRESSE -> Persontilgang.IkkeTilgang.AvvisningskodeType.AVVIST_STRENGT_FORTROLIG_ADRESSE
                    AvvisningskodeDTO.AVVIST_STRENGT_FORTROLIG_UTLAND -> Persontilgang.IkkeTilgang.AvvisningskodeType.AVVIST_STRENGT_FORTROLIG_UTLAND
                    AvvisningskodeDTO.AVVIST_PERSON_UTLAND -> Persontilgang.IkkeTilgang.AvvisningskodeType.AVVIST_PERSON_UTLAND
                    AvvisningskodeDTO.AVVIST_SKJERMING -> Persontilgang.IkkeTilgang.AvvisningskodeType.AVVIST_SKJERMING
                    AvvisningskodeDTO.AVVIST_FORTROLIG_ADRESSE -> Persontilgang.IkkeTilgang.AvvisningskodeType.AVVIST_FORTROLIG_ADRESSE
                    AvvisningskodeDTO.AVVIST_HABILITET -> Persontilgang.IkkeTilgang.AvvisningskodeType.AVVIST_HABILITET
                    AvvisningskodeDTO.AVVIST_AVDØD,
                    AvvisningskodeDTO.AVVIST_UKJENT_BOSTED,
                    AvvisningskodeDTO.AVVIST_GEOGRAFISK,
                    -> Persontilgang.IkkeTilgang.AvvisningskodeType.UKJENT
                },
            )
        }
    }
}
