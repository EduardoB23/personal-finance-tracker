-- Metas são da família, como contas, cartões e orçamentos (decisão da COD-16). O perfil sai da meta.
ALTER TABLE goals DROP COLUMN profile_id;
