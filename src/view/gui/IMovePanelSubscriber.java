package view.gui;

/**
 * Interface for any object that subscribes to a MovePanel
 * @author Randal
 *
 */
public interface IMovePanelSubscriber {

	void onLeft();
	void onRight();
	void onForward();
	
	void onAIStep(int moves);
}
