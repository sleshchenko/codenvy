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
package com.codenvy.api.dao.mongo.util;

import org.eclipse.che.api.core.acl.AclEntryImpl;

import java.util.List;

/**
 * @author Sergii Leschenko
 */
public class StackAcl extends AclEntryImpl {
    private final String stack;

    public StackAcl(String stack, String userId, List<String> actions) {
        super(userId, actions);
        this.stack = stack;
    }

    public StackAcl(String stack, AclEntryImpl acl) {
        super(acl.getUser(), acl.getActions());
        this.stack = stack;
    }

    public String getStack() {
        return stack;
    }
}
