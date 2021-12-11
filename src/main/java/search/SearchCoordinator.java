package search;

import cluster.management.ServiceRegistry;
import com.google.protobuf.InvalidProtocolBufferException;
import model.DocumentData;
import model.Result;
import model.SerializationUtils;
import model.Task;
import model.proto.SearchModel;
import network.OnRequestCallback;
import network.WebClient;
import org.apache.zookeeper.KeeperException;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class SearchCoordinator implements OnRequestCallback {
    private static final String ENDPOINT = "/search";
    private static final String BOOK_DIRECTORY = "./resources/books/";
    private final WebClient client;
    private final ServiceRegistry workerServiceRegistery;
    private final List<String> documents;

    public SearchCoordinator(ServiceRegistry workerServiceRegistery, WebClient client) {
        this.workerServiceRegistery = workerServiceRegistery;
        this.client = client;
        this.documents = readDocumentList();
    }

    @Override
    public byte[] handleRequest(byte[] requestPayload) {
        try {
            SearchModel.Request request = SearchModel.Request.parseFrom(requestPayload);
            SearchModel.Response response = createResponse(request);
            return response.toByteArray();
        } catch (InvalidProtocolBufferException | InterruptedException | KeeperException e) {
            e.printStackTrace();
            return SearchModel.Request.getDefaultInstance().toByteArray();
        }
    }

    @Override
    public String getEndpoint() {
        return ENDPOINT;
    }

    private SearchModel.Response createResponse(SearchModel.Request request) throws InterruptedException, KeeperException {
        SearchModel.Response.Builder builder = SearchModel.Response.newBuilder();
        System.out.println("Received search query "+ request.getSearchQuery());
        List<String> searchTerms = TFIDF.getWordsFromLine(request.getSearchQuery());
        List<String> workers = workerServiceRegistery.getAllServiceAddress();
        if (workers.isEmpty()) {
            System.out.println("No available search workers in the cluster");
            return builder.build();
        }

        List<Task> tasks = createTasks(workers.size(), searchTerms);
        List<Result> results = sendTaskToWorkers(workers, tasks);
        List<SearchModel.Response.DocumentStats> sortedDocuments = aggregateResults(results, searchTerms);
        builder.addAllRelevantDocument(sortedDocuments);

        return builder.build();
    }

    private List<SearchModel.Response.DocumentStats> aggregateResults(List<Result> results, List<String> searchTerms) {
        Map<String, DocumentData> allDocumentsResult = new HashMap<>();

        for (Result result: results) {
            allDocumentsResult.putAll(result.getDocumentToDocumentData());
        }
        System.out.println("Calculating score for all the documents");
        Map<Double, List<String>> scoreToDocuments = TFIDF.getDocumentScores(searchTerms, allDocumentsResult);

        return sortDocumentScore(scoreToDocuments);
    }

    private List<SearchModel.Response.DocumentStats> sortDocumentScore(Map<Double, List<String>> scoreToDocuments) {
        List<SearchModel.Response.DocumentStats> sortedDocumentStatsList = new ArrayList<>();
        for (Map.Entry<Double, List<String>> docScorePair: scoreToDocuments.entrySet()) {
            double score = docScorePair.getKey();
            for (String doc: docScorePair.getValue()) {
                File documentPath = new File(doc);
                SearchModel.Response.DocumentStats documentStats = SearchModel.Response.DocumentStats.newBuilder()
                        .setDocumentName(documentPath.getName())
                        .setDocumentSize(documentPath.length())
                        .setScore(score)
                        .build();
                sortedDocumentStatsList.add(documentStats);
            }
        }

        return sortedDocumentStatsList;
    }

    private List<Result> sendTaskToWorkers(List<String> workers, List<Task> tasks) {
        CompletableFuture<Result>[] futures = new CompletableFuture[workers.size()];
        for (int idx = 0; idx < workers.size(); idx++) {
            String worker = workers.get(idx);
            Task task = tasks.get(idx);
            byte[] payload = SerializationUtils.serialize(task);
            futures[idx] = client.sendTask(worker, payload);
        }

        List<Result> results = new ArrayList<>();
        for (CompletableFuture<Result> future: futures) {
            try {
                Result result = future.get();
                results.add(result);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        System.out.printf("Received %d/%d results%n", results.size(), workers.size());
        return results;
    }

    private List<Task> createTasks(int numOfWorkers, List<String> searchTerms) {
        List<List<String>> workersDocuments = splitDocumentList(numOfWorkers, documents);
        List<Task> tasks = new ArrayList<>();
        for (List<String> documentPerWorker: workersDocuments) {
            Task task = new Task(searchTerms, documentPerWorker);
            tasks.add(task);
        }
        return tasks;
    }

    private List<List<String>> splitDocumentList(int numberOfWorkers, List<String> documents) {
        int numberOfDocumentsPerWorker = (documents.size() + numberOfWorkers - 1)/numberOfWorkers;
        List<List<String>> workerDocuments = new ArrayList<>();

        for (int idx = 0; idx < numberOfWorkers; idx++) {
            int firstDocumentIndex = idx * numberOfDocumentsPerWorker;
            int lastDocumentIndex = Math.min(firstDocumentIndex + numberOfDocumentsPerWorker, documents.size());
            if (firstDocumentIndex > lastDocumentIndex) {
                break;
            }
            List<String> currentWorkerDocuments = new ArrayList<>(documents.subList(firstDocumentIndex, lastDocumentIndex));
            workerDocuments.add(currentWorkerDocuments);
        }

        return workerDocuments;
    }

    private List<String> readDocumentList() {
        File documentsDirectory = new File(BOOK_DIRECTORY);
        return Arrays.stream(Objects.requireNonNull(documentsDirectory.list()))
                .map(docName -> BOOK_DIRECTORY + docName)
                .collect(Collectors.toList());
    }
}
