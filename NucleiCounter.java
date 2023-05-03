/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

package com.mycompany.imagej;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.Point;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import java.io.File;
import java.util.ArrayList;
import java.awt.Polygon;

import ij.gui.Roi;
import ij.process.ImageProcessor;
/**
 * You should replace the parameter fields with your own inputs and outputs,
 * and replace the {@link run} method implementation with your own logic.
 * </p>
 */
@Plugin(type = Command.class, menuPath = "Plugins>NucleiCounter")
public class NucleiCounter implements Command {

    @Parameter
    private Dataset currentData;

    @Parameter
    private UIService uiService;

    @Parameter
    private OpService opService;
    
    
    ArrayList<Nucleus> nucleiList = new ArrayList<Nucleus>();
    ArrayList<Polygon> nucleiPolygons = new ArrayList<Polygon>();
    
    @Override
    public void run() {
        final Img<Float> image = currentData.getImgPlus().getImg();
    }
    
    
    // Loops through polygons and computes centroids and areas
    void getNucleiPropreties () {
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
        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        // ask the user for a file to open
        final File file = ij.ui().chooseFile(null, "open");

        if (file != null) {
            // load the dataset
            final Dataset dataset = ij.scifio().datasetIO().open(file.getPath());

            // show the image
            ij.ui().show(dataset);

            // invoke the plugin
            ij.command().run(NucleiCounter.class, true);
        }
    }

}
