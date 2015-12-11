package prism.conditional;

import jdd.JDDNode;
import parser.ast.ExpressionConditional;
import prism.Model;
import prism.Prism;
import prism.PrismException;
import prism.StateValues;

abstract public class ConditionalModelChecker<M extends Model> {
	protected Prism prism;

	public ConditionalModelChecker(final Prism prism) {
		this.prism = prism;
	}

	abstract public StateValues checkExpression(final M model, final ExpressionConditional expression, final JDDNode statesOfInterest) throws PrismException;
}
