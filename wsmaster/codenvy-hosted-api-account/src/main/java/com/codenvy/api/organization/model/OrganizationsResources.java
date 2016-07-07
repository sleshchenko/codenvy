/*
 *  [2012] - [2016] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.api.organization.model;

import com.codenvy.api.resources.model.Resource;

import java.util.List;

/**
 * Available resources for workspaces of organization can be computed by next algorithm
 * If reserve != null
 *   then reserve
 * else if total == null
 *   then total of parent organization - parent reserve
 * else Total - sum(total of suborganizations)
 *
 * Available resources for redistributing for suborganizations can be computed by next algorithm
 * if (total == null)
 *   then Resources distributing unavailable for current organization
 * else Total - sum(total of suborganizations) - reserve
 *
 * @author Sergii Leschenko
 */
public interface OrganizationsResources {
    /**
     * Returns list of reserved resources for itself
     */
    List<? extends Resource> getReverve();

    /**
     * Returns list of all accessible resource
     */
    List<? extends Resource> getTotal();
}
