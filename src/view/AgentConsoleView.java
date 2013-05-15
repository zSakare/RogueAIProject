package view;

import logic.Agent;

public class AgentConsoleView implements IAgentView {

	private Agent agent;
	
	public AgentConsoleView(Agent agent) {
		this.agent = agent;
		
		agent.addView(this);
	}
	
	@Override
	public void onUpdate(int posx, int posy) {
		print_view();
		System.out.println(getExploredArea());
	}

	@Override
	public void setAgent(Agent agent) {
		this.agent = agent;
		agent.addView(this);
	}
	
	void print_view(boolean show_arrow) {
		int i, j;
		char view[][] = agent.getLastView();
		int size;
		int half_size;
		
		if (view != null) {
			size = view.length;
			half_size = size / 2;
			System.out.println("\n+-----+");
			for (i = 0; i < size; i++) {
				System.out.print("|");
				for (j = 0; j < size; j++) {
					if (show_arrow && (i == half_size) && (j == half_size)) {
						System.out.print('^');
					} else {
						System.out.print(view[i][j]);
					}
				}
				System.out.println("|");
			}
			System.out.println("+-----+");
		}
	}
	
	void print_view() {
		print_view(false);
	}
	
	/**
	 * Returns a representation of the map currently explored by the agent.
	 * 
	 * @return String representing map
	 */
	public String getExploredArea() {
		int minx, maxx, miny, maxy, posx, posy;
		int initx, inity;
		int direction;
		
		minx = agent.getMinX();
		maxx = agent.getMaxX();
		miny = agent.getMinY();
		maxy = agent.getMaxY();
		posx = agent.getX();
		posy = agent.getY();
		initx = agent.getInitX();
		inity = agent.getInitY();
		direction = agent.getDirection();
		
		String res = "-- Agent Map: --\n";
		res += " - This is what the AI can see... -\n";
		// draw top border
		for (int x = minx; x < maxx + 2; ++x) {
			res += '%';
		}
		res += '\n';
		for (int y = miny; y < maxy; ++y) {
			res += '%';
			for (int x = minx; x < maxx; ++x) {
				if (x == posx && y == posy) {
					res += getCharacterForDirection(direction);
				} else if (x == initx && y == inity) {
					res += 'S';
				} else {
					res += String.valueOf(agent.charAt(x,y));
				}
			}
			res += "%\n";
		}
		// draw bottom border
		for (int x = minx; x < maxx + 2; ++x) {
			res += '%';
		}
		return res;
	}

	

	public static final char [] arrows = {'>','^','<','v'};
	
	public static char getCharacterForDirection(int dirn) {
		return arrows[dirn];
	}

}
