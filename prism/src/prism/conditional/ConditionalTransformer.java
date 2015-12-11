package prism.conditional;

import jdd.JDDNode;
import parser.ast.ExpressionConditional;
import prism.Model;
import prism.ModelTransformation;
import prism.Prism;
import prism.PrismException;
import prism.NonProbModelChecker;

public abstract class ConditionalTransformer<MC extends NonProbModelChecker, M extends Model> {

	protected Prism prism;
	protected MC modelChecker;

	public ConditionalTransformer(final MC modelChecker, final Prism prism) {
		this.prism = prism;
		this.modelChecker = modelChecker;
	}

	/**
	 * Test whether the transformer can handle a given conditional expression or not.<br/>
	 * 
	 * @param expression
	 * @return True iff this transformation type can handle the expression.
	 */
	public abstract boolean canHandle(final Model model, final ExpressionConditional expression) throws PrismException;

	public abstract ModelTransformation<M, M> transform(final M model, final ExpressionConditional expression, JDDNode statesOfInterest) throws PrismException;
}
