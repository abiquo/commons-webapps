/**
 * Copyright (C) 2008 - Abiquo Holdings S.L. All rights reserved.
 *
 * Please see /opt/abiquo/tomcat/webapps/legal/ on Abiquo server
 * or contact contact@abiquo.com for licensing information.
 */
package com.abiquo.commons.web.listener;

import static java.lang.Integer.valueOf;
import static java.lang.System.getProperty;
import static java.lang.Thread.currentThread;
import static org.apache.curator.framework.CuratorFrameworkFactory.newClient;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListener;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter;
import org.apache.curator.retry.RetryNTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abiquo.commons.web.ClusterConstants;

/**
 * Check for node distribution to directly use the {@link AMQPConsumersService} or delegate it to
 * the cluster leader notification.
 */
public abstract class LeadElectionContextListener extends LeaderSelectorListenerAdapter
    implements ServletContextListener

{
    /** Tune {@link CuratorFrameworkFactory}. Connection timeout */
    private static final int ZK_CONNECTION_TIMEOUT_MS =
        valueOf(getProperty("abiquo.api.zk." + "connectionTimeoutMs", "15000")); // 1sec

    /** Tune {@link CuratorFrameworkFactory}. Num or retries on zk operation */
    private static final int ZK_RETRIES =
        valueOf(getProperty("abiquo.api.zk." + "connectionRetries", "10")); // 10times

    /**
     * Connection to ZooKeeper server. Property not set indicate non-distributed API
     * {@link LeadElectionContextListener#isDistributed()}.
     */
    private final static String ZK_SERVER = getProperty(ClusterConstants.ZK_SERVER); // localhost:2181

    /** Tune {@link CuratorFrameworkFactory}. Session timeout */
    private static final int ZK_SESSION_TIMEOUT_MS =
        valueOf(getProperty("abiquo.api.zk." + "sessionTimeoutMs", "15000")); // 15sec

    /** Tune {@link CuratorFrameworkFactory}. Ms to sleep between retries. */
    private static final int ZK_SLEEP_MS_BETWEEN_RETRIES =
        valueOf(getProperty("abiquo.api.zk." + "sleepMsBetweenRetries", "5000")); // 1sec

    protected static final Logger LOGGER =
        LoggerFactory.getLogger(LeadElectionContextListener.class);

    /** Check node configuration to know if participates in a cluster. */
    private final static boolean isDistributed()
    {
        return ZK_SERVER != null;
    }

    /** Zk-client connected to the cluster using the ZK_SERVER connection. */
    private CuratorFramework curatorClient;

    /**
     * Zk-recipe to select one participant in the cluster. (@see {@link LeaderSelectorListener} )
     */
    protected LeaderSelector leaderSelector;

    /**
     * Called when the application starts.
     * <p>
     * Use this method to perform initialization tasks, such as getting beans from the Spring
     * context, and initializing class members.
     */
    public abstract void initializeContext(ServletContextEvent sce);

    /**
     * Called in non-distributed environments when the node starts.
     * <p>
     * Use this method to start services in non-distributed environments.
     */
    public abstract void onStart(ServletContextEvent sce);

    /**
     * Called when the node is going to shut down. This is not going to be invoked when the node
     * loses the leadership in a distributed environment; only when the application is manually shut
     * down.
     * <p>
     * Use this method to shutdown all services and release the resources.
     */
    public abstract void onShutdown(ServletContextEvent sce);

    /**
     * In a distributed environment, this method is invoked when the node gains the leadership.
     * <p>
     * Use this method to start the services after the node has taken the leadership.
     * 
     * @throws Exception
     */
    public abstract void onLeadershipTaken() throws Exception;

    /**
     * In a distributed environment, this method is invoked when the node loses the leadership.
     * <p>
     * Use this method to stop the services after the node has lost the leadership.
     */
    public abstract void onLeadershipSuspended();

    /**
     * Get the path for the node in Zookeeper.
     */
    private String zookeeperNodePath;

    @Override
    public void contextDestroyed(final ServletContextEvent sce)
    {
        if (isDistributed())
        {
            stopZookeeper();
        }

        onShutdown(sce);
    }

    @Override
    public void contextInitialized(final ServletContextEvent sce)
    {
        zookeeperNodePath = sce.getServletContext().getContextPath() + "/leader-election";

        initializeContext(sce);

        if (isDistributed())
        {
            try
            {
                startZookeeper();
            }
            catch (Exception e)
            {
                String error =
                    "Cannot start, zookeeper configuration enabled but not connection to zk server at "
                        + ZK_SERVER;
                LOGGER.error(error, e);
                throw new RuntimeException(error, e);
            }
        }
        else
        {
            onStart(sce);
        }
    }

    /**
     * /!\ NOTE : This method should only return when leadership is being relinquished.
     */
    @Override
    public void takeLeadership(final CuratorFramework client) throws Exception
    {
        Exception failedToTake = null;
        try
        {
            LOGGER.info("Taking leadership on {} ...", zookeeperNodePath);
            onLeadershipTaken();
        }
        catch (Exception e)
        {
            failedToTake = e;
            LOGGER.error("Failed to take leadership on " + zookeeperNodePath, e);
        }

        if (failedToTake == null)
        {
            try
            {
                LOGGER.info("Leader on {}", zookeeperNodePath);
                currentThread().join();
            }
            catch (InterruptedException e)
            {
                LOGGER.info("leadership interrupted");
            }

            LOGGER.info("Current node no longer the {} leader", zookeeperNodePath);
        }

        try
        {
            onLeadershipSuspended();
        }
        catch (Exception e)
        {
            LOGGER.warn("Fail to cleanup onLeadershipSuspended on " + zookeeperNodePath, e);
        }

        if (failedToTake != null)
        {
            throw failedToTake;
        }
    }

    /** Connects to ZK-Server and adds as participant to {@link LeaderSelector} cluster. */
    protected void startZookeeper() throws Exception
    {
        curatorClient = newClient(ZK_SERVER, ZK_SESSION_TIMEOUT_MS, ZK_CONNECTION_TIMEOUT_MS, //
            new RetryNTimes(ZK_RETRIES, ZK_SLEEP_MS_BETWEEN_RETRIES));
        curatorClient.start();

        LOGGER.info("Connected to {}", ZK_SERVER);

        leaderSelector = new LeaderSelector(curatorClient, zookeeperNodePath, this);
        leaderSelector.autoRequeue();
        leaderSelector.setId(getHostName());
        leaderSelector.start();

        LOGGER.info("Participating in leader selector at {}", zookeeperNodePath);
    }

    protected void stopZookeeper()
    {
        LOGGER.debug("closing LeaderSelector ...");
        try
        {

            leaderSelector.close();
        }
        catch (Exception e)
        {
            LOGGER.warn("Cannot close leaderSelector", e);
        }
        try
        {

            curatorClient.close();
        }
        catch (Exception e)
        {
            LOGGER.warn("Cannot close curatorClient", e);
        }

        leaderSelector = null;
        curatorClient = null;
    }

    /**
     * Return the configure system *hostname*.
     * <p>
     * localhost/127.0.0.1 if not configured
     */
    public static String getHostName()
    {
        try
        {
            return InetAddress.getLocalHost().toString();
        }
        catch (UnknownHostException e)
        {
            return "cannot get hostname";
        }
    }
}
