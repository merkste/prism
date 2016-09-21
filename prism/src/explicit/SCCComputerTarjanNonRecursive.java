//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Christian von Essen <christian.vonessen@imag.fr> (Verimag, Grenoble)
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

package explicit;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import prism.PrismComponent;
import prism.PrismException;

/**
 * Tarjan's SCC algorithm operating on a Model object.
 * This is a non-recursive variant of SCCComputerTarjan, using an explicit stack object.
 */
public class SCCComputerTarjanNonRecursive extends SCCComputer
{
	/* The model to compute (B)SCCs for */
	private Model model;
	/* Number of nodes (model states) */
	private int numNodes;

	/* Next index to give to a node */
	private int index = 0;
	/* Stack of nodes */
	private List<Integer> stack = new LinkedList<Integer>();
	/* Computation stack */
	private Stack<ComputationTask> taskStack = new Stack<ComputationTask>();
	/* List of nodes in the graph. Invariant: {@code nodeList.get(i).id == i} */
	private ArrayList<Node> nodeList;
	/* Nodes currently on the stack. */
	private BitSet onStack;
	/** Should we filter trivial SCCs? */
	private boolean filterTrivialSCCs;


	/**
	 * Build (B)SCC computer for a given model.
	 */
	public SCCComputerTarjanNonRecursive(PrismComponent parent, Model model, SCCConsumer consumer) throws PrismException
	{
		super(parent, consumer);
		this.model = model;
		this.numNodes = model.getNumStates();
		this.nodeList = new ArrayList<Node>(numNodes);
		for (int i = 0; i < numNodes; i++) {
			nodeList.add(new Node(i));
		}
		onStack = new BitSet();
	}

	// Methods for SCCComputer interface

	@Override
	public void computeSCCs(boolean filterTrivialSCCs) throws PrismException
	{
		this.filterTrivialSCCs = filterTrivialSCCs;
		tarjan();
		consumer.notifyDone();
	}

	
	// SCC Computation

	/**
	 * Execute Tarjan's algorithm. Determine maximal strongly connected components
	 * (SCCS) for the graph of the model and stored in {@code sccs}.
	 * @throws PrismException 
	 */
	public void tarjan() throws PrismException
	{
		for (int i = 0; i < numNodes; i++) {
			if (nodeList.get(i).lowlink == -1) {
				taskStack.push(ComputationTask.newTaskTarjan(i));
				compute();
			}
		}

	}

	
	private void compute() throws PrismException {
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
		Iterator<Integer> it = model.getSuccessorsIterator(i);
		while (it.hasNext()) {
			int e = it.next();
			taskStack.push(ComputationTask.newTaskDoEdge(i, e));
		}
	}
	
	private void firstVisit(int i) {
		final Node v = nodeList.get(i);
		v.index = index;
		v.lowlink = index;
		index++;
		stack.add(0, i);
		onStack.set(i);
	}
	
	private void doEdge(int from, int to) {
		Node n = nodeList.get(to);
		Node v = nodeList.get(from);
		if (n.index == -1) {
			taskStack.push(ComputationTask.newTaskAfterEdge(from, to));
			taskStack.push(ComputationTask.newTaskTarjan(to));
		} else if (onStack.get(to)) {
			v.lowlink = Math.min(v.lowlink, n.index);
		}
	}

	private void afterEdge(int from, int to) {
		Node n = nodeList.get(to);
		Node v = nodeList.get(from);
		v.lowlink = Math.min(v.lowlink, n.lowlink);		
	}
	
	private void afterVisit(int i) throws PrismException {
		Node v = nodeList.get(i);
		if (v.lowlink == v.index) {
			int n;
			BitSet component = new BitSet();
			do {
				n = stack.remove(0);
				component.set(n);
			} while (n != i);
			// we have removed exactly the nodes in component from the stack
			// do corresponding set-minus for the onStack structure:
			onStack.andNot(component);

			// found an SCC, should we report?
			if (!filterTrivialSCCs) {
				// we don't filter, so we report
				consumer.notifyNextSCC(component);
			} else {
				if (!isTrivialSCC(model, component)) {
					// only report if non-trivial
					consumer.notifyNextSCC(component);
				}
			}
		}
		
	}
	
	/**
	 * A small class wrapping a node.
	 * It carries extra information necessary for Tarjan's algorithm.
	 */
	protected static class Node
	{
		public int lowlink = -1;
		public int index = -1;
		public int id;

		public Node(int id)
		{
			this.id = id;
		}
	}
	
	
	protected static class ComputationTask
	{
		enum TASK_TYPE {TARJAN, DO_EDGE, AFTER_EDGE, AFTER_VISIT};
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
