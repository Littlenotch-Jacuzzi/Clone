package clone;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;

import sun.misc.Unsafe;

public class PerfectClone 
{
	private static IArrayList clonedObjects = new IArrayList();
	private static IArrayList clonedRetObjects = new IArrayList();
	private static Unsafe unsafe = hackUnsafe();
	
	public static <T> T clone(T target) {
		clonedObjects.clear();
		clonedRetObjects.clear();
		return clone0(target, null);
	}
	
	/**
	 * Add {@code ret} parameter to reduce workload.
	 */
	@SuppressWarnings("unchecked")
	private static <T> T clone0(T target, Object ret) {
		if(target == null)
			return null;
		
		if(canDirectlyClone(target.getClass()))
			return target;
		
		if(target.getClass() == Object.class)
			return (T) new Object();
		
		if(isPackagingClass(target.getClass()))
			return clonePackagingClassObject(target);
		
		int index = clonedObjects.indexOf(target);
		if(index != -1) return (T) clonedRetObjects.get(index);
		
		if(target.getClass().isArray()) {
			if(ret != null) {
				if(ret.getClass() != target.getClass())
					ret = (T) Array.newInstance(target.getClass().getComponentType(), Array.getLength(target));
			}else {
				ret = (T) Array.newInstance(target.getClass().getComponentType(), Array.getLength(target));
			}
			
			if(Array.getLength(target) == 0) return (T) ret;
			
			clonedObjects.add(target);
			clonedRetObjects.add(ret);
			
			for(int i = 0; i < Array.getLength(target); i++) {
				Array.set(ret, i, clone0(Array.get(target, i), Array.get(ret, i)));
			}
			
			return (T) ret;
		}
		
		if(ret != null) {
			if(ret.getClass() != target.getClass()) {
				ret = (T) instantiateObject(target.getClass());
			}
		}else {
			ret = (T) instantiateObject(target.getClass());
		}
		
		clonedObjects.add(target);
		clonedRetObjects.add(ret);
		
		ArrayList<Field> fields = new ArrayList<Field>();
		Class<?> iterator = target.getClass();
		hackPackage(iterator);
		do {
			hackPackage(iterator);
			fields.addAll(Arrays.asList(iterator.getDeclaredFields()));
		}while((iterator = iterator.getSuperclass()) != Object.class);

		for(Field field : fields) {
			try {
				if(!(Modifier.isStatic(field.getModifiers()))) {
					field.setAccessible(true);
					field.set(ret, clone0(field.get(target), field.get(ret)));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return (T) ret;
	}
	
	/**
	 * Use magical {@code Unsafe} to instantiate an object.
	 */
	@SuppressWarnings("unchecked")
	private static <T> T instantiateObject(Class<T> type) {
		try {
			return (T) unsafe.allocateInstance(type);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private static <T> T clonePackagingClassObject(T target) {
		if(target.getClass() == String.class) {
			return (T) String.valueOf(((String) target).toCharArray());
		}else if(target.getClass() == Character.class) {
			return (T) Character.valueOf(((Character) target).charValue());
		}else if(target.getClass() == Boolean.class) {
			return (T) Boolean.valueOf(((Boolean) target).booleanValue());
		}else if(target.getClass() == Byte.class) {
			return (T) Byte.valueOf(((Byte) target).byteValue());
		}else if(target.getClass() == Short.class) {
			return (T) Short.valueOf(((Short) target).shortValue());
		}else if(target.getClass() == Integer.class) {
			return (T) Integer.valueOf(((Integer) target).intValue());
		}else if(target.getClass() == Long.class) {
			return (T) Long.valueOf(((Long) target).longValue());
		}else if(target.getClass() == Float.class) {
			return (T) Float.valueOf(((Float) target).floatValue());
		}else if(target.getClass() == Double.class) {
			return (T) Double.valueOf(((Double) target).doubleValue());
		}
		
		return null;
	}
	
	private static Unsafe hackUnsafe() {
		try {
			Field field = Unsafe.class.getDeclaredField("theUnsafe");
			field.setAccessible(true);
			return (Unsafe) field.get(null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private static void hackPackage(Class<?> targetClass) {
		try {
			Method addOpens = Module.class.getDeclaredMethod("implAddExportsOrOpens", 
					String.class, Module.class, boolean.class, boolean.class);
			addOpens.setAccessible(true);
			addOpens.invoke(targetClass.getModule(), targetClass.getPackageName(), PerfectClone.class.getModule(), true, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static boolean canDirectlyClone(Class<?> targetClass) {
		return (targetClass.isPrimitive() || (targetClass == Class.class) || 
				(targetClass == Module.class) || (targetClass.isEnum()));
	}
	
	private static boolean isPackagingClass(Class<?> targetClass) {
		return ((targetClass == String.class) || (targetClass == Character.class) ||
				(targetClass == Boolean.class) || (targetClass == Byte.class) ||
				(targetClass == Short.class) || (targetClass == Integer.class) || 
				(targetClass == Long.class) || (targetClass == Float.class) || 
				(targetClass == Double.class));
	}
	
	/**
	 * I overrode the {@code indexOf(Object)} method because this method in ArrayList is not proper(the {@code equals()} method should not be used here).
	 */
	private static class IArrayList extends ArrayList<Object>
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 962042845135067366L;

		@Override
		public int indexOf(Object o) {
			for(int i = 0 ; i < size(); i++) {
				if(o == get(i)) {
					return i;
				}
			}
			return -1;
		}
	}
}