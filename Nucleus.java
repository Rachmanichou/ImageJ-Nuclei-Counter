package com.mycompany.imagej;

import net.imglib2.Point ;

public class Nucleus {
	public double area ;
	public Point center ;
	public int nbNeighbors ;
	
	public Nucleus (double area, Point center, int nbNeighbors) {
		this.area = area ;
		this.center = center ;
		this.nbNeighbors = nbNeighbors ;
	}
	
	public Nucleus () {
		this.area = 0 ;
		this.center = null ;
		this.nbNeighbors = 0 ;
	}
}
