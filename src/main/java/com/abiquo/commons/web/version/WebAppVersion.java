/**
 * Abiquo community edition
 * cloud management application for hybrid clouds
 * Copyright (C) 2008-2010 - Abiquo Holdings S.L.
 *
 * This application is free software; you can redistribute it and/or
 * modify it under the terms of the GNU LESSER GENERAL PUBLIC
 * LICENSE as published by the Free Software Foundation under
 * version 3 of the License
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * LESSER GENERAL PUBLIC LICENSE v.3 for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

package com.abiquo.commons.web.version;

import static java.lang.Integer.valueOf;
import static java.lang.String.format;
import static java.util.regex.Pattern.compile;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;

/**
 * Extracts the version of the deployed WAR, reading the properties file packed and generated by
 * maven-archiver. It is heavily inspired from JCloudsVersion extractor.
 * 
 * @author Enric Ruiz
 */
public class WebAppVersion
{
    /**
     * Matches four groups, the major, the minor and the (optional) patch versions separated by one
     * dot each and followed by any string that begins with '-'
     */
    private static final Pattern VERSION_PATTERN = compile("(\\d+)\\.(\\d+)\\.?(\\d?)(\\-.*)?");

    private static final String VERSION_PROPERTY_NAME = "version";

    private static WebAppVersion instance = null;

    public final String version;

    public final int majorVersion;

    public final int minorVersion;

    public final int patchVersion;

    public final boolean snapshot;

    private WebAppVersion(final ServletContext servletContext)
    {
        // Read properties file
        version = readVersionPropertyFromClasspath(servletContext);

        // Parse version String
        Matcher versionMatcher = VERSION_PATTERN.matcher(version);

        if (!versionMatcher.matches())
        {
            throw new IllegalArgumentException(format("Version '%s' did not match pattern '%s'",
                version, VERSION_PATTERN));
        }

        majorVersion = valueOf(versionMatcher.group(1));
        minorVersion = valueOf(versionMatcher.group(2));

        if (versionMatcher.groupCount() > 3)
        {
            String patch = versionMatcher.group(3);
            patchVersion = isBlank(patch) ? 0 : valueOf(patch);
        }
        else
        {
            patchVersion = 0;
        }

        snapshot = version.contains("-SNAPSHOT");
    }

    private String readVersionPropertyFromClasspath(final ServletContext servletContext)
    {
        final String resourceFile = formatResourceFilePath(servletContext);
        Properties versionProperties = new Properties();

        try
        {
            InputStream inputStream = servletContext.getResourceAsStream(resourceFile);

            if (inputStream == null)
            {
                throw new IllegalStateException(format("The resource file '%s' could not be found",
                    resourceFile));
            }

            versionProperties.load(inputStream);
        }
        catch (IOException ex)
        {
            throw new IllegalStateException(format("Unable to load resource file '%s'",
                resourceFile));
        }

        String version = versionProperties.getProperty(VERSION_PROPERTY_NAME);

        if (version == null)
        {
            throw new IllegalStateException(format(
                "The resource property '%s' could not be loaded", VERSION_PROPERTY_NAME));
        }

        return version;
    }

    private String formatResourceFilePath(final ServletContext servletContext)
    {
        String contextPath = servletContext.getContextPath();
        return format("META-INF/maven/com.abiquo/%s/pom.properties",
            contextPath.replace("/", EMPTY));
    }

    @Override
    public String toString()
    {
        return version;
    }

    public static WebAppVersion get(final ServletContext servletContext)
    {
        if (instance == null)
        {
            instance = new WebAppVersion(servletContext);
        }

        return instance;
    }
}
