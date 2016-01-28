package explicit.quantile.topologicalSorting;

import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import common.iterable.collections.SetFactory;

import explicit.quantile.dataStructure.RewardWrapper;

public class TarjanIterative extends SCCCalculator
{
	private int index = 0;
	/* Stack of nodes */
	private List<Integer> stack = new LinkedList<Integer>();
	/* Computation stack */
	private Stack<ComputationTask> taskStack = new Stack<ComputationTask>();
	/* List of nodes in the graph. Invariant: {@code nodeList.get(i).id == i} */
	private TarjanNode[] nodes = new TarjanNode[model.getNumStates()];
	/* Nodes currently on the stack. */
	private BitSet onStack = new BitSet();

	public TarjanIterative(RewardWrapper model, SetFactory setFactory, Set<Integer> theStates)
	{
		super(model, setFactory, theStates);
		for (int state : states)
			nodes[state] = new TarjanNode(state);
	}

	@Override
	public void calculateSCCs()
	{
		tarjan();
		storage.notifyDone();
	}

	/**
	 * Execute Tarjan's algorithm. Determine maximal strongly connected components
	 * (SCCS) for the graph of the model and stored in {@code sccs}.
	 */
	public void tarjan()
	{
		for (int state : states) {
			if (nodes[state].lowlink == -1) {
				taskStack.push(ComputationTask.newTaskTarjan(state));
				compute();
			}
		}
	}

	private void compute()
	{
		while (!taskStack.isEmpty()) {
			ComputationTask task = taskStack.pop();
			switch (task.task_type) {
			case TARJAN:
				tarjan(task.from);
				break;
			case DO_EDGE:
				doEdge(task.from, task.to);
				break;
			case AFTER_EDGE:
				afterEdge(task.from, task.to);
				break;
			case AFTER_VISIT:
				afterVisit(task.from);
				break;
			}
		}
	}

	private void tarjan(int i)
	{
		firstVisit(i);

		taskStack.push(ComputationTask.newTaskAfterVisit(i));
		for (int successor : model.getZeroRewardSuccessors(i)) {
			if (states.contains(successor))
				taskStack.push(ComputationTask.newTaskDoEdge(i, successor));
		}
	}

	private void firstVisit(int i)
	{
		final TarjanNode v = nodes[i];
		v.index = index;
		v.lowlink = index;
		index++;
		stack.add(0, i);
		onStack.set(i);
	}

	private void doEdge(int from, int to)
	{
		TarjanNode n = nodes[to];
		TarjanNode v = nodes[from];
		if (n.index == -1) {
			taskStack.push(ComputationTask.newTaskAfterEdge(from, to));
			taskStack.push(ComputationTask.newTaskTarjan(to));
		} else if (onStack.get(to))
			v.lowlink = Math.min(v.lowlink, n.index);
	}

	private void afterEdge(int from, int to)
	{
		TarjanNode n = nodes[to];
		TarjanNode v = nodes[from];
		v.lowlink = Math.min(v.lowlink, n.lowlink);
	}

	private void afterVisit(int i)
	{
		TarjanNode v = nodes[i];
		if (v.lowlink == v.index) {
			int n;
			Set<Integer> component = setFactory.getSet();
			do {
				n = stack.remove(0);
				onStack.set(n, false);
				component.add(n);
			} while (n != i);
			storage.notifyNextSCC(component);
		}
	}

	protected static class ComputationTask
	{
		enum TASK_TYPE {
			TARJAN, DO_EDGE, AFTER_EDGE, AFTER_VISIT
		};

		public TASK_TYPE task_type;
		public int from, to;

		public static ComputationTask newTaskTarjan(int i)
		{
			ComputationTask task = new ComputationTask();
			task.task_type = TASK_TYPE.TARJAN;
			task.from = i;
			return task;
		}

		public static ComputationTask newTaskDoEdge(int from, int to)
		{
			ComputationTask task = new ComputationTask();
			task.task_type = TASK_TYPE.DO_EDGE;
			task.from = from;
			task.to = to;
			return task;
		}

		public static ComputationTask newTaskAfterEdge(int from, int to)
		{
			ComputationTask task = new ComputationTask();
			task.task_type = TASK_TYPE.AFTER_EDGE;
			task.from = from;
			task.to = to;
			return task;
		}

		public static ComputationTask newTaskAfterVisit(int i)
		{
			ComputationTask task = new ComputationTask();
			task.task_type = TASK_TYPE.AFTER_VISIT;
			task.from = i;
			return task;
		}
	}
}