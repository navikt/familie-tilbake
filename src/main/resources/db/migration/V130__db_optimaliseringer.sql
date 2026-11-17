CREATE INDEX tilbakekreving_ekstern_fagsak_ekstern_id_fra_oppdrag_ytelse_idx ON tilbakekreving_ekstern_fagsak (ekstern_id_fra_oppdrag, ytelse);
CREATE UNIQUE INDEX tilbakekreving_ekstern_fagsak_tilbakekreving_ref_idx ON tilbakekreving_ekstern_fagsak (tilbakekreving_ref);
CREATE INDEX tilbakekreving_ekstern_fagsak_behandling_ekstern_fagsak_ref_idx ON tilbakekreving_ekstern_fagsak_behandling (ekstern_fagsak_ref);
CREATE INDEX tilbakekreving_ekstern_fagsak_beh_utvidet_periode_ref_idx ON tilbakekreving_ekstern_fagsak_behandling_utvidet_periode (ekstern_fagsak_behandling_ref);

CREATE INDEX tilbakekreving_behandling_tilbakekreving_id_idx ON tilbakekreving_behandling (tilbakekreving_id);

CREATE INDEX tilbakekreving_kravgrunnlag_tilbakekreving_id_idx ON tilbakekreving_kravgrunnlag (tilbakekreving_id);
CREATE INDEX tilbakekreving_kravgrunnlag_periode_kravgrunnlag_id_idx ON tilbakekreving_kravgrunnlag_periode (kravgrunnlag_id);
CREATE INDEX tilbakekreving_kravgrunnlag_beløp_kravgrunnlag_periode_id_idx ON tilbakekreving_kravgrunnlag_beløp (kravgrunnlag_periode_id);

CREATE INDEX tilbakekreving_brev_tilbakekreving_ref_idx ON tilbakekreving_brev (tilbakekreving_ref);
CREATE UNIQUE INDEX tilbakekreving_bruker_tilbakekreving_ref_idx ON tilbakekreving_bruker (tilbakekreving_ref);
CREATE INDEX tilbakekreving_behandlingslogg_tilbakekreving_ref_idx ON tilbakekreving_behandlingslogg (tilbakekreving_ref);

CREATE UNIQUE INDEX tilbakekreving_foreldelsesvurdering_behandling_ref_idx ON tilbakekreving_foreldelsesvurdering (behandling_ref);
CREATE INDEX tilbakekreving_foreldelsesvurdering_periode_vurdering_ref_idx ON tilbakekreving_foreldelsesvurdering_periode (foreldelsesvurdering_ref);
CREATE UNIQUE INDEX tilbakekreving_faktavurdering_behandling_ref_idx ON tilbakekreving_faktavurdering (behandling_ref);
CREATE INDEX tilbakekreving_faktavurdering_periode_faktavurdering_ref_idx ON tilbakekreving_faktavurdering_periode (faktavurdering_ref);
CREATE UNIQUE INDEX tilbakekreving_vilkårsvurdering_behandling_ref_idx ON tilbakekreving_vilkårsvurdering (behandling_ref);
CREATE INDEX tilbakekreving_vilkårsvurdering_periode_vurdering_ref_idx ON tilbakekreving_vilkårsvurdering_periode (vurdering_ref);
CREATE UNIQUE INDEX tilbakekreving_totrinnsvurdering_behandling_ref_idx ON tilbakekreving_totrinnsvurdering (behandling_ref);
CREATE INDEX tilbakekreving_totrinnsvurdering_vurdertsteg_ref_idx ON tilbakekreving_totrinnsvurdering_vurdertsteg (totrinnsvurdering_ref);
CREATE UNIQUE INDEX tilbakekreving_foreslåvedtak_behandling_ref_idx ON tilbakekreving_foreslåvedtak (behandling_ref);
CREATE UNIQUE INDEX tilbakekreving_brukeruttalelse_behandling_ref_idx ON tilbakekreving_brukeruttalelse (behandling_ref);
CREATE UNIQUE INDEX tilbakekreving_uttalelse_informasjon_brukeruttalelse_ref_idx ON tilbakekreving_uttalelse_informasjon (brukeruttalelse_ref);
CREATE UNIQUE INDEX tilbakekreving_uttalelsesfrist_behandling_ref_idx ON tilbakekreving_uttalelsesfrist (behandling_ref);
CREATE UNIQUE INDEX tilbakekreving_forhåndsvarsel_unntak_behandling_ref_idx ON tilbakekreving_forhåndsvarsel_unntak (behandling_ref);
CREATE UNIQUE INDEX tilbakekreving_revurdering_behandling_ref_idx ON tilbakekreving_revurdering (behandling_ref);

CREATE INDEX tilbakekreving_neste_påminnelse_idx ON tilbakekreving (neste_påminnelse);

ALTER TABLE tilbakekreving_behandlingslogg
    ADD CONSTRAINT tilbakekreving_behandlingslogg_tilbakekreving_ref_fkey
        FOREIGN KEY (tilbakekreving_ref) REFERENCES tilbakekreving (id);
ALTER TABLE tilbakekreving_behandlingslogg
    ADD CONSTRAINT tilbakekreving_behandlingslogg_behandling_id_fkey
        FOREIGN KEY (behandling_id) REFERENCES tilbakekreving_behandling (id);
ALTER TABLE tilbakekreving_behandling
    ADD CONSTRAINT tilbakekreving_behandling_ekstern_fagsak_behandling_id_fkey
        FOREIGN KEY (ekstern_fagsak_behandling_id) REFERENCES tilbakekreving_ekstern_fagsak_behandling (id);
ALTER TABLE tilbakekreving_varselbrev
    ADD CONSTRAINT tilbakekreving_varselbrev_kravgrunnlag_ref_fkey
        FOREIGN KEY (kravgrunnlag_ref) REFERENCES tilbakekreving_kravgrunnlag (id);
ALTER TABLE tilbakekreving_varselbrev
    ADD CONSTRAINT tilbakekreving_varselbrev_id_fkey
        FOREIGN KEY (id) REFERENCES tilbakekreving_brev (id) ON DELETE CASCADE;
ALTER TABLE tilbakekreving_vedtaksbrev
    ADD CONSTRAINT tilbakekreving_vedtaksbrev_id_fkey
        FOREIGN KEY (id) REFERENCES tilbakekreving_brev (id) ON DELETE CASCADE;
ALTER TABLE tilbakekreving_informasjonsbrev
    ADD CONSTRAINT tilbakekreving_informasjonsbrev_id_fkey
        FOREIGN KEY (id) REFERENCES tilbakekreving_brev (id) ON DELETE CASCADE;
