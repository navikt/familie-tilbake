package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.FrontendDto
import no.nav.tilbakekreving.api.v1.dto.ManuellBrevmottakerResponsDto
import no.nav.tilbakekreving.entities.MottakerType
import no.nav.tilbakekreving.entities.RegistrertBrevmottakerEntity
import no.nav.tilbakekreving.entities.mapper.tilManuellAdresseInfoEntity
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
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

    fun kombiner(annen: DødsboMottaker, sporing: Sporing): RegistrertBrevmottaker {
        throw ModellFeil.UgyldigOperasjonException("Ugyldig mottaker $annen", sporing)
    }

    fun kombiner(annen: VergeMottaker, sporing: Sporing): RegistrertBrevmottaker {
        throw ModellFeil.UgyldigOperasjonException("Ugyldig mottaker $annen", sporing)
    }

    fun kombiner(annen: FullmektigMottaker, sporing: Sporing): RegistrertBrevmottaker {
        throw ModellFeil.UgyldigOperasjonException("Ugyldig mottaker $annen", sporing)
    }

    fun kombiner(annen: UtenlandskAdresseMottaker, sporing: Sporing): RegistrertBrevmottaker {
        throw ModellFeil.UgyldigOperasjonException("Ugyldig mottaker $annen", sporing)
    }

    fun fjernBrevmottaker(
        brevmottakerId: UUID,
        defaultMottaker: RegistrertBrevmottaker,
        sporing: Sporing,
    ): RegistrertBrevmottaker {
        throw ModellFeil.UgyldigOperasjonException("Ugyldig mottakerId $brevmottakerId", sporing)
    }

    fun tilEntity(): RegistrertBrevmottakerEntity

    class DefaultMottaker(
        override val id: UUID = UUID.randomUUID(),
        val navn: String,
        val personIdent: String? = null,
    ) : RegistrertBrevmottaker {
        override fun kombiner(annen: DødsboMottaker, sporing: Sporing): RegistrertBrevmottaker {
            return annen
        }

        override fun kombiner(annen: VergeMottaker, sporing: Sporing): RegistrertBrevmottaker {
            return DefaultBrukerAdresseOgVergeMottaker(
                id = UUID.randomUUID(),
                defaultMottaker = this,
                verge = annen,
            )
        }

        override fun kombiner(annen: FullmektigMottaker, sporing: Sporing): RegistrertBrevmottaker {
            return DefaultBrukerAdresseOgFullmektigMottaker(
                id = UUID.randomUUID(),
                defaultMottaker = this,
                fullmektig = annen,
            )
        }

        override fun kombiner(annen: UtenlandskAdresseMottaker, sporing: Sporing): RegistrertBrevmottaker {
            return annen
        }

        override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
            return listOf()
        }

        override fun tilEntity(): RegistrertBrevmottakerEntity {
            return RegistrertBrevmottakerEntity(
                mottakerType = MottakerType.DEFAULT_MOTTAKER,
                id = id,
                navn = navn,
                personIdent = personIdent,
                organisasjonsnummer = null,
                vergetype = null,
                manuellAdresseInfoEntity = null,
                defaultMottaker = null,
                utenlandskAdresse = null,
                verge = null,
                fullmektig = null,
            )
        }
    }

    class UtenlandskAdresseMottaker(
        override val id: UUID,
        val navn: String,
        val manuellAdresseInfo: ManuellAdresseInfo? = null,
    ) : RegistrertBrevmottaker {
        override fun kombiner(annen: FullmektigMottaker, sporing: Sporing): RegistrertBrevmottaker {
            return UtenlandskAdresseOgFullmektigMottaker(
                id = UUID.randomUUID(),
                utenlandskAdresse = this,
                fullmektig = annen,
            )
        }

        override fun kombiner(annen: VergeMottaker, sporing: Sporing): RegistrertBrevmottaker {
            return UtenlandskAdresseOgVergeMottaker(
                id = UUID.randomUUID(),
                utenlandskAdresse = this,
                verge = annen,
            )
        }

        override fun kombiner(annen: UtenlandskAdresseMottaker, sporing: Sporing): RegistrertBrevmottaker {
            return annen
        }

        override fun fjernBrevmottaker(
            brevmottakerId: UUID,
            defaultMottaker: RegistrertBrevmottaker,
            sporing: Sporing,
        ): RegistrertBrevmottaker {
            if (brevmottakerId == id) return defaultMottaker
            throw ModellFeil.UgyldigOperasjonException("Ugyldig mottakerId $brevmottakerId", sporing)
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

        override fun tilEntity(): RegistrertBrevmottakerEntity = RegistrertBrevmottakerEntity(
            mottakerType = MottakerType.UTENLANDSK_ADRESSE_MOTTAKER,
            id = this.id,
            navn = navn,
            manuellAdresseInfoEntity = manuellAdresseInfo?.let(::tilManuellAdresseInfoEntity),
            personIdent = null,
            organisasjonsnummer = null,
            vergetype = null,
            defaultMottaker = null,
            utenlandskAdresse = null,
            verge = null,
            fullmektig = null,
        )
    }

    class FullmektigMottaker(
        override val id: UUID,
        val navn: String,
        val organisasjonsnummer: String? = null,
        val personIdent: String? = null,
        val vergeType: Vergetype,
        val manuellAdresseInfo: ManuellAdresseInfo? = null,
    ) : RegistrertBrevmottaker {
        override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
            return listOf(
                ManuellBrevmottakerResponsDto(
                    id = id,
                    brevmottaker = Brevmottaker(
                        type = FULLMEKTIG,
                        vergetype = vergeType,
                        navn = navn,
                        organisasjonsnummer = organisasjonsnummer,
                        personIdent = personIdent,
                        manuellAdresseInfo = manuellAdresseInfo,
                    ),
                ),
            )
        }

        override fun tilEntity(): RegistrertBrevmottakerEntity = RegistrertBrevmottakerEntity(
            mottakerType = MottakerType.FULLMEKTIG_MOTTAKER,
            id = this.id,
            navn = navn,
            personIdent = personIdent,
            organisasjonsnummer = organisasjonsnummer,
            vergetype = vergeType,
            manuellAdresseInfoEntity = manuellAdresseInfo?.let(::tilManuellAdresseInfoEntity),
            defaultMottaker = null,
            utenlandskAdresse = null,
            verge = null,
            fullmektig = null,
        )
    }

    class VergeMottaker(
        override val id: UUID,
        val navn: String,
        val vergeType: Vergetype,
        val personIdent: String? = null,
        val manuellAdresseInfo: ManuellAdresseInfo? = null,
    ) : RegistrertBrevmottaker {
        override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
            return listOf(
                ManuellBrevmottakerResponsDto(
                    id = id,
                    brevmottaker = Brevmottaker(
                        type = VERGE,
                        vergetype = vergeType,
                        navn = navn,
                        organisasjonsnummer = null,
                        personIdent = personIdent,
                        manuellAdresseInfo = manuellAdresseInfo,
                    ),
                ),
            )
        }

        override fun tilEntity(): RegistrertBrevmottakerEntity = RegistrertBrevmottakerEntity(
            mottakerType = MottakerType.VERGE_MOTTAKER,
            id = this.id,
            navn = navn,
            personIdent = personIdent,
            vergetype = vergeType,
            manuellAdresseInfoEntity = manuellAdresseInfo?.let(::tilManuellAdresseInfoEntity),
            organisasjonsnummer = null,
            defaultMottaker = null,
            utenlandskAdresse = null,
            verge = null,
            fullmektig = null,
        )
    }

    class DødsboMottaker(
        override val id: UUID,
        val navn: String,
        val manuellAdresseInfo: ManuellAdresseInfo? = null,
    ) : RegistrertBrevmottaker {
        override fun fjernBrevmottaker(
            brevmottakerId: UUID,
            defaultMottaker: RegistrertBrevmottaker,
            sporing: Sporing,
        ): RegistrertBrevmottaker {
            if (brevmottakerId == id) {
                return defaultMottaker
            }
            throw ModellFeil.UgyldigOperasjonException("Ugyldig mottakerId $brevmottakerId", sporing)
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

        override fun tilEntity(): RegistrertBrevmottakerEntity = RegistrertBrevmottakerEntity(
            mottakerType = MottakerType.DODSBO_MOTTAKER,
            id = this.id,
            navn = navn,
            manuellAdresseInfoEntity = manuellAdresseInfo?.let(::tilManuellAdresseInfoEntity),
            personIdent = null,
            organisasjonsnummer = null,
            vergetype = null,
            defaultMottaker = null,
            utenlandskAdresse = null,
            verge = null,
            fullmektig = null,
        )
    }

    class UtenlandskAdresseOgVergeMottaker(
        override val id: UUID,
        var utenlandskAdresse: UtenlandskAdresseMottaker,
        var verge: VergeMottaker,
    ) : RegistrertBrevmottaker {
        override fun kombiner(annen: UtenlandskAdresseMottaker, sporing: Sporing): RegistrertBrevmottaker {
            utenlandskAdresse = annen
            return this
        }

        override fun kombiner(annen: VergeMottaker, sporing: Sporing): RegistrertBrevmottaker {
            verge = annen
            return this
        }

        override fun kombiner(annen: FullmektigMottaker, sporing: Sporing): RegistrertBrevmottaker {
            return UtenlandskAdresseOgFullmektigMottaker(
                id = UUID.randomUUID(),
                utenlandskAdresse = utenlandskAdresse,
                fullmektig = annen,
            )
        }

        override fun fjernBrevmottaker(
            brevmottakerId: UUID,
            defaultMottaker: RegistrertBrevmottaker,
            sporing: Sporing,
        ): RegistrertBrevmottaker {
            if (utenlandskAdresse.id == brevmottakerId) return defaultMottaker.kombiner(verge, sporing)
            if (verge.id == brevmottakerId) return utenlandskAdresse
            throw ModellFeil.UgyldigOperasjonException("Ugyldig mottakerId $brevmottakerId", sporing)
        }

        override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
            return utenlandskAdresse.tilFrontendDto() + verge.tilFrontendDto()
        }

        override fun tilEntity(): RegistrertBrevmottakerEntity = RegistrertBrevmottakerEntity(
            mottakerType = MottakerType.UTENLANDSK_ADRESSE_OG_VERGE_MOTTAKER,
            id = this.id,
            utenlandskAdresse = utenlandskAdresse.tilEntity(),
            verge = verge.tilEntity(),
            navn = null,
            personIdent = null,
            organisasjonsnummer = null,
            vergetype = null,
            manuellAdresseInfoEntity = null,
            defaultMottaker = null,
            fullmektig = null,
        )
    }

    class UtenlandskAdresseOgFullmektigMottaker(
        override val id: UUID,
        var utenlandskAdresse: UtenlandskAdresseMottaker,
        var fullmektig: FullmektigMottaker,
    ) : RegistrertBrevmottaker {
        override fun kombiner(annen: UtenlandskAdresseMottaker, sporing: Sporing): RegistrertBrevmottaker {
            utenlandskAdresse = annen
            return this
        }

        override fun kombiner(annen: FullmektigMottaker, sporing: Sporing): RegistrertBrevmottaker {
            fullmektig = annen
            return this
        }

        override fun kombiner(annen: VergeMottaker, sporing: Sporing): RegistrertBrevmottaker {
            return UtenlandskAdresseOgVergeMottaker(
                id = UUID.randomUUID(),
                utenlandskAdresse = utenlandskAdresse,
                verge = annen,
            )
        }

        override fun fjernBrevmottaker(
            brevmottakerId: UUID,
            defaultMottaker: RegistrertBrevmottaker,
            sporing: Sporing,
        ): RegistrertBrevmottaker {
            if (utenlandskAdresse.id == brevmottakerId) return defaultMottaker.kombiner(fullmektig, sporing)
            if (fullmektig.id == brevmottakerId) return utenlandskAdresse
            throw ModellFeil.UgyldigOperasjonException("Ugyldig mottakerId $brevmottakerId", sporing)
        }

        override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
            return utenlandskAdresse.tilFrontendDto() + fullmektig.tilFrontendDto()
        }

        override fun tilEntity(): RegistrertBrevmottakerEntity =
            RegistrertBrevmottakerEntity(
                mottakerType = MottakerType.UTENLANDSK_ADRESSE_OG_FULLMEKTIG_MOTTAKER,
                id = id,
                utenlandskAdresse = utenlandskAdresse.tilEntity(),
                fullmektig = fullmektig.tilEntity(),
                navn = null,
                personIdent = null,
                organisasjonsnummer = null,
                vergetype = null,
                manuellAdresseInfoEntity = null,
                defaultMottaker = null,
                verge = null,
            )
    }

    class DefaultBrukerAdresseOgVergeMottaker(
        override val id: UUID,
        var defaultMottaker: DefaultMottaker,
        var verge: VergeMottaker,
    ) : RegistrertBrevmottaker {
        override fun kombiner(annen: VergeMottaker, sporing: Sporing): RegistrertBrevmottaker {
            verge = annen
            return this
        }

        override fun kombiner(annen: UtenlandskAdresseMottaker, sporing: Sporing): RegistrertBrevmottaker {
            return UtenlandskAdresseOgVergeMottaker(
                id = UUID.randomUUID(),
                utenlandskAdresse = annen,
                verge = verge,
            )
        }

        override fun kombiner(annen: FullmektigMottaker, sporing: Sporing): RegistrertBrevmottaker {
            return DefaultBrukerAdresseOgFullmektigMottaker(
                id = UUID.randomUUID(),
                defaultMottaker = defaultMottaker,
                fullmektig = annen,
            )
        }

        override fun fjernBrevmottaker(
            brevmottakerId: UUID,
            defaultMottaker: RegistrertBrevmottaker,
            sporing: Sporing,
        ): RegistrertBrevmottaker {
            if (verge.id == brevmottakerId) return defaultMottaker
            throw ModellFeil.UgyldigOperasjonException("Ugyldig mottakerId $brevmottakerId", sporing)
        }

        override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
            return defaultMottaker.tilFrontendDto() + verge.tilFrontendDto()
        }

        override fun tilEntity(): RegistrertBrevmottakerEntity =
            RegistrertBrevmottakerEntity(
                mottakerType = MottakerType.DEFAULT_BRUKER_ADRESSE_OG_VERGE_MOTTAKER,
                id = id,
                defaultMottaker = defaultMottaker.tilEntity(),
                verge = verge.tilEntity(),
                navn = null,
                personIdent = null,
                organisasjonsnummer = null,
                vergetype = null,
                manuellAdresseInfoEntity = null,
                utenlandskAdresse = null,
                fullmektig = null,
            )
    }

    class DefaultBrukerAdresseOgFullmektigMottaker(
        override val id: UUID,
        var defaultMottaker: DefaultMottaker,
        var fullmektig: FullmektigMottaker,
    ) : RegistrertBrevmottaker {
        override fun kombiner(annen: FullmektigMottaker, sporing: Sporing): RegistrertBrevmottaker {
            fullmektig = annen
            return this
        }

        override fun kombiner(annen: UtenlandskAdresseMottaker, sporing: Sporing): RegistrertBrevmottaker {
            return UtenlandskAdresseOgFullmektigMottaker(
                id = UUID.randomUUID(),
                utenlandskAdresse = annen,
                fullmektig = fullmektig,
            )
        }

        override fun kombiner(annen: VergeMottaker, sporing: Sporing): RegistrertBrevmottaker {
            return DefaultBrukerAdresseOgVergeMottaker(
                id = UUID.randomUUID(),
                defaultMottaker = defaultMottaker,
                verge = annen,
            )
        }

        override fun fjernBrevmottaker(
            brevmottakerId: UUID,
            defaultMottaker: RegistrertBrevmottaker,
            sporing: Sporing,
        ): RegistrertBrevmottaker {
            if (fullmektig.id == brevmottakerId) return defaultMottaker
            throw ModellFeil.UgyldigOperasjonException("Ugyldig mottakerId $brevmottakerId", sporing)
        }

        override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
            return defaultMottaker.tilFrontendDto() + fullmektig.tilFrontendDto()
        }

        override fun tilEntity(): RegistrertBrevmottakerEntity =
            RegistrertBrevmottakerEntity(
                mottakerType = MottakerType.DEFAULT_BRUKER_ADRESSE_OG_FULLMEKTIG_MOTTAKER,
                id = id,
                defaultMottaker = defaultMottaker.tilEntity(),
                fullmektig = fullmektig.tilEntity(),
                navn = null,
                personIdent = null,
                organisasjonsnummer = null,
                vergetype = null,
                manuellAdresseInfoEntity = null,
                utenlandskAdresse = null,
                verge = null,
            )
    }
}
