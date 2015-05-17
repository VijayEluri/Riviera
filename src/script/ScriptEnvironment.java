package script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import gui.style.Stylesheet;
import inspect.Inspectable;
import logging.Logs;
import script.exceptions.InternalException;
import script.exceptions.TemplateAlreadyDefinedException;
import script.exceptions.VariableTypeAlreadyDefinedException;
import script.exceptions.ScriptException;
import script.operations.ScriptExecutable_CallFunction;
import script.parsing.Referenced;
import script.proxies.FauxTemplate;
import script.proxies.FauxTemplate_Ace;
import script.proxies.FauxTemplate_Archetype;
import script.proxies.FauxTemplate_ArchetypeTree;
import script.proxies.FauxTemplate_Asset;
import script.proxies.FauxTemplate_Color;
import script.proxies.FauxTemplate_DiscreteRegion;
import script.proxies.FauxTemplate_GraphicalElement;
import script.proxies.FauxTemplate_Interface;
import script.proxies.FauxTemplate_InterfaceElement;
import script.proxies.FauxTemplate_Label;
import script.proxies.FauxTemplate_Line;
import script.proxies.FauxTemplate_List;
import script.proxies.FauxTemplate_MovementEvaluator;
import script.proxies.FauxTemplate_Object;
import script.proxies.FauxTemplate_Panel;
import script.proxies.FauxTemplate_Path;
import script.proxies.FauxTemplate_Point;
import script.proxies.FauxTemplate_Rectangle;
import script.proxies.FauxTemplate_RiffDali;
import script.proxies.FauxTemplate_Scenario;
import script.proxies.FauxTemplate_Scheduler;
import script.proxies.FauxTemplate_SchedulerListener;
import script.proxies.FauxTemplate_Terrain;
import script.proxies.FauxTemplate_Terrestrial;
import script.values.ScriptFunction;
import script.values.ScriptTemplate_Abstract;
import script.values.ScriptValue;
import script.values.ScriptValueType;
import script.values.ScriptValue_Variable;

@Inspectable
public class ScriptEnvironment {
	private final Map<String, ScriptValueType> variableTypes = new HashMap<String, ScriptValueType>();
	private final Map<String, ScriptTemplate_Abstract> templates = new HashMap<String, ScriptTemplate_Abstract>();
	private final List<javax.swing.Timer> timers = new LinkedList<javax.swing.Timer>();
	private final ThreadLocal<ThreadStack> threads = new ThreadLocal<ThreadStack>() {
		@Override
		protected ThreadStack initialValue() {
			return new ThreadStack();
		}
	};

	public ScriptEnvironment() {
		this.initialize();
	}

	public void initialize() {
		assert Logs.openNode("Script environment initializations", "Initializing Script Environment");
		try {
			this.addType(null, "void", ScriptValueType.VOID);
			this.addType(null, "boolean", ScriptValueType.BOOLEAN);
			this.addType(null, "short", ScriptValueType.SHORT);
			this.addType(null, "int", ScriptValueType.INT);
			this.addType(null, "long", ScriptValueType.LONG);
			this.addType(null, "float", ScriptValueType.FLOAT);
			this.addType(null, "double", ScriptValueType.DOUBLE);
			this.addType(null, "String", ScriptValueType.STRING);
			// Faux object templates
			FauxTemplate template = new FauxTemplate_Object(this);
			this.addType(null, FauxTemplate_Object.OBJECTSTRING, template);
			template = new FauxTemplate_List(this);
			this.addType(null, FauxTemplate_List.LISTSTRING, template);
			template = new Stylesheet(this);
			this.addType(null, Stylesheet.STYLESHEETSTRING, template);
			template = new FauxTemplate_Interface(this);
			this.addType(null, FauxTemplate_Interface.INTERFACESTRING, template);
			template = new FauxTemplate_InterfaceElement(this);
			this.addType(null, FauxTemplate_InterfaceElement.INTERFACEELEMENTSTRING, template);
			template = new FauxTemplate_Label(this);
			this.addType(null, FauxTemplate_Label.LABELSTRING, template);
			template = new FauxTemplate_Rectangle(this);
			this.addType(null, FauxTemplate_Rectangle.RECTANGLESTRING, template);
			template = new FauxTemplate_GraphicalElement(this);
			this.addType(null, FauxTemplate_GraphicalElement.GRAPHICALELEMENTSTRING, template);
			template = new FauxTemplate_Point(this);
			this.addType(null, FauxTemplate_Point.POINTSTRING, template);
			template = new FauxTemplate_Line(this);
			this.addType(null, FauxTemplate_Line.LINESTRING, template);
			template = new FauxTemplate_Panel(this);
			this.addType(null, FauxTemplate_Panel.PANELSTRING, template);
			template = new FauxTemplate_DiscreteRegion(this);
			this.addType(null, FauxTemplate_DiscreteRegion.DISCRETEREGIONSTRING, template);
			template = new FauxTemplate_Color(this);
			this.addType(null, FauxTemplate_Color.COLORSTRING, template);
			template = new FauxTemplate_Asset(this);
			this.addType(null, FauxTemplate_Asset.ASSETSTRING, template);
			template = new FauxTemplate_RiffDali(this);
			this.addType(null, FauxTemplate_RiffDali.RIFFDALISTRING, template);
			template = new FauxTemplate_Terrestrial(this);
			this.addType(null, FauxTemplate_Terrestrial.TERRESTRIALSTRING, template);
			template = new FauxTemplate_Scenario(this);
			this.addType(null, FauxTemplate_Scenario.SCENARIOSTRING, template);
			template = new FauxTemplate_Scheduler(this);
			this.addType(null, FauxTemplate_Scheduler.SCHEDULERSTRING, template);
			template = new FauxTemplate_SchedulerListener(this);
			this.addType(null, FauxTemplate_SchedulerListener.SCHEDULERLISTENERSTRING, template);
			template = new FauxTemplate_MovementEvaluator(this);
			this.addType(null, FauxTemplate_MovementEvaluator.MOVEMENTEVALUATORSTRING, template);
			template = new FauxTemplate_Path(this);
			this.addType(null, FauxTemplate_Path.PATHSTRING, template);
			template = new FauxTemplate_Terrain(this);
			this.addType(null, FauxTemplate_Terrain.TERRAINSTRING, template);
			template = new FauxTemplate_Archetype(this);
			this.addType(null, FauxTemplate_Archetype.ARCHETYPESTRING, template);
			template = new FauxTemplate_Ace(this);
			this.addType(null, FauxTemplate_Ace.ACESTRING, template);
			template = new FauxTemplate_ArchetypeTree(this);
			this.addType(null, FauxTemplate_ArchetypeTree.ARCHETYPETREESTRING, template);
		} catch (ScriptException ex) {
			throw new InternalException("Exception occurred during script initialization: " + ex);
		} finally {
			assert Logs.closeNode();
		}
	}

	// Template functions
	public void addTemplate(Referenced ref, String name, ScriptTemplate_Abstract template) throws ScriptException {
		if (this.templates.containsKey(name)) {
			throw new TemplateAlreadyDefinedException(ref, name);
		}
		this.templates.put(name, template);
	}

	public ScriptTemplate_Abstract getTemplate(ScriptValueType code) {
		return this.getTemplate(this.getName(code));
	}

	@Inspectable
	public Map<String, ScriptTemplate_Abstract> getTemplates() {
		return Collections.unmodifiableMap(this.templates);
	}

	public boolean isTemplateDefined(String name) {
		return this.templates.containsKey(name);
	}

	public ScriptTemplate_Abstract getTemplate(String name) {
		return this.templates.get(name);
	}

	// Variable-type functions
	public void addType(Referenced ref, String name) throws ScriptException {
		this.addType(ref, name, new ScriptValueType(this));
	}

	public void addType(Referenced ref, String name, ScriptTemplate_Abstract template) throws ScriptException {
		this.addType(ref, name);
		this.addTemplate(ref, name, template);
	}

	public void addType(Referenced ref, String name, ScriptValueType keyword) throws ScriptException {
		assert Logs.addNode("Variable-Type Additions", "Adding variable type name to the variable-map (" + name + ")");
		if (this.variableTypes.containsKey(name)) {
			throw new VariableTypeAlreadyDefinedException(ref, name);
		}
		this.variableTypes.put(name, keyword);
	}

	public void addVariableToStack(String name, ScriptValue_Variable var) throws ScriptException {
		this.threads.get().addVariable(name, var);
	}

	public void advanceNestedStack() {
		this.threads.get().advanceNestedStack();
	}

	// Stack functions
	public void advanceStack(ScriptTemplate_Abstract template, ScriptFunction fxn) throws ScriptException {
		if (fxn == null) {
			throw new NullPointerException("fxn must not be null");
		}
		this.threads.get().advanceStack(template, fxn);
	}

	public ScriptFunction getCurrentFunction() {
		return this.threads.get().getCurrentFunction();
	}

	public ScriptTemplate_Abstract getCurrentObject() {
		return this.threads.get().getCurrentObject();
	}

	public ScriptValue_Variable getVariableFromStack(String name) {
		return this.threads.get().getVariableFromStack(name);
	}

	public void retreatNestedStack() {
		this.threads.get().retreatNestedStack();
	}

	public void retreatStack() {
		this.threads.get().retreatStack();
	}

	public void clearStacks() {
		this.threads.set(new ThreadStack());
	}

	public void execute() {
		assert Logs.openNode("Executing Script-Environment (Default Run)");
		try {
			this.clearStacks();
			for (ScriptTemplate_Abstract template : this.templates.values()) {
				template.initialize();
			}
			List<ScriptValue> params = Collections.emptyList();
			List<String> templateNames = new ArrayList<String>();
			for (Map.Entry<String, ScriptTemplate_Abstract> entry : this.templates.entrySet()) {
				if (entry.getValue().getFunction("main", params) != null) {
					templateNames.add(entry.getKey());
				}
			}
			if (templateNames.isEmpty()) {
				JOptionPane.showMessageDialog(null, "No classes compiled are executable.", "No Executable Class", JOptionPane.WARNING_MESSAGE);
				return;
			}
			Object selection;
			if (templateNames.size() > 1) {
				selection = JOptionPane.showInputDialog(null, "Select the appropriate class to run from", "Multiple Executable Classes", JOptionPane.QUESTION_MESSAGE, null, templateNames.toArray(), templateNames.get(0));
			} else {
				selection = templateNames.get(0);
			}
			if (selection == null) {
				return;
			}
			assert Logs.addNode(this);
			ScriptExecutable_CallFunction.callFunction(this, null, this.getTemplate((String) selection), "main", params);
		} catch (ScriptException ex) {
			Logs.printException(ex);
		} catch (InternalException ex) {
			Logs.printException(ex);
		} finally {
			assert Logs.closeNode();
		}
	}

	public String getName(ScriptValueType keyword) {
		assert keyword != null;
		for (Map.Entry<String, ScriptValueType> entry : this.variableTypes.entrySet()) {
			if (keyword.equals(entry.getValue())) {
				return entry.getKey();
			}
		}
		throw new IllegalArgumentException("Name not found for keyword");
	}

	public ScriptValueType getType(String name) {
		return this.variableTypes.get(name);
	}

	// Variable functions
	public ScriptValue_Variable retrieveVariable(String name) throws ScriptException {
		assert Logs.openNode("Variable Retrievals", "Retrieving Variable (" + name + ")");
		ScriptValue_Variable value = null;
		if (value == null) {
			assert Logs.addSnapNode("Checking current variable stack", this.threads.get());
			value = this.getVariableFromStack(name);
		}
		if (value == null) {
			assert Logs.openNode("Checking current object for valid variable");
			assert Logs.addNode(this.getCurrentObject());
			value = this.getCurrentObject().getVariable(name);
			assert Logs.closeNode();
		}
		if (value == null) {
			assert Logs.openNode("Checking static template stack");
			assert Logs.addNode(this.getTemplate(name));
			if (this.getTemplate(name) != null) {
				value = this.getTemplate(name).getStaticReference();
			}
			assert Logs.closeNode();
		}
		if (value == null) {
			assert Logs.addNode("Value not found");
		} else {
			assert Logs.addSnapNode("Value found", value);
		}
		assert Logs.closeNode();
		return value;
	}

	public void addTimer(javax.swing.Timer timer) {
		this.timers.add(timer);
	}

	public void stopExecution() {
		for (javax.swing.Timer timer : this.timers) {
			timer.stop();
		}
		this.timers.clear();
	}

	public void reset() {
		assert Logs.openNode("Resetting Environment");
		this.variableTypes.clear();
		this.templates.clear();
		this.clearStacks();
		System.gc();
		this.initialize();
		assert Logs.closeNode();
	}

}
