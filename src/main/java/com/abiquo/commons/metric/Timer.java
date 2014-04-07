/**
 * Copyright (C) 2008 - Abiquo Holdings S.L. All rights reserved.
 *
 * Please see /opt/abiquo/tomcat/webapps/legal/ on Abiquo server
 * or contact contact@abiquo.com for licensing information.
 */
package com.abiquo.commons.metric;

import static java.lang.System.currentTimeMillis;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;

import com.google.common.base.Joiner;

/**
 * Measure the time to take an action and report *the elapsed milliseconds* as a Nimrod gauge log
 * event
 * <p>
 * <a href="https//:github.com/sbtourist/nimrod>Nimrod</a>
 */
public class Timer implements Closeable
{
    private static final String TEMPLATE_WITH_TAGS = "[nimrod][{}][gauge][{}][{}][{}]";

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
        long end = currentTimeMillis();
        long value = end - start;

        log.debug(TEMPLATE_WITH_TAGS, new Object[] {end, metricName, value,
        Joiner.on(",").skipNulls().withKeyValueSeparator(":").join(tags)});
    }
}
