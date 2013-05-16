package controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import logic.Agent;
import view.IAgentView;

public class AgentConsoleController implements IAgentController {

	private IAgentView view;
	private Agent agent;
	
	private InputStream in = null;
	private OutputStream out = null;
	private Socket socket = null;
	
	public AgentConsoleController(Agent agent, IAgentView view) {
		this.agent = agent;
		this.view = view;
		view.setController(this);
	}
	
	@Override
	public void setView(IAgentView view) {
		this.view = view;
		view.setController(this);
	}

	@Override
	public void setAgent(Agent agent) {
		this.agent = agent;
	}
	
	public void initialiseNetwork(int port) {
		try { // open socket to Game Engine
			socket = new Socket("localhost", port);
			in = socket.getInputStream();
			out = socket.getOutputStream();
		} catch (IOException e) {
			System.out.println("Could not bind to port: " + port + ": " + e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public void shutdown() {
		/*
		try {
			socket.close();
		} catch (IOException e) {
			System.err.println("Error closing socket");
		}*/
	}
	/**
	 * Receive a viewport from the engine.
	 * Let the agent process it.
	 * @return viewport, or null if error
	 */
	public char[][] waitForViewport() {
		int i, j, ch;
		char view[][] = new char[5][5];
		
		try { // scan 5-by-5 window around current location
			for (i = 0; i < 5; i++) {
				for (j = 0; j < 5; j++) {
					if (!((i == 2) && (j == 2))) {
						ch = in.read();
						if (ch == -1) {
							System.exit(-1);
						}
						view[i][j] = (char) ch;
					}
				}
			}

			agent.parse_view(view);
			
			agent.updateViews();
			
		} catch (IOException e) {
			System.out.println("Lost connection, could not ETC ETC");
			view = null;
		}
		
		return view;
	}
	
	@Override
	public void onAction(char action) {
		agent.handle_action(action);
		
		try {
			out.write(action);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
