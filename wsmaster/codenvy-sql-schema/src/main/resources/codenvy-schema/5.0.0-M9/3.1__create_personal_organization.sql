--
--  [2012] - [2017] Codenvy, S.A.
--  All Rights Reserved.
--
-- NOTICE:  All information contained herein is, and remains
-- the property of Codenvy S.A. and its suppliers,
-- if any.  The intellectual and technical concepts contained
-- herein are proprietary to Codenvy S.A.
-- and its suppliers and may be covered by U.S. and Foreign Patents,
-- patents in process, and are protected by trade secret or copyright law.
-- Dissemination of this information or reproduction of this material
-- is strictly forbidden unless prior written permission is obtained
-- from Codenvy S.A..
--
-- Prepate existing accounts to migration
UPDATE Account
SET name=CONCAT('tomigrate',name)
WHERE id LIKE 'user%';

-- Create new accounts
INSERT INTO Account
SELECT CONCAT('organization', SUBSTRING(id, 4, length(id))) as id, SUBSTRING(name, 10, length(name)), 'organizational' as type
FROM Account
WHERE name LIKE 'tomigrate%';

-- Create personal organizations
INSERT INTO Organization(id, account_id)
SELECT id, id
FROM Account
WHERE name NOT LIKE 'tomigrate%';

-- TODO Add permissions for users to their personal organizations

-- Relink workspaces to new accounts
UPDATE Workspace
SET accountid=SUBSTRING(accountid, 4, LENGTH(accountid))
WHERE accountid like 'user%';

-- TODO Migrate free resources limit here

-- Remove old accounts
DELETE FROM ACCOUNT
WHERE name LIKE 'tomigrate%';
