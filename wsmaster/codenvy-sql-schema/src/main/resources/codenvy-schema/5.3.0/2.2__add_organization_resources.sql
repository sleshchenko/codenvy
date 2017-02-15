--
--  [2012] - [2016] Codenvy, S.A.
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

-- Organization resources  -----------------------------------------------------------
CREATE TABLE organization_resources (
    organization_id       VARCHAR(255)         NOT NULL,

    PRIMARY KEY (organization_id)
);
-- constraints
ALTER TABLE organization_resources ADD CONSTRAINT fk_organization_resources_organization_id FOREIGN KEY (organization_id) REFERENCES organization (id);
--------------------------------------------------------------------------------

-- Organization resources to resources caps ------------------------------------------------
CREATE TABLE organization_resources_caps (
    organization_resources_id               VARCHAR(255)    NOT NULL,
    resource_id                                         BIGINT          NOT NULL,

    PRIMARY KEY (organization_resources_id, resource_id)
);
-- constraints
ALTER TABLE organization_resources_caps ADD CONSTRAINT fk_org_resources_caps_resource_id FOREIGN KEY (resource_id) REFERENCES resource (id);
ALTER TABLE organization_resources_caps ADD CONSTRAINT fk_org_resources_caps_org_resources_id FOREIGN KEY (organization_resources_id) REFERENCES organization_resources (organization_id);
--------------------------------------------------------------------------------

-- Organization resources to reserved resources ------------------------------------------------
CREATE TABLE organization_resources_reserved (
    organization_resources_id               VARCHAR(255)    NOT NULL,
    resource_id                                         BIGINT          NOT NULL,

    PRIMARY KEY (organization_resources_id, resource_id)
);
-- constraints
ALTER TABLE organization_resources_reserved ADD CONSTRAINT fk_org_resources_reverved_resource_id FOREIGN KEY (resource_id) REFERENCES resource (id);
ALTER TABLE organization_resources_reserved ADD CONSTRAINT fk_org_resources_reverved_org_resources_id FOREIGN KEY (organization_resources_id) REFERENCES organization_resources (organization_id);
--------------------------------------------------------------------------------

-- Migrate data from Organization distributed resources table -----------------

INSERT INTO organization_resources(organizationId)
SELECT organization_id
FROM organization_distributed_resources;

INSERT INTO organization_resources_reserved(organization_resources_id, resource_id)
SELECT organization_distributed_resources_id, resource_id
FROM organization_distributed_resources_resource;

DROP TABLE organization_distributed_resources_resource;
DROP TABLE organization_distributed_resources;
