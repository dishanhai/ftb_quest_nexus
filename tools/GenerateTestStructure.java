// Author: dishanhai. Generates the small, owned GameTest fixture without external tools.
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

public final class GenerateTestStructure {
    public static void main(String[] args) throws Exception {
        Path target = Path.of(args[0]);
        Files.createDirectories(target.getParent());
        try (var out = new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(target)))) {
            out.writeByte(10); out.writeUTF("");
            out.writeByte(3); out.writeUTF("DataVersion"); out.writeInt(3465);
            out.writeByte(9); out.writeUTF("size"); out.writeByte(3); out.writeInt(3);
            out.writeInt(5); out.writeInt(5); out.writeInt(5);
            out.writeByte(9); out.writeUTF("palette"); out.writeByte(10); out.writeInt(1);
            out.writeByte(8); out.writeUTF("Name"); out.writeUTF("minecraft:air"); out.writeByte(0);
            for (String name : new String[]{"blocks", "entities"}) {
                out.writeByte(9); out.writeUTF(name); out.writeByte(10); out.writeInt(0);
            }
            out.writeByte(0);
        }
        System.out.println("Generated " + target + " (" + Files.size(target) + " bytes)");
    }
}
