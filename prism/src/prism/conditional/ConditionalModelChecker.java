package prism.conditional;

import explicit.BasicModelTransformation;
import jdd.JDD;
import jdd.JDDNode;
import parser.ast.ExpressionConditional;
import parser.type.Type;
import parser.type.TypeBool;
import parser.type.TypeDouble;
import parser.type.TypeInt;
import prism.Model;
import prism.Prism;
import prism.PrismException;
import prism.StateValues;
import prism.StateValuesMTBDD;

abstract public class ConditionalModelChecker<M extends Model> {
	protected Prism prism;

	public ConditionalModelChecker(final Prism prism) {
		this.prism = prism;
	}

	abstract public StateValues checkExpression(final M model, final ExpressionConditional expression, final JDDNode statesOfInterest) throws PrismException;

	public StateValues createUndefinedStateValues(M model, ExpressionConditional expression) throws PrismException
	{
		JDDNode value = null;
		Type type = expression.getType();
		if (type instanceof TypeBool) {
			value = JDD.Constant(BasicModelTransformation.DEFAULT_BOOLEAN ? 1 : 0);
		} else if (type instanceof TypeDouble) {
			value = JDD.Constant(BasicModelTransformation.DEFAULT_DOUBLE);
		} else if (type instanceof TypeInt) {
			value = JDD.Constant(BasicModelTransformation.DEFAULT_INTEGER);
		} else {
			throw new PrismException("Unexpected result type of conditional expression: " + type);
		}
		return new StateValuesMTBDD(value, model);
	}
}
