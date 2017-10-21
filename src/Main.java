import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class Main {

    private static final String FILE_PREFIX = "segments/";
    public static final String HMM_FILE_PREFIX = "hmm/";

    public static void main(String[] args) throws IOException {
        String path = "signature.jpg";

        File fileImage = new File(path);

        BufferedImage image = ImageIO.read(fileImage);

        //Начальное разделение на две части
        List<BufferedImage> imageList = ImageProcessor.splitHorizontal(image);

        List<BufferedImage> states = new ArrayList<>();

        for (BufferedImage part : imageList) {
            states.addAll(ImageProcessor.splitVertical(part));
        }

        Map<Integer, List<BufferedImage>> segmentsMap = new HashMap<>();

        for (int i = 0; i < states.size(); i++) {
            BufferedImage state = states.get(i);
            segmentsMap.put((i + 1), ImageProcessor.getSegments(state));
        }

        List<int[]> features = new ArrayList<>();
        Map<Integer, List<int[]>> segmentedMap = new HashMap<>();

        for (Map.Entry<Integer, List<BufferedImage>> entry : segmentsMap.entrySet()) {
            Integer state = entry.getKey();
            segmentedMap.put(state, new ArrayList<>());

            List<BufferedImage> segments = entry.getValue();

            segments.forEach(segment -> {
                int[] featureVector = ImageProcessor.getFeatureVector(segment);
                features.add(featureVector);
                segmentedMap.get(state).add(featureVector);
            });

        }

        System.out.println(features.size());

        System.out.println("K-Means started...");

        Map<Integer, List<int[]>> clusters = kMeans(features);

        Map<Integer, List<int[]>> coded = Util.code(clusters);

        double[][] B = new double[4][coded.size()];

        for (int i = 0; i < B.length; i++) {
            for (int j = 0; j < B[0].length; j++) {
                B[i][j] = getObservationProbability(segmentedMap, coded, i + 1, j);
            }
        }

        printMatrix(B);

        double[] PI = new double[] { 1, 0, 0, 0 };
        double[][] A = new double[][] {
                { 0, 1, 0, 0},
                { 0, 0, 1, 0},
                { 0, 0, 0, 1},
                { 0, 0, 0, 0}
        };

        writeMatrixToFile(HMM_FILE_PREFIX + "A.txt", A);
        writeMatrixToFile(HMM_FILE_PREFIX + "B.txt", B);
        writeArrayToFile(HMM_FILE_PREFIX + "PI.txt", PI);

        HMM hmm = new HMM(4, Util.K);
        hmm.a = A;
        hmm.b = B;
        hmm.pi = PI;

        int[] data = new int[64];
        int cnt = 0;

        for (Map.Entry<Integer, List<int[]>> entry : segmentedMap.entrySet()) {
            List<int[]> vectors = entry.getValue();

            for (int[] vector : vectors) {
                data[cnt++] = getVectorCodedIndex(vector, coded);
            }
        }

        double[][] doubles = hmm.forwardProc(data);

        double sum = 0;

        for (int i = 0; i < doubles.length; i++) {
            for (int j = 0; j < doubles[0].length; j++) {
                sum += doubles[i][j];
            }
        }

        System.out.println(sum);
    }

    public static int getVectorCodedIndex(int[] vector, Map<Integer, List<int[]>> coded) {
        int cnt = 0;

        for (Map.Entry<Integer, List<int[]>> entry : coded.entrySet()) {
            List<int[]> vectors = entry.getValue();

            if (vectors.contains(vector)) {
                break;
            }

            cnt++;
        }

        return cnt;
    }

    private static double getObservationProbability(Map<Integer, List<int[]>> segmented, Map<Integer, List<int[]>> coded, int state, int index) {
        List<int[]> stateVectors = segmented.get(state);
        List<int[]> codedVectors = coded.get(index);

        double cnt = 0;
        for (int[] stateVector : stateVectors) {
            if (codedVectors.contains(stateVector)) {
                cnt++;
            }
        }

        return cnt / stateVectors.size();
    }

    public static Map<Integer, List<int[]>> kMeans(List<int[]> features) {
        Map<Integer, List<int[]>> clusters = Util.kMeans(features);

        while (actualNumberOfClusters(clusters) < Util.K) {
            clusters = Util.kMeans(features);
        }

        return clusters;
    }

    private static int actualNumberOfClusters(Map<Integer, List<int[]>> clusters) {
        return clusters.size();
    }

    private static void printClusters(Map<Integer, List<int[]>> clusters) {
        for (Map.Entry<Integer, List<int[]>> entry : clusters.entrySet()) {
            System.out.println("\n Cluster №: " + entry.getKey());

            for (int[] vector : entry.getValue()) {
                System.out.print("[ ");
                for (int i : vector) {
                    System.out.print(i + " ");
                }
                System.out.print("] ");
            }
        }
    }

    private static void writeSegments(Map<Integer, List<BufferedImage>> map) throws IOException {
        int cnt = 0;
        for (Map.Entry<Integer, List<BufferedImage>> entry : map.entrySet()) {
            Integer state = entry.getKey();

            List<BufferedImage> segments = entry.getValue();

            for (BufferedImage segment : segments) {
                ImageIO.write(segment, "jpg", new File(FILE_PREFIX + state + "_" + (cnt++) + ".jpg"));
            }
        }
    }

    public static String getArrayAsString(int[] arr) {
        StringBuilder builder = new StringBuilder();

        for (int v : arr) {
            builder.append(v).append(" ");
        }

        return builder.toString();
    }

    private static void writeArrayToFile(String fileName, double[] array) throws FileNotFoundException {
        File file = new File(fileName);

        PrintWriter writer = new PrintWriter(file);

        writer.println(array.length);

        for (double value : array) {
            writer.println(value);
        }

        writer.close();
    }

    private static void writeMatrixToFile(String fileName, double[][] matrix) throws FileNotFoundException {
        File file = new File(fileName);

        PrintWriter writer = new PrintWriter(file);

        writer.println(matrix.length);
        writer.println(matrix[0].length);

        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                writer.println(matrix[i][j]);
            }
        }

        writer.close();
    }

    private static void printMatrix(double[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                System.out.print(matrix[i][j] + "       ");
            }
            System.out.println();
        }
    }
}