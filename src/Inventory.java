

import java.util.HashMap;

public class Inventory {
	// yay encapsulation
	private HashMap<Character, Integer> inventory;
	
	public Inventory() {
		inventory = new HashMap<Character, Integer>();
	}
	
	public Inventory(Inventory src) {
		inventory = new HashMap<Character, Integer>(src.inventory);
	}
	
	public int get(char c) {
		Integer amt = inventory.get(c);
		return (amt != null) ? amt : 0;
	}
	
	public void add(char c) {
		Integer amt = inventory.get(c);
		if (amt == null) {
			inventory.put(c, 1);
		} else {
			inventory.put(c, amt.intValue() + 1);
		}
	}
	
	public void use(char c) {
		Integer amt = inventory.get(c);
		if (amt != null) {
			inventory.put(c, amt.intValue() - 1);
		}
	}
	
	@Override
	public int hashCode() {
		return inventory.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o.getClass().equals(this.getClass())) {
			Inventory i = (Inventory)o;
			return inventory.equals(i.inventory);
		}
		return false;
	}
	
	@Override
	public String toString() {
		String str = "[";
		for (Character c : inventory.keySet()) {
			str += c + ":" + get(c) + " ";
		}
		str += "]";
		return str;
	}
}
