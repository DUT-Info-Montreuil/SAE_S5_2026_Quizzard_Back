-- UC3 « Insérer une page de quiz dans le cours » (QUIZZARD-39), dossier de conception §4.2.
-- Insérer une page décale d'un rang les pages déjà placées au même endroit (UPDATE ordre = ordre + 1).
-- PostgreSQL vérifie une contrainte UNIQUE ordinaire ligne par ligne : la mise à jour échouerait sur un doublon
-- temporaire. La contrainte est donc vérifiée à la fin de la transaction (DEFERRABLE INITIALLY DEFERRED).
ALTER TABLE page_inseree DROP CONSTRAINT uq_page_inseree_position;
ALTER TABLE page_inseree
    ADD CONSTRAINT uq_page_inseree_position UNIQUE (id_version, numero_page, ordre) DEFERRABLE INITIALLY DEFERRED;
