package cluster.management;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.Collections;
import java.util.List;

public class LeaderElection implements Watcher {
    private final ZooKeeper zooKeeper;
    private static final String ELECTION_NAMESPACE = "/election";
    private String currentZnodeName;

    public LeaderElection(ZooKeeper zooKeeper) {
        this.zooKeeper = zooKeeper;
    }

    public void volunteerForLeadership() throws InterruptedException, KeeperException {
        String znodePrefix = ELECTION_NAMESPACE + "/c_";
        String znodeFullPath = zooKeeper.create(znodePrefix, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        currentZnodeName = znodeFullPath.replace(ELECTION_NAMESPACE + "/", "");
    }

    public void reelectLeader() throws InterruptedException, KeeperException {
        String predecessorZnodeName = "";
        Stat predecessorStat = null;
        while (predecessorStat == null) {
            List<String> children = zooKeeper.getChildren(ELECTION_NAMESPACE, this);
            Collections.sort(children);
            String smallestChild = children.get(0);
            if (smallestChild.equals(currentZnodeName)) {
                System.out.println("I'm the leader");
                return;
            } else {
                System.out.println("I'm not the leader");
                int predecessorIndex = Collections.binarySearch(children, currentZnodeName) - 1;
                predecessorZnodeName = children.get(predecessorIndex);
                predecessorStat = zooKeeper.exists(ELECTION_NAMESPACE + "/" + predecessorZnodeName, this);
            }
        }

        System.out.println("Watching node: " + predecessorZnodeName);
        System.out.println();
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        switch (watchedEvent.getType()) {
            case NodeDeleted:
                try {
                    reelectLeader();
                } catch (InterruptedException e) {
                } catch (KeeperException e) {
                }
        }
    }
}
