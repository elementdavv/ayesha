//
//  Android PDF Writer
//  http://github.com/elementdavv/pdfwriter
//
//  by Element Davv (elementdavv@hotmail.com)
//

package crl.android.pdfwriter;

import java.io.IOException;
import java.lang.Byte;

public class XObjectImage {
	private PDFDocument mDocument;
	private IndirectObject mIndirectObject;
    private Image image;
	private String mName;
	private String mId;
	
    private static String APP = "PDFWriter";
	private static int mImageCount = 0;

	public XObjectImage(PDFDocument document, byte[] src) throws InvalidImageException, IOException {
        if (src.length < 13)
            throw new InvalidImageException("Invalid stream.");

        if (src[0] == (byte)0xFF && src[1] == (byte)0xD8) {
            image = new Jpeg(src);
        }
        else if (src[0] == (byte)0x89 && src[1] == (byte)0x50
                && src[2] == (byte)0x4e && src[3] == (byte)0x47) {
            image = new Png(src);
        }
        else
            throw new InvalidImageException("unknown stream.");

		mDocument = document;
		mId = Indentifiers.generateId(src);
		mName = "/img" + (++mImageCount);
	}

	public void appendToDocument() throws InvalidImageException {
        mIndirectObject = image.appendToDocument(mDocument);
	}
	
	public String asXObjectReference() {
		return mName + " " + mIndirectObject.getIndirectReference();
	}

	public String getName() {
		return mName;
	}

	public String getId() {
		return mId;
	}

	public int getWidth() {
		return image.getWidth();
	}
	
	public int getHeight() {
		return image.getHeight();		
	}
}
