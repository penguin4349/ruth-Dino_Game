package com.example.ruth202305final;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import javafx.scene.*;
import javafx.scene.paint.*;
import javafx.scene.canvas.*;
import javafx.scene.text.Font;

import java.io.IOException;
import java.util.ArrayList;
import java.util.random.RandomGenerator;

public class DinoGame extends Application {
    // consts
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final double INITIAL_SPEED = 200.0;
    private static final double GROUND_HEIGHT = 153;
    private static final double MIN_OBSTACLE_CLEARANCE = 300;
    private static final double MAX_OBSTACLE_CLEARANCE = 800;
    private static final double MIN_COIN_CLEARANCE = 800;
    private static final double MAX_COIN_CLEARANCE = 2400;

    // assets
    private static final Image BG_IMG = new Image("file:images/bkg.png", true);
    private static final Image MTN_IMG = new Image("file:images/mtns.png", true);
    private static final Image GROUND_IMG = new Image("file:images/grass.png", true);
    private static final Font normal_font = Font.loadFont("file:fonts/arcade_classic.ttf", 40);
    private static final Font large_font = Font.loadFont("file:fonts/arcade_classic.ttf", 120);


    // UI
    private Group root;
    private Scene scene;
    private Canvas canvas;
    private GraphicsContext gc;
    private AnimationTimer timer;

    // game state
    private double t = 0.0; // time
    private double speed = 1.0;
    private int score = 0;
    private double ground_x = 0.0;
    private double mtn_x = 0.0;
    private double obstacle_clearance = 0.0;
    private double coin_clearance = 240.0;
    private Boolean game_over = false;

    private final Player player = new Player();
    private final ArrayList<Obstacle> obstacles = new ArrayList<>();
    private final ArrayList<Coin> coins = new ArrayList<>();

    @Override
    public void start(Stage stage) throws IOException {
        root = new Group();
        scene = new Scene(root, WIDTH, HEIGHT, Color.WHITE);
        canvas = new Canvas(WIDTH, HEIGHT);
        gc = canvas.getGraphicsContext2D();

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if(!game_over){
                    update();
                }
                draw();
            }
        };

        timer.start();

        root.getChildren().add(canvas);

        scene.setOnKeyPressed(e -> {
            if(e.getCode() == KeyCode.SPACE) {
                if(!game_over){
                    player.jump();
                }else{
                    restart();
                }
            }
        });

        stage.setTitle("Dino Game");
        stage.setScene(scene);
        stage.show();
    }

    private void restart(){
        t = 0.0;
        speed = 1.0;
        score = 0;
        obstacles.clear();
        obstacle_clearance = 0.0;
        coins.clear();
        coin_clearance = 240.0;
        player.reset();
        game_over = false;
    }

    private void update(){
        // update time
        double dt = speed / 60.0;
        t += dt;
        // move floor
        update_floor(dt);
        // spawn entities
        update_spawn(dt);
        // update entities
        player.update(dt);
        obstacles.forEach(obstacle -> obstacle.update(dt));
        coins.forEach(coin -> coin.update(dt));
        // collide entities
        update_collision();
    }

    private void update_collision(){
        // check coins
        for (int i = coins.size()-1; i >= 0; i--){
            // despawn coins that player collected
            if (coins.get(i).collide(player)){
                coins.remove(i);
                score += 1000;
                speed *= 1.05;
                break;
            }
            // despawn coins that collided with obstacles
            for (int j = 0; j < obstacles.size(); j++){
                if (coins.get(i).collide(obstacles.get(j))){
                    coins.remove(i);
                    break;
                }
            }
        }
        // check if player hits obstacle
        obstacles.forEach(obstacle -> {
            if (player.collide(obstacle)){
                game_over = true;
            }
        });

    }

    private void update_spawn(double dt){
        // coins
        coin_clearance -= dt * INITIAL_SPEED;
        if (coin_clearance < 0) {
            coins.add(new Coin());
            coin_clearance += RandomGenerator.getDefault().nextDouble(MIN_COIN_CLEARANCE, MAX_COIN_CLEARANCE);
        }
        // obstacles
        obstacle_clearance -= dt * INITIAL_SPEED;
        if (obstacle_clearance < 0) {
            obstacles.add(new Obstacle());
            obstacle_clearance += RandomGenerator.getDefault().nextDouble(MIN_OBSTACLE_CLEARANCE, MAX_OBSTACLE_CLEARANCE);
        }
    }

    private void update_floor(double dt){
        ground_x -= dt * INITIAL_SPEED;
        if (ground_x < 0) ground_x += WIDTH;
        mtn_x -= dt * INITIAL_SPEED / 10;
        if (mtn_x < 0) mtn_x += WIDTH;
    }

    private void draw(){
        gc.clearRect(0, 0, WIDTH, HEIGHT);

        // draw background
        gc.drawImage(BG_IMG, 0, 0);
        gc.drawImage(MTN_IMG, mtn_x, HEIGHT - GROUND_HEIGHT - 134);
        gc.drawImage(MTN_IMG, mtn_x - WIDTH, HEIGHT - GROUND_HEIGHT - 134);
        gc.drawImage(MTN_IMG, mtn_x + WIDTH, HEIGHT - GROUND_HEIGHT - 134);
        gc.drawImage(GROUND_IMG, ground_x, HEIGHT - GROUND_HEIGHT);
        gc.drawImage(GROUND_IMG, ground_x - WIDTH, HEIGHT - GROUND_HEIGHT);
        gc.drawImage(GROUND_IMG, ground_x + WIDTH, HEIGHT - GROUND_HEIGHT);

        // draw entities
        player.draw(gc);
        obstacles.forEach(obstacle -> obstacle.draw(gc));
        coins.forEach(coin -> coin.draw(gc));

        // draw score
        if(!game_over) {
            gc.setFont(normal_font);
            gc.setFill(Color.WHITE);
            gc.fillText("SCORE   " + (score + Math.round(t * 50)), 10, 30);
        }else{
            gc.setFont(large_font);
            gc.setFill(Color.WHITE);
            gc.fillText("GAME    OVER", 100, 200);
            gc.setFont(normal_font);
            gc.fillText("SCORE   " + (score + Math.round(t * 50)), 100, 240);
            gc.fillText("PRESS   SPACE   TO   PLAY   AGAIN", 140, 540);
        }
    }

    public static void main(String[] args) {
        launch();
    }

    public abstract class Entity{
        public abstract double get_x();
        public abstract double get_y();
        public abstract double get_w();
        public abstract double get_h();
        public abstract CollisionShape get_collisionShape();

        abstract void update(double dt);
        abstract void draw(GraphicsContext gc);

        public enum CollisionShape{
            Box,
            Circle
        }

        public Boolean collide(Entity that){
            switch (this.get_collisionShape()){
                case Box -> {
                    switch (that.get_collisionShape()){
                        case Box -> {
                            return boxBoxCollide(this, that);
                        }
                        case Circle -> {
                            return circleBoxCollide(that, this);
                        }
                    }
                }
                case Circle -> {
                    switch (that.get_collisionShape()){
                        case Box -> {
                            return circleBoxCollide(this, that);
                        }
                        case Circle -> {
                            return circleCircleCollide(this, that);
                        }
                    }
                }
            }
            return false;
        }

        static Boolean circleCircleCollide(Entity c1, Entity c2){
            // get minimum separation between circles
            double separation = (c1.get_w() + c2.get_w()) / 2.0; // w is diameter
            // get circle center coordinates
            double x1 = c1.get_x() + c1.get_w() / 2.0; // w is diameter
            double x2 = c2.get_x() + c2.get_w() / 2.0; // w is diameter
            double y1 = c1.get_y() + c1.get_w() / 2.0; // w is diameter
            double y2 = c2.get_y() + c2.get_w() / 2.0; // w is diameter
            // calculate distance between centers and compare
            double distance = norm2(x2 - x1, y2 - y1);
            return distance < separation;
        }

        static Boolean circleBoxCollide(Entity c, Entity b){
            // get necessary coordinates
            double r = c.get_w() / 2.0; // w is diameter
            double xc = c.get_x() + r;
            double yc = c.get_y() + r;
            double bx2 = b.get_x() + b.get_w();
            double by2 = b.get_y() + b.get_h();
            // check if circle center is within the rounded rectangle resulting from expanding box by r
            Boolean in_horizontal_box = xc > b.get_x() - r && xc < bx2 + r && yc > b.get_y() && yc < by2;
            Boolean in_vertical_box = yc > b.get_y() - r && yc < by2 + r && xc > b.get_x() && xc < bx2;
            Boolean in_corner_circles =
                    norm2(xc - b.get_x(), yc - b.get_y()) < r ||
                    norm2(xc - bx2, yc - b.get_y()) < r ||
                    norm2(xc - b.get_x(), yc - by2) < r ||
                    norm2(xc - bx2, yc - by2) < r ;
            // if in any of the regions, is collided (OR)
            return in_corner_circles || in_horizontal_box || in_vertical_box;
        }

        static Boolean boxBoxCollide(Entity b1, Entity b2){
            // get upper bounds of box coordinates
            double b1x2 = b1.get_x() + b1.get_w();
            double b1y2 = b1.get_y() + b1.get_h();
            double b2x2 = b2.get_x() + b2.get_w();
            double b2y2 = b2.get_y() + b2.get_h();
            // intersect box intervals on each dimension separately
            Boolean x_intersect = !((b1.get_x() < b2.get_x() && b1x2 < b2.get_x()) || (b1.get_x() > b2x2 && b1x2 > b2x2));
            Boolean y_intersect = !((b1.get_y() < b2.get_y() && b1y2 < b2.get_y()) || (b1.get_y() > b2y2 && b1y2 > b2y2));
            // both dimensions intersect == box actually intersects
            return x_intersect && y_intersect;
        }

        static double norm2(double x, double y){
            return Math.sqrt(Math.pow(x, 2.0) + Math.pow(y, 2.0));
        }
    }

    public class Player extends Entity{
        double jumpHeight = 0.0;
        double vel_y = 0.0;
        Boolean jumping = false;
        double spriteTime = 0.0;
        int spriteIndex = 0;

        static final double SPRITE_DURATION = 0.25;
        static Image[] sprites = {
                new Image("file:images/dino1.png", true),
                new Image("file:images/dino2.png", true),
                new Image("file:images/dino3.png", true)
        };

        @Override
        public double get_x() {
            return 60;
        }

        @Override
        public double get_y() {
            return HEIGHT - GROUND_HEIGHT - 90 + 10 - jumpHeight;
        }

        @Override
        public double get_w() {
            return 90;
        }

        @Override
        public double get_h() {
            return 90;
        }

        @Override
        public CollisionShape get_collisionShape() {
            return CollisionShape.Box;
        }

        public void reset(){
            jumping = false;
            jumpHeight = 0.0;
            vel_y = 0.0;
        }

        @Override
        public void update(double dt){
            if (jumping){
                // compute gravity
                double new_vel_y = vel_y - dt * 400;
                jumpHeight += (vel_y + new_vel_y) / 2 * dt;
                vel_y = new_vel_y;
                // see if floor has been hit
                if (jumpHeight < 0.0){
                    jumpHeight = 0.0;
                    vel_y = 0.0;
                    jumping = false;
                }
            }
            // cycle between animation frames
            spriteTime += dt;
            if (spriteTime >= SPRITE_DURATION){
                spriteTime -= SPRITE_DURATION;
                spriteIndex ++;
                spriteIndex %= sprites.length;
            }
        }

        @Override
        public void draw(GraphicsContext gc){
            gc.drawImage(sprites[spriteIndex], get_x(), get_y());
        }

        public void jump(){
            if (!jumping) {
                vel_y = 400;
                jumping = true;
            }
        }
    }

    public class Obstacle extends Entity{
        public enum ObstacleType{
            Fire1,
            Fire2,
            Fire3
        }

        final static Image sprite1 = new Image("file:images/fire1.png", true);
        final static Image sprite2 = new Image("file:images/fire2.png", true);
        final static Image sprite3 = new Image("file:images/fire3.png", true);

        final ObstacleType obstacleType;
        double pos_x;

        public Obstacle(){
            obstacleType = ObstacleType.values()[RandomGenerator.getDefault().nextInt(ObstacleType.values().length)];
            pos_x = WIDTH;
        }

        @Override
        public double get_x() {
            return pos_x;
        }

        @Override
        public double get_y() {
            return HEIGHT - GROUND_HEIGHT - get_h() + 10;
        }

        @Override
        public double get_w() {
            switch (obstacleType){
                case Fire1 -> {
                    return 50;
                }
                case Fire2 -> {
                    return 70;
                }
                case Fire3 -> {
                    return 100;
                }
            }
            return 0;
        }

        @Override
        public double get_h() {
            switch (obstacleType){
                case Fire1 -> {
                    return 50;
                }
                case Fire2 -> {
                    return 70;
                }
                case Fire3 -> {
                    return 65;
                }
            }
            return 0;
        }

        @Override
        public CollisionShape get_collisionShape() {
            return CollisionShape.Box;
        }

        @Override
        public void update(double dt){
            this.pos_x -= dt * INITIAL_SPEED;
        }

        @Override
        public void draw(GraphicsContext gc){
            Image image = switch (obstacleType){
                case Fire1 -> sprite1;
                case Fire2 -> sprite2;
                case Fire3 -> sprite3;
            };
            gc.drawImage(image, get_x(), get_y());
        }
    }

    public class Coin extends Entity {

        double pos_x;
        double pos_y;

        static final Image sprite = new Image("file:images/coin.png", true);

        Coin(){
            this.pos_y = RandomGenerator.getDefault().nextDouble(HEIGHT - GROUND_HEIGHT - 200, HEIGHT - GROUND_HEIGHT - 50);
            this.pos_x = WIDTH;
        }

        @Override
        public double get_x() {
            return pos_x;
        }

        @Override
        public double get_y() {
            return pos_y;
        }

        @Override
        public double get_w() {
            return 30;
        }

        @Override
        public double get_h() {
            return 37;
        }

        @Override
        public CollisionShape get_collisionShape() {
            return CollisionShape.Circle;
        }

        @Override
        void update(double dt) {
            pos_x -= INITIAL_SPEED * dt;
        }

        @Override
        void draw(GraphicsContext gc) {
            gc.drawImage(sprite, get_x(), get_y());
        }
    }
}