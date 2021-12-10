package cluster.management;

import org.apache.zookeeper.KeeperException;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class OnElectionAction implements OnElectionCallback {
    private final ServiceRegistry serviceRegistry;
    private final int port;

    public OnElectionAction(ServiceRegistry serviceRegistry, int port) {
        this.serviceRegistry = serviceRegistry;
        this.port = port;
    }
    @Override
    public void onElectedToBeLeader() {
        try {
            serviceRegistry.unregisterFromCluster();
            serviceRegistry.registryForUpdates();
        } catch (InterruptedException e) {
        } catch (KeeperException e) {
        }

    }

    @Override
    public void onWorker() {
        try {
            String currentAddress = String.format("https://%s:%d", InetAddress.getLocalHost().getCanonicalHostName(), port);
            serviceRegistry.registerToCluster(currentAddress);
        } catch (UnknownHostException e) {
        } catch (InterruptedException e) {
        } catch (KeeperException e) {
        }

    }
}
