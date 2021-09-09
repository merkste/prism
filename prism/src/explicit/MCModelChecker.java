package explicit;

import java.util.BitSet;

import prism.PrismException;

public interface MCModelChecker<M extends DTMC>
{
	/**
	 * Create a new DTMC model checker with the same settings as this one.
	 */
	DTMCModelChecker createDTMCModelChecker() throws PrismException;

	/**
	 * Prob0 precomputation algorithm (using predecessor relation),
	 * i.e. determine the states of a Markov chain which, with probability 0,
	 * reach a state in {@code target}, while remaining in those in {@code remain}.
	 * @param dtmc The DTMC
	 * @param remain Remain in these states (optional: {@code null} means "all states")
	 * @param target Target states
	 * @param pre The predecessor relation
	 */
	BitSet prob0(M model, BitSet remain, BitSet target, PredecessorRelation pre) throws PrismException;

	/**
	 * Prob1 precomputation algorithm (using predecessor relation),
	 * i.e. determine the states of a Markov chain which, with probability 1,
	 * reach a state in {@code target}, while remaining in those in {@code remain}.
	 * @param dtmc The DTMC
	 * @param remain Remain in these states (optional: null means "all")
	 * @param target Target states
	 * @param pre The predecessor relation of the DTMC
	 */
	BitSet prob1(M model, BitSet remain, BitSet target, PredecessorRelation pre) throws PrismException;
}
