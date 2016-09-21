package automata;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

import jltl2ba.APElement;
import jltl2ba.MyBitSet;
import jltl2ba.SimpleLTL;
import jltl2ba.TLProperties;
import jltl2dstar.NBA;
import parser.ast.Expression;
import prism.PrismComponent;
import prism.PrismDevNullLog;
import prism.PrismException;
import acceptance.AcceptanceBuchi;
import acceptance.AcceptanceOmega;
import acceptance.AcceptanceReach;

import common.iterable.IterableBitSet;
import common.Wrapper;

import explicit.LTS;
import explicit.LTSExplicit;
import explicit.NonProbModelChecker;
import explicit.SCCComputer;
import explicit.SCCConsumer;

public class LTL2WDBA extends PrismComponent
{
	public LTL2WDBA(PrismComponent parent)
	{
		super(parent);
	}

	public DA<BitSet, AcceptanceReach> cosafeltl2wdba(SimpleLTL ltl) throws PrismException
	{
			DA<BitSet, AcceptanceBuchi> da = ltl2wdba(ltl);

			BitSet F = da.getAcceptance().getAcceptingStates();
			BitSet notF = (BitSet) F.clone();
			notF.flip(0, da.size());

			LTS lts = new LTSFromDA(da);
			BitSet canAvoidF = new NonProbModelChecker(this).computeExistsGlobally(lts, notF);
			BitSet canNotAvoidF = (BitSet)canAvoidF.clone();
			canNotAvoidF.flip(0, da.size());
			
			DA<BitSet, AcceptanceReach> dfa = toDFA(da, canNotAvoidF);
			//dfa.printDot(System.out);
			return dfa;
	}

	@SuppressWarnings("unchecked")
	private DA<BitSet, AcceptanceReach> toDFA(DA<BitSet, ? extends AcceptanceOmega> da, BitSet goalStates)
	{
		AcceptanceReach reach = new AcceptanceReach(goalStates);
		DA.switchAcceptance(da, reach);
		return (DA<BitSet, AcceptanceReach>) da;
	}

	private DA<BitSet, AcceptanceBuchi> ltl2wdba(SimpleLTL ltl) throws PrismException
	{
		ltl = ltl.simplify();

		NBA nba = ltl.toNBA();
		PowersetDA P = powersetConstruction(nba);
		determineF(P);

		return P.da;
	}

	public DA<BitSet, AcceptanceBuchi> obligation2wdba(SimpleLTL ltl) throws PrismException
	{
		return ltl2wdba(ltl);
	}

	private static class PowersetDA
	{
		public NBA nba;

		public DA<BitSet, AcceptanceBuchi> da;
		public MyBitSet powersetOneF;
		public MyBitSet powersetAllF;

		public ArrayList<BitSet> idToState;
		public BitSet F = new BitSet();
	}

	private PowersetDA powersetConstruction(NBA nba) throws PrismException
	{
		//System.out.println("--- NBA ---");
		//nba.print_hoa(System.out);
		DA<BitSet, AcceptanceBuchi> da = new DA<BitSet, AcceptanceBuchi>();
		da.setAcceptance(new AcceptanceBuchi());

		HashMap<BitSet,Integer> stateToId = new HashMap<BitSet,Integer>();
		ArrayList<BitSet> idToState = new ArrayList<BitSet>();

		MyBitSet nbaF = nba.getFinalStates();
		MyBitSet powersetOneF = new MyBitSet();
		MyBitSet powersetAllF = new MyBitSet();

		Queue<Integer> todo = new LinkedList<Integer>();
		BitSet initialState = new BitSet();
		initialState.set(nba.getStartState().getName());
		int initialId = da.addState();
		stateToId.put(initialState, initialId);
		idToState.add(initialState);
		todo.add(initialId);
		da.setStartState(initialId);
		da.setAPList(new ArrayList<String>(nba.getAPSet().asList()));

		if (initialState.intersects(nbaF)) {
			powersetOneF.set(initialId);
		}
		if (nbaF.containsAll(initialState)) {
			powersetAllF.set(initialId);
		}

		//System.out.println("new: "+initialId+" "+initialState);

		BitSet visited = new BitSet();
		while (!todo.isEmpty()) {
			int curId = todo.poll();
			if (visited.get(curId)) continue;

			BitSet cur = idToState.get(curId);
			//System.out.println("Expand "+curId+" "+cur);

			for (APElement e : nba.getAPSet().elements()) {
				BitSet to = new BitSet();
				for (int f : IterableBitSet.getSetBits(cur)) {
					to.or(nba.get(f).getEdge(e));
				}

				Integer toId = stateToId.get(to);
				if (toId == null) {
					toId = da.addState();
					stateToId.put(to, toId);
					idToState.add(to);
					todo.add(toId);

					//System.out.println("new: "+toId+" "+to);
					
					
					if (to.intersects(nbaF)) {
						powersetOneF.set(toId);
					}
					if (nbaF.containsAll(to)) {
						powersetAllF.set(toId);
					}
				}

				//System.out.println(" delta(" + curId + ", " + e + ") = " + toId);
				da.addEdge(curId, e, toId);
			}
		}

		//da.printHOA(System.out);

		PowersetDA result = new PowersetDA();
		result.nba = nba;
		result.da = da;
		result.idToState = idToState;
		result.powersetOneF = powersetOneF;
		result.powersetAllF = powersetAllF;
		//System.out.println("powersetOneF = "+powersetOneF);
		//System.out.println("powersetAllF = "+powersetAllF);

		return result;
	}

	private void determineF(final PowersetDA P) throws PrismException
	{
		LTS daLTS = new LTSFromDA(P.da);
		SCCComputer sccComputer = SCCComputer.createSCCComputer(this, daLTS,
			new SCCConsumer(this, daLTS) {

			@Override
			public void notifyNextSCC(BitSet scc) throws PrismException {
				if (hasAcceptingCycle(P, scc)) {
					// mark all SCC states as final in powerset automaton
					P.F.or(scc);
				}
			}
		});
		sccComputer.computeSCCs();

		// construct acceptance
		P.da.getAcceptance().setAcceptingStates(P.F);
	}

	private boolean hasAcceptingCycle(PowersetDA p, BitSet scc) throws PrismException
	{
		//System.out.println("hasAcceptingCycle "+scc+"?");
		 if (!scc.intersects(p.powersetOneF)) {
			// none of the NBA states in this powerset SCC are final
			//System.out.println(" -> no (none final)");
			return false;
		}
		if (p.powersetAllF.containsAll(scc)) {
			// all NBA states in this powerset SCC are final
			//System.out.println(" -> yes (all final)");
			return true;
		}

		// first, 
		Cycle cycle = findCycle(p, scc);
		//System.out.println(cycle);
		final BuchiLTS buchilts = buildLTSforCycle(p, cycle);
		// String name = "lts"+scc+".dot";
		// buchilts.lts.exportToDotFile(name, p.F);
		//System.out.println("DOT: "+name);

		final Wrapper<Boolean> result = new Wrapper<Boolean>(false);

		SCCComputer sccComputer = SCCComputer.createSCCComputer(this, buchilts.lts, new SCCConsumer(this, buchilts.lts) {

			@Override
			public void notifyNextSCC(BitSet scc) throws PrismException
			{
				if (scc.intersects(buchilts.F)) {
					result.set(true);
				}
			}
		});
		sccComputer.computeSCCs();

		//System.out.println(result.get() ? " -> yes" : " -> no");
		return result.get();
	}

	private static class Cycle {
		LinkedList<APElement> word;
		int cycleStart;
		
		public String toString()
		{
			String s = "Cycle starting at "+cycleStart+" with ";
			s+= word.toString();
			return s;
		}
	}

	private Cycle findCycle(PowersetDA p, BitSet scc) throws PrismException
	{
		// pick a start state
		int R = scc.nextSetBit(0);

		Stack<Integer> states = new Stack<Integer>();
		Stack<BitSet> letters = new Stack<BitSet>();
		BitSet onStack = new BitSet();

		states.push(R);
		onStack.set(R);

		final DA<BitSet, AcceptanceBuchi> da = p.da;

		while (true) {
			int cur = states.peek();
			// find first letter that allows remaining in SCC
			int n = da.getNumEdges(cur);
			boolean found = false;
			int to = -1;
			for (int i=0; i<n; i++) {
				to = p.da.getEdgeDest(cur, i);
				if (scc.get(to)) {
					BitSet letter = da.getEdgeLabel(cur, i);
					letters.add(letter);
					states.add(to);
					found = true;
					break;
				}
			}
			if (!found) {
				throw new PrismException("Implementation error in findCycle");
			}

			if (!onStack.get(to)) {
				// not yet a cycle
				onStack.set(to);
				continue;
			}

			// found a cycle
			int cycleStart = to;
			Cycle cycle = new Cycle();
			cycle.word = new LinkedList<APElement>();
			cycle.cycleStart = cycleStart;

			do {
				APElement label = new APElement();
				label.or(letters.pop());
				cycle.word.addFirst(label);
				states.pop();
			} while (states.peek() != to);

			return cycle;
		}
	}

	private static class CycleLTSState
	{
		int nbaState;
		int cyclePos;

		CycleLTSState(int nbaState, int cyclePos)
		{
			this.nbaState = nbaState;
			this.cyclePos = cyclePos;
		}
		
		public String toString()
		{
			return "("+nbaState+","+cyclePos+")";
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + cyclePos;
			result = prime * result + nbaState;
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			CycleLTSState other = (CycleLTSState) obj;
			if (cyclePos != other.cyclePos)
				return false;
			if (nbaState != other.nbaState)
				return false;
			return true;
		}
	}
	
	private static class BuchiLTS
	{
		LTS lts;
		BitSet F;
	}

	private BuchiLTS buildLTSforCycle(PowersetDA p, Cycle cycle)
	{
		HashMap<CycleLTSState, Integer> stateToIndex = new HashMap<CycleLTSState, Integer>();
		ArrayList<CycleLTSState> indexToState = new ArrayList<CycleLTSState>();

		Stack<Integer> todo = new Stack<Integer>();
		BitSet expanded = new BitSet();
		BitSet F = new BitSet();

		LTSExplicit lts = new LTSExplicit();

		for (int startNBA : IterableBitSet.getSetBits(p.idToState.get(cycle.cycleStart))) {
			int i = lts.addState();
			todo.push(i);
			CycleLTSState s = new CycleLTSState(startNBA, 0);
			stateToIndex.put(s, i);
			indexToState.add(s);
			if (p.nba.get(startNBA).isFinal()) {
				F.set(i);
			}

			//System.out.println("new: " + i + " = "+ s);
		}

		while (!todo.isEmpty()) {
			int curProd = todo.pop();
			if (expanded.get(curProd)) continue;

			CycleLTSState cur = indexToState.get(curProd);
			// expand
			//System.out.println("Expand "+curProd+" = "+cur);
			APElement letter = cycle.word.get(cur.cyclePos);
			MyBitSet toSet = p.nba.get(cur.nbaState).getEdge(letter);
			int cyclePos = (cur.cyclePos+1) % cycle.word.size();

			for (int to : toSet) {
				CycleLTSState toProd = new CycleLTSState(to, cyclePos);
				Integer prodTo = stateToIndex.get(toProd);
				if (prodTo == null) {
					prodTo = lts.addState();
					todo.push(prodTo);
					stateToIndex.put(toProd, prodTo);
					indexToState.add(toProd);

					if (p.nba.get(to).isFinal()) {
						F.set(prodTo);
					}
					
					//System.out.println("new: " + prodTo + " = " +toProd);
				}

				lts.addEdge(curProd, prodTo);
				//System.out.println(" " + curProd +" -> " +prodTo);
			}
		}

		BuchiLTS result = new BuchiLTS();
		result.lts = lts;
		result.F = F;
		return result;
	}
	
	
	/**
	 * Simple test method: convert LTL formula (in LBT format) to HOA/Dot/txt
	 */
	public static void main(String args[])
	{
		try {
			// Usage:
			// * ... 'X p1' 
			// * ... 'X p1' da.hoa 
			// * ... 'X p1' da.hoa hoa 
			// * ... 'X p1' da.dot dot 
			// * ... 'X p1' - hoa 
			// * ... 'X p1' - txt 

			// Convert to Expression (from PRISM format)
			/*String pltl = "P=?[" + ltl + "]";
			PropertiesFile pf = Prism.getPrismParser().parsePropertiesFile(new ModulesFile(), new ByteArrayInputStream(pltl.getBytes()));
			Prism.releasePrismParser();
			Expression expr = pf.getProperty(0);
			expr = ((ExpressionProb) expr).getExpression();
			System.out.println("LTL: " + expr);*/

			// Convert to Expression (from LBT format)
			String ltl = args[0];
			SimpleLTL sltl = SimpleLTL.parseFormulaLBT(args[0]);
			Expression expr = Expression.createFromJltl2ba(sltl);
			// System.out.println("LBT: " + ltl);
			// System.out.println("LTL: " + expr);

			// Build/export DA
			PrismComponent parent = new PrismComponent();
			parent.setLog(new PrismDevNullLog());
			LTL2WDBA ltl2wdba = new LTL2WDBA(parent);
			TLProperties tl = TLProperties.analyse(sltl);
			
			DA<BitSet, ? extends AcceptanceOmega> da;
			if (tl.isSyntacticGuarantee()) {
				da = ltl2wdba.cosafeltl2wdba(sltl);
			} else if (tl.isSyntacticObligation()) {
				da = ltl2wdba.obligation2wdba(sltl);
			} else {
				throw new Exception("Can not construct an automaton for " + sltl +", not co-safe or obligation");
			}
			PrintStream out = (args.length < 2 || "-".equals(args[1])) ? System.out : new PrintStream(args[1]);
			String format = (args.length < 3) ? "hoa" : args[2];
			da.print(out, format);

		} catch (Exception e) {
			e.printStackTrace();
			System.err.print("Error: " + e);
		}
	}
}
