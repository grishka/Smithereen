package smithereen.util.validation;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

public class ObjectValidator{
	public static <T> void validate(T obj){
		validate(obj, "");
	}

	private static <T> void validate(T obj, String path){
		Class<?> objClass=obj.getClass();
		boolean allRequired=objClass.isAnnotationPresent(AllFieldsAreRequired.class);
		try{
			for(Field f:objClass.getDeclaredFields()){
				if(!f.getType().isPrimitive()){
					f.setAccessible(true);
					if(allRequired || f.isAnnotationPresent(RequiredField.class)){
						if(f.get(obj)==null)
							throw new ObjectValidationException("Required field "+path+f.getName()+" is not present");
					}
					Class<?> fieldType=f.getType();
					if(fieldType.getPackageName().startsWith("smithereen.")){
						Object value=f.get(obj);
						if(value!=null)
							validate(value, path+f.getName()+".");
					}else if(Collection.class.isAssignableFrom(fieldType)){
						Collection<?> collection=(Collection<?>) f.get(obj);
						if(collection!=null){
							if(collection.isEmpty() && f.isAnnotationPresent(NonEmpty.class)){
								throw new ObjectValidationException("Field "+path+f.getName()+" is an empty array");
							}
							boolean allowNull=f.isAnnotationPresent(AllowNullElements.class);
							int i=0;
							for(Object el:collection){
								if(el==null && !allowNull)
									throw new ObjectValidationException("Field "+path+f.getName()+" contains a null at index "+i);
								else if(el!=null && el.getClass().getPackageName().startsWith("smithereen."))
									validate(el, path+f.getName()+"["+i+"].");
								i++;
							}
						}
					}else if(Map.class.isAssignableFrom(fieldType)){
						Map<?, ?> map=(Map<?, ?>) f.get(obj);
						if(map!=null){
							if(map.isEmpty() && f.isAnnotationPresent(NonEmpty.class)){
								throw new ObjectValidationException("Field "+path+f.getName()+" is an empty object");
							}
							boolean allowNull=f.isAnnotationPresent(AllowNullElements.class);
							for(Map.Entry<?, ?> entry:map.entrySet()){
								Object value=entry.getValue();
								if(value==null && !allowNull)
									throw new ObjectValidationException("Field "+path+f.getName()+" contains a null for key '"+entry.getKey()+"'");
								else if(value!=null && value.getClass().getPackageName().startsWith("smithereen."))
									validate(value, path+f.getName()+"['"+entry.getKey()+"'].");
							}
						}
					}
				}
			}
		}catch(IllegalAccessException x){
			throw new RuntimeException(x);
		}
	}
}
