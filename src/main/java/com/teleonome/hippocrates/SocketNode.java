package com.teleonome.hippocrates;


import java.net.Socket;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Queue;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.BufferedInputStream;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.log4j.*;
import org.apache.log4j.spi.*;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.teleonome.framework.utils.Utils;

// Contributors:  Moses Hohman 

/**
   Read {@link LoggingEvent} objects sent from a remote client using
   Sockets (TCP). These logging events are logged according to local
   policy, as if they were generated locally.


For example, the socket node might decide to log events to a
   local file and also resent them to a second socket node.

    @author  Ceki Gülcü

    @since 0.8.4
 */
public class SocketNode implements Runnable {

	Socket socket;
	LoggerRepository hierarchy;
	ObjectInputStream ois;

	static Logger logger = Logger.getLogger(SocketNode.class);
	MqttClient anMqttClient;
	HippocratesSocketServer aHippocratesSocketServer;
	String ipAddress="";
	String mode;
	int debugsRecordToShow=6;
	
	String lastLogHy="", messageHy="";
	String lastLogHe="", messageHe="";
	String lastLogWeb="" , messageWeb="";
	Timestamp longAgoHy,longAgoHe,longAgoWeb;
	
	public SocketNode(HippocratesSocketServer d, MqttClient m ,Socket socket,String mo,int r, LoggerRepository hierarchy) {
		this.socket = socket;
		ipAddress = socket.getInetAddress().getHostAddress();
		mode=mo;
		debugsRecordToShow=r;
		
		aHippocratesSocketServer=d;
		anMqttClient=m;
		this.hierarchy = hierarchy;
		try {
			ois = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
		}
		catch(Exception e) {
			logger.error("Could not open ObjectInputStream to "+socket, e);
		}
	}

	//public
	//void finalize() {
	//System.err.println("-------------------------Finalize called");
	// System.err.flush();
	//}

	public void run() {
		LoggingEvent event;
		Logger remoteLogger;

		try {
			while(true) {
				// read an event from the wire
				event = (LoggingEvent) ois.readObject();
				// get a logger from the hierarchy. The name of the logger is taken to be the name contained in the event.
				String completePattern = "%d [%p|%c|%C{1}] %m%n";
				PatternLayout completePatternLayout = new PatternLayout(completePattern);
				String completeMessage  = completePatternLayout.format(event);
				aHippocratesSocketServer.generateReport(event);
				if(mode.equals(HippocratesSocketServer.LISTEN_AND_PUBLISH)) {
					if(anMqttClient!=null && anMqttClient.isConnected()) {
						try {

							MqttMessage message = new MqttMessage(completeMessage.getBytes());
							message.setQos(2);
							anMqttClient.getTopic("Diagnostics").publish( message);
						} catch (MqttException e) {
							// TODO Auto-generated catch block
							//logger.debug(Utils.getStringException(e));
							System.out.println(Utils.getStringException(e));
						}
					}else {
						aHippocratesSocketServer.connect();
					}
				}
				
				
				
			}
		} catch(java.io.EOFException e) {
			logger.info("Caught java.io.EOFException closing conneciton.");
		} catch(java.net.SocketException e) {
			logger.info("Caught java.net.SocketException closing conneciton.");
		} catch(IOException e) {
			logger.info("Caught java.io.IOException: "+e);
			logger.info("Closing connection.");
		} catch(Exception e) {
			logger.error("Unexpected exception. Closing conneciton.", e);
		}

		try {
			ois.close();
		} catch(Exception e) {
			logger.info("Could not close connection.", e);
		}
	}
	
	
}