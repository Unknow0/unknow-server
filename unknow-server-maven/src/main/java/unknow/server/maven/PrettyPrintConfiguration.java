package unknow.server.maven;

import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugins.annotations.Parameter;

import com.github.javaparser.printer.configuration.DefaultConfigurationOption;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration.ConfigOption;
import com.github.javaparser.printer.configuration.Indentation.IndentType;
import com.github.javaparser.printer.configuration.PrinterConfiguration;

/**
 * Configuration du formateur de code (Pretty Printer).
 *
 * <p>Chaque option correspond à une valeur de l'enum {@code ConfigOption}.
 * Les paramètres définis ici peuvent être configurés directement dans le pom.xml sous le tag
 * {@code <prettyPrintConfiguration>}.</p>
 */
public class PrettyPrintConfiguration {

	/**
	 * Order imports alphabetically.
	 */
	@Parameter(defaultValue = "true")
	private boolean orderImports;

	/**
	 * The list of package prefix to group together to use (default ["java.", "javax.", "org.", "com."])
	 */
	@Parameter
	private List<String> orderImportGroups = Arrays.asList("java.", "javax.", "org.", "com.");

	/**
	 * Print comments only. It can be combined with {@code PRINT_JAVADOC} to print regular comments and javadoc.
	 */
	@Parameter(defaultValue = "true")
	private boolean printComments;

	/**
	 * Print javadoc comments only. It can be combined with {@code PRINT_COMMENTS} to print regular javadoc and comments.
	 */
	@Parameter(defaultValue = "true")
	private boolean printJavadoc;

	/**
	 * Insert spaces around operators.
	 */
	@Parameter(defaultValue = "true")
	private boolean spaceAroundOperators;

	/**
	 * Align method parameters in columns.
	 */
	@Parameter(defaultValue = "false")
	private boolean columnAlignParameters;

	/**
	 * Align the first element of a method chain.
	 */
	@Parameter(defaultValue = "true")
	private boolean columnAlignFirstMethodChain;

	/**
	 * Indent the case when it is true, don't if false
	 * <pre>{@code
	 * switch(x) {            switch(x) {
	 *    case 1:             case 1:
	 *        return y;           return y;
	 *    case 2:             case 2:
	 *        return z;           return x;
	 * }                       }
	 * }<pre>
	 */
	@Parameter(defaultValue = "true")
	private boolean indentCaseInSwitch;

	/**
	* By default enum constants get aligned like this:
	* <pre>{@code
	*     enum X {
	*        A, B, C, D
	*     }
	* }<pre>
	* until the amount of constants passes this currentValue (5 by default).
	* Then they get aligned like this:
	* <pre>{@code
	*     enum X {
	*        A,
	*        B,
	*        C,
	*        D,
	*        E,
	*        F,
	*        G
	*     }
	* }</pre>
	* Set it to a very large number (e.g. {@code Integer.MAX_VALUE} to always align horizontally.
	* Set it to 1 or less to always align vertically.
	*/
	@Parameter(defaultValue = "5")
	private int maxEnumConstantsToAlignHorizontally;

	/**
	 * The end-of-line character used when printing code.  Default is system-dependent.
	 */
	@Parameter
	private String endOfLineCharacter = System.getProperty("line.separator");

	/**
	 * Indentation property.
	 */
	@Parameter
	private Indentation indentation = new Indentation(IndentType.SPACES, 4);

	public boolean isOrderImports() {
		return orderImports;
	}

	public void setOrderImports(boolean orderImports) {
		this.orderImports = orderImports;
	}

	public List<String> getOrderImportGroups() {
		return orderImportGroups;
	}

	public void setOrderImportGroups(List<String> orderImportGroups) {
		this.orderImportGroups = orderImportGroups;
	}

	public boolean isPrintComments() {
		return printComments;
	}

	public void setPrintComments(boolean printComments) {
		this.printComments = printComments;
	}

	public boolean isPrintJavadoc() {
		return printJavadoc;
	}

	public void setPrintJavadoc(boolean printJavadoc) {
		this.printJavadoc = printJavadoc;
	}

	public boolean isSpaceAroundOperators() {
		return spaceAroundOperators;
	}

	public void setSpaceAroundOperators(boolean spaceAroundOperators) {
		this.spaceAroundOperators = spaceAroundOperators;
	}

	public boolean isColumnAlignParameters() {
		return columnAlignParameters;
	}

	public void setColumnAlignParameters(boolean columnAlignParameters) {
		this.columnAlignParameters = columnAlignParameters;
	}

	public boolean isColumnAlignFirstMethodChain() {
		return columnAlignFirstMethodChain;
	}

	public void setColumnAlignFirstMethodChain(boolean columnAlignFirstMethodChain) {
		this.columnAlignFirstMethodChain = columnAlignFirstMethodChain;
	}

	public boolean isIndentCaseInSwitch() {
		return indentCaseInSwitch;
	}

	public void setIndentCaseInSwitch(boolean indentCaseInSwitch) {
		this.indentCaseInSwitch = indentCaseInSwitch;
	}

	public int getMaxEnumConstantsToAlignHorizontally() {
		return maxEnumConstantsToAlignHorizontally;
	}

	public void setMaxEnumConstantsToAlignHorizontally(int maxEnumConstantsToAlignHorizontally) {
		this.maxEnumConstantsToAlignHorizontally = maxEnumConstantsToAlignHorizontally;
	}

	public String getEndOfLineCharacter() {
		return endOfLineCharacter;
	}

	public void setEndOfLineCharacter(String endOfLineCharacter) {
		this.endOfLineCharacter = endOfLineCharacter;
	}

	public Indentation getIndentation() {
		return indentation;
	}

	public void setIndentation(Indentation indentation) {
		this.indentation = indentation;
	}

	public PrinterConfiguration toPrinterConfiguration() {
		PrinterConfiguration config = new DefaultPrinterConfiguration().addOption(new DefaultConfigurationOption(ConfigOption.ORDER_IMPORTS, orderImports))
				.addOption(new DefaultConfigurationOption(ConfigOption.SORT_IMPORTS_STRATEGY, new ImportGroupsOrdering(orderImportGroups)))
				.addOption(new DefaultConfigurationOption(ConfigOption.MAX_ENUM_CONSTANTS_TO_ALIGN_HORIZONTALLY, maxEnumConstantsToAlignHorizontally))
				.addOption(new DefaultConfigurationOption(ConfigOption.END_OF_LINE_CHARACTER, endOfLineCharacter))
				.addOption(new DefaultConfigurationOption(ConfigOption.INDENTATION, indentation.toIndentation()));
		if (indentCaseInSwitch)
			config.addOption(new DefaultConfigurationOption(ConfigOption.INDENT_CASE_IN_SWITCH));
		if (spaceAroundOperators)
			config.addOption(new DefaultConfigurationOption(ConfigOption.SPACE_AROUND_OPERATORS));
		if (columnAlignFirstMethodChain)
			config.addOption(new DefaultConfigurationOption(ConfigOption.COLUMN_ALIGN_FIRST_METHOD_CHAIN));
		if (printComments)
			config.addOption(new DefaultConfigurationOption(ConfigOption.PRINT_COMMENTS));
		if (printJavadoc)
			config.addOption(new DefaultConfigurationOption(ConfigOption.PRINT_JAVADOC));
		if (columnAlignParameters)
			config.addOption(new DefaultConfigurationOption(ConfigOption.COLUMN_ALIGN_PARAMETERS));
		return config;

	}
}
