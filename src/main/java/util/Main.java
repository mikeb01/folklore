package util;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import performance.PerformanceMain;

public class Main
{
    private final static Map<String, Class<?>> MAIN_CLASSES = new TreeMap<String, Class<?>>(String.CASE_INSENSITIVE_ORDER);
    
    public static void main(String[] args)
    {
        MAIN_CLASSES.put("strings", PerformanceMain.class);
        MAIN_CLASSES.put("async.server.edge", distributed.async.EdgeService.class);
        MAIN_CLASSES.put("async.server.data", distributed.DataService.class);
        MAIN_CLASSES.put("async.client", distributed.async.Client.class);
        MAIN_CLASSES.put("sync.server.edge", distributed.sync.EdgeService.class);
        MAIN_CLASSES.put("sync.client", distributed.sync.Client.class);
        
        if (0 == args.length || !MAIN_CLASSES.containsKey(args[0].trim()))
        {
            throw new RuntimeException("Invalid service, must be one of: " + MAIN_CLASSES);
        }
        
        String name = args[0];
        Object remainingArgs = Arrays.copyOfRange(args, 1, args.length);
        
        Class<?> mainClass = MAIN_CLASSES.get(name);
        
        if (null == mainClass)
        {
            throw new RuntimeException("Unable to find class for: " + name);
        }
        
        try
        {
            Method method = mainClass.getDeclaredMethod("main", String[].class);
            method.invoke(null, remainingArgs);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
