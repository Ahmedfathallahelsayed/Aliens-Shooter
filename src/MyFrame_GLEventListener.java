import Texture.TextureReader;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.ArrayList;

public class MyFrame_GLEventListener implements GLEventListener, KeyListener {

    String assetsFolderName = "Assets";
    String[] textureNames = new String[]{"background.png","Man1.png","Man2.png","Man3.png","Man4.png","ARMM.png","pacman2.png"};
    TextureReader.Texture[] texture = new TextureReader.Texture[textureNames.length];
    int[] textureIndex = new int[textureNames.length];

    double ManTranslateXValue = 0;
    double ManTranslateYValue = 0;
    int rotateAngleValue = 0;
    int control = 1;

    ArrayList<Double> bulletsX = new ArrayList<>();
    ArrayList<Double> bulletsY = new ArrayList<>();
    ArrayList<Double> bulletsSpeedX = new ArrayList<>();
    ArrayList<Double> bulletsSpeedY = new ArrayList<>();
    boolean fire = false;
    // Aliens
    ArrayList<Double> alienX = new ArrayList<>();
    ArrayList<Double> alienY = new ArrayList<>();
    ArrayList<Double> alienSpeed = new ArrayList<>();


    boolean up=false, down=false, left=false, right=false;
    double moveSpeed = 0.04;

    int bulletsToShoot = 15;
    int bulletsShot = 0;
    int fireCounter = 0;
    @Override
    public void init(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();
        gl.glClearColor(0f,0f,0f,0f);
        gl.glEnable(GL.GL_TEXTURE_2D);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        gl.glGenTextures(textureIndex.length, textureIndex, 0);

        for(int i=0;i<textureNames.length;i++){
            try{
                texture[i] = TextureReader.readTexture(assetsFolderName + "//" + textureNames[i], true);
                gl.glBindTexture(GL.GL_TEXTURE_2D, textureIndex[i]);
                new GLU().gluBuild2DMipmaps(GL.GL_TEXTURE_2D, GL.GL_RGBA,
                        texture[i].getWidth(), texture[i].getHeight(),
                        GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, texture[i].getPixels());
            }catch(IOException e){
                e.printStackTrace();
            }
        }

        int alienCount = 20;
        int zones = 5;
        double zoneWidth = 2.0 / zones;

        for(int i = 0; i < alienCount; i++){
            int zoneIndex = i % zones;
            double minX = -1 + zoneIndex * zoneWidth;
            double maxX = minX + zoneWidth;
            double x = minX + Math.random() * (maxX - minX);

            double y = Math.random() < 0.5 ? 1.2 + Math.random() * 0.2 : -1.2 - Math.random() * 0.2;

            alienX.add(x);
            alienY.add(y);
            alienSpeed.add(0.005 + Math.random() * 0.01);
        }
    }


    @Override
    public void display(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);

        updateManPositionAndAngle();
        DrawBackground(gl);
        DrawMan(gl);
        handleBullets(gl);
        updateAndDrawAliens(gl);

    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
    @Override
    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {}

    public void DrawBackground(GL gl){
        gl.glEnable(GL.GL_BLEND);
        gl.glBindTexture(GL.GL_TEXTURE_2D, textureIndex[0]);
        gl.glBegin(GL.GL_QUADS);
        gl.glTexCoord2f(0,0); gl.glVertex3f(-1,-1,-1);
        gl.glTexCoord2f(0,1); gl.glVertex3f(-1,1,-1);
        gl.glTexCoord2f(1,1); gl.glVertex3f(1,1,-1);
        gl.glTexCoord2f(1,0); gl.glVertex3f(1,-1,-1);
        gl.glEnd();
        gl.glDisable(GL.GL_BLEND);
    }

    public void DrawMan(GL gl){
        gl.glPushMatrix();
        gl.glScaled(0.1,0.1,1);
        gl.glTranslated(ManTranslateXValue, ManTranslateYValue,0);
        gl.glRotated(rotateAngleValue,0,0,1);

        gl.glEnable(GL.GL_BLEND);
        gl.glBindTexture(GL.GL_TEXTURE_2D, textureIndex[control]);
        gl.glBegin(GL.GL_QUADS);
        gl.glTexCoord2f(0,0); gl.glVertex3f(-1,-1,-1);
        gl.glTexCoord2f(0,1); gl.glVertex3f(-1,1,-1);
        gl.glTexCoord2f(1,1); gl.glVertex3f(1,1,-1);
        gl.glTexCoord2f(1,0); gl.glVertex3f(1,-1,-1);
        gl.glEnd();
        gl.glDisable(GL.GL_BLEND);
        gl.glPopMatrix();
    }

    public void updateManPositionAndAngle(){
        double dx=0, dy=0;
        if(up) dy += moveSpeed;
        if(down) dy -= moveSpeed;
        if(left) dx -= moveSpeed;
        if(right) dx += moveSpeed;

        ManTranslateXValue += dx;
        ManTranslateYValue += dy;

        if(dx != 0 || dy != 0){
            rotateAngleValue = (int)Math.toDegrees(Math.atan2(dy, dx)) - 90;
        }
    }

    public void handleBullets(GL gl){
        // مقياس الجندي
        double manScale = 0.1;

        double localOffsetX = 0.0;
        double localOffsetY = 1.0;

        if(fire && bulletsShot < bulletsToShoot){
            fireCounter++;
            if(fireCounter % 20 == 0){
                double angleRad = Math.toRadians(rotateAngleValue + 90);

                double rotatedOffsetX = localOffsetX * Math.cos(angleRad) - localOffsetY * Math.sin(angleRad);
                double rotatedOffsetY = localOffsetX * Math.sin(angleRad) + localOffsetY * Math.cos(angleRad);

                double startX = ManTranslateXValue + rotatedOffsetX * manScale;
                double startY = ManTranslateYValue + rotatedOffsetY * manScale;

                bulletsX.add(startX);
                bulletsY.add(startY);

                double speed = 0.05;
                bulletsSpeedX.add(speed * Math.cos(angleRad));
                bulletsSpeedY.add(speed * Math.sin(angleRad));

                bulletsShot++;
            }
        }

        for(int i=0; i<bulletsX.size(); i++){
            bulletsX.set(i, bulletsX.get(i) + bulletsSpeedX.get(i));
            bulletsY.set(i, bulletsY.get(i) + bulletsSpeedY.get(i));

            gl.glPushMatrix();
            gl.glScaled(0.05,0.05,1);
            gl.glTranslated(bulletsX.get(i), bulletsY.get(i), 0);
            gl.glEnable(GL.GL_BLEND);
            gl.glBindTexture(GL.GL_TEXTURE_2D, textureIndex[5]);
            gl.glBegin(GL.GL_QUADS);
            gl.glTexCoord2f(0,0); gl.glVertex3f(-1,-1,-1);
            gl.glTexCoord2f(0,1); gl.glVertex3f(-1,1,-1);
            gl.glTexCoord2f(1,1); gl.glVertex3f(1,1,-1);
            gl.glTexCoord2f(1,0); gl.glVertex3f(1,-1,-1);
            gl.glEnd();
            gl.glDisable(GL.GL_BLEND);
            gl.glPopMatrix();
        }

        if(bulletsShot >= bulletsToShoot){
            fire = false;
            bulletsShot = 0;
            fireCounter = 0;
        }
    }


    public void updateAndDrawAliens(GL gl) {
        for(int i = 0; i < alienX.size(); i++){
            double dx = ManTranslateXValue - alienX.get(i);
            double dy = ManTranslateYValue - alienY.get(i);
            double length = Math.sqrt(dx*dx + dy*dy);
            if(length != 0){
                dx /= length;
                dy /= length;
            }
            double speed = alienSpeed.get(i);
            alienX.set(i, alienX.get(i) + dx * speed);
            alienY.set(i, alienY.get(i) + dy * speed);
        }

        // رسم كل alien
        for(int i = 0; i < alienX.size(); i++){
            gl.glPushMatrix();
            gl.glScaled(0.1, 0.1, 1);
            gl.glTranslated(alienX.get(i), alienY.get(i), 0);
            gl.glEnable(GL.GL_BLEND);
            gl.glBindTexture(GL.GL_TEXTURE_2D, textureIndex[6]);
            gl.glBegin(GL.GL_QUADS);
            gl.glTexCoord2f(0,0); gl.glVertex3f(-1,-1,-1);
            gl.glTexCoord2f(0,1); gl.glVertex3f(-1,1,-1);
            gl.glTexCoord2f(1,1); gl.glVertex3f(1,1,-1);
            gl.glTexCoord2f(1,0); gl.glVertex3f(1,-1,-1);
            gl.glEnd();
            gl.glDisable(GL.GL_BLEND);
            gl.glPopMatrix();
        }

        for(int i = alienX.size() - 1; i >= 0; i--){
            for(int j = bulletsX.size() - 1; j >= 0; j--){
                double dx = bulletsX.get(j) - alienX.get(i);
                double dy = bulletsY.get(j) - alienY.get(i);
                if(Math.sqrt(dx*dx + dy*dy) < 0.1){ // مسافة الاصطدام
                    alienX.remove(i);
                    alienY.remove(i);
                    alienSpeed.remove(i);
                    bulletsX.remove(j);
                    bulletsY.remove(j);
                    bulletsSpeedX.remove(j);
                    bulletsSpeedY.remove(j);
                    break;
                }
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        switch(e.getKeyCode()){
            case KeyEvent.VK_UP: up = true; break;
            case KeyEvent.VK_DOWN: down = true; break;
            case KeyEvent.VK_LEFT: left = true; break;
            case KeyEvent.VK_RIGHT: right = true; break;
            case KeyEvent.VK_SPACE: fire = true; break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch(e.getKeyCode()){
            case KeyEvent.VK_UP: up=false; break;
            case KeyEvent.VK_DOWN: down=false; break;
            case KeyEvent.VK_LEFT: left=false; break;
            case KeyEvent.VK_RIGHT: right=false; break;
            case KeyEvent.VK_SPACE: fire=false; break;
        }
    }
}
