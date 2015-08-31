package explicit.conditional;

import java.util.BitSet;

import explicit.Model;
import explicit.StateValues;
import parser.ast.ExpressionConditional;
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
}