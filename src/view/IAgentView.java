package view;

import logic.Agent;

public interface IAgentView {

	void setAgent(Agent agent);
	
	/* To be called when the agent view needs to be refreshed */
	void onUpdate(int x, int y);
	
	
}
