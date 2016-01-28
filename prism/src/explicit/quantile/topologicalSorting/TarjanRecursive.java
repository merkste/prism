package explicit.quantile.topologicalSorting;

import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import common.iterable.collections.SetFactory;

import explicit.quantile.dataStructure.RewardWrapper;

public class TarjanRecursive extends SCCCalculator
{
	private TarjanNode[] nodes;
	private int index;
	private BitSet onStack;
	private List<Integer> stack;

	public TarjanRecursive(RewardWrapper model, SetFactory setFactory, Set<Integer> theStates)
	{
		super(model, setFactory, theStates);
		//XXX: ich muss aufpassen, wenn ich nur ein Array fuer Zunion aufbaue, denn (siehe weiter unten)
		//nodes = new Node[Zunion.cardinality()];
		nodes = new TarjanNode[model.getNumStates()];
		for (int state : states)
			nodes[state] = new TarjanNode(state);
		index = 0;
		stack = new LinkedList<Integer>();
		onStack = new BitSet();
	}

	public void calculateSCCs()
	{
		for (int state : states)
			if (nodes[state].lowlink == -1)
				tarjan(state);
		storage.notifyDone();
	}

	private void tarjan(int state)
	{
		final TarjanNode v = nodes[state];
		v.index = index;
		v.lowlink = index;
		index++;
		stack.add(0, state);
		onStack.set(state);
		for (int successor : model.getZeroRewardSuccessors(state)) {
			if (states.contains(successor)) {
				//XXX: hier muss eine Umrechnung stattfinden
				//XXX: ich brauche nicht den Zugriff auf das successor-vielte Element, sondern auf das Element an dessen Position successor steht
				TarjanNode n = nodes[successor];
				if (n.index == -1) {
					tarjan(successor);
					v.lowlink = Math.min(v.lowlink, n.lowlink);
				} else {
					if (onStack.get(successor))
						v.lowlink = Math.min(v.lowlink, n.index);
				}
			}
		}
		if (v.lowlink == v.index) {
			Set<Integer> component = setFactory.getSet();
			int n;
			do {
				n = stack.remove(0);
				onStack.clear(n);
				component.add(n);
			} while (n != state);
			storage.notifyNextSCC(component);
		}
	}
}