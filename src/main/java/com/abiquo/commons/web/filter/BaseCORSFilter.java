/**
 * Copyright (C) 2008 - Abiquo Holdings S.L. All rights reserved.
 *
 * Please see /opt/abiquo/tomcat/webapps/legal/ on Abiquo server
 * or contact contact@abiquo.com for licensing information.
 */
package com.abiquo.commons.web.filter;

import static com.abiquo.commons.web.CORSConstants.ACCESS_CREDENTIALS_CORS_HEADER;
import static com.abiquo.commons.web.CORSConstants.ACCESS_EXPOSE_CORS_HEADER;
import static com.abiquo.commons.web.CORSConstants.ACCESS_HEADERS_CORS_HEADER;
import static com.abiquo.commons.web.CORSConstants.ACCESS_ORIGIN_CORS_HEADER;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.net.HttpHeaders;

/**
 * Adds the required headers to the response to allow Cross-Origin Resource Sharing.
 * 
 * @author Ignasi Barrera
 */
public abstract class BaseCORSFilter implements Filter
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseCORSFilter.class);

    /**
     * Return a boolean indicating if credentials are allowed in CORS requests.
     */
    protected abstract boolean allowCredentials();

    /**
     * Return a list of allowed headers in CORS requests.
     */
    protected abstract List<String> allowedHeaders();

    /**
     * Return a list of exposed headers in CORS requests.
     */
    protected List<String> exposedHeaders()
    {
        return Collections.emptyList();
    }

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException
    {
        LOGGER.info("Loading CORS filter");
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response,
        final FilterChain chain) throws IOException, ServletException
    {
        chain.doFilter(request, response);

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String origin = req.getHeader("Origin");

        // Only set the CORS headers if the 'Origin' header is in the request
        if (origin != null && !origin.isEmpty())
        {
            res.addHeader(ACCESS_ORIGIN_CORS_HEADER, origin);
            res.addHeader(ACCESS_CREDENTIALS_CORS_HEADER, String.valueOf(allowCredentials()));

            List<String> allowedHeaders = allowedHeaders();
            if (allowCredentials())
            {
                allowedHeaders =
                    ImmutableList.<String> builder().addAll(allowedHeaders)
                        .add(HttpHeaders.AUTHORIZATION).build();
            }
            if (allowedHeaders != null && !allowedHeaders.isEmpty())
            {
                res.addHeader(ACCESS_HEADERS_CORS_HEADER,
                    allowedHeaders.stream().collect(Collectors.joining(", ")));
            }

            List<String> exposedHeaders = exposedHeaders();
            if (exposedHeaders != null && !exposedHeaders.isEmpty())
            {
                res.addHeader(ACCESS_EXPOSE_CORS_HEADER,
                    exposedHeaders.stream().collect(Collectors.joining(", ")));
            }
        }
    }

    @Override
    public void destroy()
    {
        LOGGER.info("Destroying CORS filter");
    }

}
