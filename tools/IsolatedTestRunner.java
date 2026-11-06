// Author: dishanhai. Offline fallback; Gradle/JUnit remains the normal test entry point.
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.Test;

public final class IsolatedTestRunner {
    public static void main(String[] names) throws Exception {
        int passed = 0;
        for (String name : names) {
            Class<?> type = Class.forName(name);
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            for (var method : type.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(Test.class)) continue;
                method.setAccessible(true);
                try {
                    method.invoke(constructor.newInstance());
                    System.out.println("PASS " + type.getSimpleName() + "." + method.getName());
                    passed++;
                } catch (InvocationTargetException exception) {
                    System.err.println("FAIL " + type.getSimpleName() + "." + method.getName());
                    exception.getCause().printStackTrace();
                    System.exit(1);
                }
            }
        }
        System.out.println("Tests passed: " + passed);
    }
}
