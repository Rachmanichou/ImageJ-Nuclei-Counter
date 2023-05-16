/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

package com.mycompany.imagej;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.process.ImageConverter;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.Point;
import net.imglib2.algorithm.region.hypersphere.HyperSphere;

import org.apache.log4j.BasicConfigurator;
import org.scijava.plugin.Plugin;

import java.util.ArrayList;
import java.awt.Polygon;

/**
 * You should replace the parameter fields with your own inputs and outputs,
 * and replace the {@link run} method implementation with your own logic.
 * </p>
 */
public class NucleiCounter implements PlugIn {
    
    public ArrayList<Nucleus> nucleiList = new ArrayList<Nucleus>();
    public ArrayList<Polygon> nucleiPolygons = new ArrayList<Polygon>();
    
    FloatType LOWTHRESH = new FloatType(34.0F);
    FloatType HIGHTHRESH = new FloatType(96.0F);
    double DIFFTHRESH = 0.0;
    
    @Override
    public void run(String arg) {
    	// get the image into the appropriate format (grey-scale, Img<FloatType>)
    	ImagePlus imp = IJ.getImage();
    	ImageConverter ic = new ImageConverter(imp);
    	ic.convertToGray8();
    	imp.updateImage();
        final Img<FloatType> image = ImageJFunctions.wrap(imp);
        
        // draw nuclei
        PolygonFromThreshold polygonfromthreshold = new PolygonFromThreshold (image, LOWTHRESH, HIGHTHRESH, DIFFTHRESH);
        
        nucleiPolygons = polygonfromthreshold.createPolygons();
        getNucleiProperties();
        displayCentroids(image);
        
        System.out.println("Number of nuclei = " + nucleiList.size());
        for (Nucleus n : nucleiList) {
        	System.out.println("("+n.center.getDoublePosition(0) + ", " + n.center.getDoublePosition(1)+")");
        }
    }
    
    // Draw spheres of radius 1 with every value inside set to 1 at the center of every nucleus
    void displayCentroids (Img<FloatType> img) {
    	for (Nucleus n:nucleiList) {
    		HyperSphere<FloatType> hyperSphere = new HyperSphere<>(img, n.center, 1);
    		for (FloatType value:hyperSphere) {
    			value.setOne();
    		}
    	}
    }
    
    // Loops through polygons and computes centroids and areas
    void getNucleiProperties () {
    	Nucleus nucleus = new Nucleus ();
    	for (int i = 0 ; i < nucleiPolygons.size() ; i++) {
    		nucleus.area = area(nucleiPolygons.get(i));
    		nucleus.center = centroid(nucleiPolygons.get(i));
    		nucleiList.add(nucleus);
    	}
    }
    
    
    // Compute the area of a polygon
    float area (Polygon polygon) {
    	float sum = 0 ;
    	for (int i = 0; i < polygon.npoints ; i++) {
    	    sum += polygon.xpoints[i] * (polygon.ypoints[(i + 1) % polygon.npoints] - 
    	    		polygon.ypoints[(i + polygon.npoints - 1) % polygon.npoints]);
    	}
    	return (float) 0.5 * Math.abs(sum) ;
    }
    
    // Return centroid: "average" point of the polygon
    Point centroid (Polygon polygon) {
    	long x = 0,y = 0;
    	for (int i = 0 ; i < polygon.npoints ; i++) {
    		x += polygon.xpoints[i]/polygon.npoints ;
    		y += polygon.ypoints[i]/polygon.npoints ;
    	}
    	return new Point(x,y);
    }
    
    

    /**
     * This main function serves for development purposes.
     * It allows you to run the plugin immediately out of
     * your integrated development environment (IDE).
     *
     * @param args whatever, it's ignored
     * @throws Exception
     */
    public static void main(final String... args) throws Exception {
        // Add appenders to the logger and initialize the log4j system
    	BasicConfigurator.configure();
    	
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		// see: https://stackoverflow.com/a/7060464/1207769
		Class<?> clazz = NucleiCounter.class;
		java.net.URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
		java.io.File file = new java.io.File(url.toURI());
		System.setProperty("plugins.dir", file.getAbsolutePath());
		
    	// create the ImageJ application context with all available services
        new ImageJ();

        // ask the user for a file to open
        ImagePlus image = IJ.openImage();

        if (image != null) {
            // show the image
            image.show();

            // invoke the plugin
            IJ.runPlugIn(clazz.getName(), "");
        }
    }

}
