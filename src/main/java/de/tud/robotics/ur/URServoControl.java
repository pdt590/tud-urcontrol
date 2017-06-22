package de.tud.robotics.ur;

import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;

import de.tud.robotics.ur.api.JointPosition;
import de.tud.robotics.ur.api.MotionExtended;
import de.tud.robotics.ur.strategy.DefaultServoMotionStrategy;
import de.tud.robotics.ur.strategy.MotionStrategy;

public class URServoControl {

	private static final Logger LOG = Logger.getLogger(URServoControl.class.getSimpleName());

	private CommonURClient client;

	private float time;
	private float lookaheadTime;
	private int gain;

	private volatile JointPosition jointPosition;

	private JointPosition lastJointPosition;
	private double[] velocity;
	
	private static int a = -1;
	private static final int v = -1;

	private volatile MotionStrategy strategy;

	private URServoThread thread;
	
	private volatile float speedPercentage = 0.5f;

	private MotionExtended mx;
	
	public URServoControl(CommonURClient client) {
		this.client = client;
		Validate.notNull(client);
		this.strategy = new DefaultServoMotionStrategy();
		this.mx = client.getProxy(MotionExtended.class);;
	}

	public void setTarget(JointPosition pos) {
		if (jointPosition != null && pos.equals(jointPosition, 5))
			return;
		this.jointPosition = pos;
		synchronized (thread) {
			thread.notify();
		}
	}

	public void setStrategy(MotionStrategy strategy) {
		this.strategy = strategy;
	}

	public void start() {
		if(thread == null) thread = new URServoThread();
		if(thread.isAlive()) return;
		thread.start();
	}
	
	public void dispose() {
		thread.interrupt();
		synchronized (thread) {
			thread.notify();
		}
		thread = null;
	}

	private class URServoThread extends Thread {

		public URServoThread() {
			super(client.getName() + "-URServoThread");
		}

		@Override
		public void run() {
			double speed = 0;
			JointPosition currentJointPosition;
			double distance = 0;
			double[] currentVelocity = new double[6];
			int repeatCount = 0;
			
			while (!isInterrupted()) {
				if (jointPosition == null) {
					syncWait(1000);
					continue;
				}
				currentJointPosition = client.getJointData().toJointPosition();

				distance = strategy.calcDistance(currentJointPosition,jointPosition);
				time = strategy.calcTime(distance, speedPercentage);
				lookaheadTime = strategy.calcLookaheadTime(time);
				speed = distance * time;
				gain = strategy.calcGain(speed);
				// get current velocity
				for (int i = 0; i < client.getJointData().getJoints().length; i++) {
					currentVelocity[i] = client.getJointData().getJoints()[i].getQdactual();
					if (currentVelocity[i] < 0 && jointPosition.getJoint(i) - currentJointPosition.getJoint(i) > 0) {
						// es ist eine Richtungsänderung eines Joints
						time = time + (0.1f * time);
						break;
					}
				}

				//LOG.log(Level.FINER, distance + " " + time + "   " + lookaheadTime + "   " + gain);
				//System.out.println("NEUERPUNKT: " + distance + " " + time + "   " + lookaheadTime + "   " + gain);

				mx.servoj(jointPosition, a, v, time, lookaheadTime, gain);
				if(jointPosition.equals(lastJointPosition)) {
					repeatCount++;
					// stop sending position after 80s
					if(repeatCount > 10000) jointPosition = null;
				} else {
					lastJointPosition = jointPosition;
					repeatCount = 0;
				}
				try {
					Thread.sleep(8);
				} catch (InterruptedException e) {
				}
			}
		}

		private void syncWait(long time) {
			synchronized (this) {
				try {
					this.wait(time);
				} catch (InterruptedException e) {
				}
			}
		}
	}

}
