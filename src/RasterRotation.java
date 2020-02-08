import javax.imageio.ImageIO;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;

public class RasterRotation {
    public static void main(String[] args) throws IOException{
        if (args.length == 0) {
            System.out.println("USAGE: java RasterRotation <input> <output> <angle>");
            return;
        }

        // reading args
        File input = new File(args[0]);
        String output_string = args[1];
        File output = new File(output_string);
        double angle = Double.parseDouble(args[2]);

        // small code block for getting output file extension from args
        String outputFileExtension = "";
        int extensionDotIndex = output_string.lastIndexOf('.');
        if (extensionDotIndex > 0) {
            outputFileExtension = output_string.substring(extensionDotIndex + 1);
        }

        // read input image
        BufferedImage img = ImageIO.read(input);

        int width = img.getWidth();
        int height = img.getHeight();

        // setup for creating output image object
        int[] rgbArray = new int[width*height];
        DataBuffer rgbData = new DataBufferInt(rgbArray, rgbArray.length);
        WritableRaster raster = Raster.createPackedRaster(rgbData, width, height, width,
                new int[]{0xff0000, 0xff00, 0xff}, null);
        ColorModel colorModel = new DirectColorModel(24, 0xff0000, 0xff00, 0xff);
        BufferedImage rotatedImg = new BufferedImage(colorModel, raster, false, null);

        // setting center of rotation to middle of raster matrix
        double centerX = (double)width/2;
        double centerY = (double)height/2;

        // looping over dest (rotatedImg) pixels and setting their rgb values
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                double[] coords = new double[]{i, j};
                // translating, rotating, then translating back to rotate relative to the image center
                coords = translate2d(coords, -centerX, -centerY);
                coords = rotate2d(coords, angle);
                coords = translate2d(coords, centerX, centerY);

                int newPixelRgb = findFilteredPixelRgb(coords, img);
                rotatedImg.setRGB(i, j, newPixelRgb);

            }
        }
        ImageIO.write(rotatedImg, outputFileExtension, output);
        System.out.println("Image rotated by " + angle + " degrees.");

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

    // "map by area" algorithm
    private static int findFilteredPixelRgb(double[] coords, BufferedImage image) {
        double x = coords[0];
        double y = coords[1];
        int floorX = (int) Math.floor(x);
        int ceilX = (int) Math.ceil(x);
        int floorY = (int) Math.floor(y);
        int ceilY = (int) Math.ceil(y);

        try {
            // 24bit encoded rgb ints of nearest overlapping src pixels
            int upperLeftPixelRgbInt = image.getRGB(floorX, floorY);
            int upperRightPixelRgbInt = image.getRGB(ceilX, floorY);
            int lowerLeftPixelRgbInt = image.getRGB(floorX, ceilY);
            int lowerRightPixelRgbInt = image.getRGB(ceilX, ceilY);

            // converting to {r,g,b} int value arrays
            int[] upperLeftPixelRgbValues = getRgbValues(upperLeftPixelRgbInt);
            int[] upperRightPixelRgbValues = getRgbValues(upperRightPixelRgbInt);
            int[] lowerLeftPixelRgbValues = getRgbValues(lowerLeftPixelRgbInt);
            int[] lowerRightPixelRgbValues = getRgbValues(lowerRightPixelRgbInt);

            // x and y relative to the upper left overlapping src pixel
            double relX = x - floorX;
            double relY = y - floorY;
            int[] desiredPixelRgbValues = new int[3];

            // calculating overlapping-area weighted averages from nearest src pixels for each color channel
            for (int i = 0; i < 3; i++) {
                desiredPixelRgbValues[i] =  (int) Math.round(((1 - relX) * (1 - relY) * upperLeftPixelRgbValues[i] +
                        relX * (1 - relY) * upperRightPixelRgbValues[i] +
                        (1 - relX) * relY * lowerLeftPixelRgbValues[i] +
                        relX * relY * lowerRightPixelRgbValues[i]));
            }
            return getRgbInt(desiredPixelRgbValues);

        } catch (ArrayIndexOutOfBoundsException e) {
            // if this area doesn't map to any pre-rotation src pixel, return a black int rgb
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