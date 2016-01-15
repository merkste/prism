package explicit.conditional.transformer;

import java.util.BitSet;

import explicit.Model;
import explicit.ModelTransformation;
import explicit.ProbModelChecker;
import parser.ast.ExpressionConditional;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismLangException;

//FIXME ALG: add comment
public abstract class ConditionalTransformer<MC extends ProbModelChecker, M extends Model> extends PrismComponent
{
	protected MC modelChecker;

	public ConditionalTransformer(final MC modelChecker)
	{
		super(modelChecker);
		this.modelChecker = modelChecker;
	}

	/**
	 * Test whether the transformer can handle a  model and conditional expression.
	 * 
	 * @return True iff this transformation type can handle the expression.
	 * @throws PrismLangException if the expression is broken
	 */
	public abstract boolean canHandle(final Model model, final ExpressionConditional expression) throws PrismLangException;

	public abstract ModelTransformation<M, M> transform(final M model, final ExpressionConditional expression, final BitSet statesOfInterest)
			throws PrismException;

	/**
	 * Throw an exception, iff the transformation cannot handle the model and expression.
	 */
	public void checkCanHandle(final Model model, final ExpressionConditional expression) throws PrismException
	{
		if (! canHandle(model, expression)) {
			throw new PrismException("Cannot transform " + model.getModelType() + " for " + expression);
		}
	}
}