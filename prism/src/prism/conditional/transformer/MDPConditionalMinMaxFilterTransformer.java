package prism.conditional.transformer;

import explicit.conditional.ExpressionInspector;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionFilter;
import parser.ast.ExpressionLabel;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionTemporal;
import prism.ModelType;
import prism.NondetModel;
import prism.NondetModelChecker;
import prism.PrismLangException;

//FIXME ALG: add comment
public class MDPConditionalMinMaxFilterTransformer extends MDPMinMaxFilterTransformer
{
	public MDPConditionalMinMaxFilterTransformer(final NondetModelChecker modelChecker)
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
	@Override
	public boolean canHandle(final NondetModel model, final Expression expression) throws PrismLangException
	{
		if (model.getModelType() != ModelType.MDP) {
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

	protected Expression transformExpression(final ExpressionFilter expressionFilter, String stateLabel)
	{
		ExpressionFilter result = (ExpressionFilter)expressionFilter.deepCopy();

		final ExpressionConditional operand = (ExpressionConditional) ExpressionInspector.trimUnaryOperations(expressionFilter.getOperand());

		final ExpressionProb objective = (ExpressionProb) ExpressionInspector.trimUnaryOperations(operand.getObjective());
		final Expression condition = operand.getCondition();

		final ExpressionProb nextObjectiv = (ExpressionProb) objective.deepCopy();
		nextObjectiv.setExpression(ExpressionTemporal.Next(Expression.Parenth(objective.getExpression())));

		final ExpressionTemporal nextCondition = ExpressionTemporal.Next(Expression.Parenth(condition));
		
		result.setOperand(new ExpressionConditional(nextObjectiv, nextCondition));
		result.setOperator("state");
		result.setFilter(new ExpressionLabel(stateLabel));

		return result;
	}
}
