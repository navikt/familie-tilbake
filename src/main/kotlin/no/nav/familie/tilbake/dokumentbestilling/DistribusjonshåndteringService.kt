package no.nav.familie.tilbake.dokumentbestilling

import no.nav.familie.kontrakter.felles.dokdist.AdresseType
import no.nav.familie.kontrakter.felles.dokdist.ManuellAdresse
import no.nav.familie.kontrakter.felles.tilbakekreving.MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE
import no.nav.familie.kontrakter.felles.tilbakekreving.MottakerType.DØDSBO
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingService
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants.BRUKER_ID_VEDTAKSLØSNINGEN
import no.nav.familie.tilbake.config.FeatureToggleService
import no.nav.familie.tilbake.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata

import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevmottagerUtil
import no.nav.familie.tilbake.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.Brevdata
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.PdfBrevService
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.ManuellBrevmottakerRepository
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.domene.ManuellBrevmottaker
import no.nav.familie.tilbake.dokumentbestilling.vedtak.Vedtaksbrevgrunnlag
import no.nav.familie.tilbake.dokumentbestilling.vedtak.VedtaksbrevgunnlagService
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import no.nav.familie.tilbake.organisasjon.OrganisasjonService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class DistribusjonshåndteringService(
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
    private val manuelleBrevmottakerRepository: ManuellBrevmottakerRepository,
    private val pdfBrevService: PdfBrevService,
    private val eksterneDataForBrevService: EksterneDataForBrevService,
    private val organisasjonService: OrganisasjonService,
    private val vedtaksbrevgrunnlagService: VedtaksbrevgunnlagService,
    private val featureToggleService: FeatureToggleService,
) {

    fun sendBrev(
        behandling: Behandling,
        brevtype: Brevtype,
        varsletBeløp: Long? = null,
        fritekst: String? = null,
        brevdata: (Brevmottager, Brevmetadata?) -> Brevdata
    ) {
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val vedtaksbrevgrunnlag = when (brevtype) {
            Brevtype.VEDTAK -> vedtaksbrevgrunnlagService.hentVedtaksbrevgrunnlag(behandling.id)
            else -> null
        }
        val brevmottakere = utledMottakere(behandling, fagsak).toList()

        brevmottakere.forEachIndexed { index, brevmottaker ->
            pdfBrevService.sendBrev(
                behandling = behandling,
                fagsak = fagsak,
                brevtype = brevtype,
                data = brevdata(
                    brevmottaker.somBrevmottager,
                    genererMetadataForBrev(
                        behandling.id,
                        vedtaksbrevgrunnlag,
                        brevmottager = brevmottaker.somBrevmottager,
                        manuellAdresseinfo = brevmottaker.manuellAdresse,
                        annenMottakersNavn = brevmottakere[brevmottakere.lastIndex - index].navn
                    )
                ),
                varsletBeløp = varsletBeløp,
                fritekst = fritekst
            )
        }
    }

    fun genererMetadataForBrev(
        behandlingId: UUID,
        vedtaksbrevgrunnlag: Vedtaksbrevgrunnlag? = null,
        brevmottager: Brevmottager = Brevmottager.MANUELL,
        manuellAdresseinfo: Adresseinfo? = null,
        annenMottakersNavn: String? = null,
    ): Brevmetadata? {
        require(brevmottager != Brevmottager.MANUELL || manuellAdresseinfo != null)

        val behandling: Behandling by lazy { behandlingRepository.findByIdOrThrow(behandlingId) }
        val fagsak: Fagsak by lazy { fagsakRepository.findByIdOrThrow(behandling.fagsakId) }
        val fagsystem = vedtaksbrevgrunnlag?.fagsystem ?: fagsak.fagsystem

        val aktivVerge = vedtaksbrevgrunnlag?.aktivVerge ?: behandling.aktivVerge

        val personinfo: Personinfo = eksterneDataForBrevService.hentPerson(
            ident = vedtaksbrevgrunnlag?.bruker?.ident ?: fagsak.bruker.ident,
            fagsystem = fagsystem
        )
        val adresseinfo: Adresseinfo = manuellAdresseinfo ?: eksterneDataForBrevService.hentAdresse(
            personinfo = personinfo,
            brevmottager = brevmottager,
            verge = aktivVerge,
            fagsystem = fagsystem
        )
        val vergenavn = BrevmottagerUtil.getVergenavn(aktivVerge, adresseinfo)

        val gjelderDødsfall = personinfo.dødsdato != null

        val persistertSaksbehandlerId =
            vedtaksbrevgrunnlag?.behandling?.ansvarligSaksbehandler ?: behandling.ansvarligSaksbehandler

        val brevmetadata = Brevmetadata(
            sakspartId = personinfo.ident,
            sakspartsnavn = personinfo.navn,
            finnesVerge = aktivVerge != null,
            vergenavn = vergenavn,
            finnesAnnenMottaker = annenMottakersNavn != null || aktivVerge != null,
            annenMottakersNavn = annenMottakersNavn,
            mottageradresse = adresseinfo,
            behandlendeEnhetId = vedtaksbrevgrunnlag?.behandling?.behandlendeEnhet ?: behandling.behandlendeEnhet,
            behandlendeEnhetsNavn = vedtaksbrevgrunnlag?.behandling?.behandlendeEnhetsNavn ?: behandling.behandlendeEnhetsNavn,
            ansvarligSaksbehandler = hentAnsvarligSaksbehandlerNavn(persistertSaksbehandlerId, vedtaksbrevgrunnlag),
            saksnummer = vedtaksbrevgrunnlag?.eksternFagsakId ?: fagsak.eksternFagsakId,
            språkkode = vedtaksbrevgrunnlag?.bruker?.språkkode ?: fagsak.bruker.språkkode,
            ytelsestype = vedtaksbrevgrunnlag?.ytelsestype ?: fagsak.ytelsestype,
            gjelderDødsfall = gjelderDødsfall,
            institusjon = (vedtaksbrevgrunnlag?.institusjon ?: fagsak.institusjon)?.let {
                organisasjonService.mapTilInstitusjonForBrevgenerering(it.organisasjonsnummer)
            }
        )
        return brevmetadata
    }

    fun lagBrevmetadataForMottakerTilForhåndsvisning(
        behandlingId: UUID,
    ): Pair<Brevmetadata?, Brevmottager> {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)

        val (bruker, tilleggsmottaker) = utledMottakere(behandling, fagsak)
        val (brevmottager, manuellAdresseinfo) = when (tilleggsmottaker) {
            null -> bruker.somBrevmottager to bruker.manuellAdresse
            else -> tilleggsmottaker.somBrevmottager to tilleggsmottaker.manuellAdresse
        }
        val metadata = genererMetadataForBrev(
            behandlingId = behandling.id,
            brevmottager = brevmottager,
            manuellAdresseinfo = manuellAdresseinfo,
            annenMottakersNavn = if (tilleggsmottaker != null) {
                eksterneDataForBrevService.hentPerson(fagsak.bruker.ident, fagsak.fagsystem).navn
            } else null
        )
        return metadata to brevmottager
    }

    private fun hentAnsvarligSaksbehandlerNavn(
        persistertSaksbehandlerId: String,
        vedtaksbrevgrunnlag: Vedtaksbrevgrunnlag?
    ): String {
        return if (vedtaksbrevgrunnlag != null) {
            when (vedtaksbrevgrunnlag.aktivtSteg) {
                Behandlingssteg.FORESLÅ_VEDTAK ->
                    eksterneDataForBrevService.hentPåloggetSaksbehandlernavnMedDefault(persistertSaksbehandlerId)
                else ->
                    eksterneDataForBrevService.hentSaksbehandlernavn(persistertSaksbehandlerId)
            }
        } else if (persistertSaksbehandlerId != BRUKER_ID_VEDTAKSLØSNINGEN){
            eksterneDataForBrevService.hentPåloggetSaksbehandlernavnMedDefault(persistertSaksbehandlerId)
        } else {
            ""
        }
    }

    private fun utledMottakere(behandling: Behandling, fagsak: Fagsak): Pair<Any,Any?> {
        if (BehandlingService.sjekkOmManuelleBrevmottakereErStøttet(behandling, fagsak, featureToggleService)) {
            val (manuellBrukeradresse, manuellTilleggsmottaker) = manuelleBrevmottakerRepository.findByBehandlingId(behandling.id)
                .partition { it.type == BRUKER_MED_UTENLANDSK_ADRESSE || it.type == DØDSBO }
            return Pair(manuellBrukeradresse.singleOrNull() ?: Brevmottager.BRUKER, manuellTilleggsmottaker.singleOrNull())
        } else {
            val defaultMottaker = if (fagsak.institusjon != null) Brevmottager.INSTITUSJON else Brevmottager.BRUKER
            val tilleggsmottaker = if (behandling.harVerge) Brevmottager.VERGE else null
            return Pair(defaultMottaker, tilleggsmottaker)
        }
    }
}

val Any?.somBrevmottager: Brevmottager
    get() = this as? Brevmottager ?: Brevmottager.MANUELL
val Any?.navn: String?
    get() = if (this is ManuellBrevmottaker) navn else null
val Any?.manuellAdresse: Adresseinfo?
    get() = if (this is ManuellBrevmottaker)
        Adresseinfo(
            ident = ident.orEmpty(),
            mottagernavn = navn,
            manuellAdresse = if (hasManuellAdresse())
                ManuellAdresse(
                    adresseType = when (landkode) {
                        "NO" -> AdresseType.norskPostadresse
                        else -> AdresseType.utenlandskPostadresse
                    },
                    adresselinje1 = adresselinje1,
                    adresselinje2 = adresselinje2,
                    postnummer = postnummer,
                    poststed = poststed,
                    land = landkode!!,
                ) else null,
        ) else null

