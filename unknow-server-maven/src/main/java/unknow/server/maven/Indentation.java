package unknow.server.maven;

import org.apache.maven.plugins.annotations.Parameter;

import com.github.javaparser.printer.configuration.Indentation.IndentType;

/**
* Represents indentation configuration.
*/
public class Indentation {

	/** Type of indentation (e.g., SPACES or TABS). */
	@Parameter
	private IndentType type = IndentType.TABS;

	/** Number of spaces or tabs used per indentation level. */
	@Parameter
	private int value = 1;

	public Indentation() {
	}

	public Indentation(IndentType type, int value) {
		this.type = type;
		this.value = value;
	}

	public IndentType getType() {
		return type;
	}

	public void setType(IndentType type) {
		this.type = type;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return type + "(" + value + ")";
	}

	public com.github.javaparser.printer.configuration.Indentation toIndentation() {
		return new com.github.javaparser.printer.configuration.Indentation(type, value);
	}
}