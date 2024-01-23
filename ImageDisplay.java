import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;

public class ImageDisplay {

    JFrame frame;
    JLabel lbIm1;
    BufferedImage imgOne;
    int width = 512; // default image width and height
    int height = 512;
    int maxLevel = 9;

    private void readImageRGB(int width, int height, String imgPath, BufferedImage img) {
        try {
            int frameLength = width * height * 3;
            File file = new File(imgPath);
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            raf.seek(0);
            long len = frameLength;
            byte[] bytes = new byte[(int) len];
            raf.read(bytes);
            int ind = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    byte a = 0;
                    byte r = bytes[ind];
                    byte g = bytes[ind + height * width];
                    byte b = bytes[ind + height * width * 2];

                    int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                    img.setRGB(x, y, pix);
                    ind++;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private BufferedImage haarTransform(BufferedImage image, int level) {
        level = maxLevel - level;
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage transformedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Initialize arrays to hold RGB values
        float[][] red = new float[width][height];
        float[][] green = new float[width][height];
        float[][] blue = new float[width][height];

        // Extract RGB values from image
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = new Color(image.getRGB(x, y));
                red[x][y] = color.getRed();
                green[x][y] = color.getGreen();
                blue[x][y] = color.getBlue();
            }
        }

        // Perform Haar Transform
        haarTransform2D(red, level);
        haarTransform2D(green, level);
        haarTransform2D(blue, level);

        // Store transformed values back in image
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = Math.min(255, Math.max(0, (int) red[x][y]));
                int g = Math.min(255, Math.max(0, (int) green[x][y]));
                int b = Math.min(255, Math.max(0, (int) blue[x][y]));
                Color color = new Color(r, g, b);
                transformedImage.setRGB(x, y, color.getRGB());
            }
        }
        return transformedImage;
    }

    private void haarTransform2D(float[][] channelData, int level) {
        int width = channelData.length;
        int height = channelData[0].length;

        for (int l = 0; l < level; l++) {
            int w = width >> l;
            int h = height >> l;

            float[][] temp = new float[w][h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w / 2; x++) {
                    temp[x][y] = (channelData[2 * x][y] + channelData[2 * x + 1][y]) / 2;
                    temp[w / 2 + x][y] = (channelData[2 * x][y] - channelData[2 * x + 1][y]) / 2;
                }
            }

            for (int y = 0; y < h / 2; y++) {
                for (int x = 0; x < w; x++) {
                    channelData[x][y] = (temp[x][2 * y] + temp[x][2 * y + 1]) / 2;
                    channelData[x][h / 2 + y] = (temp[x][2 * y] - temp[x][2 * y + 1]) / 2;
                }
            }
        }
    }

    private BufferedImage haarInverseTransform(BufferedImage image, int level) {
        level = maxLevel - level;
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage transformedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Initialize arrays to hold RGB values
        float[][] red = new float[width][height];
        float[][] green = new float[width][height];
        float[][] blue = new float[width][height];

        // Extract RGB values from image
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = new Color(image.getRGB(x, y));
                red[x][y] = color.getRed();
                green[x][y] = color.getGreen();
                blue[x][y] = color.getBlue();
            }
        }

        // Perform Inverse Haar Transform
        haarInverseTransform2D(red, level);
        haarInverseTransform2D(green, level);
        haarInverseTransform2D(blue, level);

        // Store transformed values back in image
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = Math.min(255, Math.max(0, (int) red[x][y]));
                int g = Math.min(255, Math.max(0, (int) green[x][y]));
                int b = Math.min(255, Math.max(0, (int) blue[x][y]));
                Color color = new Color(r, g, b);
                transformedImage.setRGB(x, y, color.getRGB());
            }
        }
        return transformedImage;
    }

    private void haarInverseTransform2D(float[][] channelData, int level) {
        int width = channelData.length;
        int height = channelData[0].length;

        for (int l = level - 1; l >= 0; l--) {
            int w = width >> l;
            int h = height >> l;

            float[][] temp = new float[w][h];
            for (int y = 0; y < h / 2; y++) {
                for (int x = 0; x < w; x++) {
                    temp[x][2 * y] = channelData[x][y] + channelData[x][h / 2 + y];
                    temp[x][2 * y + 1] = channelData[x][y] - channelData[x][h / 2 + y];
                }
            }

            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w / 2; x++) {
                    channelData[2 * x][y] = temp[x][y] + temp[w / 2 + x][y];
                    channelData[2 * x + 1][y] = temp[x][y] - temp[w / 2 + x][y];
                }
            }
        }
    }

    private BufferedImage upscaleImage(BufferedImage image) {
        int newWidth = 512;
        int newHeight = 512;

        // Create a new image with larger dimensions
        BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaledImage.createGraphics();
        g2d.drawImage(image, 0, 0, newWidth, newHeight, null);
        g2d.dispose();
        return scaledImage;
    }

    private void displayImages(BufferedImage originalImage, BufferedImage transformedImage, BufferedImage inverseTransformedImage, BufferedImage upscaledImage) {
        // Initialize JFrame
        frame = new JFrame();
        GridBagLayout gLayout = new GridBagLayout();
        frame.getContentPane().setLayout(gLayout);

        JLabel lbText1 = new JLabel("Original Image (Left) and Haar Transformed Image (Right)");
        lbText1.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel lbText2 = new JLabel("Inverse Haar Transformed Image (Left) and Upscaled Image (Right)");
        lbText2.setHorizontalAlignment(SwingConstants.CENTER);
        lbIm1 = new JLabel(new ImageIcon(originalImage));
        JLabel lbIm2 = new JLabel(new ImageIcon(transformedImage));
        JLabel lbIm3 = new JLabel(new ImageIcon(inverseTransformedImage));
        JLabel lbIm4 = new JLabel(new ImageIcon(upscaledImage));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 0;
        frame.getContentPane().add(lbText1, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 1;
        frame.getContentPane().add(lbIm1, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 1;
        frame.getContentPane().add(lbIm2, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 2;
        frame.getContentPane().add(lbText2, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 3;
        frame.getContentPane().add(lbIm3, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 3;
        frame.getContentPane().add(lbIm4, c);

        frame.pack();
        frame.setVisible(true);
    }

    private void displayImage(BufferedImage img, String title) {
        JFrame frame = new JFrame();
        frame.setTitle(title);
        GridBagLayout gLayout = new GridBagLayout();
        frame.getContentPane().setLayout(gLayout);

        JLabel lbIm1 = new JLabel(new ImageIcon(img));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 0;

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 1;
        frame.getContentPane().add(lbIm1, c);

        frame.pack();
        frame.setVisible(true);
    }

    public void showIms(String[] args) {

        // Read a parameter from command line
        int level = Integer.parseInt(args[1]);
        int n = Integer.parseInt(args[1]);
        if (level < -1 || level > maxLevel) {
            System.out.println("Level must be between -1 and " + maxLevel);
            return;
        }

        // Read in the specified image
        imgOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        readImageRGB(width, height, args[0], imgOne);
        
        // Haar Transform
        // BufferedImage haarTransformedImage = haarTransform(imgOne, level);

        // // Inverse Haar Transform
        // BufferedImage inverseHaarTransformedImage = haarInverseTransform(haarTransformedImage, level);

        // // Upscale the image back to 512x512
        // BufferedImage upscaledImage = upscaleImage(inverseHaarTransformedImage);

        // Display images
        // displayImages(imgOne, haarTransformedImage, inverseHaarTransformedImage, upscaledImage);
        if (n >= 0 && n <= 9) {
            BufferedImage haarTransformedImage = haarTransform(imgOne, level);

        // Inverse Haar Transform
        BufferedImage inverseHaarTransformedImage = haarInverseTransform(haarTransformedImage, level);

        // Upscale the image back to 512x512
        BufferedImage upscaledImage = upscaleImage(inverseHaarTransformedImage);

        // Display images
        displayImage(inverseHaarTransformedImage, "Decoded Image");
        } else if (n == -1) {
            for (int i = 0; i <= maxLevel; i++) {
				// encodeImage(i);
                // BufferedImage img = decodeImage(i);
                // displayImage(img, "Level " + i);
                 BufferedImage haarTransformedImage = haarTransform(imgOne, i);

        // Inverse Haar Transform
        BufferedImage inverseHaarTransformedImage = haarInverseTransform(haarTransformedImage, i);

        // Upscale the image back to 512x512
        BufferedImage upscaledImage = upscaleImage(inverseHaarTransformedImage);

        // Display images
        displayImage(inverseHaarTransformedImage, "Level " + i);
			
                // Add delay
                try {
                    Thread.sleep(600);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
			}
            return;
        } else {
            System.out.println("Invalid value for n. Please enter a value between -1 and 9.");
            return;
        }
        
    }

    public static void main(String[] args) {
        ImageDisplay ren = new ImageDisplay();
        ren.showIms(args);
    }
}
