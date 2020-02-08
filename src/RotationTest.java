import javax.imageio.ImageIO;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;

public class RotationTest {
    public static void main(String[] args) throws IOException{
        BufferedImage img = ImageIO.read(new File("zolta_morda.png"));

        int width = img.getWidth();
        int height = img.getHeight();
        int[] rgbArray = new int[width*height];
        DataBuffer rgbData = new DataBufferInt(rgbArray, rgbArray.length);
        WritableRaster raster = Raster.createPackedRaster(rgbData, width, height, width,
                new int[]{0xff0000, 0xff00, 0xff}, null);
        ColorModel colorModel = new DirectColorModel(24, 0xff0000, 0xff00, 0xff);
        BufferedImage rotatedImg = new BufferedImage(colorModel, raster, false, null);

        double centerX = (double)width/2;
        double centerY = (double)height/2;
        double angle = 45;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                //translate origin to center of raster
                double[] coords = new double[]{i, j};
                coords = translate2d(coords, -centerX, -centerY);
                coords = rotate2d(coords, angle);
                coords = translate2d(coords, centerX, centerY);
                int newPixelRgb = findFilteredPixelRgb(coords, img);
                rotatedImg.setRGB(i, j, newPixelRgb);

            }
        }

        ImageIO.write(rotatedImg, "png", new File("output.png"));

    }


    private static double[] rotate2d(double[] coords, double angle) {
        double angleInRadians = Math.toRadians(angle);
        double newX = coords[0] * Math.cos(angleInRadians) - coords[1]  * Math.sin(angleInRadians);
        double newY = coords[1]  * Math.cos(angleInRadians) + coords[0] * Math.sin(angleInRadians);
        return new double[]{newX, newY};
    }

    private static double[] translate2d(double[] coords, double offsetX, double offsetY) {
        return new double[]{coords[0] + offsetX, coords[1] + offsetY};
    }

    private static int findFilteredPixelRgb(double[] coords, BufferedImage image) {
        double x = coords[0];
        double y = coords[1];
        try {
            int upperLeftPixelRgbInt = image.getRGB((int) Math.floor(x), (int) Math.floor(y));
            int upperRightPixelRgbInt = image.getRGB((int) Math.ceil(x), (int) Math.floor(y));
            int lowerLeftPixelRgbInt = image.getRGB((int) Math.floor(x), (int) Math.ceil(y));
            int lowerRightPixelRgbInt = image.getRGB((int) Math.ceil(x), (int) Math.ceil(y));

            int[] upperLeftPixelRgbValues = getRgbValues(upperLeftPixelRgbInt);
            int[] upperRightPixelRgbValues = getRgbValues(upperRightPixelRgbInt);
            int[] lowerLeftPixelRgbValues = getRgbValues(lowerLeftPixelRgbInt);
            int[] lowerRightPixelRgbValues = getRgbValues(lowerRightPixelRgbInt);

            int[] desiredPixelRgbValues = new int[3];
            for (int i = 0; i < 3; i++) {
                desiredPixelRgbValues[i] =  (int) Math.round(((1 - x) * (1 - y) * upperLeftPixelRgbValues[i] +
                                                              x * (1 - y) * upperRightPixelRgbValues[i] +
                                                              (1 - x) * y * lowerLeftPixelRgbValues[i] +
                                                              x * y * lowerRightPixelRgbValues[i]));
            }
            return getRgbInt(desiredPixelRgbValues);

        } catch (ArrayIndexOutOfBoundsException e) {
            return 0;
        }
    }

    private static int getRgbInt(int[] rgbVals) {
        int rgbInt = rgbVals[0]; //red 8x0 8x0 8xR
        rgbInt = (rgbInt << 8) + rgbVals[1]; //green 8x0 8xR 8x0 + 8x0 8x0 8xG
        rgbInt = (rgbInt << 8) + rgbVals[2]; //blue 8xR 8xG 8x0 + 8x0 8x0 8xB
        return rgbInt;
    }


    private static int[] getRgbValues(int rgbInt) {
        return new int[]{rgbInt>>16,rgbInt>>8&255,rgbInt&255};
    }


}

/*
// ---------pseudocode----------
// rotation center
double centerX
double centerY
// rotation angle
double angle

for each pixel in image:
    Coords translatedCoords = translate(-centerX, -centerY)
    Coords rotatedCoords = rotate(-angle, translatedCoords)
    Coords translatedBackCoords = translate(centerX, centerY)
    int newPixelRgb = findRgbue(translatedBackCoords)
    pixel.setRgb(newPixelRgb)


function findRgbue(x, y, image) -> int:
    try:
        int upperLeftPixelRgb = image.getRgb(floor(x), floor(y))
        int upperRightPixelRgb = image.getRgb(ceil(x), floor(y))
        int lowerLeftPixelRgb = image.getRgb(floor(x), ceil(y))
        int lowerRightPixelRgb = image.getRgb(ceil(x), ceil(y))

    catch(outOfIndexException e):
        return int(0, 0, 0)

    int desiredPixelRgb = ((1 - x) * (1 - y) * upperLeftPixelRgb +
                           x * (1 - y) * upperRightPixelRgb +
                           (1 - x) * y * lowerLeftPixelRgb +
                           x * y * lowerRightPixelRgb)

    return desiredPixelRgb

 */