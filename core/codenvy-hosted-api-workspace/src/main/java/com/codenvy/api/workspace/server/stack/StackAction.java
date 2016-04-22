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
package com.codenvy.api.workspace.server.stack;

/**
 * @author Sergii Leschenko
 */
public enum StackAction {
    SET_PERMISSIONS("setPermissions"),
    READ_PERMISSIONS("readPermissions"),
    READ("read"),
    UPDATE("update"),
    DELETE("delete");

    private final String action;

    StackAction(String action) {
        this.action = action;
    }

    @Override
    public String toString() {
        return action;
    }
}
