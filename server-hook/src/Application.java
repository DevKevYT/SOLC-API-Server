import java.io.IOException;

import com.devkev.devscript.raw.Block;
import com.devkev.devscript.raw.Command;
import com.devkev.devscript.raw.Library;
import com.devkev.devscript.raw.Process;
import com.devkev.main.Hook;
import com.devkev.main.Main;
import com.devkev.main.Version;
import com.sn1pe2win.config.dataflow.Node;
import com.sn1pe2win.config.dataflow.Variable;

public class Application extends Hook {
	
	Server server;
	public static final Version VERSION = Version.of("2.2.0");
	
	public Application() {
		super(VERSION); 
	}

	@Override
	public void init(Node args) {
		int mc = -1;
		int t = -1;
		
		Variable maxConnections = args.get("max-clients");
		if(maxConnections == Variable.UNKNOWN) {
			Main.config.setMaxClientsConfig(10);
		} else {
			mc = maxConnections.getAsInt();
		}
		
		Variable timeout = args.get("client-timeout");
		if(timeout == Variable.UNKNOWN) {
			Main.config.setClientTimeoutConfig(10000);
		} else {
			t = timeout.getAsInt();
		}
		
		int port = 6969;
		Variable vport = args.get("port");
		if(vport != Variable.UNKNOWN) {
			port = vport.getAsInt();
		}
		
		long maxd = 15780000;
		Variable vmaxd = args.get("phase-deletion-after");
		if(!vmaxd.isUnknown()) {
			maxd = vmaxd.getAsLong();
		}
		
		server = new Server(port, mc, t, maxd);
	}
	
	@Override
	public Library adminCommands() {
		return new Library("admin commands") {
			
			@Override
			public Command[] createLib() {
				return new Command[] {
					
					new Command("auto-update", "string ...", "(COMING SOON ...) <enable <[daily,weekly,monthly]> <hour-of-day[0-24]> | disable>") {
						@Override
						public Object execute(Object[] arg0, Process arg1, Block arg2) throws Exception {
							return null;
						}
					},
					
					new Command("list-phase", "", "Listet alle Phasierungen auf") {
						@Override
						public Object execute(Object[] arg0, Process arg1, Block arg2) throws Exception {
							Main.config.modifyConfig(node -> {
								Node phases = node.getCreateNode("fileroute");
								for(Variable entry : phases.getVariables()) {
									if(entry.isNode()) {
										System.out.println("ID: " + entry.getAsNode().getName() + "\nPath: " + entry.getAsNode().get("file").getAsString() + "\n");
									}
								}
							});
							return null;
						}
					},
				};
			}

			@Override
			public void scriptExit(Process arg0, int arg1, String arg2) {
			}

			@Override
			public void scriptImport(Process arg0) {
			}
		};
	}

	@Override
	public void listen() {
		try {
			server.listen();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void shutdown() {
		server.shutdown();
	}

}
