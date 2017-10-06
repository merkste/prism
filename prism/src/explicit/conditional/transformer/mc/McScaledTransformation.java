package explicit.conditional.transformer.mc;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ToDoubleFunction;

import common.functions.Predicate;
import common.iterable.FunctionalIterator;
import explicit.BasicModelTransformation;
import explicit.CTMCSimple;
import explicit.DTMC;
import explicit.DTMCSimple;
import explicit.ModelTransformation;
import explicit.modelviews.DTMCAlteredDistributions;
import prism.PrismUtils;

public class McScaledTransformation
{
	public static BasicModelTransformation<DTMC,DTMCAlteredDistributions> transform(DTMC model, double[] originProbs)
	{
		return transform(model, originProbs, originProbs);
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
			return transitions.map((ToDoubleFunction<Entry<?, Double>>) Entry::getValue).sum();
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
		DTMCSimple original = new DTMCSimple(4);
		original.setProbability(0, 1, 0.3);
		original.setProbability(0, 2, 0.3);
		original.setProbability(0, 3, 0.4);
		original.setProbability(1, 2, 0.5);
		original.setProbability(1, 3, 0.5);
		original.setProbability(2, 2, 0.8);
		original.setProbability(2, 3, 0.2);
		original.setProbability(3, 3, 1.0);

		System.out.println("Original DTMC:");
		System.out.println(original);

		ModelTransformation<DTMC, DTMCAlteredDistributions> scaled = McScaledTransformation.transform(original, new double[] {0.7, 0.0, 1.0, 1.0});
		System.out.println("Scaled DTMC:");
		System.out.println(scaled.getTransformedModel());
		System.out.println();

		// CTMC
		original = new CTMCSimple(4);
		original.setProbability(0, 1, 3);
		original.setProbability(0, 2, 3);
		original.setProbability(0, 3, 4);
		original.setProbability(1, 2, 5);
		original.setProbability(1, 3, 5);
		original.setProbability(2, 2, 8);
		original.setProbability(2, 3, 2);
		original.setProbability(3, 3, 10);

		System.out.println("Original CTMC:");
		System.out.println(original);

		scaled = McScaledTransformation.transform(original, new double[] {0.7, 0.0, 1.0, 1.0});
		System.out.println("Scaled CTMC:");
		System.out.println(scaled.getTransformedModel());
	}
}
