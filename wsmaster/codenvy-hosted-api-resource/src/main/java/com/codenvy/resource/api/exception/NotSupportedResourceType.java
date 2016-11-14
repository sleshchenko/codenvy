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

/**
 * //TODO Add usage of it instead of throwing NotFoundException
 * Should be thrown in case when sent resource with non supported type
 *
 * @author Sergii Leschenko
 */
public class NotSupportedResourceType extends Exception {
    public NotSupportedResourceType(String message) {
        super(message);
    }
}
