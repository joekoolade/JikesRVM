/*
 * Copyright (c) 1994, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.lang;

import java.io.InputStream;
import java.io.ObjectStreamField;
import java.io.UTFDataFormatException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericSignatureFormatError;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.JikesRVMSupport;
import java.lang.reflect.MalformedParameterizedTypeException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jikesrvm.VM;
import org.jikesrvm.classlibrary.JavaLangSupport;
import org.jikesrvm.classlibrary.OpenJDKConstantPool;
import org.jikesrvm.classloader.Atom;
import org.jikesrvm.classloader.BootstrapClassLoader;
import org.jikesrvm.classloader.MethodReference;
import org.jikesrvm.classloader.RVMClass;
import org.jikesrvm.classloader.RVMClassLoader;
import org.jikesrvm.classloader.RVMField;
import org.jikesrvm.classloader.RVMMethod;
import org.jikesrvm.classloader.RVMType;
import org.jikesrvm.classloader.TypeReference;
import org.jikesrvm.runtime.Callbacks;
import org.jikesrvm.runtime.Reflection;
import org.jikesrvm.runtime.RuntimeEntrypoints;
import org.vmmagic.pragma.Inline;
import org.vmmagic.pragma.NoInline;
import org.vmmagic.pragma.Pure;

import sun.reflect.ConstantPool;
import sun.reflect.generics.factory.CoreReflectionFactory;
import sun.reflect.generics.factory.GenericsFactory;
import sun.reflect.generics.repository.ClassRepository;
import sun.reflect.generics.repository.ConstructorRepository;
import sun.reflect.generics.repository.MethodRepository;
import sun.reflect.generics.scope.ClassScope;
import sun.security.util.SecurityConstants;

/**
 * Instances of the class {@code Class} represent classes and interfaces in a
 * running Java application. An enum is a kind of class and an annotation is a
 * kind of interface. Every array also belongs to a class that is reflected as a
 * {@code Class} object that is shared by all arrays with the same element type
 * and number of dimensions. The primitive Java types ({@code boolean},
 * {@code byte}, {@code char}, {@code short}, {@code int}, {@code long},
 * {@code float}, and {@code double}), and the keyword {@code void} are also
 * represented as {@code Class} objects.
 *
 * <p>
 * {@code Class} has no public constructor. Instead {@code Class} objects are
 * constructed automatically by the Java Virtual Machine as classes are loaded
 * and by calls to the {@code defineClass} method in the class loader.
 *
 * <p>
 * The following example uses a {@code Class} object to print the class name of
 * an object:
 *
 * <p>
 * <blockquote>
 * 
 * <pre>
 * void printClassName(Object obj)
 * {
 *   System.out.println("The class of " + obj + " is " + obj.getClass().getName());
 * }
 * </pre>
 * 
 * </blockquote>
 *
 * <p>
 * It is also possible to get the {@code Class} object for a named type (or for
 * void) using a class literal (JLS Section <A HREF=
 * "http://java.sun.com/docs/books/jls/second_edition/html/expressions.doc.html#251530">15.8.2</A>).
 * For example:
 *
 * <p>
 * <blockquote>
 * {@code System.out.println("The name of class Foo is: "+Foo.class.getName());}
 * </blockquote>
 *
 * @param <T>
 *          the type of the class modeled by this {@code Class} object. For
 *          example, the type of {@code String.class} is {@code
 * Class<String>}. Use {@code Class<?>} if the class being modeled is unknown.
 *
 * @author unascribed
 * @see java.lang.ClassLoader#defineClass(byte[], int, int)
 * @since JDK1.0
 */
public final class Class<T> implements java.io.Serializable, java.lang.reflect.GenericDeclaration,
java.lang.reflect.Type, java.lang.reflect.AnnotatedElement
{
  private static final int ANNOTATION = 0x00002000;
  private static final int ENUM       = 0x00004000;
  private static final int SYNTHETIC  = 0x00001000;

  /**
   * This field holds the RVMType object for this class.
   */
  final RVMType type;

  /**
   * This field holds the protection domain of this class.
   */
  ProtectionDomain pd;

  /**
   * The signers of this class
   */
  Object[] signers;

  /**
   * Prevents this class from being instantiated, except by the create method in
   * this class.
   */
  private Class(RVMType type)
  {
    this.type = type;
  }

  /**
   * Create a java.lang.Class corresponding to a given RVMType
   */
  static <T> Class<T> create(RVMType type)
  {
    Class<T> c = new Class<T>(type);
    return c;
  }

  /**
   * Converts the object to a string. The string representation is the string
   * "class" or "interface", followed by a space, and then by the fully qualified
   * name of the class in the format returned by {@code getName}. If this
   * {@code Class} object represents a primitive type, this method returns the
   * name of the primitive type. If this {@code Class} object represents void this
   * method returns "void".
   *
   * @return a string representation of this class object.
   */
  public String toString()
  {
    return (isInterface() ? "interface " : (isPrimitive() ? "" : "class ")) + getName();
  }

  /**
   * Returns the {@code Class} object associated with the class or interface with
   * the given string name. Invoking this method is equivalent to:
   *
   * <blockquote> {@code Class.forName(className, true, currentLoader)}
   * </blockquote>
   *
   * where {@code currentLoader} denotes the defining class loader of the current
   * class.
   *
   * <p>
   * For example, the following code fragment returns the runtime {@code Class}
   * descriptor for the class named {@code java.lang.Thread}:
   *
   * <blockquote> {@code Class t = Class.forName("java.lang.Thread")}
   * </blockquote>
   * <p>
   * A call to {@code forName("X")} causes the class named {@code X} to be
   * initialized.
   *
   * @param className
   *          the fully qualified name of the desired class.
   * @return the {@code Class} object for the class with the specified name.
   * @exception LinkageError
   *              if the linkage fails
   * @exception ExceptionInInitializerError
   *              if the initialization provoked by this method fails
   * @exception ClassNotFoundException
   *              if the class cannot be located
   */
  @Inline
  public static Class<?> forName(String typeName) throws ClassNotFoundException
  {
    ClassLoader parentCL = RVMClass.getClassLoaderFromStackFrame(1);
    return forName0(typeName, true, parentCL);
  }

  /**
   * Returns the {@code Class} object associated with the class or interface with
   * the given string name, using the given class loader. Given the fully
   * qualified name for a class or interface (in the same format returned by
   * {@code getName}) this method attempts to locate, load, and link the class or
   * interface. The specified class loader is used to load the class or interface.
   * If the parameter {@code loader} is null, the class is loaded through the
   * bootstrap class loader. The class is initialized only if the
   * {@code initialize} parameter is {@code true} and if it has not been
   * initialized earlier.
   *
   * <p>
   * If {@code name} denotes a primitive type or void, an attempt will be made to
   * locate a user-defined class in the unnamed package whose name is
   * {@code name}. Therefore, this method cannot be used to obtain any of the
   * {@code Class} objects representing primitive types or void.
   *
   * <p>
   * If {@code name} denotes an array class, the component type of the array class
   * is loaded but not initialized.
   *
   * <p>
   * For example, in an instance method the expression:
   *
   * <blockquote> {@code Class.forName("Foo")} </blockquote>
   *
   * is equivalent to:
   *
   * <blockquote>
   * {@code Class.forName("Foo", true, this.getClass().getClassLoader())}
   * </blockquote>
   *
   * Note that this method throws errors related to loading, linking or
   * initializing as specified in Sections 12.2, 12.3 and 12.4 of <em>The Java
   * Language Specification</em>. Note that this method does not check whether the
   * requested class is accessible to its caller.
   *
   * <p>
   * If the {@code loader} is {@code null}, and a security manager is present, and
   * the caller's class loader is not null, then this method calls the security
   * manager's {@code checkPermission} method with a
   * {@code RuntimePermission("getClassLoader")} permission to ensure it's ok to
   * access the bootstrap class loader.
   *
   * @param name
   *          fully qualified name of the desired class
   * @param initialize
   *          whether the class must be initialized
   * @param loader
   *          class loader from which the class must be loaded
   * @return class object representing the desired class
   *
   * @exception LinkageError
   *              if the linkage fails
   * @exception ExceptionInInitializerError
   *              if the initialization provoked by this method fails
   * @exception ClassNotFoundException
   *              if the class cannot be located by the specified class loader
   *
   * @see java.lang.Class#forName(String)
   * @see java.lang.ClassLoader
   * @since 1.2
   */
  public static Class<?> forName(String className, boolean initialize, ClassLoader classLoader)
  throws ClassNotFoundException, LinkageError, ExceptionInInitializerError
  {
    if (classLoader == null)
    {
      SecurityManager security = System.getSecurityManager();
      if (security != null)
      {
        ClassLoader parentCL = RVMClass.getClassLoaderFromStackFrame(1);
        if (parentCL != null)
        {
          try
          {
            security.checkPermission(new RuntimePermission("getClassLoader"));
          } catch (SecurityException e)
          {
            throw new ClassNotFoundException("Security exception when"
            + " trying to get a classloader so we can load the" + " class named \"" + className + "\"", e);
        }
      }
    }
    classLoader = BootstrapClassLoader.getBootstrapClassLoader();
  }
  return forName0(className, initialize, classLoader);
}

  /**
   * Called after security check for system loader access checks have been made.
   */
  private static Class<?> forName0(String name, boolean initialize, ClassLoader loader) throws ClassNotFoundException
  {
    if (name == null)
    {
      throw new NullPointerException("Name parameter must not be null (but was)!");
    }

    try
    {
      if (name.startsWith("["))
      {
        if (!JavaLangSupport.validArrayDescriptor(name))
        {
          throw new ClassNotFoundException(name);
        }
      }
      Atom descriptor = Atom.findOrCreateAsciiAtom(name.replace('.', '/')).descriptorFromClassName();
      TypeReference tRef = TypeReference.findOrCreate(loader, descriptor);
      RVMType ans = tRef.resolve();
      Callbacks.notifyForName(ans);
      if (initialize && !ans.isInitialized())
      {
        ans.prepareForFirstUse();
      }
      return ans.getClassForType();
    } catch (NoClassDefFoundError ncdfe)
    {
      Throwable cause2 = ncdfe.getCause();
      ClassNotFoundException cnf;
      // If we get a NCDFE that was caused by a CNFE, throw the original CNFE.
      if (cause2 instanceof ClassNotFoundException)
        cnf = (ClassNotFoundException) cause2;
      else
        cnf = new ClassNotFoundException(name, ncdfe);
      throw cnf;
    }
  }

  /**
   * Creates a new instance of the class represented by this {@code Class} object.
   * The class is instantiated as if by a {@code new} expression with an empty
   * argument list. The class is initialized if it has not already been
   * initialized.
   *
   * <p>
   * Note that this method propagates any exception thrown by the nullary
   * constructor, including a checked exception. Use of this method effectively
   * bypasses the compile-time exception checking that would otherwise be
   * performed by the compiler. The
   * {@link java.lang.reflect.Constructor#newInstance(java.lang.Object...)
   * Constructor.newInstance} method avoids this problem by wrapping any exception
   * thrown by the constructor in a (checked)
   * {@link java.lang.reflect.InvocationTargetException}.
   *
   * @return a newly allocated instance of the class represented by this object.
   * @exception IllegalAccessException
   *              if the class or its nullary constructor is not accessible.
   * @exception InstantiationException
   *              if this {@code Class} represents an abstract class, an
   *              interface, an array class, a primitive type, or void; or if the
   *              class has no nullary constructor; or if the instantiation fails
   *              for some other reason.
   * @exception ExceptionInInitializerError
   *              if the initialization provoked by this method fails.
   * @exception SecurityException
   *              If a security manager, <i>s</i>, is present and any of the
   *              following conditions is met:
   *
   *              <ul>
   *
   *              <li>invocation of {@link SecurityManager#checkMemberAccess
   *              s.checkMemberAccess(this, Member.PUBLIC)} denies creation of new
   *              instances of this class
   *
   *              <li>the caller's class loader is not the same as or an ancestor
   *              of the class loader for the current class and invocation of
   *              {@link SecurityManager#checkPackageAccess
   *              s.checkPackageAccess()} denies access to the package of this
   *              class
   *
   *              </ul>
   *
   */
  public T newInstance() throws InstantiationException, IllegalAccessException
  {
    // Basic checks
    checkMemberAccess(Member.PUBLIC);

    if (!type.isClassType())
      throw new InstantiationException();

    RVMClass cls = type.asClass();

    if (cls.isAbstract() || cls.isInterface())
      throw new InstantiationException();

    // Ensure that the class is initialized
    if (!cls.isInitialized())
    {
      RuntimeEntrypoints.initializeClassForDynamicLink(cls);
    }

    // Find the defaultConstructor
    RVMMethod defaultConstructor = getDefaultConstructor();
    if (defaultConstructor == null)
      throw new InstantiationException();

    // Check that caller is allowed to access it
    if (!defaultConstructor.isPublic())
    {
      RVMClass accessingClass = RVMClass.getClassFromStackFrame(1);
      JavaLangSupport.checkAccess(defaultConstructor, accessingClass);
    }

    // Allocate an uninitialized instance;
    @SuppressWarnings("unchecked") // yes, we're giving an anonymous object a type.
    T obj = (T) RuntimeEntrypoints.resolvedNewScalar(cls);

    // Run the default constructor on the it.
    Reflection.invoke(defaultConstructor, null, obj, null, true);
    return obj;
  }

  /**
   * Cached default constructor value
   */
  RVMMethod                                 defaultConstructor;

  /**
   * Determines if the specified {@code Object} is assignment-compatible with the
   * object represented by this {@code Class}. This method is the dynamic
   * equivalent of the Java language {@code instanceof} operator. The method
   * returns {@code true} if the specified {@code Object} argument is non-null and
   * can be cast to the reference type represented by this {@code Class} object
   * without raising a {@code ClassCastException.} It returns {@code false}
   * otherwise.
   *
   * <p>
   * Specifically, if this {@code Class} object represents a declared class, this
   * method returns {@code true} if the specified {@code Object} argument is an
   * instance of the represented class (or of any of its subclasses); it returns
   * {@code false} otherwise. If this {@code Class} object represents an array
   * class, this method returns {@code true} if the specified {@code Object}
   * argument can be converted to an object of the array class by an identity
   * conversion or by a widening reference conversion; it returns {@code false}
   * otherwise. If this {@code Class} object represents an interface, this method
   * returns {@code true} if the class or any superclass of the specified
   * {@code Object} argument implements this interface; it returns {@code false}
   * otherwise. If this {@code Class} object represents a primitive type, this
   * method returns {@code false}.
   *
   * @param obj
   *          the object to check
   * @return true if {@code obj} is an instance of this class
   *
   * @since JDK1.1
   */
  public boolean isInstance(Object obj)
  {
    return JavaLangSupport.isInstanceOf((Class<?>) (Object) this, obj);
  }

  /**
   * Determines if the class or interface represented by this {@code Class} object
   * is either the same as, or is a superclass or superinterface of, the class or
   * interface represented by the specified {@code Class} parameter. It returns
   * {@code true} if so; otherwise it returns {@code false}. If this {@code Class}
   * object represents a primitive type, this method returns {@code true} if the
   * specified {@code Class} parameter is exactly this {@code Class} object;
   * otherwise it returns {@code false}.
   *
   * <p>
   * Specifically, this method tests whether the type represented by the specified
   * {@code Class} parameter can be converted to the type represented by this
   * {@code Class} object via an identity conversion or via a widening reference
   * conversion. See <em>The Java Language Specification</em>, sections 5.1.1 and
   * 5.1.4 , for details.
   *
   * @param cls
   *          the {@code Class} object to be checked
   * @return the {@code boolean} value indicating whether objects of the type
   *         {@code cls} can be assigned to objects of this class
   * @exception NullPointerException
   *              if the specified Class parameter is null.
   * @since JDK1.1
   */
  public boolean isAssignableFrom(Class<?> cls)
  {
    RVMType otherType = java.lang.JikesRVMSupport.getTypeForClass(cls);
    RVMType myType = java.lang.JikesRVMSupport.getTypeForClass((Class<?>) (Object) this);
    return myType.isAssignableFrom(otherType);
  }

  /**
   * Determines if the specified {@code Class} object represents an interface
   * type.
   *
   * @return {@code true} if this object represents an interface; {@code false}
   *         otherwise.
   */
  public boolean isInterface()
  {
    RVMType myType = java.lang.JikesRVMSupport.getTypeForClass((Class<?>) (Object) this);
    return myType.isClassType() && myType.asClass().isInterface();
  }

  /**
   * Determines if this {@code Class} object represents an array class.
   *
   * @return {@code true} if this object represents an array class; {@code false}
   *         otherwise.
   * @since JDK1.1
   */
  public boolean isArray()
  {
    return java.lang.JikesRVMSupport.getTypeForClass((Class<?>) (Object) this).isArrayType();
  }

  /**
   * Determines if the specified {@code Class} object represents a primitive type.
   *
   * <p>
   * There are nine predefined {@code Class} objects to represent the eight
   * primitive types and void. These are created by the Java Virtual Machine, and
   * have the same names as the primitive types that they represent, namely
   * {@code boolean}, {@code byte}, {@code char}, {@code short}, {@code int},
   * {@code long}, {@code float}, and {@code double}.
   *
   * <p>
   * These objects may only be accessed via the following public static final
   * variables, and are the only {@code Class} objects for which this method
   * returns {@code true}.
   *
   * @return true if and only if this class represents a primitive type
   *
   * @see java.lang.Boolean#TYPE
   * @see java.lang.Character#TYPE
   * @see java.lang.Byte#TYPE
   * @see java.lang.Short#TYPE
   * @see java.lang.Integer#TYPE
   * @see java.lang.Long#TYPE
   * @see java.lang.Float#TYPE
   * @see java.lang.Double#TYPE
   * @see java.lang.Void#TYPE
   * @since JDK1.1
   */
  public boolean isPrimitive()
  {
    return java.lang.JikesRVMSupport.getTypeForClass((Class<?>) (Object) this).isPrimitiveType();
  }

  /**
   * Returns true if this {@code Class} object represents an annotation type. Note
   * that if this method returns true, {@link #isInterface()} would also return
   * true, as all annotation types are also interfaces.
   *
   * @return {@code true} if this class object represents an annotation type;
   *         {@code false} otherwise
   * @since 1.5
   */
  public boolean isAnnotation()
  {
    return (getModifiers() & ANNOTATION) != 0;
  }

  /**
   * Returns {@code true} if this class is a synthetic class; returns
   * {@code false} otherwise.
   * 
   * @return {@code true} if and only if this class is a synthetic class as
   *         defined by the Java Language Specification.
   * @since 1.5
   */
  public boolean isSynthetic()
  {
    return (getModifiers() & SYNTHETIC) != 0;
  }

  /**
   * Returns the name of the entity (class, interface, array class, primitive
   * type, or void) represented by this {@code Class} object, as a {@code String}.
   *
   * <p>
   * If this class object represents a reference type that is not an array type
   * then the binary name of the class is returned, as specified by the Java
   * Language Specification, Second Edition.
   *
   * <p>
   * If this class object represents a primitive type or void, then the name
   * returned is a {@code String} equal to the Java language keyword corresponding
   * to the primitive type or void.
   *
   * <p>
   * If this class object represents a class of arrays, then the internal form of
   * the name consists of the name of the element type preceded by one or more
   * '{@code [}' characters representing the depth of the array nesting. The
   * encoding of element type names is as follows:
   *
   * <blockquote>
   * <table summary="Element types and encodings">
   * <tr>
   * <th>Element Type
   * <th>&nbsp;&nbsp;&nbsp;
   * <th>Encoding
   * <tr>
   * <td>boolean
   * <td>&nbsp;&nbsp;&nbsp;
   * <td align=center>Z
   * <tr>
   * <td>byte
   * <td>&nbsp;&nbsp;&nbsp;
   * <td align=center>B
   * <tr>
   * <td>char
   * <td>&nbsp;&nbsp;&nbsp;
   * <td align=center>C
   * <tr>
   * <td>class or interface
   * <td>&nbsp;&nbsp;&nbsp;
   * <td align=center>L<i>classname</i>;
   * <tr>
   * <td>double
   * <td>&nbsp;&nbsp;&nbsp;
   * <td align=center>D
   * <tr>
   * <td>float
   * <td>&nbsp;&nbsp;&nbsp;
   * <td align=center>F
   * <tr>
   * <td>int
   * <td>&nbsp;&nbsp;&nbsp;
   * <td align=center>I
   * <tr>
   * <td>long
   * <td>&nbsp;&nbsp;&nbsp;
   * <td align=center>J
   * <tr>
   * <td>short
   * <td>&nbsp;&nbsp;&nbsp;
   * <td align=center>S
   * </table>
   * </blockquote>
   *
   * <p>
   * The class or interface name <i>classname</i> is the binary name of the class
   * specified above.
   *
   * <p>
   * Examples: <blockquote>
   * 
   * <pre>
   * String.class.getName()
   *     returns "java.lang.String"
   * byte.class.getName()
   *     returns "byte"
   * (new Object[3]).getClass().getName()
   *     returns "[Ljava.lang.Object;"
   * (new int[3][4][5][6][7][8][9]).getClass().getName()
   *     returns "[[[[[[[I"
   * </pre>
   * 
   * </blockquote>
   *
   * @return the name of the class or interface represented by this object.
   */
  public String getName()
  {
    if (name == null)
      name = getName0();
    return name;
  }

  // cache the name to reduce the number of calls into the VM
  private transient String name;

  private String getName0()
  {
    return java.lang.JikesRVMSupport.getTypeForClass((Class<?>) (Object) this).toString();
  }

  /**
   * Returns the class loader for the class. Some implementations may use null to
   * represent the bootstrap class loader. This method will return null in such
   * implementations if this class was loaded by the bootstrap class loader.
   *
   * <p>
   * If a security manager is present, and the caller's class loader is not null
   * and the caller's class loader is not the same as or an ancestor of the class
   * loader for the class whose class loader is requested, then this method calls
   * the security manager's {@code checkPermission} method with a
   * {@code RuntimePermission("getClassLoader")} permission to ensure it's ok to
   * access the class loader for the class.
   *
   * <p>
   * If this object represents a primitive type or void, null is returned.
   *
   * @return the class loader that loaded the class or interface represented by
   *         this object.
   * @throws SecurityException
   *           if a security manager exists and its {@code checkPermission} method
   *           denies access to the class loader for the class.
   * @see java.lang.ClassLoader
   * @see SecurityManager#checkPermission
   * @see java.lang.RuntimePermission
   */
  public ClassLoader getClassLoader()
  {
    SecurityManager security = System.getSecurityManager();
    if (security != null)
    {
      ClassLoader parentCL = RVMClass.getClassLoaderFromStackFrame(1);
      if (parentCL != null)
      {
        security.checkPermission(new RuntimePermission("getClassLoader"));
      }
    }
    RVMType type = java.lang.JikesRVMSupport.getTypeForClass((Class<?>) (Object) this);
    ClassLoader cl = type.getClassLoader();
    return cl == BootstrapClassLoader.getBootstrapClassLoader() ? null : cl;
  }

  // Package-private to allow ClassLoader access
  ClassLoader getClassLoader0()
  {
    ClassLoader classLoader = java.lang.JikesRVMSupport.getTypeForClass((Class<?>) (Object) this).getClassLoader();
    if (classLoader == BootstrapClassLoader.getBootstrapClassLoader())
      return null;
    return classLoader;
  }

  /**
   * Returns an array of {@code TypeVariable} objects that represent the type
   * variables declared by the generic declaration represented by this
   * {@code GenericDeclaration} object, in declaration order. Returns an array of
   * length 0 if the underlying generic declaration declares no type variables.
   *
   * @return an array of {@code TypeVariable} objects that represent the type
   *         variables declared by this generic declaration
   * @throws GenericSignatureFormatError
   *           if the generic signature of this generic declaration does not
   *           conform to the format specified in the Java Virtual Machine
   *           Specification, 3rd edition
   * @since 1.5
   */
  public TypeVariable<Class<T>>[] getTypeParameters()
  {
    if (getGenericSignature() != null)
      return (TypeVariable<Class<T>>[]) getGenericInfo().getTypeParameters();
    else
      return (TypeVariable<Class<T>>[]) new TypeVariable<?>[0];
  }

  /**
   * Returns the {@code Class} representing the superclass of the entity (class,
   * interface, primitive type or void) represented by this {@code Class}. If this
   * {@code Class} represents either the {@code Object} class, an interface, a
   * primitive type, or void, then null is returned. If this object represents an
   * array class then the {@code Class} object representing the {@code Object}
   * class is returned.
   *
   * @return the superclass of the class represented by this object.
   */
  public Class<? super T> getSuperclass()
  {
    RVMType type = java.lang.JikesRVMSupport.getTypeForClass((Class<?>) (Object) this);
    if (type.isArrayType())
    {
      return Object.class;
    } else if (type.isClassType())
    {
      RVMClass myClass = type.asClass();
      if (myClass.isInterface())
        return null;
      RVMType supe = myClass.getSuperClass();
      return supe == null ? null : (Class<? super T>) supe.getClassForType();
    } else
    {
      return null;
    }
  }

  /**
   * Returns the {@code Type} representing the direct superclass of the entity
   * (class, interface, primitive type or void) represented by this {@code Class}.
   *
   * <p>
   * If the superclass is a parameterized type, the {@code Type} object returned
   * must accurately reflect the actual type parameters used in the source code.
   * The parameterized type representing the superclass is created if it had not
   * been created before. See the declaration of
   * {@link java.lang.reflect.ParameterizedType ParameterizedType} for the
   * semantics of the creation process for parameterized types. If this
   * {@code Class} represents either the {@code Object} class, an interface, a
   * primitive type, or void, then null is returned. If this object represents an
   * array class then the {@code Class} object representing the {@code Object}
   * class is returned.
   *
   * @throws GenericSignatureFormatError
   *           if the generic class signature does not conform to the format
   *           specified in the Java Virtual Machine Specification, 3rd edition
   * @throws TypeNotPresentException
   *           if the generic superclass refers to a non-existent type declaration
   * @throws MalformedParameterizedTypeException
   *           if the generic superclass refers to a parameterized type that
   *           cannot be instantiated for any reason
   * @return the superclass of the class represented by this object
   * @since 1.5
   */
  public Type getGenericSuperclass()
  {
    if (getGenericSignature() != null)
    {
      // Historical irregularity:
      // Generic signature marks interfaces with superclass = Object
      // but this API returns null for interfaces
      if (isInterface())
        return null;
      return getGenericInfo().getSuperclass();
    } else
      return getSuperclass();
  }

  /**
   * Gets the package for this class. The class loader of this class is used to
   * find the package. If the class was loaded by the bootstrap class loader the
   * set of packages loaded from CLASSPATH is searched to find the package of the
   * class. Null is returned if no package object was created by the class loader
   * of this class.
   *
   * <p>
   * Packages have attributes for versions and specifications only if the
   * information was defined in the manifests that accompany the classes, and if
   * the class loader created the package instance with the attributes from the
   * manifest.
   *
   * @return the package of the class, or null if no package information is
   *         available from the archive or codebase.
   */
  public Package getPackage()
  {
    return Package.getPackage(this);
  }

  /**
   * Determines the interfaces implemented by the class or interface represented
   * by this object.
   *
   * <p>
   * If this object represents a class, the return value is an array containing
   * objects representing all interfaces implemented by the class. The order of
   * the interface objects in the array corresponds to the order of the interface
   * names in the {@code implements} clause of the declaration of the class
   * represented by this object. For example, given the declaration: <blockquote>
   * {@code class Shimmer implements FloorWax, DessertTopping { ... }}
   * </blockquote> suppose the value of {@code s} is an instance of
   * {@code Shimmer}; the value of the expression: <blockquote>
   * {@code s.getClass().getInterfaces()[0]} </blockquote> is the {@code Class}
   * object that represents interface {@code FloorWax}; and the value of:
   * <blockquote> {@code s.getClass().getInterfaces()[1]} </blockquote> is the
   * {@code Class} object that represents interface {@code DessertTopping}.
   *
   * <p>
   * If this object represents an interface, the array contains objects
   * representing all interfaces extended by the interface. The order of the
   * interface objects in the array corresponds to the order of the interface
   * names in the {@code extends} clause of the declaration of the interface
   * represented by this object.
   *
   * <p>
   * If this object represents a class or interface that implements no interfaces,
   * the method returns an array of length 0.
   *
   * <p>
   * If this object represents a primitive type or void, the method returns an
   * array of length 0.
   *
   * @return an array of interfaces implemented by this class.
   */
  public Class<?>[] getInterfaces()
  {
    RVMType type = java.lang.JikesRVMSupport.getTypeForClass((Class<?>) (Object) this);
    if (type.isArrayType())
    {
      // arrays implement JavaLangSerializable & JavaLangCloneable
      return new Class[] { RVMType.JavaLangCloneableType.getClassForType(),
          RVMType.JavaIoSerializableType.getClassForType() };
    } else if (type.isClassType())
    {
      RVMClass[] interfaces = type.asClass().getDeclaredInterfaces();
      Class<?>[] jinterfaces = new Class[interfaces.length];
      for (int i = 0; i != interfaces.length; i++)
        jinterfaces[i] = interfaces[i].getClassForType();
      return jinterfaces;
    } else
    {
      return new Class[0];
    }
  }

  /**
   * Returns the {@code Type}s representing the interfaces directly implemented by
   * the class or interface represented by this object.
   *
   * <p>
   * If a superinterface is a parameterized type, the {@code Type} object returned
   * for it must accurately reflect the actual type parameters used in the source
   * code. The parameterized type representing each superinterface is created if
   * it had not been created before. See the declaration of
   * {@link java.lang.reflect.ParameterizedType ParameterizedType} for the
   * semantics of the creation process for parameterized types.
   *
   * <p>
   * If this object represents a class, the return value is an array containing
   * objects representing all interfaces implemented by the class. The order of
   * the interface objects in the array corresponds to the order of the interface
   * names in the {@code implements} clause of the declaration of the class
   * represented by this object. In the case of an array class, the interfaces
   * {@code Cloneable} and {@code Serializable} are returned in that order.
   *
   * <p>
   * If this object represents an interface, the array contains objects
   * representing all interfaces directly extended by the interface. The order of
   * the interface objects in the array corresponds to the order of the interface
   * names in the {@code extends} clause of the declaration of the interface
   * represented by this object.
   *
   * <p>
   * If this object represents a class or interface that implements no interfaces,
   * the method returns an array of length 0.
   *
   * <p>
   * If this object represents a primitive type or void, the method returns an
   * array of length 0.
   *
   * @throws GenericSignatureFormatError
   *           if the generic class signature does not conform to the format
   *           specified in the Java Virtual Machine Specification, 3rd edition
   * @throws TypeNotPresentException
   *           if any of the generic superinterfaces refers to a non-existent type
   *           declaration
   * @throws MalformedParameterizedTypeException
   *           if any of the generic superinterfaces refer to a parameterized type
   *           that cannot be instantiated for any reason
   * @return an array of interfaces implemented by this class
   * @since 1.5
   */
  public Type[] getGenericInterfaces()
  {
    if (getGenericSignature() != null)
      return getGenericInfo().getSuperInterfaces();
    else
      return getInterfaces();
  }

  /**
   * Returns the {@code Class} representing the component type of an array. If
   * this class does not represent an array class this method returns null.
   *
   * @return the {@code Class} representing the component type of this class if
   *         this class is an array
   * @see java.lang.reflect.Array
   * @since JDK1.1
   */
  public Class<?> getComponentType()
  {
    RVMType typeForClass = java.lang.JikesRVMSupport.getTypeForClass((Class<?>) (Object) this);
    if (typeForClass.isArrayType())
    {
      return typeForClass.asArray().getElementType().getClassForType();
    }
    return null;
  }

  /**
   * Returns the Java language modifiers for this class or interface, encoded in
   * an integer. The modifiers consist of the Java Virtual Machine's constants for
   * {@code public}, {@code protected}, {@code private}, {@code final},
   * {@code static}, {@code abstract} and {@code interface}; they should be
   * decoded using the methods of class {@code Modifier}.
   *
   * <p>
   * If the underlying class is an array class, then its {@code public},
   * {@code private} and {@code protected} modifiers are the same as those of its
   * component type. If this {@code Class} represents a primitive type or void,
   * its {@code public} modifier is always {@code true}, and its {@code protected}
   * and {@code private} modifiers are always {@code false}. If this object
   * represents an array class, a primitive type or void, then its {@code final}
   * modifier is always {@code true} and its interface modifier is always
   * {@code false}. The values of its other modifiers are not determined by this
   * specification.
   *
   * <p>
   * The modifier encodings are defined in <em>The Java Virtual Machine
   * Specification</em>, table 4.1.
   *
   * @return the {@code int} representing the modifiers for this class
   * @see java.lang.reflect.Modifier
   * @since JDK1.1
   */
  public int getModifiers()
  {
    RVMType type = java.lang.JikesRVMSupport.getTypeForClass((Class<?>) (Object) this);
    return JavaLangSupport.getModifiersFromRvmType(type);
  }

  /**
   * Gets the signers of this class.
   *
   * @return the signers of this class, or null if there are no signers. In
   *         particular, this method returns null if this object represents a
   *         primitive type or void.
   * @since JDK1.1
   */
  public Object[] getSigners()
  {
    VM.sysFail("getSigners");
    return null;
  }

  /**
   * Set the signers of this class.
   */
  void setSigners(Object[] signers)
  {
    VM.sysFail("setSigners");
  }

  /**
   * If this {@code Class} object represents a local or anonymous class within a
   * method, returns a {@link java.lang.reflect.Method Method} object representing
   * the immediately enclosing method of the underlying class. Returns
   * {@code null} otherwise.
   *
   * In particular, this method returns {@code null} if the underlying class is a
   * local or anonymous class immediately enclosed by a type declaration, instance
   * initializer or static initializer.
   *
   * @return the immediately enclosing method of the underlying class, if that
   *         class is a local or anonymous class; otherwise {@code null}.
   * @since 1.5
   */
  public Method getEnclosingMethod()
  {
    EnclosingMethodInfo enclosingInfo = getEnclosingMethodInfo();

    if (enclosingInfo == null)
      return null;
    else
    {
      if (!enclosingInfo.isMethod())
        return null;

      MethodRepository typeInfo = MethodRepository.make(enclosingInfo.getDescriptor(), getFactory());
      Class<?> returnType = toClass(typeInfo.getReturnType());
      Type[] parameterTypes = typeInfo.getParameterTypes();
      Class<?>[] parameterClasses = new Class<?>[parameterTypes.length];

      // Convert Types to Classes; returned types *should*
      // be class objects since the methodDescriptor's used
      // don't have generics information
      for (int i = 0; i < parameterClasses.length; i++)
        parameterClasses[i] = toClass(parameterTypes[i]);

      // Perform access check
      Class<?> enclosingCandidate = enclosingInfo.getEnclosingClass();
      // be very careful not to change the stack depth of this
      // checkMemberAccess call for security reasons
      // see java.lang.SecurityManager.checkMemberAccess
      //
      // Note that we need to do this on the enclosing class
      enclosingCandidate.checkMemberAccess(Member.DECLARED);
      /*
       * Loop over all declared methods; match method name, number of and type of
       * parameters, *and* return type. Matching return type is also necessary because
       * of covariant returns, etc.
       */
      for (Method m : enclosingCandidate.getDeclaredMethods())
      {
        if (m.getName().equals(enclosingInfo.getName()))
        {
          Class<?>[] candidateParamClasses = m.getParameterTypes();
          if (candidateParamClasses.length == parameterClasses.length)
          {
            boolean matches = true;
            for (int i = 0; i < candidateParamClasses.length; i++)
            {
              if (!candidateParamClasses[i].equals(parameterClasses[i]))
              {
                matches = false;
                break;
              }
            }

            if (matches)
            { // finally, check return type
              if (m.getReturnType().equals(returnType))
                return m;
            }
          }
        }
      }

      throw new InternalError("Enclosing method not found");
    }
  }

  private Object[] getEnclosingMethod0()
  {
    RVMType type = java.lang.JikesRVMSupport.getTypeForClass((Class<?>) (Object) this);
    if (!type.isClassType())
    {
      return null;
    }
    RVMClass thisClass = type.asClass();
    MethodReference enclosingMethod = thisClass.getEnclosingMethod();
    if (enclosingMethod == null)
    {
      TypeReference enclosingClassTypeRef = thisClass.getEnclosingClass();
      if (enclosingClassTypeRef == null)
      {
        return null;
      }
      RVMClass enclClass = enclosingClassTypeRef.resolve().asClass();
      Object[] returnValue = new Object[3];
      returnValue[0] = enclClass.getClassForType();
      return returnValue;
    }
    RVMMethod resolve = enclosingMethod.resolve();
    Object[] returnValue = new Object[3];
    Class<?> immediatelyEnclosingClass = resolve.getDeclaringClass().getClassForType();
    returnValue[0] = immediatelyEnclosingClass;
    String name = null;
    String descriptor = null;
    try
    {
      name = enclosingMethod.getName().toUnicodeString();
      descriptor = enclosingMethod.getDescriptor().toString();
      returnValue[1] = name;
      returnValue[2] = descriptor;
    } catch (UTFDataFormatException e)
    {
      throw new Error(e);
    }

    if (name == null)
    {
      returnValue[1] = null;
      returnValue[2] = null;
      return returnValue;
    }

    if (VM.VerifyAssertions)
      VM._assert(descriptor != null);

    return returnValue;
  }

  private EnclosingMethodInfo getEnclosingMethodInfo()
  {
    Object[] enclosingInfo = getEnclosingMethod0();
    if (enclosingInfo == null)
      return null;
    else
    {
      return new EnclosingMethodInfo(enclosingInfo);
    }
  }

  private final static class EnclosingMethodInfo
  {
    private Class<?> enclosingClass;
    private String   name;
    private String   descriptor;

    private EnclosingMethodInfo(Object[] enclosingInfo)
    {
      if (enclosingInfo.length != 3)
        throw new InternalError("Malformed enclosing method information");
      try
      {
        // The array is expected to have three elements:

        // the immediately enclosing class
        enclosingClass = (Class<?>) enclosingInfo[0];
        assert (enclosingClass != null);

        // the immediately enclosing method or constructor's
        // name (can be null).
        name = (String) enclosingInfo[1];

        // the immediately enclosing method or constructor's
        // descriptor (null iff name is).
        descriptor = (String) enclosingInfo[2];
        assert ((name != null && descriptor != null) || name == descriptor);
      } catch (ClassCastException cce)
      {
        throw new InternalError("Invalid type in enclosing method information");
      }
    }

    boolean isPartial()
    {
      return enclosingClass == null || name == null || descriptor == null;
    }

    boolean isConstructor()
    {
      return !isPartial() && "<init>".equals(name);
    }

    boolean isMethod()
    {
      return !isPartial() && !isConstructor() && !"<clinit>".equals(name);
    }

    Class<?> getEnclosingClass()
    {
      return enclosingClass;
    }

    String getName()
    {
      return name;
    }

    String getDescriptor()
    {
      return descriptor;
    }

  }

  private static Class<?> toClass(Type o)
  {
    if (o instanceof GenericArrayType)
      return Array.newInstance(toClass(((GenericArrayType) o).getGenericComponentType()), 0).getClass();
    return (Class<?>) o;
  }

  /**
   * If this {@code Class} object represents a local or anonymous class within a
   * constructor, returns a {@link java.lang.reflect.Constructor Constructor}
   * object representing the immediately enclosing constructor of the underlying
   * class. Returns {@code null} otherwise. In particular, this method returns
   * {@code null} if the underlying class is a local or anonymous class
   * immediately enclosed by a type declaration, instance initializer or static
   * initializer.
   *
   * @return the immediately enclosing constructor of the underlying class, if
   *         that class is a local or anonymous class; otherwise {@code null}.
   * @since 1.5
   */
  public Constructor<?> getEnclosingConstructor()
  {
    EnclosingMethodInfo enclosingInfo = getEnclosingMethodInfo();

    if (enclosingInfo == null)
      return null;
    else
    {
      if (!enclosingInfo.isConstructor())
        return null;

      ConstructorRepository typeInfo = ConstructorRepository.make(enclosingInfo.getDescriptor(), getFactory());
      Type[] parameterTypes = typeInfo.getParameterTypes();
      Class<?>[] parameterClasses = new Class<?>[parameterTypes.length];

      // Convert Types to Classes; returned types *should*
      // be class objects since the methodDescriptor's used
      // don't have generics information
      for (int i = 0; i < parameterClasses.length; i++)
        parameterClasses[i] = toClass(parameterTypes[i]);

      // Perform access check
      Class<?> enclosingCandidate = enclosingInfo.getEnclosingClass();
      // be very careful not to change the stack depth of this
      // checkMemberAccess call for security reasons
      // see java.lang.SecurityManager.checkMemberAccess
      //
      // Note that we need to do this on the enclosing class
      enclosingCandidate.checkMemberAccess(Member.DECLARED);
      /*
       * Loop over all declared constructors; match number of and type of parameters.
       */
      for (Constructor<?> c : enclosingCandidate.getDeclaredConstructors())
      {
        Class<?>[] candidateParamClasses = c.getParameterTypes();
        if (candidateParamClasses.length == parameterClasses.length)
        {
          boolean matches = true;
          for (int i = 0; i < candidateParamClasses.length; i++)
          {
            if (!candidateParamClasses[i].equals(parameterClasses[i]))
            {
              matches = false;
              break;
            }
          }

          if (matches)
            return c;
        }
      }

      throw new InternalError("Enclosing constructor not found");
    }
  }

  /**
   * If the class or interface represented by this {@code Class} object is a
   * member of another class, returns the {@code Class} object representing the
   * class in which it was declared. This method returns null if this class or
   * interface is not a member of any other class. If this {@code Class} object
   * represents an array class, a primitive type, or void,then this method returns
   * null.
   *
   * @return the declaring class for this class
   * @since JDK1.1
   */
  public Class<?> getDeclaringClass()
  {
    return getDeclaringClass0();
  }

  private Class<?> getDeclaringClass0()
  {
    // TODO OPENJDK/ICEDTEA share with GNU Classpath
    RVMType type = java.lang.JikesRVMSupport.getTypeForClass((Class<?>) (Object) this);
    if (!type.isClassType())
      return null;
    TypeReference dc = type.asClass().getDeclaringClass();
    if (dc == null)
      return null;
    return dc.resolve().getClassForType();
  }

  /**
   * Returns the immediately enclosing class of the underlying class. If the
   * underlying class is a top level class this method returns {@code null}.
   * 
   * @return the immediately enclosing class of the underlying class
   * @since 1.5
   */
  public Class<?> getEnclosingClass()
  {
    // There are five kinds of classes (or interfaces):
    // a) Top level classes
    // b) Nested classes (static member classes)
    // c) Inner classes (non-static member classes)
    // d) Local classes (named classes declared within a method)
    // e) Anonymous classes

    // JVM Spec 4.8.6: A class must have an EnclosingMethod
    // attribute if and only if it is a local class or an
    // anonymous class.
    EnclosingMethodInfo enclosingInfo = getEnclosingMethodInfo();
    Class<?> enclosingCandidate;

    if (enclosingInfo == null)
    {
      // This is a top level or a nested class or an inner class (a, b, or c)
      enclosingCandidate = getDeclaringClass();
    } else
    {
      Class<?> enclosingClass = enclosingInfo.getEnclosingClass();
      // This is a local class or an anonymous class (d or e)
      if (enclosingClass == this || enclosingClass == null)
        throw new InternalError("Malformed enclosing method information");
      else
        enclosingCandidate = enclosingClass;
    }

    // if (enclosingCandidate != null)
    // enclosingCandidate.checkPackageAccess(ClassLoader.getClassLoader(JikesRVMSupport.getCallerClass()),
    // true);
    return enclosingCandidate;
  }

  /**
   * Returns the simple name of the underlying class as given in the source code.
   * Returns an empty string if the underlying class is anonymous.
   *
   * <p>
   * The simple name of an array is the simple name of the component type with
   * "[]" appended. In particular the simple name of an array whose component type
   * is anonymous is "[]".
   *
   * @return the simple name of the underlying class
   * @since 1.5
   */
  public String getSimpleName()
  {
    if (isArray())
      return getComponentType().getSimpleName() + "[]";

    String simpleName = getSimpleBinaryName();
    if (simpleName == null)
    { // top level class
      simpleName = getName();
      return simpleName.substring(simpleName.lastIndexOf(".") + 1); // strip the package name
    }
    // According to JLS3 "Binary Compatibility" (13.1) the binary
    // name of non-package classes (not top level) is the binary
    // name of the immediately enclosing class followed by a '$' followed by:
    // (for nested and inner classes): the simple name.
    // (for local classes): 1 or more digits followed by the simple name.
    // (for anonymous classes): 1 or more digits.

    // Since getSimpleBinaryName() will strip the binary name of
    // the immediatly enclosing class, we are now looking at a
    // string that matches the regular expression "\$[0-9]*"
    // followed by a simple name (considering the simple of an
    // anonymous class to be the empty string).

    // Remove leading "\$[0-9]*" from the name
    int length = simpleName.length();
    if (length < 1 || simpleName.charAt(0) != '$')
      throw new InternalError("Malformed class name");
    int index = 1;
    while (index < length && isAsciiDigit(simpleName.charAt(index)))
      index++;
    // Eventually, this is the empty string iff this is an anonymous class
    return simpleName.substring(index);
  }

  /**
   * Character.isDigit answers {@code true} to some non-ascii digits. This one
   * does not.
   */
  private static boolean isAsciiDigit(char c)
  {
    return '0' <= c && c <= '9';
  }

  /**
   * Returns the canonical name of the underlying class as defined by the Java
   * Language Specification. Returns null if the underlying class does not have a
   * canonical name (i.e., if it is a local or anonymous class or an array whose
   * component type does not have a canonical name).
   * 
   * @return the canonical name of the underlying class if it exists, and
   *         {@code null} otherwise.
   * @since 1.5
   */
  public String getCanonicalName()
  {
    if (isArray())
    {
      String canonicalName = getComponentType().getCanonicalName();
      if (canonicalName != null)
        return canonicalName + "[]";
      else
        return null;
    }
    if (isLocalOrAnonymousClass())
      return null;
    Class<?> enclosingClass = getEnclosingClass();
    if (enclosingClass == null)
    { // top level class
      return getName();
    } else
    {
      String enclosingName = enclosingClass.getCanonicalName();
      if (enclosingName == null)
        return null;
      return enclosingName + "." + getSimpleName();
    }
  }

  /**
   * Returns {@code true} if and only if the underlying class is an anonymous
   * class.
   *
   * @return {@code true} if and only if this class is an anonymous class.
   * @since 1.5
   */
  public boolean isAnonymousClass()
  {
    return "".equals(getSimpleName());
  }

  /**
   * Returns {@code true} if and only if the underlying class is a local class.
   *
   * @return {@code true} if and only if this class is a local class.
   * @since 1.5
   */
  public boolean isLocalClass()
  {
    return isLocalOrAnonymousClass() && !isAnonymousClass();
  }

  /**
   * Returns {@code true} if and only if the underlying class is a member class.
   *
   * @return {@code true} if and only if this class is a member class.
   * @since 1.5
   */
  public boolean isMemberClass()
  {
    return getSimpleBinaryName() != null && !isLocalOrAnonymousClass();
  }

  /**
   * Returns the "simple binary name" of the underlying class, i.e., the binary
   * name without the leading enclosing class name. Returns {@code null} if the
   * underlying class is a top level class.
   */
  private String getSimpleBinaryName()
  {
    Class<?> enclosingClass = getEnclosingClass();
    if (enclosingClass == null) // top level class
      return null;
    // Otherwise, strip the enclosing class' name
    try
    {
      return getName().substring(enclosingClass.getName().length());
    } catch (IndexOutOfBoundsException ex)
    {
      throw new InternalError("Malformed class name");
    }
  }

  /**
   * Returns {@code true} if this is a local class or an anonymous class. Returns
   * {@code false} otherwise.
   */
  private boolean isLocalOrAnonymousClass()
  {
    // JVM Spec 4.8.6: A class must have an EnclosingMethod
    // attribute if and only if it is a local class or an
    // anonymous class.
    return getEnclosingMethodInfo() != null;
  }

  /**
   * Returns an array containing {@code Class} objects representing all the public
   * classes and interfaces that are members of the class represented by this
   * {@code Class} object. This includes public class and interface members
   * inherited from superclasses and public class and interface members declared
   * by the class. This method returns an array of length 0 if this {@code Class}
   * object has no public member classes or interfaces. This method also returns
   * an array of length 0 if this {@code Class} object represents a primitive
   * type, an array class, or void.
   *
   * @return the array of {@code Class} objects representing the public members of
   *         this class
   * @exception SecurityException
   *              If a security manager, <i>s</i>, is present and any of the
   *              following conditions is met:
   *
   *              <ul>
   *
   *              <li>invocation of {@link SecurityManager#checkMemberAccess
   *              s.checkMemberAccess(this, Member.PUBLIC)} method denies access
   *              to the classes within this class
   *
   *              <li>the caller's class loader is not the same as or an ancestor
   *              of the class loader for the current class and invocation of
   *              {@link SecurityManager#checkPackageAccess
   *              s.checkPackageAccess()} denies access to the package of this
   *              class
   *
   *              </ul>
   *
   * @since JDK1.1
   */
  public Class<?>[] getClasses()
  {
    // be very careful not to change the stack depth of this
    // checkMemberAccess call for security reasons
    // see java.lang.SecurityManager.checkMemberAccess
    checkMemberAccess(Member.PUBLIC);

    // Privileged so this implementation can look at DECLARED classes,
    // something the caller might not have privilege to do. The code here
    // is allowed to look at DECLARED classes because (1) it does not hand
    // out anything other than public members and (2) public member access
    // has already been ok'd by the SecurityManager.

    return java.security.AccessController.doPrivileged(new java.security.PrivilegedAction<Class<?>[]>() {
      public Class[] run()
      {
        List<Class<?>> list = new ArrayList<Class<?>>();
        Class<?> currentClass = Class.this;
        while (currentClass != null)
        {
          Class<?>[] members = currentClass.getDeclaredClasses();
          for (int i = 0; i < members.length; i++)
          {
            if (Modifier.isPublic(members[i].getModifiers()))
            {
              list.add(members[i]);
            }
          }
          currentClass = currentClass.getSuperclass();
        }
        return list.toArray(new Class[0]);
      }
    });
  }

  /**
   * Returns an array containing {@code Field} objects reflecting all the
   * accessible public fields of the class or interface represented by this
   * {@code Class} object. The elements in the array returned are not sorted and
   * are not in any particular order. This method returns an array of length 0 if
   * the class or interface has no accessible public fields, or if it represents
   * an array class, a primitive type, or void.
   *
   * <p>
   * Specifically, if this {@code Class} object represents a class, this method
   * returns the public fields of this class and of all its superclasses. If this
   * {@code Class} object represents an interface, this method returns the fields
   * of this interface and of all its superinterfaces.
   *
   * <p>
   * The implicit length field for array class is not reflected by this method.
   * User code should use the methods of class {@code Array} to manipulate arrays.
   *
   * <p>
   * See <em>The Java Language Specification</em>, sections 8.2 and 8.3.
   *
   * @return the array of {@code Field} objects representing the public fields
   * @exception SecurityException
   *              If a security manager, <i>s</i>, is present and any of the
   *              following conditions is met:
   *
   *              <ul>
   *
   *              <li>invocation of {@link SecurityManager#checkMemberAccess
   *              s.checkMemberAccess(this, Member.PUBLIC)} denies access to the
   *              fields within this class
   *
   *              <li>the caller's class loader is not the same as or an ancestor
   *              of the class loader for the current class and invocation of
   *              {@link SecurityManager#checkPackageAccess
   *              s.checkPackageAccess()} denies access to the package of this
   *              class
   *
   *              </ul>
   *
   * @since JDK1.1
   */
  public Field[] getFields() throws SecurityException
  {
    // be very careful not to change the stack depth of this
    // checkMemberAccess call for security reasons
    // see java.lang.SecurityManager.checkMemberAccess
    checkMemberAccess(Member.PUBLIC);

    RVMType type = java.lang.JikesRVMSupport.getTypeForClass((Class<?>) (Object) this);
    RVMField[] static_fields = type.getStaticFields();
    RVMField[] instance_fields = type.getInstanceFields();
    ArrayList<Field> coll = new ArrayList<Field>(static_fields.length + instance_fields.length);
    for (RVMField field : static_fields)
    {
      if (field.isPublic())
      {
        coll.add(JikesRVMSupport.createField(field));
      }
    }
    for (RVMField field : instance_fields)
    {
      if (field.isPublic())
      {
        coll.add(JikesRVMSupport.createField(field));
      }
    }

    return coll.toArray(new Field[coll.size()]);
  }

  /**
   * Returns an array containing {@code Method} objects reflecting all the public
   * <em>member</em> methods of the class or interface represented by this
   * {@code Class} object, including those declared by the class or interface and
   * those inherited from superclasses and superinterfaces. Array classes return
   * all the (public) member methods inherited from the {@code Object} class. The
   * elements in the array returned are not sorted and are not in any particular
   * order. This method returns an array of length 0 if this {@code Class} object
   * represents a class or interface that has no public member methods, or if this
   * {@code Class} object represents a primitive type or void.
   *
   * <p>
   * The class initialization method {@code <clinit>} is not included in the
   * returned array. If the class declares multiple public member methods with the
   * same parameter types, they are all included in the returned array.
   *
   * <p>
   * See <em>The Java Language Specification</em>, sections 8.2 and 8.4.
   *
   * @return the array of {@code Method} objects representing the public methods
   *         of this class
   * @exception SecurityException
   *              If a security manager, <i>s</i>, is present and any of the
   *              following conditions is met:
   *
   *              <ul>
   *
   *              <li>invocation of {@link SecurityManager#checkMemberAccess
   *              s.checkMemberAccess(this, Member.PUBLIC)} denies access to the
   *              methods within this class
   *
   *              <li>the caller's class loader is not the same as or an ancestor
   *              of the class loader for the current class and invocation of
   *              {@link SecurityManager#checkPackageAccess
   *              s.checkPackageAccess()} denies access to the package of this
   *              class
   *
   *              </ul>
   *
   * @since JDK1.1
   */
  public Method[] getMethods() throws SecurityException
  {
    // be very careful not to change the stack depth of this
    // checkMemberAccess call for security reasons
    // see java.lang.SecurityManager.checkMemberAccess
    checkMemberAccess(Member.PUBLIC);

    RVMType type = java.lang.JikesRVMSupport.getTypeForClass((Class<?>) (Object) this);
    RVMMethod[] static_methods = type.getStaticMethods();
    RVMMethod[] virtual_methods = type.getVirtualMethods();
    ArrayList<Method> coll = new ArrayList<Method>(static_methods.length + virtual_methods.length);
    for (RVMMethod meth : static_methods)
    {
      if (meth.isPublic())
      {
        coll.add(JikesRVMSupport.createMethod(meth));
      }
    }
    for (RVMMethod meth : virtual_methods)
    {
      if (meth.isPublic())
      {
        coll.add(JikesRVMSupport.createMethod(meth));
      }
    }
    return coll.toArray(new Method[coll.size()]);
  }

  /**
   * Returns an array containing {@code Constructor} objects reflecting all the
   * public constructors of the class represented by this {@code Class} object. An
   * array of length 0 is returned if the class has no public constructors, or if
   * the class is an array class, or if the class reflects a primitive type or
   * void.
   *
   * Note that while this method returns an array of {@code
   * Constructor<T>} objects (that is an array of constructors from this class),
   * the return type of this method is {@code
   * Constructor<?>[]} and <em>not</em> {@code Constructor<T>[]} as might be
   * expected. This less informative return type is necessary since after being
   * returned from this method, the array could be modified to hold
   * {@code Constructor} objects for different classes, which would violate the
   * type guarantees of {@code Constructor<T>[]}.
   *
   * @return the array of {@code Constructor} objects representing the public
   *         constructors of this class
   * @exception SecurityException
   *              If a security manager, <i>s</i>, is present and any of the
   *              following conditions is met:
   *
   *              <ul>
   *
   *              <li>invocation of {@link SecurityManager#checkMemberAccess
   *              s.checkMemberAccess(this, Member.PUBLIC)} denies access to the
   *              constructors within this class
   *
   *              <li>the caller's class loader is not the same as or an ancestor
   *              of the class loader for the current class and invocation of
   *              {@link SecurityManager#checkPackageAccess
   *              s.checkPackageAccess()} denies access to the package of this
   *              class
   *
   *              </ul>
   *
   * @since JDK1.1
   */
  public Constructor<?>[] getConstructors() throws SecurityException
  {
    // be very careful not to change the stack depth of this
    // checkMemberAccess call for security reasons
    // see java.lang.SecurityManager.checkMemberAccess
    checkMemberAccess(Member.PUBLIC);
    RVMType type = java.lang.JikesRVMSupport.getTypeForClass((Class<?>) (Object) this);
    if (!type.isClassType())
      return new Constructor[0];

    RVMMethod[] methods = type.asClass().getConstructorMethods();
    ArrayList<Constructor<T>> coll = new ArrayList<Constructor<T>>(methods.length);
    for (RVMMethod method : methods)
    {
      if (method.isPublic())
      {
        @SuppressWarnings("unchecked")
        Constructor<T> x = (Constructor<T>) JikesRVMSupport.createConstructor(method);
        coll.add(x);
      }
    }
    return coll.toArray(new Constructor[coll.size()]);
  }

  @Pure
  private RVMField getFieldInternal(Atom name, RVMType type)
  {
    RVMClass ctype = type.asClass();
    // (1) Check my public declared fields
    RVMField[] fields = ctype.getDeclaredFields();
    for (RVMField field : fields)
    {
      if (field.isPublic() && field.getName() == name)
      {
        return field;
      }
    }

    // (2) Check superinterfaces
    RVMClass[] interfaces = ctype.getDeclaredInterfaces();
    for (RVMClass anInterface : interfaces)
    {
      RVMField ans = anInterface.getClassForType().getFieldInternal(name, type);
      if (ans != null)
        return ans;
    }

    // (3) Check superclass (if I have one).
    if (ctype.getSuperClass() != null)
    {
      return ctype.getSuperClass().getClassForType().getFieldInternal(name, type);
    }

    return null;
  }

  /**
   * Returns a {@code Field} object that reflects the specified public member
   * field of the class or interface represented by this {@code Class} object. The
   * {@code name} parameter is a {@code String} specifying the simple name of the
   * desired field.
   *
   * <p>
   * The field to be reflected is determined by the algorithm that follows. Let C
   * be the class represented by this object:
   * <OL>
   * <LI>If C declares a public field with the name specified, that is the field
   * to be reflected.</LI>
   * <LI>If no field was found in step 1 above, this algorithm is applied
   * recursively to each direct superinterface of C. The direct superinterfaces
   * are searched in the order they were declared.</LI>
   * <LI>If no field was found in steps 1 and 2 above, and C has a superclass S,
   * then this algorithm is invoked recursively upon S. If C has no superclass,
   * then a {@code NoSuchFieldException} is thrown.</LI>
   * </OL>
   *
   * <p>
   * See <em>The Java Language Specification</em>, sections 8.2 and 8.3.
   *
   * @param name
   *          the field name
   * @return the {@code Field} object of this class specified by {@code name}
   * @exception NoSuchFieldException
   *              if a field with the specified name is not found.
   * @exception NullPointerException
   *              if {@code name} is {@code null}
   * @exception SecurityException
   *              If a security manager, <i>s</i>, is present and any of the
   *              following conditions is met:
   *
   *              <ul>
   *
   *              <li>invocation of {@link SecurityManager#checkMemberAccess
   *              s.checkMemberAccess(this, Member.PUBLIC)} denies access to the
   *              field
   *
   *              <li>the caller's class loader is not the same as or an ancestor
   *              of the class loader for the current class and invocation of
   *              {@link SecurityManager#checkPackageAccess
   *              s.checkPackageAccess()} denies access to the package of this
   *              class
   *
   *              </ul>
   *
   * @since JDK1.1
   */
  public Field getField(String name) throws NoSuchFieldException, SecurityException
  {
    // be very careful not to change the stack depth of this
    // checkMemberAccess call for security reasons
    // see java.lang.SecurityManager.checkMemberAccess
    checkMemberAccess(Member.PUBLIC);
    RVMType type = java.lang.JikesRVMSupport.getTypeForClass((Class<?>) (Object) this);
    if (!type.isClassType())
      throw new NoSuchFieldException();

    Atom aName = Atom.findUnicodeAtom(name);
    if (aName == null)
      throwNoSuchFieldException(name);

    RVMField answer = getFieldInternal(aName, type);

    if (answer == null)
    {
      throwNoSuchFieldException(name);
    }
    return JikesRVMSupport.createField(answer);
  }

  @Pure
  private RVMMethod getMethodInternal1(Atom aName, Class<?>... parameterTypes)
  {
    RVMMethod answer = null;
    RVMType type = java.lang.JikesRVMSupport.getTypeForClass((Class<?>) (Object) this);
    for (RVMClass current = type.asClass(); current != null && answer == null; current = current.getSuperClass())
    {
      RVMMethod[] methods = current.getDeclaredMethods();
      for (RVMMethod meth : methods)
      {
        if (meth.getName() == aName && meth.isPublic() && parametersMatch(meth.getParameterTypes(), parameterTypes))
        {
          if (answer == null)
          {
            answer = meth;
          } else
          {
            RVMMethod m2 = meth;
            if (answer.getReturnType().resolve().isAssignableFrom(m2.getReturnType().resolve()))
            {
              answer = m2;
            }
          }
        }
      }
    }
    return answer;
  }

  @Pure
  private RVMMethod getMethodInternal2(Atom aName, Class<?>... parameterTypes)
  {
    RVMMethod answer = null;
    RVMType type = java.lang.JikesRVMSupport.getTypeForClass((Class<?>) (Object) this);
    RVMMethod[] methods = type.asClass().getVirtualMethods();
    for (RVMMethod meth : methods)
    {
      if (meth.getName() == aName && meth.isPublic() && parametersMatch(meth.getParameterTypes(), parameterTypes))
      {
        if (answer == null)
        {
          answer = meth;
        } else
        {
          RVMMethod m2 = meth;
          if (answer.getReturnType().resolve().isAssignableFrom(m2.getReturnType().resolve()))
          {
            answer = m2;
          }
        }
      }
    }
    return answer;
  }

  /**
   * Returns a {@code Method} object that reflects the specified public member
   * method of the class or interface represented by this {@code Class} object.
   * The {@code name} parameter is a {@code String} specifying the simple name of
   * the desired method. The {@code parameterTypes} parameter is an array of
   * {@code Class} objects that identify the method's formal parameter types, in
   * declared order. If {@code parameterTypes} is {@code null}, it is treated as
   * if it were an empty array.
   *
   * <p>
   * If the {@code name} is "{@code <init>};"or "{@code <clinit>}" a
   * {@code NoSuchMethodException} is raised. Otherwise, the method to be
   * reflected is determined by the algorithm that follows. Let C be the class
   * represented by this object:
   * <OL>
   * <LI>C is searched for any <I>matching methods</I>. If no matching method is
   * found, the algorithm of step 1 is invoked recursively on the superclass of
   * C.</LI>
   * <LI>If no method was found in step 1 above, the superinterfaces of C are
   * searched for a matching method. If any such method is found, it is
   * reflected.</LI>
   * </OL>
   *
   * To find a matching method in a class C:&nbsp; If C declares exactly one
   * public method with the specified name and exactly the same formal parameter
   * types, that is the method reflected. If more than one such method is found in
   * C, and one of these methods has a return type that is more specific than any
   * of the others, that method is reflected; otherwise one of the methods is
   * chosen arbitrarily.
   *
   * <p>
   * Note that there may be more than one matching method in a class because while
   * the Java language forbids a class to declare multiple methods with the same
   * signature but different return types, the Java virtual machine does not. This
   * increased flexibility in the virtual machine can be used to implement various
   * language features. For example, covariant returns can be implemented with
   * {@linkplain java.lang.reflect.Method#isBridge bridge methods}; the bridge
   * method and the method being overridden would have the same signature but
   * different return types.
   *
   * <p>
   * See <em>The Java Language Specification</em>, sections 8.2 and 8.4.
   *
   * @param name
   *          the name of the method
   * @param parameterTypes
   *          the list of parameters
   * @return the {@code Method} object that matches the specified {@code name} and
   *         {@code parameterTypes}
   * @exception NoSuchMethodException
   *              if a matching method is not found or if the name is
   *              "&lt;init&gt;"or "&lt;clinit&gt;".
   * @exception NullPointerException
   *              if {@code name} is {@code null}
   * @exception SecurityException
   *              If a security manager, <i>s</i>, is present and any of the
   *              following conditions is met:
   *
   *              <ul>
   *
   *              <li>invocation of {@link SecurityManager#checkMemberAccess
   *              s.checkMemberAccess(this, Member.PUBLIC)} denies access to the
   *              method
   *
   *              <li>the caller's class loader is not the same as or an ancestor
   *              of the class loader for the current class and invocation of
   *              {@link SecurityManager#checkPackageAccess
   *              s.checkPackageAccess()} denies access to the package of this
   *              class
   *
   *              </ul>
   *
   * @since JDK1.1
   */
  public Method getMethod(String name, Class<?>... parameterTypes) throws NoSuchMethodException, SecurityException
  {
    checkMemberAccess(Member.PUBLIC);

    RVMType type = java.lang.JikesRVMSupport.getTypeForClass((Class<?>) (Object) this);
    if (!type.isClassType())
      throwNoSuchMethodException(name, parameterTypes);

    if (name == null)
    {
      throwNoSuchMethodException(name, parameterTypes);
    }
    Atom aName = Atom.findOrCreateUnicodeAtom(name);
    if (aName == RVMClassLoader.StandardClassInitializerMethodName
    || aName == RVMClassLoader.StandardObjectInitializerMethodName)
    {
      // <init> and <clinit> are not methods.
      throwNoSuchMethodException(name, parameterTypes);
    }

    // (1) Scan the declared public methods of this class and each of its
    // superclasses
    RVMMethod answer = getMethodInternal1(aName, parameterTypes);
    if (answer == null)
    {
      // (2) Now we need to consider methods inherited from interfaces.
      // Because we inject the requisite Miranda methods, we can do this simply
      // by looking at this class's virtual methods instead of searching interface
      // hierarchies.
      answer = getMethodInternal2(aName, parameterTypes);
    }
    if (answer == null)
    {
      throwNoSuchMethodException(name, parameterTypes);
    }
    return JikesRVMSupport.createMethod(answer);
  }


  @Pure
  private RVMMethod getDefaultConstructor()
  {
    if (defaultConstructor == null)
    {
      RVMMethod[] methods = type.asClass().getConstructorMethods();
      for (RVMMethod method : methods)
      {
        if (method.getParameterTypes().length == 0)
        {
          defaultConstructor = method;
          break;
        }
      }
    }
    return defaultConstructor;
  }

  /**
   * Returns a {@code Constructor} object that reflects the specified public
   * constructor of the class represented by this {@code Class} object. The
   * {@code parameterTypes} parameter is an array of {@code Class} objects that
   * identify the constructor's formal parameter types, in declared order.
   *
   * If this {@code Class} object represents an inner class declared in a
   * non-static context, the formal parameter types include the explicit enclosing
   * instance as the first parameter.
   *
   * <p>
   * The constructor to reflect is the public constructor of the class represented
   * by this {@code Class} object whose formal parameter types match those
   * specified by {@code parameterTypes}.
   *
   * @param parameterTypes
   *          the parameter array
   * @return the {@code Constructor} object of the public constructor that matches
   *         the specified {@code parameterTypes}
   * @exception NoSuchMethodException
   *              if a matching method is not found.
   * @exception SecurityException
   *              If a security manager, <i>s</i>, is present and any of the
   *              following conditions is met:
   *
   *              <ul>
   *
   *              <li>invocation of {@link SecurityManager#checkMemberAccess
   *              s.checkMemberAccess(this, Member.PUBLIC)} denies access to the
   *              constructor
   *
   *              <li>the caller's class loader is not the same as or an ancestor
   *              of the class loader for the current class and invocation of
   *              {@link SecurityManager#checkPackageAccess
   *              s.checkPackageAccess()} denies access to the package of this
   *              class
   *
   *              </ul>
   *
   * @since JDK1.1
   */
  public Constructor<T> getConstructor(Class<?>... parameterTypes) throws NoSuchMethodException, SecurityException
  {
    // be very careful not to change the stack depth of this
    // checkMemberAccess call for security reasons
    // see java.lang.SecurityManager.checkMemberAccess
    checkMemberAccess(Member.PUBLIC);
    RVMType type = java.lang.JikesRVMSupport.getTypeForClass((Class<?>) (Object) this);
    if (!type.isClassType())
      throwNoSuchMethodException("<init>", parameterTypes);

    RVMMethod answer = null;
    if (parameterTypes == null || parameterTypes.length == 0)
    {
      answer = getDefaultConstructor();
    } else
    {
      RVMMethod[] methods = type.asClass().getConstructorMethods();
      for (RVMMethod method : methods)
      {
        if (method.isPublic() && parametersMatch(method.getParameterTypes(), parameterTypes))
        {
          answer = method;
          break;
        }
      }
    }
    if (answer == null)
    {
      throwNoSuchMethodException("<init>", parameterTypes);
    }
    return JikesRVMSupport.createConstructor(answer);
  }

  /**
   * Returns an array of {@code Class} objects reflecting all the classes and
   * interfaces declared as members of the class represented by this {@code Class}
   * object. This includes public, protected, default (package) access, and
   * private classes and interfaces declared by the class, but excludes inherited
   * classes and interfaces. This method returns an array of length 0 if the class
   * declares no classes or interfaces as members, or if this {@code Class} object
   * represents a primitive type, an array class, or void.
   *
   * @return the array of {@code Class} objects representing all the declared
   *         members of this class
   * @exception SecurityException
   *              If a security manager, <i>s</i>, is present and any of the
   *              following conditions is met:
   *
   *              <ul>
   *
   *              <li>invocation of {@link SecurityManager#checkMemberAccess
   *              s.checkMemberAccess(this, Member.DECLARED)} denies access to the
   *              declared classes within this class
   *
   *              <li>the caller's class loader is not the same as or an ancestor
   *              of the class loader for the current class and invocation of
   *              {@link SecurityManager#checkPackageAccess
   *              s.checkPackageAccess()} denies access to the package of this
   *              class
   *
   *              </ul>
   *
   * @since JDK1.1
   */
  public Class<?>[] getDeclaredClasses() throws SecurityException
  {
    // be very careful not to change the stack depth of this
    // checkMemberAccess call for security reasons
    // see java.lang.SecurityManager.checkMemberAccess
    checkMemberAccess(Member.DECLARED);
    RVMType type = java.lang.JikesRVMSupport.getTypeForClass((Class<?>) (Object) this);
    if (!type.isClassType())
      return new Class[0];

    // Get array of declared classes from RVMClass object
    RVMClass cls = type.asClass();
    TypeReference[] declaredClasses = cls.getDeclaredClasses();

    // The array can be null if the class has no declared inner class members
    if (declaredClasses == null)
      return new Class[0];

    // Count the number of actual declared inner and static classes.
    // (The array may contain null elements, which we want to skip.)
    int count = 0;
    int length = declaredClasses.length;
    for (int i = 0; i < length; ++i)
    {
      if (declaredClasses[i] != null)
      {
        ++count;
      }
    }

    // Now build actual result array.
    Class<?>[] result = new Class[count];
    count = 0;
    for (int i = 0; i < length; ++i)
    {
      if (declaredClasses[i] != null)
      {
        result[count++] = declaredClasses[i].resolve().getClassForType();
      }
    }

    return result;
  }

  /**
   * Returns an array of {@code Field} objects reflecting all the fields declared
   * by the class or interface represented by this {@code Class} object. This
   * includes public, protected, default (package) access, and private fields, but
   * excludes inherited fields. The elements in the array returned are not sorted
   * and are not in any particular order. This method returns an array of length 0
   * if the class or interface declares no fields, or if this {@code Class} object
   * represents a primitive type, an array class, or void.
   *
   * <p>
   * See <em>The Java Language Specification</em>, sections 8.2 and 8.3.
   *
   * @return the array of {@code Field} objects representing all the declared
   *         fields of this class
   * @exception SecurityException
   *              If a security manager, <i>s</i>, is present and any of the
   *              following conditions is met:
   *
   *              <ul>
   *
   *              <li>invocation of {@link SecurityManager#checkMemberAccess
   *              s.checkMemberAccess(this, Member.DECLARED)} denies access to the
   *              declared fields within this class
   *
   *              <li>the caller's class loader is not the same as or an ancestor
   *              of the class loader for the current class and invocation of
   *              {@link SecurityManager#checkPackageAccess
   *              s.checkPackageAccess()} denies access to the package of this
   *              class
   *
   *              </ul>
   *
   * @since JDK1.1
   */
  public Field[] getDeclaredFields() throws SecurityException
  {
    // be very careful not to change the stack depth of this
    // checkMemberAccess call for security reasons
    // see java.lang.SecurityManager.checkMemberAccess
    checkMemberAccess(Member.DECLARED);
    RVMType type = java.lang.JikesRVMSupport.getTypeForClass((Class<?>) (Object) this);

    if (!type.isClassType())
      return new Field[0];

    RVMField[] fields = type.asClass().getDeclaredFields();
    Field[] ans = new Field[fields.length];
    for (int i = 0; i < fields.length; i++)
    {
      ans[i] = JikesRVMSupport.createField(fields[i]);
    }
    return ans;
  }

  /**
   * Returns an array of {@code Method} objects reflecting all the methods
   * declared by the class or interface represented by this {@code Class} object.
   * This includes public, protected, default (package) access, and private
   * methods, but excludes inherited methods. The elements in the array returned
   * are not sorted and are not in any particular order. This method returns an
   * array of length 0 if the class or interface declares no methods, or if this
   * {@code Class} object represents a primitive type, an array class, or void.
   * The class initialization method {@code <clinit>} is not included in the
   * returned array. If the class declares multiple public member methods with the
   * same parameter types, they are all included in the returned array.
   *
   * <p>
   * See <em>The Java Language Specification</em>, section 8.2.
   *
   * @return the array of {@code Method} objects representing all the declared
   *         methods of this class
   * @exception SecurityException
   *              If a security manager, <i>s</i>, is present and any of the
   *              following conditions is met:
   *
   *              <ul>
   *
   *              <li>invocation of {@link SecurityManager#checkMemberAccess
   *              s.checkMemberAccess(this, Member.DECLARED)} denies access to the
   *              declared methods within this class
   *
   *              <li>the caller's class loader is not the same as or an ancestor
   *              of the class loader for the current class and invocation of
   *              {@link SecurityManager#checkPackageAccess
   *              s.checkPackageAccess()} denies access to the package of this
   *              class
   *
   *              </ul>
   *
   * @since JDK1.1
   */
  public Method[] getDeclaredMethods() throws SecurityException
  {
    // be very careful not to change the stack depth of this
    // checkMemberAccess call for security reasons
    // see java.lang.SecurityManager.checkMemberAccess
    checkMemberAccess(Member.DECLARED);
    RVMType type = java.lang.JikesRVMSupport.getTypeForClass((Class<?>) (Object) this);

    if (!type.isClassType())
      return new Method[0];

    RVMMethod[] methods = type.asClass().getDeclaredMethods();
    ArrayList<Method> coll = new ArrayList<Method>(methods.length);
    for (RVMMethod meth : methods)
    {
      if (!meth.isClassInitializer() && !meth.isObjectInitializer())
      {
        coll.add(JikesRVMSupport.createMethod(meth));
      }
    }
    return coll.toArray(new Method[coll.size()]);
  }

  /**
   * Returns an array of {@code Constructor} objects reflecting all the
   * constructors declared by the class represented by this {@code Class} object.
   * These are public, protected, default (package) access, and private
   * constructors. The elements in the array returned are not sorted and are not
   * in any particular order. If the class has a default constructor, it is
   * included in the returned array. This method returns an array of length 0 if
   * this {@code Class} object represents an interface, a primitive type, an array
   * class, or void.
   *
   * <p>
   * See <em>The Java Language Specification</em>, section 8.2.
   *
   * @return the array of {@code Constructor} objects representing all the
   *         declared constructors of this class
   * @exception SecurityException
   *              If a security manager, <i>s</i>, is present and any of the
   *              following conditions is met:
   *
   *              <ul>
   *
   *              <li>invocation of {@link SecurityManager#checkMemberAccess
   *              s.checkMemberAccess(this, Member.DECLARED)} denies access to the
   *              declared constructors within this class
   *
   *              <li>the caller's class loader is not the same as or an ancestor
   *              of the class loader for the current class and invocation of
   *              {@link SecurityManager#checkPackageAccess
   *              s.checkPackageAccess()} denies access to the package of this
   *              class
   *
   *              </ul>
   *
   * @since JDK1.1
   */
  public Constructor<?>[] getDeclaredConstructors() throws SecurityException
  {
    // be very careful not to change the stack depth of this
    // checkMemberAccess call for security reasons
    // see java.lang.SecurityManager.checkMemberAccess
    checkMemberAccess(Member.DECLARED);
    RVMType type = java.lang.JikesRVMSupport.getTypeForClass((Class<?>) (Object) this);

    if (!type.isClassType()) return new Constructor[0];
    RVMMethod[] methods = type.asClass().getConstructorMethods();
    Constructor<?>[] ans = new Constructor[methods.length];
    for (int i = 0; i<methods.length; i++) {
      ans[i] = JikesRVMSupport.createConstructor(methods[i]);
    }
    return ans;
  }

  /**
   * Returns a {@code Field} object that reflects the specified declared field of
   * the class or interface represented by this {@code Class} object. The
   * {@code name} parameter is a {@code String} that specifies the simple name of
   * the desired field. Note that this method will not reflect the {@code length}
   * field of an array class.
   *
   * @param name
   *          the name of the field
   * @return the {@code Field} object for the specified field in this class
   * @exception NoSuchFieldException
   *              if a field with the specified name is not found.
   * @exception NullPointerException
   *              if {@code name} is {@code null}
   * @exception SecurityException
   *              If a security manager, <i>s</i>, is present and any of the
   *              following conditions is met:
   *
   *              <ul>
   *
   *              <li>invocation of {@link SecurityManager#checkMemberAccess
   *              s.checkMemberAccess(this, Member.DECLARED)} denies access to the
   *              declared field
   *
   *              <li>the caller's class loader is not the same as or an ancestor
   *              of the class loader for the current class and invocation of
   *              {@link SecurityManager#checkPackageAccess
   *              s.checkPackageAccess()} denies access to the package of this
   *              class
   *
   *              </ul>
   *
   * @since JDK1.1
   */
  public Field getDeclaredField(String name) throws NoSuchFieldException, SecurityException
  {
    // be very careful not to change the stack depth of this
    // checkMemberAccess call for security reasons
    // see java.lang.SecurityManager.checkMemberAccess
    checkMemberAccess(Member.DECLARED);
    RVMType type = java.lang.JikesRVMSupport.getTypeForClass((Class<?>) (Object) this);
    if (!type.isClassType() || name == null)
      throwNoSuchFieldException(name);

    Atom aName = Atom.findOrCreateUnicodeAtom(name);
    RVMField answer = type.asClass().findDeclaredField(aName);
    if (answer == null)
    {
      throwNoSuchFieldException(name);
    }
    return JikesRVMSupport.createField(answer);
  }

  /**
   * Returns a {@code Method} object that reflects the specified declared method
   * of the class or interface represented by this {@code Class} object. The
   * {@code name} parameter is a {@code String} that specifies the simple name of
   * the desired method, and the {@code parameterTypes} parameter is an array of
   * {@code Class} objects that identify the method's formal parameter types, in
   * declared order. If more than one method with the same parameter types is
   * declared in a class, and one of these methods has a return type that is more
   * specific than any of the others, that method is returned; otherwise one of
   * the methods is chosen arbitrarily. If the name is "&lt;init&gt;"or
   * "&lt;clinit&gt;" a {@code NoSuchMethodException} is raised.
   *
   * @param name
   *          the name of the method
   * @param parameterTypes
   *          the parameter array
   * @return the {@code Method} object for the method of this class matching the
   *         specified name and parameters
   * @exception NoSuchMethodException
   *              if a matching method is not found.
   * @exception NullPointerException
   *              if {@code name} is {@code null}
   * @exception SecurityException
   *              If a security manager, <i>s</i>, is present and any of the
   *              following conditions is met:
   *
   *              <ul>
   *
   *              <li>invocation of {@link SecurityManager#checkMemberAccess
   *              s.checkMemberAccess(this, Member.DECLARED)} denies access to the
   *              declared method
   *
   *              <li>the caller's class loader is not the same as or an ancestor
   *              of the class loader for the current class and invocation of
   *              {@link SecurityManager#checkPackageAccess
   *              s.checkPackageAccess()} denies access to the package of this
   *              class
   *
   *              </ul>
   *
   * @since JDK1.1
   */
  public Method getDeclaredMethod(String name, Class<?>... parameterTypes)
  throws NoSuchMethodException, SecurityException
  {
    // be very careful not to change the stack depth of this
    // checkMemberAccess call for security reasons
    // see java.lang.SecurityManager.checkMemberAccess
    checkMemberAccess(Member.DECLARED);
    RVMType type = java.lang.JikesRVMSupport.getTypeForClass((Class<?>) (Object) this);
    
    if (!type.isClassType()) throwNoSuchMethodException(name, parameterTypes);

    if (name == null) {
      throwNoSuchMethodException(name, parameterTypes);
    }
    Atom aName = Atom.findOrCreateUnicodeAtom(name);
    if (aName == RVMClassLoader.StandardClassInitializerMethodName ||
        aName == RVMClassLoader.StandardObjectInitializerMethodName) {
      // <init> and <clinit> are not methods.
      throwNoSuchMethodException(name, parameterTypes);
    }

    RVMMethod[] methods = type.asClass().getDeclaredMethods();
    RVMMethod answer = null;
    for (RVMMethod meth : methods) {
      if (meth.getName() == aName &&
          parametersMatch(meth.getParameterTypes(), parameterTypes)) {
        if (answer == null) {
          answer = meth;
        } else {
          RVMMethod m2 = meth;
          if (answer.getReturnType().resolve().isAssignableFrom(m2.getReturnType().resolve())) {
            answer = m2;
          }
        }
      }
    }
    if (answer == null) {
      throwNoSuchMethodException(name, parameterTypes);
    }
    return JikesRVMSupport.createMethod(answer);
  }

  /**
   * Returns a {@code Constructor} object that reflects the specified constructor
   * of the class or interface represented by this {@code Class} object. The
   * {@code parameterTypes} parameter is an array of {@code Class} objects that
   * identify the constructor's formal parameter types, in declared order.
   *
   * If this {@code Class} object represents an inner class declared in a
   * non-static context, the formal parameter types include the explicit enclosing
   * instance as the first parameter.
   *
   * @param parameterTypes
   *          the parameter array
   * @return The {@code Constructor} object for the constructor with the specified
   *         parameter list
   * @exception NoSuchMethodException
   *              if a matching method is not found.
   * @exception SecurityException
   *              If a security manager, <i>s</i>, is present and any of the
   *              following conditions is met:
   *
   *              <ul>
   *
   *              <li>invocation of {@link SecurityManager#checkMemberAccess
   *              s.checkMemberAccess(this, Member.DECLARED)} denies access to the
   *              declared constructor
   *
   *              <li>the caller's class loader is not the same as or an ancestor
   *              of the class loader for the current class and invocation of
   *              {@link SecurityManager#checkPackageAccess
   *              s.checkPackageAccess()} denies access to the package of this
   *              class
   *
   *              </ul>
   *
   * @since JDK1.1
   */
  public Constructor<T> getDeclaredConstructor(Class<?>... parameterTypes)
  throws NoSuchMethodException, SecurityException
  {
    // be very careful not to change the stack depth of this
    // checkMemberAccess call for security reasons
    // see java.lang.SecurityManager.checkMemberAccess
    checkMemberAccess(Member.DECLARED);
    RVMType type = java.lang.JikesRVMSupport.getTypeForClass((Class<?>) (Object) this);
    if (!type.isClassType())
      throwNoSuchMethodException("<init>", parameterTypes);

    RVMMethod answer = null;
    if (parameterTypes == null || parameterTypes.length == 0)
    {
      answer = getDefaultConstructor();
    } else
    {
      RVMMethod[] methods = type.asClass().getConstructorMethods();
      for (RVMMethod method : methods)
      {
        if (parametersMatch(method.getParameterTypes(), parameterTypes))
        {
          answer = method;
          break;
        }
      }
    }
    if (answer == null)
    {
      throwNoSuchMethodException("<init>", parameterTypes);
    }
    return JikesRVMSupport.createConstructor(answer);

  }

  /**
   * Finds a resource with a given name. The rules for searching resources
   * associated with a given class are implemented by the defining
   * {@linkplain ClassLoader class loader} of the class. This method delegates to
   * this object's class loader. If this object was loaded by the bootstrap class
   * loader, the method delegates to
   * {@link ClassLoader#getSystemResourceAsStream}.
   *
   * <p>
   * Before delegation, an absolute resource name is constructed from the given
   * resource name using this algorithm:
   *
   * <ul>
   *
   * <li>If the {@code name} begins with a {@code '/'} (<tt>'&#92;u002f'</tt>),
   * then the absolute name of the resource is the portion of the {@code name}
   * following the {@code '/'}.
   *
   * <li>Otherwise, the absolute name is of the following form:
   *
   * <blockquote> {@code modified_package_name/name} </blockquote>
   *
   * <p>
   * Where the {@code modified_package_name} is the package name of this object
   * with {@code '/'} substituted for {@code '.'} (<tt>'&#92;u002e'</tt>).
   *
   * </ul>
   *
   * @param name
   *          name of the desired resource
   * @return A {@link java.io.InputStream} object or {@code null} if no resource
   *         with this name is found
   * @throws NullPointerException
   *           If {@code name} is {@code null}
   * @since JDK1.1
   */
  public InputStream getResourceAsStream(String name)
  {
    name = resolveName(name);
    ClassLoader cl = getClassLoader0();
    if (cl == null)
    {
      // A system class.
      return ClassLoader.getSystemResourceAsStream(name);
    }
    return cl.getResourceAsStream(name);
  }

  /**
   * Finds a resource with a given name. The rules for searching resources
   * associated with a given class are implemented by the defining
   * {@linkplain ClassLoader class loader} of the class. This method delegates to
   * this object's class loader. If this object was loaded by the bootstrap class
   * loader, the method delegates to {@link ClassLoader#getSystemResource}.
   *
   * <p>
   * Before delegation, an absolute resource name is constructed from the given
   * resource name using this algorithm:
   *
   * <ul>
   *
   * <li>If the {@code name} begins with a {@code '/'} (<tt>'&#92;u002f'</tt>),
   * then the absolute name of the resource is the portion of the {@code name}
   * following the {@code '/'}.
   *
   * <li>Otherwise, the absolute name is of the following form:
   *
   * <blockquote> {@code modified_package_name/name} </blockquote>
   *
   * <p>
   * Where the {@code modified_package_name} is the package name of this object
   * with {@code '/'} substituted for {@code '.'} (<tt>'&#92;u002e'</tt>).
   *
   * </ul>
   *
   * @param name
   *          name of the desired resource
   * @return A {@link java.net.URL} object or {@code null} if no resource with
   *         this name is found
   * @since JDK1.1
   */
  public java.net.URL getResource(String name)
  {
    name = resolveName(name);
    ClassLoader cl = getClassLoader0();
    if (cl == null)
    {
      // A system class.
      return ClassLoader.getSystemResource(name);
    }
    return cl.getResource(name);
  }

  /** protection domain returned when the internal domain is null */
  private static java.security.ProtectionDomain allPermDomain;

  /**
   * Returns the {@code ProtectionDomain} of this class. If there is a security
   * manager installed, this method first calls the security manager's
   * {@code checkPermission} method with a
   * {@code RuntimePermission("getProtectionDomain")} permission to ensure it's ok
   * to get the {@code ProtectionDomain}.
   *
   * @return the ProtectionDomain of this class
   *
   * @throws SecurityException
   *           if a security manager exists and its {@code checkPermission} method
   *           doesn't allow getting the ProtectionDomain.
   *
   * @see java.security.ProtectionDomain
   * @see SecurityManager#checkPermission
   * @see java.lang.RuntimePermission
   * @since 1.2
   */
  public java.security.ProtectionDomain getProtectionDomain()
  {
    SecurityManager sm = System.getSecurityManager();
    if (sm != null)
    {
      sm.checkPermission(SecurityConstants.GET_PD_PERMISSION);
    }
    if (pd == null)
    {
      if (allPermDomain == null)
      {
        java.security.Permissions perms = new java.security.Permissions();
        perms.add(SecurityConstants.ALL_PERMISSION);
        allPermDomain = new java.security.ProtectionDomain(null, perms);
      }
      pd = allPermDomain;
    }
    return pd;
  }

  /**
   * Set the ProtectionDomain for this class. Called by ClassLoader.defineClass.
   */
  void setProtectionDomain0(java.security.ProtectionDomain pd)
  {
    this.pd = pd;
  }

  /*
   * Return the Virtual Machine's Class object for the named primitive type.
   */
  static Class<?> getPrimitiveClass(String className)
  {
    TypeReference typeRef = TypeReference.mapPrimitiveClassNameToTypeReference(className);
    if (typeRef != null)
    {
      return typeRef.resolve().getClassForType();
    }
    VM.sysFail("Unknown primitive type name: " + className);
    return null;
  }

  /**
   * Add a package name prefix if the name is not absolute Remove leading "/" if
   * name is absolute
   */
  private String resolveName(String name)
  {
    if (name == null)
    {
      return name;
    }
    if (!name.startsWith("/"))
    {
      Class<?> c = this;
      while (c.isArray())
      {
        c = c.getComponentType();
      }
      String baseName = c.getName();
      int index = baseName.lastIndexOf('.');
      if (index != -1)
      {
        name = baseName.substring(0, index).replace('.', '/') + "/" + name;
      }
    } else
    {
      name = name.substring(1);
    }
    return name;
  }

  /**
   * Utility for security checks
   */
  private void checkMemberAccess(int type)
  {
    SecurityManager security = System.getSecurityManager();
    if (security != null)
    {
      security.checkMemberAccess(this, type);
      String packageName = getPackageName();
      if (packageName != "")
      {
        security.checkPackageAccess(packageName);
      }
    }
  }

  @Pure
  private String getPackageName()
  {
    String name = getName();
    int index = name.lastIndexOf('.');
    if (index >= 0)
      return name.substring(0, index);
    return "";
  }

  /**
   * Reflection support.
   */

  // reflection data that might get invalidated when JVM TI RedefineClasses() is
  // called
  static class ReflectionData<T>
  {
    volatile Field[]          declaredFields;
    volatile Field[]          publicFields;
    volatile Method[]         declaredMethods;
    volatile Method[]         publicMethods;
    volatile Constructor<T>[] declaredConstructors;
    volatile Constructor<T>[] publicConstructors;
    // Intermediate results for getFields and getMethods
    volatile Field[]  declaredPublicFields;
    volatile Method[] declaredPublicMethods;
    // Value of classRedefinedCount when we created this ReflectionData instance
    final int redefinedCount;

    ReflectionData(int redefinedCount)
    {
      this.redefinedCount = redefinedCount;
    }
  }

  // private volatile transient SoftReference<ReflectionData<T>> reflectionData;
  //
  // // Incremented by the VM on each call to JVM TI RedefineClasses()
  // // that redefines this class or a superclass.
  // private volatile transient int classRedefinedCount = 0;
  //
  // // Lazily create and cache ReflectionData
  // private ReflectionData<T> reflectionData()
  // {
  // SoftReference<ReflectionData<T>> reflectionData = this.reflectionData;
  // int classRedefinedCount = this.classRedefinedCount;
  // ReflectionData<T> rd;
  // if (useCaches && reflectionData != null && (rd = reflectionData.get()) !=
  // null
  // && rd.redefinedCount == classRedefinedCount)
  // {
  // return rd;
  // }
  // // else no SoftReference or cleared SoftReference or stale ReflectionData
  // // -> create and replace new instance
  // return newReflectionData(reflectionData, classRedefinedCount);
  // }
  //
  // private ReflectionData<T> newReflectionData(SoftReference<ReflectionData<T>>
  // oldReflectionData,
  // int classRedefinedCount)
  // {
  // if (!useCaches)
  // return null;
  //
  // while (true)
  // {
  // ReflectionData<T> rd = new ReflectionData<T>(classRedefinedCount);
  // // try to CAS it...
  // if (Atomic.casReflectionData(this, oldReflectionData, new
  // SoftReference<ReflectionData<T>>(rd)))
  // {
  // return rd;
  // }
  // // else retry
  // oldReflectionData = this.reflectionData;
  // classRedefinedCount = this.classRedefinedCount;
  // if (oldReflectionData != null && (rd = oldReflectionData.get()) != null
  // && rd.redefinedCount == classRedefinedCount)
  // {
  // return rd;
  // }
  // }
  // }

  // Generic signature handling
  private String getGenericSignature()
  {
    VM.sysFail("getGenericSignature");
    return null;
  }

  // Generic info repository; lazily initialized
  private transient ClassRepository genericInfo;

  // accessor for factory
  private GenericsFactory getFactory()
  {
    // create scope and factory
    return CoreReflectionFactory.make(this, ClassScope.make(this));
  }

  // accessor for generic info repository
  private ClassRepository getGenericInfo()
  {
    // lazily initialize repository if necessary
    if (genericInfo == null)
    {
      // create and cache generic info repository
      genericInfo = ClassRepository.make(getGenericSignature(), getFactory());
    }
    return genericInfo; // return cached repository
  }

  // Annotations handling
  byte[] getRawAnnotations()
  {
    RVMClass myType = java.lang.JikesRVMSupport.getTypeForClass((Class<?>) (Object) this).asClass();
    return myType.getRawAnnotations();
  }

  ConstantPool getConstantPool()
  {
    RVMClass myType = java.lang.JikesRVMSupport.getTypeForClass((Class<?>) (Object) this).asClass();
    OpenJDKConstantPool constantPool = new OpenJDKConstantPool(myType);
    return constantPool;
  }



  /** use serialVersionUID from JDK 1.1 for interoperability */
  private static final long serialVersionUID = 3206093459760846163L;

  /**
   * Class Class is special cased within the Serialization Stream Protocol.
   *
   * A Class instance is written initially into an ObjectOutputStream in the
   * following format:
   * 
   * <pre>
   *      {@code TC_CLASS} ClassDescriptor
   *      A ClassDescriptor is a special cased serialization of
   *      a {@code java.io.ObjectStreamClass} instance.
   * </pre>
   * 
   * A new handle is generated for the initial time the class descriptor is
   * written into the stream. Future references to the class descriptor are
   * written as references to the initial class descriptor instance.
   *
   * @see java.io.ObjectStreamClass
   */
  private static final ObjectStreamField[] serialPersistentFields = new ObjectStreamField[0];

  /**
   * Returns the assertion status that would be assigned to this class if it were
   * to be initialized at the time this method is invoked. If this class has had
   * its assertion status set, the most recent setting will be returned;
   * otherwise, if any package default assertion status pertains to this class,
   * the most recent setting for the most specific pertinent package default
   * assertion status is returned; otherwise, if this class is not a system class
   * (i.e., it has a class loader) its class loader's default assertion status is
   * returned; otherwise, the system class default assertion status is returned.
   * <p>
   * Few programmers will have any need for this method; it is provided for the
   * benefit of the JRE itself. (It allows a class to determine at the time that
   * it is initialized whether assertions should be enabled.) Note that this
   * method is not guaranteed to return the actual assertion status that was (or
   * will be) associated with the specified class when it was (or will be)
   * initialized.
   *
   * @return the desired assertion status of the specified class.
   * @see java.lang.ClassLoader#setClassAssertionStatus
   * @see java.lang.ClassLoader#setPackageAssertionStatus
   * @see java.lang.ClassLoader#setDefaultAssertionStatus
   * @since 1.4
   */
  public boolean desiredAssertionStatus()
  {
    ClassLoader loader = getClassLoader();
    // If the loader is null this is a system class, so ask the VM
    if (loader == null)
      return type.getDesiredAssertionStatus();

    synchronized (loader)
    {
      // If the classloader has been initialized with
      // the assertion directives, ask it. Otherwise,
      // ask the VM.
      return (loader.classAssertionStatus == null ? type.getDesiredAssertionStatus()
      : loader.desiredAssertionStatus(getName()));
    }
  }

  /**
   * Returns true if and only if this class was declared as an enum in the source
   * code.
   *
   * @return true if and only if this class was declared as an enum in the source
   *         code
   * @since 1.5
   */
  public boolean isEnum()
  {
    // An enum must both directly extend java.lang.Enum and have
    // the ENUM bit set; classes for specialized enum constants
    // don't do the former.
    return (this.getModifiers() & ENUM) != 0 && this.getSuperclass() == java.lang.Enum.class;
  }

  @NoInline
  private void throwNoSuchFieldException(String name) throws NoSuchFieldException
  {
    throw new NoSuchFieldException(name);
  }

  @NoInline
  private void throwNoSuchMethodException(String name, Class<?>... parameterTypes) throws NoSuchMethodException
  {
    String typeString;
    if (parameterTypes == null || parameterTypes.length == 0)
    {
      typeString = "()";
    } else
    {
      typeString = "(";
      for (int i = 0; i < parameterTypes.length - 1; i++)
      {
        Class<?> c = parameterTypes[i];
        typeString += c.toString() + ", ";
      }
      typeString += parameterTypes[parameterTypes.length - 1].toString() + ")";
    }
    throw new NoSuchMethodException(name + typeString);
  }

  /**
   * Compare parameter lists for agreement.
   */
  private boolean parametersMatch(TypeReference[] lhs, Class<?>[] rhs)
  {
    if (rhs == null)
      return lhs.length == 0;
    if (lhs.length != rhs.length)
      return false;

    for (int i = 0, n = lhs.length; i < n; ++i)
    {
      if (rhs[i] == null)
        return false;
      if (lhs[i].resolve() != rhs[i].type)
      {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns the elements of this enum class or null if this Class object does not
   * represent an enum type.
   *
   * @return an array containing the values comprising the enum class represented
   *         by this Class object in the order they're declared, or null if this
   *         Class object does not represent an enum type
   * @since 1.5
   */
  public T[] getEnumConstants()
  {
    T[] values = getEnumConstantsShared();
    return (values != null) ? values.clone() : null;
  }

  /**
   * Returns the elements of this enum class or null if this Class object does not
   * represent an enum type; identical to getEnumConstantsShared except that the
   * result is uncloned, cached, and shared by all callers.
   */
  T[] getEnumConstantsShared()
  {
    if (enumConstants == null)
    {
      if (!isEnum())
        return null;
      try
      {
        final Method values = getMethod("values");
        java.security.AccessController.doPrivileged(new java.security.PrivilegedAction<Void>() {
          public Void run()
          {
            values.setAccessible(true);
            return null;
          }
        });
        enumConstants = (T[]) values.invoke(null);
      }
      // These can happen when users concoct enum-like classes
      // that don't comply with the enum spec.
      catch (InvocationTargetException ex)
      {
        return null;
      } catch (NoSuchMethodException ex)
      {
        return null;
      } catch (IllegalAccessException ex)
      {
        return null;
      }
    }
    return enumConstants;
  }

  private volatile transient T[] enumConstants = null;

  /**
   * Returns a map from simple name to enum constant. This package-private method
   * is used internally by Enum to implement public static <T extends Enum<T>> T
   * valueOf(Class<T>, String) efficiently. Note that the map is returned by this
   * method is created lazily on first use. Typically it won't ever get created.
   */
  Map<String, T> enumConstantDirectory()
  {
    if (enumConstantDirectory == null)
    {
      T[] universe = getEnumConstantsShared();
      if (universe == null)
        throw new IllegalArgumentException(getName() + " is not an enum type");
      Map<String, T> m = new HashMap<String, T>(2 * universe.length);
      for (T constant : universe)
        m.put(((Enum<?>) constant).name(), constant);
      enumConstantDirectory = m;
    }
    return enumConstantDirectory;
  }

  private volatile transient Map<String, T> enumConstantDirectory = null;

  /**
   * Casts an object to the class or interface represented by this {@code Class}
   * object.
   *
   * @param obj
   *          the object to be cast
   * @return the object after casting, or null if obj is null
   *
   * @throws ClassCastException
   *           if the object is not null and is not assignable to the type T.
   *
   * @since 1.5
   */
  public T cast(Object obj)
  {
    if (obj != null && !isInstance(obj))
      throw new ClassCastException(cannotCastMsg(obj));
    return (T) obj;
  }

  private String cannotCastMsg(Object obj)
  {
    return "Cannot cast " + obj.getClass().getName() + " to " + getName();
  }

  /**
   * Casts this {@code Class} object to represent a subclass of the class
   * represented by the specified class object. Checks that that the cast is
   * valid, and throws a {@code ClassCastException} if it is not. If this method
   * succeeds, it always returns a reference to this class object.
   *
   * <p>
   * This method is useful when a client needs to "narrow" the type of a
   * {@code Class} object to pass it to an API that restricts the {@code Class}
   * objects that it is willing to accept. A cast would generate a compile-time
   * warning, as the correctness of the cast could not be checked at runtime
   * (because generic types are implemented by erasure).
   *
   * @return this {@code Class} object, cast to represent a subclass of the
   *         specified class object.
   * @throws ClassCastException
   *           if this {@code Class} object does not represent a subclass of the
   *           specified class (here "subclass" includes the class itself).
   * @since 1.5
   */
  public <U> Class<? extends U> asSubclass(Class<U> clazz)
  {
    if (clazz.isAssignableFrom(this))
      return (Class<? extends U>) this;
    else
      throw new ClassCastException(this.toString());
  }

  /**
   * @throws NullPointerException
   *           {@inheritDoc}
   * @since 1.5
   */
  public <A extends Annotation> A getAnnotation(Class<A> annotationClass)
  {
    if (annotationClass == null)
      throw new NullPointerException();

    RVMType type = java.lang.JikesRVMSupport.getTypeForClass((Class<?>) (Object) this);
    return type.getAnnotation(annotationClass);
  }

  /**
   * @throws NullPointerException
   *           {@inheritDoc}
   * @since 1.5
   */
  public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass)
  {
    if (annotationClass == null)
      throw new NullPointerException();

    RVMType type = java.lang.JikesRVMSupport.getTypeForClass((Class<?>) (Object) this);
    return type.isAnnotationPresent(annotationClass);
  }

  /**
   * @since 1.5
   */
  public Annotation[] getAnnotations()
  {
    RVMType type = java.lang.JikesRVMSupport.getTypeForClass((Class<?>) (Object) this);
    return type.getAnnotations();
  }

  /**
   * @since 1.5
   */
  public Annotation[] getDeclaredAnnotations()
  {
    RVMType type = java.lang.JikesRVMSupport.getTypeForClass((Class<?>) (Object) this);
    return type.getDeclaredAnnotations();
  }

}
