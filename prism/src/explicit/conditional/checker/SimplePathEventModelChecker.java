package explicit.conditional.checker;

import java.util.BitSet;

import explicit.ProbModelChecker;
import prism.PrismComponent;
import prism.PrismException;

public interface SimplePathEventModelChecker<M extends explicit.Model, C extends ProbModelChecker>
{
	public static final BitSet ALL_STATES = null;
	public static final BitSet NO_STATES  = new BitSet(0);

	public C getModelChecker();

	/**
	 * Instantiate a fresh model checker for a model.
	 * 
	 * @param model
	 * @return model checker instance
	 * @throws PrismException if instantiation fails
	 */
	default C getModelChecker(M model) throws PrismException
	{
		C parent  = getModelChecker();
		@SuppressWarnings("unchecked")
		C checker = (C) C.createModelChecker(model.getModelType(), parent);
		checker.inheritSettings(parent);
		return checker;
	}



	public static abstract class Basic<M extends explicit.Model, C extends ProbModelChecker> extends PrismComponent implements SimplePathEventModelChecker<M, C>
	{
		protected final C modelChecker;

		public Basic(C modelChecker)
		{
			super(modelChecker);
			this.modelChecker = modelChecker;
		}

		@Override
		public C getModelChecker()
		{
			return modelChecker;
		}

		/**
		 * Subtract probabilities from one in-place.
		 * 
		 * @param probabilities
		 * @return argument array altered to hold result
		 */
		public static double[] subtractFromOne(double[] probabilities)
		{
			for (int state = 0; state < probabilities.length; state++) {
				probabilities[state] = 1 - probabilities[state];
			}
			return probabilities;
		}
	}
}
