# Search Cluster 
#### A naive TF-IDF based implementation of a distributed and fault-tolerant search cluster.

This repo only contains implementation of cluster.

These days there are numerous tools which abstract away the low level implementation and makes deploying distributed systems very easy.

This project is just to play around zookeper and tackle challenges faced while designing a fault-tolerant distributed systems.


Setup
-----
```
docker compose up
```

Working
-------
Work is still in progress to add a frontend server which accept GET request and make a POST call to this cluster.

The leader server currenlty just accept one parameter in the POST request, `search_query`.
See: [search_cluster_query.proto](https://github.com/kakshay21/distributed-search-cluster/blob/main/src/main/java/model/proto/search_cluster_query.proto#L10)
And then leader divide the task and send it to worker instances.


The leader election criteria is also naive at this point. We select the first one among the available pool of workers.
To reduce the herd effect, all the worker listen to their previous worker instance and when the leader node goes down, this chaining helps us to avoid making large number of calls to leader service registry.
