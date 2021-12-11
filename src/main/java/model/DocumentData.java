package model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class DocumentData implements Serializable {
    private final Map<String, Double> termFrequencyMap = new HashMap<>();

    public void putTermFrequencyData(String term, double termFrequency) { termFrequencyMap.put(term, termFrequency); }

    public double getFrequency(String term) { return termFrequencyMap.get(term); }
}
