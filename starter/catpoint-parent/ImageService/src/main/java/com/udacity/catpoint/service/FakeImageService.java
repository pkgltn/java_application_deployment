package com.udacity.catpoint.service;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * Service that tries to guess if an image displays a cat.
 */
public class FakeImageService implements ImageService  {
    private final Random r = new Random();

    public boolean getImageContainsCat() {
        return imageContainsCat;
    }

    private boolean imageContainsCat;

    File sample_cat = new File("java_application_deployment\\starter\\catpoint-parent\\sample-cat.jpg/");
    File sample_not_cat = new File("java_application_deployment\\starter\\catpoint-parent\\sample-not-cat.jpg/");
    File sample_not_a_cat_fail = new File("java_application_deployment\\starter\\catpoint-parent\\sample-not-a-cat-fail.jpg/");

    private BufferedImage sampleCatImage;
    private BufferedImage sampleNotCatImage;
    private BufferedImage sampleNotACatFailImage;

    private boolean equalImages(BufferedImage image1,BufferedImage image2){
        boolean b=true;
        if (image1.getWidth() == image2.getWidth() && image1.getHeight() == image2.getHeight()) {
            for (int x = 0; x < image1.getWidth(); x++) {
                for (int y = 0; y < image1.getHeight(); y++) {
                    if (image1.getRGB(x, y) != image2.getRGB(x, y))
                        b=false;
                    }
                }
            } else {
                b= false;
            }


        return b;
    }
    public boolean imageContainsCat(BufferedImage image, float confidenceThreshold) {

        try {
            if(image!=null) {
            sampleCatImage= ImageIO.read(sample_cat);
            sampleNotCatImage= ImageIO.read(sample_not_cat);
            sampleNotACatFailImage= ImageIO.read(sample_not_a_cat_fail);
            boolean isSampleCat=equalImages(image,sampleCatImage);
            boolean isSampleNotCat=equalImages(image,sampleNotCatImage);
            boolean isSampleNotACatFail=equalImages(image,sampleNotACatFailImage);
            if(isSampleCat){
                imageContainsCat=true;
            }
            if(isSampleNotCat || isSampleNotACatFail){
                imageContainsCat=false;
            }
            }else {
                JOptionPane.showMessageDialog(null, "Invalid image selected.");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Invalid image selected.");
        }



        //imageContainsCat=r.nextBoolean();
        return imageContainsCat;
    }
}
