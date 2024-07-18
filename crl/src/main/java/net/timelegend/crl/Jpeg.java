package net.timelegend.crl;
//
//  Android PDF Writer
//  http://github.com/elementdavv/pdfwriter
//
//  by Element Davv (elementdavv@hotmail.com)
//

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Jpeg extends Image {
    protected int channels;

    private final static List<Integer> marks = Arrays.asList(new Integer[]
            {0xffc0, 0xffc1, 0xffc2, 0xffc3,
            0xffc5, 0xffc6, 0xffc7, 0xffc8,
            0xffc9, 0xffca, 0xffcb, 0xffcc,
            0xffcd, 0xffce, 0xffcf});

    private static final Map<Integer, String> color_space;
    static {
        Map<Integer, String> amap = new HashMap<>();
        amap.put(1, "/DeviceGray");
        amap.put(3, "/DeviceRGB");
        amap.put(4, "/DeviceCMYK");
        color_space = Collections.unmodifiableMap(amap);
    }

    public Jpeg(byte[] src) throws InvalidImageException {
        super(src);
        int mark = 0;
        int pos = 2;

        while (pos < src.length) {
            mark = getWord(src, pos);
            pos += 2;
            if (marks.contains(mark)) {
                break;
            }
            pos += getWord(src, pos);
        }

        if (!marks.contains(mark)) {
            throw new InvalidImageException("Invalid JPEG stream");
        }
        pos += 2;

        bits = src[pos++];
        height = getWord(src, pos);
        pos += 2;

        width = getWord(src, pos);
        pos += 2;

        channels = src[pos];
        colorSpace = color_space.get(channels);

        imgData = src;
    }

    public static int getWord(byte[] src, int pos) {
        return ((src[pos] & 0xFF) << 8) | (src[pos + 1] & 0xFF);
    }

	public IndirectObject appendToDocument(PDFDocument document) {
		IndirectObject indirectObject = document.newIndirectObject();
		document.includeIndirectObject(indirectObject);
		indirectObject.addDictionaryContent(
			" /Type /XObject\n" +
			" /Subtype /Image\n" +
            " /Filter /DCTDecode\n" +
			" /Width " + width + "\n" +
			" /Height " + height + "\n" +
			" /BitsPerComponent " + bits + "\n" +
			" /ColorSpace " + colorSpace + "\n" +
            (channels == 4 ? " /Decode [1.0 0.0 1.0 0.0 1.0 0.0 1.0 0.0]\n" : "") +
			" /Length " + imgData.length + "\n"
		);
		indirectObject.setStream(imgData);
        return indirectObject;
    }
}
