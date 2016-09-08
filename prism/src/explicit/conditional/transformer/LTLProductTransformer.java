package explicit.conditional.transformer;

import java.util.BitSet;
import java.util.Vector;

import parser.ast.Expression;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismLangException;
import acceptance.AcceptanceOmega;
import acceptance.AcceptanceReach;
import acceptance.AcceptanceType;
import automata.DA;
import common.BitSetTools;
import explicit.DTMC;
import explicit.LTLModelChecker;
import explicit.MDP;
import explicit.Model;
import explicit.LTLModelChecker.LTLProduct;
import explicit.ProbModelChecker;

// FIXME ALG: add comment
public class LTLProductTransformer<M extends Model> extends PrismComponent
{
	private final ProbModelChecker modelChecker;
	private final LTLModelChecker ltlModelChecker;

	public LTLProductTransformer(final ProbModelChecker modelChecker)
	{
		super(modelChecker);
		this.modelChecker = modelChecker;
		ltlModelChecker = new LTLModelChecker(this);
	}

	public boolean canHandle(final Model model, final Expression expression)
			throws PrismLangException
	{
		return LTLModelChecker.isSupportedLTLFormula(model.getModelType(), expression);
	}

	public LTLProduct<M> transform(final M model, final Expression expression, final BitSet statesOfInterest, final AcceptanceType... acceptanceTypes)
			throws PrismException
	{
		final LabeledDA labeledDA = constructDA(model, expression, acceptanceTypes);
		return constructProduct(model, labeledDA, statesOfInterest);
	}

	public LabeledDA constructDA(final M model, final Expression expression, final AcceptanceType...acceptanceTypes)
			throws PrismException
	{
		Vector<BitSet> labels                   = new Vector<BitSet>();
		DA<BitSet,? extends AcceptanceOmega> da = null;

		if (Expression.isCoSafeLTLSyntactic(expression, true)) {
			boolean containsRabin = false, containsReach = false;
			for (AcceptanceType type : acceptanceTypes) {
				containsRabin |= type == AcceptanceType.RABIN;
				containsReach |= type == AcceptanceType.REACH;
			}
			if (containsReach) {
				getLog().print("\n[" + expression + "] is co-safe, attempting to construct acceptance REACH ... ");
				da = ltlModelChecker.constructDAForLTLFormula(modelChecker, model, expression, labels, AcceptanceType.REACH, AcceptanceType.RABIN);
				if (da.getAcceptance().getType() == AcceptanceType.REACH) {
					getLog().println("Success.");
				} else if (containsRabin){
					getLog().println("Failed. Falling back to acceptance RABIN.");
				} else {
					getLog().println("Failed. Falling back to other acceptance types and build new automaton.");
					da     = null;
					labels.clear();
				}
			}
		}
		// Either formula is not co-safe or construction of acceptance REACH failed and RABIN is not allowed.
		if (da == null) {
			da = ltlModelChecker.constructDAForLTLFormula(modelChecker, model, expression, labels, acceptanceTypes);
		}

		return new LabeledDA(da, labels);
	}

	@SuppressWarnings("unchecked")
	public LTLProduct<M> constructProduct(final M model, final LabeledDA labeledDA, final BitSet statesOfInterest)
			throws PrismException
	{
		final DA<BitSet, ? extends AcceptanceOmega> automaton = labeledDA.getAutomaton();
		final Vector<BitSet> labels = labeledDA.getLabels();

		mainLog.println("\nConstructing " + model.getModelType() + "-" + automaton.getAutomataType() + " product...");
		final LTLProduct<M> product;
		if (model instanceof DTMC) {
			product = (LTLProduct<M>) ltlModelChecker.constructProductModel(automaton, (DTMC)model, labels, statesOfInterest);
		} else if (model instanceof MDP) {
			product = (LTLProduct<M>) ltlModelChecker.constructProductModel(automaton, (MDP)model, labels, statesOfInterest);
		} else {
			throw new PrismException("Unsupported model type " + model.getClass());
		}
		mainLog.print("\n" + product.getProductModel().infoStringTable());
		return product;
	}

	public BitSet findAcceptingStates(final LTLProduct<M> product)
			throws PrismException
	{
		return findAcceptingStates(product, null);
	}

	public BitSet findAcceptingStates(final M productModel, final AcceptanceOmega acceptance)
			throws PrismException
	{
		return findAcceptingStates(productModel, acceptance, null);
	}

	public BitSet findAcceptingStates(final LTLProduct<M> product, final BitSet restrict)
			throws PrismException
	{
		return findAcceptingStates(product.getProductModel(), product.getAcceptance(), restrict);
	}

	protected BitSet findAcceptingStates(final M productModel, final AcceptanceOmega acceptance, final BitSet restrict)
			throws PrismException
	{
		if (acceptance.getType() == AcceptanceType.REACH) {
			BitSet goalStates = ((AcceptanceReach) acceptance).getGoalStates();
			return (restrict == null) ? goalStates : BitSetTools.intersect(goalStates, restrict);
		}
		if (productModel instanceof DTMC) {
			return ltlModelChecker.findAcceptingBSCCs((DTMC) productModel, acceptance, restrict);
		}
		if (productModel instanceof MDP) {
			return ltlModelChecker.findAcceptingECStates((MDP) productModel, acceptance, restrict);
		}
		throw new PrismException("Unsupported product model type: " + productModel.getClass());
	}



	public static class LabeledDA
	{
		final DA<BitSet, ? extends AcceptanceOmega> automaton;
		final Vector<BitSet> labels;

		public LabeledDA(DA<BitSet, ? extends AcceptanceOmega> automaton, Vector<BitSet> labels)
		{
			this.automaton = automaton;
			this.labels = labels;
		}

		public DA<BitSet, ? extends AcceptanceOmega> getAutomaton()
		{
			return automaton;
		}

		public Vector<BitSet> getLabels()
		{
			return labels;
		}

		public LabeledDA liftToProduct(LTLProduct<? extends Model> product)
		{
			Vector<BitSet> lifted = new Vector<BitSet>(labels.size());
			for (BitSet label : labels) {
				lifted.add(product.liftFromModel(label));
			}
			return new LabeledDA(automaton, lifted);
		}
	}
}