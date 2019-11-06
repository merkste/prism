package prism.conditional.checker;

import java.util.HashMap;
import java.util.Map;

import jdd.Clearable;
import jdd.JDD;
import jdd.JDDNode;
import prism.PrismException;
import prism.ProbModel;
import prism.ProbModelChecker;
import prism.StochModel;
import prism.StochModelChecker;
import prism.conditional.checker.SimplePathEvent.Finally;
import prism.conditional.checker.SimplePathEvent.Until;

//FIXME ALG: add comment
public interface CachedMCModelChecker<M extends ProbModel, C extends ProbModelChecker> extends MCModelChecker<M,C>, Clearable
{
	Map<SimplePathEvent<M>, JDDNode> getCache();

	/**
	 * [ REFS: <i>result</i>, DEREFS: none ]
	 */
	default JDDNode lookup(SimplePathEvent<M> path)
	{
		return getCache().get(path).copy();
	}

	/**
	 * [ REFS: <i>path, probs</i>, DEREFS: none ]
	 */
	default JDDNode store(SimplePathEvent<M> path, JDDNode probs)
	{
		getCache().put(path.clone(), probs.copy());
		return probs;
	}

	/**
	 * [ REFS: none, DEREFS: all pairs <i>(path, probs)</i> ]
	 */
	@Override
	default void clear()
	{
		getCache().keySet().forEach(SimplePathEvent::clear);
		getCache().values().forEach(JDD::Deref);
		getCache().clear();
	}

	@Override
	default JDDNode computeProbs(Finally<M> eventually)
			throws PrismException
	{
		JDDNode probs = lookup(eventually);
		if (probs == null) {
			probs = MCModelChecker.super.computeProbs(eventually);
			store(eventually, probs);
		}
		return probs;
	}

	@Override
	default JDDNode computeProbs(Until<M> until)
			throws PrismException
	{
		JDDNode probs = lookup(until);
		if (probs == null) {
			probs = MCModelChecker.super.computeProbs(until);
			store(until, probs);
		}
		return probs;
	}



	public static class CTMC extends MCModelChecker.CTMC implements CachedMCModelChecker<StochModel, StochModelChecker>
	{
		protected Map<SimplePathEvent<StochModel>, JDDNode> cache;

		public CTMC(StochModelChecker modelChecker)
		{
			super(modelChecker);
			cache = new HashMap<>();
		}

		@Override
		public Map<SimplePathEvent<StochModel>, JDDNode> getCache()
		{
			return cache;
		}
	}

	public static class DTMC extends MCModelChecker.DTMC implements CachedMCModelChecker<ProbModel, ProbModelChecker>
	{
		protected Map<SimplePathEvent<ProbModel>, JDDNode> cache;

		public DTMC(ProbModelChecker modelChecker)
		{
			super(modelChecker);
			cache = new HashMap<>();
		}

		@Override
		public Map<SimplePathEvent<ProbModel>, JDDNode> getCache()
		{
			return cache;
		}
	}
}
