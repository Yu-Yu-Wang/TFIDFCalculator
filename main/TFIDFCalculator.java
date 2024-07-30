import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

public class TFIDFCalculator {
    public static void main(String[] args) {
        try {
            List<String> lines = readLines(args[0]);
            List<String> requests = readLines(args[1]);

            String[] paragraphContent = processParagraphs(lines);
            String[] answerRequest = requests.toArray(new String[0]);

            List<String> requestWords = ListWriter.processList(answerRequest[0].split("\\s+"));
            List<String> requestFiles = ListWriter.processList(answerRequest[1].split("\\s+"));
            
            FileAndDataAnalysis.handle(paragraphContent, requestWords, requestFiles);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<String> readLines(String fileName) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    private static String[] processParagraphs(List<String> lines) {
        int rows = lines.size();
        String[] paragraphContent = new String[rows / 5];
        String[] lineContent = new String[5];
        int paragraphNumber = 0;
        int rows1 = 0;

        for (String line : lines) {
            lineContent[rows1] = ModelAdjust.processText(line);
            rows1 += 1;
            if (rows1 == 5) {
                paragraphContent[paragraphNumber++] = String.join(" ", lineContent);
                rows1 = 0;
            }
        }
        return paragraphContent;
    }
}

class FileAndDataAnalysis {
    public static void handle(String[] paragraphContent, List<String> requestWords, List<String> requestFiles) {
        List<List<String>> nestedList = new ArrayList<>();
        List<Integer> requestFilesInIntType = convertStringListToIntList(requestFiles);

        for (String paragraph : paragraphContent) {
            List<String> stringList = ListWriter.processList(paragraph.split("\\s+"));
            nestedList.add(new ArrayList<>(stringList));
        }

        TFIDF tfidf = new TFIDF();
        List<Double> Answer = new ArrayList<>();
        Answer = tfidf.tfIdfCalculate(nestedList, requestWords, requestFilesInIntType);

        CreateFile.fileGenerator(Answer);
    }

    private static List<Integer> convertStringListToIntList(List<String> requestFiles) {
        List<Integer> intList = new ArrayList<>();
        for (String str : requestFiles) {
            try {
                intList.add(Integer.parseInt(str));
            } catch (NumberFormatException e) {
                System.out.println("Invalid number format for string: " + str);
            }
        }
        return intList;
    }
}

class CreateFile {
    public static void fileGenerator(List<Double> answer) {
        try (BufferedWriter outputWriter = new BufferedWriter(new FileWriter("output.txt"))) {
            for (double value : answer) {
                outputWriter.write(String.format("%.5f", value) + " ");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ModelAdjust {
    public static String processText(String text) {
        return text.replaceAll("[^a-zA-Z]+", " ").toLowerCase().trim();
    }
}

class ListWriter {
    public static List<String> processList(String[] words) {
        List<String> list = new ArrayList<>();
        for (String word : words) {
            list.add(word.toLowerCase());
        }
        return list;
    }
}

class TrieNode {
    TrieNode[] children = new TrieNode[26];
    boolean isEndOfWord = false;
    int count = 0;
    //ArrayList<Integer> ocuurance_in_doc;
    //int total_occurance_in_doc;
}

class Trie {
    TrieNode root = new TrieNode();

    public void insert(String word) {
        TrieNode node = root;
        for (char c : word.toCharArray()) {
            if (node.children[c - 'a'] == null) {
                node.children[c - 'a'] = new TrieNode();
            }
            node = node.children[c - 'a'];
        }
        node.isEndOfWord = true;
        node.count++;
    }

    public int search(String word) {
        TrieNode node = root;
        for (char c : word.toCharArray()) {
            node = node.children[c - 'a'];
            if (node == null) {
                return 0;
            }
        }
        return node.isEndOfWord ? node.count : 0;
    }
}

class TFIDF {
    public List<Double> tf(List<List<String>> docs, List<String> requestWords, List<Integer> requestFilesInIntType) {
        List<Double> TFAnswer = new ArrayList<>();
        for (int i = 0 ; i < requestWords.size() ; i++) {
            Trie trie = new Trie();
            for (String word : docs.get(requestFilesInIntType.get(i))) {
                trie.insert(word);
            }
            int numberTermInDoc = 0;
            numberTermInDoc = trie.search(requestWords.get(i));
            TFAnswer.add((double) numberTermInDoc / docs.get(requestFilesInIntType.get(i)).size());
        }
        
        return TFAnswer;
    }

    public List<Double> idf(List<List<String>> docs, List<String> term) {
        Integer[] numberDocContainTerm = new Integer[term.size()];
        for (int i = 0; i < numberDocContainTerm.length; i++) {
            numberDocContainTerm[i] = 0;
        }
        List<Double> IDFAnswer = new ArrayList<>();
        for (List<String> doc : docs) {
            Trie trie = new Trie();
            for (String word : doc) {
                trie.insert(word);
            }
            for (int i = 0 ; i < term.size() ; i++) {
                if (trie.search(term.get(i)) > 0) {
                    numberDocContainTerm[i]++;
                }
            }
        }
        for (int i = 0 ; i < term.size() ; i++) {
            IDFAnswer.add(Math.log((double) docs.size() / (numberDocContainTerm[i])));
        }
        return IDFAnswer;
    }

    public List<Double> tfIdfCalculate(List<List<String>> nestedList, List<String> requestWords, List<Integer> requestFilesInIntType) {
        List<Double> TFValue = new ArrayList<>();
        List<Double> IDFValue = new ArrayList<>();
        List<Double> Answer = new ArrayList<>();
        TFMultipleIDF tfMultipleIDF = new TFMultipleIDF();
        TFValue = tf(nestedList, requestWords, requestFilesInIntType);
        IDFValue = idf(nestedList, requestWords);
        Answer = tfMultipleIDF.solve(TFValue, IDFValue);
        return Answer;
    }
}

class TFMultipleIDF {
    public List<Double> solve(List<Double> TFValue, List<Double> IDFValue) {
        List<Double> Answer = new ArrayList<>();
        for(int i = 0 ; i < TFValue.size() ; i++) {
            Answer.add(TFValue.get(i) * IDFValue.get(i));
        }
        return Answer;
    }

}

