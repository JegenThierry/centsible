CREATE OR REPLACE FUNCTION log_account_history()
    RETURNS TRIGGER AS
$$
BEGIN
    INSERT INTO account_history (account_id,
                                 user_id,
                                 name,
                                 balance,
                                 currency,
                                 created_at)
    VALUES (OLD.id,
            OLD.user_id,
            OLD.name,
            OLD.balance,
            OLD.currency,
            now());

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_account_history
    AFTER UPDATE ON accounts
    FOR EACH ROW
    WHEN (OLD.* IS DISTINCT FROM NEW.*)
EXECUTE FUNCTION log_account_history();
