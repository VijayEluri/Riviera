/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.application;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

import org.jdesktop.application.ResourceConverter.ResourceConverterException;

import com.bluespot.reflection.Primitive;

/**
 * A read-only encapsulation of one or more ResourceBundles that adds automatic
 * string conversion, support for field and Swing component property injection,
 * string resource variable substitution, and chaining.
 * <p>
 * ResourceMaps are typically obtained with the {@code ApplicationContext}
 * {@link ApplicationContext#getResourceMap getResourceMap} method which lazily
 * creates per Application, package, and class ResourceMaps that are linked
 * together with the ResourceMap <tt>parent</tt> property.
 * <p>
 * An individual ResourceMap provides read-only access to all of the resources
 * defined by the ResourceBundles named when the ResourceMap was created as well
 * as all of its parent ResourceMaps. Resources are retrieved with the
 * <tt>getObject</tt> method which requires both the name of the resource and
 * its expected type. The latter is used to convert strings if neccessary.
 * Converted values are cached. As a convenience, <tt>getObject</tt> wrapper
 * methods for common GUI types, like <tt>getFont</tt>, and <tt>getColor</tt>,
 * are provided.
 * <p>
 * The <tt>getObject</tt> method scans raw string resource values for
 * <tt>${resourceName}</tt> variable substitutions before performing string
 * conversion. Variables named this way can refer to String resources defined
 * anywhere in their ResourceMap or any parent ResourceMap. The special variable
 * <tt>${null}</tt> means that the value of the resource will be null.
 * <p>
 * ResourceMaps can be used to "inject" resource values into Swing component
 * properties and into object fields. The <tt>injectComponents</tt> method uses
 * Component names ({@link Component#setName}) to match resources names with
 * properties. The <tt>injectFields</tt> method sets fields that have been
 * tagged with the <tt>&#064;Resource</tt> annotation to the value of resources
 * with the same name.
 * 
 * @author Hans Muller (Hans.Muller@Sun.COM)
 * @see #injectComponents
 * @see #injectFields
 * @see ResourceConverter
 * @see ResourceBundle
 */
public class ResourceMap {
    private static Logger logger = Logger.getLogger(ResourceMap.class.getName());
    private final static Object nullResource = new String("null resource");
    private final ClassLoader classLoader;
    private final ResourceMap parent;
    private final List<String> bundleNames;
    private final String resourcesDir;
    private Map<String, Object> bundlesMapP = null; // see getBundlesMap()
    private Locale locale = Locale.getDefault(); // ...
    private Set<String> bundlesMapKeysP = null; // set getBundlesMapKeys()
    private boolean bundlesLoaded = false; // ResourceBundles are loaded lazily

    /**
     * Creates a ResourceMap that contains all of the resources defined in the
     * named {@link ResourceBundle}s as well as (recursively) the
     * <tt>parent</tt> ResourceMap. The <tt>parent</tt> may be null. Typically
     * just one ResourceBundle is specified however one might name additional
     * ResourceBundles that contain platform or Swing look and feel specific
     * resources. When multiple bundles are named, a resource defined in
     * bundle<sub>n</sub> will overide the same resource defined in
     * bundles<sub>0..n-1</sub>. In other words bundles named later in the
     * argument list take precendence over the bundles named earlier.
     * <p>
     * ResourceBundles are loaded with the specified ClassLoader. If
     * <tt>classLoader</tt> is null, an IllegalArgumentException is thrown.
     * <p>
     * At least one bundleName must be specified and all of the bundleNames must
     * be non-empty strings, or an IllegalArgumentException is thrown. The
     * bundles are listed in priority order, highest priority first. In other
     * words, resources in the the first ResourceBundle named first, shadow
     * resources with the same name later in the list.
     * <p>
     * All of the bundleNames must share a common package prefix. The package
     * prefix implicitly specifies the resources directory (see
     * {@link #getResourcesDir}). For example, the resources directory for
     * bundle names "myapp.resources.foo" and "myapp.resources.bar", would be
     * "myapp/resources/". If bundle names don't share a common package prefix,
     * then an IllegalArgumentException is thrown.
     * 
     * @param parent
     *            parent ResourceMap or null
     * @param classLoader
     *            the ClassLoader to be used to load the ResourceBundle
     * @param bundleNames
     *            names of the ResourceBundle to be loaded
     * @throws IllegalArgumentException
     *             if classLoader or any bundleName is null, if no bundleNames
     *             are specified, if any bundleName is an empty (zero length)
     *             String, or if all of the bundleNames don't have a common
     *             package prefix
     * @see ResourceBundle
     * @see #getParent
     * @see #getClassLoader
     * @see #getResourcesDir
     * @see #getBundleNames
     */
    public ResourceMap(final ResourceMap parent, final ClassLoader classLoader, final List<String> bundleNames) {
        if (classLoader == null) {
            throw new IllegalArgumentException("null ClassLoader");
        }
        if ((bundleNames == null) || (bundleNames.size() == 0)) {
            throw new IllegalArgumentException("no bundle specified");
        }
        for (final String bn : bundleNames) {
            if ((bn == null) || (bn.length() == 0)) {
                throw new IllegalArgumentException("invalid bundleName: \"" + bn + "\"");
            }
        }
        final String bpn = this.bundlePackageName(bundleNames.get(0));
        for (final String bn : bundleNames) {
            if (!bpn.equals(this.bundlePackageName(bn))) {
                throw new IllegalArgumentException("bundles not colocated: \"" + bn + "\" != \"" + bpn + "\"");
            }
        }
        this.parent = parent;
        this.classLoader = classLoader;
        this.bundleNames = Collections.unmodifiableList(new ArrayList<String>(bundleNames));
        this.resourcesDir = bpn.replace(".", "/") + "/";
    }

    private String bundlePackageName(final String bundleName) {
        final int i = bundleName.lastIndexOf(".");
        return (i == -1) ? "" : bundleName.substring(0, i);
    }

    /**
     * Just a convenience version of the constructor for the common case where
     * there's only one bundle name. Defined as:
     * <code>this(parent, classLoader, Arrays.asList(bundleNames))</code>.
     */
    public ResourceMap(final ResourceMap parent, final ClassLoader classLoader, final String... bundleNames) {
        this(parent, classLoader, Arrays.asList(bundleNames));
    }

    /**
     * Returns the parent ResourceMap, or null. Logically, this ResourceMap
     * contains all of the resources defined here and (recursively) in the
     * parent.
     * 
     * @return the parent ResourceMap or null
     */
    public ResourceMap getParent() {
        return this.parent;
    }

    /**
     * Returns the names of the ResourceBundles that define the resources
     * contained by this ResourceMap.
     * 
     * @return the names of the ResourceBundles in this ResourceMap
     */
    public List<String> getBundleNames() {
        return this.bundleNames;
    }

    /**
     * Returns the ClassLoader used to load the ResourceBundles for this
     * ResourceMap.
     * 
     * @return the classLoader constructor argument
     */
    public ClassLoader getClassLoader() {
        return this.classLoader;
    }

    /**
     * Returns the resources directory that contains all of the ResourceBundles
     * in this ResourceMap. It can be used with the the classLoader property to
     * load files from the resources directory. For example:
     * 
     * <pre>
     * String filename = myResourceMap.getResourcesDir() + &quot;myIcon.png&quot;;
     * URL url = myResourceMap.getClassLoader().getResource(filename);
     * new ImageIcon(iconURL);
     * </pre>
     * 
     * @return the the resources directory for this ResourceMap
     */
    public String getResourcesDir() {
        return this.resourcesDir;
    }

    /*
     * Lazily flattens all of the ResourceBundles named in bundleNames into a
     * single Map - bundlesMapP. The bundleNames list is in priority order, the
     * first entry shadows later entries.
     */
    private synchronized Map<String, Object> getBundlesMap() {
        // If the default locale has changed, then reload
        final Locale defaultLocale = Locale.getDefault();
        if (this.locale != defaultLocale) {
            this.bundlesLoaded = false;
            this.locale = defaultLocale;
        }
        if (!this.bundlesLoaded) {
            final Map<String, Object> bundlesMap = new ConcurrentHashMap<String, Object>();
            for (int i = this.bundleNames.size() - 1; i >= 0; i--) {
                try {
                    final String bundleName = this.bundleNames.get(i);
                    final ResourceBundle bundle = ResourceBundle.getBundle(bundleName, this.locale, this.classLoader);
                    final Enumeration<String> keys = bundle.getKeys();
                    while (keys.hasMoreElements()) {
                        final String key = keys.nextElement();
                        bundlesMap.put(key, bundle.getObject(key));
                    }
                } catch (final MissingResourceException ignore) {
                    /*
                     * bundleName is just a location to check, it's not
                     * guaranteed to name a ResourceBundle
                     */
                }
            }
            this.bundlesMapP = bundlesMap;
            this.bundlesLoaded = true;
        }
        return this.bundlesMapP;
    }

    private void checkNullKey(final String key) {
        if (key == null) {
            throw new IllegalArgumentException("null key");
        }
    }

    private synchronized Set<String> getBundlesMapKeys() {
        if (this.bundlesMapKeysP == null) {
            final Set<String> allKeys = new HashSet<String>(this.getResourceKeySet());
            final ResourceMap parent = this.getParent();
            if (parent != null) {
                allKeys.addAll(parent.keySet());
            }
            this.bundlesMapKeysP = Collections.unmodifiableSet(allKeys);
        }
        return this.bundlesMapKeysP;
    }

    /**
     * Return a unmodifiable {@link Set} that contains all of the keys in this
     * ResourceMap and (recursively) its parent ResourceMaps.
     * 
     * @return all of the keys in this ResourceMap and its parent
     * @see #getParent
     */
    public Set<String> keySet() {
        return this.getBundlesMapKeys();
    }

    /**
     * Returns true if this resourceMap or its parent (recursively) contains the
     * specified key.
     * 
     * @return true if this resourceMap or its parent contains the specified
     *         key.
     * @see #getParent
     * @see #keySet
     */
    public boolean containsKey(final String key) {
        this.checkNullKey(key);
        if (this.containsResourceKey(key)) {
            return true;
        }
        return (this.parent != null) ? this.parent.containsKey(key) : false;
    }

    /**
     * Unchecked exception thrown by {@link #getObject} when resource lookup
     * fails, for example because string conversion fails. This is not a missing
     * resource exception. If a resource isn't defined for a particular key,
     * getObject does not throw an exception.
     * 
     * @see #getObject
     */
    public static class LookupException extends RuntimeException {
        /**
         * 
         */
        private static final long serialVersionUID = 1904156427722754796L;
        private final Class type;
        private final String key;

        /**
         * Constructs an instance of this class with some useful information
         * about the failure.
         * 
         * @param msg
         *            the detail message
         * @param type
         *            the type of the resource
         * @param key
         *            the name of the resource
         */
        public LookupException(final String msg, final String key, final Class type) {
            super(String.format("%s: resource %s, type %s", msg, key, type));
            this.key = key;
            this.type = type;
        }

        /**
         * Returns the type of the resource for which lookup failed.
         * 
         * @return the resource type
         */
        public Class getType() {
            return this.type;
        }

        /**
         * Returns the type of the name of resource for which lookup failed.
         * 
         * @return the resource name
         */
        public String getKey() {
            return this.key;
        }
    }

    /**
     * By default this method is used by {@code keySet} to get the names of the
     * resources defined in this ResourceMap. This method lazily loads the
     * ResourceBundles named by the constructor.
     * <p>
     * The protected {@code getResource}, {@code putResource}, and {@code
     * containsResourceKey}, {@code getResourceKeySet} abstract the internal
     * representation of this ResourceMap's list of {@code ResourceBundles}.
     * Most applications can ignore them.
     * 
     * @return the names of the resources defined in this ResourceMap
     * @see #getResource
     * @see #putResource
     * @see #containsResourceKey
     */
    protected Set<String> getResourceKeySet() {
        final Map<String, Object> bundlesMap = this.getBundlesMap();
        if (bundlesMap == null) {
            return Collections.emptySet();
        } else {
            return bundlesMap.keySet();
        }
    }

    /**
     * By default this method is used by {@code getObject} to see if a resource
     * is defined by this ResourceMap. This method lazily loads the
     * ResourceBundles named by the constructor.
     * <p>
     * The protected {@code getResource}, {@code putResource}, and {@code
     * containsResourceKey}, {@code getResourceKeySet} abstract the internal
     * representation of this ResourceMap's list of {@code ResourceBundles}.
     * Most applications can ignore them.
     * <p>
     * If {@code key} is null, an IllegalArgumentException is thrown.
     * 
     * @param key
     *            the name of the resource
     * @return true if a resource named {@code key} is defined in this
     *         ResourceMap
     * @see #getResource
     * @see #putResource
     * @see #getResourceKeySet
     */
    protected boolean containsResourceKey(final String key) {
        this.checkNullKey(key);
        final Map<String, Object> bundlesMap = this.getBundlesMap();
        return (bundlesMap != null) && bundlesMap.containsKey(key);
    }

    /**
     * By default this method is used by {@code getObject} to look up resource
     * values in the internal representation of the {@code ResourceBundles}
     * named when this ResourceMap was constructed. If a resource named {@code
     * key} is {@link #containsResourceKey defined} then its value is returned,
     * otherwise null. The {@code getResource} method lazily loads the
     * ResourceBundles named by the constructor.
     * <p>
     * The protected {@code getResource}, {@code putResource}, and {@code
     * containsResourceKey}, {@code getResourceKeySet} abstract the internal
     * representation of this ResourceMap's list of {@code ResourceBundles}.
     * Most applications can ignore them.
     * <p>
     * If {@code key} is null, an IllegalArgumentException is thrown.
     * 
     * @param key
     *            the name of the resource
     * @return the value of the resource named {@code key} (can be null)
     * @see #putResource
     * @see #containsResourceKey
     * @see #getResourceKeySet
     */
    protected Object getResource(final String key) {
        this.checkNullKey(key);
        final Map<String, Object> bundlesMap = this.getBundlesMap();
        final Object value = (bundlesMap != null) ? bundlesMap.get(key) : null;
        return (value == ResourceMap.nullResource) ? null : value;
    }

    /**
     * By default this method is used by {@code getObject} to cache values that
     * have been retrieved, evaluated (as in ${key} expressions), and string
     * converted. A subclass could override this method to defeat caching or to
     * refine the caching strategy. The {@code putResource} method lazily loads
     * ResourceBundles.
     * <p>
     * The protected {@code getResource}, {@code putResource}, and {@code
     * containsResourceKey}, {@code getResourceKeySet} abstract the internal
     * representation of this ResourceMap's list of {@code ResourceBundles}.
     * Most applications can ignore them.
     * <p>
     * If {@code key} is null, an IllegalArgumentException is thrown.
     * 
     * @param key
     *            the name of the resource
     * @param value
     *            the value of the resource (can be null)
     * @see #getResource
     * @see #containsResourceKey
     * @see #getResourceKeySet
     */
    protected void putResource(final String key, final Object value) {
        this.checkNullKey(key);
        final Map<String, Object> bundlesMap = this.getBundlesMap();
        if (bundlesMap != null) {
            bundlesMap.put(key, (value == null) ? ResourceMap.nullResource : value);
        }
    }

    /**
     * Returns the value of the resource named <tt>key</tt>, or null if no
     * resource with that name exists. A resource exists if it's defined in this
     * ResourceMap or (recursively) in the ResourceMap's parent.
     * <p>
     * String resources may contain variables that name other resources. Each
     * <tt>${variable-key}</tt> variable is replaced with the value of a string
     * resource named <tt>variable-key</tt>. For example, given the following
     * resources:
     * 
     * <pre>
     * Application.title = My Application
     * ErrorDialog.title = Error: ${application.title}
     * WarningDialog.title = Warning: ${application.title}
     * </pre>
     * 
     * The value of <tt>"WarningDialog.title"</tt> would be
     * <tt>"Warning: My Application"</tt>. To include "${" in a resource, insert
     * a backslash before the "$". For example, the value of <tt>escString</tt>
     * in the example below, would be <tt>"${hello}"</tt>:
     * 
     * <pre>
     * escString = \\${hello}
     * </pre>
     * 
     * Note that, in a properties file, the backslash character is used for line
     * continuation, so we've had to escape that too. If the value of a resource
     * is the special variable <tt>${null}</tt>, then the resource will be
     * removed from this ResourceMap.
     * <p>
     * The value returned by getObject will be of the specified type. If a
     * string valued resource exists for <tt>key</tt>, and <tt>type</tt> is not
     * String.class, the value will be converted using a ResourceConverter and
     * the ResourceMap entry updated with the converted value.
     * <p>
     * If the named resource exists and an error occurs during lookup, then a
     * ResourceMap.LookupException is thrown. This can happen if string
     * conversion fails, or if resource parameters can't be evaluated, or if the
     * existing resource is of the wrong type.
     * <p>
     * An IllegalArgumentException is thrown if key or type are null.
     * 
     * @param key
     *            resource name
     * @param providedType
     *            resource type
     * @see #getParent
     * @see ResourceConverter#forType
     * @see ResourceMap.LookupException
     * @throws LookupException
     *             if an error occurs during lookup or string conversion
     * @throws IllegalArgumentException
     *             if <tt>key</tt> or <tt>type</tt> are null
     */
    public Object getObject(final String key, final Class<?> providedType) {
        if (key == null) {
            throw new NullPointerException("key is null");
        }
        if (providedType == null) {
            throw new NullPointerException("providedType is null");
        }
        Class<?> type = providedType;
        if (type.isPrimitive()) {
            type = Primitive.asBoxed(type);
            assert type != null : "type was null after conversion from " + providedType;
        }
        Object value = null;
        ResourceMap resourceMapNode = this;
        /*
         * Find the ResourceMap bundlesMap that contains a non-null value for
         * the specified key, first check this ResourceMap, then its parents.
         */
        while (resourceMapNode != null) {
            if (resourceMapNode.containsResourceKey(key)) {
                value = resourceMapNode.getResource(key);
                break;
            }
            resourceMapNode = resourceMapNode.getParent();
        }
        if (resourceMapNode == null) {
            throw new AssertionError("resourceMapNode is null");
        }
        /*
         * If we've found a String expression then replace any ${key} variables,
         * and then reset the the original resourceMapNode entry.
         */
        if ((value instanceof String) && ((String) value).contains("${")) {
            value = this.evaluateStringExpression((String) value);
            resourceMapNode.putResource(key, value);
        }

        /*
         * If the value we've found in resourceMapNode is the expected type,
         * then we're done. If the expected type is primitive and the value is
         * the corresponding object type then we're done too. Otherwise, if it's
         * a String, then try and convert the String and replace the original
         * resourceMapNode entry, otherwise return null.
         */
        if (value != null) {
            final Class<?> valueClass = value.getClass();
            if (!providedType.isAssignableFrom(valueClass)) {
                if (value instanceof String) {
                    final ResourceConverter stringConverter = ResourceConverter.forType(providedType);
                    if (stringConverter != null) {
                        final String sValue = (String) value;
                        try {
                            value = stringConverter.parseString(sValue, resourceMapNode);
                            resourceMapNode.putResource(key, value);
                        } catch (final ResourceConverterException e) {
                            final String msg = "string conversion failed";
                            final LookupException lfe = new LookupException(msg, key, providedType);
                            lfe.initCause(e);
                            throw lfe;
                        }
                    } else {
                        final String msg = "no StringConverter for required type";
                        throw new LookupException(msg, key, providedType);
                    }
                } else {
                    final String msg = "named resource has wrong type";
                    throw new LookupException(msg, key, providedType);
                }
            }
        }
        return value;
    }

    /*
     * Given the following resources:
     * 
     * hello = Hello world = World place = ${world}
     * 
     * The value of evaluateStringExpression("${hello} ${place}") would be
     * "Hello World". The value of ${null} is null.
     */
    private String evaluateStringExpression(final String expr) {
        if (expr.trim().equals("${null}")) {
            return null;
        }
        final StringBuffer value = new StringBuffer();
        int i0 = 0, i1 = 0;
        while ((i1 = expr.indexOf("${", i0)) != -1) {
            if ((i1 == 0) || ((i1 > 0) && (expr.charAt(i1 - 1) != '\\'))) {
                final int i2 = expr.indexOf("}", i1);
                if ((i2 != -1) && (i2 > i1 + 2)) {
                    final String k = expr.substring(i1 + 2, i2);
                    final String v = this.getString(k);
                    value.append(expr.substring(i0, i1));
                    if (v != null) {
                        value.append(v);
                    } else {
                        final String msg = String.format("no value for \"%s\" in \"%s\"", k, expr);
                        throw new LookupException(msg, k, String.class);
                    }
                    i0 = i2 + 1; // skip trailing "}"
                } else {
                    final String msg = String.format("no closing brace in \"%s\"", expr);
                    throw new LookupException(msg, "<not found>", String.class);
                }
            } else { // we've found an escaped variable - "\${"
                value.append(expr.substring(i0, i1 - 1));
                value.append("${");
                i0 = i1 + 2; // skip past "${"
            }
        }
        value.append(expr.substring(i0));
        return value.toString();
    }

    /**
     * If no arguments are specified, return the String value of the resource
     * named <tt>key</tt>. This is equivalent to calling
     * <tt>getObject(key, String.class)</tt> If arguments are provided, then the
     * type of the resource named <tt>key</tt> is assumed to be
     * {@link String#format(String, Object...) format} string, which is applied
     * to the arguments if it's non null. For example, given the following
     * resources
     * 
     * <pre>
     * hello = Hello % s
     * </pre>
     * 
     * then the value of <tt>getString("hello", "World")</tt> would be
     * <tt>"Hello World"</tt>.
     * 
     * @return the String value of the resource named <tt>key</tt>
     * @throws LookupException
     *             if an error occurs during lookup or string conversion
     * @throws IllegalArgumentException
     *             if <tt>key</tt> is null
     * @see #getObject
     * @see String#format(String, Object...)
     */
    public String getString(final String key, final Object... args) {
        if (args.length == 0) {
            return (String) this.getObject(key, String.class);
        }
        final String format = (String) this.getObject(key, String.class);
        return (format == null) ? null : String.format(format, args);
    }

    /**
     * A convenience method that's shorthand for calling:
     * <tt>getObject(key, Boolean.class)</tt>.
     * 
     * @param key
     *            the name of the resource
     * @throws LookupException
     *             if an error occurs during lookup or string conversion
     * @throws IllegalArgumentException
     *             if <tt>key</tt> is null
     * @return the Boolean value of the resource named key
     * @see #getObject
     */
    public final Boolean getBoolean(final String key) {
        return (Boolean) this.getObject(key, Boolean.class);
    }

    /**
     * A convenience method that's shorthand for calling:
     * <tt>getObject(key, Integer.class)</tt>.
     * 
     * @param key
     *            the name of the resource
     * @throws LookupException
     *             if an error occurs during lookup or string conversion
     * @throws IllegalArgumentException
     *             if <tt>key</tt> is null
     * @return the Integer value of the resource named key
     * @see #getObject
     */
    public final Integer getInteger(final String key) {
        return (Integer) this.getObject(key, Integer.class);
    }

    /**
     * A convenience method that's shorthand for calling:
     * <tt>getObject(key, Long.class)</tt>.
     * 
     * @param key
     *            the name of the resource
     * @throws LookupException
     *             if an error occurs during lookup or string conversion
     * @throws IllegalArgumentException
     *             if <tt>key</tt> is null
     * @return the Long value of the resource named key
     * @see #getObject
     */
    public final Long getLong(final String key) {
        return (Long) this.getObject(key, Long.class);
    }

    /**
     * A convenience method that's shorthand for calling:
     * <tt>getObject(key, Short.class)</tt>.
     * 
     * @param key
     *            the name of the resource
     * @throws LookupException
     *             if an error occurs during lookup or string conversion
     * @throws IllegalArgumentException
     *             if <tt>key</tt> is null
     * @return the Short value of the resource named key
     * @see #getObject
     */
    public final Short getShort(final String key) {
        return (Short) this.getObject(key, Short.class);
    }

    /**
     * A convenience method that's shorthand for calling:
     * <tt>getObject(key, Byte.class)</tt>.
     * 
     * @param key
     *            the name of the resource
     * @throws LookupException
     *             if an error occurs during lookup or string conversion
     * @throws IllegalArgumentException
     *             if <tt>key</tt> is null
     * @return the Byte value of the resource named key
     * @see #getObject
     */
    public final Byte getByte(final String key) {
        return (Byte) this.getObject(key, Byte.class);
    }

    /**
     * A convenience method that's shorthand for calling:
     * <tt>getObject(key, Float.class)</tt>.
     * 
     * @param key
     *            the name of the resource
     * @throws LookupException
     *             if an error occurs during lookup or string conversion
     * @throws IllegalArgumentException
     *             if <tt>key</tt> is null
     * @return the Float value of the resource named key
     * @see #getObject
     */
    public final Float getFloat(final String key) {
        return (Float) this.getObject(key, Float.class);
    }

    /**
     * A convenience method that's shorthand for calling:
     * <tt>getObject(key, Double.class)</tt>.
     * 
     * @param key
     *            the name of the resource
     * @throws LookupException
     *             if an error occurs during lookup or string conversion
     * @throws IllegalArgumentException
     *             if <tt>key</tt> is null
     * @return the Double value of the resource named key
     * @see #getObject
     */
    public final Double getDouble(final String key) {
        return (Double) this.getObject(key, Double.class);
    }

    /**
     * 
     * A convenience method that's shorthand for calling:
     * <tt>getObject(key, Icon.class)</tt>. This method relies on the ImageIcon
     * ResourceConverter that's registered by this class. See
     * {@link #getImageIcon} for more information.
     * 
     * 
     * @param key
     *            the name of the resource
     * @return the Icon value of the resource named key
     * @see #getObject
     * @throws LookupException
     *             if an error occurs during lookup or string conversion
     * @throws IllegalArgumentException
     *             if <tt>key</tt> is null
     */
    public final Icon getIcon(final String key) {
        return (Icon) this.getObject(key, Icon.class);
    }

    /**
     * 
     * A convenience method that's shorthand for calling:
     * <tt>getObject(key, ImageIcon.class)</tt>. This method relies on the
     * ImageIcon ResourceConverter that's registered by this class.
     * <p>
     * If the resource named <tt>key</tt> is a String, it should name an image
     * file to be found in the resources subdirectory that also contains the
     * ResourceBundle (typically a ".properties" file) that was used to create
     * the corresponding ResourceMap.
     * <p>
     * For example, given the ResourceMap produced by
     * <tt>Application.getClass(com.mypackage.MyClass.class)</tt>, and a
     * ResourceBundle called <tt>MyClass.properties</tt> in
     * <tt>com.mypackage.resources</tt>:
     * 
     * <pre>
     * openIcon = myOpenIcon.png
     * </pre>
     * 
     * then <tt>resourceMap.getIcon("openIcon")</tt> would load the image file
     * called "myOpenIcon.png" from the resources subdirectory, effectively like
     * this:
     * 
     * <pre>
     * String filename = myResourceMap.getResourcesDir() + &quot;myOpenIcon.png&quot;;
     * URL url = myResourceMap.getClassLoader().getResource(filename);
     * new ImageIcon(iconURL);
     * </pre>
     * 
     * 
     * @param key
     *            the name of the resource
     * @return the ImageIcon value of the resource named key
     * @see #getObject
     * @throws LookupException
     *             if an error occurs during lookup or string conversion
     * @throws IllegalArgumentException
     *             if <tt>key</tt> is null
     */
    public final ImageIcon getImageIcon(final String key) {
        return (ImageIcon) this.getObject(key, ImageIcon.class);
    }

    /**
     * 
     * A convenience method that's shorthand for calling:
     * <tt>getObject(key, Font.class)</tt>. This method relies on the Font
     * ResourceConverter that's registered by this class. Font resources may be
     * defined with strings that are recognized by {@link Font#decode},
     * <tt><i>face</i>-<i>STYLE</i>-<i>size</i></tt>. For example:
     * 
     * <pre>
     * myFont = Arial - PLAIN - 12
     * </pre>
     * 
     * 
     * @param key
     *            the name of the resource
     * @return the Font value of the resource named key
     * @see #getObject
     * @see ResourceConverter#forType
     * @see Font#decode
     * @throws LookupException
     *             if an error occurs during lookup or string conversion
     */
    public final Font getFont(final String key) {
        return (Font) this.getObject(key, Font.class);
    }

    /**
     * 
     * A convenience method that's shorthand for calling:
     * <tt>getObject(key, Color.class)</tt>. This method relies on the Color
     * ResourceConverter that's registered by this class. It defines an improved
     * version of <tt>Color.decode()</tt> that supports colors with an alpha
     * channel and comma separated RGB[A] values. Legal format for color
     * resources are:
     * 
     * <pre>
     * myHexRGBColor = #RRGGBB 
     * myHexAlphaRGBColor = #AARRGGBB
     * myRGBColor = R, G, B
     * myAlphaRGBColor = R, G, B, A
     * </pre>
     * 
     * The first two examples, with the leading "#" encode the color with 3 or 4
     * hex values and the latter with integer values between 0 and 255. In both
     * cases the value represented by "A" is the color's (optional) alpha
     * channel.
     * 
     * 
     * @param key
     *            the name of the resource
     * @return the Color value of the resource named key
     * @see #getObject
     * @see ResourceConverter#forType
     * @throws LookupException
     *             if an error occurs during lookup or string conversion
     * @throws IllegalArgumentException
     *             ResourceConverter is null
     */
    public final Color getColor(final String key) {
        return (Color) this.getObject(key, Color.class);
    }

    /**
     * 
     * A convenience method that's shorthand for calling:
     * <tt>getObject(key, KeyStroke.class)</tt>. This method relies on the
     * KeyStroke ResourceConverter that's registered by this class and uses
     * {@link KeyStroke#getKeyStroke} to convert strings.
     * 
     * 
     * @param key
     *            the name of the resource
     * @return the KeyStroke value of the resource named key
     * @see #getObject
     * @see KeyStroke#getKeyStroke
     * @throws LookupException
     *             if an error occurs during lookup or string conversion
     * @throws IllegalArgumentException
     *             if <tt>key</tt> is null
     */
    public final KeyStroke getKeyStroke(final String key) {
        return (KeyStroke) this.getObject(key, KeyStroke.class);
    }

    /**
     * A convenience method that's shorthand for calling:
     * <tt>getKeyStroke(key).getKeyCode()</tt>. If there's no resource named
     * <tt>key</tt> then null is returned.
     * 
     * @param key
     *            the name of the resource
     * @throws LookupException
     *             if an error occurs during lookup or string conversion
     * @throws IllegalArgumentException
     *             if <tt>key</tt> is null
     * @return the KeyCode value of the resource named key
     * @see #getObject
     */
    public Integer getKeyCode(final String key) {
        final KeyStroke ks = this.getKeyStroke(key);
        return (ks != null) ? new Integer(ks.getKeyCode()) : null;
    }

    /**
     * Unchecked exception thrown by {@link #injectComponent} and
     * {@link #injectComponents} when a property value specified by a resource
     * can not be set.
     * 
     * @see #injectComponent
     * @see #injectComponents
     */
    public static class PropertyInjectionException extends RuntimeException {
        /**
         * 
         */
        private static final long serialVersionUID = 1538641928469899229L;
        private final String key;
        private final Component component;
        private final String propertyName;

        /**
         * Constructs an instance of this class with some useful information
         * about the failure.
         * 
         * @param msg
         *            the detail message
         * @param key
         *            the name of the resource
         * @param component
         *            the component whose property couldn't be set
         * @param propertyName
         *            the name of the component property
         */
        public PropertyInjectionException(final String msg, final String key, final Component component,
                final String propertyName) {
            super(String.format("%s: resource %s, property %s, component %s", msg, key, propertyName, component));
            this.key = key;
            this.component = component;
            this.propertyName = propertyName;
        }

        /**
         * Returns the the name of resource whose value was to be used to set
         * the property
         * 
         * @return the resource name
         */
        public String getKey() {
            return this.key;
        }

        /**
         * Returns the component whose property could not be set
         * 
         * @return the component
         */
        public Component getComponent() {
            return this.component;
        }

        /**
         * Returns the the name of property that could not be set
         * 
         * @return the property name
         */
        public String getPropertyName() {
            return this.propertyName;
        }
    }

    private void injectComponentProperty(final Component component, final PropertyDescriptor pd, final String key) {
        final Method setter = pd.getWriteMethod();
        final Class<?> type = pd.getPropertyType();
        if ((setter != null) && (type != null) && this.containsKey(key)) {
            final Object value = this.getObject(key, type);
            final String propertyName = pd.getName();
            try {
                // Note: this could be generalized, we could delegate
                // to a component property injector.
                if ("text".equals(propertyName) && (component instanceof AbstractButton)) {
                    MnemonicText.configure(component, (String) value);
                } else if ("text".equals(propertyName) && (component instanceof JLabel)) {
                    MnemonicText.configure(component, (String) value);
                } else {
                    setter.invoke(component, value);
                }
            } catch (final Exception e) {
                final String pdn = pd.getName();
                final String msg = "property setter failed";
                final RuntimeException re = new PropertyInjectionException(msg, key, component, pdn);
                re.initCause(e);
                throw re;
            }
        } else if (type != null) {
            final String pdn = pd.getName();
            final String msg = "no value specified for resource";
            throw new PropertyInjectionException(msg, key, component, pdn);
        } else if (setter == null) {
            final String pdn = pd.getName();
            final String msg = "can't set read-only property";
            throw new PropertyInjectionException(msg, key, component, pdn);
        }
    }

    private void injectComponentProperties(final Component component) {
        final String componentName = component.getName();
        if (componentName != null) {
            /*
             * Optimization: punt early if componentName doesn't appear in any
             * componentName.propertyName resource keys
             */
            boolean matchingResourceFound = false;
            for (final String key : this.keySet()) {
                final int i = key.lastIndexOf(".");
                if ((i != -1) && componentName.equals(key.substring(0, i))) {
                    matchingResourceFound = true;
                    break;
                }
            }
            if (!matchingResourceFound) {
                return;
            }
            BeanInfo beanInfo = null;
            try {
                beanInfo = Introspector.getBeanInfo(component.getClass());
            } catch (final IntrospectionException e) {
                final String msg = "introspection failed";
                final RuntimeException re = new PropertyInjectionException(msg, null, component, null);
                re.initCause(e);
                throw re;
            }
            final PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
            if ((pds != null) && (pds.length > 0)) {
                for (final String key : this.keySet()) {
                    final int i = key.lastIndexOf(".");
                    final String keyComponentName = (i == -1) ? null : key.substring(0, i);
                    if (componentName.equals(keyComponentName)) {
                        if ((i + 1) == key.length()) {
                            /*
                             * key has no property name suffix, e.g.
                             * "myComponentName." This is probably a mistake.
                             */
                            final String msg = "component resource lacks property name suffix";
                            ResourceMap.logger.warning(msg);
                            break;
                        }
                        final String propertyName = key.substring(i + 1);
                        boolean matchingPropertyFound = false;
                        for (final PropertyDescriptor pd : pds) {
                            if (pd.getName().equals(propertyName)) {
                                this.injectComponentProperty(component, pd, key);
                                matchingPropertyFound = true;
                                break;
                            }
                        }
                        if (!matchingPropertyFound) {
                            final String msg = String.format(
                                    "[resource %s] component named %s doesn't have a property named %s", key,
                                    componentName, propertyName);
                            ResourceMap.logger.warning(msg);
                        }
                    }
                }
            }
        }
    }

    /**
     * Set each property in <tt>target</tt> to the value of the resource named
     * <tt><i>componentName</i>.propertyName</tt>, where
     * <tt><i>componentName</i></tt> is the value of the target component's name
     * property, i.e. the value of <tt>target.getName()</tt>. The type of the
     * resource must match the type of the corresponding property. Properties
     * that aren't defined by a resource aren't set.
     * <p>
     * For example, given a button configured like this:
     * 
     * <pre>
     * myButton = new JButton();
     * myButton.setName(&quot;myButton&quot;);
     * </pre>
     * 
     * And a ResourceBundle properties file with the following resources:
     * 
     * <pre>
     * myButton.text = Hello World
     * myButton.foreground = 0, 0, 0 
     * myButton.preferredSize = 256, 256
     * </pre>
     * 
     * Then <tt>injectComponent(myButton)</tt> would initialize myButton's text,
     * foreground, and preferredSize properties to <tt>Hello World</tt>,
     * <tt>new Color(0,0,0)</tt>, and <tt>new Dimension(256,256)</tt>
     * respectively.
     * <p>
     * This method calls {@link #getObject} to look up resources and it uses
     * {@link Introspector#getBeanInfo} to find the target component's
     * properties.
     * <p>
     * If target is null an IllegalArgumentException is thrown. If a resource is
     * found that matches the target component's name but the corresponding
     * property can't be set, an (unchecked) {@link PropertyInjectionException}
     * is thrown.
     * 
     * 
     * 
     * @param target
     *            the Component to inject
     * @see #injectComponents
     * @see #getObject
     * @see ResourceConverter#forType
     * @throws LookupException
     *             if an error occurs during lookup or string conversion
     * @throws PropertyInjectionException
     *             if a property specified by a resource can't be set
     * @throws IllegalArgumentException
     *             if target is null
     */
    public void injectComponent(final Component target) {
        if (target == null) {
            throw new IllegalArgumentException("null target");
        }
        this.injectComponentProperties(target);
    }

    /**
     * Applies {@link #injectComponent} to each Component in the hierarchy with
     * root <tt>root</tt>.
     * 
     * @param root
     *            the root of the component hierarchy
     * @throws PropertyInjectionException
     *             if a property specified by a resource can't be set
     * @throws IllegalArgumentException
     *             if target is null
     * @see #injectComponent
     */
    public void injectComponents(final Component root) {
        this.injectComponent(root);
        if (root instanceof JMenu) {
            /*
             * Warning: we're bypassing the popupMenu here because
             * JMenu#getPopupMenu creates it; doesn't seem right to do so at
             * injection time. Unfortunately, this means that attempts to inject
             * the popup menu's "label" property will fail.
             */
            final JMenu menu = (JMenu) root;
            for (final Component child : menu.getMenuComponents()) {
                this.injectComponents(child);
            }
        } else if (root instanceof Container) {
            final Container container = (Container) root;
            for (final Component child : container.getComponents()) {
                this.injectComponents(child);
            }
        }
    }

    /**
     * Unchecked exception thrown by {@link #injectFields} when an error occurs
     * while attempting to set a field (a field that had been marked with
     * <tt>&#064;Resource</tt>).
     * 
     * @see #injectFields
     */
    public static class InjectFieldException extends RuntimeException {
        /**
         * 
         */
        private static final long serialVersionUID = 6264485532912395300L;
        private final Field field;
        private final Object target;
        private final String key;

        /**
         * Constructs an instance of this class with some useful information
         * about the failure.
         * 
         * @param msg
         *            the detail message
         * @param field
         *            the Field we were attempting to set
         * @param target
         *            the object whose field we were attempting to set
         * @param key
         *            the name of the resource
         */
        public InjectFieldException(final String msg, final Field field, final Object target, final String key) {
            super(String.format("%s: resource %s, field %s, target %s", msg, key, field, target));
            this.field = field;
            this.target = target;
            this.key = key;
        }

        /**
         * Return the Field whose value couldn't be set.
         * 
         * @return the field whose value couldn't be set
         */
        public Field getField() {
            return this.field;
        }

        /**
         * Return the Object whose Field we were attempting to set
         * 
         * @return the Object whose Field we were attempting to set
         */
        public Object getTarget() {
            return this.target;
        }

        /**
         * Returns the type of the name of resource for which lookup failed.
         * 
         * @return the resource name
         */
        public String getKey() {
            return this.key;
        }
    }

    private void injectField(final Field field, final Object target, final String key) {
        Class<?> type = field.getType();
        if (type.isArray()) {
            type = type.getComponentType();
            final Pattern p = Pattern.compile(key + "\\[([\\d]+)\\]"); // matches
            // key[12]
            for (final String arrayElementKey : this.keySet()) {
                final Matcher m = p.matcher(arrayElementKey);
                if (m.matches()) {
                    /*
                     * field's value is an array, arrayElementKey is a resource
                     * name of the form "MyClass.myArray[12]" and m.group(1)
                     * matches the array index. Set the index element of the
                     * field's array to the value of the resource.
                     */
                    final Object value = this.getObject(arrayElementKey, type);
                    if (!field.isAccessible()) {
                        field.setAccessible(true);
                    }
                    try {
                        final int index = Integer.parseInt(m.group(1));
                        Array.set(field.get(target), index, value);
                    }
                    /*
                     * Array.set throws IllegalArgumentException,
                     * ArrayIndexOutOfBoundsException field.get throws
                     * IllegalAccessException(Checked), IllegalArgumentException
                     * Integer.parseInt throws NumberFormatException (Checked)
                     */
                    catch (final Exception e) {
                        final String msg = "unable to set array element";
                        final InjectFieldException ife = new InjectFieldException(msg, field, target, key);
                        ife.initCause(e);
                        throw ife;
                    }
                }
            }
        } else { // field is not an array
            final Object value = this.getObject(key, type);
            if (value != null) {
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                try {
                    field.set(target, value);
                }
                /*
                 * Field.set throws IllegalAccessException,
                 * IllegalArgumentException, ExceptionInInitializerError
                 */
                catch (final Exception e) {
                    final String msg = "unable to set field's value";
                    final InjectFieldException ife = new InjectFieldException(msg, field, target, key);
                    ife.initCause(e);
                    throw ife;
                }
            }
        }
    }

    /**
     * Set each field with a <tt>&#064;Resource</tt> annotation in the target
     * object, to the value of a resource whose name is the simple name of the
     * target class followed by "." followed by the name of the field. If the
     * key <tt>&#064;Resource</tt> parameter is specified, then a resource with
     * that name is used instead. Array valued fields can also be initialized
     * with resources whose names end with "[index]". For example:
     * 
     * <pre>
     * class MyClass {
     *     &#064;Resource
     *     String sOne;
     *     &#064;Resource(key = &quot;sTwo&quot;)
     *     String s2;
     *     &#064;Resource
     *     int[] numbers = new int[2];
     * }
     * </pre>
     * 
     * Given the previous class and the following resource file:
     * 
     * <pre>
     * MyClass.sOne = One
     * sTwo = Two
     * MyClass.numbers[0] = 10
     * MyClass.numbers[1] = 11
     * </pre>
     * 
     * Then <tt>injectFields(new MyClass())</tt> would initialize the MyClass
     * <tt>sOne</tt> field to "One", the <tt>s2</tt> field to "Two", and the two
     * elements of the numbers array to 10 and 11.
     * <p>
     * If <tt>target</tt> is null an IllegalArgumentException is thrown. If an
     * error occurs during resource lookup, then an unchecked LookupException is
     * thrown. If a target field marked with <tt>&#064;Resource</tt> can't be
     * set, then an unchecked InjectFieldException is thrown.
     * 
     * @param target
     *            the object whose fields will be initialized
     * @throws LookupException
     *             if an error occurs during lookup or string conversion
     * @throws InjectFieldException
     *             if a field can't be set
     * @throws IllegalArgumentException
     *             if target is null
     * @see #getObject
     */
    public void injectFields(final Object target) {
        if (target == null) {
            throw new IllegalArgumentException("null target");
        }
        final Class<?> targetType = target.getClass();
        if (targetType.isArray()) {
            throw new IllegalArgumentException("array target");
        }
        final String keyPrefix = targetType.getSimpleName() + ".";
        for (final Field field : targetType.getDeclaredFields()) {
            final Resource resource = field.getAnnotation(Resource.class);
            if (resource != null) {
                final String rKey = resource.key();
                final String key = (rKey.length() > 0) ? rKey : keyPrefix + field.getName();
                this.injectField(field, target, key);
            }
        }
    }

    /*
     * Register ResourceConverters that are defined in this class and documented
     * here.
     */
    static {
        final ResourceConverter[] stringConverters = { new ColorStringConverter(), new IconStringConverter(),
                new ImageStringConverter(), new FontStringConverter(), new KeyStrokeStringConverter(),
                new DimensionStringConverter(), new PointStringConverter(), new RectangleStringConverter(),
                new InsetsStringConverter(), new EmptyBorderStringConverter() };
        for (final ResourceConverter sc : stringConverters) {
            ResourceConverter.register(sc);
        }
    }

    /*
     * If path doesn't have a leading "/" then the resourcesDir is prepended,
     * otherwise the leading "/" is removed.
     */
    private static String resourcePath(final String path, final ResourceMap resourceMap) {
        String rPath = path;
        if (path == null) {
            rPath = null;
        } else if (path.startsWith("/")) {
            rPath = (path.length() > 1) ? path.substring(1) : null;
        } else {
            rPath = resourceMap.getResourcesDir() + path;
        }
        return rPath;
    }

    private static ImageIcon loadImageIcon(final String s, final ResourceMap resourceMap)
            throws ResourceConverterException {
        final String rPath = ResourceMap.resourcePath(s, resourceMap);
        if (rPath == null) {
            final String msg = String.format("invalid image/icon path \"%s\"", s);
            throw new ResourceConverterException(msg, s);
        }
        final URL url = resourceMap.getClassLoader().getResource(rPath);
        if (url != null) {
            return new ImageIcon(url);
        }
        final String msg = String.format("couldn't find Icon resource \"%s\"", s);
        throw new ResourceConverterException(msg, s);
    }

    private static class FontStringConverter extends ResourceConverter {
        FontStringConverter() {
            super(Font.class);
        }

        /*
         * Just delegates to Font.decode. Typical string is: face-STYLE-size,
         * for example "Arial-PLAIN-12"
         */
        @Override
        public Object parseString(final String s, final ResourceMap ignore) throws ResourceConverterException {
            return Font.decode(s);
        }
    }

    private static class ColorStringConverter extends ResourceConverter {
        ColorStringConverter() {
            super(Color.class);
        }

        private void error(final String msg, final String s, final Exception e) throws ResourceConverterException {
            throw new ResourceConverterException(msg, s, e);
        }

        private void error(final String msg, final String s) throws ResourceConverterException {
            this.error(msg, s, null);
        }

        /*
         * An improved version of Color.decode() that supports colors with an
         * alpha channel and comma separated RGB[A] values. Legal format for
         * color resources are: "#RRGGBB", "#AARRGGBB", "R, G, B", "R, G, B, A"
         * Thanks to Romain Guy for the code.
         */
        @Override
        public Object parseString(final String s, final ResourceMap ignore) throws ResourceConverterException {
            Color color = null;
            if (s.startsWith("#")) {
                switch (s.length()) {
                // RGB/hex color
                case 7:
                    color = Color.decode(s);
                    break;
                // ARGB/hex color
                case 9:
                    final int alpha = Integer.decode(s.substring(0, 3));
                    final int rgb = Integer.decode("#" + s.substring(3));
                    color = new Color(alpha << 24 | rgb, true);
                    break;
                default:
                    throw new ResourceConverterException("invalid #RRGGBB or #AARRGGBB color string", s);
                }
            } else {
                final String[] parts = s.split(",");
                if (parts.length < 3 || parts.length > 4) {
                    throw new ResourceConverterException("invalid R, G, B[, A] color string", s);
                }
                try {
                    // with alpha component
                    if (parts.length == 4) {
                        final int r = Integer.parseInt(parts[0].trim());
                        final int g = Integer.parseInt(parts[1].trim());
                        final int b = Integer.parseInt(parts[2].trim());
                        final int a = Integer.parseInt(parts[3].trim());
                        color = new Color(r, g, b, a);
                    } else {
                        final int r = Integer.parseInt(parts[0].trim());
                        final int g = Integer.parseInt(parts[1].trim());
                        final int b = Integer.parseInt(parts[2].trim());
                        color = new Color(r, g, b);
                    }
                } catch (final NumberFormatException e) {
                    throw new ResourceConverterException("invalid R, G, B[, A] color string", s, e);
                }
            }
            return color;
        }
    }

    private static class IconStringConverter extends ResourceConverter {
        IconStringConverter() {
            super(Icon.class);
        }

        @Override
        public Object parseString(final String s, final ResourceMap resourceMap) throws ResourceConverterException {
            return ResourceMap.loadImageIcon(s, resourceMap);
        }

        @Override
        public boolean supportsType(final Class testType) {
            return testType.equals(Icon.class) || testType.equals(ImageIcon.class);
        }
    }

    private static class ImageStringConverter extends ResourceConverter {
        ImageStringConverter() {
            super(Image.class);
        }

        @Override
        public Object parseString(final String s, final ResourceMap resourceMap) throws ResourceConverterException {
            return ResourceMap.loadImageIcon(s, resourceMap).getImage();
        }
    }

    private static class KeyStrokeStringConverter extends ResourceConverter {
        KeyStrokeStringConverter() {
            super(KeyStroke.class);
        }

        @Override
        public Object parseString(String s, final ResourceMap ignore) {
            if (s.contains("shortcut")) {
                final int k = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
                s = s.replaceAll("shortcut", (k == Event.META_MASK) ? "meta" : "control");
            }
            return KeyStroke.getKeyStroke(s);
        }
    }

    /*
     * String s is assumed to contain n number substrings separated by commas.
     * Return a list of those integers or null if there are too many, too few,
     * or if a substring can't be parsed. The format of the numbers is specified
     * by Double.valueOf().
     */
    private static List<Double> parseDoubles(final String s, final int n, final String errorMsg)
            throws ResourceConverterException {
        final String[] doubleStrings = s.split(",", n + 1);
        if (doubleStrings.length != n) {
            throw new ResourceConverterException(errorMsg, s);
        } else {
            final List<Double> doubles = new ArrayList<Double>(n);
            for (final String doubleString : doubleStrings) {
                try {
                    doubles.add(Double.valueOf(doubleString));
                } catch (final NumberFormatException e) {
                    throw new ResourceConverterException(errorMsg, s, e);
                }
            }
            return doubles;
        }
    }

    private static class DimensionStringConverter extends ResourceConverter {
        DimensionStringConverter() {
            super(Dimension.class);
        }

        @Override
        public Object parseString(final String s, final ResourceMap ignore) throws ResourceConverterException {
            final List<Double> xy = ResourceMap.parseDoubles(s, 2, "invalid x,y Dimension string");
            final Dimension d = new Dimension();
            d.setSize(xy.get(0), xy.get(1));
            return d;
        }
    }

    private static class PointStringConverter extends ResourceConverter {
        PointStringConverter() {
            super(Point.class);
        }

        @Override
        public Object parseString(final String s, final ResourceMap ignore) throws ResourceConverterException {
            final List<Double> xy = ResourceMap.parseDoubles(s, 2, "invalid x,y Point string");
            final Point p = new Point();
            p.setLocation(xy.get(0), xy.get(1));
            return p;
        }
    }

    private static class RectangleStringConverter extends ResourceConverter {
        RectangleStringConverter() {
            super(Rectangle.class);
        }

        @Override
        public Object parseString(final String s, final ResourceMap ignore) throws ResourceConverterException {
            final List<Double> xywh = ResourceMap.parseDoubles(s, 4, "invalid x,y,width,height Rectangle string");
            final Rectangle r = new Rectangle();
            r.setFrame(xywh.get(0), xywh.get(1), xywh.get(2), xywh.get(3));
            return r;
        }
    }

    private static class InsetsStringConverter extends ResourceConverter {
        InsetsStringConverter() {
            super(Insets.class);
        }

        @Override
        public Object parseString(final String s, final ResourceMap ignore) throws ResourceConverterException {
            final List<Double> tlbr = ResourceMap.parseDoubles(s, 4, "invalid top,left,bottom,right Insets string");
            return new Insets(tlbr.get(0).intValue(), tlbr.get(1).intValue(), tlbr.get(2).intValue(), tlbr.get(3)
                    .intValue());
        }
    }

    private static class EmptyBorderStringConverter extends ResourceConverter {
        EmptyBorderStringConverter() {
            super(EmptyBorder.class);
        }

        @Override
        public Object parseString(final String s, final ResourceMap ignore) throws ResourceConverterException {
            final List<Double> tlbr = ResourceMap.parseDoubles(s, 4, "invalid top,left,bottom,right EmptyBorder string");
            return new EmptyBorder(tlbr.get(0).intValue(), tlbr.get(1).intValue(), tlbr.get(2).intValue(), tlbr.get(3)
                    .intValue());
        }
    }
}
