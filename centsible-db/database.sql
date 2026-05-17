SELECT format(
               'CREATE DATABASE %I
                    WITH ENCODING   = ''UTF8''
                         LC_COLLATE = ''en_US.UTF-8''
                         LC_CTYPE   = ''en_US.UTF-8''
                         TEMPLATE   = template0',
               'budget_planner'
       )
WHERE NOT EXISTS (SELECT
                  FROM pg_database
                  WHERE datname = 'budget_planner')
\gexec
