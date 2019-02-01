/**
 * Copyright (C) 2008 - Abiquo Holdings S.L. All rights reserved.
 *
 * Please see /opt/abiquo/tomcat/webapps/legal/ on Abiquo server
 * or contact contact@abiquo.com for licensing information.
 */
package com.abiquo.commons.metric.tracing;

import static java.lang.System.currentTimeMillis;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;

public class NimrodMetricsLogger
{
    protected static final String Alert = "[nimrod][{}][alert][{}][{}][{}]";

    protected static final String Gauge = "[nimrod][{}][gauge][{}][{}][{}]";

    protected static final String Counter = "[nimrod][{}][counter][{}][{}][{}]";

    protected static final String TimerStart = "[nimrod][{}][timer][{}][start][{}]";

    protected static final String TimerStop = "[nimrod][{}][timer][{}][stop][{}]";

    protected final Logger log;

    public static NimrodMetricsLogger create(final Logger logger)
    {
        return new NimrodMetricsLogger(logger);
    }

    public NimrodMetricsLogger(final Logger logger)
    {
        this.log = logger;
    }

    public void alert(final String identifier, final String value, final Map<String, String> tags)
    {
        log.debug(Alert,
            new Object[] {currentTimeMillis(), identifier, value, CommaSeparatedList(tags)});
    }

    public void alert(final String identifier, final String value)
    {
        alert(identifier, value, Collections.emptyMap());
    }

    public void gauge(final String identifier, final double value, final Map<String, String> tags)
    {
        log.debug(Gauge,
            new Object[] {currentTimeMillis(), identifier, value, CommaSeparatedList(tags)});
    }

    public void gauge(final String identifier, final double value)
    {
        gauge(identifier, value, Collections.emptyMap());
    }

    public void counter(final String identifier, final long value, final Map<String, String> tags)
    {
        log.debug(Counter,
            new Object[] {currentTimeMillis(), identifier, value, CommaSeparatedList(tags)});
    }

    public void counter(final String identifier, final long value)
    {
        counter(identifier, value, Collections.emptyMap());
    }

    public void timer(final String identifier, final long start, final long stop,
        final Map<String, String> tags)
    {
        log.debug(TimerStart, new Object[] {start, identifier, CommaSeparatedList(tags)});
        log.debug(TimerStop, new Object[] {stop, identifier, CommaSeparatedList(tags)});
    }

    public void timer(final String identifier, final long start, final long stop)
    {
        timer(identifier, start, stop, Collections.emptyMap());
    }

    public AutoCloseable timer(final String identifier, final Map<String, String> tags)
    {
        final long start = currentTimeMillis();
        log.debug(TimerStart, new Object[] {start, identifier, CommaSeparatedList(tags)});

        return () -> log.debug(TimerStop,
            new Object[] {currentTimeMillis(), identifier, CommaSeparatedList(tags)});
    }

    public AutoCloseable timer(final String identifier)
    {
        final long start = currentTimeMillis();
        log.debug(TimerStart, new Object[] {start, identifier, Collections.emptyMap()});

        return () -> log.debug(TimerStop, new Object[] {currentTimeMillis(), identifier, //
        Collections.emptyMap()});
    }

    protected static final String CommaSeparatedList(final Map<String, String> tags)
    {
        return tags.entrySet().stream() //
            .map(entry -> entry.getKey().concat(":").concat(entry.getValue())) //
            .collect(Collectors.joining(","));
    };
}
