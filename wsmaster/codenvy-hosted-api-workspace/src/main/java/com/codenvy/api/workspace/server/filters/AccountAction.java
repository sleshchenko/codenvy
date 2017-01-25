/*
 *  [2012] - [2017] Codenvy, S.A.
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
package com.codenvy.api.workspace.server.filters;

/**
 * Actions that can be performed by users in accounts.
 *
 * @author Sergii Leshchenko
 */
public enum AccountAction {
    /**
     * When user creates workspace that will belong to account.
     */
    CREATE_WORKSPACE,

    /**
     * When user tries to do any operation with existing workspace.
     */
    MANAGE_WORKSPACES,

    /**
     * When user tries to do any operation with resources.
     */
    SEE_RESOURCE
}
