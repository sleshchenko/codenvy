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
package com.codenvy.api.permission.shared;

import java.util.List;

/**
 * Represents users' permissions to access to some resources
 *
 * @author Sergii Leschenko
 */
public interface Permissions {
    /**
     * Returns used id
     */
    String getUser();

    /**
     * Returns domain id
     */
    String getDomain();

    /**
     * Returns instance id
     */
    String getInstance();

    /**
     * List of actions which user can perform for particular instance
     */
    List<String> getActions();
}
