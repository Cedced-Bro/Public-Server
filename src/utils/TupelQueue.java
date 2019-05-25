package utils;

import java.util.Queue;

/**
 * This is a special designed Queue which differs from the already in Java implemented {@link Queue}.</br>
 * As the name tells this Queue is just for {@link Tupel}.
 * 
 * @see
 * {@link utils.TupelQueue.Node}</br>
 * {@link utils.Tupel}</br>
 * {@link utils.Vector2D}</br>
 * {@link utils.Vector3D}
 * @author Cedric
 * @version 1.0
 *
 */
public class TupelQueue {
	/**
	 * This is the first {@link Node} of the Queue.
	 */
	private Node first;

	/**
	 * This helps creating an identifier for a special Node.</br>
	 * For example if {@link Node#identifier} changes the data can still be found as it is signed by a unique identifier.
	 */
	private static long lastIdentifier;
	
	static {
		lastIdentifier = Long.MIN_VALUE;
	}
	
	/**
	 * Creates a new {@link TupelQueue}.
	 */
	public TupelQueue() {}
	
	/**
	 * Adds a {@link Tupel} {@link TupelQueue}.
	 * 
	 * @param t
	 */
	public void add(Tupel t) {
		if (first == null) first = new Node(t, lastIdentifier++);
		else first.add(new Node(t, lastIdentifier++));
	}
	
	public int add(Tupel t, int index) {
		if (first == null) {
			first = new Node(t, lastIdentifier++);
			return index;
		}
		else return first.add(new Node(t, lastIdentifier++), index);
	}
	
	public Tupel remove(Tupel t) {
		if (first == null) return null;
		else return first.remove(t);
	}
	
	public Tupel remove(int index) {
		if (first == null) return null;
		if (index == 0)
			return removeFirst();
		else return first.remove(index);
	}
	
	public Tupel removeFirst() {
		if (first == null) return null;
		else {
			Tupel data = first.getData();
			first = first.getNextNode();
			return data;
		}
	}

	public int size() {
		if (first == null) return 0;
		else return first.size();
	}
	
	public void removeAll() {
		first = null;
	}

	public Tupel get(int index) {
		if (first == null) return null;
		if (index == 0) return first.getData();
		else return first.get(index);
	}
	
	public Tupel getByIdentifier(int identifier) {
		if (first == null) return null;
		else return first.getByIdentifier(identifier);
	}
	
	public int getIndex(Tupel data) {
		if (first != null) return first.getIndex(data);
		else return -1;
	}
	
	public int getIndex(int identifier) {
		if (first != null) return first.getIndex(identifier);
		else return -1;
	}
	
	public int getIdentifier(Tupel data) {
		if (first != null) return first.getIdentifier(data);
		else return -1;
	}
	
	public int getIdentifier(int index) {
		if (first != null) {
			Tupel data = first.get(index);
			if (data != null)
				return getIdentifier(data);
		}
		return 0;
	}
	
	public Tupel changeData(Tupel newData, int index) {
		if (first == null) return null;
		else if (index == 0) {
			Tupel returnData = first.getData();
			first.data = newData;
			return returnData;
		} else
			return first.changeData(newData, index);
	}
	
	/**
	 * 
	 * @author Cedric
	 *
	 */
	private class Node {
		private Node next;
		private Tupel data;
		private long identifier;
		
		Node(Tupel data, long identifier) {
			this.data = data;
			this.identifier = identifier;
			this.next = null;
		}

		Node getNextNode() {
			return next;
		}
		
		public void add(Node node) {
			if (isEnd()) next = node;
			else next.add(node);
		}

		public int add(Node node, int index) {
			if (isEnd() || index == 0) {
				next = node;
				return index;
			}
			else return next.add(node, index-1);
		}
		
		public Tupel changeData(Tupel newData, int index) {
			if (isEnd() && index != 0) return null;
			else if (index == 0) {
				Tupel returnData = data;
				data = newData;
				return returnData;
			} else return next.changeData(newData, index--);
		}
		
		public Tupel remove(Tupel data) {
			if (!isEnd())
				if (next.getData().equals(data)) {
					Tupel rData = next.getData();
					next = next.getNextNode();
					return rData;
				} else
					return next.remove(data);
			return null;
		}

		public Tupel remove(int index) {
			if (!isEnd())
				if (index == 1) {
					Tupel rData = next.getData();
					next = next.getNextNode();
					return rData;
				} else
					return next.remove(index-1);
			return null;
		}
		
		public Tupel get(int index) {
			if (index == 0) return data;
			else if (!isEnd()) return next.get(index-1);
			else return null;
		}
		
		public Tupel getByIdentifier(int identifier) {
			if (this.identifier == identifier) return this.data;
			else if (!isEnd()) return next.getByIdentifier(identifier);
			else return null;
		}
		
		public int getIndex(Tupel data) {
			if (this.data.equals(data))
				return 0;
			else if (isEnd()) return -1;
			else {
				int i = next.getIndex(data);
				if (i == -1) return i;
				else return i++;
			}
		}
		
		public int getIndex(int identifier) {
			if (this.identifier == identifier)
				return 0;
			else if (isEnd()) return -1;
			else {
				int i = next.getIndex(identifier);
				if (i == -1) return i;
				else return i++;
			}
		}
		
		public int getIdentifier(Tupel data) {
			if (this.data == data)
				return 0;
			else if (isEnd()) return -1;
			else
				return next.getIdentifier(data);
		}
		
		public Tupel getData() {
			return data;
		}

		public int size() {
			if (isEnd()) return 1;
			else return next.size()+1;
		}
			
		public boolean isEnd() {
			return (next == null);
		}
	}
}
