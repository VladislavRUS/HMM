import java.io.*;

/**
 * Created by User on 28.05.2017.
 */
public class Test {

    private static final String TEST_IMAGE = "test/test_signature.jpg";
    //private static final String TEST_IMAGE = "test/lena_top.jpg";

    public static void main(String[] args) throws IOException {
        double[][] A = readMatrixFromFile(Main.HMM_FILE_PREFIX + "A.txt");
        double[][] B = readMatrixFromFile(Main.HMM_FILE_PREFIX + "B.txt");
        double[] PI = readArrayFromFile(Main.HMM_FILE_PREFIX + "PI.txt");

        HMM markovModel = new HMM(4, Util.K);

        markovModel.a = A;
        markovModel.b = B;
        markovModel.pi = PI;

        double totalSum = 0, iterations = 1;

        for (int i = 0; i < iterations; i++) {
            System.out.println("Iteration: " + i);

            int[] observableFromImage = Util.getObservableFromImage(TEST_IMAGE);
            double[][] forwardProc = markovModel.forwardProc(observableFromImage);
            totalSum += getMatrixSum(forwardProc);
        }

        System.out.println(totalSum / iterations);
    }

    private static double getMatrixSum(double[][] matrix) {
        double sum = 0;

        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                sum += matrix[i][j];
            }
        }

        return sum;
    }

    private static double[][] readMatrixFromFile(String fileName) throws IOException {
        File file = new File(fileName);

        BufferedReader reader = new BufferedReader(new FileReader(file));

        int height = Integer.parseInt(reader.readLine());
        int width = Integer.parseInt(reader.readLine());

        double[][] matrix = new double[height][width];

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                matrix[i][j] = Double.parseDouble(reader.readLine());
            }
        }

        reader.close();

        return matrix;
    }

    private static double[] readArrayFromFile(String fileName) throws IOException {
        File file = new File(fileName);

        BufferedReader reader = new BufferedReader(new FileReader(file));

        int length = Integer.parseInt(reader.readLine());

        double[] array = new double[length];

        for (int i = 0; i < length; i++) {
            array[i] = Double.parseDouble(reader.readLine());
        }

        reader.close();

        return array;
    }
}
