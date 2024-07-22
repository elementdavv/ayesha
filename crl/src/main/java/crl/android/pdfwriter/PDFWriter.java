//
//  Android PDF Writer
//  http://coderesearchlabs.com/androidpdfwriter
//
//  by Javier Santo Domingo (j-a-s-d@coderesearchlabs.com)
//

package crl.android.pdfwriter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public class PDFWriter {

	private PDFDocument mDocument;
	private IndirectObject mCatalog;
	private Pages mPages;
	private Page mCurrentPage;
    private OutputStream os;
    private Map<String, String> mInfo;

	public PDFWriter(OutputStream os) throws IOException {
		this(os, null);
	}

	public PDFWriter(OutputStream os, Map<String, String> info) throws IOException {
		this(os, info, PaperSize.A4_WIDTH, PaperSize.A4_HEIGHT);
	}

	public PDFWriter(OutputStream os, int pageWidth, int pageHeight) throws IOException {
		this(os, null, pageWidth, pageHeight);
	}
	
	public PDFWriter(OutputStream os, Map<String, String> info, int pageWidth, int pageHeight) throws IOException {
        this.os = os;
        this.mInfo = info;
        // DO NOT CHANGE THIS CALLING ORDER
		mDocument = new PDFDocument(os);
		mCatalog = mDocument.newIndirectObject();
		mDocument.includeIndirectObject(mCatalog);
		mPages = new Pages(mDocument, pageWidth, pageHeight);
		renderCatalog();
	}
	
	private void renderCatalog() {
		mCatalog.setDictionaryContent("  /Type /Catalog\n  /Pages " + mPages.getIndirectObject().getIndirectReference() + "\n");
	}
	
	public void newPage(int index) throws IOException {
        flush();
		mCurrentPage = mPages.newPage(index);
		mDocument.includeIndirectObject(mCurrentPage.getIndirectObject());
	}
	
    public void newPage(int index, int pageWidth, int pageHeight) throws IOException {
        flush();
		mCurrentPage = mPages.newPage(index, pageWidth, pageHeight);
		mDocument.includeIndirectObject(mCurrentPage.getIndirectObject());
    }

    private void flush() throws IOException {
        if (mCurrentPage != null) {
            mPages.flush();
            mDocument.flush();
            mCurrentPage = null;
        }
    }

    public void end() throws IOException {
        mPages.render();
		mDocument.includeIndirectObject(mPages.getIndirectObject());
        Info info = new Info(mDocument, mInfo);
        mDocument.endInfo(info);
        flush();
        mDocument.end();
        this.os = null;
    }

    public void setOpaque(Double opaque) {
        mCurrentPage.setOpaque(opaque);
    }

	public int getPageCount() {
		return mPages.getCount();
	}
	
	public void setFont(String subType, String baseFont) {
		mCurrentPage.setFont(subType, baseFont);
	}

	public void setFont(String subType, String baseFont, String encoding) {
		mCurrentPage.setFont(subType, baseFont, encoding);
	}
	
	public void addRawContent(String rawContent) {
		mCurrentPage.addRawContent(rawContent);
	}

	public void addText(int leftPosition, int topPositionFromBottom, int fontSize, String text) {
		addText(leftPosition, topPositionFromBottom, fontSize, text, Transformation.DEGREES_0_ROTATION);
	}
	
	public void addText(int leftPosition, int topPositionFromBottom, int fontSize, String text, String transformation) {
		mCurrentPage.addText(leftPosition, topPositionFromBottom, fontSize, text, transformation);
	}

	public void addTextAsHex(int leftPosition, int topPositionFromBottom, int fontSize, String hex) {
		addTextAsHex(leftPosition, topPositionFromBottom, fontSize, hex, Transformation.DEGREES_0_ROTATION);
	}
	
	public void addTextAsHex(int leftPosition, int topPositionFromBottom, int fontSize, String hex, String transformation) {
		mCurrentPage.addTextAsHex(leftPosition, topPositionFromBottom, fontSize, hex, transformation);
	}
	
	public void addLine(int fromLeft, int fromBottom, int toLeft, int toBottom) {
		mCurrentPage.addLine(fromLeft, fromBottom, toLeft, toBottom);
	}
	
	public void addRectangle(int fromLeft, int fromBottom, int toLeft, int toBottom) {
		mCurrentPage.addRectangle(fromLeft, fromBottom, toLeft, toBottom);
	}

    public void newImagePage(int index, byte[] src)
            throws InvalidImageException, IOException {
		XObjectImage xImage = new XObjectImage(mDocument, src);
        newPage(index, xImage.getWidth(), xImage.getHeight());
        addImage(xImage);
    }

	public void addImage(byte[] src)
            throws InvalidImageException, IOException {
		addImage(src, 0, 0);
	}

	public void addImage(XObjectImage xImage)
            throws InvalidImageException {
		addImage(xImage, 0, 0);
	}

	public void addImage(byte[] src, int fromLeft, int fromBottom)
            throws InvalidImageException, IOException {
		addImage(src, fromLeft, fromBottom, Transformation.DEGREES_0_ROTATION);
	}

	public void addImage(XObjectImage xImage, int fromLeft, int fromBottom)
            throws InvalidImageException {
		addImage(xImage, fromLeft, fromBottom, Transformation.DEGREES_0_ROTATION);
	}
	
	public void addImage(byte[] src, int fromLeft, int fromBottom, String transformation)
            throws InvalidImageException, IOException {
		XObjectImage xImage = new XObjectImage(mDocument, src);
		addImage(xImage, fromLeft, fromBottom, transformation);
	}

	public void addImage(XObjectImage xImage, int fromLeft, int fromBottom, String transformation)
            throws InvalidImageException {
		addImage(xImage, fromLeft, fromBottom, xImage.getWidth(), xImage.getHeight(), transformation);
	}
	
	public void addImage(byte[] src, int fromLeft, int fromBottom, int toLeft, int toBottom)
            throws InvalidImageException, IOException {
		addImage(src, fromLeft, fromBottom, toLeft, toBottom, Transformation.DEGREES_0_ROTATION);
	}
	
	public void addImage(XObjectImage xImage, int fromLeft, int fromBottom, int toLeft, int toBottom)
            throws InvalidImageException {
		addImage(xImage, fromLeft, fromBottom, toLeft, toBottom, Transformation.DEGREES_0_ROTATION);
	}
	
	public void addImage(byte[] src, int fromLeft, int fromBottom, int toLeft, int toBottom, String transformation)
            throws InvalidImageException, IOException {
		XObjectImage xImage = new XObjectImage(mDocument, src);
		addImage(xImage, fromLeft, fromBottom, toLeft, toBottom, transformation);
	}

	public void addImage(XObjectImage xImage, int fromLeft, int fromBottom, int toLeft, int toBottom, String transformation)
            throws InvalidImageException {
		mCurrentPage.addImage(xImage, fromLeft, fromBottom, toLeft, toBottom, transformation);
	}
	
	public void addImageKeepRatio(byte[] src, int fromLeft, int fromBottom, int width, int height)
            throws InvalidImageException, IOException {
		addImageKeepRatio(src, fromLeft, fromBottom, width, height, Transformation.DEGREES_0_ROTATION);
	}
	
	public void addImageKeepRatio(byte[] src, int fromLeft, int fromBottom, int width, int height, String transformation)
            throws InvalidImageException, IOException {
		XObjectImage xImage = new XObjectImage(mDocument, src);
		final float imgRatio = (float) xImage.getWidth() / (float) xImage.getHeight();
		final float boxRatio = (float) width / (float) height;
		float ratio;

		if (imgRatio < boxRatio) {
			ratio = (float) width / (float) xImage.getWidth();
		} else { 
			ratio = (float) height / (float) xImage.getHeight();
		}

		width = (int) (xImage.getWidth() * ratio);
		height = (int) (xImage.getHeight() * ratio);
		mCurrentPage.addImage(xImage, fromLeft, fromBottom, width, height, transformation);
	}
}
