package prism.conditional;

import java.io.FileNotFoundException;

import jdd.JDD;
import jdd.JDDNode;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import prism.Model;
import prism.ModelExpressionTransformation;
import prism.ModelTransformation;
import prism.ModelTransformationNested;
import prism.NondetModel;
import prism.NondetModelChecker;
import prism.Prism;
import prism.PrismException;
import prism.PrismLangException;
import prism.ProbModel;
import prism.ProbModelChecker;
import prism.StateModelChecker;
import prism.conditional.transform.MDPGoalFailStopTransformer;
import prism.conditional.transform.MDPGoalFailStopTransformer.MDPGoalFailStopTransformation;

// FIXME ALG: add comment
public interface NewFinallyUntilTransformer<M extends Model, MC extends StateModelChecker> extends NewResetConditionalTransformer<M, MC>
{
	default boolean canHandle(Model model, ExpressionConditional expression)
			throws PrismLangException
	{
		return getNormalFormTransformer().canHandle(model, expression);
	}

	@Override
	default boolean canHandleObjective(M model, ExpressionConditional expression) throws PrismLangException
	{
		assert false : "Method should not be called, normally";
		return getNormalFormTransformer().canHandleObjective((NondetModel) model, expression);
	}

	@Override
	default boolean canHandleCondition(M model, ExpressionConditional expression) throws PrismLangException
	{
		assert false : "Method should not be called, normally";
		return getNormalFormTransformer().canHandleCondition((NondetModel) model, expression);
	}

	@Override
	default ModelExpressionTransformation<M, M> transform(M model, ExpressionConditional expression, JDDNode statesOfInterest)
			throws PrismException
	{
		checkCanHandle(model, expression);
//		try {
//			model.exportToFile(Prism.EXPORT_DOT, true, new java.io.File("original.dot"));
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

		// 1) Normal-Form Transformation
		MDPGoalFailStopTransformer transformer = getNormalFormTransformer();
		MDPGoalFailStopTransformation normalFormTransformation = transformer.transform((NondetModel) model, expression, statesOfInterest);
		M normalFormModel     = (M) normalFormTransformation.getTransformedModel();
//		try {
//			normalFormModel.exportToFile(Prism.EXPORT_DOT, true, new java.io.File("normal.dot"));
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

		// 2) Reset Transformation
//		//>>> Debug: print trap states
//		getLog().println("trapStates:");
//		JDD.PrintMinterms(getLog(), normalFormTransformation.getTrapStates());
//		new StateValuesMTBDD(normalFormTransformation.getTrapStates(), normalFormModel).print(getLog());
		JDDNode trapStates = normalFormTransformation.getTrapStates();
		JDDNode badStates = computeProb0E(normalFormModel, null, trapStates);
		JDD.Deref(trapStates);
//		//>>> Debug: print trap states
//		getLog().println("badStates:");
//		JDD.PrintMinterms(getLog(), badStates);
//		new StateValuesMTBDD(badStates.copy(), normalFormModel).print(getLog());
		JDDNode transformedStatesOfInterest = normalFormTransformation.getTransformedStatesOfInterest();
		// reset deterministic from states that do not satisfy the condition
		JDDNode unsatisfiedStates = normalFormTransformation.getConditonUnsatisfiedStates();
		// reset from fail state as well
		JDDNode failState = normalFormTransformation.getTransformedModel().getLabelDD(normalFormTransformation.getFailLabel());
		unsatisfiedStates = JDD.Or(unsatisfiedStates, failState.copy());
		ModelTransformation<M, ? extends M> resetTransformation = transformReset(normalFormModel, unsatisfiedStates, badStates, transformedStatesOfInterest);

//		try {
//			resetTransformation.getTransformedModel().exportToFile(Prism.EXPORT_DOT, true, new java.io.File("reset.dot"));
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

		// 3) Compose Transformations
		ModelTransformationNested<M, M, ? extends M> nested = new ModelTransformationNested<>((ModelTransformation<M,M>)normalFormTransformation, resetTransformation);

		return new ModelExpressionTransformation<M, M>()
		{
			@Override
			public M getOriginalModel()
			{
				return nested.getOriginalModel();
			}

			@Override
			public M getTransformedModel()
			{
				return nested.getTransformedModel();
			}

			@Override
			public void clear()
			{
				nested.clear();
			}

			@Override
			public prism.StateValues projectToOriginalModel(prism.StateValues svTransformedModel) throws PrismException
			{
				return nested.projectToOriginalModel(svTransformedModel);
			}

			@Override
			public JDDNode getTransformedStatesOfInterest()
			{
				return nested.getTransformedStatesOfInterest();
			}

			@Override
			public ExpressionProb getTransformedExpression()
			{
				// FIXME ALG: use ExpressionQuant for objective type
				return (ExpressionProb) normalFormTransformation.getTransformedExpression().getObjective();
			}

			@Override
			public Expression getOriginalExpression()
			{
				return normalFormTransformation.getOriginalExpression();
			}
		};
	}

	default JDDNode computeBadStates(M model, JDDNode objectiveGoal, JDDNode conditionRemain, JDDNode conditionGoal, boolean negated)
	{
		JDDNode badStates;
		if (negated) {
			// bad states == {s | Pmax=1[<> Condition]}
			badStates = computeProb1E(model, conditionRemain, conditionGoal);
		} else {
			// bad states == {s | Pmin=0[<> Condition]}
			badStates = computeProb0E(model, conditionRemain, conditionGoal);
		}
		// reduce number of choices, i.e.
		// - do not reset from goal states
		return JDD.And(badStates, JDD.Not(objectiveGoal.copy()));
	}

	default MDPGoalFailStopTransformation doNormalFormTransformation(M model,ExpressionConditional expression,JDDNode statesOfInterest)
			throws PrismException
	{
		return getNormalFormTransformer().transform((NondetModel)model, expression, statesOfInterest);
	}

	MDPGoalFailStopTransformer getNormalFormTransformer();



	public static abstract class DTMC extends NewResetConditionalTransformer.DTMC implements NewFinallyUntilTransformer<ProbModel, ProbModelChecker>
	{
		public DTMC(ProbModelChecker modelChecker)
		{
			super(modelChecker);
		}
	}



	public static class MDP extends NewResetConditionalTransformer.MDP implements NewFinallyUntilTransformer<NondetModel, NondetModelChecker>
	{
		public MDP(NondetModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public MDPGoalFailStopTransformer getNormalFormTransformer()
		{
			return new MDPGoalFailStopTransformer(modelChecker);
		}
	}
}
