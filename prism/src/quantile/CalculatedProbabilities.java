package quantile;

import java.util.ArrayList;

import jdd.JDD;
import jdd.JDDNode;

public class CalculatedProbabilities {
	// storage for x_s[i]
	private ArrayList<JDDNode> x = new ArrayList<JDDNode>();
	private int windowStart = 0;

	public void clear() {
		for (JDDNode x_s : x) {
			if (x_s != null) JDD.Deref(x_s);
		}
	}

	/**
	 * REFs: x_s
	 */
	public void storeProbabilities(int i, JDDNode x_s)
	{
		if (i < x.size() && x.get(i) != null) {
			JDD.Deref(x.get(i));
		}
		x.add(i,  x_s);
	}

	public JDDNode getProbabilities(int i)
	{
		return x.get(i).copy();
	}

	public boolean hasProbabilities(int i)
	{
		return ( i < x.size() ) && ( x.get(i)!=null );
	}

	public void advanceWindow(int currentIndex, int windowSize) {
		int newWindowStart = currentIndex - windowSize;
		if (newWindowStart < 0) newWindowStart = 0;

		for (int i = windowStart; i < newWindowStart; i++) {
			JDDNode n = x.set(i, null);
			if (n != null) JDD.Deref(n);
		}

		windowStart = newWindowStart;
	}
}
