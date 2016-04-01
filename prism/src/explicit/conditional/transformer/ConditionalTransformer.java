package explicit.conditional.transformer;

import java.util.BitSet;

import explicit.Model;
import explicit.ModelTransformation;
import explicit.ProbModelChecker;
import explicit.StateModelChecker;
import parser.ast.ExpressionConditional;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismLog;

// FIXME ALG: add comment
public interface ConditionalTransformer<M extends Model>
{
	default String getName() {
		Class<?> type = this.getClass();
		type = type.getEnclosingClass() == null ? type : type.getEnclosingClass();
		return type.getSimpleName();
	}

	/**
	 * Test whether the transformer can handle a model and a conditional expression.
	 * 
	 * @return True iff this transformation type can handle the expression.
	 * @throws PrismLangException if the expression is broken
	 */
	boolean canHandle(Model model, ExpressionConditional expression)
			throws PrismLangException;

	/**
	 * Throw an exception, iff the transformer cannot handle the model and expression.
	 */
	default void checkCanHandle(Model model, ExpressionConditional expression) throws PrismException
	{
		if (! canHandle(model, expression)) {
			throw new PrismException("Cannot transform " + model.getModelType() + " for " + expression);
		}
	}

	ModelTransformation<M, M> transform(M model, ExpressionConditional expression, BitSet statesOfInterest)
			throws PrismException;

	PrismLog getLog();

	LTLProductTransformer<M> getLtlTransformer();

	StateModelChecker getModelChecker();



	public static abstract class Basic<M extends Model, MC extends ProbModelChecker> extends PrismComponent implements ConditionalTransformer<M>
	{
		protected MC modelChecker;
		protected LTLProductTransformer<M> ltlTransformer;

		public Basic(MC modelChecker) {
			super(modelChecker);
			this.modelChecker = modelChecker;
		}

		public MC getModelChecker()
		{
			return modelChecker;
		}

		public LTLProductTransformer<M> getLtlTransformer()
		{
			if (ltlTransformer == null) {
				ltlTransformer = new LTLProductTransformer<M>(modelChecker);
			}
			return ltlTransformer;
		}
	}
}