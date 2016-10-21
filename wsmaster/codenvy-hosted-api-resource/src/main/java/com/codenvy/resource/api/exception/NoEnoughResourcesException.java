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
package com.codenvy.resource.api.exception;

import com.codenvy.resource.model.Resource;
import com.codenvy.resource.spi.impl.ResourceImpl;

import java.util.Collections;
import java.util.List;

/**
 * Should be thrown in case when account doesn't have enough resources to perform some operation
 *
 * <p>It contains detailed information about resources, required, available, missing.
 *
 * @author Sergii Leschenko
 */
public class NoEnoughResourcesException extends Exception {
    //TODO Rework this exception
    private static final String MESSAGE = "Your account doesn't have enough resources to use.";

    private List<Resource> requiredResources;

    public NoEnoughResourcesException(Resource requiredResources) {
        super(MESSAGE);
        this.requiredResources = Collections.singletonList(requiredResources);
    }

    public NoEnoughResourcesException(List<Resource> requiredResources) {
        super(MESSAGE);
        this.requiredResources = requiredResources;
    }

    public List<Resource> getRequiredResources() {
        return requiredResources;
    }
}
