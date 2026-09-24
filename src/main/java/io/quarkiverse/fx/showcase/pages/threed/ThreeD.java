package io.quarkiverse.fx.showcase.pages.threed;

import java.util.Locale;

import io.quarkiverse.fx.showcase.core.Fx;
import javafx.geometry.Bounds;
import javafx.scene.Camera;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

/**
 * Helpers shared by the 3D pages : sub scenes, cameras, meshes and generated textures.
 * <p>
 * Everything is built from fixed values (no animation, no random), so that the rendering is the same on every run.
 */
final class ThreeD {

    static final Color BACKGROUND = Color.web("#1f2430");

    private ThreeD() {
    }

    static Image image(String path) {
        // synchronous loading : the image is complete when the page is built
        return new Image(Fx.resourceUrl(path));
    }

    /**
     * A sub scene with a depth buffer and antialiasing.
     */
    static SubScene subScene(Group root, double width, double height, Camera camera, Color fill) {
        SubScene subScene = new SubScene(root, width, height, true, SceneAntialiasing.BALANCED);
        subScene.setFill(fill);
        subScene.setCamera(camera);
        return subScene;
    }

    /**
     * A perspective camera looking at the origin from {@code distance}, tilted down by {@code tilt} degrees.
     */
    static PerspectiveCamera orbitCamera(double fieldOfView, double tilt, double pan, double distance) {
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setFieldOfView(fieldOfView);
        camera.setNearClip(1);
        camera.setFarClip(5000);
        camera.getTransforms().addAll(new Rotate(pan, Rotate.Y_AXIS), new Rotate(-tilt, Rotate.X_AXIS),
                new Translate(0, 0, -distance));
        return camera;
    }

    static VBox captioned(Node node, String caption, double width) {
        Label label = new Label(caption);
        label.setStyle("-fx-font-size: 11px; -fx-text-fill: #444444;");
        label.setMaxWidth(width);
        label.setWrapText(true);
        return new VBox(2, node, label);
    }

    static PhongMaterial phong(Color diffuse, Color specular, double power) {
        PhongMaterial material = new PhongMaterial(diffuse);
        material.setSpecularColor(specular);
        material.setSpecularPower(power);
        return material;
    }

    /**
     * A square based pyramid (apex up) with texture coordinates : 5 points, 5 texture coordinates, 6 faces.
     */
    static TriangleMesh pyramid(float half, float height) {
        TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_TEXCOORD);
        mesh.getPoints().addAll(
                0, -height, 0, // 0 apex
                -half, 0, -half, // 1 front left
                half, 0, -half, // 2 front right
                half, 0, half, // 3 back right
                -half, 0, half); // 4 back left
        mesh.getTexCoords().addAll(
                0.5f, 0, // 0 apex
                0, 1, // 1
                1, 1, // 2
                0, 0, // 3
                1, 0); // 4
        // front faces : counter-clockwise when seen from outside (on screen, y down)
        mesh.getFaces().addAll(
                0, 0, 1, 1, 2, 2, // front
                0, 0, 2, 1, 3, 2, // right
                0, 0, 3, 1, 4, 2, // back
                0, 0, 4, 1, 1, 2, // left
                1, 3, 3, 2, 2, 4, // base
                1, 3, 4, 1, 3, 2);
        return mesh;
    }

    /**
     * An open box (no top) : 8 points, 10 faces, each side mapped to one color cell of {@link #faceColors()}.
     */
    static TriangleMesh openBox(float half, float halfHeight) {
        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().addAll(
                -half, -halfHeight, -half, // 0 top front left
                half, -halfHeight, -half, // 1 top front right
                half, -halfHeight, half, // 2 top back right
                -half, -halfHeight, half, // 3 top back left
                -half, halfHeight, -half, // 4 bottom front left
                half, halfHeight, -half, // 5 bottom front right
                half, halfHeight, half, // 6 bottom back right
                -half, halfHeight, half); // 7 bottom back left
        for (int i = 0; i < 5; i++) {
            mesh.getTexCoords().addAll((i * 10 + 5) / 50f, 0.5f);
        }
        // outer sides are the front faces
        mesh.getFaces().addAll(
                // front (z = -half), color 0
                0, 0, 5, 0, 1, 0,
                0, 0, 4, 0, 5, 0,
                // right (x = half), color 1
                1, 1, 6, 1, 2, 1,
                1, 1, 5, 1, 6, 1,
                // back (z = half), color 2
                2, 2, 7, 2, 3, 2,
                2, 2, 6, 2, 7, 2,
                // left (x = -half), color 3
                3, 3, 4, 3, 0, 3,
                3, 3, 7, 3, 4, 3,
                // bottom (y = halfHeight), color 4
                4, 4, 6, 4, 5, 4,
                4, 4, 7, 4, 6, 4);
        return mesh;
    }

    /**
     * 5 color cells of 10x10 pixels, one per side of {@link #openBox(float, float)}.
     */
    static Image faceColors() {
        Color[] colors = { Color.web("#4c78a8"), Color.web("#54a24b"), Color.web("#e45756"), Color.web("#b279a2"),
                Color.web("#f58518") };
        WritableImage image = new WritableImage(50, 10);
        PixelWriter writer = image.getPixelWriter();
        for (int x = 0; x < 50; x++) {
            for (int y = 0; y < 10; y++) {
                writer.setColor(x, y, colors[x / 10]);
            }
        }
        return image;
    }

    /**
     * A normal map of 4x4 beveled tiles, computed with integers only.
     */
    static Image bevelNormalMap() {
        int size = 128;
        int tile = 32;
        int bevel = 7;
        WritableImage image = new WritableImage(size, size);
        PixelWriter writer = image.getPixelWriter();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int tx = x % tile;
                int ty = y % tile;
                int argb;
                int left = tx;
                int right = tile - 1 - tx;
                int top = ty;
                int bottom = tile - 1 - ty;
                int min = Math.min(Math.min(left, right), Math.min(top, bottom));
                if (min >= bevel) {
                    argb = 0xFF8080FF; // flat (0, 0, 1)
                } else if (min == left) {
                    argb = 0xFF3380D9; // (-0.6, 0, 0.8)
                } else if (min == right) {
                    argb = 0xFFCC80D9; // (0.6, 0, 0.8)
                } else if (min == top) {
                    argb = 0xFF8033D9; // (0, -0.6, 0.8)
                } else {
                    argb = 0xFF80CCD9; // (0, 0.6, 0.8)
                }
                writer.setArgb(x, y, argb);
            }
        }
        return image;
    }

    static String bounds(Bounds b) {
        return String.format(Locale.ROOT, "%.1f x %.1f x %.1f", b.getWidth(), b.getHeight(), b.getDepth());
    }

    static String f3(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
