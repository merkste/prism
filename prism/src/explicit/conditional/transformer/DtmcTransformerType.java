package explicit.conditional.transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import prism.PrismException;

public enum DtmcTransformerType
{
	Quotient,
	Finally,
	Until,
	Next,
	Ltl;

	public static SortedSet<DtmcTransformerType> getValuesOf(final String specification) throws PrismException
	{
		final SortedSet<DtmcTransformerType> include = new TreeSet<>();
		final List<DtmcTransformerType> exclude = new ArrayList<>();

		for (String name : specification.split(",")) {
			if (name.equalsIgnoreCase("all")) {
				include.addAll(allValuesButQuotient());
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
		String specification = "all? ( -? (";
		for (DtmcTransformerType type : DtmcTransformerType.values()) {
			specification += type.name() + " | ";
		}
		specification = specification.substring(0, specification.length() - 3);
		specification += ")*";
		try {
			return "All, and " + getValuesOf("quotient,all") + " (preceeded by '-' for exlusion)";
		} catch (PrismException e) {
			// Generating a help text must not throw
			throw new RuntimeException(e);
		}
	}

	private static DtmcTransformerType valueOfIgnoringCase(final String name) throws PrismException
	{
		for (DtmcTransformerType type : DtmcTransformerType.values()) {
			if (name.equalsIgnoreCase(type.name())) {
				return type;
			}
		}
		throw new PrismException("Unknown DTMC transformer type: " + name);
	}

	private static List<DtmcTransformerType> allValuesButQuotient()
	{
		final List<DtmcTransformerType> all = new ArrayList<>();
		for (DtmcTransformerType type : DtmcTransformerType.values()) {
			if (type != Quotient) {
				all.add(type);
			}
		}
		return all;
	}
}