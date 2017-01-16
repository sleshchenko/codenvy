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

-- admin@codenvy.onprem
-- root-prod

-- Rename existing account to make free old account's names
UPDATE Account
SET name=CONCAT('tomigrate', name)
WHERE id LIKE 'user%' or id LIKE 'User%';

-- Create new organizational accounts
INSERT INTO Account(id, name, type)
SELECT CONCAT('organization', SUBSTRING(id, 4, length(id))) as newid,
       SUBSTRING(name, 10, length(name)),
       'organizational' as type
FROM Account
WHERE id LIKE 'user%' or id LIKE 'User%';

-- Create personal organizations
INSERT INTO Organization(id, account_id)
SELECT CONCAT('organization', SUBSTRING(id, 4, length(id))) as org_id,
       CONCAT('organization', SUBSTRING(id, 4, length(id))) as org_id
FROM Account
WHERE id LIKE 'user%' or id LIKE 'User%';

-- TODO Revise member id
-- Add users permissions in their personal organizations
-- Create members
INSERT INTO member(id, organizationid, userid)
SELECT SUBSTRING(id, 4, length(id)) as memberId,
       CONCAT('organization', SUBSTRING(id, 4, length(id))) as orgId,
       id
FROM Account
WHERE id LIKE 'user%' or id LIKE 'User%';

-- Add actions to members
CREATE TEMP TABLE Actions (action VARCHAR(255));
INSERT INTO ACTIONS VALUES ('setPermissions'),
                           ('update'),
                           ('delete'),
                           ('manageSuborganizations'),
                           ('manageResources'),
                           ('createWorkspaces'),
                           ('manageWorkspaces');

INSERT INTO Member_actions(member_id, actions)
SELECT SUBSTRING(id, 4, length(id)) as member_id, action
FROM Account as a, actions as orgActions
WHERE a.id LIKE 'user%' or a.id LIKE 'User%';

-- Relink workspaces to new accounts
UPDATE Workspace
SET accountid=CONCAT('organization', SUBSTRING(accountid, 4, LENGTH(accountid)))
WHERE accountid like 'user%' or accountid like 'User%';

-- Relink free resources limits to new accounts
INSERT INTO freeresourceslimit
SELECT CONCAT('organization', SUBSTRING(accountid, 4, length(accountid))) as accountid
FROM freeresourceslimit
WHERE accountid like 'user%' or accountid like 'User%';

-- Relink resources to new limits
UPDATE freeresourceslimit_resource
SET freeresourceslimit_accountid=CONCAT('organization', SUBSTRING(freeresourceslimit_accountid, 4, LENGTH(freeresourceslimit_accountid)))
WHERE freeresourceslimit_accountid like 'user%' or freeresourceslimit_accountid like 'User%';

-- Remove old free resources limits
DELETE FROM freeresourceslimit
WHERE accountid in (SELECT id FROM Account WHERE id LIKE 'user%' or id LIKE 'User%');
