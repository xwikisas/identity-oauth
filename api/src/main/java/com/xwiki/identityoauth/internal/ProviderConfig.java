/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xwiki.identityoauth.internal;

import java.util.Map;

import org.xwiki.rendering.syntax.Syntax;

class ProviderConfig
{
    private String name;

    private String loginCode;

    private String configPage;

    private Syntax documentSyntax;

    private int orderHint;

    private Map<String, String> config;

    public Map<String, String> getConfig()
    {
        return config;
    }

    public void setConfig(Map<String, String> config)
    {
        this.config = config;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getLoginCode()
    {
        return loginCode;
    }

    public void setLoginCode(String loginCode)
    {
        this.loginCode = loginCode;
    }

    public int getOrderHint()
    {
        return orderHint;
    }

    public void setOrderHint(int orderHint)
    {
        this.orderHint = orderHint;
    }

    public Syntax getProviderDocumentSyntax()
    {
        return documentSyntax;
    }

    public void setDocumentSyntax(Syntax documentSyntax)
    {
        this.documentSyntax = documentSyntax;
    }

    public String getConfigPage() { return configPage; }

    public void setConfigPage(String page) { this.configPage = page;}
}
