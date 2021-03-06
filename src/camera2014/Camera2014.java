/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package camera2014;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.JApplet;
import javax.swing.JPanel;

/**
 *
 * @author 1310
 */
public class Camera2014 extends JApplet implements Runnable {

    public static final long FPS = 25;
    public static final Color redColor = new Color(120, 120, 120)/*new Color(200, 70, 20)*/, blueColor = new Color(20, 150, 120);
    BufferedImage originalImage, filteredImage;
    int width, height;
    int maxRadius = 0, xPos = 0, yPos = 0;

    @Override
    public void init() {
        this.setSize(680, 240);

        add(new Camera2014.CustomPanel());

        (new Thread(this)).start();
    }

    class CustomPanel extends JPanel {

        @Override
        public void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            Graphics2D g2d = (Graphics2D) graphics;

            if (originalImage != null) {
                g2d.drawImage(originalImage, null, 0, 0);
                g2d.drawImage(filteredImage, null, width, 0);

                g2d.setColor(Color.white);
                g2d.drawOval(xPos - maxRadius, yPos - maxRadius, maxRadius * 2, maxRadius * 2);
            }
        }
    }
    int longestHorizontal = 0, xLongest = 0;
    int longestVertical = 0, yLongest = 0;

    @Override
    public void run() {
        long lastRunTime = 0, now;
        while (true) {
            now = System.currentTimeMillis();
            //Only run a maximum of FPS times per second
            if (now - lastRunTime > 1000 / FPS) {
                try {
                    String axisIP = "http://10.13.10.20/jpg/image.jpg?resolution=320x240";
                    URL imageURL = new URL("http://192.168.1.149:8080/shot.jpg");//C:\\Users\\Braden\\Desktop\\blue.jpg");

                    //Get the image from the URL
                    originalImage = ImageIO.read(imageURL);

                    width = originalImage.getWidth();
                    height = originalImage.getHeight();

                    //Get the pixel data from the image
                    int[] pixels = originalImage.getRGB(0, 0, width, height, null, 0, width);
                    //Copy the pixel data so we can use it to make a filtered image
                    int[] filteredPixels = pixels.clone();
                    
                    //Reset everything to 0
                    xPos = 0;
                    yPos = 0;
                    maxRadius = 0;
                    xLongest = 0;
                    yLongest = 0;
                    longestHorizontal = 0;
                    longestVertical = 0;

                    int xFinishLast = 0, yLast = 0;

                    //Apply threshold
                    for (int i = 0; i < pixels.length; i++) {
                        int rgb = pixels[i];

                        //Walk through the image from left to right, then top to bottom
                        //This means we get horizontal "slices" of the image
                        int x = i % width, y = i / width;

                        if (checkBlueThreshold(rgb)) {
                            //If we're in a different "chunk" of this horizontal slice, or if we've moved to the next line
                            if (x > xFinishLast || y > yLast) {
                                int xFinish, yFinish;
                                //Start at the first x value of the current "chunk" of this horizontal slice
                                //Walk through this chunk until we find a pixel outside of our threshold
                                for (xFinish = x; xFinish < width - 1 && checkBlueThreshold(pixels[y * width + xFinish]); xFinish++) {
                                }
                                
                                //We now have the left and right endpoints of this horizontal chunk

                                //Find the midpoint of the chunk
                                int xMid = (x + xFinish) / 2;

                                //Start at the current y value, and walk down the image until we get outside the threshold
                                //The x-value we're walking at is the midpoint we found earlier
                                for (yFinish = y; yFinish < height - 1 && checkBlueThreshold(pixels[yFinish * width + xMid]); yFinish++) {
                                }

                                //Find the midpoint of the vertical chunk
                                int yMid = (y + yFinish) / 2;

                                //Now find the height of each horizontal/vertical chunk
                                int xLength = xFinish - x, yLength = yFinish - y;

                                //filteredPixels[y * width + x] = 0xFFFF00;
                                filteredPixels[yFinish * width + xMid] = 0xFFFF00;

                                //If we've found a new longest horizontal chunk then record it and its midpoint
                                if (xLength > longestHorizontal) {
                                    longestHorizontal = xLength;
                                    xLongest = xMid;
                                }

                                //If we've found a new longest vertical chunk then record it and its midpoint
                                if (yLength > longestVertical) {
                                    longestVertical = yLength;
                                    yLongest = yMid;
                                }

                                //Record where the endpoint of the current chunk is
                                //Record the current y-position of this chunk
                                //These are used to know when to start looking for the next chunk
                                //Either the next chunk will be on the same line with a higher x value, or the next line
                                xFinishLast = xFinish;
                                yLast = y;
                            }
                        } else {
                            //If the pixel is outside our threshold, then don't draw it in the filtered image
                            filteredPixels[i] = 0;
                        }
                    }

                    //Record the position of the longest horizontal and vertical chunks
                    xPos = xLongest;
                    yPos = yLongest;
                    //Determine the radius of the chunks (the larger one determines the radius
                    maxRadius = Math.max(longestHorizontal, longestVertical) / 2;

                    //Create a new image to show the filters on
                    filteredImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                    filteredImage.setRGB(0, 0, width, height, filteredPixels, 0, width);

                    //Refresh the drawing surface
                    repaint();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            //long timeTaken = (System.currentTimeMillis() - now);
            //System.out.println("Algorithm time: " + timeTaken + " FPS: " + (1000 / timeTaken));
        }
    }

    //This function returns whether or not the color specified by rgb is within the threshold specified for red
    private boolean checkRedThreshold(int rgb) {
        int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, b = rgb & 0xFF;
        return r > redColor.getRed() && g < redColor.getGreen() && b < redColor.getBlue();
    }

    //This function returns whether or not the color specified by rgb is within the threshold specified for blue
    private boolean checkBlueThreshold(int rgb) {
        int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, b = rgb & 0xFF;
        return r < blueColor.getRed() && g < blueColor.getGreen() && b > blueColor.getBlue();
    }
}
