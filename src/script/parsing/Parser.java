/**
 * 
 */
package script.parsing;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import logic.iterators.LineIterator;
import logic.iterators.UnderlyingIOException;
import gui.style.Stylesheet;
import gui.style.StylesheetBackgroundColorElement;
import gui.style.StylesheetBorderElement;
import gui.style.StylesheetFontSizeElement;
import gui.style.StylesheetMarginElement;
import gui.style.StylesheetPaddingElement;
import gui.style.StylesheetProperty;
import gui.style.Stylesheets;
import gui.style.dimensions.StylesheetAbsoluteHeightElement;
import gui.style.dimensions.StylesheetAbsoluteWidthElement;
import gui.style.dimensions.StylesheetMagnitude;
import gui.style.dimensions.StylesheetPercentageHeightElement;
import gui.style.dimensions.StylesheetPercentageWidthElement;
import logging.CommonString;
import logging.Logs;
import script.ScriptEnvironment;
import script.exceptions.InternalException;
import script.exceptions.TemplateNotFoundException;
import script.exceptions.UnenclosedBracketException;
import script.exceptions.UnexpectedTypeException;
import script.exceptions.UnknownModifierException;
import script.exceptions.UnparseableElementException;
import script.exceptions.VariableAlreadyDefinedException;
import script.exceptions.VariableNotFoundException;
import script.exceptions.ScriptException;
import script.operations.ScriptExecutable;
import script.operations.ScriptExecutable_AssignValue;
import script.operations.ScriptExecutable_AutoMathematicator;
import script.operations.ScriptExecutable_CallFunction;
import script.operations.ScriptExecutable_CastExpression;
import script.operations.ScriptExecutable_CreateVariable;
import script.operations.ScriptExecutable_EvalAssignMathExpression;
import script.operations.ScriptExecutable_EvaluateBoolean;
import script.operations.ScriptExecutable_EvaluateMathExpression;
import script.operations.ScriptExecutable_ForStatement;
import script.operations.ScriptExecutable_IfStatement;
import script.operations.ScriptExecutable_ParseFunction;
import script.operations.ScriptExecutable_RetrieveCurrentObject;
import script.operations.ScriptExecutable_RetrieveVariable;
import script.operations.ScriptExecutable_ReturnValue;
import script.parsing.parsers.CommentRemover;
import script.parsing.parsers.QuoteTokenizer;
import script.proxies.FauxTemplate_Object;
import script.values.NoopScriptFunction;
import script.values.RiffScriptFunction;
import script.values.ScriptFunction;
import script.values.ScriptFunction_Constructor;
import script.values.ScriptTemplate;
import script.values.ScriptTemplate_Abstract;
import script.values.ScriptValue;
import script.values.ScriptValueType;
import script.values.ScriptValue_Boolean;
import script.values.ScriptValue_Null;
import script.values.ScriptValue_Numeric;
import script.values.ScriptValue_Variable;

/**
 * A collection of static methods that parse RiffScript code.
 * 
 * <ol>
 * <li>File --> List of naked strings
 * <li>List of naked strings --> Comments-removed naked strings
 * <li>Comments-removed naked strings --> Curly Bracket Groupings
 * <li>Curly Bracket Groupings --> Lists of line of code
 * </ol>
 * 
 * @author Aaron Faanes
 * 
 */
public final class Parser {

	private Parser() {
		throw new AssertionError("Instantiation is not allowed");
	}

	private static final List<TemplateParams> classParams = new LinkedList<TemplateParams>();

	public static void clearPreparseLists() {
		classParams.clear();
	}

	public static List<Exception> preparseFile(ScriptEnvironment env, String filename, BufferedReader reader) throws IOException {
		try {
			Iterator<String> iter = new LineIterator(reader);
			List<Object> strings = new ArrayList<Object>();
			int i = 1;
			while (iter.hasNext()) {
				strings.add(new ScriptLine(env, filename, i++, iter.next()));
			}
			return preparseFile(env, filename, strings);
		} catch (UnderlyingIOException ex) {
			// Unwrap and throw the IOException
			throw ex.getCause();
		}
	}

	private static List<Object> preparseList(List<Object> stringList) throws ScriptException {
		new CommentRemover().apply(stringList);
		stringList = new QuoteTokenizer().apply(stringList);
		stringList = createGroupings(stringList, CharacterGroup.CURLY_BRACES);
		stringList = createGroupings(stringList, CharacterGroup.PARENTHESES);
		stringList = parseOperators(stringList);
		stringList = removeEmptyScriptLines(stringList);
		stringList = splitByWhitespace(stringList);
		stringList = removeEmptyScriptLines(stringList);
		stringList = extractKeywords(stringList);
		stringList = extractNumbers(stringList);
		return stringList;
	}

	private static List<Exception> preparseFile(ScriptEnvironment env, String filename, List<Object> stringList) {
		List<Exception> exceptions = new ArrayList<Exception>();
		assert Logs.openNode("File Preparsing", "Preparsing file (" + filename + ")");
		try {
			assert Logs.addSnapNode(CommonString.ELEMENTS, stringList);
			preparseElements(env, preparseList(stringList));
			assert Logs.addNode("Preparsed successfully");
		} catch (ScriptException ex) {
			Logs.printException(ex);
			exceptions.add(ex);
		} catch (InternalException ex) {
			Logs.printException(ex);
			exceptions.add(ex);
		} finally {
			assert Logs.closeNode();
		}
		return exceptions;
	}

	private static ScriptExecutable_ParseFunction preparseFunction(ScriptEnvironment env, final ScriptTemplate_Abstract object, List<Object> modifiers, ScriptGroup paramGroup, ScriptGroup body, String name) throws ScriptException {
		if (name.equals("")) {
			assert Logs.openNode("Preparsing Functions", "Preparsing Function (constructor)");
		} else {
			assert Logs.openNode("Preparsing Functions", "Preparsing Function (" + name + ")");
		}
		if (object == null) {
			throw new NullPointerException("object must not be null");
		}
		assert Logs.addSnapNode("Reference Template", object);
		assert Logs.addSnapNode("Modifiers", modifiers);
		assert Logs.addSnapNode("Parameters", paramGroup);
		assert Logs.addSnapNode("Body", body);
		ScriptExecutable_ParseFunction function;
		assert env != null : "ScriptEnvironment for parseFunction is null.";
		ScriptValueType returnType = null;
		Referenced ref = null;
		String functionName = null;
		ScriptKeywordType permission = ScriptKeywordType.PRIVATE;
		boolean isStatic, isAbstract;
		isStatic = isAbstract = false;
		for (int i = 0; i < modifiers.size(); i++) {
			if (modifiers.get(i) instanceof ScriptKeyword) {
				if (ref == null) {
					ref = (ScriptKeyword) modifiers.get(i);
				}
				if (i == modifiers.size() - 1) {
					throw new UnexpectedTypeException((Referenced) modifiers.get(i), "Function name");
				}
				ScriptKeyword keyword = (ScriptKeyword) modifiers.get(i);
				if (keyword.equals(ScriptKeywordType.STATIC)) {
					assert Logs.addNode("Modifier Parsing", "Static modifier found");
					isStatic = true;
				} else if (keyword.equals(ScriptKeywordType.ABSTRACT)) {
					assert Logs.addNode("Modifier Parsing", "Abstract modifier found");
					isAbstract = true;
				} else if (keyword.equals(ScriptKeywordType.PRIVATE) || keyword.equals(ScriptKeywordType.PUBLIC) || keyword.equals(ScriptKeywordType.PROTECTED)) {
					assert Logs.addNode("Modifier Parsing", "Permission modifier found (" + keyword + ")");
					permission = keyword.getType();
				} else {
					if (ScriptValueType.isReturnablePrimitiveType(keyword.getValueType()) && returnType == null) {
						returnType = keyword.getValueType();
					} else {
						throw new UnexpectedTypeException((Referenced) modifiers.get(i), "Modifier");
					}
				}
			}
			if (modifiers.get(i) instanceof ScriptLine) {
				if (ref == null) {
					ref = (ScriptLine) modifiers.get(i);
				}
				if (i == modifiers.size() - 1) {
					// It's a function name
					if (returnType != null) {
						functionName = ((ScriptLine) modifiers.get(i)).getString();
						break;
					}
					// It's a constructor
					if (returnType == null) {
						returnType = ScriptValueType.createType((ScriptLine) modifiers.get(i), ((ScriptLine) modifiers.get(i)).getString());
						break;
					}
				}
				if (i != modifiers.size() - 2) {
					throw new UnexpectedTypeException((Referenced) modifiers.get(i), "Modifier");
				}
				if (!(modifiers.get(modifiers.size() - 1) instanceof ScriptLine)) {
					throw new UnexpectedTypeException((Referenced) modifiers.get(modifiers.size() - 1), "Function name");
				}
				if (returnType != null) {
					throw new UnexpectedTypeException((Referenced) modifiers.get(i), "Modifier");
				}
				returnType = ScriptValueType.createType((ScriptLine) modifiers.get(i), ((ScriptLine) modifiers.get(i)).getString());
				functionName = ((ScriptLine) modifiers.get(modifiers.size() - 1)).getString();
			}
		}
		if (functionName == null) {
			function = new ScriptExecutable_ParseFunction(ref, returnType, object, functionName, parseParamGroup(env, paramGroup, object.getType()), permission, true, false, body);
			assert Logs.addSnapNode("Function parsed is a constructor (" + returnType + ")", function);
		} else if (returnType != null) {
			function = new ScriptExecutable_ParseFunction(ref, returnType, object, functionName, parseParamGroup(env, paramGroup, object.getType()), permission, isStatic, isAbstract, body);
			assert Logs.addSnapNode("Function parsed is a regular function with a primitive return type (" + returnType + ")", function);
		} else {
			throw new UnexpectedTypeException((Referenced) modifiers.get(modifiers.size() - 1), "Function parameters");
		}
		assert Logs.closeNode();
		return function;
	}

	private static List<Object> createGroupings(List<Object> stringList, CharacterGroup group) throws ScriptException {
		assert Logs.openNode("Character-Group Parsing", "Creating Groupings (" + group + ")");
		assert Logs.addSnapNode(CommonString.ELEMENTS, stringList);
		assert Logs.addSnapNode("Character Group", group);
		stringList = removeSingleLineGroupings(stringList, group);
		boolean foundGroup = false;
		for (int i = 0; i < stringList.size(); i++) {
			Object obj = stringList.get(i);
			if (obj instanceof ScriptGroup) {
				if (group.isRecursive()) {
					assert Logs.openNode("Group found - recursing to parse");
					((ScriptGroup) obj).setElements(createGroupings(((ScriptGroup) obj).getElements(), group));
					assert Logs.closeNode();
				}
				continue;
			}
			if (!(obj instanceof ScriptLine)) {
				continue;
			}
			ScriptLine scriptLine = (ScriptLine) obj;
			int j = scriptLine.getString().indexOf(group.getEnd());
			if (j == -1) {
				continue;
			}
			assert Logs.addSnapNode("Found closing character - searching backwards for opening character", scriptLine);
			assert foundGroup = true;
			List<Object> newList = new LinkedList<Object>();
			newList.add(new ScriptLine(scriptLine.getString().substring(0, j), scriptLine, (short) 0));
			scriptLine.setString(scriptLine.getString().substring(j + group.getEnd().length()));
			for (int q = i - 1; q >= 0; q--) {
				if (!(stringList.get(q) instanceof ScriptLine)) {
					newList.add(stringList.get(q));
					stringList.remove(q);
					i--;
					continue;
				}
				ScriptLine backwardScriptLine = (ScriptLine) stringList.get(q);
				int x = backwardScriptLine.getString().lastIndexOf(group.getStart());
				if (x == -1) {
					if (q == 0) {
						throw new UnenclosedBracketException(scriptLine);
					}
					newList.add(backwardScriptLine);
					stringList.remove(q);
					i--;
					continue;
				}
				assert Logs.addSnapNode("Found opening character", backwardScriptLine);
				newList.add(new ScriptLine(
						backwardScriptLine.getString().substring(x + group.getStart().length()),
						backwardScriptLine,
						x + group.getStart().length()
						));
				backwardScriptLine.setString(backwardScriptLine.getString().substring(0, x));
				Collections.reverse(newList);
				assert Logs.openNode("Recursing to parse elements in newly created group");
				stringList.add(i, new ScriptGroup((Referenced) newList.get(0), createGroupings(newList, group), group));
				assert Logs.closeNode();
				assert Logs.openNode("Recursing to parse remaining elements");
				List<Object> list = createGroupings(stringList, group);
				assert Logs.closeNode();
				assert Logs.closeNode();
				return list;
			}
		}
		if (!foundGroup) {
			assert Logs.closeNode("No group found - naturally returning");
		}
		return stringList;
	}

	private static List<Object> parseOperator(ScriptLine line, String operator) {
		int location = line.getString().indexOf(operator);
		if (location == -1) {
			return null;
		}
		assert Logs.openNode("Operator Parsing", ScriptOperatorType.parse(operator) + " found in script-line: " + line.getString());
		assert Logs.addNode(line);
		List<Object> list = new LinkedList<Object>();
		String string = line.getString().substring(0, location).trim();
		String originalString = line.getString();
		if (string.length() > 0) {
			list.add(line);
			line.setString(string);
		}
		list.add(new ScriptOperator(new ScriptLine(operator, line, location), ScriptOperatorType.parse(operator)));
		string = originalString.substring(location + operator.length()).trim();
		if (string.length() > 0) {
			list.add(new ScriptLine(string, line, (short) (location + operator.length())));
		}
		assert Logs.closeNode("Split-string list formed from operator parse", list);
		return list;
	}

	private static List<Object> parseOperators(List<Object> list) {
		for (int i = 0; i < list.size(); i++) {
			Object element = list.get(i);
			if (element instanceof ScriptGroup) {
				((ScriptGroup) element).setElements(parseOperators(((ScriptGroup) element).getElements()));
				continue;
			}
			if (!(element instanceof ScriptLine)) {
				continue;
			}
			list.remove(i);
			list.addAll(i, parseOperators((ScriptLine) element));
		}
		return list;
	}

	private static List<Object> parseOperators(ScriptLine line) {
		List<Object> list = parseOperator(line, ";");
		if (list != null) {
			return parseOperators(list);
		}
		list = parseOperator(line, ",");
		if (list != null) {
			return parseOperators(list);
		}
		list = parseOperator(line, ".");
		if (list != null) {
			return parseOperators(list);
		}
		list = parseOperator(line, ":");
		if (list != null) {
			return parseOperators(list);
		}
		list = parseOperator(line, "#");
		if (list != null) {
			return parseOperators(list);
		}
		// Comparision operations
		list = parseOperator(line, "==");
		if (list != null) {
			return parseOperators(list);
		}
		list = parseOperator(line, "!=");
		if (list != null) {
			return parseOperators(list);
		}
		list = parseOperator(line, ">=");
		if (list != null) {
			return parseOperators(list);
		}
		list = parseOperator(line, "<=");
		if (list != null) {
			return parseOperators(list);
		}
		list = parseOperator(line, ">");
		if (list != null) {
			return parseOperators(list);
		}
		list = parseOperator(line, "<");
		if (list != null) {
			return parseOperators(list);
		}
		// Boolean operations
		list = parseOperator(line, "!");
		if (list != null) {
			return parseOperators(list);
		}
		list = parseOperator(line, "&&");
		if (list != null) {
			return parseOperators(list);
		}
		list = parseOperator(line, "||");
		if (list != null) {
			return parseOperators(list);
		}
		// Single-line equation operations
		list = parseOperator(line, "++");
		if (list != null) {
			return parseOperators(list);
		}
		list = parseOperator(line, "--");
		if (list != null) {
			return parseOperators(list);
		}
		list = parseOperator(line, "+=");
		if (list != null) {
			return parseOperators(list);
		}
		list = parseOperator(line, "-=");
		if (list != null) {
			return parseOperators(list);
		}
		list = parseOperator(line, "*=");
		if (list != null) {
			return parseOperators(list);
		}
		list = parseOperator(line, "/=");
		if (list != null) {
			return parseOperators(list);
		}
		list = parseOperator(line, "%=");
		if (list != null) {
			return parseOperators(list);
		}
		// Assignment operation
		list = parseOperator(line, "=");
		if (list != null) {
			return parseOperators(list);
		}
		// Mathematical operations.
		list = parseOperator(line, "-");
		if (list != null) {
			return parseOperators(list);
		}
		list = parseOperator(line, "+");
		if (list != null) {
			return parseOperators(list);
		}
		list = parseOperator(line, "*");
		if (list != null) {
			return parseOperators(list);
		}
		list = parseOperator(line, "/");
		if (list != null) {
			return parseOperators(list);
		}
		list = parseOperator(line, "%");
		if (list != null) {
			return parseOperators(list);
		}
		return Collections.<Object> singletonList(line);
	}

	private static List<Object> removeEmptyScriptLines(List<Object> list) {
		assert Logs.openNode("Empty Script-Line Removals", "Empty Script-Line Removal");
		assert Logs.addSnapNode(CommonString.ELEMENTS, list);
		Iterator<Object> iter = list.iterator();
		while (iter.hasNext()) {
			Object element = iter.next();
			if (element instanceof ScriptGroup) {
				assert Logs.openNode("Found script-group - recursing to parse");
				((ScriptGroup) element).setElements(removeEmptyScriptLines(((ScriptGroup) element).getElements()));
				assert Logs.closeNode();
				continue;
			}
			if (!(element instanceof ScriptLine)) {
				continue;
			}
			if (((ScriptLine) element).getString().trim().length() == 0) {
				assert Logs.addSnapNode("Removing line", element);
				iter.remove();
			}
		}
		assert Logs.closeNode();
		return list;
	}

	private static List<Object> extractKeywords(List<Object> lineList) throws ScriptException {
		ListIterator<Object> iter = lineList.listIterator();
		while (iter.hasNext()) {
			Object line = iter.next();
			if (line instanceof ScriptGroup) {
				ScriptGroup group = (ScriptGroup) line;
				group.setElements(extractKeywords(group.getElements()));
				continue;
			}
			if (!(line instanceof ScriptLine)) {
				continue;
			}
			ScriptLine scriptLine = (ScriptLine) line;
			ScriptKeywordType keyword = ScriptKeywordType.fromCanonical(scriptLine.getString());
			if (keyword == null) {
				continue;
			}
			iter.remove();
			iter.add(new ScriptKeyword(scriptLine, keyword));
		}
		return lineList;
	}

	// Procedural parsing functions
	private static List<Object> extractNumbers(List<Object> lineList) {
		for (int i = 0; i < lineList.size(); i++) {
			if (lineList.get(i) instanceof ScriptGroup) {
				((ScriptGroup) lineList.get(i)).setElements(extractNumbers(((ScriptGroup) lineList.get(i)).getElements()));
				continue;
			}
			if (!(lineList.get(i) instanceof ScriptLine)) {
				continue;
			}
			ScriptLine line = (ScriptLine) lineList.get(i);
			String number = line.getString();
			if (number.matches("^[0-9]*px$")) {
				String numString = ((ScriptLine) lineList.get(i)).getString().substring(0, ((ScriptLine) lineList.get(i)).getString().length() - 2);
				ScriptValue_Numeric scriptShort = new ScriptValue_Numeric(line.getEnvironment(), Short.parseShort(numString));
				assert Logs.addSnapNode("Number Extractions", "Short numeric value parsed (" + number + ")", scriptShort);
				lineList.remove(i);
				lineList.add(i, scriptShort);
				continue;
			}
			if (!number.matches("^[0-9]+[fd]{0,1}$")) {
				continue;
			}
			if (number.matches("^[0-9]*$") && i < lineList.size() - 1 && lineList.get(i + 1) instanceof ScriptOperator && ((ScriptOperator) lineList.get(i + 1)).getType() == ScriptOperatorType.MODULUS) {
				ScriptValue_Numeric scriptFloat = new ScriptValue_Numeric(line.getEnvironment(), Float.parseFloat(number));
				lineList.remove(i);
				lineList.add(i, scriptFloat);
				continue;
			}
			if (i > 0 && lineList.get(i - 1) instanceof ScriptOperator && ((ScriptOperator) lineList.get(i - 1)).getType() == ScriptOperatorType.PERIOD) {
				number = "." + number;
				lineList.remove(i - 1);
				i--;
				if (i > 0 && lineList.get(i - 1) instanceof ScriptOperator && ((ScriptOperator) lineList.get(i - 1)).getType() == ScriptOperatorType.MINUS) {
					if (i <= 1 || !(lineList.get(i - 2) instanceof ScriptValue_Numeric)) {
						number = "-" + number;
						lineList.remove(i - 1);
						i--;
					}
				}
			} else if (i > 0 && lineList.get(i - 1) instanceof ScriptOperator && ((ScriptOperator) lineList.get(i - 1)).getType() == ScriptOperatorType.MINUS) {
				if (i <= 1 || !(lineList.get(i - 2) instanceof ScriptValue_Numeric)) {
					number = "-" + number;
					lineList.remove(i - 1);
					i--;
				}
			}
			if (i < lineList.size() - 1 && lineList.get(i + 1) instanceof ScriptOperator && ((ScriptOperator) lineList.get(i + 1)).getType() == ScriptOperatorType.PERIOD) {
				number += "." + ((ScriptLine) lineList.get(i + 2)).getString();
				lineList.remove(i + 1);
				lineList.remove(i + 1);
			}
			if (number.length() > 3 && number.substring(number.length() - 2).equals("em")) {
				number = number.substring(0, number.length() - 2);
				ScriptValue_Numeric scriptFloat = new ScriptValue_Numeric(line.getEnvironment(), Float.parseFloat(number) * 14);
				assert Logs.addSnapNode("Number Extractions", "Float numeric value parsed (" + number + ")", scriptFloat);
				lineList.remove(i);
				lineList.remove(i);
				lineList.add(i, scriptFloat);
				continue;
			}
			if (number.charAt(number.length() - 1) == 'f') {
				ScriptValue_Numeric scriptFloat = new ScriptValue_Numeric(line.getEnvironment(), Float.parseFloat(number));
				assert Logs.addSnapNode("Number Extractions", "Float numeric value parsed (" + number + ")", scriptFloat);
				lineList.remove(i);
				lineList.add(i, scriptFloat);
				continue;
			}
			if (number.indexOf(".") != -1) {
				ScriptValue_Numeric scriptDouble = new ScriptValue_Numeric(line.getEnvironment(), Double.parseDouble(number));
				assert Logs.addSnapNode("Number Extractions", "Double numeric value parsed (" + number + ")", scriptDouble);
				lineList.remove(i);
				lineList.add(i, scriptDouble);
				continue;
			}
			if (number.length() < 5) {
				ScriptValue_Numeric scriptShort = new ScriptValue_Numeric(line.getEnvironment(), Short.parseShort(number));
				lineList.remove(i);
				lineList.add(i, scriptShort);
				continue;
			}
			if (number.length() < 10) {
				ScriptValue_Numeric scriptInt = new ScriptValue_Numeric(line.getEnvironment(), Integer.parseInt(number));
				assert Logs.addSnapNode("Number Extractions", "Integer numeric value parsed (" + number + ")", scriptInt);
				lineList.remove(i);
				lineList.add(i, scriptInt);
				continue;
			}
			ScriptValue_Numeric scriptLong = new ScriptValue_Numeric(line.getEnvironment(), Long.parseLong(number));
			assert Logs.addSnapNode("Number Extractions", "Long numeric value parsed (" + number + ")", scriptLong);
			lineList.remove(i);
			lineList.add(i, scriptLong);
		}
		return lineList;
	}

	private static List<ScriptExecutable> parseBodyList(ScriptEnvironment env, List<Object> bodyElements, ScriptValueType type) throws ScriptException {
		assert Logs.openNode("Body List Parsing", "Parsing Body List (" + bodyElements.size() + " element(s))");
		List<Object> elements = new LinkedList<Object>();
		List<ScriptExecutable> statementBodyList = new LinkedList<ScriptExecutable>();
		for (int j = 0; j < bodyElements.size(); j++) {
			if (bodyElements.get(j) instanceof ScriptOperator && ((ScriptOperator) bodyElements.get(j)).getType() == ScriptOperatorType.SEMICOLON) {
				statementBodyList.add(parseExpression(env, elements, false, type));
				elements.clear();
				continue;
			}
			elements.add(bodyElements.get(j));
			if (bodyElements.get(j) instanceof ScriptGroup && ((ScriptGroup) bodyElements.get(j)).getType() == CharacterGroup.CURLY_BRACES) {
				if (j + 1 < bodyElements.size() && bodyElements.get(j + 1).equals(ScriptKeywordType.ELSE)) {
					continue;
				}
				statementBodyList.add(parseFlowElement(env, elements, type));
				elements.clear();
				continue;
			}
		}
		if (elements.size() != 0) {
			statementBodyList.add(parseExpression(env, elements, false, type));
		}
		assert Logs.closeNode();
		return statementBodyList;
	}

	public static List<Exception> parseElements(ScriptEnvironment env) {
		List<Exception> exceptions = new ArrayList<Exception>();
		assert Logs.openNode("Element Parsing", "Parsing Elements");
		try {
			List<ScriptTemplate_Abstract> queuedTemplates = new LinkedList<ScriptTemplate_Abstract>();
			for (TemplateParams params : classParams) {
				ScriptTemplate_Abstract template = preparseTemplate(params.getDebugReference(), env, params.getModifiers(), params.getBody(), params.getName());
				queuedTemplates.add(template);
				env.addTemplate(params.getDebugReference(), params.getName(), template);
			}
			for (int i = 0; i < queuedTemplates.size(); i++) {
				queuedTemplates.get(i).initializeFunctions(classParams.get(i).getDebugReference());
			}
		} catch (ScriptException ex) {
			Logs.printException(ex);
			exceptions.add(ex);
		} catch (InternalException ex) {
			Logs.printException(ex);
			exceptions.add(ex);
		} finally {
			assert Logs.closeNode();
		}
		return exceptions;
	}

	private static ScriptExecutable parseExpression(ScriptEnvironment env, List<Object> list, boolean automaticallyAddToStack, ScriptValueType type) throws ScriptException {
		if (list.size() == 1 && list.get(0) instanceof ScriptExecutable) {
			return (ScriptExecutable) list.get(0);
		}
		for (int i = 0; i < list.size(); i++) {
			//assert Debugger.addSnapNode(DebugString.ELEMENTS,list);
			Object obj = list.get(i);
			//assert Debugger.addSnapNode("Current element ("+obj+")",obj);
			Object nextObj = null;
			if (i < list.size() - 1) {
				nextObj = list.get(i + 1);
			}
			if (obj instanceof ScriptExecutable) {
				if (i == list.size() - 1) {
					return (ScriptExecutable) obj;
				}
				continue;
			}
			if (obj.equals(ScriptKeywordType.NULL)) {
				ScriptExecutable returnValue = new ScriptValue_Null((Referenced) obj);
				assert Logs.addSnapNode("Expression Parsing", "'null' keyword parsed", returnValue);
				return returnValue;
			}
			// This keyword
			if (obj.equals(ScriptKeywordType.THIS)) {
				ScriptExecutable returnValue = new ScriptExecutable_RetrieveCurrentObject((Referenced) obj, type);
				assert Logs.addSnapNode("Expression Parsing", "'this' keyword parsed", returnValue);
				return returnValue;
			}
			// Returns!
			if (obj.equals(ScriptKeywordType.RETURN)) {
				Referenced ref = (Referenced) list.get(i);
				list.remove(i);
				ScriptExecutable_ReturnValue returnValue;
				if (list.size() == 1 && list.get(0) instanceof ScriptValue) {
					returnValue = new ScriptExecutable_ReturnValue(ref, (ScriptValue) list.get(0));
				} else {
					returnValue = new ScriptExecutable_ReturnValue(ref, (ScriptValue) parseExpression(env, list, automaticallyAddToStack, type));
				}
				assert Logs.addSnapNode("Expression Parsing", "Return value parsed", returnValue);
				return returnValue;
			}
			if (obj instanceof ScriptGroup) {
				ScriptGroup group = (ScriptGroup) obj;
				List<Object> groupList = group.getElements();
				list.remove(i);
				if (groupList.size() == 1) {
					// Casting
					ScriptExecutable_CastExpression caster = null;
					if (groupList.get(0) instanceof ScriptLine) {
						caster = new ScriptExecutable_CastExpression((Referenced) groupList.get(0), ScriptValueType.createType((ScriptLine) groupList.get(0), ((ScriptLine) groupList.get(0)).getString()), parseExpression(env, list, automaticallyAddToStack, type));
					} else if (groupList.get(0) instanceof ScriptKeyword) {
						caster = new ScriptExecutable_CastExpression((Referenced) groupList.get(0), ((ScriptKeyword) groupList.get(0)).getValueType(), parseExpression(env, list, automaticallyAddToStack, type));
					}
					assert Logs.addSnapNode("Expression Parsing", "Cast Expression Parsed", caster);
					return caster;
				}
				list.add(i, parseExpression(env, groupList, automaticallyAddToStack, type));
				continue;
			}
			// Primitive object creation.
			if (obj instanceof ScriptKeyword && (((ScriptKeyword) obj).equals(ScriptKeywordType.STYLESHEET) || ScriptValueType.isPrimitiveType(((ScriptKeyword) obj).getValueType()))) {
				// obj is ScriptKeyword for variable type
				// nextObj is the variableName
				ScriptKeywordType permission = ScriptKeywordType.PRIVATE;
				boolean isStatic = false;
				int loc = i;
				while (loc > 0) {
					if (!(list.get(loc) instanceof ScriptKeyword)) {
						throw new UnexpectedTypeException((Referenced) list.get(loc), "Keyword");
					}
					loc--;
				}
				int temp = i;
				i = loc;
				loc = temp - loc;
				for (; loc > 0; loc--) {
					ScriptKeywordType currKeyword = ((ScriptKeyword) list.get(i)).getType();
					if (currKeyword == ScriptKeywordType.PRIVATE || currKeyword == ScriptKeywordType.PUBLIC) {
						permission = ((ScriptKeyword) list.get(i)).getType();
					} else if (currKeyword == ScriptKeywordType.STATIC) {
						isStatic = true;
					}
					list.remove(i);
				}
				do {
					if (!(list.get(i + 1) instanceof ScriptLine)) {
						throw new UnexpectedTypeException((Referenced) list.get(i + 1), "Variable name");
					}
					String name = ((ScriptLine) list.get(i + 1)).getString();
					if (env.retrieveVariable(name) != null) {
						throw new VariableAlreadyDefinedException((Referenced) list.get(i + 1), null, name);
					}
					ScriptValue_Variable creator;
					if (((ScriptKeyword) obj).equals(ScriptKeywordType.STYLESHEET)) {
						creator = new ScriptExecutable_CreateVariable((Referenced) list.get(i), ScriptValueType.createType(env, Stylesheet.STYLESHEETSTRING), name, permission);
					} else {
						creator = new ScriptExecutable_CreateVariable((Referenced) list.get(i), ((ScriptKeyword) obj).getValueType(), name, permission);
					}
					ScriptExecutable exec = (ScriptExecutable) creator;
					if (((ScriptKeyword) obj).equals(ScriptKeywordType.STYLESHEET)) {
						if (list.size() > i + 2 && list.get(i + 2) instanceof ScriptGroup) {
							exec = new ScriptExecutable_AssignValue((Referenced) list.get(i + 1), (ScriptValue) exec, parseStylesheet((Referenced) list.get(i + 1), env, (ScriptGroup) list.get(i + 2)));
						}
						list.remove(i + 2);
					}
					assert Logs.addSnapNode("Expression Parsing", "Variable creation element parsed", exec);
					if (automaticallyAddToStack && env.getCurrentObject() != null && env.getCurrentObject().isConstructing()) {
						if (isStatic) {
							env.getCurrentObject().addTemplatePreconstructorExpression(exec);
						} else {
							env.getCurrentObject().addPreconstructorExpression(exec);
						}
					}
					env.addVariableToStack(name, creator);
					list.remove(i);
					list.remove(i);
					list.add(i, exec);
					if (i < list.size() - 1) {
						i++;
					}
				} while (list.get(i).equals(ScriptOperatorType.COMMA));
				return parseExpression(env, list, automaticallyAddToStack, type);
			}
			// Object creation
			if (obj.equals(ScriptKeywordType.NEW)) {
				if (!(nextObj instanceof ScriptLine)) {
					throw new UnexpectedTypeException((Referenced) nextObj, "Object name");
				}
				if (!(list.get(i + 2) instanceof ScriptGroup)) {
					throw new UnexpectedTypeException((Referenced) nextObj, "Parameters");
				}
				if (env.getTemplate(((ScriptLine) nextObj).getString()) == null) {
					throw new TemplateNotFoundException((ScriptLine) nextObj, ((ScriptLine) nextObj).getString());
				}
				ScriptExecutable returnValue = new ScriptExecutable_CallFunction((Referenced) obj, new ScriptExecutable_RetrieveVariable((Referenced) obj, null, ((ScriptLine) nextObj).getString(), env.retrieveVariable(((ScriptLine) nextObj).getString()).getType()), "", parseParamGroup(env, (ScriptGroup) list.get(i + 2), type));
				assert Logs.addSnapNode("Object construction element parsed", returnValue);
				list.remove(i);
				list.remove(i);
				list.remove(i);
				return returnValue;
			}
			/* Operators that still need to be implemented
				public static final short COLON=23;*/
			// Operators!
			if (obj instanceof ScriptOperator) {
				ScriptValue_Variable lhs;
				ScriptValue left;
				ScriptExecutable returnValue;
				switch (((ScriptOperator) obj).getType()) {
				case GREATER:
				case LESS:
				case GREATEREQUALS:
				case LESSEQUALS:
				case EQUIVALENCY:
				case NONEQUIVALENCY:
					if (i < 1 || !(list.get(i - 1) instanceof ScriptValue)) {
						throw new UnexpectedTypeException((Referenced) list.get(i), "Variable");
					}
					i--;
					left = (ScriptValue) list.get(i);
					list.remove(i);
					list.remove(i);
					if (list.size() == 1 && list.get(0) instanceof ScriptValue) {
						returnValue = new ScriptExecutable_EvaluateBoolean((Referenced) obj, left, (ScriptValue) list.get(i), ((ScriptOperator) obj).getType());
						list.remove(i);
						assert Logs.addSnapNode("Expression Parsing", "Boolean expression parsed", returnValue);
						return returnValue;
					}
					returnValue = new ScriptExecutable_EvaluateBoolean((Referenced) obj, left, (ScriptValue) parseExpression(env, list, automaticallyAddToStack, type), ((ScriptOperator) obj).getType());
					assert Logs.addSnapNode("Expression Parsing", "Boolean expression parsed", returnValue);
					return returnValue;
				case PERIOD:
					if (i < 1 || !(list.get(i - 1) instanceof ScriptValue)) {
						throw new UnexpectedTypeException((Referenced) list.get(i - 1), "Variable");
					}
					i--;
					left = (ScriptValue) list.get(i);
					list.remove(i);
					list.remove(i);
					if (!(list.get(i) instanceof ScriptLine)) {
						throw new UnexpectedTypeException((Referenced) list.get(i), "Function name");
					}
					ScriptLine name = (ScriptLine) list.get(i);
					list.remove(i);
					if (list.get(i) instanceof ScriptGroup) {
						ScriptGroup group = (ScriptGroup) list.get(i);
						returnValue = new ScriptExecutable_CallFunction(group, left, name.getString(), parseParamGroup(env, group.getElements(), type));
						assert Logs.addSnapNode("Expression Parsing", "Object function call parsed", returnValue);
						list.remove(i);
					} else {
						returnValue = new ScriptExecutable_RetrieveVariable(name, left, name.getString(), env.getTemplate(left.getType()).getVariable(name.getString()).getType());
						assert Logs.addSnapNode("Expression Parsing", "Object member-variable placeholder parsed", returnValue);
					}
					list.add(i, returnValue);
					return parseExpression(env, list, automaticallyAddToStack, type);
				case ASSIGNMENT:
					i--;
					if (!(list.get(i) instanceof ScriptExecutable)) {
						throw new UnexpectedTypeException((Referenced) list.get(i), "Variable name");
					}
					lhs = (ScriptValue_Variable) list.get(i);
					list.remove(i);
					list.remove(i);
					if (list.size() == 1 && list.get(i) instanceof ScriptValue) {
						returnValue = new ScriptExecutable_AssignValue((Referenced) lhs, lhs, (ScriptValue) list.get(i));
					} else {
						returnValue = new ScriptExecutable_AssignValue((Referenced) lhs, lhs, (ScriptValue) parseExpression(env, list, automaticallyAddToStack, type));
					}
					assert Logs.addSnapNode("Expression Parsing", "Variable assignment expression parsed", returnValue);
					return returnValue;
				case PLUS:
				case MINUS:
				case MULTIPLY:
				case DIVIDE:
				case MODULUS:
					if (i == 0 || !(list.get(0) instanceof ScriptValue)) {
						throw new UnexpectedTypeException((Referenced) list.get(0), "Variable");
					}
					left = (ScriptValue) list.get(0);
					list.remove(0);
					list.remove(0);
					//public ScriptExecutable_EvaluateMathExpression(Referenced ref, ScriptValue_Abstract lhs, ScriptValue_Abstract rhs,ScriptOperatorType expressionType)
					if (list.size() == 1 && list.get(0) instanceof ScriptValue) {
						returnValue = new ScriptExecutable_EvaluateMathExpression((Referenced) obj, left, (ScriptValue) list.get(0), ((ScriptOperator) obj).getType());
					} else {
						returnValue = new ScriptExecutable_EvaluateMathExpression((Referenced) obj, left, (ScriptValue) parseExpression(env, list, automaticallyAddToStack, type), ((ScriptOperator) obj).getType());
					}
					assert Logs.addSnapNode("Expression Parsing", "Mathematical expression parsed", returnValue);
					return returnValue;
				case PLUSEQUALS:
				case MINUSEQUALS:
				case MULTIPLYEQUALS:
				case DIVIDEEQUALS:
				case MODULUSEQUALS:
					if (i == 0 || !(list.get(0) instanceof ScriptExecutable)) {
						throw new UnexpectedTypeException((Referenced) list.get(0), "Variable");
					}
					lhs = (ScriptValue_Variable) list.get(0);
					list.remove(0);
					list.remove(0);
					if (list.size() == 1 && list.get(0) instanceof ScriptValue) {
						//public ScriptExecutable_EvalAssignMathExpression(Referenced ref, ScriptValue lhs, ScriptValue rhs,ScriptOperatorType operation){
						returnValue = new ScriptExecutable_EvalAssignMathExpression((Referenced) lhs, lhs, (ScriptValue) list.get(0), ((ScriptOperator) obj).getType());
					} else {
						returnValue = new ScriptExecutable_EvalAssignMathExpression((Referenced) lhs, lhs, (ScriptValue) parseExpression(env, list, automaticallyAddToStack, type), ((ScriptOperator) obj).getType());
					}
					assert Logs.addSnapNode("Expression Parsing", "Mathematical assignment expression parsed", returnValue);
					return returnValue;
				case INCREMENT:
				case DECREMENT:
					//public ScriptExecutable_AutoMathematicator(Referenced ref,ScriptValue_Abstract value,ScriptOperatorType operator,boolean isPost)
					if (i > 0 && list.get(i - 1) instanceof ScriptValue) {
						// Post-increment
						i--;
						returnValue = new ScriptExecutable_AutoMathematicator((Referenced) list.get(i + 1), (ScriptValue) list.get(i), ((ScriptOperator) obj).getType(), true);
						assert Logs.addSnapNode("Expression Parsing", "Auto-mathematicator parsed", returnValue);
						list.remove(i);
						list.remove(i);
						return returnValue;
					} else {
						// Pre-increment
						list.remove(i);
						returnValue = new ScriptExecutable_AutoMathematicator((Referenced) list.get(i), (ScriptValue) parseExpression(env, list, automaticallyAddToStack, type), ((ScriptOperator) obj).getType(), false);
						assert Logs.addSnapNode("Expression Parsing", "Auto-mathematicator parsed", returnValue);
						return returnValue;
					}
				default:
					throw new IllegalArgumentException("Unexpected operator:" + obj);
				}
			}
			// Placeholder object creation, function calls, right-side variables
			if (obj instanceof ScriptLine) {
				ScriptExecutable returnValue;
				if (nextObj == null || nextObj instanceof ScriptOperator) {
					// It's a variable we've previously defined
					if (env.retrieveVariable(((ScriptLine) obj).getString()) == null) {
						Logs.addSnapNode("Environment before exception", env);
						throw new VariableNotFoundException((Referenced) obj, ((ScriptLine) obj).getString());
					}
					list.remove(i);
					returnValue = new ScriptExecutable_RetrieveVariable((Referenced) obj, null, ((ScriptLine) obj).getString(), env.retrieveVariable(((ScriptLine) obj).getString()).getType());
					list.add(i, returnValue);
					if (nextObj == null) {
						assert Logs.addSnapNode("Expression Parsing", "Variable placeholder parsed", returnValue);
						return (ScriptExecutable) list.get(i);
					}
					assert Logs.addSnapNode("Expression Parsing", "Variable placeholder parsed", returnValue);
					return parseExpression(env, list, automaticallyAddToStack, type);
				}
				if (nextObj instanceof ScriptGroup) {
					if (i + 1 == list.size() - 1 || !(list.get(i + 2) instanceof ScriptGroup)) {
						// It's a function call!
						//public ScriptExecutable_CallFunction(Referenced ref,ScriptValue_Abstract object,String functionName,List<ScriptValue_Abstract>params)
						ScriptExecutable_CallFunction fxnCall = new ScriptExecutable_CallFunction((Referenced) obj, null, ((ScriptLine) obj).getString(), parseParamGroup(env, (ScriptGroup) nextObj, type));
						list.remove(i);
						list.remove(i);
						list.add(i, fxnCall);
						assert Logs.addSnapNode("Expression Parsing", "Function call parsed", fxnCall);
						return fxnCall;
					}
				}
				if (nextObj instanceof ScriptLine) {
					// Object placeholder creation
					if (env.retrieveVariable(((ScriptLine) nextObj).getString()) != null) {
						throw new VariableAlreadyDefinedException((Referenced) nextObj, null, ((ScriptLine) nextObj).getString());
					}
					ScriptKeywordType permission = ScriptKeywordType.PRIVATE;
					boolean isStatic = false;
					if (i > 0 && list.get(i - 1) instanceof ScriptKeyword) {
						ScriptKeywordType currKeyword = ((ScriptKeyword) list.get(i - 1)).getType();
						if (currKeyword.equals(ScriptKeywordType.PRIVATE) || currKeyword.equals(ScriptKeywordType.PUBLIC)) {
							permission = currKeyword;
						} else if (currKeyword.equals(ScriptKeywordType.STATIC)) {
							isStatic = true;
							break;
						}
						list.remove(i - 1);
						i--;
					}
					ScriptExecutable_CreateVariable creator = new ScriptExecutable_CreateVariable((Referenced) obj, ScriptValueType.createType((ScriptLine) obj, ((ScriptLine) obj).getString()), ((ScriptLine) list.get(i + 1)).getString(), permission);
					assert Logs.addSnapNode("Expression Parsing", "Variable creation element parsed", creator);
					if (automaticallyAddToStack && env.getCurrentObject() != null && env.getCurrentObject().isConstructing()) {
						if (isStatic) {
							env.getCurrentObject().addTemplatePreconstructorExpression(creator);
						} else {
							env.getCurrentObject().addPreconstructorExpression(creator);
						}
					}
					env.addVariableToStack(creator.getName(), creator);
					list.remove(i);
					list.remove(i);
					list.add(i, creator);
					return parseExpression(env, list, automaticallyAddToStack, type);
				}
			}
		}
		if (list.size() == 1 && list.get(0) instanceof ScriptExecutable) {
			return (ScriptExecutable) list.get(0);
		}
		throw new AssertionError("Defaulted in parseExpression");
	}

	private static ScriptExecutable parseFlowElement(ScriptEnvironment env, List<Object> list, ScriptValueType type) throws ScriptException {
		assert Logs.openNode("Flow Element Parsing", "Parsing Flow Element");
		assert Logs.addSnapNode(CommonString.ELEMENTS, list);
		for (int i = 0; i < list.size(); i++) {
			Object obj = list.get(i);
			if (obj instanceof ScriptExecutable) {
				continue;
			}
			if (obj instanceof ScriptKeyword) {
				if (obj.equals(ScriptKeywordType.FOR)) {
					env.advanceNestedStack();
					list.remove(i);
					if (!(list.get(i) instanceof ScriptGroup)) {
						throw new UnexpectedTypeException(env, list.get(i), "Param group");
					}
					List<ScriptExecutable> parameterList = parseBodyList(env, ((ScriptGroup) list.get(i)).getElements(), type);
					if (parameterList.size() != 3) {
						throw new UnexpectedTypeException(env, list.get(i), "'for' statement parameters");
					}
					list.remove(i);
					if (!(list.get(i) instanceof ScriptGroup)) {
						throw new UnexpectedTypeException(env, list.get(i), "Curly group");
					}
					List<ScriptExecutable> bodyList = parseBodyList(env, ((ScriptGroup) list.get(i)).getElements(), type);
					ScriptExecutable_ForStatement forStatement = new ScriptExecutable_ForStatement(parameterList.get(0), parameterList.get(1), parameterList.get(2), bodyList);
					assert Logs.closeNode("For statement parsed", forStatement);
					env.retreatNestedStack();
					return forStatement;
				}
				if (obj.equals(ScriptKeywordType.ELSE)) {
					env.advanceNestedStack();
					list.remove(i);
					i--;
					if (i < 0 || !(list.get(i) instanceof ScriptExecutable_IfStatement)) {
						int q = i;
						if (q < 0) {
							q = 0;
						}
						throw new UnexpectedTypeException(env, list.get(q), "If statement");
					}
					ScriptExecutable_IfStatement previous = (ScriptExecutable_IfStatement) list.get(i);
					list.remove(i);
					if (list.get(i) instanceof ScriptKeyword && list.get(i).equals(ScriptKeywordType.IF)) {
						Referenced ref = (Referenced) list.get(i);
						list.remove(i);
						previous.setElseStatement(parseIfStatement(ref, list, null, i, type));
					} else if (list.get(i) instanceof ScriptGroup) {
						Referenced ref = (Referenced) list.get(i);
						previous.setElseStatement(parseIfStatement(ref, list, new ScriptValue_Boolean(((Referenced) obj).getEnvironment(), true), i, type));
					} else {
						throw new UnexpectedTypeException(env, list.get(i), "Keyword or code group");
					}
					assert Logs.closeNode("'Else' script group parsed", previous);
					env.retreatNestedStack();
					return null;
				}
				if (obj.equals(ScriptKeywordType.IF)) {
					env.advanceNestedStack();
					Referenced ref = (Referenced) list.get(i);
					list.remove(i);
					ScriptExecutable exec = parseIfStatement(ref, list, null, i, type);
					list.remove(i);
					env.retreatNestedStack();
					if (i < list.size() && list.get(i) instanceof ScriptKeyword && list.get(i).equals(ScriptKeywordType.ELSE)) {
						assert Logs.openNode("Found else keyword, recursing...");
						list.add(i, exec);
						parseFlowElement(env, list, type);
						assert Logs.closeNode();
					}
					assert Logs.closeNode("If group parsed", exec);
					return exec;
				}
			}
		}
		throw new UnexpectedTypeException(env, list.get(0), "Keyword");
	}

	public static ScriptFunction parseFunction(ScriptExecutable_ParseFunction function, ScriptValueType type) throws ScriptException {
		assert Logs.openNode("Parsing Functions", "Parsing Function (" + RiffScriptFunction.getDisplayableFunctionName(function.getName()) + ")");
		ScriptFunction fxn;
		if (function.getName() == null || function.getName().equals("")) {
			fxn = new ScriptFunction_Constructor(function.getReturnType(), function.getParameters(), function.getPermission());
		} else {
			fxn = new RiffScriptFunction(function.getReturnType(), function.getParameters(), function.getPermission(), function.isAbstract(), function.isStatic());
		}
		fxn.addExpressions(parseBodyList(function.getEnvironment(), function.getBody().getElements(), type));
		assert Logs.closeNode();
		return fxn;
	}

	private static ScriptExecutable_IfStatement parseIfStatement(Referenced ref, List<Object> list, ScriptValue value, int i, ScriptValueType type) throws ScriptException {
		assert Logs.openNode("If-Statement Parsing", "Parsing 'if' Statement (" + list.size() + " element(s))");
		assert Logs.addSnapNode("Boolean-Testing-Value", value);
		assert Logs.addSnapNode("Body Elements", list);
		if (value == null) {
			if (!(list.get(i) instanceof ScriptGroup)) {
				throw new UnexpectedTypeException(ref, list.get(i), "Param group");
			}
			value = (ScriptValue) parseExpression(ref.getEnvironment(), ((ScriptGroup) list.get(i)).getElements(), false, type);
			list.remove(i);
		}
		if (!(list.get(i) instanceof ScriptGroup)) {
			throw new UnexpectedTypeException(ref, list.get(i), "Curly group");
		}
		ScriptExecutable_IfStatement statement = new ScriptExecutable_IfStatement(ref, value, parseBodyList(ref.getEnvironment(), ((ScriptGroup) list.get(i)).getElements(), type));
		assert Logs.closeNode();
		return statement;
	}

	private static List<ScriptValue> parseParamGroup(ScriptEnvironment env, List<Object> elementsList, ScriptValueType type) throws ScriptException {
		assert Logs.openNode("Parameter-Group Parsing", "Parsing Parameter-Group (" + elementsList.size() + " element(s) in group)");
		assert Logs.addSnapNode(CommonString.ELEMENTS, elementsList);
		Iterator<Object> iter = elementsList.iterator();
		List<ScriptValue> groupList = new LinkedList<ScriptValue>();
		List<Object> currentParamList = new LinkedList<Object>();
		env.advanceNestedStack();
		while (iter.hasNext()) {
			Object obj = iter.next();
			if (obj instanceof ScriptOperator && ((ScriptOperator) obj).getType() == ScriptOperatorType.COMMA) {
				if (currentParamList.size() == 1 && currentParamList.get(0) instanceof ScriptValue) {
					groupList.add((ScriptValue) currentParamList.get(0));
				} else {
					groupList.add((ScriptValue) parseExpression(env, currentParamList, false, type));
				}
				currentParamList.clear();
				continue;
			}
			currentParamList.add(obj);
		}
		if (currentParamList.size() > 0) {
			if (currentParamList.size() == 1 && currentParamList.get(0) instanceof ScriptValue) {
				groupList.add((ScriptValue) currentParamList.get(0));
			} else {
				groupList.add((ScriptValue) parseExpression(env, currentParamList, false, type));
			}
		}
		env.retreatNestedStack();
		assert Logs.closeNode();
		return groupList;
	}

	private static List<ScriptValue> parseParamGroup(ScriptEnvironment env, ScriptGroup group, ScriptValueType type) throws ScriptException {
		return parseParamGroup(env, group.getElements(), type);
	}

	private static Stylesheet parseStylesheet(Referenced ref, ScriptEnvironment env, ScriptGroup group) throws ScriptException {
		assert Logs.openNode("Stylesheet Parsing", "Parsing Stylesheet");
		List<Object> elements = group.getElements();
		assert Logs.addSnapNode("Elements (" + elements.size() + " element(s))", elements);
		Stylesheet stylesheet = new Stylesheet(ref.getEnvironment(), true);
		for (int i = 0; i < elements.size(); i++) {
			if (elements.get(i) instanceof ScriptOperator && ((ScriptOperator) elements.get(i)).getType() == ScriptOperatorType.SEMICOLON) {
				continue;
			}
			assert elements.get(i) instanceof ScriptLine : "This element should be a ScriptLine: " + elements.get(i);
			Referenced keyRef = (ScriptLine) elements.get(i);
			String key = ((ScriptLine) elements.get(i)).getString();
			int offset = i + 1;
			if (elements.get(offset) instanceof ScriptOperator && ((ScriptOperator) elements.get(i + 1)).getType() == ScriptOperatorType.MINUS) {
				key = key + "-" + ((ScriptLine) elements.get(i + 2)).getString();
				offset += 2;
			}
			key = key.toLowerCase();
			if (key.equals("color")) {
				Color colorElem;
				assert elements.get(offset) instanceof ScriptOperator;
				assert ((ScriptOperator) elements.get(offset)).getType() == ScriptOperatorType.COLON;
				offset++;
				if (elements.get(offset) instanceof ScriptOperator && ((ScriptOperator) elements.get(offset)).getType() == ScriptOperatorType.POUNDSIGN) {
					offset++;
					if (elements.get(offset) instanceof ScriptLine) {
						colorElem = Stylesheets.getColor("#" + ((ScriptLine) elements.get(offset)).getString());
					} else {
						colorElem = Stylesheets.getColor("#" + ((ScriptValue_Numeric) elements.get(offset)).longValue());
					}

				} else {
					colorElem = Stylesheets.getColor(((ScriptLine) elements.get(offset)).getString());
				}
				assert Logs.addSnapNode("Stylesheet Element Parsing", "Color stylesheet-element parsed", colorElem);
				stylesheet.addElement(StylesheetProperty.COLOR, colorElem);
				i += offset - i;
				continue;
			}
			if (key.equals("font-size")) {
				StylesheetFontSizeElement fontSizeElem;
				assert elements.get(offset) instanceof ScriptOperator;
				assert ((ScriptOperator) elements.get(offset)).getType() == ScriptOperatorType.COLON;
				offset++;
				fontSizeElem = new StylesheetFontSizeElement(((ScriptValue_Numeric) elements.get(offset)).intValue());
				assert Logs.addSnapNode("Stylesheet Element Parsing", "Font size stylesheet-element parsed", fontSizeElem);
				stylesheet.addElement(StylesheetProperty.FONTSIZE, fontSizeElem);
				i += offset - i;
				continue;
			}
			if (key.equals("width")) {
				assert elements.get(offset) instanceof ScriptOperator;
				assert ((ScriptOperator) elements.get(offset)).getType() == ScriptOperatorType.COLON;
				offset++;
				StylesheetMagnitude<?> widthElem;
				if (elements.get(offset + 1) instanceof ScriptOperator && ((ScriptOperator) elements.get(offset + 1)).getType() == ScriptOperatorType.MODULUS) {
					widthElem = new StylesheetPercentageWidthElement(((ScriptValue_Numeric) elements.get(offset)).doubleValue() / 100);
					offset++;
				} else {
					assert elements.get(offset) instanceof ScriptValue_Numeric : "Should be a numeric value: " + elements.get(offset);
					widthElem = new StylesheetAbsoluteWidthElement(((ScriptValue_Numeric) elements.get(offset)).intValue());
				}
				assert Logs.addSnapNode("Stylesheet Element Parsing", "Width stylesheet-element parsed", widthElem);
				stylesheet.addElement(StylesheetProperty.WIDTH, widthElem);
				i += offset - i;
				continue;
			}
			if (key.equals("height")) {
				assert elements.get(offset) instanceof ScriptOperator;
				assert ((ScriptOperator) elements.get(offset)).getType() == ScriptOperatorType.COLON;
				offset++;
				StylesheetMagnitude<?> heightElem;
				if (elements.get(offset + 1) instanceof ScriptOperator && ((ScriptOperator) elements.get(offset + 1)).getType() == ScriptOperatorType.MODULUS) {
					heightElem = new StylesheetPercentageHeightElement(((ScriptValue_Numeric) elements.get(offset)).doubleValue() / 100);
					offset++;
				} else {
					assert elements.get(offset) instanceof ScriptValue_Numeric : "Should be a numeric value: " + elements.get(offset);
					heightElem = new StylesheetAbsoluteHeightElement(((ScriptValue_Numeric) elements.get(offset)).intValue());
				}
				assert Logs.addSnapNode("Stylesheet Element Parsing", "Height stylesheet-element parsed", heightElem);
				stylesheet.addElement(StylesheetProperty.HEIGHT, heightElem);
				i += offset - i;
				continue;
			}
			if (key.equals("margin-bottom") || key.equals("margin-top") || key.equals("margin-left") || key.equals("margin-right") || key.equals("margin")) {
				StylesheetMarginElement marginElem;
				assert elements.get(offset) instanceof ScriptOperator;
				assert ((ScriptOperator) elements.get(offset)).getType() == ScriptOperatorType.COLON;
				offset++;
				assert elements.get(offset) instanceof ScriptValue_Numeric : "This element should be a ScriptValue_Numeric: " + elements.get(offset);
				marginElem = new StylesheetMarginElement(((ScriptValue_Numeric) elements.get(offset)).intValue());
				assert Logs.addSnapNode("Stylesheet Element Parsing", "Margin stylesheet-element parsed", marginElem);
				if (key.equals("margin")) {
					stylesheet.addElement(StylesheetProperty.MARGINBOTTOM, marginElem);
					stylesheet.addElement(StylesheetProperty.MARGINTOP, marginElem);
					stylesheet.addElement(StylesheetProperty.MARGINLEFT, marginElem);
					stylesheet.addElement(StylesheetProperty.MARGINRIGHT, marginElem);
				} else if (key.equals("margin-bottom")) {
					stylesheet.addElement(StylesheetProperty.MARGINBOTTOM, marginElem);
				} else if (key.equals("margin-top")) {
					stylesheet.addElement(StylesheetProperty.MARGINTOP, marginElem);
				} else if (key.equals("margin-left")) {
					stylesheet.addElement(StylesheetProperty.MARGINLEFT, marginElem);
				} else if (key.equals("margin-right")) {
					stylesheet.addElement(StylesheetProperty.MARGINRIGHT, marginElem);
				}
				i += offset - i;
				continue;
			}
			if (key.equals("padding-bottom") || key.equals("padding-top") || key.equals("padding-left") || key.equals("padding-right") || key.equals("padding")) {
				StylesheetPaddingElement paddingElem;
				assert elements.get(offset) instanceof ScriptOperator;
				assert ((ScriptOperator) elements.get(offset)).getType() == ScriptOperatorType.COLON;
				offset++;
				assert elements.get(offset) instanceof ScriptValue_Numeric : "This element should be a ScriptValue_Numeric: " + elements.get(offset);
				paddingElem = new StylesheetPaddingElement(((ScriptValue_Numeric) elements.get(offset)).intValue());
				assert Logs.addSnapNode("Stylesheet Element Parsing", "Padding stylesheet-element parsed", paddingElem);
				if (key.equals("padding")) {
					stylesheet.addElement(StylesheetProperty.PADDINGBOTTOM, paddingElem);
					stylesheet.addElement(StylesheetProperty.PADDINGTOP, paddingElem);
					stylesheet.addElement(StylesheetProperty.PADDINGLEFT, paddingElem);
					stylesheet.addElement(StylesheetProperty.PADDINGRIGHT, paddingElem);
				} else if (key.equals("padding-bottom")) {
					stylesheet.addElement(StylesheetProperty.PADDINGBOTTOM, paddingElem);
				} else if (key.equals("padding-top")) {
					stylesheet.addElement(StylesheetProperty.PADDINGTOP, paddingElem);
				} else if (key.equals("padding-left")) {
					stylesheet.addElement(StylesheetProperty.PADDINGLEFT, paddingElem);
				} else if (key.equals("padding-right")) {
					stylesheet.addElement(StylesheetProperty.PADDINGRIGHT, paddingElem);
				}
				i += offset - i;
				continue;
			}
			if (key.equals("border-bottom") || key.equals("border-top") || key.equals("border-left") || key.equals("border-right") || key.equals("border")) {
				StylesheetBorderElement borderElem;
				if (!(elements.get(offset) instanceof ScriptOperator) || ((ScriptOperator) elements.get(offset)).getType() != ScriptOperatorType.COLON) {
					if (elements.get(offset) instanceof Referenced) {
						throw new UnexpectedTypeException((Referenced) elements.get(offset), "colon");
					} else {
						throw new UnexpectedTypeException(keyRef, elements.get(offset), "colon");
					}
				}
				offset++;
				if (!(elements.get(offset) instanceof ScriptValue_Numeric)) {
					if (elements.get(offset) instanceof Referenced) {
						throw new UnexpectedTypeException((Referenced) elements.get(offset), "Number");
					} else {
						throw new UnexpectedTypeException(keyRef, elements.get(offset), "Number");
					}
				}
				if (!(elements.get(offset + 1) instanceof ScriptKeyword)) {
					if (elements.get(offset + 1) instanceof Referenced) {
						throw new UnexpectedTypeException((Referenced) elements.get(offset + 1), "Border Style");
					} else {
						throw new UnexpectedTypeException(keyRef, elements.get(offset + 1), "Border Style");
					}
				}
				int width = ((ScriptValue_Numeric) elements.get(offset++)).intValue();
				ScriptKeywordType style = ((ScriptKeyword) elements.get(offset++)).getType();
				Color color;
				if (elements.get(offset) instanceof ScriptOperator && ((ScriptOperator) elements.get(offset)).getType() == ScriptOperatorType.POUNDSIGN) {
					offset++;
					if (elements.get(offset) instanceof ScriptLine) {
						color = Stylesheets.getColor("#" + ((ScriptLine) elements.get(offset)).getString());
					} else {
						color = Stylesheets.getColor("#" + ((ScriptValue_Numeric) elements.get(offset)).longValue());
					}
				} else {
					color = Stylesheets.getColor(((ScriptLine) elements.get(offset)).getString());
					if (color == null) {
						throw new UnparseableElementException((ScriptLine) elements.get(offset), "parseStylesheet");
					}
				}
				borderElem = new StylesheetBorderElement(width, style, color);
				assert Logs.addSnapNode("Stylesheet Element Parsing", "Border stylesheet-element parsed", borderElem);
				if (key.equals("border")) {
					stylesheet.addElement(StylesheetProperty.BORDERBOTTOM, borderElem);
					stylesheet.addElement(StylesheetProperty.BORDERTOP, borderElem);
					stylesheet.addElement(StylesheetProperty.BORDERLEFT, borderElem);
					stylesheet.addElement(StylesheetProperty.BORDERRIGHT, borderElem);
				} else if (key.equals("border-bottom")) {
					stylesheet.addElement(StylesheetProperty.BORDERBOTTOM, borderElem);
				} else if (key.equals("border-top")) {
					stylesheet.addElement(StylesheetProperty.BORDERTOP, borderElem);
				} else if (key.equals("border-left")) {
					stylesheet.addElement(StylesheetProperty.BORDERLEFT, borderElem);
				} else if (key.equals("border-right")) {
					stylesheet.addElement(StylesheetProperty.BORDERRIGHT, borderElem);
				}
				i += offset - i;
				continue;
			}
			if (key.equals("background-color")) {
				assert elements.get(offset) instanceof ScriptOperator;
				assert ((ScriptOperator) elements.get(offset)).getType() == ScriptOperatorType.COLON;
				offset++;
				Color color;
				if (elements.get(offset) instanceof ScriptOperator && ((ScriptOperator) elements.get(offset)).getType() == ScriptOperatorType.POUNDSIGN) {
					offset++;
					if (elements.get(offset) instanceof ScriptLine) {
						color = Stylesheets.getColor("#" + ((ScriptLine) elements.get(offset)).getString());
					} else {
						color = Stylesheets.getColor("#" + ((ScriptValue_Numeric) elements.get(offset)).longValue());
					}
				} else {
					color = Stylesheets.getColor(((ScriptLine) elements.get(offset)).getString());
				}
				StylesheetBackgroundColorElement bgColorElem = new StylesheetBackgroundColorElement(color);
				assert Logs.addSnapNode("Stylesheet Element Parsing", "Background color stylesheet-element parsed", bgColorElem);
				stylesheet.addElement(StylesheetProperty.BACKGROUNDCOLOR, bgColorElem);
				i += offset - i;
				continue;
			}
		}
		assert Logs.closeNode();
		return stylesheet;
	}

	// Object-oriented parsing functions
	private static void preparseElements(ScriptEnvironment env, List<Object> lineList) throws ScriptException {
		assert Logs.openNode("Preparsing Elements", "Preparsing Elements (" + lineList.size() + " element(s))");
		assert Logs.addSnapNode(CommonString.ELEMENTS, lineList);
		List<Object> modifiers = new LinkedList<Object>();
		for (int i = 0; i < lineList.size(); i++) {
			Referenced element = (Referenced) lineList.get(i);
			if (element instanceof ScriptKeyword) {
				if (element.equals(ScriptKeywordType.CLASS)) {
					i++;
					if (!(lineList.get(i) instanceof ScriptLine)) {
						// If there's no name for the class, throw an exception. (Anonymous classes are not currently allowed.)
						throw new UnexpectedTypeException((Referenced) lineList.get(i), "Class name");
					}
					String name = ((ScriptLine) lineList.get(i)).getString();
					i++;
					ScriptGroup body = null;
					if (modifiers.size() == 1) {
						// We ignore public keywords for classes and treat them all as such.
						if (ScriptKeywordType.PUBLIC.equals(modifiers.get(0))) {
							modifiers.remove(0);
						} else {
							throw new UnexpectedTypeException((Referenced) modifiers.get(0), "Keyword");
						}
					} else if (modifiers.size() > 1) {
						// We don't allow modifiers for classes.
						throw new UnexpectedTypeException((Referenced) modifiers.get(0), "Keyword");
					}
					do {
						// Collect suffix modifiers until we reach our curlyGroup
						if (lineList.get(i) instanceof ScriptGroup) {
							body = (ScriptGroup) lineList.get(i);
							break;
						}
						modifiers.add(lineList.get(i));
						i++;
					} while (i < lineList.size());
					if (body == null) {
						// If there's no body, throw an exception
						throw new UnexpectedTypeException((Referenced) lineList.get(i - 1), "Class Definition Body");
					}
					List<Object> thisModifiers = new LinkedList<Object>();
					thisModifiers.addAll(modifiers);
					classParams.add(new TemplateParams(element, name, thisModifiers, body));
					env.addType(element, name);
					modifiers.clear();
					continue;
				}
			}
			modifiers.add(lineList.get(i));
		}
		if (modifiers.size() != 0) {
			throw new UnknownModifierException((Referenced) lineList.get(lineList.size() - 1), modifiers);
		}
		assert Logs.closeNode();
	}

	private static ScriptTemplate_Abstract preparseTemplate(Referenced ref, ScriptEnvironment env, List<Object> modifiers, ScriptGroup body, String className) throws ScriptException {
		assert Logs.openNode("Template Preparsing", "Preparsing Template (" + className + ")");
		assert Logs.addSnapNode("Modifiers (" + modifiers.size() + " modifier(s))", modifiers);
		assert Logs.addSnapNode("Template Body (" + body.getElements().size() + " element(s))", body);
		String extendedClass = null;
		List<ScriptValueType> implemented = new LinkedList<ScriptValueType>();
		for (int i = 0; i < modifiers.size(); i++) {
			ScriptElement obj = (ScriptElement) modifiers.get(i);
			assert Logs.addSnapNode("(" + i + ") Current element", obj);
			if (modifiers.get(i) instanceof ScriptKeyword) {
				if (modifiers.get(i).equals(ScriptKeywordType.EXTENDS)) {
					if (i >= modifiers.size() - 1 || !(modifiers.get(i + 1) instanceof ScriptLine)) {
						throw new UnexpectedTypeException(env, modifiers.get(i), "Class type");
					}
					extendedClass = ((ScriptLine) modifiers.get(i + 1)).getString();
					assert Logs.addNode("Extended class parsed (" + extendedClass + ")");
					modifiers.remove(i);
					modifiers.remove(i);
					i--;
				} else if (modifiers.get(i).equals(ScriptKeywordType.IMPLEMENTS)) {
					if (i == modifiers.size() - 1) {
						throw new UnexpectedTypeException(env, modifiers.get(i), "Interfaces");
					}
					boolean flag = false;
					while (i < modifiers.size()) {
						flag = true;
						modifiers.remove(i);
						if (!(modifiers.get(i) instanceof ScriptLine)) {
							throw new UnexpectedTypeException(env, modifiers.get(i), "Interface type");
						}
						implemented.add(ScriptValueType.createType((ScriptLine) modifiers.get(i), ((ScriptLine) modifiers.get(i)).getString()));
						modifiers.remove(i);
					}
					if (!flag) {
						throw new UnexpectedTypeException(env, obj, "Interfaces");
					}
				} else {
					throw new UnexpectedTypeException(env, modifiers.get(i), "Keyword");
				}
			}
		}
		List<Object> list = body.getElements();
		if (extendedClass == null || extendedClass.equals("")) {
			extendedClass = FauxTemplate_Object.OBJECTSTRING;
		}
		ScriptTemplate_Abstract template = ScriptTemplate.createTemplate(ref.getEnvironment(), ScriptValueType.createType(ref, className), ScriptValueType.createType(ref, extendedClass), implemented, false);
		template.setConstructing(true);
		env.advanceStack(template, NoopScriptFunction.instance());
		List<Object> elements = new LinkedList<Object>();
		boolean stylesheet = false;
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i) instanceof ScriptOperator && ((ScriptOperator) list.get(i)).getType() == ScriptOperatorType.SEMICOLON) {
				List<Object> expressionList = new LinkedList<Object>();
				expressionList.addAll(elements);
				parseExpression(env, expressionList, true, ScriptValueType.createType(env, className));
				elements.clear();
				continue;
			}
			if (list.get(i) instanceof ScriptGroup) {
				if (stylesheet) {
					List<Object> expressionList = new LinkedList<Object>();
					expressionList.addAll(elements);
					expressionList.add(list.get(i));
					parseExpression(env, expressionList, true, ScriptValueType.createType(env, className));
					elements.clear();
					stylesheet = false;
					continue;
				}
				String name = ((ScriptLine) list.get(i - 1)).getString();
				if (name.equals(className)) {
					name = "";
				}
				ScriptExecutable_ParseFunction fxn = preparseFunction(env, template, elements, (ScriptGroup) list.get(i), (ScriptGroup) list.get(++i), name);
				template.addFunction(ref, name, fxn);
				elements.clear();
				continue;
			}
			if (list.get(i) instanceof ScriptKeyword && list.get(i).equals(ScriptKeywordType.STYLESHEET)) {
				stylesheet = true;
			}
			elements.add(list.get(i));
		}
		env.retreatStack();
		template.setConstructing(false);
		assert Logs.closeNode();
		return template;
	}

	private static List<Object> removeSingleLineGroupings(List<Object> lineList, CharacterGroup group) {
		for (int i = 0; i < lineList.size(); i++) {
			if (lineList.get(i) instanceof ScriptGroup) {
				if (group.isRecursive()) {
					((ScriptGroup) lineList.get(i)).setElements(removeSingleLineGroupings(((ScriptGroup) lineList.get(i)).getElements(), group));
				}
				continue;
			}
			if (!(lineList.get(i) instanceof ScriptLine)) {
				continue;
			}
			List<Object> returnedList = removeSingleLineGroupings((ScriptLine) lineList.get(i), group);
			lineList.remove(i);
			lineList.addAll(i, returnedList);
		}
		return lineList;
	}

	private static List<Object> removeSingleLineGroupings(ScriptLine line, CharacterGroup group) {
		String openChar = group.getStart();
		String closingChar = group.getEnd();
		boolean recurse = group.isRecursive();
		int endGroup = -1;
		int beginGroup = -1;
		int offset = 0;
		String string;
		while (true) {
			endGroup = line.getString().indexOf(closingChar, offset);
			if (endGroup == -1) {
				List<Object> list = new LinkedList<Object>();
				list.add(line);
				return list;
			}
			string = line.getString().substring(0, endGroup);
			beginGroup = string.lastIndexOf(openChar);
			if (beginGroup != -1) {
				break;
			}
			offset = endGroup + 1;
		}
		assert Logs.openNode("Single-Line Grouping Removals", "Removing Single-Line Groupings (Syntax: " + openChar + "..." + closingChar + " )");
		assert Logs.addNode(line);
		assert Logs.addNode("Allowed to Recurse: " + recurse);
		List<Object> list = new LinkedList<Object>();
		List<Object> itemList = new LinkedList<Object>();
		ScriptLine newGroup = new ScriptLine(string.substring(beginGroup + openChar.length()), line, (short) (beginGroup + openChar.length()));
		assert Logs.openNode("Recursing for left-side groups.");
		list.addAll(removeSingleLineGroupings(
				new ScriptLine(line.getString().substring(0, beginGroup), line, (short) 0),
				group));
		assert Logs.closeNode();
		itemList.add(newGroup);
		list.add(new ScriptGroup(line, itemList, group));
		assert Logs.openNode("Recursing for right-side groups.");
		list.addAll(removeSingleLineGroupings(
				new ScriptLine(line.getString().substring(endGroup + closingChar.length()), line, (short) (endGroup + closingChar.length())),
				group));
		assert Logs.closeNode();
		assert Logs.closeNode();
		return list;
	}

	private static List<Object> splitByWhitespace(List<Object> list) {
		assert Logs.openNode("Split-By-Whitespace List Operations", "Splitting lines in list by whitespace (" + list.size() + " element(s))");
		assert Logs.addSnapNode("Elements", list);
		for (int i = 0; i < list.size(); i++) {
			Object obj = list.get(i);
			if (obj instanceof ScriptGroup) {
				((ScriptGroup) obj).setElements(splitByWhitespace(((ScriptGroup) obj).getElements()));
				continue;
			}
			if (!(obj instanceof ScriptLine)) {
				continue;
			}
			list.remove(i);
			list.addAll(i, splitByWhitespace((ScriptLine) obj));
		}
		assert Logs.closeNode();
		return list;
	}

	private static List<Object> splitByWhitespace(ScriptLine line) {
		assert Logs.openNode("Split-By-Whitespace Operations", "Splitting line by whitespace (" + line.getLineNumber() + ":'" + line.getString() + "')");
		assert Logs.addSnapNode("Line", line);
		List<Object> list = new LinkedList<Object>();
		if (line.getString().indexOf(" ") == -1 && line.getString().indexOf("\t") == -1) {
			list.add(line);
			assert Logs.closeNode("Line contains no whitespace.");
			return list;
		}
		String string = line.getString();
		String nextWord = "";
		int offset = 0;
		while (string.length() > 0) {
			if (string.indexOf(" ") == 0 || string.indexOf("\t") == 0) {
				if (nextWord.length() > 0) {
					list.add(line = new ScriptLine(nextWord, line, offset));
					offset = nextWord.length();
					nextWord = "";
				}
				offset++;
				string = string.substring(1);
			} else {
				nextWord += string.charAt(0);
				string = string.substring(1);
			}
		}
		if (nextWord.length() > 0) {
			list.add(new ScriptLine(nextWord, line, offset));
		}
		assert Logs.closeNode("Returning list", list);
		return list;
	}

}

class StylesheetParams {
	private Referenced reference;
	private List<Object> modifiers;
	private String name;
	private ScriptGroup body;

	public StylesheetParams(Referenced ref, List<Object> modifiers, String name, ScriptGroup body) {
		this.reference = ref;
		this.modifiers = modifiers;
		this.name = name;
		this.body = body;
	}

	public ScriptGroup getBody() {
		return this.body;
	}

	public Referenced getDebugReference() {
		return this.reference;
	}

	public List<Object> getModifiers() {
		return this.modifiers;
	}

	public String getName() {
		return this.name;
	}
}

class TemplateParams {
	private Referenced reference;
	private List<Object> modifiers;
	private String name;
	private ScriptGroup body;

	public TemplateParams(Referenced ref, String name, List<Object> modifiers, ScriptGroup body) {
		this.reference = ref;
		this.modifiers = modifiers;
		this.name = name;
		this.body = body;
	}

	public ScriptGroup getBody() {
		return this.body;
	}

	public Referenced getDebugReference() {
		return this.reference;
	}

	public List<Object> getModifiers() {
		return this.modifiers;
	}

	public String getName() {
		return this.name;
	}
}
