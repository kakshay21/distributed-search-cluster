import org.apache.zookeeper.*;
import cluster.management.LeaderElection;

import java.io.IOException;

public class Application implements Watcher {
    private final static String ZOOKEEPER_ADDRESS = "localhost:2181";
    private final static int SESSION_TIMEOUT = 3000;
    private static ZooKeeper zooKeeper;

    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        Application application = new Application();
        application.connectToZookeeper();

        LeaderElection leaderElection = new LeaderElection(zooKeeper);
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
