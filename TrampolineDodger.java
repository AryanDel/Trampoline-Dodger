import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Random;


public class TrampolineDodger extends JPanel implements ActionListener, KeyListener {

    // 1. BASIC SETTINGS
    final int WIDTH = 900;
    final int HEIGHT = 600;
    final int FLOOR = 515;
    final Random random = new Random();

    // 2. PLAYER VARIABLES
    double playerX = 450;
    double playerY = 250;
    double speedY = 0;
    boolean left = false;
    boolean right = false;
    boolean up = false;
    boolean down = false;
    int lives = 5;
    int safeTime = 0;      // Temporary protection after getting hit
    int wingsTime = 0;     // How long the wings last

    // 3. GAME VARIABLES
    int score = 0;
    int level = 1;
    boolean started = false;
    boolean gameOver = false;
    boolean won = false;
    boolean paused = false;

    // Each projectile is one simple object with four values.
    static class Projectile {
        int x;
        int y;
        int speed;
        int type;          // 0 = red bullet, 1 = fireball, 2 = basketball
        double speedY;    // Only basketball uses vertical speed

        Projectile(int kind) {
            type = kind;
        }
    }

    Projectile spike = new Projectile(0);
    Projectile fireball = new Projectile(1);
    Projectile blueSpike = new Projectile(2);

    // 4. MYSTERY BOX (ALWAYS GIVES WINGS)
    int giftX = 400;
    int giftY = -50;
    int giftWait = 400;

    // 5. FINAL UFO BOSS
    double bossX = 450;
    int bossDirection = 1;
    int bossTime = 0;
    int bossHits = 0;
    int laserX = 450;
    int laserY = -100;
    int laserWait = 100;
    final int BOSS_Y = 325;

    // 6. YOUR CUSTOM ARTWORK
    Image face1 = loadPicture("player-level1.png");
    Image face2 = loadPicture("player-level2.png");
    Image face3 = loadPicture("player-level3.png");
    Image face4 = loadPicture("player-level4.png");
    Image fireImage = loadPicture("fireball.png");
    Image ufoImage = loadPicture("ufo-shield.png");
    Image background2 = loadPicture("background-level2.png");
    Image background3 = loadPicture("background-level3.png");
    Image background4 = loadPicture("background-level4.png");
    Image trampolineImage = loadPicture("trampoline.png");
    Image bulletImage = loadPicture("bullet-red.png");
    Image basketballImage = loadPicture("basketball-blue.png");

    Image loadPicture(String filename) {
        File file = new File("assets/" + filename);
        if (file.exists()) {
            return new ImageIcon(file.getPath()).getImage();
        }
        return null; // Game still runs even if an image is missing.
    }

    public TrampolineDodger() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addKeyListener(this);
        resetProjectile(spike);
        resetProjectile(fireball);
        resetProjectile(blueSpike);
        new Timer(16, this).start(); // About 60 updates per second
    }

    void resetProjectile(Projectile p) {
        p.x = WIDTH + 50 + random.nextInt(250);
        p.speed = 4 + level + random.nextInt(3);

        if (p.type == 2) { // Basketball begins above the floor, moving diagonally.
            p.y = 130 + random.nextInt(140);
            p.speedY = 3 + random.nextInt(3);
        } else {
            p.y = 110 + random.nextInt(300);
            p.speedY = 0;
        }
    }

    void startGame() {
        score = 0;
        level = 1;
        lives = 5;
        safeTime = 0;
        wingsTime = 0;
        playerX = 450;
        playerY = 250;
        speedY = 0;
        bossX = 450;
        bossTime = 0;
        bossHits = 0;
        laserY = -100;
        laserWait = 100;
        giftWait = 400;
        giftY = -50;
        resetProjectile(spike);
        resetProjectile(fireball);
        resetProjectile(blueSpike);
        started = true;
        gameOver = false;
        won = false;
        paused = false;
        requestFocusInWindow();
    }

    // 7. THE GAME LOOP: called repeatedly by the timer.
    public void actionPerformed(ActionEvent event) {
        if (started && !gameOver && !paused) {
            updateGame();
        }
        repaint();
    }

    void updateGame() {
        score++; // Faster level progression, useful for a short demo.

        if (level == 1 && score >= 1000) changeLevel(2);
        if (level == 2 && score >= 2000) changeLevel(3);
        if (level == 3 && score >= 3000) changeLevel(4);

        if (safeTime > 0) safeTime--;
        if (wingsTime > 0) wingsTime--;

        movePlayer();
        moveProjectile(spike);
        if (level >= 2) moveProjectile(fireball);
        if (level >= 3) moveProjectile(blueSpike);
        updateGift();
        if (level == 4) updateBoss();
    }

    void changeLevel(int nextLevel) {
        level = nextLevel;
        playerX = 450;
        playerY = 250;
        speedY = 0;
        safeTime = 60;
        resetProjectile(spike);
        resetProjectile(fireball);
        resetProjectile(blueSpike);
    }

    // 8. MOVING AND BOUNCING
    void movePlayer() {
        if (left) playerX -= 6;
        if (right) playerX += 6;

        // Gravity makes the player fall faster each frame.
        speedY += 0.48;
        if (up && speedY > 0) speedY -= 0.35; // Hover / slow falling
        if (down) speedY += 0.35;              // Fall faster
        if (wingsTime > 0 && up) speedY = -5;  // Fly with wings
        if (speedY > 12) speedY = 12;

        playerY += speedY;

        if (playerX < 23) playerX = 23;
        if (playerX > WIDTH - 23) playerX = WIDTH - 23;
        if (playerY < 25) {
            playerY = 25;
            speedY = 0;
        }

        if (playerY + 22 >= FLOOR) {
            if (level == 3 && overLavaGap()) {
                loseLife();
                playerX = 450;
                playerY = 300;
                speedY = -10;
            } else {
                playerY = FLOOR - 22;
                speedY = -15.5; // Negative velocity makes us bounce UP.
            }
        }

        // Level 2 has dangerous castle walls.
        if (level == 2 && playerY > 180 && playerY < 355) {
            if (playerX < 30 || playerX > WIDTH - 30) loseLife();
        }
    }

    boolean overLavaGap() {
        // Four 80-pixel holes, with equal safe sections between them.
        boolean gap1 = playerX > 116 && playerX < 196;
        boolean gap2 = playerX > 312 && playerX < 392;
        boolean gap3 = playerX > 508 && playerX < 588;
        boolean gap4 = playerX > 704 && playerX < 784;
        return gap1 || gap2 || gap3 || gap4;
    }

    // 9. PROJECTILES AND COLLISION DETECTION
    void moveProjectile(Projectile p) {
        p.x -= p.speed; // All projectiles travel left.

        if (p.type == 2) { // Basketball falls and bounces while moving left.
            p.y += (int) p.speedY;
            p.speedY += 0.5; // Gravity
            if (p.y >= FLOOR - 20) {
                p.y = FLOOR - 20;
                p.speedY = -10; // Negative speed sends it back upward.
            }
        }

        if (p.x < -50) {
            resetProjectile(p);
            score += 10;
        }

        // Close enough in X and Y means a hit.
        if (Math.abs(playerX - p.x) < 32 &&
            Math.abs(playerY - p.y) < 32) {
            loseLife();
            resetProjectile(p);
        }
    }

    void loseLife() {
        if (safeTime == 0) {
            lives--;
            safeTime = 90;
            if (lives <= 0) gameOver = true;
        }
    }

    // 10. SIMPLE POWER-UP: CATCH THE ? BOX FOR WINGS
    void updateGift() {
        if (giftWait > 0) {
            giftWait--;
            return;
        }
        giftY += 2;

        if (Math.abs(playerX - giftX) < 35 &&
            Math.abs(playerY - giftY) < 35) {
            wingsTime = 350;
            giftY = -50;
            giftWait = 500;
        }
        if (giftY > FLOOR) {
            giftX = 100 + random.nextInt(700);
            giftY = -50;
            giftWait = 500;
        }
    }

    // 11. FINAL BOSS: A MOVING UFO WITH ONE LASER
    void updateBoss() {
        bossTime++;
        bossX += 2 * bossDirection;
        if (bossX > 650 || bossX < 250) bossDirection = -bossDirection;

        boolean shieldDown = bossTime % 420 >= 270;

        if (laserWait > 0) laserWait--;
        if (laserWait == 0 && laserY < 0) {
            laserX = (int) bossX;
            laserY = BOSS_Y + 45;
        }
        if (laserY >= 0) {
            laserY += 8;
            if (Math.abs(playerX - laserX) < 20 &&
                Math.abs(playerY - laserY) < 28) {
                loseLife();
                laserY = -100;
                laserWait = 75;
            }
            if (laserY > FLOOR) {
                laserY = -100;
                laserWait = 75;
            }
        }

        // Jump ABOVE the UFO, then land on it while its shield is down.
        boolean overBoss = Math.abs(playerX - bossX) < 85;
        boolean touchingTop = playerY + 22 >= BOSS_Y - 43 &&
                              playerY + 22 <= BOSS_Y - 30;
        if (overBoss && touchingTop && speedY > 0 && shieldDown) {
            bossHits++;
            speedY = -15.5;
            bossTime = 0;
            laserY = -100;
            laserWait = 100;
            if (bossHits == 3) {
                won = true;
                gameOver = true;
            }
        }
    }

    // 12. DRAW EVERYTHING ON THE SCREEN
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawBackground(g);
        drawPlayer(g);
        drawProjectile(g, spike);
        if (level >= 2) drawProjectile(g, fireball);
        if (level >= 3) drawProjectile(g, blueSpike);
        drawGift(g);
        if (level == 4) drawBoss(g);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.drawString("SCORE: " + score, 20, 30);
        g.drawString("LIVES: " + lives, 220, 30);
        g.drawString("LEVEL: " + level + " / 4", 365, 30);
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.drawString("A/D or arrows: move    UP: hover    DOWN: fall faster    P: pause", 20, 580);
        if (wingsTime > 0) g.drawString("WINGS! Hold UP to fly", 20, 55);

        if (!started) drawMessage(g, "TRAMPOLINE DODGER", "Press SPACE to play");
        if (paused) drawMessage(g, "PAUSED", "Press P to continue");
        if (gameOver && !won) drawMessage(g, "GAME OVER", "Press SPACE to restart");
        if (won) drawMessage(g, "YOU BEAT THE UFO!", "Press SPACE to play again");
    }

    void drawBackground(Graphics g) {
        // 1. Draw the level background first.
        if (level == 1) g.setColor(new Color(25, 50, 90));
        if (level == 2) g.setColor(new Color(68, 35, 112));
        if (level == 3) g.setColor(new Color(105, 43, 26));
        if (level == 4) g.setColor(new Color(14, 13, 52));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        if (level == 2 && background2 != null)
            g.drawImage(background2, 0, 0, WIDTH, HEIGHT, this);
        if (level == 3 && background3 != null)
            g.drawImage(background3, 0, 0, WIDTH, HEIGHT, this);
        if (level == 4 && background4 != null)
            g.drawImage(background4, 0, 0, WIDTH, HEIGHT, this);

        // 2. Draw the floor over the background.
        g.setColor(new Color(25, 25, 35, 195));
        g.fillRect(0, FLOOR + 12, WIDTH, HEIGHT - FLOOR);

        if (level == 3) {
            // Five green safe platforms separated by four lava holes.
            drawTrampoline(g, 0, 116);
            drawTrampoline(g, 196, 116);
            drawTrampoline(g, 392, 116);
            drawTrampoline(g, 588, 116);
            drawTrampoline(g, 784, 116);

            g.setColor(Color.ORANGE);
            g.fillRect(116, FLOOR, 80, 60);
            g.fillRect(312, FLOOR, 80, 60);
            g.fillRect(508, FLOOR, 80, 60);
            g.fillRect(704, FLOOR, 80, 60);
        } else {
            drawTrampoline(g, 80, WIDTH - 160);
        }

        // Level 2's castle walls are still dangerous.
        if (level == 2) {
            g.setColor(Color.RED);
            g.fillRect(0, 180, 15, 175);
            g.fillRect(WIDTH - 15, 180, 15, 175);
        }
    }

    void drawTrampoline(Graphics g, int x, int width) {
        if (trampolineImage != null) {
            g.drawImage(trampolineImage, x, FLOOR - 22, width, 68, this);
        } else {
            g.setColor(Color.GREEN);
            g.fillRect(x, FLOOR, width, 12);
        }
    }

    void drawPlayer(Graphics g) {
        // Wings are drawn behind the face, so the face stays visible.
        if (wingsTime > 0) {
            g.setColor(Color.CYAN);
            g.fillOval((int) playerX - 40, (int) playerY - 16, 30, 30);
            g.fillOval((int) playerX + 10, (int) playerY - 16, 30, 30);
        }

        Image face = face1;
        if (level == 2) face = face2;
        if (level == 3) face = face3;
        if (level == 4) face = face4;

        if (face != null) {
            g.drawImage(face, (int) playerX - 25, (int) playerY - 25, 50, 50, this);
        } else {
            g.setColor(Color.CYAN);
            g.fillOval((int) playerX - 23, (int) playerY - 23, 46, 46);
        }
    }

    void drawProjectile(Graphics g, Projectile p) {
        if (p.type == 0 && bulletImage != null) {
            g.drawImage(bulletImage, p.x - 28, p.y - 15, 56, 30, this);
        } else if (p.type == 1 && fireImage != null) {
            g.drawImage(fireImage, p.x - 30, p.y - 30, 60, 60, this);
        } else if (p.type == 2 && basketballImage != null) {
            g.drawImage(basketballImage, p.x - 20, p.y - 20, 40, 40, this);
        } else {
            if (p.type == 2) g.setColor(Color.BLUE);
            else g.setColor(Color.RED);
            g.fillOval(p.x - 19, p.y - 19, 38, 38);
        }
    }

    void drawGift(Graphics g) {
        if (giftWait > 0) return;
        g.setColor(Color.YELLOW);
        g.fillRect(giftX - 18, giftY - 18, 36, 36);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 25));
        g.drawString("?", giftX - 7, giftY + 9);
    }

    void drawBoss(Graphics g) {
        boolean shieldDown = bossTime % 420 >= 270;
        if (ufoImage != null) {
            g.drawImage(ufoImage, (int) bossX - 94, BOSS_Y - 65, 188, 135, this);
        } else {
            g.setColor(Color.LIGHT_GRAY);
            g.fillOval((int) bossX - 90, BOSS_Y - 40, 180, 80);
        }
        g.setColor(shieldDown ? Color.GREEN : Color.RED);
        g.drawRect((int) bossX - 96, BOSS_Y - 65, 192, 135);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString(shieldDown ? "SHIELD DOWN - JUMP ON UFO!" : "SHIELD UP", 570, 85);
        g.setColor(Color.WHITE);
        g.drawString("UFO HITS: " + bossHits + " / 3", 570, 105);
        if (laserY >= 0) {
            g.setColor(Color.PINK);
            g.fillRect(laserX - 7, laserY - 22, 14, 44);
        }
    }

    void drawMessage(Graphics g, String title, String subtitle) {
        g.setColor(new Color(0, 0, 0, 210));
        g.fillRect(0, 150, WIDTH, 250);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.drawString(title, 220, 255);
        g.setFont(new Font("Arial", Font.BOLD, 22));
        g.drawString(subtitle, 270, 300);
    }

    // 13. KEYBOARD INPUT
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_SPACE && (!started || gameOver)) startGame();
        if (key == KeyEvent.VK_P && started && !gameOver) paused = !paused;
        if (key == KeyEvent.VK_A || key == KeyEvent.VK_LEFT) left = true;
        if (key == KeyEvent.VK_D || key == KeyEvent.VK_RIGHT) right = true;
        if (key == KeyEvent.VK_W || key == KeyEvent.VK_UP) up = true;
        if (key == KeyEvent.VK_S || key == KeyEvent.VK_DOWN) down = true;
    }

    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_A || key == KeyEvent.VK_LEFT) left = false;
        if (key == KeyEvent.VK_D || key == KeyEvent.VK_RIGHT) right = false;
        if (key == KeyEvent.VK_W || key == KeyEvent.VK_UP) up = false;
        if (key == KeyEvent.VK_S || key == KeyEvent.VK_DOWN) down = false;
    }

    public void keyTyped(KeyEvent e) { }

    // Java starts here. This creates the game window.
    public static void main(String[] args) {
        JFrame window = new JFrame("Trampoline Dodger - Easy Java Edition");
        TrampolineDodger game = new TrampolineDodger();
        window.add(game);
        window.pack();
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);
        window.setLocationRelativeTo(null);
        window.setVisible(true);
        game.requestFocusInWindow();
    }
}
