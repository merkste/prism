package explicit.conditional.checker;

import java.util.HashMap;
import java.util.Map;

import explicit.CTMCModelChecker;
import explicit.DTMCModelChecker;
import explicit.ProbModelChecker;
import explicit.conditional.checker.SimplePathProperty.Finally;
import explicit.conditional.checker.SimplePathProperty.Until;
import jdd.Clearable;
import prism.PrismException;

public interface CachedMCModelChecker<M extends explicit.DTMC, C extends ProbModelChecker> extends MCModelChecker<M, C>, Clearable
{
	double[] lookup(SimplePathProperty<M> path);

	double[] store(SimplePathProperty<M> path, double[] probs);

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



	public static class DTMC extends MCModelChecker.DTMC implements CachedMCModelChecker<explicit.DTMC, DTMCModelChecker>
	{
		protected Map<SimplePathProperty<explicit.DTMC>, double[]> cache;

		public DTMC(DTMCModelChecker modelChecker)
		{
			super(modelChecker);
			cache = new HashMap<>();
		}

		@Override
		public double[] lookup(SimplePathProperty<explicit.DTMC> path)
		{
			return cache.get(path);
		}

		@Override
		public double[] store(SimplePathProperty<explicit.DTMC> path, double[] probs)
		{
			cache.put(path, probs);
			return probs;
		}

		@Override
		public void clear()
		{
			cache.clear();
		}
	}



	public static class CTMC extends MCModelChecker.CTMC implements CachedMCModelChecker<explicit.CTMC, CTMCModelChecker>
	{
		protected Map<SimplePathProperty<explicit.CTMC>, double[]> cache;

		public CTMC(CTMCModelChecker modelChecker)
		{
			super(modelChecker);
			cache = new HashMap<>();
		}

		@Override
		public double[] lookup(SimplePathProperty<explicit.CTMC> path)
		{
			return cache.get(path);
		}

		@Override
		public double[] store(SimplePathProperty<explicit.CTMC> path, double[] probs)
		{
			cache.put(path, probs);
			return probs;
		}

		@Override
		public void clear()
		{
			cache.clear();
		}
	}
}
