package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.FrontendDto
import no.nav.tilbakekreving.api.v1.dto.ManuellBrevmottakerResponsDto
import no.nav.tilbakekreving.kontrakter.brev.Brevmottaker
import no.nav.tilbakekreving.kontrakter.brev.ManuellAdresseInfo
import no.nav.tilbakekreving.kontrakter.brev.MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE
import no.nav.tilbakekreving.kontrakter.brev.MottakerType.DØDSBO
import no.nav.tilbakekreving.kontrakter.brev.MottakerType.FULLMEKTIG
import no.nav.tilbakekreving.kontrakter.brev.MottakerType.VERGE
import no.nav.tilbakekreving.kontrakter.verge.Vergetype
import java.util.UUID

sealed interface RegistrertBrevmottaker : FrontendDto<List<ManuellBrevmottakerResponsDto>> {
    val id: UUID

    fun kombiner(annen: DødsboMottaker): RegistrertBrevmottaker {
        throw IllegalArgumentException("Ugyldig mottaker $annen")
    }

    fun kombiner(annen: VergeMottaker): RegistrertBrevmottaker {
        throw IllegalArgumentException("Ugyldig mottaker $annen")
    }

    fun kombiner(annen: FullmektigMottaker): RegistrertBrevmottaker {
        throw IllegalArgumentException("Ugyldig mottaker $annen")
    }

    fun kombiner(annen: UtenlandskAdresseMottaker): RegistrertBrevmottaker {
        throw IllegalArgumentException("Ugyldig mottaker $annen")
    }

    fun fjernBrevmottaker(
        brevmottakerId: UUID,
        defaultMottaker: RegistrertBrevmottaker,
    ): RegistrertBrevmottaker {
        throw IllegalArgumentException("Ugyldig mottakerId $brevmottakerId")
    }

    class DefaultMottaker(
        override val id: UUID = UUID.randomUUID(),
        val navn: String,
        val personIdent: String? = null,
    ) : RegistrertBrevmottaker {
        override fun kombiner(annen: DødsboMottaker): RegistrertBrevmottaker {
            return annen
        }

        override fun kombiner(annen: VergeMottaker): RegistrertBrevmottaker {
            return DefaultBrukerAdresseOgVergeMottaker(
                id = UUID.randomUUID(),
                defaultMottaker = this,
                verge = annen,
            )
        }

        override fun kombiner(annen: FullmektigMottaker): RegistrertBrevmottaker {
            return DefaultBrukerAdresseOgFullmektigMottaker(
                id = UUID.randomUUID(),
                defaultMottaker = this,
                fullmektig = annen,
            )
        }

        override fun kombiner(annen: UtenlandskAdresseMottaker): RegistrertBrevmottaker {
            return annen
        }

        override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
            return listOf()
        }
    }

    class UtenlandskAdresseMottaker(
        override val id: UUID,
        val navn: String,
        val manuellAdresseInfo: ManuellAdresseInfo? = null,
    ) : RegistrertBrevmottaker {
        override fun kombiner(annen: FullmektigMottaker): RegistrertBrevmottaker {
            return UtenlandskAdresseOgFullmektigMottaker(
                id = UUID.randomUUID(),
                utenlandskAdresse = this,
                fullmektig = annen,
            )
        }

        override fun kombiner(annen: VergeMottaker): RegistrertBrevmottaker {
            return UtenlandskAdresseOgVergeMottaker(
                id = UUID.randomUUID(),
                utenlandskAdresse = this,
                verge = annen,
            )
        }

        override fun kombiner(annen: UtenlandskAdresseMottaker): RegistrertBrevmottaker {
            return annen
        }

        override fun fjernBrevmottaker(
            brevmottakerId: UUID,
            defaultMottaker: RegistrertBrevmottaker,
        ): RegistrertBrevmottaker {
            if (brevmottakerId == id) return defaultMottaker
            throw IllegalArgumentException("Ugyldig mottakerId $brevmottakerId")
        }

        override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
            return listOf(
                ManuellBrevmottakerResponsDto(
                    id = id,
                    brevmottaker = Brevmottaker(
                        type = BRUKER_MED_UTENLANDSK_ADRESSE,
                        vergetype = null,
                        navn = navn,
                        personIdent = null,
                        manuellAdresseInfo = manuellAdresseInfo,
                    ),
                ),
            )
        }
    }

    class FullmektigMottaker(
        override val id: UUID,
        val navn: String,
        val organisasjonsnummer: String? = null,
        val personIdent: String? = null,
        val manuellAdresseInfo: ManuellAdresseInfo? = null,
    ) : RegistrertBrevmottaker {
        override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
            return listOf(
                ManuellBrevmottakerResponsDto(
                    id = id,
                    brevmottaker = Brevmottaker(
                        type = FULLMEKTIG,
                        vergetype = null,
                        navn = navn,
                        organisasjonsnummer = organisasjonsnummer,
                        personIdent = personIdent,
                        manuellAdresseInfo = manuellAdresseInfo,
                    ),
                ),
            )
        }
    }

    class VergeMottaker(
        override val id: UUID,
        val navn: String,
        val vergetype: Vergetype? = null,
        val personIdent: String? = null,
        val manuellAdresseInfo: ManuellAdresseInfo? = null,
    ) : RegistrertBrevmottaker {
        override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
            return listOf(
                ManuellBrevmottakerResponsDto(
                    id = id,
                    brevmottaker = Brevmottaker(
                        type = VERGE,
                        vergetype = vergetype,
                        navn = navn,
                        organisasjonsnummer = null,
                        personIdent = personIdent,
                        manuellAdresseInfo = manuellAdresseInfo,
                    ),
                ),
            )
        }
    }

    class DødsboMottaker(
        override val id: UUID,
        val navn: String,
        val manuellAdresseInfo: ManuellAdresseInfo? = null,
    ) : RegistrertBrevmottaker {
        override fun fjernBrevmottaker(
            brevmottakerId: UUID,
            defaultMottaker: RegistrertBrevmottaker,
        ): RegistrertBrevmottaker {
            if (brevmottakerId == id) {
                return defaultMottaker
            }
            throw IllegalArgumentException("Ugyldig mottakerId $brevmottakerId")
        }

        override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
            return listOf(
                ManuellBrevmottakerResponsDto(
                    id = id,
                    brevmottaker = Brevmottaker(
                        type = DØDSBO,
                        vergetype = null,
                        navn = navn,
                        organisasjonsnummer = null,
                        personIdent = null,
                        manuellAdresseInfo = manuellAdresseInfo,
                    ),
                ),
            )
        }
    }

    class UtenlandskAdresseOgVergeMottaker(
        override val id: UUID,
        var utenlandskAdresse: UtenlandskAdresseMottaker,
        var verge: VergeMottaker,
    ) : RegistrertBrevmottaker {
        override fun kombiner(annen: UtenlandskAdresseMottaker): RegistrertBrevmottaker {
            utenlandskAdresse = annen
            return this
        }

        override fun kombiner(annen: VergeMottaker): RegistrertBrevmottaker {
            verge = annen
            return this
        }

        override fun kombiner(annen: FullmektigMottaker): RegistrertBrevmottaker {
            return UtenlandskAdresseOgFullmektigMottaker(
                id = UUID.randomUUID(),
                utenlandskAdresse = utenlandskAdresse,
                fullmektig = annen,
            )
        }

        override fun fjernBrevmottaker(
            brevmottakerId: UUID,
            defaultMottaker: RegistrertBrevmottaker,
        ): RegistrertBrevmottaker {
            if (utenlandskAdresse.id == brevmottakerId) return defaultMottaker.kombiner(verge)
            if (verge.id == brevmottakerId) return utenlandskAdresse
            throw IllegalArgumentException("Ugyldig mottakerId $brevmottakerId")
        }

        override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
            return utenlandskAdresse.tilFrontendDto() + verge.tilFrontendDto()
        }
    }

    class UtenlandskAdresseOgFullmektigMottaker(
        override val id: UUID,
        var utenlandskAdresse: UtenlandskAdresseMottaker,
        var fullmektig: FullmektigMottaker,
    ) : RegistrertBrevmottaker {
        override fun kombiner(annen: UtenlandskAdresseMottaker): RegistrertBrevmottaker {
            utenlandskAdresse = annen
            return this
        }

        override fun kombiner(annen: FullmektigMottaker): RegistrertBrevmottaker {
            fullmektig = annen
            return this
        }

        override fun kombiner(annen: VergeMottaker): RegistrertBrevmottaker {
            return UtenlandskAdresseOgVergeMottaker(
                id = UUID.randomUUID(),
                utenlandskAdresse = utenlandskAdresse,
                verge = annen,
            )
        }

        override fun fjernBrevmottaker(
            brevmottakerId: UUID,
            defaultMottaker: RegistrertBrevmottaker,
        ): RegistrertBrevmottaker {
            if (utenlandskAdresse.id == brevmottakerId) return defaultMottaker.kombiner(fullmektig)
            if (fullmektig.id == brevmottakerId) return utenlandskAdresse
            throw IllegalArgumentException("Ugyldig mottakerId $brevmottakerId")
        }

        override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
            return utenlandskAdresse.tilFrontendDto() + fullmektig.tilFrontendDto()
        }
    }

    class DefaultBrukerAdresseOgVergeMottaker(
        override val id: UUID,
        var defaultMottaker: DefaultMottaker,
        var verge: VergeMottaker,
    ) : RegistrertBrevmottaker {
        override fun kombiner(annen: VergeMottaker): RegistrertBrevmottaker {
            verge = annen
            return this
        }

        override fun kombiner(annen: UtenlandskAdresseMottaker): RegistrertBrevmottaker {
            return UtenlandskAdresseOgVergeMottaker(
                id = UUID.randomUUID(),
                utenlandskAdresse = annen,
                verge = verge,
            )
        }

        override fun kombiner(annen: FullmektigMottaker): RegistrertBrevmottaker {
            return DefaultBrukerAdresseOgFullmektigMottaker(
                id = UUID.randomUUID(),
                defaultMottaker = defaultMottaker,
                fullmektig = annen,
            )
        }

        override fun fjernBrevmottaker(
            brevmottakerId: UUID,
            defaultMottaker: RegistrertBrevmottaker,
        ): RegistrertBrevmottaker {
            if (verge.id == brevmottakerId) return defaultMottaker
            throw IllegalArgumentException("Ugyldig mottakerId $brevmottakerId")
        }

        override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
            return defaultMottaker.tilFrontendDto() + verge.tilFrontendDto()
        }
    }

    class DefaultBrukerAdresseOgFullmektigMottaker(
        override val id: UUID,
        var defaultMottaker: DefaultMottaker,
        var fullmektig: FullmektigMottaker,
    ) : RegistrertBrevmottaker {
        override fun kombiner(annen: FullmektigMottaker): RegistrertBrevmottaker {
            fullmektig = annen
            return this
        }

        override fun kombiner(annen: UtenlandskAdresseMottaker): RegistrertBrevmottaker {
            return UtenlandskAdresseOgFullmektigMottaker(
                id = UUID.randomUUID(),
                utenlandskAdresse = annen,
                fullmektig = fullmektig,
            )
        }

        override fun kombiner(annen: VergeMottaker): RegistrertBrevmottaker {
            return DefaultBrukerAdresseOgVergeMottaker(
                id = UUID.randomUUID(),
                defaultMottaker = defaultMottaker,
                verge = annen,
            )
        }

        override fun fjernBrevmottaker(
            brevmottakerId: UUID,
            defaultMottaker: RegistrertBrevmottaker,
        ): RegistrertBrevmottaker {
            if (fullmektig.id == brevmottakerId) return defaultMottaker
            throw IllegalArgumentException("Ugyldig mottakerId $brevmottakerId")
        }

        override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
            return defaultMottaker.tilFrontendDto() + fullmektig.tilFrontendDto()
        }
    }
}
