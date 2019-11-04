package explicit.conditional.checker;

import java.util.BitSet;
import java.util.Objects;

import explicit.PredecessorRelation;
import explicit.conditional.checker.SimplePathProperty.Finally;
import explicit.conditional.checker.SimplePathProperty.Globally;
import explicit.conditional.checker.SimplePathProperty.Next;
import explicit.conditional.checker.SimplePathProperty.Reach;
import explicit.conditional.checker.SimplePathProperty.Until;
import prism.PrismException;

public class MDPModelChecker extends SimplePathEventModelChecker.Basic<explicit.MDP, explicit.MDPModelChecker>
{
	public MDPModelChecker(explicit.MDPModelChecker modelChecker)
	{
		super(modelChecker);
	}

	public BitSet computeProb0A(explicit.MDP model, boolean negated, BitSet remain, BitSet goal)
			throws PrismException
	{
		if (negated) {
			return computeProb1A(model, false, remain, goal);
		}
		Objects.requireNonNull(goal);
		PredecessorRelation pre = model.getPredecessorRelation(this, true);
		return getModelChecker(model).prob0(model, remain, goal, false, null, pre);
	}

	public BitSet computeProb0E(explicit.MDP model, boolean negated, BitSet remain, BitSet goal)
			throws PrismException
	{
		if (negated) {
			return computeProb1E(model, false, remain, goal);
		}
		Objects.requireNonNull(goal);
		PredecessorRelation pre = model.getPredecessorRelation(this, true);
		return getModelChecker(model).prob0(model, remain, goal, true, null, pre);
	}

	public BitSet computeProb1A(explicit.MDP model, boolean negated, BitSet remain, BitSet goal)
			throws PrismException
	{
		if (negated) {
			return computeProb0A(model, false, remain, goal);
		}
		Objects.requireNonNull(goal);
		PredecessorRelation pre = model.getPredecessorRelation(this, true);
		return getModelChecker(model).prob1(model, remain, goal, true, null, pre);
	}

	public BitSet computeProb1E(explicit.MDP model, boolean negated, BitSet remain, BitSet goal)
			throws PrismException
	{
		if (negated) {
			return computeProb0E(model, false, remain, goal);
		}
		Objects.requireNonNull(goal);
		PredecessorRelation pre = model.getPredecessorRelation(this, true);
		return getModelChecker(model).prob1(model, remain, goal, false, null, pre);
	}

	public double[] computeNextMaxProbs(explicit.MDP model, boolean negated, BitSet goal)
			throws PrismException
	{
		if (negated) {
			// Pmax(¬φ) = 1 - Pmin(φ);
			double[] probabilities = computeNextMinProbs(model, false, goal);
			return subtractFromOne(probabilities);
		}
		Objects.requireNonNull(goal);
		return getModelChecker(model).computeNextProbs(model, goal, false).soln;
	}

	public double[] computeNextMinProbs(explicit.MDP model, boolean negated, BitSet goal)
			throws PrismException
	{
		if (negated) {
			// Pmin(¬φ) = 1 - Pmax(φ);
			double[] probabilities = computeNextMaxProbs(model, false, goal);
			return subtractFromOne(probabilities);
		}
		Objects.requireNonNull(goal);
		return getModelChecker(model).computeNextProbs(model, goal, true).soln;
	}

	public double[] computeUntilMaxProbs(explicit.MDP model, boolean negated, BitSet remain, BitSet goal)
			throws PrismException
	{
		if (negated) {
			// Pmax(¬φ) = 1 - Pmin(φ);
			double[] probabilities = computeUntilMinProbs(model, false, remain, goal);
			return subtractFromOne(probabilities);
		}
		Objects.requireNonNull(goal);
		return getModelChecker(model).computeUntilProbs(model, remain, goal, false).soln;
	}

	public double[] computeUntilMinProbs(explicit.MDP model, boolean negated, BitSet remain, BitSet goal)
			throws PrismException
	{
		if (negated) {
			// Pmin(¬φ) = 1 - Pmax(φ);
			double[] probabilities = computeUntilMaxProbs(model, false, remain, goal);
			return subtractFromOne(probabilities);
		}
		Objects.requireNonNull(goal);
		return getModelChecker(model).computeUntilProbs(model, remain, goal, true).soln;
	}

	public BitSet computeProb0E(Reach<explicit.MDP> reach)
			throws PrismException
	{
		if (reach instanceof Finally) {
			return computeProb0E((Finally<explicit.MDP>) reach);
		}
		if (reach instanceof Globally) {
			return computeProb0E(((Globally<explicit.MDP>)reach).asFinally());
		}
		return computeProb0E(reach.asUntil());
	}

	public BitSet computeProb0E(Finally<explicit.MDP> eventually)
			throws PrismException
	{
		return computeProb0E(eventually.getModel(), eventually.isNegated(), ALL_STATES, eventually.getGoal());
	}

	public BitSet computeProb0E(Until<explicit.MDP> until)
			throws PrismException
	{
		return computeProb0E(until.getModel(), until.isNegated(), until.getRemain(), until.getGoal());
	}

	public BitSet computeProb0A(Reach<explicit.MDP> reach)
			throws PrismException
	{
		if (reach instanceof Finally) {
			return computeProb0A((Finally<explicit.MDP>) reach);
		}
		if (reach instanceof Globally) {
			return computeProb0A(((Globally<explicit.MDP>)reach).asFinally());
		}
		return computeProb0A(reach.asUntil());
	}

	public BitSet computeProb0A(Finally<explicit.MDP> eventually)
			throws PrismException
	{
		return computeProb0A(eventually.getModel(), eventually.isNegated(), ALL_STATES, eventually.getGoal());
	}

	public BitSet computeProb0A(Until<explicit.MDP> until)
			throws PrismException
	{
		return computeProb0A(until.getModel(), until.isNegated(), until.getRemain(), until.getGoal());
	}

	public BitSet computeProb1E(Reach<explicit.MDP> reach)
			throws PrismException
	{
		if (reach instanceof Finally) {
			return computeProb1E((Finally<explicit.MDP>) reach);
		}
		if (reach instanceof Globally) {
			return computeProb1E(((Globally<explicit.MDP>)reach).asFinally());
		}
		return computeProb1E(reach.asUntil());
	}

	public BitSet computeProb1E(Finally<explicit.MDP> eventually)
			throws PrismException
	{
		return computeProb1E(eventually.getModel(), eventually.isNegated(), ALL_STATES, eventually.getGoal());
	}

	public BitSet computeProb1E(Until<explicit.MDP> until)
			throws PrismException
	{
		return computeProb1E(until.getModel(), until.isNegated(), until.getRemain(), until.getGoal());
	}

	public BitSet computeProb1A(Reach<explicit.MDP> reach)
			throws PrismException
	{
		if (reach instanceof Finally) {
			return computeProb1A((Finally<explicit.MDP>) reach);
		}
		if (reach instanceof Globally) {
			return computeProb1A(((Globally<explicit.MDP>)reach).asFinally());
		}
		return computeProb1A(reach.asUntil());
	}

	public BitSet computeProb1A(Finally<explicit.MDP> eventually)
			throws PrismException
	{
		return computeProb1A(eventually.getModel(), eventually.isNegated(), ALL_STATES, eventually.getGoal());
	}

	public BitSet computeProb1A(Until<explicit.MDP> until)
			throws PrismException
	{
		return computeProb1A(until.getModel(), until.isNegated(), until.getRemain(), until.getGoal());
	}

	public double[] computeMaxProbs(SimplePathProperty<explicit.MDP> path)
			throws PrismException
	{
		if (path instanceof Finally) {
			return computeMaxProbs((Finally<explicit.MDP>) path);
		}
		if (path instanceof Globally) {
			return computeMaxProbs(((Globally<explicit.MDP>)path).asFinally());
		}
		if (path instanceof Next) {
			return computeMaxProbs((Next<explicit.MDP>) path);
		}
		if (path instanceof Reach) {
			return computeMaxProbs(((Reach<explicit.MDP>)path).asUntil());
		}
		throw new PrismException("Unsupported simple path property " + path);
	}

	public double[] computeMaxProbs(Finally<explicit.MDP> eventually)
			throws PrismException
	{
		return computeUntilMaxProbs(eventually.getModel(), eventually.isNegated(), ALL_STATES, eventually.getGoal());
	}

	public double[] computeMaxProbs(Next<explicit.MDP> next)
			throws PrismException
	{
		return computeNextMaxProbs(next.getModel(), next.isNegated(), next.getGoal());
	}

	public double[] computeMaxProbs(Until<explicit.MDP> until)
			throws PrismException
	{
		return computeUntilMaxProbs(until.getModel(), until.isNegated(), until.getRemain(), until.getGoal());
	}

	public double[] computeMinProbs(SimplePathProperty<explicit.MDP> path)
			throws PrismException
	{
		if (path instanceof Finally) {
			return computeMinProbs((Finally<explicit.MDP>) path);
		}
		if (path instanceof Globally) {
			return computeMinProbs(((Globally<explicit.MDP>)path).asFinally());
		}
		if (path instanceof Next) {
			return computeMinProbs((Next<explicit.MDP>) path);
		}
		if (path instanceof Reach) {
			return computeMinProbs(((Reach<explicit.MDP>)path).asUntil());
		}
		throw new PrismException("Unsupported simple path property " + path);		}

	public double[] computeMinProbs(Finally<explicit.MDP> eventually)
			throws PrismException
	{
		return computeUntilMinProbs(eventually.getModel(), eventually.isNegated(), ALL_STATES, eventually.getGoal());
	}

	public double[] computeMinProbs(Next<explicit.MDP> next)
			throws PrismException
	{
		return computeNextMinProbs(next.getModel(), next.isNegated(), next.getGoal());
	}

	public double[] computeMinProbs(Until<explicit.MDP> until)
			throws PrismException
	{
		return computeUntilMinProbs(until.getModel(), until.isNegated(), until.getRemain(), until.getGoal());
	}
}
