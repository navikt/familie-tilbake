DROP INDEX totrinnsresultatsgrunnlag_gruppering_fakta_feilutbetaling_i_idx;

CREATE INDEX totrinnsresultatsgrunnlag_fakta_feilutbetaling_i_idx ON totrinnsresultatsgrunnlag (fakta_feilutbetaling_id);
