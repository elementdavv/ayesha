package net.timelegend.crl;
//
//  Android PDF Writer
//  http://github.com/elementdavv/pdfwriter
//
//  by Element Davv (elementdavv@hotmail.com)
//

import android.util.Log;

import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class Png extends Image {
    public static String APP = "PDFWriter";
    public static final int TRANSFERSIZE = 8192;

    protected int colorType;
    protected int compressionMethod;
    protected int filterMethod;
    protected boolean isInterlaced;
    protected byte[] palette = null;
    protected boolean hasPalette = false;
    protected byte[] trans_indexed = null;
    protected byte[] trans_gray = null;     // only first byte used
    protected byte[] trans_rgb = null;
    protected int colors;
    protected boolean hasAlphaChannel;
    protected byte[] alphaChannel = null;
    protected int pixelBitlength;

    protected ByteArrayInputStream is;
    protected int len;
    protected byte[] buffer = new byte[TRANSFERSIZE];
    protected ByteArrayOutputStream idat = new ByteArrayOutputStream();

    public Png(byte[] src) throws InvalidImageException, IOException {
        super(src);
        is = new ByteArrayInputStream(src);
        is.skip(8);

        while (true) {
            len = getInt(is);
            String section = getString(is);

            switch (section) {
                case "IHDR":
                    onIhdr();
                    break;
                case "PLTE":
                    onPlte();
                    break;
                case "IDAT":
                    onIdat();
                    break;
                case "tRNS":
                    onTrns();
                    break;
                case "IEND":
                    onIend();
                    is.close();
                    return;
                default:
                    is.skip(len);
            }

            is.skip(4); // crc
        }
    }

    protected void onIhdr() throws IOException {
        width = getInt(is);
        height = getInt(is);
        bits = is.read();
        colorType = is.read();
        compressionMethod = is.read();
        filterMethod = is.read();
        int interlaceMethod = is.read();
        isInterlaced = (interlaceMethod == 1);
    }

    protected void onPlte() throws IOException {
        palette = new byte[len];
        is.read(palette);
        hasPalette = true;
    }

    protected void onIdat() throws InvalidImageException, IOException {
        int n;

        while (len > 0) {
            n = is.read(buffer, 0, Math.min(len, TRANSFERSIZE));

            if (n < 0) 
                throw new InvalidImageException("Bad PNG stream");

            idat.write(buffer, 0, n);
            len -= n;
        }
    }

    protected void onTrns() throws IOException {
        byte[] buff = new byte[len];
        is.read(buffer);

        switch (colorType) {
            case 3:
                trans_indexed = buff;
                break;
            case 0:
                trans_gray = buff;
                break;
            case 2:
                trans_rgb = buff;
                break;
        }
    }

    protected void onIend() {
        switch (colorType) {
            case 0:
            case 3:
            case 4:
                colors = 1;
                break;
            case 2:
            case 6:
                colors = 3;
                break;
        }

        hasAlphaChannel = (colorType == 4 || colorType == 6);
        int colors1 = colors + (hasAlphaChannel ? 1 : 0);
        pixelBitlength = bits * colors1;

        switch (colors) {
            case 1:
                colorSpace = "/DeviceGray";
                break;
            case 3:
                colorSpace = "/DeviceRGB";
                break;
        }

        imgData = idat.toByteArray();
    }

    public static final int getInt(InputStream is) throws IOException {
        return (is.read() << 24) + (is.read() << 16) + (is.read() << 8) + is.read();
    }

    public static final String getString(InputStream is) throws IOException {
        StringBuffer buf = new StringBuffer();

        for (int i = 0; i < 4; i++) {
            buf.append((char)is.read());
        }

        return buf.toString();
    }

	public IndirectObject appendToDocument(PDFDocument document) throws InvalidImageException {
		IndirectObject paramsObject = null;
        if (!hasAlphaChannel) {
            paramsObject = document.newIndirectObject();
		    document.includeIndirectObject(paramsObject);
		    paramsObject.addDictionaryContent(
			        " /Predictor " + (isInterlaced ? 1 : 15)+ "\n" +
			        " /Colors " + colors + "\n" +
                    " /BitsPerComponent " + bits + "\n" +
			        " /Columns " + width + "\n" +
			        " /Length 0\n"
		    );
        }

        IndirectObject paletteObject = null;
        if (hasPalette) {
            paletteObject = document.newIndirectObject();
		    document.includeIndirectObject(paletteObject);
		    paletteObject.addDictionaryContent(
			        " /Length " + palette.length + "\n"
            );
            paletteObject.setStream(palette);
        }

        String mask = null;
        if (trans_gray != null) {
            mask = trans_gray[0] + " " + trans_gray[0];
        }
        else if (trans_rgb != null) {
            mask = trans_rgb[0] + " " + trans_rgb[0];
            int i = 1;
            while (i < trans_rgb.length) {
                mask += " " + trans_rgb[i] + " " + trans_rgb[i];
                i++;
            }
        }
        else if (trans_indexed != null) {
            throw new InvalidImageException("Indexed color not supported");
        }
        else if (hasAlphaChannel) {
            throw new InvalidImageException("Alpha channel not supported");
        }

        if (isInterlaced) {
            throw new InvalidImageException("Interlace not supported");
        }

		IndirectObject maskObject = null;
        if (alphaChannel != null) {
            maskObject = document.newIndirectObject();
		    document.includeIndirectObject(maskObject);
		    maskObject.addDictionaryContent(
			    " /Type /XObject\n" +
			    " /Subtype /Image\n" +
			    " /Width " + width + "\n" +
			    " /Height " + height + "\n" +
			    " /BitsPerComponent 8\n" +
                " /Filter /FlateDecode\n" +
                " /ColorSpace /DeviceGray\n" +
                " /Decode [0 1]" +
			    " /Length " + alphaChannel.length + "\n"
            );
            maskObject.setStream(alphaChannel);
        }

		IndirectObject indirectObject = document.newIndirectObject();
		document.includeIndirectObject(indirectObject);
		indirectObject.addDictionaryContent(
			" /Type /XObject\n" +
			" /Subtype /Image\n" +
			" /BitsPerComponent " + (hasAlphaChannel ? 8 : bits) + "\n" +
			" /Width " + width + "\n" +
			" /Height " + height + "\n" +
            " /Filter /FlateDecode\n" +
            (!hasAlphaChannel ? " /DecodeParms "
                    + paramsObject.getIndirectReference() + "\n" : "") +   // must not use bracket
			" /ColorSpace " + (hasPalette ? "[/Indexed /DeviceRGB " + (palette.length / 3 - 1)
                    + " " + paletteObject.getIndirectReference() + "]" : colorSpace) + "\n" +
            (mask != null ? " /Mask [" + mask + "]\n" : "") + 
            (alphaChannel != null ? " /SMask " + maskObject.getIndirectReference() + "\n" : "") +
			" /Length " + imgData.length + "\n"
		);
		indirectObject.setStream(imgData);
        return indirectObject;
    }
}
