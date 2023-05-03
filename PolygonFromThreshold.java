package com.mycompany.imagej;

import java.awt.Polygon;
import java.util.ArrayList;

import net.imglib2.img.Img;
import net.imglib2.type.logic.BoolType; // A BooleanType wrapping a single primitive boolean variable (??)
import net.imglib2.type.numeric.real.FloatType;

// simpler and heavily commented version of the ThresholdToSelection of ij.plugin.filter. Can be used with any thresholding method.
public class PolygonFromThreshold {
	Img<FloatType> img;
	FormatImage<FloatType,BoolType, FloatType> formatImage;
	Img<BoolType> binThresholded;
	public Img<FloatType> thresholdedMask;
	int width, height;
	
	float lowThresh, highThresh ; // thresholding values
	double diffThresh ;
	
	// is constructed with an ImagePlus and operates on an ImageProcessor thresholded mask
	public PolygonFromThreshold(Img<FloatType> img) {
		this.img = img;
		formatImage = new FormatImage<FloatType, BoolType, FloatType>(this.img, binThresholded, thresholdedMask);
		formatImage.createMask(null, null, 0); // TODO
	}
	
	
	public ArrayList<Polygon> createPolygons () {
		width = (int)thresholdedMask.dimension(0);
		height = (int)thresholdedMask.dimension(1);
		return getPolygons();
	}
	
	ArrayList<Polygon> getPolygons () {
		boolean[] prevRow, thisRow;
		ArrayList<Polygon> polygons = new ArrayList<Polygon>(); // unparametrized in original code (!!)
		Outline[] outline; /* outline[x] is the outline that is currently unclosed at the top-left corner of pixel(x,y). 
							  The array is constructed in terms of 8-connected pixels */
		prevRow = new boolean[width + 2];
		thisRow = new boolean[width + 2]; /* prev and thisRow are two pixel too big as thisRow[x+1] selects pixel (x,y) and because
											 they read the next pixel for outline tracing (hence, thisRow[width+1] always returns false)
										  */
		outline = new Outline[width + 1];
		
		for (int y = 0; y <= height; y++) {
			boolean[] b = prevRow;
			prevRow = thisRow;
			thisRow = b;
			int xAfterLowerRightCorner = -1; /* saves the pixel that is NE to a lower-right cornering pixel 
			 								 	which is why it is set to -1 when on the first pixel of the row.
			 								 	When such a case is met, it saves the SE pixel - if selected -
			 								 	to indicate that the outline should continue on the W side of 
			 								 	this pixel
			 								 	If not, such pixels would be skipped, as the outline would be
			 								 	drawn on their left side, even if they are selected
			 								 	This causes disjunct outlines, where certain selections would
			 								 	be segmented to separate rectangular segments
			 								 */
			Outline oAfterLowerRightCorner = null;
			thisRow[1] = y < height ? selected(0,y):false; /* since we need to be able to outline or not the 
															  lower corners of last row pixels, i.e the upper
															  corners of the first out-of-bound row, we make
															  sure that none is selected
			 											   */
			for (int x = 0; x <= width; x++) {
				if (y < height && x < width - 1)
					thisRow[x+2] = selected(x+1,y); // we read one pixel ahead
				else if (x < width - 1)
					thisRow[x+2] = false; // no east-side neighboring pixel within image
				if (thisRow[x+1]) { // i.e if pixel (x,y) is selected
					if (!prevRow[x+1]) { // and if we are on the upper edge of the selected area
						if (outline[x] == null) { // and there is no left outline
							if (outline[x+1] == null) { // nor right outline
								outline[x+1] = outline[x] = new Outline(); // we create a new outline
								outline[x].append(x+1,y);
								outline[x].append(x,y);
							} else { // if there is only a top-right, draw a line from top-right to top-left
								outline[x] = outline[x+1];
								outline[x+1] = null;
								outline[x].append(x,y); // re-create top-right outline as part of a line
							}
						} else if (outline[x+1] == null) { // if (x,y) is selected, is on the upper edge of the selection, and there is no top-right outline
							if (x == xAfterLowerRightCorner) { // and that we are NE to a lower right corner pixel
								outline[x+1] = outline[x];
								outline[x] = oAfterLowerRightCorner; // the line should be drawn on the south and east sides of the pixel
								outline[x].append(x,y); //
								outline[x+1].prepend(x+1,y); // add an outline on the left of the right-hand pixel (on the right of (x,y))
							} else { // if no such case, draw a line from top-left to top-right
								outline[x+1] = outline[x];
								outline[x] = null;
								outline[x+1].prepend(x+1,y);
							}
						} else if (outline[x+1] == outline[x]) { // i.e if the next pixel is unselected
							if (x != xAfterLowerRightCorner && !thisRow[x+2] && 
									prevRow[x+2] && x < width-1 && y < height-1) { // we are at a lower right corner
								outline[x] = null;
								outline[x+1].prepend(x+1,y); // outline[x+1] unchanged
								xAfterLowerRightCorner = x+1;
								oAfterLowerRightCorner = outline[x+1];
							} else { // or there is an unselected area within the selection
								polygons.add(outline[x].getPolygon()); // close the inner hole
								outline[x+1] = null; // no line continues at the right
								outline[x] = (x == xAfterLowerRightCorner) ? oAfterLowerRightCorner : null;
							}
						} else { // both pixels at x and x+1 are selected
							outline[x].prepend(outline[x+1]); // merge their respective selection areas
							for (int x1 = 0; x <= width; x1++) {
								if (x1 != x+1 && outline[x1] == outline[x+1]) { // we reach a right side border
									outline[x1] = outline[x]; // merging is done, replace old selection with new
									outline[x+1] = null;
									outline[x] = (x == xAfterLowerRightCorner) ? oAfterLowerRightCorner : null;
									break;
								}
							}
							if (outline[x+1] != null) {
								throw new RuntimeException("assertion failed");
							}
						}
					}
					if (!thisRow[x]) { // (x,y) is selected but not (x-1,y), i.e left edge, and not on the upper edge
						if (outline[x] == null)
							throw new RuntimeException("assertion failed"); // outline should be drawn
						outline[x].append(x,y+1); // continue left edge above
					}
				} else { // pixel (x,y) is unselected
					if (prevRow[x+1]) { // but (x,y-1) is, i.e we are on the lower edge of the selection
						if (outline[x] == null) {
							if (outline[x+1] == null) { // no outlines: create new top-left and right outlines
								outline[x] = outline[x+1] = new Outline();
								outline[x].append(x,y);
								outline[x].append(x+1,y);
							} else { // only left is null: make line from right to left
								outline[x] = outline[x+1];
								outline[x+1] = null;
								outline[x].prepend(x,y);
							}
						} else if (outline[x+1] == null) { // if there is no top-right outline
							if (x == xAfterLowerRightCorner) { // and (x,y-1) and (x-1,y) are selected
								outline[x+1] = outline[x]; // create outline on north face
								outline[x] = oAfterLowerRightCorner; // which continues south on the west face
								outline[x].prepend(x,y);
								outline[x+1].append(x+1,y);
							} else { // (x,y-1) is selected but (x-1,y) not
								outline[x+1] = outline[x]; // draw line from top-right to top-left
								outline[x] = null;
								outline[x+1].append(x+1,y);
							}
						} else if (outline[x+1] == outline[x]) { // top-left and right outlines and right neighbor has no outlines
							if (x < width-1 && y < height && x!= xAfterLowerRightCorner && 
									thisRow[x+2] && !prevRow[x+2]) { // at lower right corner and next pixel is selected
								outline[x] = null;
								outline[x+1].append(x+1,y);
								xAfterLowerRightCorner = x+1;
								oAfterLowerRightCorner = outline[x+1];
							} else {
								polygons.add(outline[x].getPolygon()); // add filled outline
								outline[x+1] = null;
								outline[x] = (x == xAfterLowerRightCorner) ? oAfterLowerRightCorner : null;
							}
						} else { // top-left and right outlines and right neighbor has its outlines 
							if (x < width-1 && y < height && x!= xAfterLowerRightCorner && 
									thisRow[x+2] && !prevRow[x+2]) { // at a lower right corner and next pixel is selected
								outline[x].append(x+1,y);
								outline[x+1].prepend(x+1,y);
								xAfterLowerRightCorner = x+1;
								oAfterLowerRightCorner = outline[x];
								// outline[x+1] unchanged (the one at the right-hand side of (x, y-1) to the top)
								outline[x] = null;
							} else {
								outline[x].append(outline[x+1]); // merge
								for (int x1 = 0; x1 <= width; x1++) {
									if (x1 != x+1 && outline[x1] == outline[x+1]) {
										outline[x1] = outline[x];
										outline[x+1] = null;
										outline[x] = (x == xAfterLowerRightCorner) ? oAfterLowerRightCorner : null;
										break;
									}
								}
								if (outline[x+1] != null)
									throw new RuntimeException("assertion failed");
							}
						}
					}
					if (thisRow[x]) { // right edge
						if (outline[x] == null)
							throw new RuntimeException("assertion failed");
						outline[x].prepend(x,y+1);
					}
				}
			}
		}
		
		if (polygons.size() == 0)
			return null;
		return polygons;
	}
	
	// Cartesian polygon in progress. Points are stored in a deque so that one can add points to both ends
	static class Outline {
		int[] x,y; // vertices
		int first, last, reserved; // expansion values for beginning and end extremities
		final int GROW = 10; // default extra (spare) space when enlarging arrays (similar performance with 6-20)
		
		public Outline() {
			reserved = GROW;
			x = new int[reserved];
			y = new int[reserved];
			first = last = GROW/2;
		}
		
		// Adds points x,y at the end of the list
		public void append(int x, int y) {
			// special case: the last point (last-1) is on the same edge as the new and last-2 (so the list has to be at least two elements large)
			if (last-first>=2 && collinear(this.x[last-2], this.y[last-2], this.x[last-1], this.y[last-1], x, y)) {
				this.x[last-1] = x; // replace the last point
				this.y[last-1] = y;
			} else { // in any other situation
				needs(0,1); // new point at the end
				this.x[last] = x;
				this.y[last] = y;
				last++; // needed space at the end will now have to be +1 large
			}
		}
		
		// Add point x,y at the beginning of the list
		public void prepend(int x, int y) {
			if (last-first>=2 && collinear(this.x[first+1], this.y[first+1], this.x[first], this.y[first], x , y)) {
				this.x[first] = x; // replace previous point
				this.y[first] = y;
			} else {
				needs(1, 0); // new point
				first--; // the expanded array's size is last-first
				this.x[first] = x;
				this.y[first] = y;
			}
		}
		
		// Merge with another outline at this one's end. The other outline must be deleted or stop being used.
		public void append(Outline o) {
			int size = last - first; // size of this outline
			int oSize = o.last - o.first; // size of other outline
			if (size <= o.first && oSize > reserved - last) { 
				// if this outline fits into the other outline's first end free space and that the other is too large 
				// for this outline's free space
				System.arraycopy(x, first, o.x, o.first - size, size); // prepend this outline's data into the other's
				System.arraycopy(y, first, o.y, o.first - size, size);
				x = o.x;
				y = o.y;
				first = o.first - size;
				last = o.last;
				reserved = o.reserved;
			} else { // append to this outline
				needs(0, oSize);
				System.arraycopy(o.x, o.first, x, last, oSize);
				System.arraycopy(o.y, o.first, y, last, oSize);
				last += oSize;
			}
		}
		
		// Merge with another outline at this one's beginning. The other outline must be deleted or stop being used.
		public void prepend(Outline o) {
			int size = last - first;
			int oSize = o.last - o.first;
			if (size <= o.reserved - o.last && oSize > first) { // not enough space in this array but enough in other's
				System.arraycopy(x, first, o.x, o.last, size);  // so append our own data to that of 'o'
				System.arraycopy(y, first, o.y, o.last, size);
				x = o.x;
				y = o.y;
				first = o.first;
				last = o.last + size;
				reserved = o.reserved;
			} else {  // prepend to our own array
				needs(oSize, 0);
				first -= oSize;
				System.arraycopy(o.x, o.first, x, first, oSize);
				System.arraycopy(o.y, o.first, y, first, oSize);
			}
		}
		/* Is called by methods append and prepend (hence is private) when adding element to the deque. 
		 * Makes sure enough free space is available. If not, enlarges the array*/
		private void needs (int neededAtBegin, int neededAtEnd) {
			if (neededAtBegin > first || neededAtEnd > reserved-last) { // if needs exceed the current bounds
				int extraSpace = Math.max(GROW, Math.abs(x[last-1] - x[first])); //reserve more space for outlines that span large x range
				int newSize = reserved + neededAtBegin + neededAtEnd + extraSpace;
				int newFirst = neededAtBegin + extraSpace/2;
				int[] newX = new int[newSize];
				int[] newY = new int[newSize];
				System.arraycopy(x, first, newX, newFirst, last-first);
				System.arraycopy(y, first, newY, newFirst, last-first);
				x = newX;
				y = newY;
				last += newFirst - first;
				first = newFirst;
				reserved = newSize;
			}
		}
		
		/* Suppresses intermediate points from straight lines (created, e.g, by merging outlines)
		   if points are aligned, the optimized polygon will be one point smaller, so decrement the unoptimized polygon's length value and move on
		   if points are not aligned, we replace the first aligned point with the unaligned one
		   lastly, the <code>count</code> first elements of outline (all unaligned) are stored into a new array of appropriate size */
		public Polygon getPolygon() {
			int i, j = first+1;
			for (i = first+1 ; i+1 < last ; j++) {
				if (collinear(x[j - 1], y[j - 1], x[j], y[j], x[j + 1], y[j + 1])) {
					// merge i+1 into i
					last --;
					continue;
				}
				if (i != j) {
					x[i] = x[j];
					y[i] = y[j];
				}
				i++;
			}
			// wrap around: check alignment between last and first points
			if (collinear(x[j - 1], y[j - 1], x[j], y[j], x[first], y[first]))
				last--;
			else {
				x[i] = x[j];
				y[i] = y[j];
			}
			if (last - first > 2 && collinear(x[last - 1], y[last - 1], x[first], y[first], x[first + 1], y[first + 1]))
				first++;
			
			int count = last - first;
			int[] xNew = new int[count];
			int[] yNew = new int[count];
			System.arraycopy(x, first, xNew, 0, count);
			System.arraycopy(y, first, yNew, 0, count);
			return new Polygon(xNew, yNew, count);
		}
		
		// Returns whether three points are on one straight line
		public boolean collinear(int x1, int y1, int x2, int y2, int x3, int y3) {
			return (x2-x1)*(y3-y2) == (y2-y1)*(x3-x2);
		}

	}
	
    /* Is ran in a thresholded mask. 
     * binThresholded is an Img<BoolType>, setPositionAndGet returns the BoolType of a pixel, 
     * get() extracts the boolean wrapped by the BoolType */
    public boolean selected (int x, int y) {
    	return binThresholded.randomAccess().setPositionAndGet(x,y).get();
    }
}
