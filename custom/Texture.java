package custom;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Texture {
    private String bitmapPath;
    private BufferedImage texture;
    private String format;

    public Texture(String bitmapPath, String format) {
        this.bitmapPath = bitmapPath;
        this.format = format;
        setTexture();
    }

    public void setTexture() {
        try {
            File pathToFile = new File(bitmapPath);
            texture = ImageIO.read(pathToFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ByteBuffer getTextureBuffer() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(texture, format, out);
            return ByteBuffer.wrap(out.toByteArray());
        } catch (IOException ex) {
            //TODO
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
