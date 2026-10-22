DO $$
DECLARE
    linhas_atualizadas INTEGER;
BEGIN
    LOOP
        UPDATE pacientes
        SET nome_civil = nome
        WHERE id IN (
            SELECT id
            FROM pacientes
            WHERE nome_civil IS NULL
            LIMIT 1000
        );
        GET DIAGNOSTICS linhas_atualizadas = ROW_COUNT;
        EXIT WHEN linhas_atualizadas = 0;
    END LOOP;
END $$;
