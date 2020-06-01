package drno.dev;


import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.name.Rename;
import org.imgscalr.Scalr;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.*;


import static org.imgscalr.Scalr.resize;

/*
 * Hello world!
 *
 */
public class App 
{
    public static int newSize   = 700;
    public static int quality   = 90;
    public static String done   = "dn";
    public static String todo   = "img";
    public static String folder = "/Users/drno/imagenes-pruebas/";

    static Logger logger = LoggerFactory.getLogger(App.class);

    public static void main( String[] args )
    {

        //load OpenCV
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME );

        try{
            reentryProcess(folder);
            wathFolder(folder);
        }catch (IOException io){
            logger.error(io.getMessage());
            io.printStackTrace();
        }catch (InterruptedException in){
            logger.error(in.getMessage());
            in.printStackTrace();
        }
    }

    /**
     * Cambia el tamaño y el formato a jpg
     * */
    public static void resizeImageWithScalr(File file) throws IOException {
        long time = System.currentTimeMillis();
        BufferedImage image = ImageIO.read(file);
        File newFile = new File(folder + todo + "/" + (file.getName().replace(".png",".jpg")).replace(".jpeg",".jpg"));
        ImageIO.write(resize(image, Scalr.Method.QUALITY,newSize), "JPG", newFile);
        file.renameTo(new File(folder + done + "/" + file.getName()));
        logger.info("file :" + file.getName() + " : "+ (System.currentTimeMillis()-time));
    }


    public static void resizeImageWithThumbnails(File file) throws  IOException{
        long time = System.currentTimeMillis();
        Thumbnails.of(file)
                .size(newSize, newSize)
                .outputFormat("jpg")
                .toFiles(Rename.PREFIX_DOT_THUMBNAIL);
        logger.info("file :" + file.getName() + " : "+ (System.currentTimeMillis()-time));
    }


    public static void resizeImageWithOpenCV(File file) throws IOException{

        long time = System.currentTimeMillis();
        BufferedImage img = ImageIO.read(file);
        /* SI ES PNG */
        if(file.getName().endsWith(".png")){
            BufferedImage newBufferedImage = new BufferedImage(img.getWidth(),
                    img.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            newBufferedImage.createGraphics().drawImage(img, 0, 0, Color.WHITE, null);
            img = newBufferedImage;
        }
        byte[] pixels = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        Mat matImg = new Mat(img.getHeight(), img.getWidth(), CvType.CV_8UC3);
        matImg.put(0, 0, pixels);
        Mat resizeImage = new Mat();
        MatOfInt par = new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY,quality);
        Size sz = fixSize(img.getWidth(),img.getHeight());
        Imgproc.resize(matImg, resizeImage,sz,sz.width,sz.height,Imgproc.INTER_AREA);
        String newFile = (folder + todo + "/" + (file.getName().replace(".png",".jpg")).replace(".jpeg",".jpg"));
        Imgcodecs.imwrite(newFile, resizeImage,par);
        file.renameTo(new File(folder + done + "/" + file.getName()));

        logger.info("file :" + file.getName() + " : "+ (System.currentTimeMillis()-time));

    }


    /**
     * Cuando un archivo nuevo llega a la ruta
     */
    public static void wathFolder(String folder) throws IOException, InterruptedException{
        try {
            Path path = Paths.get(folder);
            WatchService watchService = FileSystems.getDefault().newWatchService();
            path.register(watchService,StandardWatchEventKinds.ENTRY_CREATE);
            boolean valid = true;
            do{
                WatchKey watchKey = watchService.take();
                for (WatchEvent event : watchKey.pollEvents()){
                    WatchEvent.Kind kind = event.kind();
                    if(StandardWatchEventKinds.ENTRY_CREATE.equals(kind)){
                        String fileName = event.context().toString();
                            if(fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                                Runnable runnable = () -> {
                                    try {
                                        //resizeImageWithScalr(new File(folder + fileName));
                                        //resizeImageWithOpenCV(new File(folder + fileName));
                                        resizeImageWithThumbnails(new File(folder + fileName));
                                    } catch (IOException e) {
                                        logger.error(e.getMessage());
                                        e.printStackTrace();
                                    }
                                };
                                runnable.run();
                            }
                    }
                }
                valid = watchKey.reset();
            }while (valid);

        }catch (IOException io){
            io.printStackTrace();
            throw new IOException(io);
        }catch (InterruptedException in){
            in.printStackTrace();
            throw new InterruptedException(in.getMessage());
        }
    }

    /**
    * Procesar archivos que quedaron sin procesar por alguna caida
    * */
    static void reentryProcess(String folder){
        File file = new File(folder);
        FilenameFilter filenameFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg"));
            }
        };
        for (File toProcess : file.listFiles(filenameFilter)) {
            Runnable runnable = () -> {
                    try {
                        resizeImageWithOpenCV(toProcess);
                    } catch (IOException e) {
                        logger.error(e.getMessage());
                        e.printStackTrace();
                    }
            };
            runnable.run();
        }
    }


    /**
     * Subir porcentaje gradualmente al llegar al tamaño minimo o maximo retornar el size
     * iniciar con 5%
     * */
    static Size fixSize(int w, int h){
        double sw = 0.0;
        double sh = 0.0;
        int p = 80;
        do{
            sw = (p / 100.0) * w;
            sh = (p / 100.0) * h;
            if(sw <= newSize || sh <= newSize){
                return new Size(sw,sh);
            }
            p-=5;
        }while ((100-p) <= 90);
        return new Size(800, 800);
    }
}
