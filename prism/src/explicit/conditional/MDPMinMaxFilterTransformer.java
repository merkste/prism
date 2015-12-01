package explicit.conditional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import common.BitSetTools;
import common.functions.primitive.MappingInt;
import common.iterable.IterableStateSet;
import common.iterable.collections.MappingList;
import explicit.Distribution;
import explicit.MDP;
import explicit.MDPModelChecker;
import explicit.MDPSimple;
import explicit.MinMax;
import explicit.Model;
import explicit.ModelExpressionTransformation;
import explicit.StateValues;
import explicit.modelviews.MDPAdditionalChoices;
import explicit.modelviews.MDPDisjointUnion;
import parser.State;
import parser.ast.Expression;
import parser.ast.ExpressionFilter;
import parser.ast.ExpressionFilter.FilterOperator;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionTemporal;
import parser.type.TypeBool;
import parser.type.TypeDouble;
import parser.type.TypeInt;
import prism.ModelType;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismLangException;

public class MDPMinMaxFilterTransformer extends PrismComponent
{
	protected MDPModelChecker modelChecker;

	public MDPMinMaxFilterTransformer(final MDPModelChecker modelChecker)
	{
		super(modelChecker);
		this.modelChecker = modelChecker;
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

	public MDPMinMaxTransformation transform(final MDP model, final Expression expression, final BitSet statesOfInterest)
			throws PrismException
	{
		if (!canHandle(model, expression)) {
			throw new PrismException("Cannot perform model tranformation for " + expression + " and " + model.infoString());
		}
		final ExpressionFilter expressionFilter = (ExpressionFilter) ExpressionInspector.trimUnaryOperations(expression);

		// Compute filter states
		final Expression filter = expressionFilter.getFilter();
		final String descriptionOfFilterStates;
		final BitSet filterStates;
		if (filter == null || Expression.isTrue(filter)) {
			descriptionOfFilterStates = "all states";
			filterStates = null;
			mainLog.println("\nStates satisfying filter " + filter + ": " + model.getNumStates());
		} else {
			descriptionOfFilterStates = "states satisfying filter";
			if (filter.isInitLabel()) {
				filterStates = BitSetTools.asBitSet(model.getInitialStates());
			} else {
				if (filter.isDeadlockLabel()) {
					filterStates = BitSetTools.asBitSet(model.getDeadlockStates());
				} else {
					filterStates = modelChecker.checkExpression(model, filter, null).getBitSet();
				}
				mainLog.println("\nStates satisfying filter " + filter + ": " + filterStates.cardinality());
			}
			if (filterStates.isEmpty()) {
				throw new PrismException("Filter satisfies no states");
			}
		}

		final MDP transformedModel;
		final Expression transformedExpression;
		final BitSet transformedStatesOfInterest;
		if (filterStates == null || filterStates.cardinality() > 1) {
			// multiple states: next-step transformation
			transformedModel = transformModel(model, filterStates);
			transformedExpression = transformExpression(expressionFilter);
			transformedStatesOfInterest = new BitSet(model.getNumStates() + 1);
			transformedStatesOfInterest.set(model.getNumStates());
		} else {
			// one state: remove filter expression
			transformedModel = model;
			transformedExpression = expressionFilter.getOperand();
			transformedStatesOfInterest = filterStates;
		}

		return new MDPMinMaxTransformation(model, transformedModel, expressionFilter, transformedExpression, transformedStatesOfInterest, descriptionOfFilterStates);
	}

	protected MDP transformModel(final MDP model, final BitSet filterStates)
	{
		final MDPSimple init = new MDPSimple();
		init.addState();
		final List<State> statesList = model.getStatesList();
		if (statesList != null) {
			init.setStatesList(Arrays.asList(statesList.get(0)));
		}

		final MDPDisjointUnion union = new MDPDisjointUnion(model, init);

		final int numberOfFilterStates = (filterStates == null) ? model.getNumStates() : filterStates.cardinality();
		final List<Distribution> distributions = new ArrayList<>(numberOfFilterStates);
		for (int target : new IterableStateSet(filterStates, model.getNumStates())) {
			final Distribution distribution = new Distribution();
			distribution.add(target, 1.0);
			distributions.add(distribution);
		}

		final MappingInt<List<Iterator<Entry<Integer, Double>>>> choices = new MappingInt<List<Iterator<Entry<Integer,Double>>>>() {
			final int initialState = model.getNumStates();

			@Override
			public List<Iterator<Entry<Integer, Double>>> apply(final int state)
			{
				if (state == initialState) {
					return new MappingList<>(distributions, Iterable::iterator);
				}
				return Collections.emptyList();
			}
		};
		return new MDPAdditionalChoices(union, choices, null);
	}

	protected Expression transformExpression(final ExpressionFilter expressionFilter)
	{
		final ExpressionProb operand = (ExpressionProb) ExpressionInspector.trimUnaryOperations(expressionFilter.getOperand());

		// FIXME ALG: fix insane constructor signature
		return new ExpressionProb(ExpressionTemporal.Next(operand.getExpression()), operand.getMinMax(), operand.getRelOp().toString(), operand.getBound());
	}

	// FIXME ALG: extract common super class
	public static final class MDPMinMaxTransformation implements ModelExpressionTransformation<MDP, MDP>
	{
		private final MDP model;
		private final MDP transformedModel;
		private final ExpressionFilter expression;
		private final Expression transformedExpression;
		private final BitSet transformedStatesOfInterest;
		private final String descriptionOfFilterStates;

		public MDPMinMaxTransformation(MDP model, MDP transformedModel, ExpressionFilter expression,
				Expression transformedExpression, BitSet transformedStatesOfInterest, String descriptionOfFilterStates) throws PrismException
		{
			this.model = model;
			this.transformedModel = transformedModel;
			this.expression = expression;
			this.transformedExpression = transformedExpression;
			this.transformedStatesOfInterest = transformedStatesOfInterest;
			this.descriptionOfFilterStates = descriptionOfFilterStates;
			final FilterOperator operatorType = expression.getOperatorType();
			if (! (operatorType == FilterOperator.MIN || operatorType == FilterOperator.MAX)) {
				throw new PrismException("unexpected filter type");
			}
		}

		@Override
		public MDP getOriginalModel()
		{
			return model;
		}

		@Override
		public MDP getTransformedModel()
		{
			return transformedModel;
		}

		@Override
		// FIXME ALG: code duplication in ConditionalTransformation
		public StateValues projectToOriginalModel(final StateValues sv) throws PrismException
		{
			if (sv.getType() instanceof TypeBool) {
				assert(sv.getBitSet() != null) : "State values are undefined.";

				final BitSet mapped = projectToOriginalModel(sv.getBitSet());
				return StateValues.createFromBitSet(mapped, model);
			}
			if (sv.getType() instanceof TypeDouble) {
				assert(sv.getDoubleArray() != null) : "State values are undefined.";

				final double[] mapped = projectToOriginalModel(sv.getDoubleArray());
				return StateValues.createFromDoubleArray(mapped, model);
			}
			if (sv.getType() instanceof TypeInt) {
				assert(sv.getIntArray() != null) : "State values are undefined.";

				final int[] mapped = projectToOriginalModel(sv.getIntArray());
				return StateValues.createFromIntegerArray(mapped, model);
			}
			throw new PrismException("Unsupported type of state values");
		}

		public BitSet projectToOriginalModel(final BitSet values)
		{
			return values.get(0, model.getNumStates());
		}

		public double[] projectToOriginalModel(final double[] values)
		{
			return Arrays.copyOf(values, model.getNumStates());
		}

		public int[] projectToOriginalModel(final int[] values)
		{
			return Arrays.copyOf(values, model.getNumStates());
		}

		@Override
		public Expression getTransformedExpression()
		{
			return transformedExpression;
		}

		@Override
		public BitSet getTransformedStatesOfInterest()
		{
			return transformedStatesOfInterest;
		}

		@Override
		public Expression getOriginalExpression()
		{
			return expression;
		}

		public String getExplanation()
		{
			switch (expression.getOperatorType()) {
			case MIN:
				return "Minimum value over " + descriptionOfFilterStates;
			case MAX:
				return "Maximum value over " + descriptionOfFilterStates;
			default:
				throw new RuntimeException("unreachable code");
			}
		}

		@Override
		public Integer mapToTransformedModel(int state)
		{
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public BitSet mapToTransformedModel(BitSet states)
		{
			// TODO Auto-generated method stub
			return null;
		}
	}
}
