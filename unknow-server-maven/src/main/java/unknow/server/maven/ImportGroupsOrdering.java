package unknow.server.maven;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.printer.configuration.ImportOrderingStrategy;

/**
 * Defines strategies used when ordering import statements.
 */
public class ImportGroupsOrdering implements ImportOrderingStrategy {
	private static final Comparator<ImportDeclaration> SORT = Comparator.comparing(NodeWithName::getNameAsString);

	private boolean sortImportsAlphabetically = false;

	private final String[][] groups;

	public ImportGroupsOrdering(List<String> groups) {
		this.groups = new String[groups.size()][];
		int i = 0;
		for (String s : groups)
			this.groups[i++] = s.split("[, ]+");
	}

	@Override
	public List<NodeList<ImportDeclaration>> sortImports(NodeList<ImportDeclaration> nodes) {
		@SuppressWarnings("unchecked")
		NodeList<ImportDeclaration>[] imports = new NodeList[groups.length + 2];
		for (int i = 0; i < imports.length; i++)
			imports[i] = new NodeList<>();
		int other = imports.length - 1;

		loop: for (ImportDeclaration importDeclaration : nodes) {
			// Check if is a static import
			if (importDeclaration.isStatic()) {
				imports[0].add(importDeclaration);
				continue;
			}
			String importName = importDeclaration.getNameAsString();

			for (int i = 0; i < groups.length; i++) {
				if (match(importName, groups[i])) {
					imports[i + 1].add(importDeclaration);
					continue loop;
				}
			}
			imports[other].add(importDeclaration);
		}
		if (sortImportsAlphabetically) {
			for (int i = 0; i < imports.length; i++)
				imports[i].sort(SORT);
		}
		return Arrays.asList(imports);
	}

	private boolean match(String importName, String[] group) {
		for (int i = 0; i < group.length; i++) {
			if (importName.startsWith(group[i]))
				return true;
		}
		return false;
	}

	@Override
	public void setSortImportsAlphabetically(boolean sortAlphabetically) {
		sortImportsAlphabetically = sortAlphabetically;
	}

	@Override
	public boolean isSortImportsAlphabetically() {
		return sortImportsAlphabetically;
	}
}