package explicit.conditional.transformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import prism.PrismException;

public enum MdpTransformerType
{
	FinallyFinally,
	LtlFinally,
	FinallyLtl,
	LtlLtl;

	public static SortedSet<MdpTransformerType> getValuesOf(final String specification) throws PrismException
	{
		final SortedSet<MdpTransformerType> include = new TreeSet<>();
		final List<MdpTransformerType> exclude = new ArrayList<>();

		for (String name : specification.split(",")) {
			if (name.equalsIgnoreCase("all")) {
				include.addAll(Arrays.asList(values()));
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
		for (MdpTransformerType type : MdpTransformerType.values()) {
			specification += type.name() + " | ";
		}
		specification = specification.substring(0, specification.length() - 3);
		specification += ")*";
		try {
			return "All, and " + getValuesOf("all") + " (preceeded by '-' for exlusion)";
		} catch (PrismException e) {
			// Generating a help text must not throw
			throw new RuntimeException(e);
		}
	}

	private static MdpTransformerType valueOfIgnoringCase(final String name) throws PrismException
	{
		for (MdpTransformerType type : MdpTransformerType.values()) {
			if (name.equalsIgnoreCase(type.name())) {
				return type;
			}
		}
		throw new PrismException("Unknown MDP transformer type: " + name);
	}
}