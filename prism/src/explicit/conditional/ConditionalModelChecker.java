package explicit.conditional;

import java.util.BitSet;

import explicit.BasicModelTransformation;
import explicit.Model;
import explicit.StateValues;
import parser.ast.ExpressionConditional;
import parser.type.Type;
import parser.type.TypeBool;
import parser.type.TypeDouble;
import parser.type.TypeInt;
import prism.PrismComponent;
import prism.PrismException;

//FIXME ALG: add comment
abstract public class ConditionalModelChecker<M extends Model> extends PrismComponent
{
	public ConditionalModelChecker(final PrismComponent parent)
	{
		super(parent);
	}

	abstract public StateValues checkExpression(final M model, final ExpressionConditional expression, final BitSet statesOfInterest) throws PrismException;

	public StateValues createUndefinedStateValues(M model, ExpressionConditional expression) throws PrismException
	{
		Object value = null;
		Type type = expression.getType();
		if (type instanceof TypeBool) {
			value = BasicModelTransformation.DEFAULT_BOOLEAN;
		} else if (type instanceof TypeDouble) {
			value = BasicModelTransformation.DEFAULT_DOUBLE;
		} else if (type instanceof TypeInt) {
			value = BasicModelTransformation.DEFAULT_INTEGER;
		} else {
			throw new PrismException("Unexpected result type of conditional expression: " + type);
		}
		return new StateValues(type, value, model);
	}
}