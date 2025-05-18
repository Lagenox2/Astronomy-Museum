package com.company;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import javax.swing.*;
import java.awt.*;
import java.nio.*;
import java.util.Random;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.stb.STBImage.stbi_load;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

/*
w - земля
b - камень
tr - блок древесины
trd - блок досок
trl - листва
o - железная руда
p - золотая руда
s - алмазная руда
*/

class SCamera
{
    float x, y, z;
    float Xrot, Zrot;

    SCamera(float x, float y, float z, float Xrot, float Zrot) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.Xrot = Xrot;
        this.Zrot = Zrot;
    }
}

class TCell
{
    float x,y,z;
}

class TColor
{
    float r,g,b;
}

class TUV
{
    float u, v;
}

class TObject
{
    float x,y,z;
    int type;
    float scale;
}

class TSelectObj
{
    int plantMas_Index;
    int colorIndex;
}

public class Main extends JFrame {

    int windowX = 1920;
    int windowY = 1080;
    int mapW = 425;
    int mapH = 425;
    int lengthOfMap = mapW * mapH;
    int ObjListCnt = 255;

    TCell[][] map = new TCell[mapW][mapH];
    TCell[][] mapNormal = new TCell[mapW][mapH];
    TColor[][] mapCol = new TColor[mapW][mapH];
    int[][][] mapInd = new int[mapW-1][mapH-1][6];
    TSelectObj selectMas[] = new TSelectObj[ObjListCnt];
    int selectMasCnt = 0;
    //int mapIndCnt = ;

    int plantMasCnt = 9600;
    TObject plantMas[] = new TObject[plantMasCnt];

    FloatBuffer mapBuffer = ByteBuffer.allocateDirect(lengthOfMap * 4 * 3).order(ByteOrder.nativeOrder()).asFloatBuffer();
    FloatBuffer mapColBuffer = ByteBuffer.allocateDirect(lengthOfMap * 4 * 3).order(ByteOrder.nativeOrder()).asFloatBuffer();
    IntBuffer mapIndBuffer = ByteBuffer.allocateDirect((mapW - 1) * (mapH - 1) * 6 * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
    DoubleBuffer curXBuffer = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder()).asDoubleBuffer();
    DoubleBuffer curYBuffer = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder()).asDoubleBuffer();
    IntBuffer Xbuffer = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
    IntBuffer Ybuffer = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
    IntBuffer curXWindowbuffer = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
    IntBuffer curYWindowbuffer = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
    SCamera camera = new SCamera(mapW / 2, mapH / 2, 20, 0, 0);
    boolean pressedOrNot = true;
    int hillTestX = mapW / 2;
    int hillTestY = mapH / 2;
    int hillTestSize = 40;
    float hillHeight = 0.007f;
    float texCoord[] = {0,0,  1,0,  1,1,  0,1};
    TUV mapUV[][] = new TUV[mapW][mapH];
    FloatBuffer texCoordB = ByteBuffer.allocateDirect(lengthOfMap * 4 * 2).order(ByteOrder.nativeOrder()).asFloatBuffer();
    int tex_pole, tex_flower, tex_flower2, tex_grass, tex_mushroom, tex_tree, tex_tree2;
    int tex_more, tex_axolotl;
    float plant[] = {-0.5f,0,0, 0.5f,0,0, 0.5f,0,1, -0.5f,0,1,
                     0,-0.5f,0, 0,0.5f,0, 0,0.5f,1, 0,-0.5f,1};
    float plantUV[] = {0,1, 1,1, 1,0, 0,0, 0,1, 1,1, 1,0, 0,0};
    int plantInd[] = {0,1,2, 2,3,0, 4,5,6, 6,7,4};
    FloatBuffer plantBuffer = ByteBuffer.allocateDirect(plant.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    IntBuffer plantIndBuffer = ByteBuffer.allocateDirect(plantInd.length * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
    FloatBuffer plantUVBuffer = ByteBuffer.allocateDirect(plantUV.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    FloatBuffer mapNormalBuffer = ByteBuffer.allocateDirect(lengthOfMap * 4 * 3).order(ByteOrder.nativeOrder()).asFloatBuffer();
    boolean selectMode = false;
    float alfa = 0;
    float zKcc = 1.7f;

    boolean IsCoordInMap(float x, float y)
    {
        return (x >= 0) && (x < mapW) && (y >= 0) && (x < mapH);
    }

    void mpCreateHill(int posX, int posY, int size, float height)
    {/*
        for (int i = posX-rad; i <= posX+rad; i++)
            for (int j = posY-rad; j <= posY+rad; j++)
                if (IsCoordInMap(i, j))
                {
                    float len = (float) Math.sqrt(Math.pow(posX-i,2) + Math.pow(posY-j,2));
                    if (len < rad)
                    {
                        if (i < mapW && j < mapH)
                        {
                            len = (float) (len / rad * (Math.PI / 2));
                            map[i][j].z += Math.cos(len) * height;
                        }
                    }
                }
    */
        //map[mapW / 2][mapH / 2].z = map[mapW / 2][mapH / 2].z * 1.02f;
        int a = 0;
        for (int l = 0; l < 20; l++)
        {
            for (int i = posX - size / 2 - a; i < posX + size / 2 - a; i++)
                for (int j = posY - size / 2 - a; j < posY + size / 2 - a; j+=2)
                {
                    if (IsCoordInMap(i, j) && IsCoordInMap(i, j+1))
                    {
                        //if (height1 < hillTestSize / 2) height1+= hillHeight;
                        //else height1 -= hillHeight;
                        map[i][j].z += height;
                        map[i][j+1].z += height;
                    }
                }
            a++;
        }
    }

    void WndResize(int x, int y)
    {
        glViewport(0, 0, x, y);
        float k = x / (float) y;
        float sz = 0.1f;
        glLoadIdentity();
        glFrustum(-k*sz, k*sz, -sz, sz, sz*2, 180);
    }

    void fillMapAndMapColAndMapInd()
    {
        for (int i = 0; i < mapW; i++) {
            for (int j = 0; j < mapH; j++) {
                mapBuffer.put(map[i][j].x);
                mapBuffer.put(map[i][j].y);
                mapBuffer.put(map[i][j].z);

                mapColBuffer.put(mapCol[i][j].r);
                mapColBuffer.put(mapCol[i][j].g);
                mapColBuffer.put(mapCol[i][j].b);

                mapNormalBuffer.put(mapNormal[i][j].x);
                mapNormalBuffer.put(mapNormal[i][j].y);
                mapNormalBuffer.put(mapNormal[i][j].z);
            }
        }
        for (int i = 0; i < mapW - 1; i++) {
            for (int j = 0; j < mapH - 1; j++) {
                for (int k = 0; k < 6; k++) {
                    mapIndBuffer.put(mapInd[i][j][k]);
                }
            }
        }
        for (int i = 0; i < mapW; i++) {
            for (int j = 0; j < mapH; j++) {
                texCoordB.put(mapUV[i][j].u);
                texCoordB.put(mapUV[i][j].v);
            }
        }
        for (int i = 0; i < plant.length; i++) {
            plantBuffer.put(plant[i]);
        }
        for (int i = 0; i < plantUV.length; i++) {
            plantUVBuffer.put(plantUV[i]);
        }
        for (int i = 0; i < plantInd.length; i++) {
            plantIndBuffer.put(plantInd[i]);
        }
        for (int i = 0; i < ObjListCnt; i++) {
            selectMas[i] = new TSelectObj();
        }
        plantBuffer.flip();
        plantUVBuffer.flip();
        plantIndBuffer.flip();
        mapNormalBuffer.flip();
        texCoordB.flip();
        mapBuffer.flip();
        mapIndBuffer.flip();
        mapColBuffer.flip();
    }

    void cmApply()
    {
        glRotatef(-camera.Xrot, 1, 0, 0);
        glRotatef(-camera.Zrot, 0, 0, 1);
        glTranslatef(-camera.x, -camera.y, -camera.z);
    }

    void cmRotation(float xAngle, float zAngle)
    {
        camera.Zrot += zAngle;
        if (camera.Zrot < 0) camera.Zrot += 360;
        if (camera.Zrot > 360) camera.Zrot -= 360;
        camera.Xrot += xAngle;
        if (camera.Xrot < 0) camera.Xrot = 0;
        if (camera.Xrot > 180) camera.Xrot = 180;
    }

    void cmAutoMoveByMouse(int centerX, int centerY, float speed, DoubleBuffer curXBuffer, DoubleBuffer curYBuffer)
    {
        glfwGetCursorPos(window, curXBuffer, curYBuffer);
        Point cur = new Point();
        Point base = new Point();
        base.x = centerX;
        base.y = centerY;
        cur.x = (int) curXBuffer.get(0);
        cur.y = (int) curYBuffer.get(0);
        cmRotation((base.y - cur.y) * speed, (base.x - cur.x) * speed);
        glfwSetCursorPos(window, base.x, base.y);
    }

    int sqr(int a)
    {
        return a * a;
    }

    TCell CalcNormals(TCell a, TCell b, TCell c, TCell n)
    {
        float wrki;
        TCell v1 = new TCell(), v2 = new TCell();

        v1.x = a.x - b.x;
        v1.y = a.y - b.y;
        v1.z = a.z - b.z;
        v2.x = b.x - c.x;
        v2.y = b.y - c.y;
        v2.z = b.z - c.z;

        n.x = (v1.y * v2.z - v1.z * v2.y);
        n.y = (v1.z * v2.x - v1.x * v2.z);
        n.z = (v1.x * v2.y - v1.y * v2.x);
        wrki = (float) Math.sqrt((sqr((int) n.x) + sqr((int) n.y) + sqr((int) n.z)));
        n.x /= wrki;
        n.y /= wrki;
        n.z /= wrki;

        return n;
    }

    void mpInit()
    {
        for (int i = 0; i < plantMasCnt; i++) {
            plantMas[i] = new TObject();
        }

        glEnable(GL_ALPHA_TEST);
        glAlphaFunc(GL_GREATER, 0.99f);

        for (int i = 0; i < mapW; i++) {
            for (int j = 0; j < mapH; j++) {
                float dc = (float) Math.random() * 0.2f;
                mapCol[i][j].r = 0.31f + dc;
                mapCol[i][j].g = 0.5f + dc;
                mapCol[i][j].b = 0.13f + dc;

                map[i][j].x = i;
                map[i][j].y = j;
                map[i][j].z = (float) Math.random() * 0.2f;

                mapUV[i][j].u = i;
                mapUV[i][j].v = j;
            }
        }

        for (int i = 0; i < mapW-1; i++)
        {
            int pos = i * mapH;
            for (int j = 0; j < mapH-1; j++)
            {
                mapInd[i][j][0] = pos;
                mapInd[i][j][1] = pos + 1;
                mapInd[i][j][2] = pos + 1 + mapH;

                mapInd[i][j][3] = pos + 1 + mapH;
                mapInd[i][j][4] = pos + mapH;
                mapInd[i][j][5] = pos;

                pos++;
            }

            /*for (int l = 0; l < 10; l++) {
                int posX = (int) (Math.random() * mapW);
                int posY = (int) (Math.random() * mapH);
             */
                mpCreateHill(mapW / 2, mapH / 2, (int) (Math.random() * 50), hillHeight);
                mpCreateHill(mapW / 2 - 25, mapH / 2, (int) (Math.random() * 50), hillHeight);
                mpCreateHill(mapW / 2 + 25, mapH / 2, (int) (Math.random() * 50), hillHeight);
                mpCreateHill(mapW / 2, mapH / 2 - 25, (int) (Math.random() * 50), hillHeight);
                mpCreateHill(mapW / 2, mapH / 2 + 25, (int) (Math.random() * 50), hillHeight);
            //}
        }

        /*
        for (int i = 0; i < mapW; i++) {
            for (int j = 0; j < mapH; j++) {
                System.out.println(map[i][j].x + " " + map[i][j].y + " " + map[i][j].z);
                System.out.println();
                System.out.println(mapCol[i][j].r + " " + mapCol[i][j].g + " " + mapCol[i][j].b);
                System.out.println();
                System.out.println();
                //System.out.println(mapInd[i][j].r + " " + mapCol[i][j].g + " " + mapCol[i][j].b);
            }
        }

        for (int i = 0; i < mapW - 1; i++) {
            for (int j = 0; j < mapH - 1; j++) {
                for (int k = 0; k < 6; k++) {
                    System.out.println(mapInd[i][j][k]);
                }
            }
        }
         */

        int travaN = 4000;
        int gribN = 500;
        int flowerN = 200;
        for (int i = 0; i < plantMasCnt; i++) {
            if (i < travaN)
            {
                plantMas[i].type = tex_grass;
                plantMas[i].scale = (float) (0.7f + (Math.random() * 5) * 0.3f);
            }
            else if (i < (travaN + flowerN))
            {
                plantMas[i].type = (Math.random() * 5) % 2 == 0 ? tex_flower : tex_flower2;
                plantMas[i].scale = (float) (0.7f + (Math.random() * 5) * 0.1f);
            }
            else if (i < (travaN + gribN + flowerN))
            {
                plantMas[i].type = tex_mushroom;
                plantMas[i].scale = (float) (0.2f + (Math.random() * 2) * 0.2f);
            }
            else
            {
                int rand = (int) (Math.random() * 11);
                if (rand % 2 == 0)
                {
                    plantMas[i].type = tex_tree2;
                    plantMas[i].scale = (float) (4 + (Math.random() * 14));
                }
                if (rand % 2 != 0)
                {
                    plantMas[i].type = tex_tree;
                    plantMas[i].scale = (float) (4 + (Math.random() * 14));
                }
            }
            plantMas[i].x = (float) (Math.random() * mapW);
            plantMas[i].y = (float) (Math.random() * mapH);
            plantMas[i].z = mpGetHeight(plantMas[i].x, plantMas[i].y);
        }
        for (int i = 0; i < mapW-1; i++)
            for (int j = 0; j < mapH-1; j++)
                mapNormal[i][j] = CalcNormals(map[i][j], map[i+1][j], map[i][j+1], mapNormal[i][j]);
    }

    void mpShow()
    {
        alfa += 0.3f;
        if (alfa > 180) alfa -= 360;

        if (!selectMode) glEnable(GL_TEXTURE_2D);
        else glDisable(GL_TEXTURE_2D);

        if (!selectMode)
        {
            glEnableClientState(GL_VERTEX_ARRAY);
            glEnableClientState(GL_TEXTURE_COORD_ARRAY);
            //glEnableClientState(GL_COLOR_ARRAY);
            glVertexPointer(3, GL_FLOAT, 0, mapBuffer);
            glTexCoordPointer(2, GL_FLOAT, 0, texCoordB);
            //glColorPointer(3, GL_FLOAT, 0, mapColBuffer);
            glBindTexture(GL_TEXTURE_2D, tex_pole);
            glDrawElements(GL_TRIANGLES, mapIndBuffer);
            glDisableClientState(GL_VERTEX_ARRAY);
            glDisableClientState(GL_TEXTURE_COORD_ARRAY);
            //glDisableClientState(GL_COLOR_ARRAY);
        }

        glEnableClientState(GL_VERTEX_ARRAY);
        glEnableClientState(GL_TEXTURE_COORD_ARRAY);
        //glEnableClientState(GL_COLOR_ARRAY);
        glVertexPointer(3, GL_FLOAT, 0, plantBuffer);
        glTexCoordPointer(2, GL_FLOAT, 0, plantUVBuffer);
        //glColorPointer(3, GL_FLOAT, 0, mapColBuffer);
        glNormal3f(0, 0, 1);
        selectMasCnt = 0;
        int selectColor = 1;
        for (int i = 0; i < plantMasCnt; i++)
        {
            if (selectMode && (plantMas[i].type == tex_tree))
                continue;
            if (selectMode) {
                int radius = 3;
                if ((plantMas[i].x > camera.x - radius)
                        && (plantMas[i].x < camera.x + radius)
                        && (plantMas[i].y > camera.y - radius)
                        && (plantMas[i].y < camera.y + radius)) {
                    glColor3ub((byte) selectColor, (byte) 0, (byte) 0);
                    selectMas[selectMasCnt].colorIndex = selectColor;
                    selectMas[selectMasCnt].plantMas_Index = i;
                    selectMasCnt++;
                    selectColor++;
                    if (selectColor >= 255)
                        break;
                }
                else
                    continue;
            }
                glBindTexture(GL_TEXTURE_2D, plantMas[i].type);
                glPushMatrix();
                glTranslatef(plantMas[i].x, plantMas[i].y, plantMas[i].z);
                glScalef(plantMas[i].scale, plantMas[i].scale, plantMas[i].scale);
                glDrawElements(GL_TRIANGLES, plantIndBuffer);
                glPopMatrix();
        }
        glDisableClientState(GL_VERTEX_ARRAY);
        glDisableClientState(GL_TEXTURE_COORD_ARRAY);
        //glDisableClientState(GL_COLOR_ARRAY);
    }

    float Player_Move()
    {
        zKcc = 1.7f;
        float ugol = (float) (-camera.Zrot / 180 * Math.PI);
        float speed = 0;
        if (glfwGetKey(window, GLFW_KEY_W) > 0) speed = 0.1f;
        if (glfwGetKey(window, GLFW_KEY_S) > 0) speed = -0.1f;
        if (glfwGetKey(window, GLFW_KEY_A) > 0) {
            speed = 0.1f;
            ugol-=Math.PI*0.5f;
        }
        if (glfwGetKey(window, GLFW_KEY_D) > 0) {
            speed = 0.1f;
            ugol+=Math.PI*0.5f;
        }
        if (glfwGetKey(window, GLFW_KEY_LEFT_CONTROL) > 0 && glfwGetKey(window, GLFW_KEY_W) > 0) speed = speed * 4;
        if (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) > 0) { speed = speed / 4; zKcc = 1.1f; }
        if (speed != 0) {
            camera.x+=Math.sin(ugol) * speed;
            camera.y+=Math.cos(ugol) * speed;
        }
        return speed;
    }

    void Player_Take()
    {
        selectMode = true;
        mpShow();
        selectMode = false;

        int clr[] = new int[3];
        glReadPixels(windowX / 2, windowY / 2, 1, 1, GL_RGB, GL_UNSIGNED_BYTE, clr);

        if (clr[0] > 0)
        {
            for (int i = 0; i < selectMasCnt; i++) {
                if (selectMas[i].colorIndex == clr[0])
                    plantMas[selectMas[i].plantMas_Index].z = -1000;
            }
        }
    }

    /*void Game_Move()
    {
        if (glfwGetKey(window, GLFW_KEY_P) > 0)
            pressedOrNot += 1;
        //if (pressedOrNot % 2 == 0)
            cmAutoMoveByMouse(curXWindowbuffer.get(0) / 2, curYWindowbuffer.get(0) / 2, Player_Move(), curXBuffer, curYBuffer);
    }*/

    float mpGetHeight(float x, float y)
    {
        if (!IsCoordInMap(x, y)) return 0;
        int cX = (int) x;
        int cY = (int) y;
        if ((cX + 1 < mapW) && (cY + 1 < mapH) && ((cX + 1 < mapW) && (cY + 1 < mapH))) {
            float h1 = ((1 - (x - cX)) * map[cX][cY].z + (x - cX) * map[cX + 1][cY].z);
            float h2 = ((1 - (x - cX)) * map[cX][cY + 1].z + (x - cX) * map[cX + 1][cY + 1].z);
            return (1 - (y - cY)) * h1 + (y - cY) * h2;
        } else
            return 0;
    }

    void Game_Move()
    {
        if (glfwGetKey(window, GLFW_KEY_P) > 0)
            pressedOrNot = !pressedOrNot;
        if (pressedOrNot)
        {
            Player_Move();
            cmAutoMoveByMouse(curXWindowbuffer.get(0) / 2, curYWindowbuffer.get(0) / 2, 0.5f, curXBuffer, curYBuffer);
            camera.z = mpGetHeight(camera.x, camera.y) + zKcc;
        }
    }


    public static void main(String[] args)
    {
        new Main().run();
    }

    public static String[][][] generateWorld(int width, int height, int depth, int blocksRange, int randomInt1, int randomInt2, int randomInt3, Random random, String[][][] world)
    {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < depth; z++) {
                    if (randomInt1 == 1) {
                        world[x][y][z] = "w";
                    }
                    if (randomInt1 == 2) {
                        world[x][y][z] = "b";
                    }
                    if (randomInt1 == 3) {
                        world[x][y][z] = "o";
                    }
                    if (randomInt1 == 4 | randomInt2 == 6) {
                        world[x][y][z] = "p";
                    }
                    if (randomInt1 == 5 | randomInt2 == 7 | randomInt3 == 5) {
                        world[x][y][z] = "s";
                    }
                }
            }
        }
    return world;
    }
/*
    public static void ShowObj() {
        float x, y;
        float cnt = 40;
        float l = 0.5f;
        float a = (float) Math.PI * 2 / cnt;
/*
        glBegin(GL_TRIANGLES);

        glColor3f(1, 1, 0); glVertex2f(0, -0.5f);
        glColor3f(1, 1, 0); glVertex2f(-0.5f, 0);
        glColor3f(1, 1, 0); glVertex2f(0, 0);

        glColor3f(1, 1, 0); glVertex2f(0, -0.5f);
        glColor3f(1, 1, 0); glVertex2f(-0.5f, 0);
        glColor3f(1, 1, 0); glVertex2f(-0.5f, -0.5f);
*//*
        glBegin(GL_TRIANGLE_FAN);

        glColor3f(1, 1, 0);
        glVertex2f(0, 0);
        for (int i = -1; i < cnt; i++) {
            x = (float) Math.sin(a * i) * l;
            y = (float) Math.cos(a * i) * l;
            glVertex2f(x, y);
        }

        glEnd();
    }
*/

    public Main() {
        super("Mineshift");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            int blocksRange = 10;
            int num, num2, num3;
            int worldHeight = 256;
            int worldWidth = 256;
            int worldZ = 256;
            Random rand = new Random();
            num = rand.nextInt(blocksRange);
            num2 = rand.nextInt(blocksRange);
            num3 = rand.nextInt(blocksRange);
            String[][][] world = new String[worldHeight][worldWidth][worldZ];
            world = generateWorld(worldWidth, worldHeight, worldZ, blocksRange, num, num2, num3, rand, world);
        for (int i = 0; i < mapW; i++) {
            for (int j = 0; j < mapH; j++) {
                mapCol[i][j] = new TColor();
                map[i][j] = new TCell();
                mapNormal[i][j] = new TCell();
            }
        }

            /*
            for (int y = 0; y < worldHeight; y++) {
                for (int x = 0; x < worldWidth; x++) {
                    for (int z = 0; z < worldZ; z++) {
                        num = rand.nextInt(blocksRange);
                        num2 = rand.nextInt(blocksRange);
                        num3 = rand.nextInt(blocksRange);
                        if (num == 1) {
                            world[x][y][z] = "w";
                        }
                        if (num == 2) {
                            world[x][y][z] = "b";
                        }
                        if (num == 3) {
                            world[x][y][z] = "o";
                        }
                        if (num == 4 | num2 == 6) {
                            world[x][y][z] = "p";
                        }
                        if (num == 5 | num2 == 7 | num3 == 5) {
                            world[x][y][z] = "s";
                        }
                    }
                }
            }

             */
        }
    // The window handle
    private long window;

    /*
    public static void DrawCube() {
        glBegin(GL_QUADS);

        glColor3f(1.0f, 1.0f, 1.0f);
        glVertex3i(250, 450, 0);
        glColor3f(0.0f, 0.0f, 1.0f);
        glVertex3i(250, 150, 0);
        glColor3f(0.0f, 1.0f, 0.0f);
        glVertex3i(550, 150, 0);
        glColor3f(1.0f, 0.0f, 0.0f);
        glVertex3i(550, 450, 0);

        glColor3f(1.0f, 0.0f, 0.0f);
        glVertex3i(250, 450, 300);
        glColor3f(1.0f, 0.0f, 0.0f);
        glVertex3i(250, 150, 300);
        glColor3f(1.0f, 0.0f, 0.0f);
        glVertex3i(550, 150, 300);
        glColor3f(1.0f, 0.0f, 0.0f);
        glVertex3i(550, 450, 300);

        glColor3f(0.0f, 1.0f, 1.0f);
        glVertex3i(250, 150, 0);
        glColor3f(0.0f, 1.0f, 1.0f);
        glVertex3i(250, 450, 0);
        glColor3f(0.0f, 1.0f, 1.0f);
        glVertex3i(250, 450, 300);
        glColor3f(0.0f, 1.0f, 1.0f);
        glVertex3i(250, 150, 300);
        glEnd();
    }
*/
    public void run() {
        System.out.println(mapUV.length);
        init();
        loop();

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    private void init() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if ( !glfwInit() )
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW
        glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 2);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
        //glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        //glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

        // Create the window
        window = glfwCreateWindow(windowX, windowY, "Mineshift", NULL, NULL);
        if ( window == NULL )
            throw new RuntimeException("Failed to create the GLFW window");

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
        });

        // Get the thread stack and push a new frame
        try ( MemoryStack stack = stackPush() ) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        } // the stack frame is popped automatically
        //glfwSetWindowMonitor(window, glfwGetPrimaryMonitor(), 0, 0, windowX, windowY, 160);

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        // Enable v-sync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(window);
        //Player player = new Player();
        //Pig pig = new Pig();
        //player.initPlayer(20, 20, 0, 20);
        //pig.initMob("Pig", 20);
    }

    private void loop() {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();

        // Set the clear color
        glClearColor(0, 0, 0, 0.0f);


        for (int i = 0; i < mapW; i++) {
            for (int j = 0; j < mapH; j++) {
                mapUV[i][j] = new TUV();
            }
        }

        IntBuffer width0 = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer(),
                height0 = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer(),
                channels0 = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
        //IntBuffer textureNumber = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
        ByteBuffer dataB0 = ByteBuffer.allocateDirect(stbi_load("./textures/more.png", width0, height0, channels0, 0).capacity()).order(ByteOrder.nativeOrder());

        dataB0.put(stbi_load("./textures/more.png", width0, height0, channels0, 0));
        dataB0.flip();

        IntBuffer texture0 = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();

        glGenTextures(texture0);

        glBindTexture(GL_TEXTURE_2D, texture0.get());
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width0.get(), height0.get(), 0, channels0.get() == 4 ? GL_RGBA : GL_RGB, GL_UNSIGNED_BYTE, dataB0);

        glBindTexture(GL_TEXTURE_2D, 0);

        texture0.flip();
        dataB0.flip();

        tex_more = texture0.get();

        IntBuffer width = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer(),
                height = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer(),
                channels = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
        //IntBuffer textureNumber = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
        ByteBuffer dataB = ByteBuffer.allocateDirect(stbi_load("./textures/pole.png", width, height, channels, 0).capacity()).order(ByteOrder.nativeOrder());

        dataB.put(stbi_load("./textures/pole.png", width, height, channels, 0));
        dataB.flip();

        IntBuffer texture = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();

        glGenTextures(texture);

        glBindTexture(GL_TEXTURE_2D, texture.get());
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(), height.get(), 0, channels.get() == 4 ? GL_RGBA : GL_RGB, GL_UNSIGNED_BYTE, dataB);

        glBindTexture(GL_TEXTURE_2D, 0);

        texture.flip();
        dataB.flip();

        tex_pole = texture.get();

        IntBuffer width1 = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer(),
                height1 = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer(),
                channels1 = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
        //IntBuffer textureNumber = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
        ByteBuffer dataB1 = ByteBuffer.allocateDirect(stbi_load("./textures/trava.png", width1, height1, channels1, 0).capacity()).order(ByteOrder.nativeOrder());

        dataB1.put(stbi_load("./textures/trava.png", width1, height1, channels1, 0));
        dataB1.flip();

        IntBuffer texture1 = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder()).asIntBuffer();

        glGenTextures(texture1);

        glBindTexture(GL_TEXTURE_2D, texture1.get());
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width1.get(), height1.get(), 0, channels1.get() == 4 ? GL_RGBA : GL_RGB, GL_UNSIGNED_BYTE, dataB1);

        glBindTexture(GL_TEXTURE_2D, 0);

        texture1.flip();
        dataB1.flip();

        tex_grass = texture1.get();

        IntBuffer width2 = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer(),
                height2 = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer(),
                channels2 = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
        //IntBuffer textureNumber = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
        ByteBuffer dataB2 = ByteBuffer.allocateDirect(stbi_load("./textures/flower.png", width2, height2, channels2, 0).capacity()).order(ByteOrder.nativeOrder());

        dataB2.put(stbi_load("./textures/flower.png", width2, height2, channels2, 0));
        dataB2.flip();

        IntBuffer texture2 = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();

        glGenTextures(texture2);

        glBindTexture(GL_TEXTURE_2D, texture2.get());
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width2.get(), height2.get(), 0, channels2.get() == 4 ? GL_RGBA : GL_RGB, GL_UNSIGNED_BYTE, dataB2);

        glBindTexture(GL_TEXTURE_2D, 0);

        texture2.flip();
        dataB2.flip();

        tex_flower = texture2.get();

        IntBuffer width3 = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer(),
                height3 = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer(),
                channels3 = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
        //IntBuffer textureNumber = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
        ByteBuffer dataB3 = ByteBuffer.allocateDirect(stbi_load("./textures/flower2.png", width3, height3, channels3, 0).capacity()).order(ByteOrder.nativeOrder());

        dataB3.put(stbi_load("./textures/flower2.png", width3, height3, channels3, 0));
        dataB3.flip();

        IntBuffer texture3 = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();

        glGenTextures(texture3);

        glBindTexture(GL_TEXTURE_2D, texture3.get());
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width3.get(), height3.get(), 0, channels3.get() == 4 ? GL_RGBA : GL_RGB, GL_UNSIGNED_BYTE, dataB3);

        glBindTexture(GL_TEXTURE_2D, 0);

        texture3.flip();
        dataB3.flip();

        tex_flower2 = texture3.get();

        IntBuffer width4 = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer(),
                height4 = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer(),
                channels4 = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
        //IntBuffer textureNumber = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
        ByteBuffer dataB4 = ByteBuffer.allocateDirect(stbi_load("./textures/grib.png", width4, height4, channels4, 0).capacity()).order(ByteOrder.nativeOrder());

        dataB4.put(stbi_load("./textures/grib.png", width4, height4, channels4, 0));
        dataB4.flip();

        IntBuffer texture4 = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();

        glGenTextures(texture4);

        glBindTexture(GL_TEXTURE_2D, texture4.get());
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width4.get(), height4.get(), 0, channels4.get() == 4 ? GL_RGBA : GL_RGB, GL_UNSIGNED_BYTE, dataB4);

        glBindTexture(GL_TEXTURE_2D, 0);

        texture4.flip();
        dataB4.flip();

        tex_mushroom = texture4.get();

        IntBuffer width5 = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer(),
                height5 = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer(),
                channels5 = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
        //IntBuffer textureNumber = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
        ByteBuffer dataB5 = ByteBuffer.allocateDirect(stbi_load("./textures/tree.png", width5, height5, channels5, 0).capacity()).order(ByteOrder.nativeOrder());

        dataB5.put(stbi_load("./textures/tree.png", width5, height5, channels5, 0));
        dataB5.flip();

        IntBuffer texture5 = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();

        glGenTextures(texture5);

        glBindTexture(GL_TEXTURE_2D, texture5.get());
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width5.get(), height5.get(), 0, channels5.get() == 4 ? GL_RGBA : GL_RGB, GL_UNSIGNED_BYTE, dataB5);

        glBindTexture(GL_TEXTURE_2D, 0);

        texture5.flip();
        dataB5.flip();

        tex_tree = texture5.get();

        IntBuffer width6 = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer(),
                height6 = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer(),
                channels6 = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
        //IntBuffer textureNumber = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
        ByteBuffer dataB6 = ByteBuffer.allocateDirect(stbi_load("./textures/tree2.png", width6, height6, channels6, 0).capacity()).order(ByteOrder.nativeOrder());

        dataB6.put(stbi_load("./textures/tree2.png", width6, height6, channels6, 0));
        dataB6.flip();

        IntBuffer texture6 = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();

        glGenTextures(texture6);

        glBindTexture(GL_TEXTURE_2D, texture6.get());

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width6.get(), height6.get(), 0, channels6.get() == 4 ? GL_RGBA : GL_RGB, GL_UNSIGNED_BYTE, dataB6);

        glBindTexture(GL_TEXTURE_2D, 0);

        texture6.flip();
        dataB6.flip();

        tex_tree2 = texture6.get();


        mpInit();
        fillMapAndMapColAndMapInd();

        glEnable(GL_DEPTH_TEST);


        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while ( !glfwWindowShouldClose(window) ) {
            //glRotatef(0f, 0f, 0f, 0f);
            if (selectMode) glClearColor(0, 0, 0, 0.0f);
            else glClearColor(0.6f, 0.8f, 1, 0.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);


            /*
            glBegin(GL_TRIANGLES);

            glColor3f(1.0f, 0.0f, 0.0f); glVertex2f(0, 0);
            glColor3f(0.0f, 1.0f, 0.0f); glVertex2f(1, 0);
            glColor3f(0.0f, 0.0f, 1.0f); glVertex2f(1, 1);

            glColor3f(1.0f, 0.0f, 0.0f); glVertex2f(0, 0);
            glColor3f(0.0f, 1.0f, 0.0f); glVertex2f(0, 1);
            glColor3f(0.0f, 0.0f, 1.0f); glVertex2f(1, 1);
*/;

            glfwGetWindowSize(window, curXWindowbuffer, curYWindowbuffer);
            glfwGetWindowSize(window, Xbuffer, Ybuffer);

            WndResize(Xbuffer.get(0), Ybuffer.get(0));

            Game_Move();
            glPushMatrix();
                cmApply();
                mpShow();
            glPopMatrix();

            glfwSwapBuffers(window); // swap the color buffers

            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents();
        }
    }

}

class Mob {
    String name;
    int health;

    void initMob(String name, int health)
    {
        this.name = name;
        this.health = health;
    }
}

class Pig extends Mob {}

class Player {
    public void initPlayer(int healt, int hungry, int urovenDospe, int ai) {
        int health = healt;
        int hunger = hungry;
        int urovenDospeh = urovenDospe;
        int air = ai;
    }
}
