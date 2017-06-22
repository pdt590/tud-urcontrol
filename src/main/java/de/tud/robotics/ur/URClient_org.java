//package de.tud.robotics.ur;
//
//import java.beans.IntrospectionException;
//import java.beans.Introspector;
//import java.beans.PropertyDescriptor;
//import java.io.BufferedWriter;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStreamWriter;
//import java.lang.reflect.InvocationTargetException;
//import java.lang.reflect.Proxy;
//import java.net.Socket;
//import java.net.SocketException;
//import java.net.UnknownHostException;
//import java.nio.ByteBuffer;
//import java.nio.ByteOrder;
//import java.util.Arrays;
//import java.util.Dictionary;
//import java.util.HashMap;
//import java.util.Hashtable;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//
//import org.apache.commons.lang3.Validate;
//
//import com.igormaznitsa.jbbp.exceptions.JBBPParsingException;
//import com.igormaznitsa.jbbp.model.JBBPFieldArrayByte;
//import com.igormaznitsa.jbbp.model.JBBPFieldInt;
//import com.igormaznitsa.jbbp.model.JBBPFieldStruct;
//import com.igormaznitsa.jbbp.model.JBBPFieldUByte;
//
//import de.tud.robotics.ur.api.Joint;
//import de.tud.robotics.ur.api.URScriptInterface;
//import de.tud.robotics.ur.client.JointMode;
//import de.tud.robotics.ur.client.RobotMessageType;
//import de.tud.robotics.ur.client.RobotPackageType;
//import de.tud.robotics.ur.client.data.AdditionalData;
//import de.tud.robotics.ur.client.data.CartesianData;
//import de.tud.robotics.ur.client.data.ConfigurationData;
//import de.tud.robotics.ur.client.data.ForceModeData;
//import de.tud.robotics.ur.client.data.JointData;
//import de.tud.robotics.ur.client.data.KinematicsData;
//import de.tud.robotics.ur.client.data.MasterboardData;
//import de.tud.robotics.ur.client.data.RealtimeCartesianData;
//import de.tud.robotics.ur.client.data.RealtimeJointData;
//import de.tud.robotics.ur.client.data.RealtimeRobotModeData;
//import de.tud.robotics.ur.client.data.RealtimeSingleJointData;
//import de.tud.robotics.ur.client.data.RobotModeData;
//import de.tud.robotics.ur.client.data.RobotPackageData;
//import de.tud.robotics.ur.client.data.ToolData;
//import de.tud.robotics.ur.parser.JBBPFieldDouble;
//import de.tud.robotics.ur.parser.URPackageJBBPParserHelper;
//
//
//public abstract class URClient_org {
//
//	protected static final Logger LOG = Logger.getLogger(URClient_org.class.getSimpleName());
//	
//	protected static final int DEFAULT_TIMEOUT = 1000;
//	
//	private static final int primaryPort = 30001;
//	private static final int secondaryPort = 30002;
//	private static final int realtimePort = 30003;
//			
//	private URClientThread thread;
//
//	private final Map<RobotPackageType, Integer> updateFequences = new HashMap<RobotPackageType, Integer>();
//
//	private String name;
//	private String host;
//
//	private RobotModeData robotModeData;
//	private JointData jointData;
//	private ToolData toolData;
//	private MasterboardData masterBoardData;
//	private CartesianData cartesianData;
//	private KinematicsData kinematicsData;
//	private ConfigurationData configurationData;
//	private ForceModeData forceModeData;
//	private AdditionalData additionalData;
//	
//	private URScriptExtendedInvocationHandler invoker;
//	private Socket socket;
//	private InputStream in;
//	private BufferedWriter writer;
//
//	private URServoControl servoControl;
//
//	private Map<Class<?>, Object> proxies = new HashMap<Class<?>, Object>();
//	private boolean realtime;
//
//	private List<URClientListener> listeners;
//	
//	public URClient_org(String host, boolean realtime) {		
//		this.host = host;
//		this.name = host;
//		Validate.notNull(this.host);
//		this.realtime = realtime;
//		robotModeData = new RobotModeData();
//		jointData = new JointData();
//		toolData = new ToolData();
//		masterBoardData = new MasterboardData();
//		cartesianData = new CartesianData();
//		kinematicsData = new KinematicsData();
//		configurationData = new ConfigurationData();
//		forceModeData = new ForceModeData();
//		additionalData = new AdditionalData();
//
//		listeners = new LinkedList<>();
//		
//		invoker = new URScriptExtendedInvocationHandler(this);
//
//		setUpdateFrequence(RobotPackageType.ROBOT_MODE_DATA, 10);
//		setUpdateFrequence(RobotPackageType.JOINT_DATA, 125);
//		setUpdateFrequence(RobotPackageType.CARTESIAN_DATA, 125);
//		setUpdateFrequence(RobotPackageType.ADDITIONAL_INFO, 10);
//		
//		servoControl = new URServoControl(this);
//	}
//
//	public void setName(String name) {
//		Validate.notNull(name);
//		this.name = name;
//	}
//
//	public String getName() {
//		return name;
//	}
//	
//	public void addListener(URClientListener l) {
//		listeners.add(l);
//	}
//	public void removeListener(URClientListener l) {
//		listeners.remove(l);
//	}
//	
//	public void connect() throws UnknownHostException, IOException {
//		LOG.log(Level.INFO, "connecting to " + name + " " + host + ":" +  (realtime ? realtimePort : secondaryPort));
//		init(host);
//	}
//	
//	public void dispose() {
//		thread.disposing = true;
//		if (thread != null)
//			thread.interrupt();
//		servoControl.dispose();
//	}
//
//	private void init(String host) throws IOException {
//		thread = new URClientThread(this.name);
//		if(realtime) {
//			socket = new Socket(host, realtimePort);
//		} else {
//			socket = new Socket(host, secondaryPort);
//		}
//		socket.setTcpNoDelay(true);
//		socket.setSoTimeout(DEFAULT_TIMEOUT);
//		in = socket.getInputStream();
//		writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
//		thread.start();
//		servoControl.start();
//	}
//
//	/**
//	 * 
//	 * @param packageType
//	 *            type of the package to update
//	 * @param updatesPerSecond
//	 *            how many times the package should be updated in a second 0 =
//	 *            automatic update disabled 125 = MAX update frequence
//	 */
//	public void setUpdateFrequence(RobotPackageType packageType, int updatesPerSecond) {
//		if (updatesPerSecond < 0)
//			updatesPerSecond = 0;
//		if (updatesPerSecond > 125)
//			updatesPerSecond = 125;
//		updateFequences.put(packageType, updatesPerSecond);
//	}
//
//	@SuppressWarnings("unchecked")
//	public <T extends URScriptInterface> T getProxy(Class<T> clazz) {
//		if (proxies.containsKey(clazz))
//			return (T) proxies.get(clazz);
//		else {
//			T result = (T) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { clazz }, invoker);
//			proxies.put(clazz, result);
//			return result;
//		}
//	}
//
//	/**
//	 * send a message to the server
//	 * 
//	 * @param msg
//	 *            the message to be written
//	 */
//
//	public void write(String msg) throws IOException {
//		if (writer == null)
//			return;
//		writer.write(msg);
//		writer.flush();
//	}
//
//	private class URClientThread extends Thread {
//
//		private boolean disposing = false;
//
//		private JBBPFieldStruct messageHeader = null;
//		private int length = 0;
//		private RobotMessageType type = null;
//		private int parsingErrorCount = 0;
//
//		private int expectedCount = 8;
//		private JBBPFieldStruct p;
//		private int packageLength = 0;
//		private RobotPackageType packageType;
//		private byte[] a;
//
//		private long currentTime;
//		// for normal client
//		private RobotModeData rmd;
//		private JointData jd;
//		private ToolData td;
//		private MasterboardData mbd;
//		private CartesianData cd;
//		private ForceModeData fmd;
//		private AdditionalData ad;
//
//		// for realtime client
//		private RealtimeJointData rtjd;
//		private RealtimeSingleJointData[] rtsjd;
//		private RealtimeCartesianData rtcd;
//		private RealtimeRobotModeData rtrmd;
//		private JBBPFieldStruct realtimeMessage;
//
//
//		public URClientThread(String name) {
//			super(name + "-URClientThread");
//		}
//
//		@Override
//		public void run() {
//			try {
//				while (!isInterrupted() && !socket.isClosed()) {
//					if(realtime) {
//						parseRealtimeClientData();
//					} else {
//						parseNormalClientData();
//					}
//				}
//			} catch (SocketException e) {
//				LOG.log(Level.WARNING, getName() + ": closed");
//			} catch (IOException e) {
//				LOG.log(Level.WARNING, "exception while retrieving data from server", e);
//			} finally {
//				disconnect();
//				if (!disposing) {
//					try {
//						connect();
//					} catch (UnknownHostException e) {
//						e.printStackTrace();
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
//				}
//				LOG.log(Level.INFO, "disposing " + this.getName());
//			}
//		}
//
//		
//		private void parseRealtimeClientData() throws IOException {
//			try {
//				messageHeader = URPackageJBBPParserHelper.getRealtimeMessageParser().parse(in);
//			} catch (JBBPParsingException e) {
//				LOG.log(Level.WARNING, "exception while parsing", e);
//				parsingErrorCount++;
//				if (parsingErrorCount > 1000)
//					return;
//			}
//			try {
//			length = messageHeader.findFieldForNameAndType("messagesize", JBBPFieldInt.class).getAsInt();
//			currentTime = System.currentTimeMillis();
//			a = messageHeader.findFieldForType(JBBPFieldArrayByte.class).getArray();
//			// TODO Remove
//			for(int i = 0; i < a.length; i = i+8) {
//				byte[] test = new byte[] {a[i],a[i+1],a[i+2],a[i+3],a[i+4],a[i+5],a[i+6],a[i+7]};
//				System.out.println("test:"+Arrays.toString(test));
//				double d = ByteBuffer.wrap(test).order(ByteOrder.BIG_ENDIAN ).getDouble();
//				
//				System.out.println(d);
//				byte[] bytes = new byte[8];
//			    ByteBuffer.wrap(bytes).putDouble(d);
//			    System.out.println("bytes:"+Arrays.toString(bytes));
//			}
//			realtimeMessage = URPackageJBBPParserHelper.getRealtimePackageParser().parse(a);
//			// parse Joint data
//			rtjd = new RealtimeJointData();
//			rtjd.setSender(name);
//			rtjd.setLastUpdated(currentTime);
//			rtsjd = new RealtimeSingleJointData[6];
//			for(int i= 0; i < rtsjd.length;i++) {
//				rtsjd[i] = new RealtimeSingleJointData();
//				rtsjd[i].setIactual((float)realtimeMessage.findFieldForNameAndType("Iactual"+i,JBBPFieldDouble.class).getAsDouble());
//				rtsjd[i].setJointMode(realtimeMessage.findFieldForNameAndType("jointMode"+i,JBBPFieldDouble.class).getAsInt());
//				rtsjd[i].setQactual(realtimeMessage.findFieldForNameAndType("qtarget"+i,JBBPFieldDouble.class).getAsDouble());
//				rtsjd[i].setQdactual(realtimeMessage.findFieldForNameAndType("qdactual"+i,JBBPFieldDouble.class).getAsDouble());
//				rtsjd[i].setQtarget(realtimeMessage.findFieldForNameAndType("qtarget"+i,JBBPFieldDouble.class).getAsDouble());
//				rtsjd[i].setVactual((float)realtimeMessage.findFieldForNameAndType("vactual"+i,JBBPFieldDouble.class).getAsDouble());
//				rtsjd[i].setTmotor((float)realtimeMessage.findFieldForNameAndType("motorTemperature"+i,JBBPFieldDouble.class).getAsDouble());
//				rtsjd[i].setIcontrol(realtimeMessage.findFieldForNameAndType("Icontrol"+i,JBBPFieldDouble.class).getAsDouble());
//				rtsjd[i].setItarget(realtimeMessage.findFieldForNameAndType("Itarget"+i,JBBPFieldDouble.class).getAsDouble());
//				rtsjd[i].setMtarget(realtimeMessage.findFieldForNameAndType("Mtarget"+i,JBBPFieldDouble.class).getAsDouble());
//				rtsjd[i].setQddtarget(realtimeMessage.findFieldForNameAndType("qddtarget"+i,JBBPFieldDouble.class).getAsDouble());
//				rtsjd[i].setQdtarget(realtimeMessage.findFieldForNameAndType("qdtarget"+i,JBBPFieldDouble.class).getAsDouble());
//			}
//			rtjd.setJoints(rtsjd);
//			jointData = rtjd;
//			// System.out.println(Arrays.toString(jointData.getJointPositionMessage().toArray()));
//			if (!jointData.getJointData(Joint.BASE).getJointMode().equals(JointMode.JOINT_POWER_OFF_MODE)) {
//				notifyListeners(jointData);
//			}
//			
//			// parse cartesian Data
//			rtcd = new RealtimeCartesianData();
//			rtcd.setLastUpdated(currentTime);
//			rtcd.setSender(name);
//			rtcd.setX(realtimeMessage.findFieldForNameAndType("tcpactualX",JBBPFieldDouble.class).getAsDouble());
//			rtcd.setY(realtimeMessage.findFieldForNameAndType("tcpactualY",JBBPFieldDouble.class).getAsDouble());
//			rtcd.setZ(realtimeMessage.findFieldForNameAndType("tcpactualZ",JBBPFieldDouble.class).getAsDouble());
//			rtcd.setRx(realtimeMessage.findFieldForNameAndType("tcpactualRx",JBBPFieldDouble.class).getAsDouble());
//			rtcd.setRy(realtimeMessage.findFieldForNameAndType("tcpactualRy",JBBPFieldDouble.class).getAsDouble());
//			rtcd.setRz(realtimeMessage.findFieldForNameAndType("tcpactualRz",JBBPFieldDouble.class).getAsDouble());
//			if (!rtcd.equals(cartesianData)) {
//				cartesianData = rtcd;
//				notifyListeners(cartesianData);
//			}
//			
//			// parse RobotMode Data
//			// data are not complete dont parse them
//			/*
//			rtrmd = new RealtimeRobotModeData();
//			rtrmd.setLastUpdated(currentTime);
//			rtrmd.setSender(name);
//			//rtrmd.setTimestamp(realtimeMessage.findFieldForNameAndType("time",JBBPFieldDouble.class).getAsDouble());
//			rtrmd.setRobotMode(realtimeMessage.findFieldForNameAndType("robotMode",JBBPFieldDouble.class).getAsInt());
//			rtrmd.setSpeedScaling(realtimeMessage.findFieldForNameAndType("speedScaling",JBBPFieldDouble.class).getAsDouble());
//			if (!rtrmd.equals(robotModeData)) {
//				robotModeData = rtrmd;
//				notifyListeners(robotModeData);
//			}*/
//			} catch(Throwable e) {
//				e.printStackTrace();
//			}
//		}
//
//		private void notifyListeners(RobotPackageData data) {
//			listeners.forEach(l -> l.notify(data));
//		}
//
//		private void parseNormalClientData() throws IOException {
//			try {
//				messageHeader = URPackageJBBPParserHelper.getMessageParser().parse(in);
//			} catch (JBBPParsingException e) {
//				LOG.log(Level.WARNING, "exception while parsing", e);
//				parsingErrorCount++;
//				if (parsingErrorCount > 1000)
//					return;
//			}
//			parsingErrorCount = 0;
//			length = messageHeader.findFieldForNameAndType("packageLength", JBBPFieldInt.class).getAsInt();
//			type = RobotMessageType.from(
//					messageHeader.findFieldForNameAndType("packageType", JBBPFieldUByte.class).getAsInt());
//			// werte nur nachrichten aus, die ein bestimmtes format
//			// haben
//			if (length != 636 || !type.equals(RobotMessageType.ROBOT_STATE))
//				return;
//			// expecting 8 Packages
//			currentTime = System.currentTimeMillis();
//			for (int i = 0; i < expectedCount; i++) {
//				p = URPackageJBBPParserHelper.getPackagepParser().parse(in);
//				packageLength = URPackageJBBPParserHelper.getPackageLength(p);
//				packageType = URPackageJBBPParserHelper.getRobotPackageType(p);
//				// LOG.log(Level.INFO, "receiving package
//				// "+packageType+"("+packageLength+" bytes)");
//				if (true /* toParse.contains(packageType) */) {
//					a = p.findFieldForType(JBBPFieldArrayByte.class).getArray();
//					// System.out.println(Arrays.toString(a));
//					switch (packageType) {
//					case ROBOT_MODE_DATA:
//						if (packageType.equals(RobotModeData.packageType)
//								&& packageLength == RobotModeData.packageLength) {
//							if (isOutdated(RobotPackageType.ROBOT_MODE_DATA, robotModeData, currentTime)) {
//								rmd = URPackageJBBPParserHelper.getRobotModeDataParser().parse(a).mapTo(
//										RobotModeData.class,
//										URPackageJBBPParserHelper.getFloatAndDoubleFieldProcessor());
//								rmd.setLastUpdated(currentTime);
//								rmd.setSender(name);
//								if (!rmd.equals(robotModeData)) {
//									// System.out.println(new Date()+"
//									// update ROBOT_MODE_DATA");
//									robotModeData = rmd;
//									notifyListeners(robotModeData);
//								}
//
//							}
//						} else {
//							LOG.log(Level.INFO,
//									"skipping defect " + packageType + "(" + packageLength + " bytes)");
//						}
//						break;
//					case JOINT_DATA:
//						if (packageType.equals(JointData.packageType)
//								&& packageLength == JointData.packageLength) {
//							if (isOutdated(RobotPackageType.JOINT_DATA, jointData, currentTime)) {
//								jd = URPackageJBBPParserHelper.getJointDataParser().parse(a).mapTo(
//										JointData.class,
//										URPackageJBBPParserHelper.getFloatAndDoubleFieldProcessor());
//								jd.setLastUpdated(currentTime);
//								jd.setSender(name);
//								// System.out.println(String.format("%.4f",
//								// (jd.getJointData(Joint.BASE).getQtarget()-jd.getJointData(Joint.BASE).getQactual()))+"
//								// "+String.format("%.4f",
//								// (jd.getJointData(Joint.ELBOW).getQtarget()-jd.getJointData(Joint.ELBOW).getQactual()))+"
//								// "+String.format("%.4f",
//								// (jd.getJointData(Joint.SHOULDER).getQtarget()-jd.getJointData(Joint.SHOULDER).getQactual()))+"
//								// "+String.format("%.4f",
//								// (jd.getJointData(Joint.WRIST1).getQtarget()-jd.getJointData(Joint.WRIST1).getQactual()))+"
//								// "+String.format("%.4f",
//								// (jd.getJointData(Joint.WRIST2).getQtarget()-jd.getJointData(Joint.WRIST2).getQactual()))+"
//								// "+String.format("%.4f",
//								// (jd.getJointData(Joint.WRIST3).getQtarget()-jd.getJointData(Joint.WRIST3).getQactual())));
//								// System.out.println(Arrays.toString(jointData.toJointMessage().toArray()));
//								if (!jd.equals(jointData)) {
//									jointData = jd;
//									// System.out.println(Arrays.toString(jointData.getJointPositionMessage().toArray()));
//									if (!jointData.getJointData(Joint.BASE).getJointMode()
//											.equals(JointMode.JOINT_POWER_OFF_MODE))
//										notifyListeners(jointData);
//								}
//
//							}
//						} else {
//							LOG.log(Level.INFO,
//									"skipping defect " + packageType + "(" + packageLength + " bytes)");
//						}
//						break;
//					case TOOL_DATA:
//						if (packageType.equals(ToolData.packageType)
//								&& packageLength == ToolData.packageLength) {
//							if (isOutdated(RobotPackageType.TOOL_DATA, toolData, currentTime)) {
//								td = URPackageJBBPParserHelper.getToolDataParser().parse(a).mapTo(
//										ToolData.class,
//										URPackageJBBPParserHelper.getFloatAndDoubleFieldProcessor());
//								td.setLastUpdated(currentTime);
//								td.setSender(name);
//								if (!td.equals(toolData)) {
//									toolData = td;
//									notifyListeners(toolData);
//								}
//							}
//						} else {
//							LOG.log(Level.INFO,
//									"skipping defect " + packageType + "(" + packageLength + " bytes)");
//						}
//						break;
//					case MASTERBOARD_DATA:
//						if (packageType.equals(MasterboardData.packageType)
//								&& packageLength == MasterboardData.packageLength) {
//							if (isOutdated(RobotPackageType.MASTERBOARD_DATA, masterBoardData, currentTime)) {
//								mbd = URPackageJBBPParserHelper.getMasterboardDataParser().parse(a).mapTo(
//										MasterboardData.class,
//										URPackageJBBPParserHelper.getFloatAndDoubleFieldProcessor());
//								mbd.setLastUpdated(currentTime);
//								mbd.setSender(name);
//								if (!mbd.equals(masterBoardData)) {
//									masterBoardData = mbd;
//									notifyListeners(masterBoardData);
//								}
//							}
//						} else {
//							LOG.log(Level.INFO,
//									"skipping defect " + packageType + "(" + packageLength + " bytes)");
//						}
//						break;
//					case CARTESIAN_DATA:
//						if (packageType.equals(CartesianData.packageType)
//								&& packageLength == CartesianData.packageLength) {
//							if (isOutdated(RobotPackageType.CARTESIAN_DATA, cartesianData, currentTime)) {
//								cd = URPackageJBBPParserHelper.getCartesianDataParser().parse(a).mapTo(
//										CartesianData.class,
//										URPackageJBBPParserHelper.getFloatAndDoubleFieldProcessor());
//								cd.setLastUpdated(currentTime);
//								cd.setSender(name);
//								// System.out.println(cartesianData.getX()+"
//								// "+cartesianData.getY()+"
//								// "+cartesianData.getZ());
//								// System.out.println(cartesianData.getRx()+"
//								// "+cartesianData.getRy()+"
//								// "+cartesianData.getRz());
//								if (!cd.equals(cartesianData)) {
//									cartesianData = cd;
//									notifyListeners(cartesianData);
//								}
//							}
//						} else {
//							LOG.log(Level.INFO,
//									"skipping defect " + packageType + "(" + packageLength + " bytes)");
//						}
//						break;
//					case KINEMATICS_DATA:
//						if (packageType.equals(KinematicsData.packageType)
//								&& packageLength == KinematicsData.packageLength) {
//							if (isOutdated(RobotPackageType.KINEMATICS_DATA, kinematicsData, currentTime)) {
//								// skip this package
//								// URPackageJBBPParserHelper.getKinematicDataParser().parse(a);
//								LOG.log(Level.INFO, "KinematicsData not implemented");
//							}
//						}
//						break;
//					case CONFIGURATION_DATA:
//						if (packageType.equals(ConfigurationData.packageType)
//								&& packageLength == ConfigurationData.packageLength) {
//							if (isOutdated(RobotPackageType.CONFIGURATION_DATA, configurationData,
//									currentTime)) {
//								// skip this package
//								// URPackageJBBPParserHelper.getConfigurationDataParser().parse(a);
//								LOG.log(Level.INFO, "ConfigurationData not implemented");
//							}
//
//						}
//						break;
//					case FORCE_MODE_DATA:
//						if (packageType.equals(ForceModeData.packageType)
//								&& packageLength == ForceModeData.packageLength) {
//							if (isOutdated(RobotPackageType.FORCE_MODE_DATA, forceModeData, currentTime)) {
//								fmd = URPackageJBBPParserHelper.getForceModeDataParser().parse(a).mapTo(
//										ForceModeData.class,
//										URPackageJBBPParserHelper.getFloatAndDoubleFieldProcessor());
//								fmd.setLastUpdated(currentTime);
//								fmd.setSender(name);
//								if (!fmd.equals(forceModeData)) {
//									forceModeData = fmd;
//									notifyListeners(forceModeData);
//								}
//							}
//						} else {
//							LOG.log(Level.INFO,
//									"skipping defect " + packageType + "(" + packageLength + " bytes)");
//						}
//						break;
//					case ADDITIONAL_INFO:
//						if (packageType.equals(AdditionalData.packageType)
//								&& packageLength == AdditionalData.packageLength) {
//							if (isOutdated(RobotPackageType.ADDITIONAL_INFO, additionalData, currentTime)) {
//								ad = URPackageJBBPParserHelper.getAdditionalDataParser().parse(a)
//										.mapTo(AdditionalData.class);
//								ad.setLastUpdated(currentTime);
//								ad.setSender(name);
//								if (!ad.equals(additionalData)) {
//									additionalData = ad;
//									notifyListeners(additionalData);
//								}
//							}
//						} else {
//							LOG.log(Level.INFO,
//									"skipping defect " + packageType + "(" + packageLength + " bytes)");
//						}
//						break;
//					case CALIBRATION_DATA:
//						// skip this package
//						// URPackageJBBPParserHelper.getCalibrationDataParser().parse(a);
//					default:
//						break;
//					}
//				}
//			}
//		}
//		/**
//		 * disposes this client
//		 */
//		protected void disconnect() {
//			try {
//				socket.close();
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//
//		private boolean isOutdated(RobotPackageType type, RobotPackageData data, long currentTime) {
//			return updateFequences.get(type) != null
//					&& currentTime - data.getLastUpdated() > 1000 / updateFequences.get(type);
//		}
//
//		/*private Event generateEvent(RobotPackageData data) {
//			Dictionary<String, Object> d = new Hashtable<>();
//			try {
//				for (PropertyDescriptor pd : Introspector.getBeanInfo(data.getClass()).getPropertyDescriptors()) {
//					if (pd.getReadMethod() != null && !"class".equals(pd.getName()))
//						// LOG.log(Level.INFO,
//						// StringUtil.decapitalize(pd.getReadMethod().getName().replace("get",
//						// ""))+" "+pd.getReadMethod().invoke(data));
//						d.put(StringUtil.decapitalize(pd.getReadMethod().getName().replace("get", "")),
//								pd.getReadMethod().invoke(data));
//				}
//			} catch (IllegalAccessException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IllegalArgumentException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (InvocationTargetException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IntrospectionException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			return new Event(Topics.RESPONSE, d);
//		}*/
//	}
//
//	public RobotModeData getRobotModeData() {
//		return robotModeData;
//	}
//
//	public JointData getJointData() {
//		return jointData;
//	}
//
//	public ToolData getToolData() {
//		return toolData;
//	}
//
//	public MasterboardData getMasterBoardData() {
//		return masterBoardData;
//	}
//
//	public CartesianData getCartesianData() {
//		return cartesianData;
//	}
//
//	public KinematicsData getKinematicsData() {
//		return kinematicsData;
//	}
//
//	public ConfigurationData getConfigurationData() {
//		return configurationData;
//	}
//
//	public ForceModeData getForceModeData() {
//		return forceModeData;
//	}
//
//	public AdditionalData getAdditionalData() {
//		return additionalData;
//	}
//
//	public URServoControl getServoControl() {
//		return servoControl;
//	}
//
//}
