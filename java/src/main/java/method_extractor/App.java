package method_extractor;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Vector;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class App {
	public static void main(String[] args) {
		String fileName = args[0];
		String indicesText = args[1];
		String stringArray[] = indicesText.split(",");

		int indices[] = new int[stringArray.length];
		for (int i = 0; i < stringArray.length; i++)
			indices[i] = Integer.parseInt(stringArray[i]);

		try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
			ArrayList<String> sb = new ArrayList<String>();
			String line = br.readLine();
			while (line != null) {
				sb.add(line);
				line = br.readLine();
			}
			String[] lines = new String[sb.size()];

			for (int i = 0; i < sb.size(); i++) {
				lines[i] = sb.get(i);
			}

			String codeSnippet = String.join("\n", lines);
			Map options = JavaCore.getOptions();
			options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_7); // or newer version
			ASTParser parser = ASTParser.newParser(AST.JLS8);
			parser.setSource(codeSnippet.toCharArray());
			parser.setKind(ASTParser.K_COMPILATION_UNIT);
			parser.setCompilerOptions(options);
			final CompilationUnit cu = (CompilationUnit) parser.createAST(null);
			Vector<ElemInfo> elems = null;
			int indicesLen = indices.length;
			// Print out in CSV format
			System.out.println("hunk_start,hunk_end,func_name,method_start,method_end");
			for (int index = 0; index < indicesLen; index += 2) {
				int startIndex = indices[index];
				int endIndex = indices[index + 1];
				elems = extractElements(cu, codeSnippet, lines, startIndex, endIndex);
				if (elems.size() > 0) {
					ElemInfo e = elems.get(elems.size() - 1);
					System.out.println(
							startIndex + "," + endIndex + "," + e.getName() + "," + e.getStart() + "," + e.getEnd());
				}
			}

		} catch (FileNotFoundException e) {

		} catch (IOException e) {

		}
	}

	private static Vector<ElemInfo> extractElements(CompilationUnit cu, String codeSnippet, String[] lines,
			int startIndex, int endIndex) {

		Vector<ElemInfo> elems = new Vector<ElemInfo>();

		cu.accept(new ASTVisitor() {
			public boolean visit(MethodDeclaration node) {
				SimpleName name = node.getName();
				int start = cu.getLineNumber(node.getStartPosition());
				int end = cu.getLineNumber(node.getStartPosition() + node.getLength());
				if (start > startIndex || end < endIndex) {
					return false;
				}
				elems.addElement(new ElemInfo(name.toString(), start, end, node));
				return true;
			}
		});

		return elems;
	}

}
