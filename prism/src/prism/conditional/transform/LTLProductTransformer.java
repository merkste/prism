package prism.conditional.transform;

import java.util.BitSet;
import java.util.Vector;

import parser.ast.Expression;
import prism.ECComputerDefault;
import prism.LTLModelChecker;
import prism.LTLModelChecker.LTLProduct;
import prism.Model;
import prism.NondetModel;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismLangException;
import prism.ProbModel;
import prism.StateModelChecker;
import acceptance.AcceptanceOmega;
import acceptance.AcceptanceOmegaDD;
import acceptance.AcceptanceReachDD;
import acceptance.AcceptanceType;
import automata.DA;
import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import mtbdd.PrismMTBDD;

// FIXME ALG: add comment
public class LTLProductTransformer<M extends Model> extends PrismComponent
{
	private final StateModelChecker modelChecker;
	private final LTLModelChecker ltlModelChecker;

	public LTLProductTransformer(final StateModelChecker modelChecker)
	{
		super(modelChecker);
		this.modelChecker = modelChecker;
		ltlModelChecker = new LTLModelChecker(this);
	}

	public StateModelChecker getModelChecker(M model) throws PrismException
	{
		// Create fresh model checker for model
		return (StateModelChecker) modelChecker.createModelChecker(model);
	}

	public boolean canHandle(final Model model, final Expression expression)
			throws PrismLangException
	{
		return LTLModelChecker.isSupportedLTLFormula(model.getModelType(), expression);
	}

	public LTLProduct<M> transform(final M model, final Expression expression, final JDDNode statesOfInterest, final AcceptanceType... acceptanceTypes)
			throws PrismException
	{
		final LabeledDA labeledDA = constructDA(model, expression, acceptanceTypes);
		return constructProduct(model, labeledDA, statesOfInterest);
	}

	public LabeledDA constructDA(final M model, final Expression expression, final AcceptanceType...acceptanceTypes)
			throws PrismException
	{
		StateModelChecker mc                    = getModelChecker(model);
		Vector<JDDNode> labels                  = new Vector<JDDNode>();
		DA<BitSet,? extends AcceptanceOmega> da = null;

		if (Expression.isCoSafeLTLSyntactic(expression, true)) {
			boolean containsRabin = false, containsReach = false;
			for (AcceptanceType type : acceptanceTypes) {
				containsRabin |= type == AcceptanceType.RABIN;
				containsReach |= type == AcceptanceType.REACH;
			}
			if (containsReach) {
				getLog().print("\n[" + expression + "] is co-safe, attempting to construct acceptance REACH ... ");
				da = ltlModelChecker.constructDAForLTLFormula(mc, model, expression, labels, AcceptanceType.REACH, AcceptanceType.RABIN);
				if (da.getAcceptance().getType() == AcceptanceType.REACH) {
					getLog().println("Success.");
				} else if (containsRabin){
					getLog().println("Failed. Falling back to acceptance RABIN.");
				} else {
					getLog().println("Failed. Falling back to other acceptance types and build new automaton.");
					da     = null;
					for (JDDNode label : labels) {
						JDD.Deref(label);
					}
					labels.clear();
				}
			}
		}
		// Either formula is not co-safe or construction of acceptance REACH failed and RABIN is not allowed.
		if (da == null) {
			da = ltlModelChecker.constructDAForLTLFormula(mc, model, expression, labels, acceptanceTypes);
		}

		return new LabeledDA(da, labels);
	}

	@SuppressWarnings("unchecked")
	public LTLProduct<M> constructProduct(final M model, final LabeledDA labeledDA, final JDDNode statesOfInterest)
			throws PrismException
	{
		final DA<BitSet, ? extends AcceptanceOmega> automaton = labeledDA.getAutomaton();
		final Vector<JDDNode> labels = labeledDA.getLabels();

		mainLog.println("\nConstructing " + model.getModelType() + "-" + automaton.getAutomataType() + " product...");
		final LTLProduct<M> product;
		if (model instanceof NondetModel) {
			product = (LTLProduct<M>) ltlModelChecker.constructProductMDP((NondetModel)model, automaton, labels, statesOfInterest);
		} else if (model instanceof ProbModel) {
			product = (LTLProduct<M>) ltlModelChecker.constructProductMC((ProbModel)model, automaton, labels, statesOfInterest);
		} else {
			throw new PrismException("Unsupported model type " + model.getClass());
		}
		mainLog.println();
		product.getProductModel().printTransInfo(mainLog, false);
		return product;
	}

	public JDDNode findAcceptingStates(final LTLProduct<M> product)
			throws PrismException
	{
		return findAcceptingStates(product, null);
	}

	public JDDNode findAcceptingStates(final M productModel, final AcceptanceOmegaDD acceptance)
			throws PrismException
	{
		return findAcceptingStates(productModel, acceptance, null, true);
	}

	public JDDNode findAcceptingStates(final LTLProduct<M> product, final JDDNode remain)
			throws PrismException
	{
		return findAcceptingStates(product.getProductModel(), product.getAcceptance(), remain, true);
	}

	public JDDNode findAcceptingStates(final LTLProduct<M> product, final JDDNode remain, boolean alwaysRemain)
			throws PrismException
	{
		return findAcceptingStates(product.getProductModel(), product.getAcceptance(), remain, alwaysRemain);
	}

	public JDDNode findAcceptingStates(final M productModel, final AcceptanceOmegaDD acceptance, JDDNode remain, boolean alwaysRemain)
			throws PrismException
	{
		if (acceptance.getType() == AcceptanceType.REACH) {
			return findAcceptingStates(productModel, (AcceptanceReachDD) acceptance, remain, alwaysRemain);
		}
		if (productModel instanceof NondetModel) {
			return ltlModelChecker.findAcceptingECStates(acceptance, (NondetModel) productModel, null, null, false, remain);
		}
		if (productModel instanceof ProbModel) {
			return ltlModelChecker.findAcceptingBSCCs(acceptance, (ProbModel) productModel, remain);
		}
		throw new PrismException("Unsupported product model type: " + productModel.getClass());
	}

	public JDDNode findAcceptingStates(final M productModel, final AcceptanceReachDD acceptance, JDDNode remain, boolean alwaysRemain)
			throws PrismException
	{
		// if restrict is null we allow all reachable states
		JDDNode goalStates      = ((AcceptanceReachDD) acceptance).getGoalStates();
		remain                  = remain == null ? productModel.getReach().copy() : JDD.And(remain.copy(), productModel.getReach().copy());
		JDDNode acceptingStates = JDD.And(goalStates, remain);
		if (! alwaysRemain) {
			return acceptingStates;
		}
		if (productModel instanceof NondetModel) {
			ECComputerDefault ecComputer = new ECComputerDefault(this, productModel.getReach(), productModel.getTrans(), productModel.getTrans01(), productModel.getAllDDRowVars(), productModel.getAllDDColVars(), ((NondetModel) productModel).getAllDDNondetVars());
			return ecComputer.findMaximalStableSet(acceptingStates);
//			ECComputer ecComputer = ECComputer.createECComputer(this, (NondetModel) productModel);
//			ecComputer.computeMECStates(acceptingStates);
//			JDD.Deref(acceptingStates);
//			return ecComputer.getMECStates().stream().reduce(JDD.Constant(0), JDD::Or);
		}
		if (productModel instanceof ProbModel) {
//			Prob1 (G acceptingStates) = Prob0 (F !acceptingStates)
			JDDNode trans01            = productModel.getTrans01();
			JDDNode reach              = productModel.getReach();
			JDDVars rowVars            = productModel.getAllDDRowVars();
			JDDVars colVars            = productModel.getAllDDColVars();
			JDDNode nonAcceptingStates = JDD.And(reach.copy(), JDD.Not(acceptingStates.copy()));
			JDDNode result             = PrismMTBDD.Prob0(trans01, reach, rowVars, colVars, acceptingStates, nonAcceptingStates);
			JDD.Deref(acceptingStates, nonAcceptingStates);
			return result;
//			SCCComputer sccComputer = SCCComputer.createSCCComputer(this, (ProbModel) productModel);
//			sccComputer.computeBSCCs();
//			List<JDDNode> bsccs = sccComputer.getBSCCs();
//			JDDNode result = bsccs.stream().filter(bscc -> JDD.IsContainedIn(bscc, acceptingStates)).reduce(JDD.Constant(0), JDD::Or);
//			JDD.Deref(acceptingStates);
//			return result;
		}
		throw new PrismException("Unsupported product model type: " + productModel.getClass());
	}



	public static class LabeledDA implements Cloneable
	{
		final DA<BitSet, ? extends AcceptanceOmega> automaton;
		Vector<JDDNode> labels;

		public LabeledDA(DA<BitSet, ? extends AcceptanceOmega> automaton, Vector<JDDNode> labels)
		{
			this.automaton = automaton;
			this.labels = labels;
		}

		public void clear()
		{
			for (JDDNode label : labels) {
				JDD.Deref(label);
			}
			labels.clear();
		}

		@Override
		public LabeledDA clone()
		{
			try {
				LabeledDA clone = (LabeledDA) super.clone();
				clone.labels    = new Vector<>(labels.size());
				for (JDDNode label : labels) {
					clone.labels.add(label.copy());
				}
				return clone;
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
				throw new RuntimeException("Object#clone is expected to work for Cloneable objects.", e);
			}
		}

		public LabeledDA copy()
		{
			return clone();
		}

		public DA<BitSet, ? extends AcceptanceOmega> getAutomaton()
		{
			return automaton;
		}

		public Vector<JDDNode> getLabels()
		{
			return labels;
		}

		public LabeledDA liftToProduct(LTLProduct<? extends Model> product)
		{
//			JDDNode reach = product.getProductModel().getReach();
			Vector<JDDNode> lifted = new Vector<JDDNode>(labels.size());
			for (JDDNode label : labels) {
				// Limit label to reachable states in product.
//				lifted.add(JDD.And(label.copy(), reach.copy());
				// Just reference the labels.
				lifted.add(label.copy());
			}
			return new LabeledDA(automaton, lifted);
		}
	}
}