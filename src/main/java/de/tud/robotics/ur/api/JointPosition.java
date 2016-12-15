package de.tud.robotics.ur.api;

import java.util.Arrays;

public class JointPosition {

	
	private double[] joints;
	
	public JointPosition() {
		joints = new double[6];
	}
	
	
	public JointPosition(double[] joints) {
		this.joints = joints;
	}

	public void setJoint(int index, double value) {
		if(index < 0) throw new IllegalArgumentException("index >= 0");
		if(index >= joints.length) new IllegalArgumentException("index to big");
		joints[index] = value;
	}

	public double getJoint(int index) {
		if(index < 0) throw new IllegalArgumentException("index >= 0");
		if(index >= joints.length) new IllegalArgumentException("index to big");
		return joints[index];
	}
	@Override
	public String toString() {
		return Arrays.toString(joints);
	}
}
