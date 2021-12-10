package cluster.management;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServiceRegistry implements Watcher {
    private static String REGISTRY_ZNODE = "/service_registry";
    private final ZooKeeper zooKeeper;
    private String currentZnode = null;
    private List<String> allServiceAddress = null;

    public ServiceRegistry(ZooKeeper zooKeeper) {
        this.zooKeeper = zooKeeper;
        createServiceRegistryZnode();
    }

    public void registerToCluster(String metadata) throws InterruptedException, KeeperException {
        currentZnode = zooKeeper.create(REGISTRY_ZNODE + "/r_", metadata.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        System.out.println("Registered to service registry");
    }

    public void registryForUpdates() {
        try {
            updateAddress();
        } catch (InterruptedException e) {
        } catch (KeeperException e) {
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
            if (zooKeeper.exists(REGISTRY_ZNODE, false) == null) {
                zooKeeper.create(REGISTRY_ZNODE, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

            }
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
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
        List<String> workerNodes = zooKeeper.getChildren(REGISTRY_ZNODE, this);
        List<String> addresses = new ArrayList<>(workerNodes.size());
        for (String workerNode : workerNodes) {
            String workerNodePath = REGISTRY_ZNODE + "/" + workerNode;
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
        } catch (InterruptedException e) {
        } catch (KeeperException e) {
        }

    }
}
