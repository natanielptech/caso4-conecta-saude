CREATE FUNCTION impedir_alteracao_auditoria() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'A tabela de auditoria é append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER auditoria_append_only
BEFORE UPDATE OR DELETE ON auditoria
FOR EACH ROW EXECUTE FUNCTION impedir_alteracao_auditoria();
