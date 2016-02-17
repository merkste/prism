package explicit.modelviews;

import java.util.BitSet;

import explicit.Model;
import explicit.ReachabilityComputer;

public enum Restriction
{
	TRANSITIVE_CLOSURE {
		public BitSet getStateSet(final Model model, final BitSet states)
		{
			return new ReachabilityComputer(model).computeSuccStar(states);

		}
	},
	TRANSITIVE_CLOSURE_SAFE {
		public BitSet getStateSet(final Model model, final BitSet states)
		{
			assert states.equals(TRANSITIVE_CLOSURE.getStateSet(model, states));
			return states;

		}
	},
	STRICT;

	public BitSet getStateSet(final Model model, final BitSet states) {
		return states;
	};
}