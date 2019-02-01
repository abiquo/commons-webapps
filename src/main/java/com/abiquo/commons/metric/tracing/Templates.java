/**
 * Copyright (C) 2008 - Abiquo Holdings S.L. All rights reserved.
 *
 * Please see /opt/abiquo/tomcat/webapps/legal/ on Abiquo server
 * or contact contact@abiquo.com for licensing information.
 */
package com.abiquo.commons.metric.tracing;

import java.util.Map;

import com.google.common.base.Joiner;

public class Templates
{
    public static String formatTimerStart(final long ts, final String metricName,
        final Map<String, String> tags)
    {
        return String.format("[trace][%s][timer][%s][start][%s]", ts, metricName, formatTags(tags));
    }

    public static String formatTimerStop(final long ts, final String metricName,
        final Map<String, String> tags)
    {
        return String.format("[trace][%s][timer][%s][stop][%s]", ts, metricName, formatTags(tags));
    }

    private static String formatTags(final Map<String, String> tags)
    {
        return Joiner.on(",").withKeyValueSeparator(":").join(tags);
    }
}
