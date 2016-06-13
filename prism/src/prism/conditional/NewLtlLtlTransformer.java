package prism.conditional;

import jdd.JDD;
import jdd.JDDNode;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import prism.Model;
import prism.ModelExpressionTransformation;
import prism.ModelTransformation;
import prism.ModelTransformationNested;
import prism.NondetModel;
import prism.NondetModelChecker;
import prism.PrismException;
import prism.PrismLangException;
import prism.ProbModel;
import prism.ProbModelChecker;
import prism.StateModelChecker;
import prism.conditional.transform.BasicModelExpressionTransformation;
import prism.conditional.transform.FailStopTransformer;
import prism.conditional.transform.GoalFailStopTransformer.NormalFormTransformation;



// FIXME ALG: add comment
public interface NewLtlLtlTransformer<M extends ProbModel, MC extends StateModelChecker> extends NewResetConditionalTransformer<M, MC>
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
		return getNormalFormTransformer().canHandleObjective(model, expression);
	}

	@Override
	default boolean canHandleCondition(M model, ExpressionConditional expression) throws PrismLangException
	{
		assert false : "Method should not be called, normally";
		return getNormalFormTransformer().canHandleCondition(model, expression);
	}

	@Override
	default ModelExpressionTransformation<M, M> transform(M model, ExpressionConditional expression, JDDNode statesOfInterest)
			throws PrismException
	{
		checkCanHandle(model, expression);

//try {
//	model.exportToFile(Prism.EXPORT_DOT, true, new java.io.File("original_sym.dot"));
//} catch (FileNotFoundException e) {
//	// TODO Auto-generated catch block
//	e.printStackTrace();
//}

		// 1) Normal-Form Transformation
		FailStopTransformer<M, MC> transformer               = getNormalFormTransformer();
		NormalFormTransformation<M> normalFormTransformation = transformer.transform(model, expression, statesOfInterest);
		M normalFormModel                                    = normalFormTransformation.getTransformedModel();
		getLog().println("Normal-form transformation: " + normalFormTransformation.getTransformedExpression());

//try {
//	normalFormModel.exportToFile(Prism.EXPORT_DOT, true, new java.io.File("normal_sym.dot"));
//} catch (FileNotFoundException e) {
//	// TODO Auto-generated catch block
//	e.printStackTrace();
//}

		// 2) Reset Transformation
// FIXME ALG: Do not reset from normal-form-states
		JDDNode badStates = computeResetStates(normalFormTransformation);

////>>> Debug: print badStates
//getLog().println("badStates:");
//new StateValuesMTBDD(badStates.copy(), normalFormModel).print(getLog());
//		// reset deterministic from states that do not satisfy the condition Should already happen in goal-fail-stop transformation
//		JDDNode unsatisfiedStates = normalFormTransformation.getConditonUnsatisfiedStates();
////>>> Debug: print unsatisfiedStates
//getLog().println("unsatisfiedStates:");
//new StateValuesMTBDD(unsatisfiedStates.copy(), normalFormModel).print(getLog());
		// reset from fail state as well
////>>> Debug: print goalState
//JDDNode goalState = normalFormModel.getLabelDD(normalFormTransformation.getGoalLabel());
//getLog().println("goalState:");
//new StateValuesMTBDD(goalState.copy(), normalFormModel).print(getLog());
////>>> Debug: print failState
//getLog().println("failState:");
//new StateValuesMTBDD(failState.copy(), normalFormModel).print(getLog());
////>>> Debug: print StopState
//JDDNode stopState = normalFormModel.getLabelDD(normalFormTransformation.getStopLabel());
//getLog().println("stopState:");
//new StateValuesMTBDD(stopState.copy(), normalFormModel).print(getLog());
		JDDNode transformedStatesOfInterest                     = normalFormTransformation.getTransformedStatesOfInterest();
		ModelTransformation<M, ? extends M> resetTransformation = transformReset(normalFormModel, badStates, transformedStatesOfInterest);

//try {
//	resetTransformation.getTransformedModel().exportToFile(Prism.EXPORT_DOT, true, new java.io.File("reset_sym.dot"));
//} catch (FileNotFoundException e) {
//	// TODO Auto-generated catch block
//	e.printStackTrace();
//}

		// transform expression
		Expression originalExpression = normalFormTransformation.getOriginalExpression();
		Expression transformedExpression = normalFormTransformation.getTransformedExpression().getObjective();
		
		
		// 3) Compose Transformations
		ModelTransformationNested<M, M, ? extends M> nested = new ModelTransformationNested<>(normalFormTransformation, resetTransformation);
		
		return new BasicModelExpressionTransformation<>(nested, originalExpression, transformedExpression);
	}

	default JDDNode computeResetStates(NormalFormTransformation<M> transformation)
	{
		JDDNode badStates = transformation.getTransformedModel().getLabelDD(transformation.getBadLabel());
		JDDNode failState = transformation.getTransformedModel().getLabelDD(transformation.getFailLabel());
		return JDD.Or(badStates.copy(), failState.copy());
	}

	FailStopTransformer<M, MC> getNormalFormTransformer();




	public static class DTMC extends NewResetConditionalTransformer.DTMC implements NewLtlLtlTransformer<ProbModel, ProbModelChecker>
	{
		public DTMC(ProbModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public FailStopTransformer.DTMC getNormalFormTransformer()
		{
			return new FailStopTransformer.DTMC(modelChecker);
		}
	}



	public static class MDP extends NewResetConditionalTransformer.MDP implements NewLtlLtlTransformer<NondetModel, NondetModelChecker>
	{
		public MDP(NondetModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public FailStopTransformer.MDP getNormalFormTransformer()
		{
			return new FailStopTransformer.MDP(modelChecker);
		}
	}
}
