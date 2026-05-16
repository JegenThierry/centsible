-- Fix duplicate "07_" migration version that existed in earlier revisions:
-- 07_AddSystemInformation.sql and everything after it was renumbered (+1).
-- This rewrites the recorded version names on already-applied environments so
-- the runner doesn't try to re-apply the renamed files. On a fresh DB the
-- updates affect zero rows and the rest of the migrations run normally.

UPDATE schema_migrations SET version = '08_AddSystemInformation.sql'    WHERE version = '07_AddSystemInformation.sql';
UPDATE schema_migrations SET version = '09_AddProviderConnections.sql'  WHERE version = '08_AddProviderConnections.sql';
UPDATE schema_migrations SET version = '10_HashRegistrationTokens.sql'  WHERE version = '09_HashRegistrationTokens.sql';
UPDATE schema_migrations SET version = '11_AddRecurringTransactions.sql' WHERE version = '10_AddRecurringTransactions.sql';
UPDATE schema_migrations SET version = '12_AddBudgets.sql'              WHERE version = '11_AddBudgets.sql';
UPDATE schema_migrations SET version = '13_AddTransactionImportHash.sql' WHERE version = '12_AddTransactionImportHash.sql';
