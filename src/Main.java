import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

enum HashFunction {
    MODULE, MULTIPLICATION, FOLDING
}

public class Main {

    static int NUMBERS_SIZE = 1000000;
    static long SEED = 2023;

    public static void main(String[] args) {

        if (!new File("results").exists()) { new File("results").mkdir(); }

        HashFunction[] hashFunctions = { HashFunction.MODULE, HashFunction.MULTIPLICATION, HashFunction.FOLDING };

        int[] tableSizes = { 1000000, 100000, 10000, 1000, 100 };
        int[] numbersOfElements = { 10000, 20000, 100000, 500000, 1000000 };

        for (HashFunction hashFunction : hashFunctions) {

            System.out.println("Hash Function: " + hashFunction);

            CSVExporter resultsCSV = new CSVExporter("results/" + hashFunction + ".csv");
            resultsCSV.addRow(new String[] { "Table Size", "Number of Elements", "Total Insertion Time", "Total Collisions", "Total Search Time" });

            String[][] insertResults = new String[tableSizes.length + 1][numbersOfElements.length + 1];
            insertResults[0][0] = "Table Size x Number of Elements";

            String[][] collisionResults = new String[tableSizes.length + 1][numbersOfElements.length + 1];
            collisionResults[0][0] = "Table Size x Number of Elements";

            String[][] searchResults = new String[tableSizes.length + 1][numbersOfElements.length + 1];
            searchResults[0][0] = "Table Size x Number of Elements";

            for (int i = 0; i < tableSizes.length; i++) {
                insertResults[i + 1][0] = String.valueOf(tableSizes[i]);
                collisionResults[i + 1][0] = String.valueOf(tableSizes[i]);
                searchResults[i + 1][0] = String.valueOf(tableSizes[i]);
            }

            for (int i = 0; i < numbersOfElements.length; i++) {
                insertResults[0][i + 1] = String.valueOf(numbersOfElements[i]);
                collisionResults[0][i + 1] = String.valueOf(numbersOfElements[i]);
                searchResults[0][i + 1] = String.valueOf(numbersOfElements[i]);
            }

            for (int i = 0; i < tableSizes.length; i++) {
                for (int j = 0; j < numbersOfElements.length; j++) {

                    int tableSize = tableSizes[i];
                    int numbersOfElement = numbersOfElements[j];

                    System.out.println();
                    System.out.println("Table Size: " + tableSize + " | Number of Elements: " + numbersOfElement);
                    System.out.println();

                    int runs = 5;

                    double sumInsertTime = 0;
                    int sumCollisions = 0;
                    double sumSearchTime = 0;

                    for (int n = 0; n < runs; n++) {
                        System.out.println((n + 1) + ".");
                        Results results = runTest(tableSize, numbersOfElement, hashFunction);

                        sumInsertTime += results.totalInsertTime();
                        sumCollisions += results.totalCollisions();
                        sumSearchTime += results.totalSearchTime();
                    }

                    double averageInsertTime = (double) Math.round((sumInsertTime / runs) * 1000d) / 1000d;
                    int averageCollisions = Math.round((float) sumCollisions / runs);
                    double averageSearchTime = (double) Math.round((sumSearchTime / runs) * 1000d) / 1000d;

                    resultsCSV.addRow(new String[] {
                            String.valueOf(tableSize),
                            String.valueOf(numbersOfElement),
                            averageInsertTime + "s",
                            String.valueOf(averageCollisions),
                            averageSearchTime + "s"
                    });

                    insertResults[i + 1][j + 1] = averageInsertTime + "s";
                    collisionResults[i + 1][j + 1] = String.valueOf(averageCollisions);
                    searchResults[i + 1][j + 1] = averageSearchTime + "s";

                }
            }

            resultsCSV.close();

            CSVExporter insertCSV = new CSVExporter("results/" + hashFunction + "_insert.csv");
            insertCSV.addRows(insertResults);
            insertCSV.close();

            CSVExporter collisionsCSV = new CSVExporter("results/" + hashFunction + "_collisions.csv");
            collisionsCSV.addRows(collisionResults);
            collisionsCSV.close();

            CSVExporter searchCSV = new CSVExporter("results/" + hashFunction + "_search.csv");
            searchCSV.addRows(searchResults);
            searchCSV.close();

        }

    }

    public static Results runTest(int tableSize, int numberOfElements, HashFunction hashFunction) {

        HashTable hashTable = new HashTable(tableSize, hashFunction);

        Random insertRandom = new Random(SEED);

        long startInsertionTime = System.nanoTime();

        for (int i = 0; i < numberOfElements; i++) {
            int newNumber = insertRandom.nextInt(NUMBERS_SIZE);
            Registry registry = new Registry(newNumber);
            hashTable.insert(registry);
        }

        long endInsertionTime = System.nanoTime();

        double totalInsertionTime = (double) Math.round(((endInsertionTime - startInsertionTime) / 1e9) * 1000d) / 1000d;
        int totalCollisions = hashTable.getCollisions();

        System.out.println("Total Insertion Time: " + totalInsertionTime + " seconds.");
        System.out.println("Total Number of Collisions: " + totalCollisions);

        Random searchRandom = new Random(SEED);

        long startSearchTime = System.nanoTime();

        for (int i = 0; i < numberOfElements; i++) {
            int searchNumber = searchRandom.nextInt(NUMBERS_SIZE);
            Node foundNumber = hashTable.search(searchNumber);

            if (foundNumber == null) {
                throw new RuntimeException("Failed to find a number that should be in the table.");
            }
        }

        long endSearchTime = System.nanoTime();

        double totalSearchTime = (double) Math.round(((endSearchTime - startSearchTime) / 1e9) * 1000d) / 1000d;
        System.out.println("Total Search Time: " + totalSearchTime + " seconds.");

        return new Results(totalInsertionTime, totalCollisions, totalSearchTime);

    }

}

record Results(double totalInsertTime, int totalCollisions, double totalSearchTime) {}

class CSVExporter {

    private FileWriter fileWriter;

    public CSVExporter(String filePath) {
        try {
            this.fileWriter = new FileWriter(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addRow(String[] row) {
        try {

            for (int i = 0; i < row.length; i++) {
                fileWriter.append(row[i]);

                if (i != row.length - 1) {
                    fileWriter.append(",");
                }
            }

            fileWriter.append("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addRows(String[][] rows) {
        for (String[] row : rows) {
            addRow(row);
        }
    }

    public void close() {
        try {
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class HashTable {

    private final List<Node> table;
    private final int size;
    private final HashFunction hashFunction;

    private int collisions;

    public HashTable(int size, HashFunction hashFunction) {
        this.size = size;
        this.table = new ArrayList<>(size);
        this.hashFunction = hashFunction;

        for (int i = 0; i < size; i++) {
            this.table.add(null);
        }

        this.collisions = 0;
    }

    public int getCollisions() {
        return this.collisions;
    }

    public int hashFunction(int key) {

        return switch (this.hashFunction) {
            case MODULE -> key % this.size;
            case MULTIPLICATION -> (int) (this.size * ((key * 0.6180339887) % 1));
            case FOLDING -> {
                String keyString = String.valueOf(key);

                int chunkSize = Math.max(1, keyString.length() / 3);
                int hash = 0;

                for (int i = 0; i < keyString.length(); i += chunkSize) {
                    int endIndex = Math.min(i + chunkSize, keyString.length());
                    String chunk = keyString.substring(i, endIndex);
                    hash += Integer.parseInt(chunk);
                }

                yield hash % this.size;
            }
        };

    }

    public void insert(Registry registry) {
        int index = hashFunction(registry.getValue());
        Node current = table.get(index);

        if (current == null) {
            table.set(index, new Node(registry));
        } else {
            collisions++;

            while (current.getNext() != null) {
                current = current.getNext();
            }

            current.setNext(new Node(registry));
        }
    }

    public Node search(int valor) {
        int index = hashFunction(valor);
        Node current = table.get(index);

        while (current != null) {
            if (current.getRegistry().getValue() == valor) {
                return current;
            }

            current = current.getNext();
        }

        return null;
    }

}

class Registry {

    private static int count = 0;

    private final int value;
    private final String id;

    public Registry(int value) {
        this.value = value;
        this.id = getNextId();
    }

    public int getValue() {
        return this.value;
    }

    public String getId() {
        return this.id;
    }

    private String getNextId() {
        count++;
        return String.format("%09d", count);
    }

}

class Node {

    private final Registry registry;
    private Node next;

    public Node(Registry registry) {
        this.registry = registry;
        this.next = null;
    }

    public Registry getRegistry() {
        return this.registry;
    }

    public void setNext(Node node) {
        this.next = node;
    }

    public Node getNext() {
        return next;
    }

}