ALTER TABLE accounts
    ADD COLUMN initial_balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00;

UPDATE accounts
SET initial_balance = balance
WHERE initial_balance = 0.00
  AND balance <> 0.00;

CREATE OR REPLACE FUNCTION update_account_balance()
    RETURNS TRIGGER AS
$$
BEGIN
    IF (TG_OP = 'DELETE') THEN
        IF OLD.type = 'INCOME' THEN
            UPDATE accounts SET balance = balance - OLD.amount WHERE id = OLD.account_id;
        ELSIF OLD.type = 'EXPENSE' THEN
            UPDATE accounts SET balance = balance + OLD.amount WHERE id = OLD.account_id;
        END IF;
        RETURN OLD;

    ELSIF (TG_OP = 'UPDATE') THEN
        IF OLD.type = 'INCOME' THEN
            UPDATE accounts SET balance = balance - OLD.amount WHERE id = OLD.account_id;
        ELSIF OLD.type = 'EXPENSE' THEN
            UPDATE accounts SET balance = balance + OLD.amount WHERE id = OLD.account_id;
        END IF;

        IF NEW.type = 'INCOME' THEN
            UPDATE accounts SET balance = balance + NEW.amount WHERE id = NEW.account_id;
        ELSIF NEW.type = 'EXPENSE' THEN
            UPDATE accounts SET balance = balance - NEW.amount WHERE id = NEW.account_id;
        END IF;
        RETURN NEW;

    ELSIF (TG_OP = 'INSERT') THEN
        IF NEW.type = 'INCOME' THEN
            UPDATE accounts SET balance = balance + NEW.amount WHERE id = NEW.account_id;
        ELSIF NEW.type = 'EXPENSE' THEN
            UPDATE accounts SET balance = balance - NEW.amount WHERE id = NEW.account_id;
        END IF;
        RETURN NEW;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_balance
    AFTER INSERT OR UPDATE OR DELETE
    ON transactions
    FOR EACH ROW
EXECUTE FUNCTION update_account_balance();
