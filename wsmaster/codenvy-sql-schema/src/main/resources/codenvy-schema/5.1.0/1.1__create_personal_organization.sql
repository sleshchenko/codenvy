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

-- Rename existing account to make free old account's names
UPDATE Account
SET name=CONCAT('tomigrate', name)
WHERE type = 'personal';

-- TODO Remove this note
-- In dump every user id starts with User or user except
-- admin@codenvy.onprem
-- root-prod
-- In case ldap synchonization format of ids in unknown

-- Create new organizational accounts
INSERT INTO Account(id, name, type)
SELECT CONCAT('organization', REPLACE_REGEXP(id, '[uU]ser', '')),
       SUBSTRING(name, 10, length(name)),
       'organizational'
FROM Account
WHERE type = 'personal';

-- Create personal organizations
INSERT INTO Organization(id, account_id)
SELECT CONCAT('organization', REPLACE_REGEXP(id, '[uU]ser', '')),
       CONCAT('organization', REPLACE_REGEXP(id, '[uU]ser', ''))
FROM Account
WHERE type = 'personal';

-- TODO Revise member id
-- Add users permissions in their personal organizations
-- Create members
INSERT INTO member(id, organizationid, userid)
SELECT CONCAT('member', REPLACE_REGEXP(id, '[uU]ser', '')),
       CONCAT('organization', REPLACE_REGEXP(id, '[uU]ser', '')),
       id
FROM Account
WHERE type = 'personal';

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
SELECT CONCAT('member', REPLACE_REGEXP(id, '[uU]ser', '')),
       action
FROM Account as a, Actions as orgActions
WHERE a.type = 'personal';

-- Relink workspaces to new accounts
UPDATE Workspace
SET accountid=(SELECT CONCAT('organization', REPLACE_REGEXP(a.id, '[uU]ser', ''))
               FROM Account a
               WHERE workspace.accountid = a.id AND a.type = 'personal');

-- Relink free resources limits to new accounts
INSERT INTO freeresourceslimit(accountid)
SELECT CONCAT('organization', REPLACE_REGEXP(accountid, '[uU]ser', ''))
FROM freeresourceslimit l
JOIN Account a ON a.id = l.accountid
WHERE a.type = 'personal';

-- Relink resources to new limits
UPDATE freeresourceslimit_resource
SET freeresourceslimit_accountid=(SELECT CONCAT('organization', REPLACE_REGEXP(a.id, '[uU]ser', ''))
                                  FROM Account a
                                  WHERE freeresourceslimit_resource.freeresourceslimit_accountid = a.id AND a.type = 'personal');

-- Remove old free resources limits
DELETE FROM freeresourceslimit
WHERE freeresourceslimit.accountid IN (SELECT id
                                       FROM Account
                                       WHERE type = 'personal');
