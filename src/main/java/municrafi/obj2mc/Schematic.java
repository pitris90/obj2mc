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
 * Based on
 * https://fileadmin.cs.lth.se/cs/Personal/Tomas_Akenine-Moller/code/tribox2.txt
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

    public static void main(String[] args) throws FileNotFoundException 
    {
        System.out.println("Hello World!");
        Obj block = null;
        // TODO: Path jako argument
        try (InputStream blockFile = new FileInputStream("C:/Users/petrb/Documents/obj2mc/block.obj")) 
        {
            block = ObjUtils.convertToRenderable(ObjReader.read(blockFile));
        } 
        catch (Exception e) 
        {
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

    private static float[] cross(float[] v1, float[] v2) 
    {
        float[] dest = new float[3];
        dest[0] = v1[1] * v2[2] - v1[2] * v2[1];
        dest[1] = v1[2] * v2[0] - v1[0] * v2[2];
        dest[2] = v1[0] * v2[1] - v1[1] * v2[0];
        return dest;
    }

    private static float dot(float[] v1, float[] v2) {
        return (float) v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2];
    }

    private static float[] sub(float[] v1, float[] v2) 
    {
        float[] dest = new float[3];
        dest[0] = v1[0] - v2[0];
        dest[1] = v1[1] - v2[1];
        dest[2] = v1[2] - v2[2];
        return dest;
    }

    private static float[] findminmax(float x0, float x1, float x2) 
    {
        float min = x0;
        float max = x0;
        if (x1 < min)
            min = x1;
        if (x1 > max)
            max = x1;
        if (x2 < min)
            min = x2;
        if (x2 > max)
            max = x2;
        float[] minmax = new float[2];
        minmax[0] = min;
        minmax[1] = max;
        return minmax;
    }

    private static boolean planeBoxOverlap(float normal[], float d, float maxbox[]) 
    {
        int q = 0;
        float vmin[] = new float[3];
        float vmax[] = new float[3];
        for (q = 0; q <= 2; q++) {
            if (normal[q] > 0.0f) 
            {
                vmin[q] = -maxbox[q];
                vmax[q] = maxbox[q];
            } 
            else 
            {
                vmin[q] = maxbox[q];
                vmax[q] = -maxbox[q];
            }
        }
        if (dot(normal, vmin) + d > 0.0f)
            return false;
        if (dot(normal, vmax) + d >= 0.0f)
            return true;

        return false;
    }

    private static boolean triBoxOverlap(float[] boxcenter, float[] boxhalfsize, float[][] triverts) {

        /*
         * Copyright 2020 Tomas Akenine-Möller
         * 
         * Permission is hereby granted, free of charge, to any person obtaining a copy
         * of this software and associated
         * documentation files (the "Software"), to deal in the Software without
         * restriction, including without limitation
         * the rights to use, copy, modify, merge, publish, distribute, sublicense,
         * and/or sell copies of the Software, and
         * to permit persons to whom the Software is furnished to do so, subject to the
         * following conditions:
         * 
         * The above copyright notice and this permission notice shall be included in
         * all copies or substantial
         * portions of the Software.
         * 
         * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
         * IMPLIED, INCLUDING BUT NOT LIMITED TO THE
         * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
         * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
         * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
         * WHETHER IN AN ACTION OF CONTRACT, TORT
         * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
         * USE OR OTHER DEALINGS IN THE SOFTWARE.
         * 
         */
        /* use separating axis theorem to test overlap between triangle and box */
        /* need to test for overlap in these directions: */
        /* 1) the {x,y,z}-directions (actually, since we use the AABB of the triangle */
        /* we do not even need to test these) */
        /* 2) normal of the triangle */
        /* 3) crossproduct(edge from tri, {x,y,z}-directin) */
        /* this gives 3x3=9 more tests */
        float[] v0 = new float[3];
        float[] v1 = new float[3];
        float[] v2 = new float[3];
        float min, max, d, p0, p1, p2, rad, fex, fey, fez;
        float[] normal = new float[3];
        float[] e0 = new float[3];
        float[] e1 = new float[3];
        float[] e2 = new float[3];

        /* This is the fastest branch on Sun */
        /* move everything so that the boxcenter is in (0,0,0) */
        v0 = sub(triverts[0], boxcenter);
        v1 = sub(triverts[1], boxcenter);
        v2 = sub(triverts[2], boxcenter);

        /* compute triangle edges */
        e0 = sub(v1, v0); /* tri edge 0 */
        e1 = sub(v2, v1); /* tri edge 1 */
        e2 = sub(v0, v2); /* tri edge 2 */

        /* Bullet 3: */
        /* test the 9 tests first (this was faster) */
        fex = Math.abs(e0[0]);
        fey = Math.abs(e0[1]);
        fez = Math.abs(e0[2]);

        p0 = e0[2] * v0[1] - e0[1] * v0[2];
        p2 = e0[2] * v2[1] - e0[1] * v2[2];
        if (p0 < p2) {
            min = p0;
            max = p2;
        } else {
            min = p2;
            max = p0;
        }
        rad = fez * boxhalfsize[1] + fey * boxhalfsize[2];
        if (min > rad || max < -rad)
            return false;

        p0 = -e0[2] * v0[0] + e0[0] * v0[2];
        p2 = -e0[2] * v2[0] + e0[0] * v2[2];
        if (p0 < p2) {
            min = p0;
            max = p2;
        } else {
            min = p2;
            max = p0;
        }
        rad = fez * boxhalfsize[0] + fex * boxhalfsize[2];
        if (min > rad || max < -rad)
            return false;

        p1 = e0[1] * v1[0] - e0[0] * v1[1];
        p2 = e0[1] * v2[0] - e0[0] * v2[1];
        if (p2 < p1) {
            min = p2;
            max = p1;
        } else {
            min = p1;
            max = p2;
        }
        rad = fey * boxhalfsize[0] + fex * boxhalfsize[1];
        if (min > rad || max < -rad)
            return false;

        fex = Math.abs(e1[0]);
        fey = Math.abs(e1[1]);
        fez = Math.abs(e1[2]);

        p0 = e1[2] * v0[1] - e1[1] * v0[2];
        p2 = e1[2] * v2[1] - e1[1] * v2[2];
        if (p0 < p2) {
            min = p0;
            max = p2;
        } else {
            min = p2;
            max = p0;
        }
        rad = fez * boxhalfsize[1] + fey * boxhalfsize[2];
        if (min > rad || max < -rad)
            return false;

        p0 = -e1[2] * v0[0] + e1[0] * v0[2];
        p2 = -e1[2] * v2[0] + e1[0] * v2[2];
        if (p0 < p2) {
            min = p0;
            max = p2;
        } else {
            min = p2;
            max = p0;
        }
        rad = fez * boxhalfsize[0] + fex * boxhalfsize[2];
        if (min > rad || max < -rad)
            return false;

        p0 = e1[1] * v0[0] - e1[0] * v0[1];
        p1 = e1[1] * v1[0] - e1[0] * v1[1];
        if (p0 < p1) {
            min = p0;
            max = p1;
        } else {
            min = p1;
            max = p0;
        }
        rad = fey * boxhalfsize[0] + fex * boxhalfsize[1];
        if (min > rad || max < -rad)
            return false;

        fex = Math.abs(e2[0]);
        fey = Math.abs(e2[1]);
        fez = Math.abs(e2[2]);

        p0 = e2[2] * v0[1] - e2[1] * v0[2];
        p1 = e2[2] * v1[1] - e2[1] * v1[2];
        if (p0 < p1) {
            min = p0;
            max = p1;
        } else {
            min = p1;
            max = p0;
        }
        rad = fez * boxhalfsize[1] + fey * boxhalfsize[2];
        if (min > rad || max < -rad)
            return false;

        p0 = -e2[2] * v0[0] + e2[0] * v0[2];
        p1 = -e2[2] * v1[0] + e2[0] * v1[2];
        if (p0 < p1) {
            min = p0;
            max = p1;
        } else {
            min = p1;
            max = p0;
        }
        rad = fez * boxhalfsize[0] + fex * boxhalfsize[2];
        if (min > rad || max < -rad)
            return false;

        p1 = e2[1] * v1[0] - e2[0] * v1[1];
        p2 = e2[1] * v2[0] - e2[0] * v2[1];
        if (p2 < p1) {
            min = p2;
            max = p1;
        } else {
            min = p1;
            max = p2;
        }
        rad = fey * boxhalfsize[0] + fex * boxhalfsize[1];
        if (min > rad || max < -rad)
            return false;

        /* Bullet 1: */
        /* first test overlap in the {x,y,z}-directions */
        /* find min, max of the triangle each direction, and test for overlap in */
        /* that direction -- this is equivalent to testing a minimal AABB around */
        /* the triangle against the AABB */

        /* test in X-direction */
        float[] minmax = new float[2];
        minmax = findminmax(v0[0], v1[0], v2[0]);
        min = minmax[0];
        max = minmax[1];
        if (min > boxhalfsize[0] || max < -boxhalfsize[0])
            return false;

        /* test in Y-direction */
        minmax = findminmax(v0[1], v1[1], v2[1]);
        min = minmax[0];
        max = minmax[1];
        if (min > boxhalfsize[1] || max < -boxhalfsize[1])
            return false;

        /* test in Z-direction */
        minmax = findminmax(v0[2], v1[2], v2[2]);
        min = minmax[0];
        max = minmax[1];
        if (min > boxhalfsize[2] || max < -boxhalfsize[2])
            return false;

        /* Bullet 2: */
        /* test if the box intersects the plane of the triangle */
        /* compute plane equation of triangle: normal*x+d=0 */
        normal = cross(e0, e1);
        d = -dot(normal, v0); /* plane eq: normal.x+d=0 */
        if (!planeBoxOverlap(normal, d, boxhalfsize))
            return false;

        return true; /* box and triangle overlaps */
    }

    public void save(File file) 
    {
        try (NBTOutputStream nbtStream = new NBTOutputStream(new FileOutputStream(file))) 
        {
            Map<String, Tag> schematic = new HashMap<String, Tag>();

            schematic.put("Width", new ShortTag("Width", width));
            schematic.put("Height", new ShortTag("Height", height));
            schematic.put("Length", new ShortTag("Length", length));
            schematic.put("Materials", new StringTag("Materials", "Alpha"));
            schematic.put("Blocks", new ByteArrayTag("Blocks", blocks));
            schematic.put("Data", new ByteArrayTag("Data", data));

            nbtStream.writeTag(new CompoundTag("Schematic", schematic));
        } 
        catch (IOException ex) 
        {
            ex.printStackTrace();
        }
    }
}
