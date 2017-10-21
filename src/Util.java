import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by User on 29.05.2017.
 */
public class Util {

    public static int K = 5;

    public static Map<Integer, List<int[]>> kMeans(List<int[]> vectors) {
        int k = K;

        if (vectors.size() == 0) {
            throw new RuntimeException("There are no enough vectors!");
        }

        int iterations = 1000;

        int[] min = find(vectors, "min");
        int[] max = find(vectors, "max");


        List<int[]> randomPoints = new ArrayList<>();

        for (int i = 0; i < k; i++) {
            randomPoints.add(getRandomPosition(min, max));
        }


        Map<Integer, List<int[]>> clusters = new HashMap<>();

        while (iterations != 0) {
            iterations--;

            clusters.clear();

            for (int[] point : vectors) {

                int closestRandomPointIdx = findClosestRandomPointIdx(point, randomPoints);
                clusters.putIfAbsent(closestRandomPointIdx, new ArrayList<>());
                clusters.get(closestRandomPointIdx).add(point);

            }

            randomPoints.clear();

            for (Map.Entry<Integer, List<int[]>> entry : clusters.entrySet()) {
                int[] center = findClusterCenter(entry.getValue());

                randomPoints.add(center);
            }
        }

        return clusters;
    }

    private static int[] findClusterCenter(List<int[]> cluster) {
        if (cluster.size() == 0) {
            throw new RuntimeException("Cluster is empty!");
        }

        int[] center = new int[cluster.get(0).length];

        for (int[] vector : cluster) {
            for (int i = 0; i < vector.length; i++) {
                center[i] += vector[i];
            }
        }

        for (int i = 0; i < center.length; i++) {
            center[i] /= cluster.size();
        }

        return center;
    }

    private static int findClosestRandomPointIdx(int[] point, List<int[]> randomPoints) {
        int minDistance = Integer.MAX_VALUE, idx = -1;

        for (int i = 0; i < randomPoints.size(); i++) {
            int distance = getDistanceBetweenTwoPoints(point, randomPoints.get(i));

            if (distance < minDistance) {
                minDistance = distance;
                idx = i;
            }
        }

        return idx;
    }

    private static int getDistanceBetweenTwoPoints(int[] p1, int[] p2) {
        if (p1.length != p2.length) {
            throw new RuntimeException("Vectors are not equal!");
        }

        int sum = 0;
        for (int i = 0; i < p1.length; i++) {
            sum += Math.pow(p1[i] - p2[i], 2);
        }

        return (int) Math.sqrt(sum);
    }

    private static int[] getRandomPosition(int[] min, int[] max) {
        if (min.length != max.length) {
            throw new RuntimeException("Length are not equal!");
        }

        int[] array = new int[min.length];

        for (int i = 0; i < array.length; i++) {
            array[i] = ThreadLocalRandom.current().nextInt(min[i], max[i]);
        }

        return array;
    }

    private static int[] find(List<int[]> vectors, String option) {

        boolean min = option.equals("min");

        int vectorSize = vectors.get(0).length;

        int[] result = new int[vectorSize];

        for (int j = 0; j < vectorSize; j++) {
            int value = vectors.get(0)[j];

            for (int[] vector : vectors) {
                if (min) {
                    if (vector[j] < value) {
                        value = vector[j];
                    }
                } else {
                    if (vector[j] > value) {
                        value = vector[j];
                    }
                }
            }

            result[j] = value;
        }

        return result;
    }

    public static Map<Integer, List<int[]>> code(Map<Integer, List<int[]>> clusters) {

        int[] maxClusterValue = new int[clusters.entrySet().size()];

        for (Map.Entry<Integer, List<int[]>> entry : clusters.entrySet()) {
            List<int[]> vectors = entry.getValue();
            List<int[]> sortedVectors = new ArrayList<>();

            for (int i = 0; i < vectors.size(); i++) {
                sortedVectors.add(sortPrimitiveArrayDescending(vectors.get(i)));
            }

            maxClusterValue[entry.getKey()] = getMaxVectorValue(sortedVectors);
        }


        return getCodedClusters(maxClusterValue, clusters);
    }

    private static Map<Integer, List<int[]>> getCodedClusters(int[] maxClusterValue, Map<Integer, List<int[]>> clusters) {
        Arrays.sort(maxClusterValue);

        Map<Integer, List<int[]>> orderedCluster = new HashMap<>();

        for (int i = 0; i < maxClusterValue.length; i++) {
            orderedCluster.put(i, getClusterThatContainsValue(maxClusterValue[i], clusters));
        }

        return orderedCluster;
    }

    private static List<int[]> getClusterThatContainsValue(int value, Map<Integer, List<int[]>> clusters) {
        for (Map.Entry<Integer, List<int[]>> entry : clusters.entrySet()) {
            List<int[]> vectors = entry.getValue();

            for (int[] vector : vectors) {
                for (int i = 0; i < vector.length; i++) {
                    if (vector[i] == value) {
                        return vectors;
                    }
                }
            }
        }

        throw new RuntimeException("Cluster that contains value was not found!");
    }

    public static int[] sortPrimitiveArrayDescending(int[] array) {
        Integer[] integerArray = getIntegerArray(array);
        Arrays.sort(integerArray, Collections.reverseOrder());
        return getPrimitiveArray(integerArray);
    }

    private static Integer[] getIntegerArray(int[] data) {
        Integer[] result = new Integer[data.length];

        for (int i = 0; i < data.length; i++) {
            result[i] = data[i];
        }

        return result;
    }

    private static int[] getPrimitiveArray(Integer[] data) {
        int[] result = new int[data.length];

        for (int i = 0; i < data.length; i++) {
            result[i] = data[i];
        }

        return result;
    }

    private static int getMaxVectorValue(List<int[]> vectors) {
        int maxValue = vectors.get(0)[0];

        for (int[] vector : vectors) {
            if (vector[0] > maxValue) {
                maxValue = vector[0];
            }
        }

        return maxValue;
    }

    public static int[] getObservableFromImage(String path) throws IOException {
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

        Map<Integer, List<int[]>> clusters = Main.kMeans(features);

        Map<Integer, List<int[]>> coded = code(clusters);

        int[] data = new int[64];
        int cnt = 0;

        for (Map.Entry<Integer, List<int[]>> entry : segmentedMap.entrySet()) {
            List<int[]> vectors = entry.getValue();

            for (int[] vector : vectors) {
                data[cnt++] = Main.getVectorCodedIndex(vector, coded);
            }
        }

        return data;
    }
}