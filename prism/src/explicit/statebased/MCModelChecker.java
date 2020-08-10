package explicit.statebased;

import java.util.BitSet;
import java.util.Objects;
import java.util.PrimitiveIterator.OfInt;

import common.BitSetTools;
import common.IterableBitSet;
import explicit.CTMCModelChecker;
import explicit.DTMCModelChecker;
import explicit.PredecessorRelation;
import explicit.ProbModelChecker;
import explicit.statebased.SimplePathEvent.Finally;
import explicit.statebased.SimplePathEvent.Globally;
import explicit.statebased.SimplePathEvent.Next;
import explicit.statebased.SimplePathEvent.Reach;
import explicit.statebased.SimplePathEvent.Until;
import prism.PrismException;

//FIXME ALG: add comment
public interface MCModelChecker<M extends explicit.DTMC, C extends ProbModelChecker> extends SimplePathEventModelChecker<M,C>
{
	BitSet computeProb0(M model, boolean negated, BitSet remain, BitSet goal)
			throws PrismException;

	BitSet computeProb1(M model, boolean negated, BitSet remain, BitSet goal)
			throws PrismException;

	double[] computeNextProbs(M model, boolean negated, BitSet goal)
			throws PrismException;

	double[] computeUntilProbs(M model, boolean negated, BitSet remain, BitSet goal)
			throws PrismException;

	// FIXME ALG: Here we actually reuse prob0/1 precomputation
	double[] computeUntilProbs(M model, boolean negated, BitSet remain, BitSet goal, BitSet prob0, BitSet prob1)
			throws PrismException;

	default BitSet computeProb0(Reach<M> reach)
			throws PrismException
	{
		if (reach instanceof Finally) {
			return computeProb0((Finally<M>) reach);
		}
		if (reach instanceof Globally) {
			return computeProb0(((Globally<M>)reach).asFinally());
		}
		return computeProb0(reach.asUntil());
	}

	default BitSet computeProb0(Finally<M> eventually)
			throws PrismException
	{
		return computeProb0(eventually.getModel(), eventually.isNegated(), ALL_STATES, eventually.getGoal());
	}

	default BitSet computeProb0(Until<M> until)
			throws PrismException
	{
		return computeProb0(until.getModel(), until.isNegated(),until.getRemain(), until.getGoal());
	}

	default BitSet computeProb1(Reach<M> reach)
			throws PrismException
	{
		if (reach instanceof Finally) {
			return computeProb1((Finally<M>) reach);
		}
		if (reach instanceof Globally) {
			return computeProb1(((Globally<M>)reach).asFinally());
		}
		return computeProb1(reach.asUntil());
	}

	default BitSet computeProb1(Finally<M> eventually)
			throws PrismException
	{
		return computeProb1(eventually.getModel(), eventually.isNegated(), ALL_STATES, eventually.getGoal());
	}

	default BitSet computeProb1(Until<M> until)
			throws PrismException
	{
		return computeProb1(until.getModel(), until.isNegated(), until.getRemain(), until.getGoal());
	}

	default double[] computeProbs(SimplePathEvent<M> path)
			throws PrismException
	{
		if (path instanceof Finally) {
			return computeProbs((Finally<M>) path);
		}
		if (path instanceof Globally) {
			return computeProbs(((Globally<M>)path).asFinally());
		}
		if (path instanceof Next) {
			return computeProbs((Next<M>)path);
		}
		if (path instanceof Reach) {
			return computeProbs(((Reach<M>)path).asUntil());
		}
		throw new PrismException("Unsupported simple path property " + path);
	}

	default double[] computeProbs(Finally<M> eventually)
			throws PrismException
	{
		return computeUntilProbs(eventually.getModel(), eventually.isNegated(), ALL_STATES, eventually.getGoal());
	}

	default double[] computeProbs(Next<M> next)
			throws PrismException
	{
		return computeNextProbs(next.getModel(), next.isNegated(), next.getGoal());
	}

	default double[] computeProbs(Until<M> until)
			throws PrismException
	{
		return computeUntilProbs(until.getModel(), until.isNegated(), until.getRemain(), until.getGoal());
	}



	public static class DTMC extends SimplePathEventModelChecker.Basic<explicit.DTMC, DTMCModelChecker> implements MCModelChecker<explicit.DTMC, DTMCModelChecker>
	{
		public DTMC(DTMCModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public BitSet computeProb0(explicit.DTMC model, boolean negated, BitSet remain, BitSet goal)
				throws PrismException
		{
			if (negated) {
				return computeProb1(model, false, remain, goal);
			}
			Objects.requireNonNull(goal);
			PredecessorRelation pre = model.getPredecessorRelation(this, true);
			return getModelChecker(model).prob0(model, remain, goal, pre);
		}

		@Override
		public BitSet computeProb1(explicit.DTMC model, boolean negated, BitSet remain, BitSet goal)
				throws PrismException
		{
			if (negated) {
				return computeProb0(model, false, remain, goal);
			}
			Objects.requireNonNull(goal);
			PredecessorRelation pre = model.getPredecessorRelation(this, true);
			return getModelChecker(model).prob1(model, remain, goal, pre);
		}

		@Override
		public double[] computeNextProbs(explicit.DTMC model, boolean negated, BitSet goal)
				throws PrismException
		{
			Objects.requireNonNull(goal);
			double[] probabilities = getModelChecker(model).computeNextProbs(model, goal).soln;
			if (negated) {
				return subtractFromOne(probabilities);
			}
			return probabilities;
		}

		@Override
		public double[] computeUntilProbs(explicit.DTMC model, boolean negated, BitSet remain, BitSet goal)
				throws PrismException
		{
			// FIXME ALG: consider precomputation
			Objects.requireNonNull(goal);
			double[] probabilities = getModelChecker(model).computeUntilProbs(model, remain, goal).soln;
			if (negated) {
				return subtractFromOne(probabilities);
			}
			return probabilities;
		}

		@Override
		public double[] computeUntilProbs(explicit.DTMC model, boolean negated, BitSet remain, BitSet goal, BitSet prob0, BitSet prob1)
				throws PrismException
		{
			double[] init = new double[model.getNumStates()]; // initialized with 0.0's
			BitSet setToOne = negated ? prob0 : prob1;
			for (OfInt iter = new IterableBitSet(setToOne).iterator(); iter.hasNext();) {
				init[iter.nextInt()] = 1.0;
			}
			BitSet known = BitSetTools.union(prob0, prob1);
			double[] probabilities = getModelChecker(model).computeReachProbs(model, remain, goal, init, known).soln;
			return negated ? subtractFromOne(probabilities) : probabilities;
		}
	}



	public static class CTMC extends SimplePathEventModelChecker.Basic<explicit.CTMC, CTMCModelChecker> implements MCModelChecker<explicit.CTMC, CTMCModelChecker>
	{
		public CTMC(CTMCModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public BitSet computeProb0(explicit.CTMC model, boolean negated, BitSet remain, BitSet goal)
				throws PrismException
		{
			if (negated) {
				return computeProb1(model, false, remain, goal);
			}
			Objects.requireNonNull(goal);
			PredecessorRelation pre = model.getPredecessorRelation(this, true);
			return getModelChecker(model).prob0(model, remain, goal, pre);
		}

		@Override
		public BitSet computeProb1(explicit.CTMC model, boolean negated, BitSet remain, BitSet goal)
				throws PrismException
		{
			if (negated) {
				return computeProb0(model, false, remain, goal);
			}
			Objects.requireNonNull(goal);
			PredecessorRelation pre = model.getPredecessorRelation(this, true);
			return getModelChecker(model).prob1(model, remain, goal, pre);
		}

		@Override
		public double[] computeNextProbs(explicit.CTMC model, boolean negated, BitSet goal)
				throws PrismException
		{
			Objects.requireNonNull(goal);
			double[] probabilities = getModelChecker(model).computeNextProbs(model, goal).soln;
			if (negated) {
				return subtractFromOne(probabilities);
			}
			return probabilities;
		}

		@Override
		public double[] computeUntilProbs(explicit.CTMC model, boolean negated, BitSet remain, BitSet goal)
				throws PrismException
		{
			// FIXME ALG: consider precomputation
			Objects.requireNonNull(goal);
			double[] probabilities = getModelChecker(model).computeUntilProbs(model, remain, goal).soln;
			if (negated) {
				return subtractFromOne(probabilities);
			}
			return probabilities;
		}

		@Override
		public double[] computeUntilProbs(explicit.CTMC model, boolean negated, BitSet remain, BitSet goal, BitSet prob0, BitSet prob1)
				throws PrismException
		{
			double[] init = new double[model.getNumStates()]; // initialized with 0.0's
			BitSet setToOne = negated ? prob0 : prob1;
			for (OfInt iter = new IterableBitSet(setToOne).iterator(); iter.hasNext();) {
				init[iter.nextInt()] = 1.0;
			}
			BitSet known = BitSetTools.union(prob0, prob1);
			double[] probabilities = getModelChecker(model).computeReachProbs(model, remain, goal, init, known).soln;
			return negated ? subtractFromOne(probabilities) : probabilities;
		}
	}
}
