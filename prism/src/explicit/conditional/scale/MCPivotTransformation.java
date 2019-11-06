package explicit.conditional.scale;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.IntFunction;

import common.BitSetTools;
import common.iterable.FunctionalIterator;
import explicit.BasicModelTransformation;
import explicit.CTMC;
import explicit.DTMC;
import explicit.DTMCSimple;
import explicit.ModelTransformation;
import explicit.ReachabilityComputer;
import explicit.modelviews.CTMCAlteredDistributions;
import explicit.modelviews.CTMCDisjointUnion;
import explicit.modelviews.DTMCAlteredDistributions;
import explicit.modelviews.DTMCDisjointUnion;

//FIXME ALG: add comment
public class MCPivotTransformation
{
	public static BasicModelTransformation<CTMC, CTMCAlteredDistributions> transform(final CTMC model, final BitSet pivotStates)
	{
		CTMCDisjointUnion union = new CTMCDisjointUnion(model, model);
		BitSet prePivotStates   = new ReachabilityComputer(model).computePre(pivotStates);
		Pivot pivot             = new Pivot(union, pivotStates, prePivotStates);
		return new BasicModelTransformation<>(model, new CTMCAlteredDistributions(union, pivot));
	}

	public static BasicModelTransformation<DTMC, DTMCAlteredDistributions> transform(final DTMC model, final BitSet pivotStates)
	{
		DTMCDisjointUnion union = new DTMCDisjointUnion(model, model);
		BitSet prePivotStates   = new ReachabilityComputer(model).computePre(pivotStates);
		Pivot pivot             = new Pivot(union, pivotStates, prePivotStates);
		return new BasicModelTransformation<>(model, new DTMCAlteredDistributions(union, pivot));
	}

	public static class Pivot implements IntFunction<Iterator<Entry<Integer, Double>>>
	{
		protected final DTMC model;
		protected final BitSet prePivotStates;
		protected final Redirect redirect;

		public Pivot(final CTMCDisjointUnion model, final BitSet pivotStates, final BitSet prePivotStates)
		{
			this.model          = model;
			this.prePivotStates = prePivotStates;
			this.redirect       = new Redirect(pivotStates, model.offset);
		}

		public Pivot(final DTMCDisjointUnion model, final BitSet pivotStates, final BitSet prePivotStates)
		{
			this.model          = model;
			this.prePivotStates = prePivotStates;
			this.redirect       = new Redirect(pivotStates, model.offset);
		}

		@Override
		public Iterator<Entry<Integer, Double>> apply(final int state)
		{
			if (! prePivotStates.get(state)) {
				return null;
			}
			return FunctionalIterator.extend(model.getTransitionsIterator(state)).map(redirect);
		}
	}

	public static class Redirect implements Function<Entry<Integer, Double>, Entry<Integer, Double>>
	{
		protected final BitSet pivotStates;
		protected final int offset;

		public Redirect(final BitSet pivotStates, final int offset)
		{
			this.pivotStates = pivotStates;
			this.offset = offset;
		}

		@Override
		public Entry<Integer, Double> apply(final Entry<Integer, Double> transition)
		{
			int target = transition.getKey();
			if (! pivotStates.get(target)) {
				return transition;
			}
			Double probability = transition.getValue();
			return new SimpleImmutableEntry<>(target + offset, probability);
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

		ModelTransformation<DTMC, DTMCAlteredDistributions> pivoted = MCPivotTransformation.transform(original, BitSetTools.asBitSet(2));
		System.out.println("Pivoted Model:");
		System.out.println(pivoted.getTransformedModel());
	}
}
