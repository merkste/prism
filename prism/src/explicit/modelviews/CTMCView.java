package explicit.modelviews;

import java.util.BitSet;
import java.util.Iterator;
import java.util.Map;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.function.IntFunction;

import common.iterable.FunctionalIterable;
import common.iterable.FunctionalIterator;
import common.iterable.FunctionalPrimitiveIterable.IterableDouble;
import common.iterable.Interval;
import common.iterable.IterableBitSet;
import explicit.CTMC;
import explicit.DTMC;
import explicit.DTMCEmbeddedSimple;
import explicit.DTMCSimple;
import explicit.DTMCUniformisedSimple;
import prism.ModelType;

public abstract class CTMCView extends MCView implements CTMC, Cloneable
{
	public CTMCView()
	{
		super();
	}

	public CTMCView(final ModelView model)
	{
		super(model);
	}



	//--- Object ---



	//--- Model ---

	@Override
	public ModelType getModelType()
	{
		return ModelType.CTMC;
	}
	


	//--- DTMC ---



	//--- CTMC ---

	@Override
	public double getExitRate(int i)
	{
		FunctionalIterator<Entry<Integer, Double>> transitions = FunctionalIterator.extend(getTransitionsIterator(i));
		return transitions.mapToDouble(Entry::getValue).sum();
	}

	@Override
	public double getMaxExitRate()
	{
		IterableDouble exitRates = new Interval(getNumStates()).mapToDouble((int s) -> getExitRate(s));
		return exitRates.max().orElse(Double.NEGATIVE_INFINITY);
	}

	@Override
	public double getMaxExitRate(BitSet subset)
	{
		IterableDouble exitRates = new IterableBitSet(subset).mapToDouble((int s) -> getExitRate(s));
		return exitRates.max().orElse(Double.NEGATIVE_INFINITY);
	}

	@Override
	public double getDefaultUniformisationRate()
	{
		return 1.02 * getMaxExitRate(); 
	}

	@Override
	public double getDefaultUniformisationRate(BitSet nonAbs)
	{
		return 1.02 * getMaxExitRate(nonAbs); 
	}

	@Override
	public DTMC buildImplicitEmbeddedDTMC()
	{
		return new DTMCEmbeddedSimple(this);
	}

	@Override
	public DTMC getImplicitEmbeddedDTMC()
	{
		return buildImplicitEmbeddedDTMC();
	}

	@Override
	public DTMCSimple buildEmbeddedDTMC()
	{
		int numStates = getNumStates();
		DTMCSimple dtmc = new DTMCSimple(numStates);
		for (int in : getInitialStates()) {
			dtmc.addInitialState(in);
		}
		for (int i = 0; i < numStates; i++) {
			final int s = i;
			FunctionalIterable<Entry<Integer,Double>> transitions = FunctionalIterable.extend(() -> getTransitionsIterator(s));
			double d = transitions.mapToDouble(Entry::getValue).sum();
			if (d == 0) {
				dtmc.setProbability(i, i, 1.0);
			} else {
				for (Map.Entry<Integer, Double> e : transitions) {
					dtmc.setProbability(i, e.getKey(), e.getValue() / d);
				}
			}
		}
		return dtmc;
	}

	@Override
	public DTMC buildImplicitUniformisedDTMC(double q)
	{
		return new DTMCUniformisedSimple(this, q);
	}

	@Override
	public DTMCSimple buildUniformisedDTMC(double q)
	{
		int numStates = getNumStates();
		DTMCSimple dtmc = new DTMCSimple(numStates);
		for (int in : getInitialStates()) {
			dtmc.addInitialState(in);
		}
		for (int i = 0; i < numStates; i++) {
			final int s = i;
			FunctionalIterable<Entry<Integer,Double>> transitions = FunctionalIterable.extend(() -> getTransitionsIterator(s));
			// Add scaled off-diagonal entries
			for (Entry<Integer, Double> e : transitions) {
				dtmc.setProbability(i, e.getKey(), e.getValue() / q);
			}
			// Add diagonal, if needed
			double d = transitions.filter(e -> e.getKey() != s).mapToDouble(Entry::getValue).sum();
			if (d < q) {
				dtmc.setProbability(i, i, 1 - (d / q));
			}
		}
		return dtmc;
	}



	//--- ModelView ---



	//--- instance methods ---

	protected CTMC uniformised(CTMC ctmc, double q)
	{
		IntFunction<Iterator<Entry<Integer, Double>>> uniformise = new IntFunction<Iterator<Entry<Integer, Double>>>()
		{
			@Override
			public Iterator<Entry<Integer, Double>> apply(int s)
			{
				FunctionalIterable<Entry<Integer, Double>> transitions = FunctionalIterable.extend(() -> getTransitionsIterator(s));
				double sum = transitions.filter(e -> e.getKey() != s).mapToDouble(Entry::getValue).sum();
				SimpleImmutableEntry<Integer, Double> diagonale = new SimpleImmutableEntry<>(s, q - sum);
				return transitions.map((Entry<Integer, Double> e) -> e.getKey() == s ? diagonale : e).iterator();
			}

		};
		return new CTMCAlteredDistributions(ctmc, uniformise);
	}
}