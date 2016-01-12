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
	 * Test whether the transformer can handle a given conditional expression or not.
	 * 
	 * @param expression
	 * @return True iff this transformation type can handle the expression.
	 * @throws PrismLangException if the expression is broken
	 */
	public abstract boolean canHandle(final Model model, final ExpressionConditional expression) throws PrismLangException;

	public abstract ModelTransformation<M, M> transform(final M model, final ExpressionConditional expression, final BitSet statesOfInterest)
			throws PrismException;
}