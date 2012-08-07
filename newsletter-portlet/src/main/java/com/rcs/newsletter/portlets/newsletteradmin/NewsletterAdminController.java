package com.rcs.newsletter.portlets.newsletteradmin;

import com.rcs.newsletter.commons.GenericController;
import com.rcs.newsletter.commons.JacksonJsonView;
import com.rcs.newsletter.commons.Utils;
import com.rcs.newsletter.core.model.dtos.NewsletterCategoryDTO;
import com.rcs.newsletter.core.service.NewsletterCategoryService;
import com.rcs.newsletter.core.service.common.ListResultsDTO;
import com.rcs.newsletter.core.service.common.ServiceActionResult;
import com.rcs.newsletter.portlets.forms.GridForm;
import java.util.HashMap;
import java.util.Map;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.portlet.ModelAndView;
import org.springframework.web.portlet.bind.annotation.RenderMapping;
import org.springframework.web.portlet.bind.annotation.ResourceMapping;

/**
 *
 * @author ggenovese <gustavo.genovese@rotterdam-cs.com>
 */
@Controller
@RequestMapping("VIEW")
public class NewsletterAdminController extends GenericController {
    
    @Autowired
    private NewsletterCategoryService categoryService;
    
    @RenderMapping
    public ModelAndView initialView(RenderRequest request, RenderResponse response){
        Map<String,Object> model = new HashMap<String,Object>();
        return new ModelAndView("admin/admin", model);
    }
    
    @ResourceMapping("getLists")
    public ModelAndView getLists(ResourceRequest request, @ModelAttribute GridForm gridParams){
        ServiceActionResult<ListResultsDTO<NewsletterCategoryDTO>> sarCategories = 
                categoryService.findAllNewsletterCategories(Utils.getThemeDisplay(request),
                                                            gridParams.calculateStart(),
                                                            gridParams.getPageSize());
        return new ModelAndView (new JacksonJsonView(), JacksonJsonView.MODEL_NAME, sarCategories);
    }
}
