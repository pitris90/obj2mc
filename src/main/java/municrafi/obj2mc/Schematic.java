package municrafi.obj2mc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import java.nio.*;
import java.util.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.validation.Schema;

import org.jnbt.ByteArrayTag;
import org.jnbt.CompoundTag;
import org.jnbt.NBTOutputStream;
import org.jnbt.ShortTag;
import org.jnbt.StringTag;
import org.jnbt.Tag;
import de.javagl.obj.*;

/**
 * Hello world!
 *
 */
public class Schematic 
{
    private byte[] blocks;
    private byte[] data;
    private short width;
    private short length;
    private short height;

    public Schematic(short width, short length, short height)
    {
        this.blocks = new byte[width * length * height];
        this.data = new byte[width * length * height];
        this.width = width;
        this.length = length;
        this.height = height;
    }
    public static void main( String[] args ) throws FileNotFoundException
    {
        System.out.println( "Hello World!" );
        Obj block = null;
        // TODO: Path jako argument 
        try (InputStream blockFile = new FileInputStream("C:/Users/petrb/Documents/obj2mc/block.obj"))
        {
            block = ObjUtils.convertToRenderable(ObjReader.read(blockFile));
        } catch (Exception e) {
            e.printStackTrace();
        }
        IntBuffer indices = null;
        FloatBuffer vertices = null;
        if (block != null)
        {
            System.out.println(block.getNumFaces());
            System.out.println(block.getNumVertices());
            indices = ObjData.getFaceVertexIndices(block);
            vertices = ObjData.getVertices(block);
        }
        int a = indices.get(0);
        int b = indices.get(1);
        int c = indices.get(2);
        float x = vertices.get(a);
        float y = vertices.get(b);
        float z = vertices.get(c);
        int stop = 0;
        short width = 80;
        short length = 90;
        short height = 100;
        Schematic s = new Schematic(width, length, height);
        File test = new File("C:/Users/petrb/Documents/obj2mc/test.schematic");
        s.save(test);
    }

    public void save(File file) {
        try (NBTOutputStream nbtStream = new NBTOutputStream(new FileOutputStream(file))) {
            Map<String, Tag> schematic = new HashMap<String, Tag>();

            schematic.put("Width", new ShortTag("Width", width));
            schematic.put("Height", new ShortTag("Height", height));
            schematic.put("Length", new ShortTag("Length", length));
            schematic.put("Materials", new StringTag("Materials", "Alpha"));
            schematic.put("Blocks", new ByteArrayTag("Blocks", blocks));
            schematic.put("Data", new ByteArrayTag("Data", data));

            nbtStream.writeTag(new CompoundTag("Schematic", schematic));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
