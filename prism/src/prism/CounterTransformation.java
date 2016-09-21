package prism;

import java.util.ArrayList;
import java.util.List;

import jdd.JDD;
import jdd.JDDNode;
import parser.ast.Expression;
import parser.ast.ExpressionReward;
import parser.ast.RewardStruct;
import parser.ast.TemporalOperatorBound;
import parser.ast.TemporalOperatorBounds;
import parser.visitor.ReplaceBound;
import prism.IntegerBound;
import prism.PrismException;

public class CounterTransformation<M extends Model> implements ModelExpressionTransformation<M, M> {
	private Expression originalExpression;
	private Expression transformedExpression;
	private M originalModel;
	private RewardCounterProduct<M> product;

	StateModelChecker mc;

	/**
	 * The originalExpression will be modified!
	 * @param mc
	 * @param originalModel
	 * @param originalExpression
	 * @param bound
	 * @param statesOfInterest
	 * @throws PrismException
	 */
	public CounterTransformation(StateModelChecker mc,
			M originalModel, Expression originalExpression,
			TemporalOperatorBound bound,
			JDDNode statesOfInterest) throws PrismException {
		this.originalModel = originalModel;
		this.originalExpression = originalExpression.deepCopy();
		this.mc = mc;
		
		transformedExpression = originalExpression;
		doTransformation(originalModel, bound, statesOfInterest);
	}
	
	/**
	 * The originalExpression will be modified!
	 * @param mc
	 * @param originalModel
	 * @param originalExpression
	 * @param bound
	 * @param statesOfInterest 
	 * @throws PrismException
	 */
	public CounterTransformation(StateModelChecker mc,
			M originalModel, Expression originalExpression,
			List<TemporalOperatorBound> bounds,
			JDDNode statesOfInterest) throws PrismException {
		this.originalModel = originalModel;
		this.originalExpression = originalExpression.deepCopy();
		this.mc = mc;
		
		transformedExpression = originalExpression;
		doTransformation(originalModel, bounds, statesOfInterest);
	}


	@Override
	public Expression getTransformedExpression() {
		return transformedExpression;
	}

	@Override
	public M getTransformedModel() {
		return product.getTransformedModel();
	}

	@Override
	public JDDNode getTransformedStatesOfInterest() {
		return product.getTransformedStatesOfInterest();
	}

	@Override
	public M getOriginalModel() {
		return originalModel;
	}

	@Override
	public Expression getOriginalExpression() {
		return originalExpression;
	}

	@Override
	public StateValues projectToOriginalModel(StateValues svTransformedModel)
			throws PrismException {
		return product.projectToOriginalModel(svTransformedModel);
	}

	private void doTransformation(M originalModel, TemporalOperatorBound bound, JDDNode statesOfInterest) throws PrismException {
		List<TemporalOperatorBound> bounds = new ArrayList<TemporalOperatorBound>();
		bounds.add(bound);
		doTransformation(originalModel, bounds, statesOfInterest);
	}
	
	private void doTransformation(M originalModel, List<TemporalOperatorBound> bounds, JDDNode statesOfInterest) throws PrismException {	
		if (originalModel instanceof NondetModel) {
				doTransformation((NondetModel)originalModel, bounds, statesOfInterest);
		} else if (originalModel instanceof ProbModel) {
			doTransformation((ProbModel)originalModel, bounds, statesOfInterest);

		} else {
			throw new PrismException("Counter-Transformation is not supported for "+originalModel.getClass());
		}
	}
	
	
	
	@SuppressWarnings("unchecked")
	private void doTransformation(NondetModel originalModel, List<TemporalOperatorBound> bounds, JDDNode statesOfInterest) throws PrismException
	{
		List<IntegerBound> intBounds = new ArrayList<IntegerBound>();

		if (bounds.isEmpty()) {
			throw new IllegalArgumentException("Can not do counter transformation without any bounds.");
		}

		for (TemporalOperatorBound bound : bounds) {
			IntegerBound intBound = IntegerBound.fromTemporalOperatorBound(bound, mc.constantValues, true);
			intBounds.add(intBound);

			if (!bound.hasSameDomainDiscreteTime(bounds.get(0))) {
				throw new IllegalArgumentException("Can only do counter transformation for bounds with same domain.");
			}
		}
		JDDNode trRewards = null;

		switch (bounds.get(0).getBoundType()) {
		case REWARD_BOUND: {
			// Get reward info
			Object rsi = bounds.get(0).getRewardStructureIndex();
			JDDNode srew = mc.getStateRewardsByIndexObject(rsi, originalModel, mc.constantValues).copy();
			JDDNode trew = mc.getTransitionRewardsByIndexObject(rsi, originalModel, mc.constantValues).copy();

			trRewards = JDD.Apply(JDD.PLUS, srew, trew);
			break;
		}
		case DEFAULT_BOUND:
		case STEP_BOUND:
		case TIME_BOUND:
			// a time/step bound, use constant reward structure assigning 1 to each state
			trRewards = JDD.Constant(1);
			break;
		}

		if (trRewards == null) {
			throw new PrismException("Couldn't generate reward information.");
		}

		int saturation_limit = IntegerBound.getMaximalInterestingValueForConjunction(intBounds);

		product = (RewardCounterProduct<M>) RewardCounterProduct.generate(mc.prism, originalModel, trRewards, saturation_limit, statesOfInterest);

		// add 'in_bound-x' label
		JDDNode statesInBound = product.getStatesWithAccumulatedRewardInBoundConjunction(intBounds);
		//JDD.PrintMinterms(mc.prism.getMainLog(), statesInBound.copy(), "statesInBound (1)");
		statesInBound = JDD.And(statesInBound, product.getTransformedModel().getReach().copy());
		//JDD.PrintMinterms(mc.prism.getMainLog(), statesInBound.copy(), "statesInBound (2)");
		String labelInBound = ((NondetModel)product.getTransformedModel()).addUniqueLabelDD("in_bound", statesInBound, mc.getDefinedLabelNames());

		// transform expression
		for (TemporalOperatorBound bound : bounds) {
			ReplaceBound replace = new ReplaceBound(bound, labelInBound);
			transformedExpression = (Expression) transformedExpression.accept(replace);

			if (!replace.wasSuccessfull()) {
				throw new PrismException("Replacing bound was not successfull.");
			}
		}
	}


	@SuppressWarnings("unchecked")
	private void doTransformation(ProbModel originalModel, List<TemporalOperatorBound> bounds, JDDNode statesOfInterest) throws PrismException
	{
		List<IntegerBound> intBounds = new ArrayList<IntegerBound>();

		if (bounds.isEmpty()) {
			throw new IllegalArgumentException("Can not do counter transformation without any bounds.");
		}

		for (TemporalOperatorBound bound : bounds) {
			IntegerBound intBound = IntegerBound.fromTemporalOperatorBound(bound, mc.constantValues, true);
			intBounds.add(intBound);

			if (!bound.hasSameDomainDiscreteTime(bounds.get(0))) {
				throw new IllegalArgumentException("Can only do counter transformation for bounds with same domain.");
			}
		}
		JDDNode trRewards = null;

		switch (bounds.get(0).getBoundType()) {
		case REWARD_BOUND: {
			// Get reward info
			Object rsi = bounds.get(0).getRewardStructureIndex();
			JDDNode srew = mc.getStateRewardsByIndexObject(rsi, originalModel, mc.constantValues).copy();
			JDDNode trew = mc.getTransitionRewardsByIndexObject(rsi, originalModel, mc.constantValues).copy();

			trRewards = JDD.Apply(JDD.PLUS, srew, trew);
			break;
		}
		case DEFAULT_BOUND:
		case STEP_BOUND:
		case TIME_BOUND:
			// a time/step bound, use constant reward structure assigning 1 to each state
			trRewards = JDD.Constant(1);
			break;
		}

		if (trRewards == null) {
			throw new PrismException("Couldn't generate reward information.");
		}

		int saturation_limit = IntegerBound.getMaximalInterestingValueForConjunction(intBounds);

		product = (RewardCounterProduct<M>) RewardCounterProduct.generate(mc.prism, originalModel, trRewards, saturation_limit, statesOfInterest);

		// add 'in_bound-x' label
		JDDNode statesInBound = product.getStatesWithAccumulatedRewardInBoundConjunction(intBounds);
		//JDD.PrintMinterms(mc.prism.getMainLog(), statesInBound.copy(), "statesInBound (1)");
		statesInBound = JDD.And(statesInBound, product.getTransformedModel().getReach().copy());
		//JDD.PrintMinterms(mc.prism.getMainLog(), statesInBound.copy(), "statesInBound (2)");
		String labelInBound = ((ProbModel)product.getTransformedModel()).addUniqueLabelDD("in_bound", statesInBound, mc.getDefinedLabelNames());

		// transform expression
		for (TemporalOperatorBound bound : bounds) {
			ReplaceBound replace = new ReplaceBound(bound, labelInBound);
			transformedExpression = (Expression) transformedExpression.accept(replace);

			if (!replace.wasSuccessfull()) {
				throw new PrismException("Replacing bound was not successfull.");
			}
		}
	}
	
	public static <M extends Model> ModelExpressionTransformation<M, M> replaceBoundsWithCounters(StateModelChecker mc,
			M originalModel, Expression originalExpression,
			List<TemporalOperatorBound> bounds,
			JDDNode statesOfInterest) throws PrismException {

		if (bounds.isEmpty()) {
			throw new PrismException("No bounds to replace!");
		}
		
		if (!originalExpression.isSimplePathFormula()) {
			throw new PrismException("Replacing bounds is only supported in simple path formulas.");
		}
		
		Prism prism = mc.prism;

		// TODO: Check nesting depth = 1

		ModelExpressionTransformation<M, M> nested = null;
		for (TemporalOperatorBound bound : bounds) {
			// resolve RewardStruct for reward bounds
			if (bound.isRewardBound()) {
				int r = ExpressionReward.getRewardStructIndexByIndexObject(bound.getRewardStructureIndex(), mc.prism.getPRISMModel(), mc.constantValues);
				bound.setResolvedRewardStructIndex(r);
			}
		}

		List<List<TemporalOperatorBound>> groupedBoundList = TemporalOperatorBounds.groupBoundsDiscreteTime(bounds);

		for (List<TemporalOperatorBound> groupedBounds : groupedBoundList) {
			if (groupedBounds.get(0).isRewardBound()) {
				String rewStructName = mc.getModulesFile().getRewardStructNames().get(groupedBounds.get(0).getResolvedRewardStructIndex());
				prism.getLog().println("Transform to incorporate counter for reward '" + rewStructName + "' and " + groupedBounds);
			} else {
				prism.getLog().println("Transform to incorporate counter for steps "+groupedBounds);
			}

			ModelExpressionTransformation<M, M> current;
			
			if (nested == null) {
				current = new CounterTransformation<M>(mc, originalModel, originalExpression, groupedBounds, statesOfInterest);
				nested = current;
			} else {
			    current = new CounterTransformation<M>(mc, nested.getTransformedModel(), nested.getTransformedExpression(), groupedBounds, nested.getTransformedStatesOfInterest());
				nested = new ModelExpressionTransformationNested<M, M, M>(nested, current);
			}
			
			prism.getLog().println("Transformed "+nested.getTransformedModel().getModelType()+": ");
			nested.getTransformedModel().printTransInfo(prism.getLog());
/*			try {
				prism.exportTransToFile(nested.getTransformedModel(), true, Prism.EXPORT_DOT_STATES, new java.io.File("t.dot"));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
			prism.getLog().println("Transformed expression: "+ nested.getTransformedExpression());
		}

		return nested;
	}

	@Override
	public void clear()
	{
		product.clear();
	}

}
