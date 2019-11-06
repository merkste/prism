package explicit.conditional.checker;

import java.util.HashMap;
import java.util.Map;

import explicit.CTMCModelChecker;
import explicit.DTMCModelChecker;
import explicit.ProbModelChecker;
import explicit.conditional.checker.SimplePathEvent.Finally;
import explicit.conditional.checker.SimplePathEvent.Until;
import jdd.Clearable;
import prism.PrismException;

//FIXME ALG: add comment
public interface CachedMCModelChecker<M extends explicit.DTMC, C extends ProbModelChecker> extends MCModelChecker<M, C>, Clearable
{
	Map<SimplePathEvent<M>, double[]> getCache();

	default double[] lookup(SimplePathEvent<M> path)
	{
		return getCache().get(path);
	}

	default double[] store(SimplePathEvent<M> path, double[] probs)
	{
		getCache().put(path, probs);
		return probs;
	}

	default void clear()
	{
		getCache().clear();
	}

	@Override
	default double[] computeProbs(Finally<M> eventually)
			throws PrismException
	{
		double[] probs = lookup(eventually);
		if (probs == null) {
			probs = store(eventually, MCModelChecker.super.computeProbs(eventually));
		}
		return probs;
	}

	@Override
	default double[] computeProbs(Until<M> until)
			throws PrismException
	{
		double[] probs = lookup(until);
		if (probs == null) {
			probs = store(until, MCModelChecker.super.computeProbs(until));
		}
		return probs;
	}



	public static class CTMC extends MCModelChecker.CTMC implements CachedMCModelChecker<explicit.CTMC, CTMCModelChecker>
	{
		protected Map<SimplePathEvent<explicit.CTMC>, double[]> cache;

		public CTMC(CTMCModelChecker modelChecker)
		{
			super(modelChecker);
			cache = new HashMap<>();
		}

		@Override
		public Map<SimplePathEvent<explicit.CTMC>, double[]> getCache()
		{
			return cache;
		}
	}



	public static class DTMC extends MCModelChecker.DTMC implements CachedMCModelChecker<explicit.DTMC, DTMCModelChecker>
	{
		protected Map<SimplePathEvent<explicit.DTMC>, double[]> cache;

		public DTMC(DTMCModelChecker modelChecker)
		{
			super(modelChecker);
			cache = new HashMap<>();
		}

		@Override
		public Map<SimplePathEvent<explicit.DTMC>, double[]> getCache()
		{
			return cache;
		}
	}
}
