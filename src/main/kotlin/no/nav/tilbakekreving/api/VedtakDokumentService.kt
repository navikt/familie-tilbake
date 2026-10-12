package no.nav.tilbakekreving.api

import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksdokumentDto
import org.springframework.stereotype.Service
import java.math.BigInteger
import java.util.UUID

@Service
class VedtakDokumentService(
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val brevsporingRepository: BrevsporingRepository,
) {
    fun hentDokumentreferanserGammelModell(
        behandlingId: UUID,
    ): List<VedtaksdokumentDto> {
        return brevsporingRepository.findAllByBehandlingId(behandlingId)
            .map { VedtaksdokumentDto(it.journalpostId, it.dokumentId) }
    }

    fun hentBehandlingIderGammelModell(vedtakId: BigInteger): List<UUID> =
        kravgrunnlagRepository.findBehandlingIdsByVedtakId(vedtakId)
}
