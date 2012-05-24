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

package com.abiquo.commons.web.listener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DisclaimerListener implements ServletContextListener
{
    private static final String NAME_HOLDER = "#########################";

    public static final Logger LOGGER = LoggerFactory.getLogger("");

    @Override
    public void contextInitialized(final ServletContextEvent sce)
    {
        InputStream disclaimer =
            Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("META-INF/DISCLAIMER");

        if (disclaimer != null)
        {
            BufferedReader reader = null;

            try
            {
                reader = new BufferedReader(new InputStreamReader(disclaimer));
                String line = null;

                while ((line = reader.readLine()) != null)
                {
                    String output =
                        printWebappName(cloudify(line), sce.getServletContext()
                            .getServletContextName());

                    LOGGER.info(output);
                }
            }
            catch (IOException ex)
            {
                LOGGER.warn("Could not read disclaimer file", ex);
            }
            finally
            {
                if (reader != null)
                {
                    try
                    {
                        reader.close();
                    }
                    catch (IOException ex)
                    {
                        LOGGER.warn("Could close disclaimer reader", ex);
                    }
                }
            }
        }
    }

    @Override
    public void contextDestroyed(final ServletContextEvent arg0)
    {

    }

    private String printWebappName(final String line, final String name)
    {
        if (name.length() > NAME_HOLDER.length())
        {
            return line.replaceAll(NAME_HOLDER,
                StringUtils.center(name.substring(0, NAME_HOLDER.length()), NAME_HOLDER.length()));
        }
        else
        {
            return line.replaceAll(NAME_HOLDER, StringUtils.center(name, NAME_HOLDER.length()));
        }

    }

    private String cloudify(final String line)
    {
        return line.replaceAll("\\*", "\u2601");
    }

}
