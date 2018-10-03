/**
 * Copyright (C) 2008 - Abiquo Holdings S.L. All rights reserved.
 *
 * Please see /opt/abiquo/tomcat/webapps/legal/ on Abiquo server
 * or contact contact@abiquo.com for licensing information.
 */
package com.abiquo.commons.web.listener;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Strings.nullToEmpty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;

import javax.servlet.ServletContext;
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
        InputStream disclaimer = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("META-INF/DISCLAIMER");

        if (disclaimer != null)
        {
            BufferedReader reader = null;

            try
            {
                reader = new BufferedReader(new InputStreamReader(disclaimer));
                String line = null;
                String year = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
                while ((line = reader.readLine()) != null)
                {
                    String output =
                        printWebappName(cloudify(printYear(line, year)), sce.getServletContext());

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

    private String printWebappName(final String line, final ServletContext servletContext)
    {
        final String name = firstNonNull(nullToEmpty(servletContext.getServletContextName()),
            servletContext.getContextPath());

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

    private String printYear(final String line, final String year)
    {
        return line.replaceAll("\\{year\\}", year);
    }

}
