package search;

import model.DocumentData;

import java.util.*;

public class TFIDF {
    public static double calculateTermFrequency(List<String> words, String term) {
        double count = 0;
        for (String word: words) {
            if (word.equalsIgnoreCase(term)) {
                count++;
            }
        }
        return count/words.size();
    }

    public static DocumentData createDocumentData(List<String> words, List<String> terms) {
        DocumentData documentData = new DocumentData();
        for (String term: terms) {
            double termFrequency = TFIDF.calculateTermFrequency(words, term);
            documentData.putTermFrequencyData(term, termFrequency);
        }
        return documentData;
    }

    public static Map<Double, List<String>> getDocumentScores(List<String> terms,
                                                              Map<String, DocumentData> documentResults) {
        TreeMap<Double, List<String>> scoreToDoc = new TreeMap<>();
        Map<String, Double> termToInverseDocumentFrequency = getTermToInverseDocumentFrequencyMap(terms, documentResults);

        for (String document: documentResults.keySet()) {
            DocumentData documentData = documentResults.get(document);
            double score = calculateDocumentScore(terms, documentData, termToInverseDocumentFrequency);
            addDocumentScoreToTreemap(scoreToDoc, score, document);
        }

        return scoreToDoc.descendingMap();
    }

    private static void addDocumentScoreToTreemap(TreeMap<Double, List<String>> scoreToDoc, double score, String document) {
        List<String> booksWithCurrentScore = scoreToDoc.get(score);
        if (booksWithCurrentScore == null) {
            booksWithCurrentScore = new ArrayList<>();
        }
        booksWithCurrentScore.add(document);
        scoreToDoc.put(score, booksWithCurrentScore);
    }

    private static double calculateDocumentScore(List<String> terms,
                                                 DocumentData documentData,
                                                 Map<String, Double> termToInverseDocumentFrequency) {
        double score = 0;
        for (String term: terms) {
            double termFrequency = documentData.getFrequency(term);
            double inverseDocumentFrequency = termToInverseDocumentFrequency.get(term);
            score += termFrequency * inverseDocumentFrequency;
        }
        return score;
    }

    private static double getInverseDocumentFrequency(String term, Map<String, DocumentData> documentResults) {
        double n = 0;
        for (String document: documentResults.keySet()) {
            DocumentData documentData = documentResults.get(document);
            double termFrequency = documentData.getFrequency(term);
            if (termFrequency > 0.0) {
                n++;
            }
        }
        return n == 0 ? 0 : Math.log(documentResults.size()/n);
    }

    private static Map<String, Double> getTermToInverseDocumentFrequencyMap(List<String> terms, Map<String, DocumentData> documentResults) {
        Map<String, Double> termToIDF = new HashMap<>();
        for (String term: terms) {
            double idf = getInverseDocumentFrequency(term, documentResults);
            termToIDF.put(term, idf);
        }
        return termToIDF;
    }

    public static List<String> getWordsFromDocument(List<String> lines) {
        List<String> words = new ArrayList<>();
        for (String line: lines) {
            words.addAll(getWordsFromLine(line));
        }
        return words;
    }

    public static List<String> getWordsFromLine(String line) {
        return Arrays.asList(line.split("(\\.)+|(,)+|( )+|(-)+|(\\?)+|(!)+|(;)+|(:)+|(/d)+|(/n)+"));
    }
}
