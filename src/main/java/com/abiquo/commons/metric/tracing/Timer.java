/**
 * Copyright (C) 2008 - Abiquo Holdings S.L. All rights reserved.
 *
 * Please see /opt/abiquo/tomcat/webapps/legal/ on Abiquo server
 * or contact contact@abiquo.com for licensing information.
 */
package com.abiquo.commons.metric.tracing;

import static java.lang.System.currentTimeMillis;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;

public class Timer implements Closeable
{
    private final Logger log;

    private final String metricName;

    private final Map<String, String> tags;

    private final long start;

    public Timer(final Logger log, final String metricName, final Map<String, String> tags)
    {
        this.log = log;
        this.metricName = metricName;
        this.tags = tags;
        this.start = currentTimeMillis();
    }

    @Override
    public void close() throws IOException
    {
        long stop = currentTimeMillis();
        log.debug(Templates.formatTimerStart(start, metricName, tags));
        log.debug(Templates.formatTimerStop(stop, metricName, tags));
    }
}
