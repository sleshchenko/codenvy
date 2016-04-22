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

import com.codenvy.api.permission.server.PermissionsDomain;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Sergii Leschenko
 */
public class StackDomain extends PermissionsDomain {
    public static final String DOMAIN_ID = "stack";

    public StackDomain() {
        super(DOMAIN_ID, Stream.of(StackAction.values())
                               .map(StackAction::toString)
                               .collect(Collectors.toSet()));
    }
}
