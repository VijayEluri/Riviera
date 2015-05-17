/**
 * 
 */
package strings;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;

import logging.Logs;

/**
 * @author Aaron Faanes
 * 
 */
public final class Strings {

	private Strings() {
		// Suppress default constructor to ensure non-instantiability.
		throw new AssertionError("Instantiation not allowed");
	}

	public static String getExtension(File f) {
		String ext = null;
		String s = f.getName();
		int i = s.lastIndexOf('.');
		if (i > 0 && i < s.length() - 1) {
			ext = s.substring(i + 1).toLowerCase();
		}
		if (ext == null) {
			ext = "";
		}
		return ext;
	}

	// Helper Function.
	public static String displayList(Collection<?> list) {
		return displayList(list, "item", "items");
	}

	// Helper Function.
	public static String displayList(Collection<?> list, String singular) {
		return displayList(list, singular, singular + "s");
	}

	public static String displayList(Collection<?> list, String singular, String plural) {
		return displayList(list, singular, plural, 0);
	}

	// Takes a list and returns a string containing its contents in a readable form.
	private static String displayList(Collection<?> list, String singular, String plural, int nestedVal) {
		StringBuilder builder = new StringBuilder("\n");
		builder.append(tab(nestedVal));
		if (list == null || list.isEmpty()) {
			builder.append("This list is empty.");
			return builder.toString();
		} else if (list.size() == 1) {
			builder.append("This list contains one " + singular);
		} else {
			builder.append("This list contains ").append(list.size()).append(' ').append(plural);
		}
		int value = 1;
		for (Object obj : list) {
			builder.append('\n').append(value).append(". ").append(tab(nestedVal));
			value++;
			if (obj instanceof Collection<?>) {
				builder.append("Nested list: ");
				builder.append(displayList((Collection<?>) obj, singular, plural, nestedVal + 1));
			} else {
				builder.append(obj);
			}
		}
		return builder.toString();
	}

	public static String displayList(Object[] array) {
		String string = "";
		if (array.length == 0) {
			string += "\nThis list is empty.";
			return string;
		} else if (array.length == 1) {
			string += "\nThis list contains one item";
		} else {
			string += "\nThis list contains " + array.length + " items";
		}
		for (int i = 0; i < array.length; i++) {
			string += "\n" + array[i];
		}
		return string;
	}

	// Returns a string with a ASCII border around the baseString. The border consists of the character provided by printChar.
	public static String printBorder(String baseString, String printChar) {
		String string = "";
		for (int i = baseString.length() + 2; i > 0; i--) {
			string += printChar;
		}
		string += "\n " + baseString + "\n";
		for (int i = baseString.length() + 2; i > 0; i--) {
			string += printChar;
		}
		string += "\n";
		return string;
	}

	// Prints a symbol n times.
	public static String printLine(String symbol, int times) {
		String string = "";
		for (int i = 0; i < times; i++) {
			string += symbol;
		}
		string += "\n";
		return string;
	}

	// Takes a string and creates a new string with a underline beneath the string provided. The underline is formed by a string of the printChar the length of the baseString.
	public static String printUnderline(String baseString, String printChar) {
		String string = baseString + "\n";
		for (int i = baseString.length(); i > 0; i--) {
			string += printChar;
		}
		string += "\n";
		return string;
	}

	public static String tab(int val) {
		return tab(val, "   ");
	}

	public static String tab(int val, String tab) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < val; i++) {
			builder.append(tab);
		}
		return builder.toString();
	}

	// Gets a single line from the provided stream. Returns null if at the end of the file.
	public static String getLineFromStream(FileReader stream) {
		if (stream == null) {
			return null;
		}
		String string = "";
		try {
			while (true) {
				int inputNum = stream.read();
				if (inputNum == -1) {
					if (string.length() == 0) {
						return null;
					} else {
						return string;
					}
				}
				char inputChar = (char) inputNum;
				if (Character.isISOControl(inputChar)) {
					if (inputChar == '\n') {
						return string;
					}
					continue;
				}
				string += inputChar;
			}
		} catch (IOException e) {
			Logs.printException(e);
			return null;
		}
	}

}
