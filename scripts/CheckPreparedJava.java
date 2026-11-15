import com.sun.source.util.JavacTask;
import java.util.Arrays;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Parse the actual patched UI without requiring its complete Android classpath. */
class CheckPreparedJava {
    public static void main(String[] paths) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("A JDK is required");
        DiagnosticCollector<JavaFileObject> messages = new DiagnosticCollector<>();
        try (StandardJavaFileManager files = compiler.getStandardFileManager(messages, null, null)) {
            JavacTask task = (JavacTask) compiler.getTask(null, files, messages, List.of("-proc:none", "-source", "17"), null,
                    files.getJavaFileObjectsFromStrings(Arrays.asList(paths)));
            task.parse();
            boolean failed = false;
            for (Diagnostic<?> message : messages.getDiagnostics()) if (message.getKind() == Diagnostic.Kind.ERROR) {
                System.err.println(message); failed = true;
            }
            if (failed) System.exit(1);
        }
        System.out.println("Prepared Java syntax checked: " + paths.length + " files");
    }
}
