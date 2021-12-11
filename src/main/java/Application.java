import cluster.management.ServiceRegistry;
import org.apache.zookeeper.*;
import cluster.management.LeaderElection;

import java.io.IOException;

public class Application implements Watcher {
    private final static String ZOOKEEPER_ADDRESS = "localhost:2181";
    private final static int SESSION_TIMEOUT = 3000;
    private static ZooKeeper zooKeeper;
    private static final int DEFAULT_PORT = 8000;

    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        int currentPortNumber = args.length == 1? Integer.parseInt(args[0]): DEFAULT_PORT;
        Application application = new Application();
        application.connectToZookeeper();

        ServiceRegistry workerServiceRegistry = new ServiceRegistry(zooKeeper, ServiceRegistry.WORKER_REGISTRY_ZNODE);
        ServiceRegistry coordinatorServiceRegistry = new ServiceRegistry(zooKeeper, ServiceRegistry.COORDINATORS_REGISTRY_ZNODE);
        OnElectionAction onElectionAction = new OnElectionAction(currentPortNumber, workerServiceRegistry, coordinatorServiceRegistry);

        LeaderElection leaderElection = new LeaderElection(zooKeeper, onElectionAction);
        leaderElection.volunteerForLeadership();
        leaderElection.reelectLeader();

        application.run();
        application.close();
        System.out.println("Disconnected from ZooKeeper, exiting application");
    }

    public void connectToZookeeper() throws IOException {
        zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, this);
    }

    public void run() throws InterruptedException {
        synchronized (zooKeeper) {
            zooKeeper.wait();
        }
    }

    public void close() throws InterruptedException {
        zooKeeper.close();
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        switch (watchedEvent.getType()) {
            case None:
                if (watchedEvent.getState().equals(Event.KeeperState.SyncConnected)) {
                    System.out.println("Successfully connected to ZooKeeper");
                } else {
                    synchronized (zooKeeper) {
                        System.out.println("Disconnected from ZooKeeper event");
                        zooKeeper.notifyAll();
                    }
                }
                break;
        }
    }
}
