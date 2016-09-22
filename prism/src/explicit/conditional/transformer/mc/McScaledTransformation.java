package explicit.conditional.transformer.mc;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;

import common.functions.Predicate;
import common.iterable.FunctionalIterator;
import explicit.BasicModelTransformation;
import explicit.DTMC;
import explicit.DTMCSimple;
import explicit.ModelTransformation;
import explicit.modelviews.DTMCAlteredDistributions;

public class McScaledTransformation
{
	public static BasicModelTransformation<DTMC,DTMCAlteredDistributions> transform(DTMC model, double[] probabilities)
	{
		ScaleDistribution scaleDistribution = new ScaleDistribution(model, probabilities);
		return new BasicModelTransformation<>(model, new DTMCAlteredDistributions(model, scaleDistribution));
	}

	public static class ScaleDistribution implements IntFunction<Iterator<Entry<Integer, Double>>>
	{
		protected final DTMC model;
		protected final double[] probabilities;
		protected final IntPredicate inSupport;
		protected Predicate<Entry<Integer, Double>> toSupport;

		private ScaleDistribution(final DTMC model, final double[] probs)
		{
			this.model         = model;
			this.probabilities = probs;
			this.inSupport     = state -> state > probs.length || probs[state] > 0.0;
			this.toSupport     = // trans -> {int state = trans.getKey(); return state > probs.length || probs[state] > 0.0;};
			                     trans -> inSupport.test(trans.getKey());
		}

		@Override
		public Iterator<Entry<Integer, Double>> apply(final int state)
		{
			double originProbability = state < probabilities.length ? probabilities[state] : 1.0;
			if (originProbability == 1.0) {
				// do not scale if prob == 1
				return null;
			}
			if (originProbability <= 0.0) {
				// deadlock state if prob <= 0
				return Collections.emptyIterator();
			}

			Function<Entry<Integer, Double>, Entry<Integer, Double>> scale = new ScaleTransition(probabilities, originProbability);
			return FunctionalIterator.extend(model.getTransitionsIterator(state)).filter(toSupport).map(scale);
		}
	}

	public static class ScaleTransition implements Function<Entry<Integer, Double>, Entry<Integer, Double>>
	{
		protected final double[] probabilities;
		protected final double originFactor;

		public ScaleTransition(double[] probabilities, double originProbability)
		{
			this.probabilities = probabilities;
			this.originFactor  = 1.0 / originProbability;
		}

		@Override
		public final Entry<Integer, Double> apply(Entry<Integer, Double> transition)
		{
			int target           = transition.getKey();
			double probability   = transition.getValue();
			double scalingFactor = (target < probabilities.length) ? probabilities[target] * originFactor : originFactor;
			return new SimpleImmutableEntry<>(target, probability * scalingFactor);
		}
	}

	public static void main(String[] args)
	{
		DTMCSimple original = new DTMCSimple(4);
		original.setProbability(0, 1, 0.3);
		original.setProbability(0, 2, 0.3);
		original.setProbability(0, 3, 0.4);
		original.setProbability(1, 2, 0.5);
		original.setProbability(1, 3, 0.5);
		original.setProbability(2, 2, 0.8);
		original.setProbability(2, 3, 0.2);
		original.setProbability(3, 3, 1.0);

		System.out.println("Original Model:");
		System.out.println(original);

		ModelTransformation<DTMC, DTMCAlteredDistributions> scaled = McScaledTransformation.transform(original, new double[] {0.7, 0.0, 1.0, 1.0});
		System.out.println("Scaled Model:");
		System.out.println(scaled.getTransformedModel());
	}
}
