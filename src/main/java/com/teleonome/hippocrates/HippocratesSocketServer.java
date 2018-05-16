package com.teleonome.hippocrates;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.BufferUtils;
import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Category;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.spi.LoggingEvent;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.internal.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.utils.Utils;



 
public class HippocratesSocketServer {
	
	String lineSep = System.lineSeparator();
	MqttClient anMqttClient ;
	String mqttBrokerAddress = "tcp://127.0.0.1:1883";
	String mqttClientId = "Hippocrates";
	MemoryPersistence persistence = new MemoryPersistence();
	String mqttStatusTopic="Status";
	int mqttQualityOfService=2;
	public static String LISTEN_ONLY="Listen";
	public static String LISTEN_AND_PUBLISH="Publish";
	private String mode="Listen";
	int debugRecordsToShow=6;
	Buffer fifo = null;
	Hashtable otherTeleonomeNameMessageIndex = new Hashtable();
	
	//static Category cat = Category.getInstance(DiagnosticSocketServer.class.getName());
	 private String buildNumber="14/05/2018 08:27";
	static int port=4712;
	SimpleDateFormat timeOnlyFormat = new SimpleDateFormat("HH:mm:ss");
	
	MqttCallback callback = new MqttCallback() {
		public void connectionLost(Throwable t) {
			try {
				//System.out.println("connection lost");
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				connect();
			} catch (MqttException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}



		public void messageArrived(String topic, MqttMessage message) throws Exception {
			//System.out.println("topic - " + topic + ": " + new String(message.getPayload()));
		}

		public void deliveryComplete(IMqttDeliveryToken token) {
		}



		@Override
		public void messageArrived(MqttTopic topic, MqttMessage message) throws Exception {
			// TODO Auto-generated method stub

		}



		@Override
		public void deliveryComplete(MqttDeliveryToken token) {
			// TODO Auto-generated method stub

		}
	};

	public void setMode(String m) {
		mode=m;
	}
	
	public void setDebugRecordsToShow(int g) {
		debugRecordsToShow=g;
	}
	
	public void connect() throws MqttException{
		try {
			//logger.debug("About to connect to the Heart");
			anMqttClient = new MqttClient(mqttBrokerAddress, mqttClientId, persistence);
			MqttConnectOptions connOpts = new MqttConnectOptions();
			connOpts.setCleanSession(true);
			anMqttClient.setCallback(callback);
			//logger.warn("Connecting to Heart: "+mqttBrokerAddress);
			anMqttClient.connect(connOpts);
			//logger.debug("Connected to Heart");
		} catch (MqttException e1) {
			// TODO Auto-generated catch block
			//logger.warn(Utils.getStringException(e1));
			System.out.println(Utils.getStringException(e1));
		}
	}

	Logger logger;

	public HippocratesSocketServer(){
		
		
		ServerSocket serverSocket;
		try {
			//fifo = new CircularFifoBuffer(debugRecordsToShow);
			 fifo = BufferUtils.synchronizedBuffer(new CircularFifoBuffer(debugRecordsToShow));
			
			serverSocket = new ServerSocket(port);
			String fileName =  Utils.getLocalDirectory() + "lib/Log4J.properties";
			PropertyConfigurator.configure(fileName);
			logger = Logger.getLogger(getClass());
			System.out.print("\033[H\033[2J");  
			System.out.flush();
			RefreshThread aRefreshThread = new RefreshThread();
			aRefreshThread.start();
			logger.warn("Time is " + timeOnlyFormat.format(new Date()) + "   Mode:" + mode + "  DebugsRecordToShow=" + debugRecordsToShow);
			logger.warn("                                                               ");
			logger.warn("Process                Last Log          How Long Ago   Message");
			logger.warn("_______________________________________________________________");
			logger.warn("Hipothalamus                                                   ");
			logger.warn("Heart                                                          ");
			logger.warn("Web                                                            ");
			logger.warn("                                                               ");
			
			try {
				connect();
			} catch(Exception e) {
				e.printStackTrace();
			}
			while(true) {
				//	cat.info("Waiting to accept a new client.");
				Socket socket = serverSocket.accept();
				System.out.println("Connected to client at " + socket.getInetAddress());
				//cat.info("Starting new socket node.");
				new Thread(new SocketNode(this, anMqttClient, socket,mode,debugRecordsToShow,  LogManager.getLoggerRepository())).start();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		


	}

	
	
	
	String lastLogHy="", messageHy="";
	String lastLogHe="        ", messageHe="";
	String lastLogWeb="        " , messageWeb="";
	Timestamp longAgoHy,longAgoHe,longAgoWeb;
	private String longAgoHyText="";
	private String longAgoHeText="";
	private String longAgoWebText="";
	
	public void generateReport(LoggingEvent event) {
		
		SimpleDateFormat timeOnlyFormat = new SimpleDateFormat("HH:mm:ss");
		
		String DATE_PATTERN = "%d";
		//String PATTERN2 = "[%p|%c|] %m";
		String classNamePattern = "%C{1}";
		String messagePattern = "%m";
		SimpleDateFormat sdg = new SimpleDateFormat("yyy-MM-dd HH:mm:ss,SSS");
		PatternLayout layoutDate = new PatternLayout(DATE_PATTERN);
		String dateString = layoutDate.format(event);
		Timestamp messageTime=null;
		try {
			messageTime = new Timestamp(sdg.parse(dateString).getTime());
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		PatternLayout layoutclassNamePattern = new PatternLayout(classNamePattern);
		String className  = layoutclassNamePattern.format(event);
		
		PatternLayout layoutMessagePattern = new PatternLayout(messagePattern);
		String messageText  = layoutMessagePattern.format(event);
		
		String completePattern = "%d [%p|%c|%C{1}] %m%n";
		PatternLayout completePatternLayout = new PatternLayout(completePattern);
		String completeMessage  = completePatternLayout.format(event);
		fifo.add(timeOnlyFormat.format(messageTime) + "   " + className + "  " + messageText);
		
		if(className.contains("PaceMaker$PulseThread") || className.contains("DenomeManager") || className.contains("MnemosyneManager")) {
			lastLogHy=timeOnlyFormat.format(messageTime);
			longAgoHy = messageTime;
			//Utils.getElapsedTimeHoursMinutesSecondsString((System.currentTimeMillis()-messageTime.getTime()));
			messageHy=messageText;
		}else if(className.contains("PaceMaker$SubscriberThread")) {
			
			// logger.info("received from:" + teleonomeName + ":" + teleonomeAddress  + ":" + lastPulseTimestamp);
			String subscriberTeleonomeName = messageText.split("#")[1];
			otherTeleonomeNameMessageIndex.put(subscriberTeleonomeName,messageText);
			
		}else if(className.contains("PublisherListener")) {
			lastLogHe=timeOnlyFormat.format(messageTime);
			longAgoHe = messageTime;
			//Utils.getElapsedTimeHoursMinutesSecondsString((System.currentTimeMillis()-messageTime.getTime()));
			messageHe=messageText;
	
		}else if(className.contains("WebAppContextListener") || className.contains("TeleonomeServlet")) {
			lastLogWeb=timeOnlyFormat.format(messageTime);
			longAgoWeb = messageTime;
			//Utils.getElapsedTimeHoursMinutesSecondsString((System.currentTimeMillis()-messageTime.getTime()));
			messageWeb=messageText;
	
		}
		
		
		
		outputReport();
	}
	
	class RefreshThread extends Thread{
	     
	    public RefreshThread(){
	        setDaemon(true);
	    }
	    public void run(){
	        while(true) {
	        	 outputReport();
		        try {
					Thread.sleep(1000*15);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        }
	    	
	    }
	    
	}
	private void outputReport() {
		
		JSONObject denomeJSONObject=null;
		String pulseTimestamp = "";
		String denomeName="";
		long lastPulseMillis=0;
		try {
			denomeJSONObject = new JSONObject(FileUtils.readFileToString(new File(Utils.getLocalDirectory() + "Teleonome.denome")));
			JSONObject denomeObject = denomeJSONObject.getJSONObject("Denome");
			 pulseTimestamp = denomeJSONObject.getString("Pulse Timestamp");
			 lastPulseMillis = denomeJSONObject.getLong("Pulse Timestamp in Milliseconds");
			 denomeName = denomeObject.getString("Name");
		} catch (JSONException | IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
			
		
		
		if(longAgoHy!=null) {
			longAgoHyText = ""+(System.currentTimeMillis()-longAgoHy.getTime());//Utils.getElapsedTimeHoursMinutesSecondsString((System.currentTimeMillis()-longAgoHy.getTime()));
		}
		
		if(longAgoHe!=null) {
			longAgoHeText = ""+(System.currentTimeMillis()-longAgoHe.getTime());//Utils.getElapsedTimeHoursMinutesSecondsString((System.currentTimeMillis()-longAgoHe.getTime()));
		}
		
		if(longAgoWeb!=null) {
			longAgoWebText = ""+(System.currentTimeMillis()-longAgoWeb.getTime());//Utils.getElapsedTimeHoursMinutesSecondsString((System.currentTimeMillis()-longAgoWeb.getTime()));
		}
		System.out.print("\033[H\033[2J");  
		System.out.flush();  
		logger.warn("                                                                                                           ");
		logger.warn("                                                                                                           ");
		logger.warn("                                                                                                           ");
		logger.warn("                                                                                                           ");
		logger.warn("                                                                                                           ");
		try {
			Runtime.getRuntime().exec("clear");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long sinceMillis = System.currentTimeMillis()-lastPulseMillis;
		String sinceMillisText = Utils.getElapsedTimeHoursMinutesSecondsString(sinceMillis);
				
		logger.warn("                      " +   denomeName + "                    build:" + buildNumber);
		logger.warn("Time is " + timeOnlyFormat.format(new Date()) + "  Last Pulse:"+pulseTimestamp+"     Since:" + sinceMillisText  );
		
		logger.warn("                                                                                                           ");
		logger.warn("                                                                                                           ");
		
		String[] lines = generateProcessMemoryStatus();
		
		logger.warn("Process        Priority      NiceLevel      VirtualMemory          Resident Memory     ShareableMemory    CurrentStatus   CPU %      Mem %   TimeUsedPercentage");
		for(int i=0;i<lines.length;i++) {
			logger.warn(lines[i]);
		}
		
		logger.warn("                                                                                                           ");
		logger.warn("                                                                                                           ");
		
		logger.warn("Mode:" + mode + "  DebugsRecordToShow=" + debugRecordsToShow + " Build:" + buildNumber);
		logger.warn("                                                                                                           ");
		logger.warn("Process                Last Log       Millis since   Message");
		logger.warn("___________________________________________________________________________________________________________");
		logger.warn("Hipothalamus           "+lastLogHy +"          " + longAgoHyText + "        "+ messageHy + "              ");
		logger.warn("Heart                  "+lastLogHe +"          " + longAgoHeText + "        "+messageHe + "               ");
		logger.warn("Web                    "+lastLogWeb +"           " + longAgoWebText + "       "+messageWeb + "             ");
		logger.warn("                                                                                                           ");
		logger.warn("                                                                                                           ");
		
		Iterator it = fifo.iterator();
		String line;
		while(it.hasNext()) {
			line=(String)it.next();
			logger.debug(line);			
		}
		
		
		//
		// the other teleonomes
		// logger.info("received from:" + teleonomeName + ":" + teleonomeAddress  + ":" + lastPulseTimestamp);
		logger.warn("                                                                                                           ");
		
		String subscriberTeleonomeName, message;
		String part1 = String.format("%23s", "");
		String part2 = String.format("%-23s", "Organism");
		
		String headerText = part1+part2 + otherTeleonomeNameMessageIndex.size() + " Teleonomes";
		logger.warn(headerText);
		logger.warn("                                                                                                           ");
		String[] tokens;
		String text;
		String labelText = String.format("%-23s", "Teleonome") + String.format("%-20s","IP Address")+ String.format("%-20s","Pulse Timestamp") + "Status";
		
		logger.info(labelText);
		logger.info("___________________________________________________________________________________________________________");
		
		for(Enumeration<String> en3 = otherTeleonomeNameMessageIndex.keys();en3.hasMoreElements();) {
			subscriberTeleonomeName = en3.nextElement();
			message = (String) otherTeleonomeNameMessageIndex.get(subscriberTeleonomeName);
			//
			// the message format looks like:
			//
			// received from:Sunflower:10.0.0.33:02/07/17 09:51:Status Message
			tokens = message.split("#");
			text = String.format("%-23s", tokens[1]) + String.format("%-20s",tokens[2])+ String.format("%-20s",tokens[3]) + tokens[4];
			logger.info(text);
		}
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	public static String[] generateProcessMemoryStatus() {
		InputStream is=null;
		String[] toReturn = new String[3];
		try {
			
				int pacemakerPid = Integer.parseInt(FileUtils.readFileToString(new File("PaceMakerProcess.info")).split("@")[0]);
			int webPid = Integer.parseInt(FileUtils.readFileToString(new File("WebServerProcess.info")).split("@")[0]);
			int heartProcessInfo = Integer.parseInt(FileUtils.readFileToString(new File("heart/HeartProcess.info")).split("@")[0]);
			String[] cmdArray = { "top", "-n1","-b","-p"+ pacemakerPid , "-p" + webPid , "-p" + heartProcessInfo  };
			ProcessBuilder pb = new ProcessBuilder(cmdArray);
			pb.redirectError();
			
			    Process p = pb.start();
			    
			     is = p.getInputStream();
			    int value = -1;
			    StringBuffer buffer = new StringBuffer();
			    while ((value = is.read()) != -1) {
			    	buffer.append((char)value);
			    }
			    int exitCode = p.waitFor();
			    
			    
			    String[] lines = buffer.toString().split("\\r?\\n");
			    String[] splited;
			//    System.out.println("lines=" + lines.length);
			    for(int i=7;i<lines.length;i++) {
			    	
			    		//24587 pi        20   0  171808  42972   5468 S  0.0 11.3   4:11.48 java
			    		splited = lines[i].trim().split("\\s+");
			    		int processId = Integer.parseInt(splited[0]);
			    		String processName="";
			    		if(processId==pacemakerPid) {
			    			processName  =TeleonomeConstants.PROCESS_HYPOTHALAMUS + "    ";
			    			
			    		}else if(processId==webPid) {
			    			processName  =TeleonomeConstants.PROCESS_HEART + "         ";
			    			
			    		}else if(processId==heartProcessInfo) {
			    			processName  =TeleonomeConstants.PROCESS_WEB_SERVER + "    ";
			    			
			    		}
			    		String user = splited[1];
			    		int priority = Integer.parseInt(splited[2]);
			    		int niceLevel = Integer.parseInt(splited[3]);
			    		int virtualMemoryUedByProcess = Integer.parseInt(splited[4])/1024;
			    		int residentMemoryUedByProcess = Integer.parseInt(splited[5])/1024;
			    		int shareableMemory = Integer.parseInt(splited[6])/1024;
			    		String currentStatus = splited[7];
			    		double cpuUsedByProcessAsPercentage = Double.parseDouble(splited[8]);
			    		double memoryUsedByProcessAsPercentage = Double.parseDouble(splited[9]);
			    		String timeUsedByProcessAsPercentage = splited[10];
			    		String command =  splited[11];
			    		
			    		String line=processName + "     " + priority +"           "+niceLevel+"                "+virtualMemoryUedByProcess +"m                   "+residentMemoryUedByProcess 
			    				+ "m                 "+shareableMemory + "m             "+currentStatus+ "              "+cpuUsedByProcessAsPercentage+ "       "+memoryUsedByProcessAsPercentage
			    				+ "          "+timeUsedByProcessAsPercentage;
			    		
			    		//System.out.println("processStatusDene=" + processStatusDene.toString(4));
			    		toReturn[i-7]=line;
			    	
			    }
			    
			} catch (IOException exp) {
			    exp.printStackTrace();
			} catch (InterruptedException ex) {
			    //Logger.getLogger(JavaApplication256.class.getName()).log(Level.SEVERE, null, ex);
			}
		try {
			is.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return toReturn;
	}
	
	
	public static void main(String argv[]) {
		HippocratesSocketServer h = new HippocratesSocketServer();
		System.out.println("argv.length=" + argv.length);
		if(argv.length==2) {
			if(argv[0].equals(LISTEN_ONLY) || argv[0].equals(LISTEN_AND_PUBLISH))
			h.setMode(argv[0]);
			try{
				int debugRecordsToShow=Integer.parseInt(argv[1]);
				h.setDebugRecordsToShow(debugRecordsToShow);
			}catch(NumberFormatException e) {
				
			}
		}
	}

}
