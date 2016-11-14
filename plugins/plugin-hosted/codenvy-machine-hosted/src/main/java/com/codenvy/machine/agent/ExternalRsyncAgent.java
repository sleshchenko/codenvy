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
package com.codenvy.machine.agent;

import org.eclipse.che.api.agent.server.model.impl.AgentImpl;

import java.util.Collections;

/**
 * Restores workspace projects into machine using external sync mechanism.
 *
 * @author Alexander Garagatyi
 */
public class ExternalRsyncAgent extends AgentImpl {
    public ExternalRsyncAgent() {
        super("com.codenvy.external_rsync",
              "Rsync sync agent that uses rsync on host",
              null,
              "Sync support",
              Collections.emptyList(),
              Collections.emptyMap(),
              null,
              null);
    }
}
