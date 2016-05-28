package prism.conditional;

import jdd.JDDNode;
import parser.ast.ExpressionConditional;
import prism.ModelTransformation;
import prism.Prism;
import prism.PrismException;
import prism.ProbModel;
import prism.StateModelChecker;

@Deprecated
public abstract class ConditionalTransformer<M extends ProbModel, MC extends StateModelChecker> implements NewConditionalTransformer<M, MC>
{

	protected Prism prism;
	protected MC modelChecker;

	public ConditionalTransformer(final MC modelChecker, final Prism prism) {
		this.prism = prism;
		this.modelChecker = modelChecker;
	}

	public String getName() {
		Class<?> type = this.getClass();
		type = type.getEnclosingClass() == null ? type : type.getEnclosingClass();
		return type.getSimpleName();
	}

//	/**
//	 * Test whether the transformer can handle a model and a conditional expression.
//	 * 
//	 * @return True iff this transformation type can handle the expression.
//	 * @throws PrismLangException if the expression is broken
//	 */
//	public abstract boolean canHandle(final Model model, final ExpressionConditional expression) throws PrismException;


//	/**
//	 * Throw an exception, iff the transformer cannot handle the model and expression.
//	 */
//	public void checkCanHandle(ProbModel model, ExpressionConditional expression) throws PrismException
//	{
//		if (! canHandle(model, expression)) {
//			throw new PrismException("Cannot transform " + model.getModelType() + " for " + expression);
//		}
//	}

	public abstract ModelTransformation<M, M> transform(final M model, final ExpressionConditional expression, JDDNode statesOfInterest) throws PrismException;
}
