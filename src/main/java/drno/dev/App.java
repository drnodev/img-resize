package drno.dev;


import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

public class App 
{
    public static int newSize   = 700;
    public static int quality   = 95;
    public static String todo   = "img";
    public static String folder = "demo/";

    static Logger logger = LoggerFactory.getLogger(App.class);

    public static void main( String[] args )
    {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME );
        process(folder);
    }

    public static void resizeImageWithOpenCV(File file) throws IOException{
        long time = System.currentTimeMillis();
        Mat matImg = Imgcodecs.imread(file.getAbsolutePath());
        MatOfInt par = new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY,quality);
        Size sz = fixSize(matImg.cols(),matImg.rows());
        Mat resizeImage = new Mat();
        Imgproc.resize(matImg, resizeImage,sz,sz.width,sz.height,Imgproc.INTER_AREA);
        String newFile = (folder + todo + "/cv/" + (file.getName().replace(".png",".jpg")).replace(".jpeg",".jpg"));
        Imgcodecs.imwrite(newFile, resizeImage,par);
        logger.info("file :" + file.getName() + " : "+ (System.currentTimeMillis()-time) + ":" + file.length() + ":" + (new File(newFile)).length());
    }



    static void process(String folder){
        File file = new File(folder);
        logger.info(file.getAbsolutePath());
        FilenameFilter filenameFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg"));
            }
        };
        logger.info(file.listFiles().length+"");
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


    static Size fixSize(double w, double h){
        return new Size(newSize, ((newSize / w) * h));
    }
}
