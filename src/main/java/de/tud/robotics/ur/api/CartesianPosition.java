package de.tud.robotics.ur.api;

import java.util.Arrays;

public class CartesianPosition {
	
	private double x;
	private double y;
	private double z;
	private double rx;
	private double ry;
	private double rz;
	
	
	public CartesianPosition(double[] arr) {
		this.x = arr[0];
		this.y = arr[1];
		this.z = arr[2];
		this.rx = arr[3];
		this.ry = arr[4];
		this.rz = arr[5];
	}
	
	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}

	public double getZ() {
		return z;
	}

	public void setZ(double z) {
		this.z = z;
	}

	public double getRx() {
		return rx;
	}

	public void setRx(double rx) {
		this.rx = rx;
	}

	public double getRy() {
		return ry;
	}

	public void setRy(double ry) {
		this.ry = ry;
	}

	public double getRz() {
		return rz;
	}

	public void setRz(double rz) {
		this.rz = rz;
	}

	@Override
	public String toString() {
		return Arrays.toString(new double[]{x,y,z,rx,ry,rz});
	}

	
}
