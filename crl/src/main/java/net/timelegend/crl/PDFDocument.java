package net.timelegend.crl;
//
//  Android PDF Writer
//  http://coderesearchlabs.com/androidpdfwriter
//
//  by Javier Santo Domingo (j-a-s-d@coderesearchlabs.com)
//

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public class PDFDocument extends Base {

	private Header mHeader;
	private Body mBody;
	private CrossReferenceTable mCRT;
	private Trailer mTrailer;
    private OutputStream os;
    private int writen;
	
	public PDFDocument(OutputStream os) throws IOException {
        this.os = os;
		mHeader = new Header();
		mCRT = new CrossReferenceTable();
		mBody = new Body(mCRT);
		mBody.setByteOffsetStart(mHeader.getPDFStringSize());
		mBody.setObjectNumberStart(0);
		mTrailer = new Trailer();
        flushHead();
	}

    private void flushHead() throws IOException {
        byte[] b= mHeader.toPDFString().getBytes("ISO-8859-1");
        os.write(b);
        writen += b.length;
    }
	
	public IndirectObject newIndirectObject() {
		return mBody.getNewIndirectObject();
	}
	
	public IndirectObject newRawObject(String content) {
		IndirectObject iobj = mBody.getNewIndirectObject();
		iobj.setContent(content);
		return iobj;
	}
	
	public IndirectObject newDictionaryObject(String dictionaryContent) {
		IndirectObject iobj = mBody.getNewIndirectObject();
		iobj.setDictionaryContent(dictionaryContent);
		return iobj;
	}
	
	public IndirectObject newStreamObject(String streamContent) {
		IndirectObject iobj = mBody.getNewIndirectObject();
		iobj.setDictionaryContent("  /Length " + streamContent.length() + "\n");
		iobj.setStreamContent(streamContent);
		return iobj;
	}
	
	public void includeIndirectObject(IndirectObject iobj) {
		mBody.includeIndirectObject(iobj);
	}

    public void flush() throws IOException {
        writen += mBody.flush(os);
    }
	
    public void endInfo(Info info) {
        mTrailer.setInfoReference(info.getIndirectObject().getIndirectReference());
    }

    public void end() throws IOException {
        os.write(toPDFString().getBytes("ISO-8859-1"));
        os.close();
        os = null;
    }

	@Override
	public String toPDFString() {
		// StringBuilder sb = new StringBuilder();
		// sb.append(mHeader.toPDFString());
		// sb.append(mBody.toPDFString());
		mCRT.setObjectNumberStart(mBody.getObjectNumberStart());
		// int x = 0;
		// while (x < mBody.getObjectsCount()) {
		// 	IndirectObject iobj = mBody.getObjectByNumberID(++x);
		// 	if (iobj != null) {
		// 		mCRT.addObjectXRefInfo(iobj.getByteOffset(), iobj.getGeneration(), iobj.getInUse());
		// 	}
		// }
		mTrailer.setObjectsCount(mBody.getObjectsCount() + 1);
		// mTrailer.setCrossReferenceTableByteOffset(sb.length());
		mTrailer.setCrossReferenceTableByteOffset(writen);
		mTrailer.setId(Indentifiers.generateId());
		// return sb.toString() + mCRT.toPDFString() + mTrailer.toPDFString();
		return mCRT.toPDFString() + mTrailer.toPDFString();
	}
	
	@Override
	public void clear() {
		mHeader.clear();
		mBody.clear();
		mCRT.clear();
		mTrailer.clear();
	}
}
