package com.teleonome.hippocrates;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.teleonome.framework.utils.Utils;
 

public class Test{

	int lastReadNumberOfLines = 0;
	String lineSep = System.lineSeparator();
	MqttClient anMqttClient ;
	String mqttBrokerAddress = "tcp://10.0.0.210:1883";
    String mqttClientId = "Diagnostics";
    MemoryPersistence persistence = new MemoryPersistence();
    String mqttStatusTopic="Status";
    int mqttQualityOfService=2;
    File selectedFile = new File("/Users/arifainchtein/Data/casete/software/wokspace/Log4JSocketListener/logfile.log");
	
	public Test(){
		
		 try {
         	anMqttClient = new MqttClient(mqttBrokerAddress, mqttClientId, persistence);
         	
             MqttConnectOptions connOpts = new MqttConnectOptions();
             connOpts.setCleanSession(true);
             //logger.warn("Connecting to Heart: "+mqttBrokerAddress);
				anMqttClient.connect(connOpts);
				
				//logger.warn("Connected to Heart: "+mqttBrokerAddress);
		             
			} catch (MqttException e1) {
				// TODO Auto-generated catch block
				//logger.warn(Utils.getStringException(e1));
				System.out.println(Utils.getStringException(e1));
			}
		 
		//File selectedFile = new File(System.getProperty("user.dir") + System.getProperty("file.separator") + "logfile.log");
//		String s;
//		String serverCommand = "java -classpath lib/log4j-1.2.8.jar org.apache.log4j.net.SimpleSocketServer 4712 log4j-server.properties";
//		try {
//			String localDirName = System.getProperty("user.dir");
//			File processLog = new File(System.getProperty("user.dir") + System.getProperty("file.separator") + "process.log");
//			Process pkillProcessId = Runtime.getRuntime().exec(new String[]{"sh","checkserverstatus.sh"});
//			try {
//				pkillProcessId.waitFor();
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			BufferedReader stdError = new BufferedReader(new InputStreamReader(pkillProcessId.getErrorStream()));
//			// read any errors from the attempted command
//			System.out.println("Here is the standard error of pkillProcessId:\n");
//			while ((s = stdError.readLine()) != null) {
//				System.out.println(s);
//			}
//
//			if(processLog.isFile()){
//				List processLogLines = FileUtils.readLines(new File("./process.log"));
//				if(processLogLines.size()>0){
//					done:
//						for(Object s2:processLogLines){
//							if(s2.toString().contains("org.apache.log4j.net.SimpleSocketServer 4712")){
//								System.out.println("s2=" + s2.toString());
//								String[] tokens = s2.toString().trim().split(" ");
//								String pidToKill = tokens[1].trim();
//								System.out.println("killing " + pidToKill);
//								Process pkillProcess = Runtime.getRuntime().exec("kill " + pidToKill);
//								BufferedReader stdInput = new BufferedReader(new InputStreamReader(pkillProcess.getInputStream()));
//
//								stdError = new BufferedReader(new InputStreamReader(pkillProcess.getErrorStream()));
//								System.out.println("Here is the standard output of the kill command:\n");
//
//								while ((s = stdInput.readLine()) != null) {
//									System.out.println(s);
//								}
//
//								// read any errors from the attempted command
//								System.out.println("Here is the standard error of the  killcommand (if any):\n");
//								while ((s = stdError.readLine()) != null) {
//									System.out.println(s);
//								}
//								break done;
//							}
//						}
//				}
//			}
//
//			Process p = Runtime.getRuntime().exec(serverCommand);
//			
//			System.out.println("Starting watch thread");
//


//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		String pulseProgressString;
//		try {
//			pulseProgressString = FileUtils.readFileToString(selectedFile);
//			
//			System.out.println(pulseProgressString);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

		FileWatcherThread f2 = new FileWatcherThread();
		f2.start();
		System.out.println("Started watch thread");
	}

	
	class FileWatcherThread extends Thread{
		//  AsyncContext anAsyncContext;
		public FileWatcherThread(){
			// anAsyncContext=c;

		}

		public void run(){
			//final Path path = FileSystems.getDefault().getPath(System.getProperty("user.dir"));
			final Path path = FileSystems.getDefault().getPath("/Users/arifainchtein/Data/casete/software/wokspace/Log4JSocketListener/");
				
			 
			System.out.println(path);
			try (final WatchService watchService = FileSystems.getDefault().newWatchService()) {
				final WatchKey watchKey = path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
				boolean keepRunning=true;
				while (keepRunning) {
					final WatchKey wk = watchService.take();
					for (WatchEvent<?> event : wk.pollEvents()) {
						//we only register "ENTRY_MODIFY" so the context is always a Path.
						final Path changed = (Path) event.context();
						File selectedFile = changed.toFile();

						String extension = FilenameUtils.getExtension(selectedFile.getAbsolutePath());
						String fileName = FilenameUtils.getName(selectedFile.getAbsolutePath());
						System.out.println("file  changed:" + changed + " extension:" + extension);
						if (changed.toString().equals("logfile.log")) {
							//
							// wait 100ms,
							Thread.sleep(50);
							String lastLine = readLine();
							System.out.println("Last Line=" + lastLine);
							 try {
								 MqttMessage message = new MqttMessage(lastLine.getBytes());
								    message.setQos(mqttQualityOfService);
									anMqttClient.getTopic("Diagnostics").publish( message);
								} catch (MqttException e) {
									// TODO Auto-generated catch block
									//logger.debug(Utils.getStringException(e));
									System.out.println(Utils.getStringException(e));
								}
							 
							 wk.reset();
							
						}
					}
				}
			}catch(Exception e){
				System.out.println(Utils.getStringException(e));
			}
		}
	}

	public String readLine(){
		//
		// read the last line of a file
		
		String command="";
		String lastLine="";
		ReversedLinesFileReader anReversedLinesFileReader=null;
		try {
			//
			// read the last line of the file
			//
			
			anReversedLinesFileReader = new ReversedLinesFileReader(selectedFile);
			lastLine = anReversedLinesFileReader.readLine();
		
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			if(anReversedLinesFileReader!=null) {
				try {
					anReversedLinesFileReader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		return lastLine;

	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new Test();
	}

}
