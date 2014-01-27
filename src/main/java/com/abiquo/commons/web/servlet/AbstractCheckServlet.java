/**
 * Copyright (C) 2008 - Abiquo Holdings S.L. All rights reserved.
 *
 * Please see /opt/abiquo/tomcat/webapps/legal/ on Abiquo server
 * or contact contact@abiquo.com for licensing information.
 */
package com.abiquo.commons.web.servlet;

import static java.lang.System.getProperty;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base implementation of the Check Servlet.
 * <p>
 * Each Remote Service in the platform must implement its own <code>CheckServlet</code> to let
 * consumers test its availability.
 * 
 * @author ibarrera
 */
public abstract class AbstractCheckServlet extends HttpServlet
{
    public static final String DATACENTER_ID = getProperty("abiquo.datacenter.id");

    /** The logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCheckServlet.class);

    /** Serial UID. */
    private static final long serialVersionUID = 1L;

    /**
     * Performs a check to validate Remote Service status.
     * 
     * @return A boolean indicating the status of the Remote Service.
     * @throws Exception If check operation fails or the Remote Service is not available.
     */
    protected abstract boolean check() throws Exception;

    /**
     * Gets the datacenter ID used to coordinate amqp producer/consumer
     * 
     * @return the configure datacenter ID
     * @throws Exception if the datacenter ID property is not found.
     */
    protected String getDatacenterUuid() throws Exception
    {
        return DATACENTER_ID;
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
        throws ServletException, IOException
    {
        try
        {
            if (check())
            {
                success(resp, getDatacenterUuid());
            }
            else
            {
                fail(resp);
            }
        }
        catch (Exception ex)
        {
            LOGGER.warn("Check operation failed");
            fail(resp, ex.getMessage());
        }
    }

    /**
     * Returns a {@link HttpServletResponse#SC_OK} HTTP code indicating that the Remote Service is
     * available.
     * 
     * @param resp The Response.
     */
    protected void success(final HttpServletResponse resp, final String datacenterId)
        throws IOException
    {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(datacenterId);
    }

    /**
     * Returns a {@link HttpServletResponse#SC_SERVICE_UNAVAILABLE} HTTP code indicating that the
     * Remote Service is not available.
     * 
     * @param resp The Response.
     * @throws If error code cannot be sent.
     */
    protected void fail(final HttpServletResponse resp) throws IOException
    {
        resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    }

    /**
     * Returns a {@link HttpServletResponse#SC_SERVICE_UNAVAILABLE} HTTP code indicating that the
     * Remote Service is not available.
     * 
     * @param resp The Response.
     * @param msg The details of the check failure.
     * @throws If error code cannot be sent.
     */
    protected void fail(final HttpServletResponse resp, final String msg) throws IOException
    {
        resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, msg);
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
        throws ServletException, IOException
    {
        doGet(req, resp);
    }
}
