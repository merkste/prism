package explicit.conditional.transformer.mdp;

import java.util.BitSet;

import explicit.BasicModelTransformation;
import explicit.MDP;
import explicit.ModelTransformation;
import explicit.conditional.transformer.ReachabilityTransformation;

//FIXME ALG: add comment
public class ConditionalMDPTransformation extends BasicModelTransformation<MDP, MDP> implements ReachabilityTransformation<MDP, MDP>
{
	private final BitSet goalStates;

	public ConditionalMDPTransformation(final MDP originalModel, final MDP transformedModel, final Integer[] mapping, final BitSet goalStates)
	{
		super(originalModel, transformedModel, mapping);
		this.goalStates = goalStates;
	}

	public ConditionalMDPTransformation(final ModelTransformation<MDP, MDP> transformation, final BitSet goalStates)
	{
		super(transformation);
		this.goalStates = goalStates;
	}

	@Override
	public BitSet getGoalStates()
	{
		return goalStates;
	}
}