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
-- Prepare existing accounts to migration
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
SELECT CONCAT('organization', SUBSTRING(id, 4, length(id))) as org_id, CONCAT('organization', SUBSTRING(id, 4, length(id))) as org_id
FROM Account
WHERE name LIKE 'tomigrate%';

-- TODO Fix adding members
INSERT INTO member
SELECT SUBSTRING(id, 4, length(id)) as memberId,
       CONCAT('organization', SUBSTRING(id, 4, length(id))) as orgId,
       id
FROM Usr
WHERE id LIKE 'user%';

--setPermissions update delete manageSuborganizations manageResources createWorkspaces manageWorkspaces

INSERT INTO Member_actions(member_Id, actions)
SELECT SUBSTRING(id, 4, length(id)) as memberId, 'set_permissions' as actions
FROM Usr
WHERE id LIKE 'user%';

-- Relink workspaces to new accounts
UPDATE Workspace
SET accountid=CONCAT('organization', SUBSTRING(accountid, 4, LENGTH(accountid)))
WHERE accountid like 'user%';

-- Migrate free resources limits
INSERT INTO freeresourceslimit
SELECT CONCAT('organization', SUBSTRING(id, 4, length(id))) as id
FROM Account
WHERE name LIKE 'tomigrate%';

-- Relink resources to new limits
UPDATE freeresourceslimit_resource
SET freeresourceslimit_accountid=CONCAT('organization', SUBSTRING(freeresourceslimit_accountid, 4, LENGTH(freeresourceslimit_accountid)))
WHERE freeresourceslimit_accountid like 'user%';

-- Remove old free resources limits
DELETE FROM freeresourceslimit
WHERE accountid in (SELECT id FROM Account WHERE name LIKE 'tomigrate%');

-- Remove old accounts
DELETE FROM ACCOUNT
WHERE name LIKE 'tomigrate%';
