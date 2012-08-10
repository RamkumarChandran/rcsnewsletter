package com.rcs.newsletter.portlets.newsletteradmin;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.LiferayPortletResponse;
import com.liferay.portal.kernel.servlet.HttpHeaders;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.theme.ThemeDisplay;
import com.rcs.newsletter.commons.NewsletterResourcePortlet;
import com.rcs.newsletter.commons.Utils;
import com.rcs.newsletter.core.model.dtos.NewsletterSubscriptionDTO;
import com.rcs.newsletter.core.model.enums.SubscriptionStatus;
import com.rcs.newsletter.core.service.NewsletterSubscriptionService;
import com.rcs.newsletter.core.service.NewsletterSubscriptorService;
import com.rcs.newsletter.core.service.common.ListResultsDTO;
import com.rcs.newsletter.core.service.common.ServiceActionResult;
import com.rcs.newsletter.util.ExcelExporterUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.poi.hssf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 *
 * @author Ariel Parra <ariel@rotterdam-cs.com>
 */
@Component
public class SubscriptorsResourceUtil {

    @Autowired
    private NewsletterSubscriptorService subscriptorService;
    
    @Autowired
    private NewsletterSubscriptionService subscriptionService;

    private static final Log logger = LogFactoryUtil.getLog(NewsletterResourcePortlet.class);

    private static final String ID_COLUMN = "Id";

    private static final String NAME_COLUMN = "Name";

    private static final String LAST_NAME_COLUMN = "Last Name";

    private static final String EMAIL_COLUMN = "Email";

    private static final String LIST_COLUMN = "List";

    private static final int ID_INDEX = 0;

    private static final int NAME_INDEX = 1;

    private static final int LAST_NAME_INDEX = 2;

    private static final int EMAIL_INDEX = 3;

    private static final int LIST_INDEX = 4;

    public void writeSubscriptorsExcel(PortletRequest request, long categoryId, ThemeDisplay themeDisplay, PortletResponse response) {

        ResourceBundle messageBundle = ResourceBundle.getBundle("Language", Utils.getCurrentLocale(request));
        String categoryName = messageBundle.getString("newsletter.admin.general.undefined");
        String fileName = messageBundle.getString("newsletter.admin.subscribers");

        ServiceActionResult<ListResultsDTO<NewsletterSubscriptionDTO>> sarSubscriptions = subscriptorService.findAllByStatusAndCategory(themeDisplay, -1, -1, "id", "asc", SubscriptionStatus.ACTIVE, categoryId);

        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet();
        HSSFRow row = sheet.createRow((short) 0);
        HSSFCellStyle cellStyle = ExcelExporterUtil.createHeaderStyle(workbook);

        HSSFCell cell1 = row.createCell(ID_INDEX);
        cell1.setCellValue(ID_COLUMN);
        cell1.setCellStyle(cellStyle);

        HSSFCell cell2 = row.createCell(NAME_INDEX);
        cell2.setCellValue(NAME_COLUMN);
        cell2.setCellStyle(cellStyle);

        HSSFCell cell3 = row.createCell(LAST_NAME_INDEX);
        cell3.setCellValue(LAST_NAME_COLUMN);
        cell3.setCellStyle(cellStyle);

        HSSFCell cell4 = row.createCell(EMAIL_INDEX);
        cell4.setCellValue(EMAIL_COLUMN);
        cell4.setCellStyle(cellStyle);

        HSSFCell cell5;
        if (categoryId != 0) {
            cell5 = row.createCell(LIST_INDEX);
            cell5.setCellValue(LIST_COLUMN);
            cell5.setCellStyle(cellStyle);
        }

        int index = 1;
        for (NewsletterSubscriptionDTO subscription : sarSubscriptions.getPayload().getResult()) {
            row = sheet.createRow((short) index);
            row.createCell(0).setCellValue(subscription.getSubscriptorId());
            row.createCell(1).setCellValue(subscription.getSubscriptorFirstName());
            row.createCell(2).setCellValue(subscription.getSubscriptorLastName());
            row.createCell(3).setCellValue(subscription.getSubscriptorEmail());

            if (categoryId != 0) {
                row.createCell(4).setCellValue(categoryName);
            }
            index++;
        }

        OutputStream os = null;
        try {
            HttpServletResponse servletResponse = ((LiferayPortletResponse) response).getHttpServletResponse();

            servletResponse.setContentType(ContentTypes.TEXT_XML_UTF8);
            servletResponse.addHeader(HttpHeaders.CACHE_CONTROL, "must-revalidate, post-check=0, pre-check=0");
            servletResponse.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=\"" + fileName + ".xls\"");
            servletResponse.addHeader(HttpHeaders.PRAGMA, "public");
            servletResponse.addHeader(HttpHeaders.EXPIRES, "0");

            os = servletResponse.getOutputStream();
            workbook.write(os);
        } catch (IOException ex) {
        } finally {
            try {
                os.close();
            } catch (IOException ex) {
            }
        }
    }

    public static boolean isValidEmailAddress(String email) {
        if (!StringUtils.hasText(email)){
            return false;
        }

        try {
            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate();
            return true;
        } catch (AddressException ex) {
            return false;
        }
    }

    public List<String> importSubscriptorsFromExcel(InputStream inputStream, long categoryId, ThemeDisplay themeDisplay) {
        List<String> ret = new LinkedList<String>();
        try {
            HSSFWorkbook workbook = new HSSFWorkbook(inputStream);

            if (workbook == null){
                ret.add("Error loading the workbook");
                return ret;
            }

            if ( categoryId == 0) {
                ret.add("Error loading the list");
                return ret;
            }

            HSSFSheet sheet = workbook.getSheetAt(0);
            List<NewsletterSubscriptionDTO> newSubscriptions = new LinkedList<NewsletterSubscriptionDTO>();
            
            for (int i = 0; i < sheet.getPhysicalNumberOfRows(); i++) {
                HSSFRow row = sheet.getRow(i);

                String firstName = "";
                String lastName = "";
                String email = "";

                HSSFCell nameCell = row.getCell(NAME_INDEX);
                if (nameCell != null && nameCell.getCellType() == HSSFCell.CELL_TYPE_STRING && StringUtils.hasText(nameCell.getStringCellValue())) {
                    firstName = nameCell.getStringCellValue();
                }else{
                    ret.add(String.format("Error loading first name from row %d", i + 1));
                    continue;
                }

                HSSFCell lastNameCell = row.getCell(LAST_NAME_INDEX);
                if (lastNameCell != null && lastNameCell.getCellType() == HSSFCell.CELL_TYPE_STRING && StringUtils.hasText(lastNameCell.getStringCellValue())) {
                    lastName = lastNameCell.getStringCellValue();
                }else{
                    ret.add(String.format("Error loading last name from row %d", i + 1));
                    continue;
                }
                
                HSSFCell emailCell = row.getCell(EMAIL_INDEX);
                if (emailCell != null && emailCell.getCellType() == HSSFCell.CELL_TYPE_STRING && isValidEmailAddress(emailCell.getStringCellValue().trim())) {
                    email = emailCell.getStringCellValue().trim();
                }else{
                    ret.add(String.format("Error loading email address from row %d", i + 1));
                    continue;                    
                }
                NewsletterSubscriptionDTO newSubscription = new NewsletterSubscriptionDTO();
                newSubscription.setSubscriptorFirstName(firstName);
                newSubscription.setSubscriptorLastName(lastName);
                newSubscription.setSubscriptorEmail(email);
                newSubscriptions.add(newSubscription);
            }
            ServiceActionResult result = subscriptionService.createSubscriptionsForCategory(themeDisplay, categoryId, newSubscriptions);
            ret.addAll(result.getMessages());
            return ret;
        } catch (IOException ex) {
            logger.error("Error in importSubscriptorsFromExcel " + ex);
            return null;
        }
    }
}