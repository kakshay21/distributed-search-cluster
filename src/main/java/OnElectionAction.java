import cluster.management.OnElectionCallback;
import cluster.management.ServiceRegistry;
import network.WebClient;
import network.WebSever;
import org.apache.zookeeper.KeeperException;
import search.SearchCoordinator;
import search.SearchWorker;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class OnElectionAction implements OnElectionCallback {
    private final ServiceRegistry workerServiceRegistry;
    private final ServiceRegistry coordinatorServiceRegistry;
    private final int port;
    private WebSever webServer;

    public OnElectionAction(int port, ServiceRegistry workerServiceRegistry, ServiceRegistry coordinatorServiceRegistry) {
        this.workerServiceRegistry = workerServiceRegistry;
        this.coordinatorServiceRegistry = coordinatorServiceRegistry;
        this.port = port;
    }
    @Override
    public void onElectedToBeLeader() {
        try {
            workerServiceRegistry.unregisterFromCluster();
        } catch (KeeperException| InterruptedException e) {
            e.printStackTrace();
        }

        workerServiceRegistry.registryForUpdates();
        if (webServer != null) {
            webServer.close();
        }

        SearchCoordinator searchCoordinator = new SearchCoordinator(workerServiceRegistry, new WebClient());
        webServer = new WebSever(port, searchCoordinator);
        webServer.startServer();

        try {
            String currentAddress = String.format("https://%s:%d/%s", InetAddress.getLocalHost().getCanonicalHostName(), port, searchCoordinator.getEndpoint());
            coordinatorServiceRegistry.registerToCluster(currentAddress);
        } catch (InterruptedException| KeeperException | UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onWorker() {
        SearchWorker searchWorker = new SearchWorker();
        webServer = new WebSever(port, searchWorker);
        try {
            String currentAddress = String.format("https://%s:%d/%s", InetAddress.getLocalHost().getCanonicalHostName(), port, searchWorker.getEndpoint());
            workerServiceRegistry.registerToCluster(currentAddress);
        } catch (UnknownHostException | InterruptedException | KeeperException e) {
            e.printStackTrace();
        }

    }
}