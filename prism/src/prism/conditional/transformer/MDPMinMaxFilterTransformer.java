package prism.conditional.transformer;

import jdd.JDD;
import jdd.JDDNode;
import explicit.MinMax;
import explicit.conditional.ExpressionInspector;
import parser.ast.Expression;
import parser.ast.ExpressionFilter;
import parser.ast.ExpressionFilter.FilterOperator;
import parser.ast.ExpressionLabel;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionTemporal;
import prism.ModelExpressionTransformation;
import prism.ModelType;
import prism.NondetModel;
import prism.NondetModelChecker;
import prism.PrismException;
import prism.PrismLangException;
import prism.SingleInitialStateTransformation;
import prism.StateValues;
import prism.StateValuesMTBDD;

//FIXME ALG: add comment
public class MDPMinMaxFilterTransformer
{
	protected NondetModelChecker modelChecker;
	protected String explanation;

	public MDPMinMaxFilterTransformer(final NondetModelChecker modelChecker)
	{
		this.modelChecker = modelChecker;
	}

	/**
	 * Test whether the transformer can handle a given expression or not.
	 * 
	 * @param expression
	 * @return True iff this transformation type can handle the expression.
	 * @throws PrismLangException if the expression is broken
	 */
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
		return canHandleOperand(expressionFilter.getOperatorType(), expressionFilter.getOperand());
	}

	public boolean canHandleOperand(final FilterOperator operatorType, final Expression operand)
			throws PrismLangException
	{
		final Expression trimmed = ExpressionInspector.trimUnaryOperations(operand);
		if (!(trimmed instanceof ExpressionProb)) {
			return false;
		}
		// Only for expression prob, since rewards might be ill defined in new initial state
		final ExpressionProb expression = (ExpressionProb) trimmed;
		if (expression.getBound() != null) {
			return false;
		}
		final MinMax minMax = expression.getRelopBoundInfo(modelChecker.getConstantValues()).getMinMax(ModelType.MDP); 
		switch (operatorType) {
		case MIN:
			return minMax.isMin();
		case MAX:
			return minMax.isMax();
		default:
			return false;
		}
	}

	public MDPMinMaxTransformation transform(final NondetModel model, final Expression expression, final JDDNode statesOfInterest)
			throws PrismException
	{
		if (!canHandle(model, expression)) {
			throw new PrismException("Cannot perform model transformation for " + expression);
		}
		final ExpressionFilter expressionFilter = (ExpressionFilter) ExpressionInspector.trimUnaryOperations(expression);

		Expression filter = expressionFilter.getFilter();
		// Create default filter (true) if none given
		if (filter == null)
			filter = Expression.True();

		String descriptionOfFilterStates;
		if (Expression.isTrue(filter)) {
			descriptionOfFilterStates = "all states";
		} else {
			descriptionOfFilterStates = "states satisfying filter";
		}
		
		switch (expressionFilter.getOperatorType()) {
		case MIN:
			explanation =  "Minimum value over " + descriptionOfFilterStates;
			break;
		case MAX:
			explanation =  "Maximum value over " + descriptionOfFilterStates;
			break;
		default:
			throw new RuntimeException("unreachable code");
		}

		JDDNode ddFilter = modelChecker.checkExpressionDD(filter, statesOfInterest);
		// Check if filter state set is empty; we treat this as an error
		if (ddFilter.equals(JDD.ZERO)) {
			throw new PrismException("Filter satisfies no states");
		}

		final NondetModel transformedModel;
		final Expression transformedExpression;

		SingleInitialStateTransformation transform = new SingleInitialStateTransformation(model, ddFilter);

		transformedModel = model.getTransformed(transform);
		String stateLabel = transformedModel.addUniqueLabelDD("newInitial", transformedModel.getStart().copy());
		transformedExpression = transformExpression(expressionFilter, stateLabel);
		transform.clear();

		return new MDPMinMaxTransformation(model, transformedModel, expressionFilter, transformedExpression);
	}

	protected Expression transformExpression(final ExpressionFilter expressionFilter, String stateLabel)
	{
		ExpressionFilter result = (ExpressionFilter)expressionFilter.deepCopy();
		final ExpressionProb operand = (ExpressionProb) ExpressionInspector.trimUnaryOperations(expressionFilter.getOperand());

		ExpressionProb newOperand = (ExpressionProb) operand.deepCopy();
		newOperand.setExpression(ExpressionTemporal.Next(Expression.Parenth(operand.getExpression())));

		result.setOperand(newOperand);
		result.setOperator("state");
		result.setFilter(new ExpressionLabel(stateLabel));
		return result;
	}
	
	public String getExplanation()
	{
		return explanation;
	}

	public static final class MDPMinMaxTransformation implements ModelExpressionTransformation<NondetModel, NondetModel>
	{
		private final NondetModel model;
		private final NondetModel transformedModel;
		private final ExpressionFilter expression;
		private final Expression transformedExpression;

		public MDPMinMaxTransformation(NondetModel model, NondetModel transformedModel, ExpressionFilter expression,
				Expression transformedExpression) throws PrismException
		{
			this.model = model;
			this.transformedModel = transformedModel;
			this.expression = expression;
			this.transformedExpression = transformedExpression;
		}

		@Override
		public NondetModel getOriginalModel()
		{
			return model;
		}

		@Override
		public NondetModel getTransformedModel()
		{
			return transformedModel;
		}

		@Override
		public StateValues projectToOriginalModel(final StateValues sv) throws PrismException
		{
			double value = sv.firstFromBDD(transformedModel.getStart());
			sv.clear();
			return new StateValuesMTBDD(JDD.Constant(value), model);
		}

		@Override
		public Expression getTransformedExpression()
		{
			return transformedExpression;
		}

		@Override
		public JDDNode getTransformedStatesOfInterest()
		{
			return transformedModel.getStart().copy();
		}

		@Override
		public Expression getOriginalExpression()
		{
			return expression;
		}

		@Override
		public void clear()
		{
			transformedModel.clear();
		}
	}
}
