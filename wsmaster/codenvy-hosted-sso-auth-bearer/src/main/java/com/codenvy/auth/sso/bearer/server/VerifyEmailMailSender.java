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
package com.codenvy.auth.sso.bearer.server;

import com.codenvy.mail.Attachment;
import com.codenvy.mail.EmailBean;
import com.codenvy.mail.MailSender;
import com.google.common.io.Files;

import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.commons.lang.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonList;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.eclipse.che.commons.lang.IoUtil.getResource;
import static org.eclipse.che.commons.lang.IoUtil.readAndCloseQuietly;

/**
 * @author Sergii Leschenko
 */
public class VerifyEmailMailSender {
    private static final Logger LOG = LoggerFactory.getLogger(VerifyEmailMailSender.class);

    // TODO made this configurable
    private static final String MAIL_TEMPLATE = "email-templates/verify_email_address.html";
    private static final String LOGO          = "/email-templates/header.png";
    private static final String LOGO_CID      = "codenvyLogo";

    private final MailSender mailSender;
    private final String     mailFrom;

    @Inject
    public VerifyEmailMailSender(MailSender mailSender,
                                 @Named("mailsender.application.from.email.address") String mailFrom) {
        this.mailSender = mailSender;
        this.mailFrom = mailFrom;
    }

    public void sendConfirmationEmail(String email, String bearerToken, String queryParams, String baseHost) throws ServerException {
        Map<String, String> props = new HashMap<>();
        props.put("logo.cid", "codenvyLogo");
        props.put("bearertoken", bearerToken);
        props.put("additional.query.params", queryParams);
        props.put("com.codenvy.masterhost.url", baseHost);

        EmailBean emailBean = new EmailBean().withBody(Deserializer.resolveVariables(getTemplate(), props))
                                             .withFrom(mailFrom)
                                             .withTo(email)
                                             .withReplyTo(null)
                                             .withSubject("Verify Your Codenvy Account")
                                             .withMimeType(TEXT_HTML)
                                             .withAttachments(singletonList(
                                                     new Attachment().withContent(Base64.getEncoder().encodeToString(getLogo()))
                                                                     .withContentId(LOGO_CID)
                                                                     .withFileName("logo.png")));
        mailSender.sendMail(emailBean);
        LOG.info("Email validation message send to {}", email);
    }

    private byte[] getLogo() throws ServerException {
        try {
            return Files.toByteArray(new File(this.getClass().getResource(LOGO).getPath()));
        } catch (IOException e) {
            throw new ServerException(e.getLocalizedMessage(), e);
        }
    }

    private String getTemplate() throws ServerException {
        try {
            return readAndCloseQuietly(getResource("/" + MAIL_TEMPLATE));
        } catch (IOException e) {
            throw new ServerException(e.getLocalizedMessage(), e);
        }
    }
}
