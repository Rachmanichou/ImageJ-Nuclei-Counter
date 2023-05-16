package com.mycompany.imagej;

import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.type.BooleanType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.Type;

// An image formatter. T is the original image's pixel type, U the boolean thresholded version, S the pixel type of the binary version
public class FormatImage<T extends RealType<T>, U extends BooleanType<U> & NativeType<U>, S extends Type<S>> {
	final Img<T> input ;
	public Img<U> output ;
	public Img<S> typedOutput ;
	
	public FormatImage (final Img<T> input, Img<U> output, Img<S> typedOutput) {
		this.input = input ;
		this.output = output ;
		this.typedOutput = typedOutput ;
	}
	
	// Generate a mask of any PixelType from a binary Img
	public void typeMask (final S MAXVAL) {
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
	
	// Returns whether the pixel's value fit into the thresholds
	private boolean isInBound (final T cursorValue, final T lowThresh, 
			final T highThresh, final double diffThresh) {
		if (cursorValue.compareTo(lowThresh) < diffThresh // TODO: class FloatType cannot be cast to class UnsignedByteType at UnnsignedByteType.compareTo(UnsignedByteType.java:50)
			|| cursorValue.compareTo(highThresh) > diffThresh) {
			return false;
		}
		return true;
	}
}
