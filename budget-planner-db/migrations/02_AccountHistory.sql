CREATE OR REPLACE FUNCTION log_account_history()
    RETURNS TRIGGER AS
$$
BEGIN
    INSERT INTO account_history (account_id,
                                 user_id,
                                 name,
                                 balance,
                                 initial_balance,
                                 currency,
                                 created_at,
                                 modified_at)
    VALUES (OLD.id,
            OLD.user_id,
            OLD.name,
            OLD.balance,
            OLD.initial_balance,
            OLD.currency,
            OLD.created_at,
            OLD.modified_at);

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_account_history
    AFTER UPDATE ON accounts
    FOR EACH ROW
    WHEN (OLD.* IS DISTINCT FROM NEW.*)
EXECUTE FUNCTION log_account_history();
