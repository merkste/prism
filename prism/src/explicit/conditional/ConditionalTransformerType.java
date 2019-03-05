package explicit.conditional;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import prism.PrismException;

// FIXME ALG: Rename to ConditionalTransform(ers/ations)
public enum ConditionalTransformerType
{
	Until,
	Next,
	Ltl,
	FinallyFinally,
	LtlFinally,
	FinallyLtl,
	LtlLtl,
	Quotient;

	public static SortedSet<ConditionalTransformerType> getValuesOf(final String specification) throws PrismException
	{
		final SortedSet<ConditionalTransformerType> include = new TreeSet<>();
		final List<ConditionalTransformerType> exclude = new ArrayList<>();

		for (String name : specification.split(",")) {
			if (name.equalsIgnoreCase("all")) {
				include.addAll(List.of(values()));
			} else if (name.equalsIgnoreCase("scale")) {
				include.addAll(scaleValues());
			} else if (name.equalsIgnoreCase("reset")) {
				include.addAll(resetValues());
			} else if (name.startsWith("-")) {
				exclude.add(valueOfIgnoringCase(name.substring(1)));
			} else {
				include.add(valueOfIgnoringCase(name));
			}
		}
		include.removeAll(exclude);
		return include;
	}

	public static String getSpecificationHelp()
	{
		try {
			return "Values: all, scale, reset and " + getValuesOf("all") + " (preceeded by '-' for exlusion)";
		} catch (PrismException e) {
			// Generating a help text must not throw
			throw new RuntimeException(e);
		}
	}

	private static ConditionalTransformerType valueOfIgnoringCase(final String name) throws PrismException
	{
		for (ConditionalTransformerType type : ConditionalTransformerType.values()) {
			if (name.equalsIgnoreCase(type.name())) {
				return type;
			}
		}
		throw new PrismException("Unknown transformer type: " + name);
	}

	/**
	 * Transformer types of the scale method.
	 * 
	 * @return List of scale patterns.
	 */
	private static List<ConditionalTransformerType> scaleValues()
	{
		return List.of(Until, Next, Ltl);
	}

	/**
	 * Transformer types of the reset method.
	 * 
	 * @return List of reset patterns.
	 */
	private static List<ConditionalTransformerType> resetValues()
	{
		return List.of(FinallyFinally, LtlFinally, FinallyLtl, LtlLtl);
	}
}