/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.wcm.client.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.wcm.client.Asset;
import org.alfresco.wcm.client.Section;
import org.alfresco.wcm.client.WebSite;
import org.alfresco.wcm.client.WebSiteService;
import org.springframework.extensions.surf.RequestContext;
import org.springframework.extensions.surf.support.ThreadLocalRequestContext;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * Load application-wide data into the Surf RequestContext and Spring model.
 * 
 * @author Chris Lack
 */
public class ApplicationDataInterceptor extends HandlerInterceptorAdapter
{
    private WebSiteService webSiteService;
    private ModelDecorator modelDecorator;

    /**
     * @see org.springframework.web.servlet.handler.HandlerInterceptorAdapter#preHandle(HttpServletRequest,
     *      HttpServletResponse, Object)
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception
    {
        RequestContext requestContext = ThreadLocalRequestContext.getRequestContext();

        // Get the website object and store it in the surf request context
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        WebSite webSite = webSiteService.getWebSite(serverName, serverPort);
        WebSiteService.setThreadWebSite(webSite);
        requestContext.setValue("webSite", webSite);

        // Get the current asset and section and store them in the surf request
        // context
        String path = request.getPathInfo();
        Asset asset = webSite.getAssetByPath(path);
        requestContext.setValue("asset", asset);

        Section section;
        if (asset != null)
        {
            section = asset.getContainingSection();
        }
        else
        {
            // If asset not found then try just the section
            section = webSite.getSectionByPath(path);
            if (section == null)
            {
                // Else store the root section for use by the 404 page.
                section = webSite.getRootSection();
            }
        }
        requestContext.setValue("section", section);

        return super.preHandle(request, response, handler);
    }

    /**
     * @see org.springframework.web.servlet.handler.HandlerInterceptorAdapter#postHandle(HttpServletRequest,
     *      HttpServletResponse, Object, ModelAndView)
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
            ModelAndView modelAndView) throws Exception
    {
        super.postHandle(request, response, handler, modelAndView);

        modelDecorator.populate(request, modelAndView);
    }

    public void setWebSiteService(WebSiteService webSiteService)
    {
        this.webSiteService = webSiteService;
    }

    public void setModelDecorator(ModelDecorator modelDecorator)
    {
        this.modelDecorator = modelDecorator;
    }
}
