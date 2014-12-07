package com.group15.toq_o.present.Presentation;

import android.graphics.Bitmap;
import android.os.Environment;

import com.google.api.client.util.DateTime;
import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFImage;
import com.sun.pdfview.PDFPage;
import com.sun.pdfview.PDFPaint;

import net.sf.andpdf.nio.ByteBuffer;
import net.sf.andpdf.refs.HardReference;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;

/**
 * Created by weili on 11/16/14.
 */
public class Presentation {

    String filename;
    String[] messages;
    String name;
    DateTime lastModified;
    String id;

    public Presentation(String name, String filename, DateTime date, String id) {
        try {
            File file = new File(Environment.getExternalStorageDirectory().getPath() + "/pdf", filename);
            FileInputStream stream = new FileInputStream(file);
            byte[] data = IOUtils.toByteArray(stream);
            //create pdf document object from bytes
            ByteBuffer bb = ByteBuffer.NEW(data);
            PDFFile pdf = new PDFFile(bb);
            int length = pdf.getNumPages();
            stream.close();
            this.filename = filename;
            messages = new String[length];
            this.name = name;
            lastModified = date;
            this.id = id;
        } catch(Exception e) {
            //do nothing
            System.out.println("should have no errors");
            //e.printStackTrace();
        }
    }

    public String getFilename() {
        return filename;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
    
    public DateTime getLastModified() {
        return lastModified;
    }

    public Slide getSlide(int slideNumber) throws IndexOutOfBoundsException {
        if (filename == null) {
            return null;
        }
        if(slideNumber<0 || slideNumber >= messages.length) {
            throw new IndexOutOfBoundsException("slide does not exist");
        }
        PDFImage.sShowImages = true;
        PDFPaint.s_doAntiAlias = true;
        HardReference.sKeepCaches = true;
        try {
            File file = new File(Environment.getExternalStorageDirectory().getPath() + "/pdf/" + filename);
            RandomAccessFile f = new RandomAccessFile(file, "r");
            byte[] data = new byte[(int)f.length()];
            f.readFully(data);
            //create pdf document object from bytes
            ByteBuffer bb = ByteBuffer.NEW(data);
            PDFFile pdf = new PDFFile(bb);
            try {
                PDFPage PDFpage = pdf.getPage(slideNumber, true);
                Bitmap img = PDFpage.getImage((int) (PDFpage.getWidth()), (int) (PDFpage.getHeight()), null, true, true);
                /*ByteArrayOutputStream stream = new ByteArrayOutputStream();
                img.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] byteArray = stream.toByteArray();
                stream.reset();
                //convert the byte array to a base64 string
                String base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP);
                //create the html + add the first image to the html
                String html = "<!DOCTYPE html><html><body bgcolor=\"#b4b4b4\"><img src=\"data:image/png;base64,"+base64+"\" hspace=10 vspace=10><br>";
                html += "</body></html>";
                return html;*/
                Slide slide = new Slide();
                slide.setImg(img);
                slide.setMessage(messages[slideNumber]);
                return slide;
            } catch(Exception e) {
                //should not reach
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("somehow an error");
        }
        return null;
    }
}
