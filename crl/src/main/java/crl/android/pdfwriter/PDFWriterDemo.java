//
//  Android PDF Writer
//  http://coderesearchlabs.com/androidpdfwriter
//
//  by Javier Santo Domingo (j-a-s-d@coderesearchlabs.com)
//

package crl.android.pdfwriter;

import android.app.Activity;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class PDFWriterDemo extends Activity {
	
	public void generateHelloWorldPDF(OutputStream os) throws InvalidImageException, IOException {
        AssetManager mngr = this.getAssets();
        Map<String, String> info = new HashMap<>();
        info.put("Title", "Chemical");
        info.put("Author", "Alex");
		PDFWriter mPDFWriter = new PDFWriter(os, info, PaperSize.FOLIO_WIDTH, PaperSize.FOLIO_HEIGHT);

        mPDFWriter.newPage(4);
        // byte[] b1 = readAllBytes("CRL-borders.png");
        // byte[] b2 = readAllBytes("CRL-star.jpg");
        byte[] b3 = readAllBytes("CRL-1bit.jpg");
        byte[] b4 = readAllBytes("CRL-8bits.jpg");
        byte[] b5 = readAllBytes("CRL-24bits.jpg");
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

        byte[] src = readAllBytes("book.jpg");
        mPDFWriter.newImagePage(2, src);
        mPDFWriter.setOpaque(0.0);
        mPDFWriter.addText(150, 150, 14, "http://coderesearchlabs.com");

        byte[] cri = readAllBytes("crisis.png");
        mPDFWriter.newImagePage(6, cri);

        mPDFWriter.end();
	}
	
    public byte[] readAllBytes(String name) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        AssetManager mngr = this.getAssets();
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

    @Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new Thread() {
            @Override
            public void run() {
                File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File newFile = new File(path, "helloworld.pdf");
                try {
                    FileOutputStream pdfFile = new FileOutputStream(newFile);
                    generateHelloWorldPDF(pdfFile);
                }
                catch (IOException | InvalidImageException e) {
                }
            }
        }.start();

        finish();
	}
}
