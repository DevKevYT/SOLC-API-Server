import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import com.devkev.devscript.raw.Output;
import com.devkev.devscript.raw.Process;
import com.devkev.main.Main;
import com.sn1pe2win.config.dataflow.Variable;

public class Server {
	
	private final int LISTEN_PORT;
	private ServerSocket listenSocket = null;
	
	private final int MAX_CONNECTIONS;
	private final long MAX_CONNECTION_TIME;
	private final long MAX_FILE_DURATION; //Seconds
	
	private ArrayList<Connection> activeClients = new ArrayList<>();
	private Thread gateway;
	private final Commands lib;
	
	/**@param maxConnections - Maximal Anzahl an Clients die sich verbinden dürfen.
	 * @param maxConnectionTime - Maximale Zeit, die ein Client verbunden sein darf. In Millisekunden*/
	public Server(int port, int maxConnections, int maxConnectionTime, long maxFileDuration) {
		
		if(maxConnections <= 0) maxConnections = -1;
		if(maxConnectionTime <= 0) maxConnectionTime = -1;
		
		System.out.println("Picked up options: MaxClientConnections=" + maxConnections + ", MaxConnectionTime=" + maxConnectionTime / 1000 + " seconds\n---");
		
		this.MAX_CONNECTIONS = maxConnections;
		this.MAX_CONNECTION_TIME = maxConnectionTime;
		this.LISTEN_PORT = port;
		this.MAX_FILE_DURATION = maxFileDuration;
		
		lib = new Commands();
	}
	
	private void addClientConnection(Socket client) throws IOException {
		
		final Connection c = new Connection(client);
		
		c.addThread(new Thread(new Runnable() {
			@Override
			public void run() {				

				try {
					Process commandHandler = new Process(true);
					
					String command = c.read();
					
					//Einfach eine Zeile!
					if(command != null) {
						commandHandler.clearLibraries();
						commandHandler.includeLibrary(lib);
						commandHandler.setVariable("connection", c, true, true);
						commandHandler.addOutput(new Output() {
							@Override
							public void warning(String arg0) {
							}
							@Override
							public void log(String arg0, boolean arg1) {
							}
							
							@Override
							public void error(String arg0) {
								Main.logger.logError("Error while executing command: " + (arg0.length() > 100 ? arg0.substring(0, 100) : arg0) + " (Exit Code: " + c.status + ")");
								//Send error message
								try {
									c.writer.write("{\"error\": \"" + arg0 + "\",\"code\": " + c.status + "}");
									c.writer.flush();
								} catch (IOException e) {}
							}
						});
						commandHandler.execute(command, false);
					}
				} catch (IOException e) {
					c.status = Codes.CODE_UNKNOWN_ERROR;
				}
				
				closeConnection(c);
				
				if(c.status >= 100) { /**Codes >= 100 sind Fehlercodes: @see Codes interface*/
					Main.logger.logError("Closed session " + c.sessionId + " (Code: " + c.status + ")");
					Main.logger.log(activeClients.size() + " active connections.");
				}
				
			}
		}));
		
		activeClients.add(c);
		c.activate();
	}
	
	private void closeConnection(Connection c) {
		try {
			c.terminateConnection();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		activeClients.remove(c);
		if(activeClients.size() < MAX_CONNECTIONS) {
			//Ein Platz ist wieder frei geworden!
			synchronized (gateway) {
				gateway.notify();
			}
		}
	}
	
	public void shutdown() {
		Main.logger.log("Terminating active connections ...", true);
		for(int i = 0; i < activeClients.size(); i++) {
			closeConnection(activeClients.get(i));
			i = 0;
		}
		Main.logger.log("Closing server ...", true);
		if(listenSocket != null) {
			try {
				listenSocket.close();
			} catch (IOException e) {}
			listenSocket = null;
		}
	}
	
	private void createGateway() {
		gateway = new Thread(new Runnable() {
			@Override
			public void run() {
				Main.logger.log("Gateway running");
				try {
					while(true) {
						
						Socket connectionSocket = listenSocket.accept();
						
						try {
							addClientConnection(connectionSocket);
						} catch(Exception e) {
							Main.logger.logError("Failed to establish an open connection to client.");
						}
						
						if(MAX_CONNECTIONS > 0) {
							//Friere den Gateway ein, wenn zu viele Verbindungen gleichzeitig aufgebaut wurden
							if(activeClients.size() >= MAX_CONNECTIONS) {
								try {
									Main.logger.logError("Exceeded max connections! " + MAX_CONNECTIONS + " Waiting for more space ...");
									synchronized (gateway) {
										gateway.wait();
									}
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						}
					}
			} catch(BindException e1) {
				Main.logger.logError("Port " + LISTEN_PORT + " is already occupied by another program or instance. Please change the port in the configuration or free this port on your machine", true);
				shutdown();
			} catch(Exception e) {
				Main.logger.logError("Server stopped due to an exception: " + e.toString() + ". Restarting ...", true);
				shutdown();
				createGateway();
			} finally {
				if(listenSocket != null) {
					try {
						listenSocket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		}, "Gateway");
		
		gateway.start();
	}
	
	private void createTimeoutObserver() {
		//Timeout thing also responsible for sheet deletion
		Thread observer = new Thread(new Runnable() {
			@Override
			public void run() {
				Main.logger.log("Observer running");
				try {
					while(true) {
						
						long currentTime = System.currentTimeMillis();
						
						for(int i = 0; i < activeClients.size(); i++) {
							if(currentTime - activeClients.get(i).startTime > MAX_CONNECTION_TIME) {
								//Wirf den client raus
								Main.logger.logError("Client exceeded max connection time! Kicking SessionID: " + activeClients.get(i).sessionId);
								activeClients.get(i).status = 1;
								
								//Sende eine Nachricht bevor die Verbindung geschlossen wird
								try {
									activeClients.get(i).writer.write("{\"message\": \"Connection timeout (" + (MAX_CONNECTION_TIME / 1000) + " sec)\"}");
									activeClients.get(i).writer.flush();
								} catch (IOException e) {
									Main.logger.logError("Failed to send abort message ...");
								}
					    		
								closeConnection(activeClients.get(i));
								i--;
							}
						}
							for(Variable sheets : Main.config.getConfig().getCreateNode("fileroute").getVariables()) {
								if(sheets.isNode()) {
									if(!sheets.getAsNode().get("created").isUnknown()) {
										try {
											if(Long.valueOf(sheets.getAsNode().get("created").getAsString()) + (MAX_FILE_DURATION*1000) < currentTime) {
												if(new File(sheets.getAsNode().get("file").getAsString()).delete()) {
													//new File(sheets.getAsNode().get("file").getAsString()).delete();
													new File(sheets.getAsNode().get("file").getAsString()).getParentFile().delete();
													sheets.delete();
													Main.logger.log("Phase file and entry for class: " + sheets.getAsNode().getName() 
															+ " deleted: Max file duration exceeded: " + MAX_FILE_DURATION + " seconds.");
													Main.config.getConfig().save(true);
													break;
												}
											}
										} catch(Exception e) {
											Main.logger.logError("Phase file for class " + sheets.getAsNode().getName() + " corrupted. Deleting.");
											if(!sheets.getAsNode().get("file").isUnknown()) 
												new File(sheets.getAsNode().get("file").getAsString()).getParentFile().delete();
												//new File(sheets.getAsNode().get("file").getAsString()).delete();
											sheets.delete();
											Main.config.getConfig().save(true);
											break;
										}
									} else {
										if(!sheets.getAsNode().get("file").isUnknown()) 
											new File(sheets.getAsNode().get("file").getAsString()).getParentFile().delete();
											//new File(sheets.getAsNode().get("file").getAsString()).delete();
										sheets.delete();
										Main.config.getConfig().save(true);
										Main.logger.log("Phase file and entry deleted: Corrupted.");
										break;
									}
								}
							}
						
						long executionTime = System.currentTimeMillis() - currentTime;
						if(1000 - executionTime > 0) {
							synchronized (this) {
								try {
									wait(1000);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						}
					}
				} catch(Exception e) {
					Main.logger.logError("Dispatcher stopped due to an exception: " + e.toString() + ". Restarting ...", true);
					e.printStackTrace();
					createTimeoutObserver();
				}
			}
		}, "dispatcher");
		observer.setDaemon(true);
		observer.start();
	}
	
	public void listen() throws IOException {
		if(listenSocket != null) return;
		
		try {
			listenSocket = new ServerSocket(LISTEN_PORT);
		} catch(BindException e) {
			Main.logger.logError("Port " + LISTEN_PORT + " is already occupied by another program or instance. Please change the port in the configuration or free this port on your machine", true);
			shutdown();
			return;
		}
		
		Main.logger.log("Server started.", true);
		System.out.println("Listening on " + LISTEN_PORT);
		
		createGateway();
		
		if(MAX_CONNECTIONS > 0) {
			createTimeoutObserver();
		}
	}
}
