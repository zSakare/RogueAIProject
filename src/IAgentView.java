


public interface IAgentView {

	void setAgent(Agent agent);
	
	/**
	 * begin displaying the view 
	 */
	void run(int port);
	
	/**
	 *  To be called when the agent view needs to be refreshed */
	void onUpdate(int x, int y);
	
	void setController(IAgentController controller);
	
	/**
	 * Notify our controllers that an action has been provided
	 */
	void notifyAction(char action);
	
}
