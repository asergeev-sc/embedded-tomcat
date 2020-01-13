package man.sab;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;

// import org.apache.commons.io.FileUtils;


public class Main {
    public static void main(String[] args) throws Exception {
        if (args !=null && args.length > 0) {
            for (String arg: args) {
                System.out.println("argument '" + arg + "'");
            }
        }


        URL appWarFileUrl = Main.class.getClassLoader().getResource("META-INF/app.war");
        // resource is not found?!
        if (appWarFileUrl == null) {
            throw new RuntimeException("File 'META-INF/app.war' is not found inside jar archive");
        }
        // create temp folder where app will be extracted
        File applicationTempDir = Files.createTempDirectory("application").toFile();
        // on JVM shutdown delete this dir
        applicationTempDir.deleteOnExit();

        System.out.println("Extracting application war file into '" + applicationTempDir.getAbsolutePath() + "' folder");
        extract(appWarFileUrl, applicationTempDir);

        Tomcat tomcat = new Tomcat();

        StandardContext ctx = (StandardContext) tomcat.addWebapp("/prov", applicationTempDir.getAbsolutePath());
        System.out.println("Сonfiguring app with basedir '" + applicationTempDir.getAbsolutePath() + "'");

        System.out.println("starting tomcat");
        tomcat.start();
        tomcat.getServer().await();
    }

    private static final int BUFFER_SIZE = 4096;

    public static void extract(URL urlToZipFile, File target) throws IOException {
        ZipInputStream zip = new ZipInputStream(urlToZipFile.openStream());
        try {
            ZipEntry entry;

            while ((entry = zip.getNextEntry()) != null) {
                File file = new File(target, entry.getName());

                if (!file.toPath().normalize().startsWith(target.toPath())) {
                    throw new IOException("Bad zip entry");
                }

                if (entry.isDirectory()) {
                    file.mkdirs();
                    continue;
                }

                byte[] buffer = new byte[BUFFER_SIZE];
                file.getParentFile().mkdirs();
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
                int count;
                while ((count = zip.read(buffer)) != -1) {
                    out.write(buffer, 0, count);
                }
                out.close();
            }
        } finally {
            zip.close();
        }
    }
}