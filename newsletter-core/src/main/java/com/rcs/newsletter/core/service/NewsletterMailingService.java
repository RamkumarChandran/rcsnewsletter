package com.rcs.newsletter.core.service;

import com.liferay.portal.theme.ThemeDisplay;
import com.rcs.newsletter.core.model.NewsletterMailing;
import com.rcs.newsletter.core.model.NewsletterTemplate;

/**
 *
 * @author juan
 */
public interface NewsletterMailingService extends CRUDService<NewsletterMailing> {

    /**
     * Send the mailing to a test email address.
     * @param mailingId
     * @param themeDisplay 
     * @param testEmail
     */
    void sendTestMailing(Long mailingId, String testEmail, ThemeDisplay themeDisplay);

    /**
     * Send the mailing to everyone.
     * @param mailingId
     * @param themeDisplay 
     */
    void sendMailing(Long mailingId, ThemeDisplay themeDisplay, Long archiveId);
    
    /**
     * 
     * @param mailingId
     * @param themeDisplay
     * @return 
     */
    String getEmailFromTemplate(Long mailingId, ThemeDisplay themeDisplay); 
    
    /**
     * Validates the template format
     * @param mailingId
     * @return 
     */
    boolean validateTemplateFormat(Long mailingId);
}
