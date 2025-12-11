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
    // PowerBar.png should be present in Assets
    String[] textureNames = new String[]{"background.png","Man1.png","Man2.png","Man3.png","Man4.png","ARMM.png","pacman2.png","PowerBar.png"};
    TextureReader.Texture[] texture = new TextureReader.Texture[textureNames.length];
    int[] textureIndex = new int[textureNames.length];

    double ManTranslateXValue = 0;
    double ManTranslateYValue = 0;
    int rotateAngleValue = 0;
    int control = 1; // man sprite index 1..4

    double facingX = 0;
    double facingY = 1;

    // animation variables for the man sprites (circular)
    int animCounter = 0;
    int animDelay = 8; // frames between sprite changes (tuneable)

    // Bullets arrays
    final int MAX_BULLETS = 500;
    double[] bulletsX = new double[MAX_BULLETS];
    double[] bulletsY = new double[MAX_BULLETS];
    double[] bulletsSpeedX = new double[MAX_BULLETS];
    double[] bulletsSpeedY = new double[MAX_BULLETS];
    int bulletsCount = 0;
    boolean fire = false;

    // shot spacing (holding SPACE)
    int fireDelayFrames = 45;
    int shotCooldown = 0;

    // Aliens arrays
    final int MAX_ALIENS = 200;
    double[] alienX = new double[MAX_ALIENS];
    double[] alienY = new double[MAX_ALIENS];
    double[] alienSpeed = new double[MAX_ALIENS];
    double[] alienAngle = new double[MAX_ALIENS];
    int alienCount = 0;

    // render scales (adjust per level)
    double alienRenderScale = 0.1; // level 1 default (used in glScaled and collision)
    final double bulletRenderScale = 0.03;

    // game level: 1 or 2 only
    int level = 1;

    // flag to ensure win window shown only once
    boolean winShown = false;

    // game over flag
    boolean gameOverShown = false;

    boolean up=false, down=false, left=false, right=false;
    double moveSpeed = 0.01;

    int bulletsToShoot = 15;
    int bulletsShot = 0;
    int fireCounter = 0;

    // ---- Time tracking (ms) ----
    long level1StartTime = 0L;
    long level2StartTime = 0L;
    long level1Time = 0L; // duration in ms (recorded when leaving level1)
    long level2Time = 0L; // duration in ms (recorded when finishing level2)

    // GLUT for drawing bitmap text
    private GLUT glut = new GLUT();

    // ----- Health system -----
    int healthMax = 100;
    int health = healthMax;
    int hitCooldownFrames = 0;        // frames of invulnerability after a hit
    final int HIT_COOLDOWN_MAX = 60;  // ~1 second at 60fps

    // PowerBar fill direction:
    // false => RIGHT-to-LEFT fill (i.e. anchored at right, shrinks leftwards)
    boolean powerBarLeftToRight = false;

    // ----- Hit visual effect (flash) -----
    int flashFrames = 0;
    final int FLASH_MAX = 18; // frames of red flash

    // ----- Screen shake -----
    int shakeFrames = 0;
    final int SHAKE_MAX = 16;       // frames of shake when hit (tuneable)
    final double SHAKE_MAG = 0.035; // maximum shake magnitude in NDC coordinates (tuneable)

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

        // initialize level 1 by default (also starts level1 timer)
        resetGame();
    }

    /**
     * إعادة تهيئة اللعبة للمستوى 1 (restart).
     * synchronized عشان نقلل مشاكل التزامن بين Swing EDT و GL thread.
     */
    public synchronized void resetGame(){
        // reset to level 1 default
        level = 1;
        alienRenderScale = 0.1; // size normal for level 1

        // reset player
        ManTranslateXValue = 0;
        ManTranslateYValue = 0;
        rotateAngleValue = 0;
        control = 1;
        facingX = 0; facingY = 1;
        up = down = left = right = false;
        animCounter = 0;

        // reset bullets
        bulletsCount = 0;
        bulletsShot = 0;
        shotCooldown = 0;
        fire = false;

        // reset aliens for level 1
        alienCount = 20;
        initAliens(alienCount, 0.0012, 0.0006);

        // reset times: start level1 timer now
        level1StartTime = System.currentTimeMillis();
        level1Time = 0L;
        level2StartTime = 0L;
        level2Time = 0L;

        // reset health (ensure Level1 defaults)
        healthMax = 100;
        health = healthMax;
        hitCooldownFrames = 0;
        gameOverShown = false;

        // reset effects
        flashFrames = 0;
        shakeFrames = 0;

        // reset win flag so dialog can show again next time
        winShown = false;
    }

    /**
     * انتقل للمستوى التالي (level 2). يقلّل حجم الفضائي للنص، يزيد العدد ويزيد السرعة.
     */
    public synchronized void goToNextLevel(){
        if(level != 1) return; // فقط من level1 الى level2
        // record level1 elapsed time up to this moment
        level1Time = System.currentTimeMillis() - level1StartTime;

        level = 2;
        alienRenderScale = 0.05; // نص الحجم
        alienCount = 60; // عدد أكبر
        // أسرع: نزوّد السرعات الأساسية
        initAliens(alienCount, 0.0025, 0.0015); // base + random range
        // reset bullets and shot state but keep player pos
        bulletsCount = 0;
        bulletsShot = 0;
        shotCooldown = 0;
        fire = false;

        // start level2 timer now
        level2StartTime = System.currentTimeMillis();
        level2Time = 0L;

        winShown = false;

        // *** Set Level2 health to 300/300 as requested previously ***
        healthMax = 300;
        health = healthMax;

        // reset effects
        flashFrames = 0;
        shakeFrames = 0;
    }

    // helper to initialize aliens with given base speed and random range
    private void initAliens(int count, double baseSpeed, double randRange){
        int zones = 5;
        double zoneWidth = 2.0 / zones;
        for(int i = 0; i < count && i < MAX_ALIENS; i++){
            int zoneIndex = i % zones;
            double minX = -1 + zoneIndex * zoneWidth;
            double maxX = minX + zoneWidth;
            double worldX = minX + Math.random() * (maxX - minX); // this is in world coords (-1..1)
            double worldY = Math.random() < 0.5 ? 1.2 + Math.random() * 0.2 : -1.2 - Math.random() * 0.2;

            // STORE positions in stored-units so that drawing does: world = alienRenderScale * stored
            // stored = world / alienRenderScale
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

        // countdown shot cooldown each frame
        if(shotCooldown > 0) shotCooldown--;

        // cooldown for hit invulnerability
        if(hitCooldownFrames > 0) hitCooldownFrames--;

        updateManPositionAndAngle();

        // --- Apply screen shake to the world-rendering (background, man, bullets, aliens)
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

        gl.glPopMatrix(); // stop shaking; HUD & flash drawn without shake

        // draw hit flash overlay (if any) ON TOP of game objects (so player sees the hit)
        if(flashFrames > 0){
            drawHitFlash(gl);
            flashFrames--;
        }

        // draw HUD (inside the GL frame) top-right, white, using GLUT
        drawHud(gl);

        // check win condition AFTER aliens updated/removed
        if (alienCount == 0 && !winShown && !gameOverShown) {
            winShown = true;
            // احسب زمن المستوى المناسب
            if (level == 1) {
                level1Time = System.currentTimeMillis() - level1StartTime;
            } else {
                level2Time = System.currentTimeMillis() - level2StartTime;
            }
            SwingUtilities.invokeLater(this::showWinMenu);
        }


        if (health <= 0 && !gameOverShown) {
            // اضبط العلم أولًا عشان نقلل احتمالية فتح نوافذ مكررة
            gameOverShown = true;
            // اطلب من الـ EDT عرض النافذة المجمّعة
            SwingUtilities.invokeLater(this::showGameOverMenu);
        }

    }

    /**
     * Draw the red full-screen flash when hit.
     */
    private void drawHitFlash(GL gl){
        float alpha = (float)flashFrames / (float)FLASH_MAX * 0.9f; // max 0.9 alpha for stronger flash
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

    /**
     * Draw the in-game HUD (top-right). Uses GLUT bitmap string.
     * Stops updating times when winShown == true (so time freezes as soon as win window appears).
     * Also draws a larger health bar at the top-right which shrinks with HP.
     * Uses PowerBar.png (last texture) as the fill texture; now configured to fill based on powerBarLeftToRight flag.
     */
    private void drawHud(GL gl){
        long now = System.currentTimeMillis();

        // set color white (used for text)
        gl.glColor3f(1f,1f,1f);

        // We'll draw in normalized device coordinates (-1..1)
        // Save projection & modelview
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        // ----- Draw health bar (top-right) -----
        // top-right positioning and larger size
        float hbRight = 0.98f;               // near right edge
        float hbTop = 0.92f;                 // near top edge
        float hbWidth = 0.60f;               // wider
        float hbHeight = 0.08f;              // taller

        // compute left from right
        float hbLeft = hbRight - hbWidth;

        double healthRatio = Math.max(0, Math.min(1.0, (double)health / (double)healthMax));

        // background (dark, semi-transparent)
        gl.glColor4f(0f, 0f, 0f, 0.6f);
        gl.glBegin(GL.GL_QUADS);
        gl.glVertex2f(hbLeft, hbTop);
        gl.glVertex2f(hbLeft, hbTop - hbHeight);
        gl.glVertex2f(hbLeft + hbWidth, hbTop - hbHeight);
        gl.glVertex2f(hbLeft + hbWidth, hbTop);
        gl.glEnd();

        // If PowerBar texture loaded, draw textured fill.
        int powerBarTexIndex = textureIndex[textureIndex.length - 1]; // last one (PowerBar.png)
        if(powerBarLeftToRight){
            // Fill anchored at left and extending to the right proportionally to healthRatio
            if(healthRatio > 0.0){
                float padding = 0.006f;
                float fillLeft = hbLeft + padding;
                float fillRight = (float)(hbLeft + padding + (hbWidth - 2*padding) * healthRatio);
                float fillTop = hbTop - 0.006f;
                float fillBottom = hbTop - hbHeight + 0.006f;

                gl.glEnable(GL.GL_BLEND);
                gl.glBindTexture(GL.GL_TEXTURE_2D, powerBarTexIndex);
                gl.glColor3f(1f,1f,1f); // ensure texture color not tinted
                gl.glBegin(GL.GL_QUADS);
                // left-top
                gl.glTexCoord2f(0.0f, 1.0f); gl.glVertex2f(fillLeft, fillTop);
                // left-bottom
                gl.glTexCoord2f(0.0f, 0.0f); gl.glVertex2f(fillLeft, fillBottom);
                // right-bottom (S coordinate equals healthRatio so texture is cropped on the right)
                gl.glTexCoord2f((float)healthRatio, 0.0f); gl.glVertex2f(fillRight, fillBottom);
                // right-top
                gl.glTexCoord2f((float)healthRatio, 1.0f); gl.glVertex2f(fillRight, fillTop);
                gl.glEnd();
                gl.glDisable(GL.GL_BLEND);
            }
        } else {
            // Anchor at RIGHT and shrink leftwards (this is the requested reversed direction)
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
                // left-top (texcoords scaled so the rightmost maps to S=1)
                gl.glTexCoord2f((float)(1.0 - healthRatio), 1.0f); gl.glVertex2f(fillLeft, fillTop);
                // left-bottom
                gl.glTexCoord2f((float)(1.0 - healthRatio), 0.0f); gl.glVertex2f(fillLeft, fillBottom);
                // right-bottom
                gl.glTexCoord2f(1.0f, 0.0f); gl.glVertex2f(fillRight, fillBottom);
                // right-top
                gl.glTexCoord2f(1.0f, 1.0f); gl.glVertex2f(fillRight, fillTop);
                gl.glEnd();
                gl.glDisable(GL.GL_BLEND);
            }
        }

        // border (white)
        gl.glColor3f(1f,1f,1f);
        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex2f(hbLeft, hbTop);
        gl.glVertex2f(hbLeft, hbTop - hbHeight);
        gl.glVertex2f(hbLeft + hbWidth, hbTop - hbHeight);
        gl.glVertex2f(hbLeft + hbWidth, hbTop);
        gl.glEnd();

        // health text inside bar (centered-ish)
        String hpText = "HP: " + health + " / " + healthMax;
        gl.glRasterPos2f(hbLeft + 0.02f, hbTop - hbHeight/2f - 0.01f);
        glut.glutBitmapString(GLUT.BITMAP_HELVETICA_12, hpText);

        // Apply scale to make bitmap text larger for timers
        gl.glScalef(1.4f, 1.4f, 1f);

        if(level == 1){
            long elapsed;
            if(winShown){
                // freeze at recorded level1Time
                elapsed = level1Time;
            } else {
                elapsed = (level1StartTime > 0) ? (now - level1StartTime) : 0L;
            }
            String text = "Level 1  " + formatTime(elapsed);

            // draw single line near top-left (adjusted coords due to scale)
            gl.glRasterPos2f(-0.70f, 0.60f);
            glut.glutBitmapString(GLUT.BITMAP_HELVETICA_18, text);
        } else {
            long lvl1 = level1Time; // recorded when entering level2
            long lvl2Current;
            if(winShown){
                lvl2Current = level2Time;
            } else {
                lvl2Current = (level2StartTime > 0) ? (now - level2StartTime) : 0L;
            }
            long total = lvl1 + lvl2Current;

            // draw three lines with a bigger vertical gap
            gl.glRasterPos2f(-0.70f, 0.60f);
            glut.glutBitmapString(GLUT.BITMAP_HELVETICA_18, "Lvl1: " + formatTime(lvl1));

            gl.glRasterPos2f(-0.70f, 0.50f);
            glut.glutBitmapString(GLUT.BITMAP_HELVETICA_18, "Lvl2: " + formatTime(lvl2Current));

            gl.glRasterPos2f(-0.70f, 0.40f);
            glut.glutBitmapString(GLUT.BITMAP_HELVETICA_18, "Total: " + formatTime(total));
        }
        // --- NEW: Draw "Tap ESC To Menu" bottom-right ---
        gl.glLoadIdentity();
        gl.glColor3f(1f,1f,1f);  // white text

        gl.glRasterPos2f(-0.90f, -0.90f);
        glut.glutBitmapString(GLUT.BITMAP_HELVETICA_18, "Tap ESC To Menu");

        // restore matrices
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPopMatrix();

        // reset color
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
        // If player dead, don't draw the man (disappear on death)
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

            // animation sequence 1->2->3->4->1...
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

                    // increased bullet speed from 0.05 -> 0.08 for a noticeable but not extreme change
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

        // update & draw bullets
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

        // Cleanup bullets that go far away
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
        // Update aliens with wandering behavior and bounce inside frame bounds:
        // We will compute stored bounds depending on level:
        double storedMin, storedMax;
        if(level == 1){
            // preserve previous behavior exactly (stored units)
            storedMin = -9.5;
            storedMax = 9.5;
        } else {
            // level 2: world bounds must be -1..1 -> convert to stored units
            storedMin = -1.0 / alienRenderScale;
            storedMax =  1.0 / alienRenderScale;
        }

        double invScale = 1.0 / alienRenderScale; // used to convert world-delta -> stored-delta

        for(int i = 0; i < alienCount; i++){
            // small random turn (less jitter)
            alienAngle[i] += (Math.random() - 0.5) * 0.15;
            // compute movement in world-space (so speeds behave consistently across levels)
            double worldDx = Math.cos(alienAngle[i]) * alienSpeed[i];
            double worldDy = Math.sin(alienAngle[i]) * alienSpeed[i];

            // convert world-delta to stored-units and apply
            alienX[i] += worldDx * invScale;
            alienY[i] += worldDy * invScale;

            // bounce on walls — reflect angle and clamp position so it doesn't go outside (walls = frame bounds)
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

        // draw aliens using current alienRenderScale
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

        // collisions: bullets vs aliens, using screen-space positions derived from render scales
        double collisionThreshold = Math.max(0.12, alienRenderScale * 1.2); // tuneable

        for(int ai = alienCount - 1; ai >= 0; ai--){
            boolean alienRemoved = false;
            for(int bi = bulletsCount - 1; bi >= 0; bi--){ // iterate backwards so removals safe
                // compute screen-space centers
                double bulletScreenX = bulletRenderScale * bulletsX[bi];
                double bulletScreenY = bulletRenderScale * bulletsY[bi];
                double alienScreenX = alienRenderScale * alienX[ai];
                double alienScreenY = alienRenderScale * alienY[ai];

                double dx = bulletScreenX - alienScreenX;
                double dy = bulletScreenY - alienScreenY;
                if(Math.sqrt(dx*dx + dy*dy) < collisionThreshold){
                    // remove alien ai by shifting later elements left
                    for(int k = ai; k < alienCount - 1; k++){
                        alienX[k] = alienX[k+1];
                        alienY[k] = alienY[k+1];
                        alienSpeed[k] = alienSpeed[k+1];
                        alienAngle[k] = alienAngle[k+1];
                    }
                    alienCount--;

                    // remove only the bullet bi by shifting later bullets left
                    for(int k = bi; k < bulletsCount - 1; k++){
                        bulletsX[k] = bulletsX[k+1];
                        bulletsY[k] = bulletsY[k+1];
                        bulletsSpeedX[k] = bulletsSpeedX[k+1];
                        bulletsSpeedY[k] = bulletsSpeedY[k+1];
                    }
                    bulletsCount--;

                    alienRemoved = true;
                    break; // break bullets loop for this alien (we removed it)
                }
            }
            if(alienRemoved){
                // alien removed; continue with next ai (already shifted)
            }
        }

        // --- PLAYER <-> ALIEN COLLISIONS (damage) ---
        // compute player screen position (because DrawMan uses glScaled(0.1) then glTranslated)
        double manScreenX = 0.1 * ManTranslateXValue;
        double manScreenY = 0.1 * ManTranslateYValue;
        double playerCollisionRadius = 0.12; // tuneable

        for(int i = alienCount - 1; i >= 0; i--){
            double alienScreenX = alienRenderScale * alienX[i];
            double alienScreenY = alienRenderScale * alienY[i];
            double dx = alienScreenX - manScreenX;
            double dy = alienScreenY - manScreenY;
            double dist = Math.sqrt(dx*dx + dy*dy);

            if(dist < playerCollisionRadius){
                // hit detected
                if(hitCooldownFrames == 0 && health > 0 && !gameOverShown){
                    health -= 10; // each touch reduces 10 HP (tuneable)
                    if(health < 0) health = 0;
                    hitCooldownFrames = HIT_COOLDOWN_MAX;

                    // trigger visual flash
                    flashFrames = FLASH_MAX;

                    // trigger screen shake
                    shakeFrames = SHAKE_MAX;

                    // optional: push alien away a bit to avoid immediate repeated collisions
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
        pauseWindow.setBackground(new Color(0, 0, 0, 0)); // شفاف

        JPanel container = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(new Color(30, 30, 30, 200)); // خلفية شفافة
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

        // Label + Slider
        gbc.gridy = 1;
        gbc.gridwidth = 1;

        JLabel volumeLabel = new JLabel("Volume:");
        volumeLabel.setForeground(Color.WHITE);
        container.add(volumeLabel, gbc);

        JSlider volumeSlider = new JSlider(0, 100, (int)(volume * 100));
        volumeSlider.setOpaque(false);
        volumeSlider.setBackground(new Color(0, 0, 0, 0));

        // set initial slider value from global sound manager (in case app used slider elsewhere)
        volumeSlider.setValue(Math.round(Sound.SoundManager.getGlobalVolume() * 100f));

        volumeSlider.addChangeListener(e -> {
            float v = volumeSlider.getValue() / 100f;
            // حدّث المتغيّر المحلي (لو تستخدمه داخل كلاس الـ listener)
            volume = v;

            // أطّلع في الكونسول (اختياري للتجربة)
            System.out.println("Current Volume = " + v);

            // فعّل الصوت فعلاً عن طريق SoundManager (يجب أن تكون الدالة موجودة في كلاسك)
            try {
                Sound.SoundManager.setGlobalVolume(v);
            } catch (Throwable ex) {
                // لا تجعل الخطأ يوقف اللعبة — بس اطبع للاختبار
                System.err.println("setGlobalVolume failed: " + ex.getMessage());
            }

            // اختياري: شغّل صوت feedback بسيط عند الإفلات من السحب
            if (!volumeSlider.getValueIsAdjusting()) {
                try {
                    // ضع ملف صغير click.wav داخل Assets اذا حابب
                    //Sound.SoundManager.playOnce("Assets/click.wav");
                } catch (Throwable ignore) { }
            }
        });


        gbc.gridx = 1;
        container.add(volumeSlider, gbc);

        // Resume Button
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

        // Back to menu
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

        Color base = new Color(0xC2A878);   // صحراوي
        Color hover = new Color(0xD9C7A6);  // افتح صحراوي
        Color border = new Color(0x7A6240); // بني غامق
        Color text = new Color(0x3B2F1B);   // بني داكن

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
    // تستدعى من الـ EDT (SwingUtilities.invokeLater)
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
        pauseWindow.setBackground(new Color(0,0,0,0)); // شفاف

        JPanel container = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // نفس خلفية الـ Pause (غامق وشفاف) مع زوايا مدورة
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

        // Title
        JLabel title = new JLabel("Game Over", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 26));
        title.setForeground(Color.WHITE);
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        container.add(title, gbc);

        // message (optional)
        gbc.gridy = 1;
        JLabel msg = new JLabel("<html><center>You died. Try again!</center></html>", JLabel.CENTER);
        msg.setFont(new Font("Arial", Font.PLAIN, 14));
        msg.setForeground(Color.LIGHT_GRAY);
        container.add(msg, gbc);

        // Restart button
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        JButton restartBtn = new JButton("Restart");
        restartBtn.setFocusable(false);
        restartBtn.setRequestFocusEnabled(false);
        styleButton(restartBtn); // نستخدم نفس دالة الستايل اللي عندك
        restartBtn.addActionListener(e -> {
            // اغلق النافذة واعمل reset للعبة
            pauseWindow.dispose();
            pauseWindow = null;
            // resetGame() مضبوط synchronized في كودك
            resetGame();
            paused = false;
            gameOverShown = false;
        });
        container.add(restartBtn, gbc);

        // Back to menu
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
                gamePanel.returnToMenu(); // صح — GamePanel يتعامل مع الـ parent داخلياً
            }
        });

        container.add(backBtn, gbc);

        pauseWindow.setVisible(true);
    }
    private void showWinMenu() {

        if (pauseWindow != null) return;

        winShown = true;
        paused = true;

        // حساب الوقت لو مش متسجل
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
        pauseWindow.setBackground(new Color(0,0,0,0)); // شفاف

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

        // ---------------------- Title ----------------------
        JLabel title = new JLabel("You Win!", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 26));
        title.setForeground(Color.WHITE);
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        container.add(title, gbc);

        // ---------------------- Time Text ----------------------
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

        // ------------------------------------------------------------
        //             BUTTONS (Up, Restart, Back)
        // ------------------------------------------------------------

        // ========== 1) Up To Next Level (فوق) ==========
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

                // حفظ وقت level 1 لو مش متسجل
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


        // ========== 2) Restart (في النص) ==========
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


        // ========== 3) Back to Menu (تحت) ==========
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

