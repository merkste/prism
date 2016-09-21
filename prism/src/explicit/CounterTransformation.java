//==============================================================================
//	
//	Copyright (c) 2015-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================


package explicit;

import java.util.BitSet;
import java.util.List;

import explicit.rewards.ConstructRewards;
import explicit.rewards.MCRewards;
import explicit.rewards.MDPRewards;
import explicit.rewards.StateRewardsConstant;
import parser.ast.Expression;
import parser.ast.ExpressionReward;
import parser.ast.RewardStruct;
import parser.ast.TemporalOperatorBound;
import parser.visitor.ReplaceBound;
import prism.IntegerBound;
import prism.PrismException;

/**
 * A model-expression transformation that incorporates a conjunction of
 * time/step/reward bounds into a counter product. 
 * @param <M> the model type
 */
public class CounterTransformation<M extends Model> implements ModelExpressionTransformation<M, M> {
	/** The original expression */
	private Expression originalExpression;
	/** The transformed expression */
	private Expression transformedExpression;
	/** The original model */
	private M originalModel;
	/** The counter product (transformed model) */
	private CounterProduct<M> product;

	/** A model checker for the original model */
	private ProbModelChecker mc;

	/**
	 * The originalExpression will be modified!
	 * @param mc a model checker for the original model
	 * @param originalModel the original model
	 * @param originalExpression the original expression
	 * @param bound the temporal operator bound to be removed
	 * @param statesOfInterest the states of interest
	 */
	public CounterTransformation(ProbModelChecker mc,
			M originalModel, Expression originalExpression,
			TemporalOperatorBound bound,
			BitSet statesOfInterest) throws PrismException {
		this.originalModel = originalModel;
		this.originalExpression = originalExpression.deepCopy();
		this.mc = mc;

		transformedExpression = originalExpression;
		doTransformation(originalModel, bound, statesOfInterest);
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
	public BitSet getTransformedStatesOfInterest() {
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

	private void doTransformation(M originalModel, TemporalOperatorBound bound, BitSet statesOfInterest) throws PrismException {
		if (originalModel instanceof DTMC) {
			doTransformation((DTMC)originalModel, bound, statesOfInterest);
		} else if (originalModel instanceof MDP) {
				doTransformation((MDP)originalModel, bound, statesOfInterest);
		} else {
			throw new PrismException("Counter-Transformation is not supported for "+originalModel.getClass());
		}
	}
	
	private void doTransformation(DTMC originalModel, TemporalOperatorBound bound, BitSet statesOfInterest) throws PrismException
	{
		IntegerBound intBound = IntegerBound.fromTemporalOperatorBound(bound, mc.constantValues, true);
		int saturation_limit = intBound.getMaximalInterestingValue();

		MCRewards rewards = null;

		switch (bound.getBoundType()) {
		case REWARD_BOUND: {
			// Get reward info
			Object rs = bound.getRewardStructureIndex();
			RewardStruct rewStruct = ExpressionReward.getRewardStructByIndexObject(rs, mc.modulesFile, mc.constantValues);

			ConstructRewards construct_rewards = new ConstructRewards();
			rewards = construct_rewards.buildMCRewardStructure(originalModel, rewStruct, originalModel.getConstantValues());
			break;
		}
		case DEFAULT_BOUND:
		case STEP_BOUND:
		case TIME_BOUND:
			// a time/step bound, use constant reward structure assigning 1 to each state
			rewards = new StateRewardsConstant(1);
			break;
		}

		if (rewards == null) {
			throw new PrismException("Couldn't generate reward information.");
		}

		product = (CounterProduct<M>) CounterProduct.generate(originalModel, rewards, saturation_limit, statesOfInterest);

		// add 'in_bound-x' label
		BitSet statesInBound = product.getStatesWithAccumulatedRewardInBound(intBound);
		String labelInBound = ((DTMCExplicit)product.getTransformedModel()).addUniqueLabel("in_bound", statesInBound, mc.getDefinedLabelNames());

		// transform expression
		ReplaceBound replace = new ReplaceBound(bound, labelInBound);
		transformedExpression = (Expression) transformedExpression.accept(replace);

		if (!replace.wasSuccessfull()) {
			throw new PrismException("Replacing bound was not successfull.");
		}
	}

	private void doTransformation(MDP originalModel, TemporalOperatorBound bound, BitSet statesOfInterest) throws PrismException
	{
		IntegerBound intBound = IntegerBound.fromTemporalOperatorBound(bound, mc.constantValues, true);
		int saturation_limit = intBound.getMaximalInterestingValue();

		MDPRewards rewards = null;

		switch (bound.getBoundType()) {
		case REWARD_BOUND: {
			// Get reward info
			Object rs = bound.getRewardStructureIndex();
			RewardStruct rewStruct = ExpressionReward.getRewardStructByIndexObject(rs, mc.modulesFile, mc.constantValues);

			ConstructRewards construct_rewards = new ConstructRewards();
			rewards = construct_rewards.buildMDPRewardStructure(originalModel, rewStruct, originalModel.getConstantValues());
			break;
		}
		case DEFAULT_BOUND:
		case STEP_BOUND:
		case TIME_BOUND:
			// a time/step bound, use constant reward structure assigning 1 to each state
			rewards = new StateRewardsConstant(1);
			break;
		}

		if (rewards == null) {
			throw new PrismException("Couldn't generate reward information.");
		}

		product = (CounterProduct<M>) CounterProduct.generate(originalModel, rewards, saturation_limit, statesOfInterest);

		// add 'in_bound-x' label
		BitSet statesInBound = product.getStatesWithAccumulatedRewardInBound(intBound);
		String labelInBound = ((MDPExplicit)product.getTransformedModel()).addUniqueLabel("in_bound", statesInBound, mc.getDefinedLabelNames());

		// transform expression
		ReplaceBound replace = new ReplaceBound(bound, labelInBound);
		transformedExpression = (Expression) transformedExpression.accept(replace);

		if (!replace.wasSuccessfull()) {
			throw new PrismException("Replacing bound was not successfull.");
		}
	}


	public static <M extends Model> ModelExpressionTransformation<M, M> replaceBoundsWithCounters(ProbModelChecker parent,
			M originalModel, Expression originalExpression,
			List<TemporalOperatorBound> bounds,
			BitSet statesOfInterest) throws PrismException {

		if (bounds.isEmpty()) {
			throw new PrismException("No bounds to replace!");
		}
		
		if (!originalExpression.isSimplePathFormula()) {
			throw new PrismException("Replacing bounds is only supported in simple path formulas.");
		}

		// TODO: Check nesting depth = 1

		ModelExpressionTransformation<M, M> nested = null;
		for (TemporalOperatorBound bound : bounds) {
			parent.getLog().println("Transform DTMC to incorporate counter for bound '"+bound+"'");
			
			ModelExpressionTransformation<M, M> current;
			
			if (nested == null) {
				current = new CounterTransformation<M>(parent, originalModel, originalExpression, bound, statesOfInterest);
				nested = current;
			} else {
				current = new CounterTransformation<M>(parent, nested.getTransformedModel(), nested.getTransformedExpression(), bound, nested.getTransformedStatesOfInterest());
				nested = new ModelExpressionTransformationNested<M, M, M>(nested, current);
			}
			
			parent.getLog().println("Transformed DTMC: "+nested.getTransformedModel().infoString());
			parent.getLog().println("Transformed expression: "+nested.getTransformedExpression());
		}

		return nested;
	}

}
