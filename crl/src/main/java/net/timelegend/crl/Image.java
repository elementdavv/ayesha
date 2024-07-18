package net.timelegend.crl;
//
//  Android PDF Writer
//  http://github.com/elementdavv/pdfwriter
//
//  by Element Davv (elementdavv@hotmail.com)
//

public abstract class Image {
    protected byte[] imgData = null;
	protected int width;
	protected int height;
    protected int bits;
    protected String colorSpace;

    public Image(byte[] src) {}

	public abstract IndirectObject appendToDocument(PDFDocument document) throws InvalidImageException;

	public int getWidth() {
        return width;
    }

	public int getHeight() {
        return height;
    }
}
