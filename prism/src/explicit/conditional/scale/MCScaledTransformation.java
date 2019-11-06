package explicit.conditional.scale;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.IntFunction;

import common.functions.Predicate;
import common.iterable.FunctionalIterator;
import explicit.BasicModelTransformation;
import explicit.CTMC;
import explicit.CTMCSimple;
import explicit.DTMC;
import explicit.DTMCSimple;
import explicit.ModelTransformation;
import explicit.modelviews.CTMCAlteredDistributions;
import explicit.modelviews.DTMCAlteredDistributions;
import prism.PrismUtils;

//FIXME ALG: add comment
public class MCScaledTransformation
{
	public static BasicModelTransformation<CTMC,CTMCAlteredDistributions> transform(CTMC model, double[] originProbs)
	{
		return transform(model, originProbs, originProbs);
	}

	public static BasicModelTransformation<DTMC,DTMCAlteredDistributions> transform(DTMC model, double[] originProbs)
	{
		return transform(model, originProbs, originProbs);
	}

	public static BasicModelTransformation<CTMC,CTMCAlteredDistributions> transform(CTMC model, double[] originProbs, double[] targetProbs)
	{
		ScaleDistribution scaleDistribution = new ScaleDistribution(model, originProbs, targetProbs);
		return new BasicModelTransformation<>(model, new CTMCAlteredDistributions(model, scaleDistribution));
	}

	public static BasicModelTransformation<DTMC,DTMCAlteredDistributions> transform(DTMC model, double[] originProbs, double[] targetProbs)
	{
		ScaleDistribution scaleDistribution = new ScaleDistribution(model, originProbs, targetProbs);
		return new BasicModelTransformation<>(model, new DTMCAlteredDistributions(model, scaleDistribution));
	}


	public static class ScaleDistribution implements IntFunction<Iterator<Entry<Integer, Double>>>
	{
		protected final DTMC model;
		protected final double[] originProbs;
		protected final double[] targetProbs;
		protected Predicate<Entry<Integer, Double>> toSupport;

		public ScaleDistribution(final DTMC model, final double[] originProbs, double[] targetProbs)
		{
			this.model       = model;
			this.originProbs = originProbs;
			this.targetProbs = targetProbs;
			this.toSupport   = trans -> {int state = trans.getKey(); return state > targetProbs.length || targetProbs[state] > 0.0;};
		}

		@Override
		public Iterator<Entry<Integer, Double>> apply(final int state)
		{
			double originProbability = state < originProbs.length ? originProbs[state] : 1.0;
			if (originProbability == 1.0) {
				// do not scale if prob == 1
				return null;
			}
			if (originProbability <= 0.0) {
				// deadlock state if prob <= 0
				return Collections.emptyIterator();
			}

			assert exitRatesAreEqual(model.getTransitionsIterator(state), scale(state, originProbability))
			     : "scaling is expected to preserve the exit rate";
			return scale(state, originProbability);
		}

		protected boolean exitRatesAreEqual(Iterator<Entry<Integer,Double>> original, Iterator<Entry<Integer,Double>> scaled)
		{
			return PrismUtils.doublesAreCloseAbs(exitRate(original), exitRate(scaled), 1e-6);
		}

		protected FunctionalIterator<Entry<Integer, Double>> scale(final int state, double originProbability)
		{
			Function<Entry<Integer, Double>, Entry<Integer, Double>> scale = new ScaleTransition(targetProbs, 1.0 / originProbability);
			return FunctionalIterator.extend(model.getTransitionsIterator(state)).filter(toSupport).map(scale);
		}

		protected double exitRate(Iterator<Entry<Integer,Double>> transitions)
		{
			return exitRate(FunctionalIterator.extend(transitions));
		}

		protected double exitRate(FunctionalIterator<Entry<Integer,Double>> transitions)
		{
			return transitions.mapToDouble(Entry::getValue).sum();
		}
	}

	public static class ScaleTransition implements Function<Entry<Integer, Double>, Entry<Integer, Double>>
	{
		protected final double[] targetProbs;
		protected final double originFactor;

		public ScaleTransition(double[] targetProbs, double originFactor)
		{
			this.targetProbs  = targetProbs;
			this.originFactor = originFactor;
		}

		@Override
		public final Entry<Integer, Double> apply(Entry<Integer, Double> transition)
		{
			int target           = transition.getKey();
			double probability   = transition.getValue();
			double scalingFactor = (target < targetProbs.length) ? targetProbs[target] * originFactor : originFactor;
			return new SimpleImmutableEntry<>(target, probability * scalingFactor);
		}
	}

	public static void main(String[] args)
	{
		// DTMC
		DTMCSimple dtmc = new DTMCSimple(4);
		dtmc.setProbability(0, 1, 0.3);
		dtmc.setProbability(0, 2, 0.3);
		dtmc.setProbability(0, 3, 0.4);
		dtmc.setProbability(1, 2, 0.5);
		dtmc.setProbability(1, 3, 0.5);
		dtmc.setProbability(2, 2, 0.8);
		dtmc.setProbability(2, 3, 0.2);
		dtmc.setProbability(3, 3, 1.0);

		System.out.println("Original DTMC:");
		System.out.println(dtmc);

		ModelTransformation<DTMC, DTMCAlteredDistributions> scaledDtmc = MCScaledTransformation.transform(dtmc, new double[] {0.7, 0.0, 1.0, 1.0});
		System.out.println("Scaled DTMC:");
		System.out.println(scaledDtmc.getTransformedModel());
		System.out.println();

		// CTMC
		CTMCSimple ctmc = new CTMCSimple(4);
		ctmc.setProbability(0, 1, 3);
		ctmc.setProbability(0, 2, 3);
		ctmc.setProbability(0, 3, 4);
		ctmc.setProbability(1, 2, 5);
		ctmc.setProbability(1, 3, 5);
		ctmc.setProbability(2, 2, 8);
		ctmc.setProbability(2, 3, 2);
		ctmc.setProbability(3, 3, 10);

		System.out.println("Original CTMC:");
		System.out.println(ctmc);

		ModelTransformation<CTMC, CTMCAlteredDistributions> scaledCtmc = MCScaledTransformation.transform(ctmc, new double[] {0.7, 0.0, 1.0, 1.0});
		System.out.println("Scaled CTMC:");
		System.out.println(scaledCtmc.getTransformedModel());
	}
}
