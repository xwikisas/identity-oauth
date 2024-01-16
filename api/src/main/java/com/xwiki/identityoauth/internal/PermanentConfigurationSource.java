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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.internal.CommonsConfigurationSource;
import org.xwiki.environment.Environment;

/**
 * Manipulate the configuration stored in the permanent directory {@code <permdir>/configuration.properties}. This class
 * duplicates some methods from org.xwiki.configuration.internal.PermanentConfigurationSource. After upgrading to an
 * XWiki parent >= 15.9, that includes XWIKI-542:The cookie encryption keys should be randomly generated, the XWiki
 * class should be used instead.
 *
 * @version $Id$
 * @since 1.17.4
 */
@Component
@Named("identity/permanent")
@Singleton
public class PermanentConfigurationSource extends CommonsConfigurationSource implements Initializable
{
    private static final String XWIKI_PROPERTIES_FILE = "configuration.properties";

    /**
     * The Environment from where to get the XWiki properties file.
     */
    @Inject
    private Environment environment;

    private FileBasedConfigurationBuilder<PropertiesConfiguration> builder;

    @Override
    public void initialize() throws InitializationException
    {
        setConfiguration(loadConfiguration());
    }

    private Configuration loadConfiguration() throws InitializationException
    {
        File permanentDirectory = this.environment.getPermanentDirectory();
        File permanentConfiguration = new File(permanentDirectory, XWIKI_PROPERTIES_FILE).getAbsoluteFile();

        // If it does not exist, create it.
        if (!permanentConfiguration.exists()) {
            try {
                FileUtils.write(permanentConfiguration, "", StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new InitializationException("Failed to create the file [" + permanentConfiguration + "]", e);
            }
        }

        try {
            // Create the configuration builder.
            this.builder = new Configurations().propertiesBuilder(permanentConfiguration);

            // Build the configuration.
            return this.builder.getConfiguration();
        } catch (ConfigurationException e) {
            throw new InitializationException(
                "Failed to create the Configuration for file [" + permanentConfiguration + "]", e);
        }
    }
}

