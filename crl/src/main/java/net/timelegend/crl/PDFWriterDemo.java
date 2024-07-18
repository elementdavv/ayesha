package net.timelegend.crl;
//
//  Android PDF Writer
//  http://coderesearchlabs.com/androidpdfwriter
//
//  by Javier Santo Domingo (j-a-s-d@coderesearchlabs.com)
//

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class PDFWriterDemo {
	
	public static void generateHelloWorldPDF(OutputStream os, Context c) throws InvalidImageException, IOException {
        AssetManager mngr = c.getAssets();
        Map<String, String> info = new HashMap<>();
        info.put("Title", "Chemical");
        info.put("Author", "Alex");
		PDFWriter mPDFWriter = new PDFWriter(os, info, PaperSize.FOLIO_WIDTH, PaperSize.FOLIO_HEIGHT);

        mPDFWriter.newPage(4);
        // byte[] b1 = readAllBytes(c, "CRL-borders.png");
        // byte[] b2 = readAllBytes(c, "CRL-star.jpg");
        byte[] b3 = readAllBytes(c, "CRL-1bit.jpg");
        byte[] b4 = readAllBytes(c, "CRL-8bits.jpg");
        byte[] b5 = readAllBytes(c, "CRL-24bits.jpg");
	    // mPDFWriter.addImage(b1, 400, 600, Transformation.DEGREES_315_ROTATION);
	    // mPDFWriter.addImage(b2, 300, 500);
	    mPDFWriter.addImage(b3, 200, 400, 135, 75);
	    mPDFWriter.addImage(b4, 150, 300, 130, 70);
	    mPDFWriter.addImageKeepRatio(b3, 100, 200, 50, 25);
	    mPDFWriter.addImageKeepRatio(b4, 50, 100, 30, 25, Transformation.DEGREES_270_ROTATION);
	    mPDFWriter.addImageKeepRatio(b5, 25, 50, 30, 25);
		
        mPDFWriter.newPage(5);
        mPDFWriter.setFont(StandardFonts.SUBTYPE, StandardFonts.TIMES_ROMAN);
        mPDFWriter.addRawContent("1 0 0 rg\n");
        mPDFWriter.addTextAsHex(70, 50, 12, "68656c6c6f20776f726c6420286173206865782921");
        mPDFWriter.setFont(StandardFonts.SUBTYPE, StandardFonts.COURIER, StandardFonts.WIN_ANSI_ENCODING);
        mPDFWriter.addRawContent("0 0 0 rg\n");
        mPDFWriter.addText(30, 90, 10, " CRL", Transformation.DEGREES_270_ROTATION);

        mPDFWriter.newPage(3);
        mPDFWriter.addRawContent("[] 0 d\n");
        mPDFWriter.addRawContent("1 w\n");
        mPDFWriter.addRawContent("0 0 1 RG\n");
        mPDFWriter.addRawContent("0 1 0 rg\n");
        mPDFWriter.addRectangle(40, 50, 280, 50);
        mPDFWriter.addText(85, 75, 18, "Code Research Laboratories");

        mPDFWriter.newPage(1);
        mPDFWriter.setFont(StandardFonts.SUBTYPE, StandardFonts.COURIER_BOLD);
        mPDFWriter.addText(150, 150, 14, "http://coderesearchlabs.com");
        mPDFWriter.addLine(150, 140, 270, 140);

        byte[] src = readAllBytes(c, "book.jpg");
        mPDFWriter.newImagePage(2, src);

        byte[] cri = readAllBytes(c, "crisis.png");
        mPDFWriter.newImagePage(6, cri);

        mPDFWriter.end();
	}
	
    public static byte[] readAllBytes(Context c, String name) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        AssetManager mngr = c.getAssets();
        InputStream is = mngr.open(name);
        byte[] b = new byte[8192];
        int n = 0;
        while ((n = is.read(b)) > 0) {
            baos.write(b, 0, n);
        }
        is.close();
        byte[] src = baos.toByteArray();
        baos.close();
        return src;
    }

	public static void helloworld(Context c) throws InvalidImageException, IOException {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File newFile = new File(path, "helloworld.pdf");
        FileOutputStream pdfFile = new FileOutputStream(newFile);
        generateHelloWorldPDF(pdfFile, c);
	}
}
