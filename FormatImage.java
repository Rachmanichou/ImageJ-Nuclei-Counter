package com.mycompany.imagej;

import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.Dimensions;
import net.imglib2.type.BooleanType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.Type;

// An image formatter. T is the original image's pixel type, U the boolean thresholded version, S the pixel type of the binary version
public class FormatImage<T extends RealType<T>, U extends BooleanType<U>, S extends Type<S>> {
	final Img<T> input ;
	public Img<U> output ;
	public Img<S> typedOutput ;
	final Dimensions dimensions ;
	
	public FormatImage (final Img<T> input, Img<U> output, Img<S> typedOutput) {
		this.input = input ;
		this.output = output ;
		this.typedOutput = typedOutput ;
		
		dimensions = input;
		output = output.factory().create(dimensions) ;
		typedOutput = typedOutput.factory().create(dimensions) ;
	}
	
	public void typeMask (final T lowThresh, final T highThresh, final double diffThresh, final S MAXVAL) {
		Cursor<S> cursorTyped = typedOutput.cursor();
		Cursor<U> cursorBool = output.cursor();
		
		while (cursorBool.hasNext()) {
			cursorBool.fwd();
			cursorTyped.fwd();
			
			cursorTyped.next().set(cursorBool.next().get()?null:MAXVAL);
		}
	}
	
	// Creates a copy of the input without the out of bound values. 
	// TODO: have a duplicate of the method but with function interface to pass different thresholding methods from a threshold class
	public void createMask (final T lowThresh, final T highThresh, final double diffThresh) {
		//Create output with same properties and cursors for both images
		Cursor< T > cursorInput = input.cursor();
		Cursor< U > cursorOutput = output.cursor();

		// Iterate over the input
		while (cursorInput.hasNext()) {
			cursorInput.fwd();
			cursorOutput.fwd();

			cursorOutput.next().set(isInBound(cursorInput.next(), lowThresh, highThresh, diffThresh));
		}
	}
	
	// Returns whether the pixel's value fit into the threshold
	private boolean isInBound (final T cursorValue, final T lowThresh, 
			final T highThresh, final double diffThresh) {
		if (cursorValue.compareTo(lowThresh) < diffThresh) {
			return false;
		} else if (cursorValue.compareTo(highThresh) > diffThresh) {
			return false;
		}
		return true;
	}
	
	/* the contrast value defines the maximal difference between a pixel value and the max pixel value.
	 * If the pixel value is out of bound, it is set to the max or min value, depending if it is higher or lower
	 * than the mean or median value. If is equal to the latter, it is set to higher. Because. */
	/*public void adjustContrast (final T nullValue, final double contrast) {
		T min = input.firstElement().createVariable();
		T max = input.firstElement().createVariable();
		
		computeMinMax(min,max); // TODO mean or median pls
		
		createMask(min, max, contrast);
	}
	
	public void computeMinMax(T min, T max) {
		final Cursor<T> cursor = (Cursor<T>)input.cursor();
		T type = cursor.next();
		
		min = type;
		max = type;
		
		while (cursor.hasNext()) {
			type = cursor.next();
			
			if (type.compareTo(min)<0)
				min = type;
			
			if (type.compareTo(max)>0)
				max = type;
		}
	}*/
}
