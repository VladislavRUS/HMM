import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;


public class ImageProcessor {

    public static long[] getImageWeightCenter(int x, int y, int width, int height, BufferedImage image) {

        long xSum = 0, ySum = 0, mSum = 0;

        if (x + width >= image.getWidth()) {
            width = image.getWidth() - x - 1;
        }

        if (y + height >= image.getHeight()) {
            height = image.getHeight() - y - 1;
        }

        for (int i = x; i < x + width; i++) {
            for (int j = y + height; j > y; j--) {
                int grayScale = getGrayscale(image.getRGB(i, j));

                mSum += grayScale;

                xSum += grayScale * i;
                ySum += grayScale * j;
            }
        }

        return new long[]{xSum / mSum, ySum / mSum};
    }

    private static int getGrayscale(int pixel) {
        int alpha = (pixel >> 24) & 0xff;
        int red = (pixel >> 16) & 0xff;
        int green = (pixel >> 8) & 0xff;
        int blue = (pixel) & 0xff;

        return (red + green + blue) / 3;
    }

    public static List<BufferedImage> splitVertical(BufferedImage image) {

        List<BufferedImage> bufferedImages = new ArrayList<>();

        long[] imageWeightCenter = ImageProcessor.getImageWeightCenter(0, 0, image.getWidth(), image.getHeight(), image);

        BufferedImage left = image.getSubimage(0, 0, (int) imageWeightCenter[0], image.getHeight());
        BufferedImage right = image.getSubimage((int) imageWeightCenter[0], 0, image.getWidth() - (int) imageWeightCenter[0], image.getHeight());

        bufferedImages.add(left);
        bufferedImages.add(right);

        return bufferedImages;
    }

    public static List<BufferedImage> splitHorizontal(BufferedImage image) {

        List<BufferedImage> bufferedImages = new ArrayList<>();

        long[] imageWeightCenter = ImageProcessor.getImageWeightCenter(0, 0, image.getWidth(), image.getHeight(), image);

        BufferedImage bottom = image.getSubimage(0, 0, image.getWidth(), (int) (imageWeightCenter[1]));
        BufferedImage top = image.getSubimage(0, (int) imageWeightCenter[1], image.getWidth(), image.getHeight() - (int) (imageWeightCenter[1]));

        bufferedImages.add(bottom);
        bufferedImages.add(top);

        return bufferedImages;
    }

    public static List<BufferedImage> getSegments(BufferedImage state) {
        List<BufferedImage> segments = new ArrayList<>();

        List<BufferedImage> bufferedImages = splitFour(state);

        for (BufferedImage bufferedImage : bufferedImages) {
            segments.addAll(splitFour(bufferedImage));
        }

        return segments;
    }

    private static List<BufferedImage> splitFour(BufferedImage image) {
        List<BufferedImage> horizontalSplit = splitHorizontal(image);

        List<BufferedImage> fourParts = new ArrayList<>();

        for (BufferedImage part : horizontalSplit) {
            fourParts.addAll(splitVertical(part));
        }

        return fourParts;
    }

    public static int[] getFeatureVector(BufferedImage image) {
        int[][] dct = DCT(image);

        int M = dct.length;
        int N = dct[0].length;

        int length = 5;

        int[] vector = new int[length];
        boolean filled = false;
        int cnt = 0;

        for (int i = 0; i < M; i++) {

            if (filled) {
                break;
            }

            for (int j = 0; j < N; j++) {
                if (cnt == length) {
                    filled = true;
                    break;
                }

                vector[cnt++] = dct[i][j];
            }
        }

        return vector;
    }

    public static int[][] DCT(BufferedImage image) {

        double M = image.getWidth();
        double N = image.getHeight();

        int[][] coefficients = new int[(int) M][(int) N];

        for (int u = 0; u < M; u++) {
            for (int v = 0; v < N; v++) {
                coefficients[u][v] = getCoefficient(u, v, M, N, image);
            }
        }

        return coefficients;
    }

    private static int getCoefficient(int u, int v, double M, double N, BufferedImage image) {

        double alphaU, alphaV;

        if (u == 0) {
            alphaU = 1 / Math.sqrt(M);

        } else {
            alphaU = Math.sqrt(2 / M);
        }

        if (v == 0) {
            alphaV = 1 / Math.sqrt(N);

        } else {
            alphaV = Math.sqrt(2 / N);
        }

        long sum = 0;

        for (int i = 0; i < M; i++) {
            for (int j = 0; j < N; j++) {
                int pixelValue = getGrayscale(image.getRGB(i, j));
                double firstCos = Math.cos(((2 * i + 1) * u * Math.PI) / (2 * M));
                double secondCos = Math.cos(((2 * j + 1) * v * Math.PI) / (2 * N));

                double value = pixelValue * firstCos * secondCos;

                sum += value;
            }
        }

        return (int) (alphaU * alphaV * sum);
    }
}