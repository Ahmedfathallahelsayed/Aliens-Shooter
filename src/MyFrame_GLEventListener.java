import Texture.TextureReader;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import javax.swing.*;

import com.sun.opengl.util.GLUT;

public class MyFrame_GLEventListener implements GLEventListener, KeyListener {

    String assetsFolderName = "Assets";
    String[] textureNames = new String[]{"background.png","Man1.png","Man2.png","Man3.png","Man4.png","ARMM.png","pacman2.png","PowerBar.png"};
    TextureReader.Texture[] texture = new TextureReader.Texture[textureNames.length];
    int[] textureIndex = new int[textureNames.length];

    double ManTranslateXValue = 0;
    double ManTranslateYValue = 0;
    int rotateAngleValue = 0;
    int control = 1;
    double facingX = 0;
    double facingY = 1;

    int animCounter = 0;
    int animDelay = 8;
    final int MAX_BULLETS = 500;
    double[] bulletsX = new double[MAX_BULLETS];
    double[] bulletsY = new double[MAX_BULLETS];
    double[] bulletsSpeedX = new double[MAX_BULLETS];
    double[] bulletsSpeedY = new double[MAX_BULLETS];
    int bulletsCount = 0;
    boolean fire = false;

    int fireDelayFrames = 45;
    int shotCooldown = 0;

    // Aliens arrays
    final int MAX_ALIENS = 200;
    double[] alienX = new double[MAX_ALIENS];
    double[] alienY = new double[MAX_ALIENS];
    double[] alienSpeed = new double[MAX_ALIENS];
    double[] alienAngle = new double[MAX_ALIENS];
    int alienCount = 0;

    double alienRenderScale = 0.1;
    final double bulletRenderScale = 0.03;

    int level = 1;

    boolean winShown = false;

    // game over flag
    boolean gameOverShown = false;

    boolean up=false, down=false, left=false, right=false;
    double moveSpeed = 0.01;

    int bulletsToShoot = 15;
    int bulletsShot = 0;
    int fireCounter = 0;

    long level1StartTime = 0L;
    long level2StartTime = 0L;
    long level1Time = 0L;
    long level2Time = 0L;
    private GLUT glut = new GLUT();

    int healthMax = 100;
    int health = healthMax;
    int hitCooldownFrames = 0;
    final int HIT_COOLDOWN_MAX = 60;


    boolean powerBarLeftToRight = false;

    int flashFrames = 0;
    final int FLASH_MAX = 18;
    int shakeFrames = 0;
    final int SHAKE_MAX = 16;
    final double SHAKE_MAG = 0.035;

    boolean paused = false;
    JFrame pauseWindow = null;
    private GamePanel gamePanel;
    float volume = 0.5f;

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

        resetGame();
    }


    public synchronized void resetGame(){
        level = 1;
        alienRenderScale = 0.1;

        ManTranslateXValue = 0;
        ManTranslateYValue = 0;
        rotateAngleValue = 0;
        control = 1;
        facingX = 0; facingY = 1;
        up = down = left = right = false;
        animCounter = 0;

        bulletsCount = 0;
        bulletsShot = 0;
        shotCooldown = 0;
        fire = false;

        alienCount = 20;
        initAliens(alienCount, 0.0012, 0.0006);

        level1StartTime = System.currentTimeMillis();
        level1Time = 0L;
        level2StartTime = 0L;
        level2Time = 0L;

        healthMax = 100;
        health = healthMax;
        hitCooldownFrames = 0;
        gameOverShown = false;

        flashFrames = 0;
        shakeFrames = 0;

        winShown = false;
    }


    public synchronized void goToNextLevel(){
        if(level != 1) return; // فقط من level1 الى level2
        level1Time = System.currentTimeMillis() - level1StartTime;

        level = 2;
        alienRenderScale = 0.05;
        alienCount = 60;
        initAliens(alienCount, 0.0025, 0.0015);
        bulletsCount = 0;
        bulletsShot = 0;
        shotCooldown = 0;
        fire = false;

        level2StartTime = System.currentTimeMillis();
        level2Time = 0L;

        winShown = false;

        healthMax = 300;
        health = healthMax;

        flashFrames = 0;
        shakeFrames = 0;
    }

    private void initAliens(int count, double baseSpeed, double randRange){
        int zones = 5;
        double zoneWidth = 2.0 / zones;
        for(int i = 0; i < count && i < MAX_ALIENS; i++){
            int zoneIndex = i % zones;
            double minX = -1 + zoneIndex * zoneWidth;
            double maxX = minX + zoneWidth;
            double worldX = minX + Math.random() * (maxX - minX); // this is in world coords (-1..1)
            double worldY = Math.random() < 0.5 ? 1.2 + Math.random() * 0.2 : -1.2 - Math.random() * 0.2;


            alienX[i] = worldX / alienRenderScale;
            alienY[i] = worldY / alienRenderScale;

            alienSpeed[i] = baseSpeed + Math.random() * randRange;
            alienAngle[i] = Math.random() * Math.PI * 2;
        }
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        if (paused) return;

        GL gl = drawable.getGL();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);

        if(shotCooldown > 0) shotCooldown--;

        if(hitCooldownFrames > 0) hitCooldownFrames--;

        updateManPositionAndAngle();

        gl.glPushMatrix();
        if(shakeFrames > 0){
            double intensity = (double)shakeFrames / (double)SHAKE_MAX; // decays
            double sx = (Math.random() * 2.0 - 1.0) * SHAKE_MAG * intensity;
            double sy = (Math.random() * 2.0 - 1.0) * SHAKE_MAG * intensity;
            gl.glTranslated((float)sx, (float)sy, 0f);
            shakeFrames--;
        }

        DrawBackground(gl);
        DrawMan(gl);
        handleBullets(gl);
        updateAndDrawAliens(gl);

        gl.glPopMatrix();
        if(flashFrames > 0){
            drawHitFlash(gl);
            flashFrames--;
        }

        drawHud(gl);

        if (alienCount == 0 && !winShown && !gameOverShown) {
            winShown = true;
            if (level == 1) {
                level1Time = System.currentTimeMillis() - level1StartTime;
            } else {
                level2Time = System.currentTimeMillis() - level2StartTime;
            }
            SwingUtilities.invokeLater(this::showWinMenu);
        }


        if (health <= 0 && !gameOverShown) {
            gameOverShown = true;
            SwingUtilities.invokeLater(this::showGameOverMenu);
        }

    }

    private void drawHitFlash(GL gl){
        float alpha = (float)flashFrames / (float)FLASH_MAX * 0.9f;
        if(alpha <= 0f) return;
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        gl.glEnable(GL.GL_BLEND);
        gl.glColor4f(1f, 0f, 0f, alpha);
        gl.glBegin(GL.GL_QUADS);
        gl.glVertex2f(-1f, -1f);
        gl.glVertex2f(-1f, 1f);
        gl.glVertex2f(1f, 1f);
        gl.glVertex2f(1f, -1f);
        gl.glEnd();
        gl.glDisable(GL.GL_BLEND);

        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPopMatrix();
    }


    private void drawHud(GL gl){
        long now = System.currentTimeMillis();

        gl.glColor3f(1f,1f,1f);


        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();


        float hbRight = 0.98f;
        float hbTop = 0.92f;
        float hbWidth = 0.60f;
        float hbHeight = 0.08f;

        float hbLeft = hbRight - hbWidth;

        double healthRatio = Math.max(0, Math.min(1.0, (double)health / (double)healthMax));

        gl.glColor4f(0f, 0f, 0f, 0.6f);
        gl.glBegin(GL.GL_QUADS);
        gl.glVertex2f(hbLeft, hbTop);
        gl.glVertex2f(hbLeft, hbTop - hbHeight);
        gl.glVertex2f(hbLeft + hbWidth, hbTop - hbHeight);
        gl.glVertex2f(hbLeft + hbWidth, hbTop);
        gl.glEnd();

        int powerBarTexIndex = textureIndex[textureIndex.length - 1];
        if(powerBarLeftToRight){
            if(healthRatio > 0.0){
                float padding = 0.006f;
                float fillLeft = hbLeft + padding;
                float fillRight = (float)(hbLeft + padding + (hbWidth - 2*padding) * healthRatio);
                float fillTop = hbTop - 0.006f;
                float fillBottom = hbTop - hbHeight + 0.006f;

                gl.glEnable(GL.GL_BLEND);
                gl.glBindTexture(GL.GL_TEXTURE_2D, powerBarTexIndex);
                gl.glColor3f(1f,1f,1f);
                gl.glBegin(GL.GL_QUADS);
                gl.glTexCoord2f(0.0f, 1.0f); gl.glVertex2f(fillLeft, fillTop);
                gl.glTexCoord2f(0.0f, 0.0f); gl.glVertex2f(fillLeft, fillBottom);
                gl.glTexCoord2f((float)healthRatio, 0.0f); gl.glVertex2f(fillRight, fillBottom);
                // right-top
                gl.glTexCoord2f((float)healthRatio, 1.0f); gl.glVertex2f(fillRight, fillTop);
                gl.glEnd();
                gl.glDisable(GL.GL_BLEND);
            }
        } else {
            if(healthRatio > 0.0){
                float padding = 0.006f;
                float fillRight = hbLeft + hbWidth - padding;
                float fillLeft = (float)(hbLeft + hbWidth - padding - (hbWidth - 2*padding) * healthRatio);
                float fillTop = hbTop - 0.006f;
                float fillBottom = hbTop - hbHeight + 0.006f;

                gl.glEnable(GL.GL_BLEND);
                gl.glBindTexture(GL.GL_TEXTURE_2D, powerBarTexIndex);
                gl.glColor3f(1f,1f,1f);
                gl.glBegin(GL.GL_QUADS);
                gl.glTexCoord2f((float)(1.0 - healthRatio), 1.0f); gl.glVertex2f(fillLeft, fillTop);
                gl.glTexCoord2f((float)(1.0 - healthRatio), 0.0f); gl.glVertex2f(fillLeft, fillBottom);
                gl.glTexCoord2f(1.0f, 0.0f); gl.glVertex2f(fillRight, fillBottom);
                gl.glTexCoord2f(1.0f, 1.0f); gl.glVertex2f(fillRight, fillTop);
                gl.glEnd();
                gl.glDisable(GL.GL_BLEND);
            }
        }

        gl.glColor3f(1f,1f,1f);
        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex2f(hbLeft, hbTop);
        gl.glVertex2f(hbLeft, hbTop - hbHeight);
        gl.glVertex2f(hbLeft + hbWidth, hbTop - hbHeight);
        gl.glVertex2f(hbLeft + hbWidth, hbTop);
        gl.glEnd();

        String hpText = "HP: " + health + " / " + healthMax;
        gl.glRasterPos2f(hbLeft + 0.02f, hbTop - hbHeight/2f - 0.01f);
        glut.glutBitmapString(GLUT.BITMAP_HELVETICA_12, hpText);

        gl.glScalef(1.4f, 1.4f, 1f);

        if(level == 1){
            long elapsed;
            if(winShown){
                elapsed = level1Time;
            } else {
                elapsed = (level1StartTime > 0) ? (now - level1StartTime) : 0L;
            }
            String text = "Level 1  " + formatTime(elapsed);

            gl.glRasterPos2f(-0.70f, 0.60f);
            glut.glutBitmapString(GLUT.BITMAP_HELVETICA_18, text);
        } else {
            long lvl1 = level1Time;
            long lvl2Current;
            if(winShown){
                lvl2Current = level2Time;
            } else {
                lvl2Current = (level2StartTime > 0) ? (now - level2StartTime) : 0L;
            }
            long total = lvl1 + lvl2Current;

            gl.glRasterPos2f(-0.70f, 0.60f);
            glut.glutBitmapString(GLUT.BITMAP_HELVETICA_18, "Lvl1: " + formatTime(lvl1));

            gl.glRasterPos2f(-0.70f, 0.50f);
            glut.glutBitmapString(GLUT.BITMAP_HELVETICA_18, "Lvl2: " + formatTime(lvl2Current));

            gl.glRasterPos2f(-0.70f, 0.40f);
            glut.glutBitmapString(GLUT.BITMAP_HELVETICA_18, "Total: " + formatTime(total));
        }
        gl.glLoadIdentity();
        gl.glColor3f(1f,1f,1f);

        gl.glRasterPos2f(-0.90f, -0.90f);
        glut.glutBitmapString(GLUT.BITMAP_HELVETICA_18, "Tap ESC To Menu");

        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPopMatrix();

        gl.glColor3f(1f,1f,1f);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
    @Override
    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {}

    public void DrawBackground(GL gl){
        gl.glEnable(GL.GL_BLEND);
        gl.glBindTexture(GL.GL_TEXTURE_2D, textureIndex[0]);
        gl.glBegin(GL.GL_QUADS);
        gl.glTexCoord2f(0,0); gl.glVertex2f(-1,-1);
        gl.glTexCoord2f(0,1); gl.glVertex2f(-1,1);
        gl.glTexCoord2f(1,1); gl.glVertex2f(1,1);
        gl.glTexCoord2f(1,0); gl.glVertex2f(1,-1);
        gl.glEnd();
        gl.glDisable(GL.GL_BLEND);
    }

    public void DrawMan(GL gl){
        if(health <= 0) return;

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
            double len = Math.sqrt(dx*dx + dy*dy);
            facingX = dx / len;
            facingY = dy / len;

            animCounter++;
            if(animCounter >= animDelay){
                animCounter = 0;
                control = control % 4 + 1;
            }
        } else {
            control = 1;
            animCounter = 0;
        }

        double minX = -9.5;
        double maxX =  9.5;
        double minY = -9.5;
        double maxY =  9.5;

        if (ManTranslateXValue < minX) ManTranslateXValue = minX;
        if (ManTranslateXValue > maxX) ManTranslateXValue = maxX;
        if (ManTranslateYValue < minY) ManTranslateYValue = minY;
        if (ManTranslateYValue > maxY) ManTranslateYValue = maxY;
    }

    public void handleBullets(GL gl){
        double manScale = 0.1;
        double bulletScale = bulletRenderScale;

        double localMuzzleX = 0;
        double localMuzzleY = 1.4;

        double angRadForTransform = Math.toRadians(rotateAngleValue);
        double cosA = Math.cos(angRadForTransform);
        double sinA = Math.sin(angRadForTransform);
        double rotLocalX = cosA * localMuzzleX - sinA * localMuzzleY;
        double rotLocalY = sinA * localMuzzleX + cosA * localMuzzleY;

        double transX = rotLocalX + ManTranslateXValue;
        double transY = rotLocalY + ManTranslateYValue;

        double scaleFactorForBullet = manScale / bulletScale;
        double worldMuzzleX_forBulletSpace = transX * scaleFactorForBullet;
        double worldMuzzleY_forBulletSpace = transY * scaleFactorForBullet;

        if(fire && bulletsShot < bulletsToShoot){
            if(shotCooldown == 0){
                double angleRad = Math.toRadians(rotateAngleValue + 90);
                double fx = Math.cos(angleRad);
                double fy = Math.sin(angleRad);

                if(bulletsCount < MAX_BULLETS){
                    bulletsX[bulletsCount] = worldMuzzleX_forBulletSpace;
                    bulletsY[bulletsCount] = worldMuzzleY_forBulletSpace;

                    double speed = 0.08 * (manScale / 0.1);
                    bulletsSpeedX[bulletsCount] = speed * fx;
                    bulletsSpeedY[bulletsCount] = speed * fy;

                    bulletsCount++;
                }

                bulletsShot++;
                shotCooldown = fireDelayFrames;
            }
        }

        if(!fire){
            bulletsShot = 0;
        }

        for(int i = 0; i < bulletsCount; i++){
            bulletsX[i] += bulletsSpeedX[i];
            bulletsY[i] += bulletsSpeedY[i];

            double angleDegBullet = Math.toDegrees(Math.atan2(bulletsSpeedY[i], bulletsSpeedX[i])) - 90;

            gl.glPushMatrix();
            gl.glScaled(bulletRenderScale, bulletRenderScale, 1);
            gl.glTranslated(bulletsX[i], bulletsY[i], 0);
            gl.glRotated(angleDegBullet, 0, 0, 1);
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

        // reset after shooting limit reached
        if(bulletsShot >= bulletsToShoot){
            fire = false;
            bulletsShot = 0;
        }

        int write = 0;
        double bound = 100.0;
        for(int i=0;i<bulletsCount;i++){
            if(Math.abs(bulletsX[i]) <= bound && Math.abs(bulletsY[i]) <= bound){
                if(write != i){
                    bulletsX[write] = bulletsX[i];
                    bulletsY[write] = bulletsY[i];
                    bulletsSpeedX[write] = bulletsSpeedX[i];
                    bulletsSpeedY[write] = bulletsSpeedY[i];
                }
                write++;
            }
        }
        bulletsCount = write;
    }

    public void updateAndDrawAliens(GL gl) {

        double storedMin, storedMax;
        if(level == 1){
            storedMin = -9.5;
            storedMax = 9.5;
        } else {
            storedMin = -1.0 / alienRenderScale;
            storedMax =  1.0 / alienRenderScale;
        }

        double invScale = 1.0 / alienRenderScale;

        for(int i = 0; i < alienCount; i++){
            alienAngle[i] += (Math.random() - 0.5) * 0.15;
            double worldDx = Math.cos(alienAngle[i]) * alienSpeed[i];
            double worldDy = Math.sin(alienAngle[i]) * alienSpeed[i];

            alienX[i] += worldDx * invScale;
            alienY[i] += worldDy * invScale;

            if(alienX[i] < storedMin){
                alienX[i] = storedMin;
                alienAngle[i] = Math.PI - alienAngle[i];
            } else if(alienX[i] > storedMax){
                alienX[i] = storedMax;
                alienAngle[i] = Math.PI - alienAngle[i];
            }
            if(alienY[i] < storedMin){
                alienY[i] = storedMin;
                alienAngle[i] = -alienAngle[i];
            } else if(alienY[i] > storedMax){
                alienY[i] = storedMax;
                alienAngle[i] = -alienAngle[i];
            }
        }

        for(int i = 0; i < alienCount; i++){
            gl.glPushMatrix();
            gl.glScaled(alienRenderScale, alienRenderScale, 1);
            gl.glTranslated(alienX[i], alienY[i], 0);
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

        double collisionThreshold = Math.max(0.12, alienRenderScale * 1.2);

        for(int ai = alienCount - 1; ai >= 0; ai--){
            boolean alienRemoved = false;
            for(int bi = bulletsCount - 1; bi >= 0; bi--){
                double bulletScreenX = bulletRenderScale * bulletsX[bi];
                double bulletScreenY = bulletRenderScale * bulletsY[bi];
                double alienScreenX = alienRenderScale * alienX[ai];
                double alienScreenY = alienRenderScale * alienY[ai];

                double dx = bulletScreenX - alienScreenX;
                double dy = bulletScreenY - alienScreenY;
                if(Math.sqrt(dx*dx + dy*dy) < collisionThreshold){
                    for(int k = ai; k < alienCount - 1; k++){
                        alienX[k] = alienX[k+1];
                        alienY[k] = alienY[k+1];
                        alienSpeed[k] = alienSpeed[k+1];
                        alienAngle[k] = alienAngle[k+1];
                    }
                    alienCount--;

                    for(int k = bi; k < bulletsCount - 1; k++){
                        bulletsX[k] = bulletsX[k+1];
                        bulletsY[k] = bulletsY[k+1];
                        bulletsSpeedX[k] = bulletsSpeedX[k+1];
                        bulletsSpeedY[k] = bulletsSpeedY[k+1];
                    }
                    bulletsCount--;

                    alienRemoved = true;
                    break;
                }
            }
            if(alienRemoved){
            }
        }


        double manScreenX = 0.1 * ManTranslateXValue;
        double manScreenY = 0.1 * ManTranslateYValue;
        double playerCollisionRadius = 0.12;

        for(int i = alienCount - 1; i >= 0; i--){
            double alienScreenX = alienRenderScale * alienX[i];
            double alienScreenY = alienRenderScale * alienY[i];
            double dx = alienScreenX - manScreenX;
            double dy = alienScreenY - manScreenY;
            double dist = Math.sqrt(dx*dx + dy*dy);

            if(dist < playerCollisionRadius){
                if(hitCooldownFrames == 0 && health > 0 && !gameOverShown){
                    health -= 10;
                    if(health < 0) health = 0;
                    hitCooldownFrames = HIT_COOLDOWN_MAX;

                    flashFrames = FLASH_MAX;

                    shakeFrames = SHAKE_MAX;

                    double pushDirX = dx == 0 ? 0.01 : dx / dist * 0.02;
                    double pushDirY = dy == 0 ? 0.01 : dy / dist * 0.02;
                    alienX[i] += pushDirX / alienRenderScale;
                    alienY[i] += pushDirY / alienRenderScale;
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
            case KeyEvent.VK_ESCAPE:
                if (!paused) showPauseMenu();
                break;

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

    private String formatTime(long ms) {
        long sec = ms / 1000;
        long min = sec / 60;
        sec = sec % 60;
        return String.format("%02d:%02d", min, sec);
    }

    private double randNum(double min, double max) {
        return min + Math.random() * (max - min);
    }
    public void setGamePanel(GamePanel gp){
        this.gamePanel = gp;
    }
    private void showPauseMenu() {

        if (pauseWindow != null) return;

        paused = true;

        pauseWindow = new JFrame("Paused");
        pauseWindow.setUndecorated(true);
        pauseWindow.setSize(420, 300);
        pauseWindow.setLocationRelativeTo(null);
        pauseWindow.setBackground(new Color(0, 0, 0, 0));

        JPanel container = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(new Color(30, 30, 30, 200));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);
            }
        };

        container.setLayout(new GridBagLayout());
        container.setOpaque(false);
        pauseWindow.add(container);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Paused", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 26));
        title.setForeground(Color.WHITE);
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        container.add(title, gbc);

        gbc.gridy = 1;
        gbc.gridwidth = 1;

        JLabel volumeLabel = new JLabel("Volume:");
        volumeLabel.setForeground(Color.WHITE);
        container.add(volumeLabel, gbc);

        JSlider volumeSlider = new JSlider(0, 100, (int)(volume * 100));
        volumeSlider.setOpaque(false);
        volumeSlider.setBackground(new Color(0, 0, 0, 0));

        volumeSlider.setValue(Math.round(Sound.SoundManager.getGlobalVolume() * 100f));

        volumeSlider.addChangeListener(e -> {
            float v = volumeSlider.getValue() / 100f;
            volume = v;

            System.out.println("Current Volume = " + v);

            try {
                Sound.SoundManager.setGlobalVolume(v);
            } catch (Throwable ex) {
                // لا تجعل الخطأ يوقف اللعبة — بس اطبع للاختبار
                System.err.println("setGlobalVolume failed: " + ex.getMessage());
            }


        });


        gbc.gridx = 1;
        container.add(volumeSlider, gbc);

        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.gridwidth = 2;

        JButton resumeBtn = new JButton("Resume");
        styleButton(resumeBtn);
        resumeBtn.addActionListener(e -> {
            paused = false;
            pauseWindow.dispose();
            pauseWindow = null;
        });
        container.add(resumeBtn, gbc);

        gbc.gridy = 3;
        JButton menuBtn = new JButton("Back to Menu");
        styleButton(menuBtn);
        menuBtn.addActionListener(e -> {
            paused = false;
            pauseWindow.dispose();
            pauseWindow = null;
            if (gamePanel != null)
                gamePanel.returnToMenu();
        });
        container.add(menuBtn, gbc);

        pauseWindow.setVisible(true);
    }

    private void styleButton(JButton btn) {
        btn.setFocusPainted(false);
        btn.setFont(new Font("Arial", Font.BOLD, 18));

        Color base = new Color(0xC2A878);
        Color hover = new Color(0xD9C7A6);
        Color border = new Color(0x7A6240);
        Color text = new Color(0x3B2F1B);

        btn.setBackground(base);
        btn.setForeground(text);
        btn.setBorder(BorderFactory.createLineBorder(border, 3));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(hover);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(base);
            }
        });
    }
    private void showGameOverMenu() {
        // إذا المينيو مفتوح متفتحش تاني
        if (pauseWindow != null) return;

        // علشان نمنع تكرار النوافذ
        gameOverShown = true;
        paused = true;

        pauseWindow = new JFrame("Game Over");
        pauseWindow.setUndecorated(true);
        pauseWindow.setSize(420, 260);
        pauseWindow.setLocationRelativeTo(null);
        pauseWindow.setBackground(new Color(0,0,0,0));

        JPanel container = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(30, 30, 30, 200));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);
            }
        };

        container.setLayout(new GridBagLayout());
        container.setOpaque(false);
        pauseWindow.add(container);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Game Over", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 26));
        title.setForeground(Color.WHITE);
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        container.add(title, gbc);

        gbc.gridy = 1;
        JLabel msg = new JLabel("<html><center>You died. Try again!</center></html>", JLabel.CENTER);
        msg.setFont(new Font("Arial", Font.PLAIN, 14));
        msg.setForeground(Color.LIGHT_GRAY);
        container.add(msg, gbc);

        gbc.gridy = 2;
        gbc.gridwidth = 2;
        JButton restartBtn = new JButton("Restart");
        restartBtn.setFocusable(false);
        restartBtn.setRequestFocusEnabled(false);
        styleButton(restartBtn);
        restartBtn.addActionListener(e -> {
            pauseWindow.dispose();
            pauseWindow = null;
            resetGame();
            paused = false;
            gameOverShown = false;
        });
        container.add(restartBtn, gbc);

        gbc.gridy = 3;
        JButton backBtn = new JButton("Back to Menu");
        backBtn.setFocusable(false);
        backBtn.setRequestFocusEnabled(false);
        styleButton(backBtn);
        backBtn.addActionListener(e -> {
            pauseWindow.dispose();
            pauseWindow = null;
            paused = false;
            if (gamePanel != null) {
                gamePanel.returnToMenu();
            }
        });

        container.add(backBtn, gbc);

        pauseWindow.setVisible(true);
    }
    private void showWinMenu() {

        if (pauseWindow != null) return;

        winShown = true;
        paused = true;

        if (level == 1) {
            if (level1Time == 0L)
                level1Time = System.currentTimeMillis() - level1StartTime;
        } else {
            if (level2Time == 0L)
                level2Time = System.currentTimeMillis() - level2StartTime;
        }

        pauseWindow = new JFrame("You Win!");
        pauseWindow.setUndecorated(true);
        pauseWindow.setSize(420, 320);
        pauseWindow.setLocationRelativeTo(null);
        pauseWindow.setBackground(new Color(0,0,0,0));

        JPanel container = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(30, 30, 30, 200));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);
            }
        };

        container.setLayout(new GridBagLayout());
        container.setOpaque(false);
        pauseWindow.add(container);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 12, 8, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("You Win!", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 26));
        title.setForeground(Color.WHITE);
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        container.add(title, gbc);

        gbc.gridy = 1;
        String timesHtml;

        if (level == 1) {
            String t1 = formatTime(level1Time);
            timesHtml = "<html><center>Level 1 Time: " + t1 + "</center></html>";
        } else {
            String t1 = formatTime(level1Time);
            String t2 = formatTime(level2Time);
            String tTotal = formatTime(level1Time + level2Time);
            timesHtml = "<html><center>Level 1: " + t1 +
                    "<br/>Level 2: " + t2 +
                    "<br/>Total: " + tTotal + "</center></html>";
        }

        JLabel timesLabel = new JLabel(timesHtml, JLabel.CENTER);
        timesLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        timesLabel.setForeground(Color.LIGHT_GRAY);
        container.add(timesLabel, gbc);


        gbc.gridy = 2;
        gbc.gridwidth = 2;

        JButton nextBtn = new JButton("Up To Next Level");
        styleButton(nextBtn);
        nextBtn.setFocusable(false);
        nextBtn.setRequestFocusEnabled(false);

        if (level == 1) {
            nextBtn.setEnabled(true);
            nextBtn.addActionListener(e -> {
                pauseWindow.dispose();
                pauseWindow = null;

                if (level1Time == 0L)
                    level1Time = System.currentTimeMillis() - level1StartTime;

                goToNextLevel();
                paused = false;
                winShown = false;

                if (gamePanel != null) {
                    SwingUtilities.invokeLater(() -> {
                        gamePanel.glcanvas.requestFocusInWindow();
                        if(!gamePanel.animator.isAnimating())
                            gamePanel.animator.start();
                    });
                }
            });
        } else {
            nextBtn.setEnabled(false);
        }

        container.add(nextBtn, gbc);


        gbc.gridy = 3;

        JButton restartBtn = new JButton("Restart");
        styleButton(restartBtn);
        restartBtn.setFocusable(false);
        restartBtn.setRequestFocusEnabled(false);
        restartBtn.addActionListener(e -> {
            pauseWindow.dispose();
            pauseWindow = null;

            resetGame();
            paused = false;
            winShown = false;

            if (gamePanel != null) {
                SwingUtilities.invokeLater(() -> {
                    gamePanel.glcanvas.requestFocusInWindow();
                    if(!gamePanel.animator.isAnimating())
                        gamePanel.animator.start();
                });
            }
        });

        container.add(restartBtn, gbc);


        gbc.gridy = 4;

        JButton backBtn = new JButton("Back to Menu");
        backBtn.setFocusable(false);
        backBtn.setRequestFocusEnabled(false);
        styleButton(backBtn);
        backBtn.addActionListener(e -> {
            pauseWindow.dispose();
            pauseWindow = null;

            paused = false;
            winShown = false;

            if (gamePanel != null) {
                gamePanel.returnToMenu();
            }
        });

        container.add(backBtn, gbc);

        pauseWindow.setVisible(true);
    }

}

