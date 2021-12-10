import org.apache.zookeeper.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class LeaderElection implements Watcher {
    private final static String ZOOKEEPER_ADDRESS = "localhost:2181";
    private final static int SESSION_TIMEOUT = 3000;
    private final static String ELECTION_NAMESPACE = "/election";
    private ZooKeeper zooKeeper;
    private String currentZeeNodeName;

    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        LeaderElection leaderElection = new LeaderElection();
        leaderElection.connectToZookeeper();
        leaderElection.volunteerForLeaderShip();
        leaderElection.electLeader();
        leaderElection.run();
        leaderElection.close();
        System.out.println("Disconnected from ZooKeeper, exiting application");
    }

    public void volunteerForLeaderShip() throws InterruptedException, KeeperException {
        String zeeNodePrefix = ELECTION_NAMESPACE + "/c_";
        String zeeNodeFullPath = zooKeeper.create(zeeNodePrefix, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        System.out.println("znode name: " + zeeNodeFullPath);
        currentZeeNodeName = zeeNodeFullPath.replace(ELECTION_NAMESPACE + "/", "");
    }

    public void electLeader() throws InterruptedException, KeeperException {
        List<String> children = zooKeeper.getChildren(ELECTION_NAMESPACE, false);
        Collections.sort(children);
        String smallestChild = children.get(0);
        if (smallestChild.equals(currentZeeNodeName)) {
            System.out.println("I'm the leader");
            return;
        }

        System.out.println("I'm not a leader, "+ smallestChild + " is the leader");
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
        }

    }
}
