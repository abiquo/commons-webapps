/**
 * Copyright (C) 2008 - Abiquo Holdings S.L. All rights reserved.
 *
 * Please see /opt/abiquo/tomcat/webapps/legal/ on Abiquo server
 * or contact contact@abiquo.com for licensing information.
 */
package com.abiquo.commons.web;

/**
 * @author <a href="mailto:serafin.sedano@abiquo.com">Serafin Sedano</a>
 */
public class ClusterConstants
{
    private ClusterConstants()
    {
    }

    /** System property that indicates whether Abiquo is in cluster or not. */
    public static final String ZK_SERVER = "abiquo.api.zk.serverConnection";
}
