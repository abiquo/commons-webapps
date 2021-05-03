/**
 * Copyright (C) 2008 - Abiquo Holdings S.L. All rights reserved.
 *
 * Please see /opt/abiquo/tomcat/webapps/legal/ on Abiquo server
 * or contact contact@abiquo.com for licensing information.
 */
package com.abiquo.commons.web.listener;

import java.util.logging.Level;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.jinterop.dcom.common.JISystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sets the logging level for the <code>J-Interop</code> framework manually, as the
 * SLF4jBridgeHandler does not work.
 *
 * @see <a href=
 *      "http://www.slf4j.org/apidocs/org/slf4j/bridge/SLF4JBridgeHandler.html">SLF4JBridgeHandler</a>
 *      for logging level mappings.
 * @author ibarrera
 */
public class JInteropLoggingListener implements ServletContextListener
{
    @Override
    public void contextInitialized(final ServletContextEvent context)
    {
        Logger logger = LoggerFactory.getLogger("org.jinterop");
        Level level = null;

        if (logger.isTraceEnabled())
        {
            level = Level.FINEST;
        }
        else if (logger.isDebugEnabled())
        {
            level = Level.FINE;
        }
        else if (logger.isInfoEnabled())
        {
            level = Level.INFO;
        }
        else if (logger.isWarnEnabled())
        {
            level = Level.WARNING;
        }
        else if (logger.isErrorEnabled())
        {
            level = Level.SEVERE;
        }
        else
        {
            level = Level.OFF;
        }

        JISystem.getLogger().setLevel(level);
    }

    @Override
    public void contextDestroyed(final ServletContextEvent context)
    {
    }

}
