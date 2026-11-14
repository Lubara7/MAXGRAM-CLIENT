import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.MultiDexContainer;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.immutable.ImmutableDexFile;

public class ExtractCodecDex {
    public static void main(String[] args) throws Exception {
        MultiDexContainer<? extends DexBackedDexFile> container = DexFileFactory.loadDexContainer(new File(args[0]), Opcodes.forApi(27));
        List<ClassDef> classes = new ArrayList<>();
        boolean codecFound = false;
        for (String entry : container.getDexEntryNames()) {
            for (ClassDef definition : container.getEntry(entry).getDexFile().getClasses()) {
                String name = definition.getType();
                if (name.startsWith("Lio/github/lubara7/tgmax/max/") || name.startsWith("Lorg/msgpack/") ||
                        name.startsWith("Lnet/jpountz/") || name.startsWith("Lio/airlift/compress/")) {
                    classes.add(definition);
                    if (name.equals("Lio/github/lubara7/tgmax/max/MaxFrameCodec;")) codecFound = true;
                }
            }
        }
        if (!codecFound) throw new IllegalStateException("Released APK does not contain the MAX frame decoder");
        DexFileFactory.writeDexFile(args[1], new ImmutableDexFile(Opcodes.forApi(27), classes));
        System.out.println("Extracted " + classes.size() + " codec classes from the released APK");
    }
}
