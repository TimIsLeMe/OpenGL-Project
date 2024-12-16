package custom;

import org.w3c.dom.Text;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Texture {
    private final String bitmapPath;
    private BufferedImage texture;

    public Texture(String bitmapPath) {
        this.bitmapPath = bitmapPath;
        loadTexture();
    }

    public Texture(BufferedImage texture) {
        this.bitmapPath = null;
        this.texture = texture;
    }

    public void loadTexture() {
        try {
            File pathToFile = new File(bitmapPath);
            texture = ImageIO.read(pathToFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Texture[] createMIP(int level) {
        float scale = 1f;
        var textures = new Texture[level];
        for(int i = 0; i < level; i++) {
            scale /= 2;
            textures[i] = createScaledVersion(scale);
        }
        return textures;
    }

    public Texture createScaledVersion(float scale) {
        BufferedImage scaledImage = new BufferedImage((int) (texture.getWidth() * scale), (int) (texture.getHeight() * scale), texture.getType());
        Graphics2D g2d = scaledImage.createGraphics();
        g2d.drawImage(texture.getScaledInstance((int) (texture.getWidth() * scale), (int) (texture.getHeight() * scale), Image.SCALE_SMOOTH), 0, 0, null);
        g2d.dispose();
        return new Texture(scaledImage);
    }
    public ByteBuffer getTextureBuffer() {
        try {
            int width = texture.getWidth();
            int height = texture.getHeight();
            // Extract raw pixel data
            int[] pixels = new int[width * height];
            texture.getRGB(0, 0, width, height, pixels, 0, width);
            ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = pixels[y * width + x];
                    int alpha = (pixel >> 24) & 0xFF;
                    int red = (pixel >> 16) & 0xFF;
                    int green = (pixel >> 8) & 0xFF;
                    int blue = pixel & 0xFF;
                    float alphaFactor = alpha / 255.0f;
                    red = (int) (red * alphaFactor);
                    green = (int) (green * alphaFactor);
                    blue = (int) (blue * alphaFactor);
                    buffer.put((byte) red);
                    buffer.put((byte) green);
                    buffer.put((byte) blue);
                    buffer.put((byte) alpha);
                }
            }
            buffer.flip();
            return buffer;

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public String getBitmapPath() {
        return bitmapPath;
    }

    public BufferedImage getTexture() {
        return texture;
    }
}
