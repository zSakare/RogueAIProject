package controller;

import logic.*;
import view.*;

public interface IAgentController {

	void setAgent(Agent agent);
	void setView(IAgentView view);
	
	void initialiseNetwork(int port);
	void shutdown();
	char[][] waitForViewport();
	
	void onAction(char action);
}
