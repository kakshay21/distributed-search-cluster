package cluster.management;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServiceRegistry implements Watcher {
    public static String WORKER_REGISTRY_ZNODE = "/workers_service_registry";
    public static String COORDINATORS_REGISTRY_ZNODE = "/coordinators_service_registry";
    private final ZooKeeper zooKeeper;
    private String currentZnode = null;
    private final String serviceRegisteryZnode;
    private List<String> allServiceAddress = null;

    public ServiceRegistry(ZooKeeper zooKeeper, String serviceRegistryZnode) {
        this.zooKeeper = zooKeeper;
        this.serviceRegisteryZnode = serviceRegistryZnode;
        createServiceRegistryZnode();
    }

    public void registerToCluster(String metadata) throws InterruptedException, KeeperException {
        currentZnode = zooKeeper.create(serviceRegisteryZnode + "/r_", metadata.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        System.out.println("Registered to service registry");
    }

    public void registryForUpdates() {
        try {
            updateAddress();
        } catch (InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
    }

    public void unregisterFromCluster() throws InterruptedException, KeeperException {
        if (currentZnode == null || zooKeeper.exists(currentZnode, false) == null) {
            return;
        }
        zooKeeper.delete(currentZnode, -1);
    }

    private void createServiceRegistryZnode() {
        try {
            if (zooKeeper.exists(serviceRegisteryZnode, false) == null) {
                zooKeeper.create(serviceRegisteryZnode, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public synchronized List<String> getAllServiceAddress() throws InterruptedException, KeeperException {
        if (allServiceAddress == null) {
            updateAddress();
        }
        return allServiceAddress;
    }

    public void updateAddress() throws InterruptedException, KeeperException {
        List<String> workerNodes = zooKeeper.getChildren(serviceRegisteryZnode, this);
        List<String> addresses = new ArrayList<>(workerNodes.size());
        for (String workerNode : workerNodes) {
            String workerNodePath = serviceRegisteryZnode + "/" + workerNode;
            Stat stat = zooKeeper.exists(workerNodePath, false);
            if (stat == null) {
                continue;
            }
            byte[] addressBytes = zooKeeper.getData(workerNodePath, false, stat);
            String address = new String(addressBytes);
            addresses.add(address);
        }
        this.allServiceAddress = Collections.unmodifiableList(addresses);
        System.out.println("The cluster addresses are: " + this.allServiceAddress);
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        try {
            updateAddress();
        } catch (InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
    }
}
