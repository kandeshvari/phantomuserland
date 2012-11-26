//  ClassRef.java -- a reference to a Class in a Constant table

package ru.dz.jpc.classfile;
import java.util.*;

public class ClassRef {
    /* Although we use the class name as the major part of a class reference,
     * in fact there can be multiple, distinct classes with the same name,
     * if they have been loaded with different class loaders.  We store the
     * class as generated by the system class loader in the refClass field;
     * other classes are stored in a hash table indexed by their loaders. */

    public String name;

    // the referred-to class as loaded by the system class loader
    private Class<?> refClass = null;
    // The referred-to class as loaded by other class loaders
    private Hashtable<ClassLoader, Class<?>> loadedRefdClasses = null;
    private static Hashtable<Object,ClassRef> rccache;

    static {
        rccache = new Hashtable<Object,ClassRef> ();
        rccache.put (Void.TYPE, new ClassRef (Void.TYPE));
        rccache.put (Boolean.TYPE, new ClassRef (Boolean.TYPE));
        rccache.put (Byte.TYPE, new ClassRef (Byte.TYPE));
        rccache.put (Character.TYPE, new ClassRef (Character.TYPE));
        rccache.put (Short.TYPE, new ClassRef (Short.TYPE));
        rccache.put (Integer.TYPE, new ClassRef (Integer.TYPE));
        rccache.put (Float.TYPE, new ClassRef (Float.TYPE));
        rccache.put (Double.TYPE, new ClassRef (Double.TYPE));
    }

    /* Use this to register class ref structures created elsewhere, e.g. in
     * tobafied code.  This is called from runtime.c/register_crefs, which is
     * to be called around the same time as walk_classes.
     * Toba hash code: _C_2Ejuw
     */
    public static void
    RegisterClassRef (ClassRef cr)
    {
        rccache.put (cr.name, cr);
        return;
    }
                      
    /* For this to work, ClassRefs in tobafied source files need to be added
     * to the cache, else we'll duplicate (for example) interface class
     * structures when we look them up by name at runtime. */
    public static ClassRef
    byName (String s)
    {
        ClassRef cr;

        s = s.intern ();
        cr = rccache.get (s);
        if (null == cr) {
            cr = new ClassRef (s);
            rccache.put (s, cr);
        }
        return cr;
    }

    /* For this to work, ClassRefs in tobafied source files need to be added
     * to the cache, else we'll duplicate (for example) interface class
     * structures when we look them up by name at runtime. */
    public static ClassRef
    bySignature (String s)
    {
        ClassRef cr;
        
        cr = null;
        switch (s.charAt (0)) {
            case 'Z':
                cr = rccache.get (Boolean.TYPE);
                break;
            case 'B':
                cr = rccache.get (Byte.TYPE);
                break;
            case 'C':
                cr = rccache.get (Character.TYPE);
                break;
            case 'S':
                cr = rccache.get (Short.TYPE);
                break;
            case 'I':
                cr = rccache.get (Integer.TYPE);
                break;
            case 'J':
                cr = rccache.get (Long.TYPE);
                break;
            case 'F':
                cr = rccache.get (Float.TYPE);
                break;
            case 'D':
                cr = rccache.get (Double.TYPE);
                break;
            case 'L':
                cr = byName (s.substring (1, s.length() - 1));
                break;
        }
        return cr;
    }


    // The constructor called when parsing class files
    private ClassRef(String s) {
	name = s;
	refClass = null;
    }
    
    // The constructor called for installing primitive class refs
    private ClassRef(Class<?> cl) {
	name = "<" + cl.getName () + ">";
	refClass = cl;
    }

    /* Return the appropriate Class structure for this reference, given
     * the loader we're to use. */
    private Class<?>
    findRefdClass (ClassLoader loader)
    {
        if (null == loader) {
            return refClass;
        }
        if (null != loadedRefdClasses) {
            return loadedRefdClasses.get (loader);
        }
        return null;
    }
    
    public boolean isResolved (ClassLoader loader) {
	return (null != findRefdClass (loader));
    }

    public void resolveTo(Class<?> cl,
                          ClassLoader cld)
    {
        if (null == cld) {
            refClass = cl;
        } else {
            if (null == loadedRefdClasses) {
                loadedRefdClasses = new Hashtable<ClassLoader, Class<?>> ();
            }
            loadedRefdClasses.put (cld, cl);
        }
    }

    public Class<?> getRefClass(ClassLoader loader) {
        Class<?> rc;

        rc = findRefdClass (loader);
	if (null == rc) {
	    throw new NoClassDefFoundError("Reference to class " + name + " is unresolved.");
	}

	return rc;
    }

    public String toString() {
	return name;
    }
};
