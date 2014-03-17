/**
 * Copyright (C) 2008 - Abiquo Holdings S.L. All rights reserved.
 *
 * Please see /opt/abiquo/tomcat/webapps/legal/ on Abiquo server
 * or contact contact@abiquo.com for licensing information.
 */
package com.abiquo.commons.web.listener;

import static com.netflix.curator.framework.CuratorFrameworkFactory.newClient;
import static java.lang.Integer.valueOf;
import static java.lang.System.getProperty;
import static java.lang.Thread.currentThread;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abiquo.commons.web.ClusterConstants;
import com.netflix.curator.RetryLoop;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.CuratorFrameworkFactory;
import com.netflix.curator.framework.api.CuratorWatcher;
import com.netflix.curator.framework.recipes.leader.LeaderSelector;
import com.netflix.curator.framework.recipes.leader.LeaderSelectorListener;
import com.netflix.curator.framework.state.ConnectionState;
import com.netflix.curator.retry.RetryNTimes;

/**
 * Check for node distribution to directly use the {@link AMQPConsumersService} or delegate it to
 * the cluster leader notification.
 */
public abstract class LeadElectionContextListener implements ServletContextListener,
    LeaderSelectorListener, CuratorWatcher
{
    /** Tune {@link CuratorFrameworkFactory}. Connection timeout */
    private static final int ZK_CONNECTION_TIMEOUT_MS = valueOf(getProperty("abiquo.api.zk."
        + "connectionTimeoutMs", "15000")); // 1sec

    /** Tune {@link CuratorFrameworkFactory}. Num or retries on zk operation */
    private static final int ZK_RETRIES = valueOf(getProperty("abiquo.api.zk."
        + "connectionRetries", "10")); // 10times

    /**
     * Connection to ZooKeeper server. Property not set indicate non-distributed API
     * {@link LeadElectionContextListener#isDistributed()}.
     */
    private final static String ZK_SERVER = getProperty(ClusterConstants.ZK_SERVER); // localhost:2181

    /** Tune {@link CuratorFrameworkFactory}. Session timeout */
    private static final int ZK_SESSION_TIMEOUT_MS = valueOf(getProperty("abiquo.api.zk."
        + "sessionTimeoutMs", "15000")); // 15sec

    /** Tune {@link CuratorFrameworkFactory}. Ms to sleep between retries. */
    private static final int ZK_SLEEP_MS_BETWEEN_RETRIES = valueOf(getProperty("abiquo.api.zk."
        + "sleepMsBetweenRetries", "5000")); // 1sec

    protected static final Logger LOGGER = LoggerFactory
        .getLogger(LeadElectionContextListener.class);

    /** Check node configuration to know if participates in a cluster. */
    private final static boolean isDistributed()
    {
        return ZK_SERVER != null;
    }

    /** Zk-client connected to the cluster using the ZK_SERVER connection. */
    private CuratorFramework curatorClient;

    /** Zk-recipe to select one participant in the cluster. (@see {@link LeaderSelectorListener} ) */
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
    public abstract String getZookeeperNodePath();

    @Override
    public void contextDestroyed(final ServletContextEvent sce)
    {
        if (isDistributed())
        {
            if (leaderSelector.hasLeadership())
            {
                onShutdown(sce);
            }

            leaderSelector.close();
        }
        else
        {
            onShutdown(sce);
        }
    }

    @Override
    public void contextInitialized(final ServletContextEvent sce)
    {
        initializeContext(sce);

        if (isDistributed())
        {
            try
            {
                startZookeeper();
            }
            // if zk-server not started then the node can't start properly
            catch (Exception e)
            {
                LOGGER.error("Fatal, zookeeper configuration enabled "
                    + "but not connection to zk server at {}", ZK_SERVER, e);
                throw new RuntimeException(e);
            }
        }
        else
        {
            onStart(sce);
        }
    }

    @Override
    public void process(final WatchedEvent event) throws Exception
    {
        if (event.getType() == EventType.NodeDeleted)
        {
            LOGGER.info("Processing deletion of Zookeeper path " + event.getPath());

            if (leaderSelector.hasLeadership())
            {
                onLeadershipSuspended();
            }

            leaderSelector.close();

            // reinitialize the leader selector
            leaderSelector = new LeaderSelector(curatorClient, getZookeeperNodePath(), this);
            leaderSelector.setId(getHostName());
            leaderSelector.start();

            LOGGER.info("Starting Leader selector instance");
        }
    }

    /**
     * If the SUSPENDED state is reported, the instance must assume that it might no longer be the
     * leader until it receives a RECONNECTED state. If the LOST state is reported, the instance is
     * no longer the leader and its takeLeadership method should exit.
     */
    @Override
    public void stateChanged(final CuratorFramework client, final ConnectionState newState)
    {
        LOGGER.info("{} to {}", newState.name(), getZookeeperNodePath());

        switch (newState)
        {
            case SUSPENDED:
                if (leaderSelector.hasLeadership())
                {
                    onLeadershipSuspended();
                }
                break;
            case RECONNECTED:
                // theory leaderSelector.requeue();
                // https://github.com/Netflix/curator/issues/24
                // The method #process will create a new leaderSelector instance. Once is
                // reconnected, watch again
                LOGGER.info("Processing Zookeeper reconnect");
                // reinitialize the leader selector
                leaderSelector = new LeaderSelector(curatorClient, getZookeeperNodePath(), this);
                leaderSelector.setId(getHostName());
                leaderSelector.start();

                watchItself();
                break;

            case CONNECTED:
                LOGGER.info("Processing Zookeeper connect");
                watchItself();
                break;

            case LOST:
                // already disconnected by SUSPENDED state
                break;
        }
    }

    /**
     * Only leader has the AMQP consumers started.
     * <p>
     * /!\ NOTE : This method should only return when leadership is being relinquished.
     */
    @Override
    public void takeLeadership(final CuratorFramework client) throws Exception
    {
        onLeadershipTaken();

        try
        {
            currentThread().join(); // do not return
        }
        catch (InterruptedException e)
        {
            // expected during ''contextDestroy''
        }

        // if interrupted by a ''stateChanged'' the handler will check if require
        // ''shutdownRegisteredConsumers''
        LOGGER.info("Current node no longer the {} leader", getZookeeperNodePath());
    }

    private void watchItself()
    {
        // Due the asynchronous nature of zookeeper, the getChildren can raise an
        // exception because the last znode could not be yet created when we are
        // looking for it. So the best solution is use the RetryLoop as Curator
        // recommends.
        // (Retry Policy is configured at initialization time)
        RetryLoop rl = curatorClient.getZookeeperClient().newRetryLoop();
        List<String> child;
        int i = 0;

        while (rl.shouldContinue())
        {
            LOGGER.info("Trying to put a watcher in the last node created. (" + i + " attempts )");
            try
            {
                Thread.sleep(3000);

                child = curatorClient.getChildren().forPath(getZookeeperNodePath());

                Collections.sort(child, new Comparator<String>()
                {
                    // names of the znodes are like 'generateduuid'-lock-seq
                    // we need to compare only the seq
                    @Override
                    public int compare(final String o1, final String o2)
                    {

                        Integer seq1 = Integer.valueOf(o1.substring(o1.indexOf("lock-") + 5));
                        Integer seq2 = Integer.valueOf(o2.substring(o2.indexOf("lock-") + 5));

                        // we want ordered descending
                        return seq2.compareTo(seq1);

                    }
                });

                String path = getZookeeperNodePath() + "/" + child.get(0);
                LOGGER.info("Starting watcher for path " + path);
                curatorClient.checkExists().usingWatcher(this).forPath(path);

                rl.markComplete();
            }
            catch (Exception e)
            {
                try
                {
                    LOGGER.warn("Node could not execute the 'watchItself' in Zookeeper");
                    rl.takeException(e);
                }
                catch (Exception e1)
                {
                    LOGGER.error("Could not Watch 'itself' node.");
                }
            }
        }

    }

    /** Connects to ZK-Server and adds as participant to {@link LeaderSelector} cluster. */
    protected void startZookeeper() throws Exception
    {
        curatorClient = newClient(ZK_SERVER, ZK_SESSION_TIMEOUT_MS, ZK_CONNECTION_TIMEOUT_MS,//
            new RetryNTimes(ZK_RETRIES, ZK_SLEEP_MS_BETWEEN_RETRIES));
        curatorClient.start();

        LOGGER.info("Connected to {}", ZK_SERVER);

        leaderSelector = new LeaderSelector(curatorClient, getZookeeperNodePath(), this);
        leaderSelector.setId(getHostName());
        leaderSelector.start();
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
