package explicit.conditional.transformer;

import explicit.MDP;
import explicit.MDPModelChecker;
import explicit.Model;
import explicit.conditional.ExpressionInspector;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionFilter;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionTemporal;
import prism.PrismLangException;

public class MDPConditionalMinMaxFilterTransformer extends MDPMinMaxFilterTransformer
{

	public MDPConditionalMinMaxFilterTransformer(final MDPModelChecker modelChecker)
	{
		super(modelChecker);
	}

	/**
	 * Test whether the transformer can handle a given expression or not.
	 * 
	 * @param expression
	 * @return True iff this transformation type can handle the expression.
	 * @throws PrismLangException if the expression is broken
	 */
	public boolean canHandle(final Model model, final Expression expression) throws PrismLangException
	{
		if (!(model instanceof MDP)) {
			return false;
		}
		final Expression trimmed = ExpressionInspector.trimUnaryOperations(expression);
		if (!(trimmed instanceof ExpressionFilter)) {
			return false;
		}
		final ExpressionFilter expressionFilter = (ExpressionFilter) trimmed;
		final Expression operand = ExpressionInspector.trimUnaryOperations(expressionFilter.getOperand());
		if (!(operand instanceof ExpressionConditional)) {
			return false;
		}
		final ExpressionConditional expressionConditional = (ExpressionConditional) operand;
		return canHandleOperand(expressionFilter.getOperatorType(), expressionConditional.getObjective());
	}

	protected Expression transformExpression(final ExpressionFilter expressionFilter)
	{
		final ExpressionConditional operand = (ExpressionConditional) ExpressionInspector.trimUnaryOperations(expressionFilter.getOperand());

		final ExpressionProb objective = (ExpressionProb) ExpressionInspector.trimUnaryOperations(operand.getObjective());
		final Expression condition = operand.getCondition();

		// FIXME ALG: fix insane constructor signature
		final ExpressionProb nextObjectiv = new ExpressionProb(ExpressionTemporal.Next(objective.getExpression()), objective.getMinMax(), objective.getRelOp().toString(), objective.getBound());
		final ExpressionTemporal nextCondition = ExpressionTemporal.Next(condition);

		return new ExpressionConditional(nextObjectiv, nextCondition); 
	}

}
