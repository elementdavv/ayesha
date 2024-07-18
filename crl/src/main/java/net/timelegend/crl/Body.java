package net.timelegend.crl;
//
//  Android PDF Writer
//  http://coderesearchlabs.com/androidpdfwriter
//
//  by Javier Santo Domingo (j-a-s-d@coderesearchlabs.com)
//

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

public class Body extends Lst {

	private int mByteOffsetStart;
	private int mObjectNumberStart;
	private int mGeneratedObjectsCount;
	private ArrayList<IndirectObject> mObjectsList;
    private int mObjectCount;
    private CrossReferenceTable mCRT;
	
	public Body(CrossReferenceTable mCRT) {
		super();
		clear();
        this.mCRT = mCRT;
	}
	
	public int getObjectNumberStart() {
		return mObjectNumberStart;
	}
	
	public void setObjectNumberStart(int Value) {
		mObjectNumberStart = Value;
	}
	
	public int getByteOffsetStart() {
		return mByteOffsetStart;
	}
	
	public void setByteOffsetStart(int Value) {
		mByteOffsetStart = Value;
	}
	
	public int getObjectsCount() {
		return mObjectCount;
	}
	
	private int getNextAvailableObjectNumber() {
		return ++mGeneratedObjectsCount + mObjectNumberStart;
	}
	
	public IndirectObject getNewIndirectObject() {
		return getNewIndirectObject(getNextAvailableObjectNumber(), 0, true);
	}
	
	public IndirectObject getNewIndirectObject(int Number, int Generation, boolean InUse) {
		IndirectObject iobj = new IndirectObject();
		iobj.setNumberID(Number);
		iobj.setGeneration(Generation);
		iobj.setInUse(InUse);
		return iobj;
	}
	
	public IndirectObject getObjectByNumberID(int Number) {
		IndirectObject iobj;
		int x = 0;
		while (x < mObjectsList.size()) {
			iobj = mObjectsList.get(x);
			if (iobj.getNumberID() == Number)
				return iobj;
			x++;
		}
		return null;
	}
	
	public void includeIndirectObject(IndirectObject iobj) {
		mObjectsList.add(iobj);
        mObjectCount++;
	}

    public int flush(OutputStream os) throws IOException {
		int x = 0;
		int offset = mByteOffsetStart;
        int writen = 0;
		while (x < mGeneratedObjectsCount + mObjectNumberStart) {
			String s = "";
			IndirectObject iobj = getObjectByNumberID(++x);
			if (iobj != null) {
                writen = iobj.flush(os);
			    iobj.setByteOffset(offset);
				mCRT.addObjectXRefInfo(iobj.getByteOffset(), iobj.getGeneration(), iobj.getInUse());
			    offset += writen;
            }
		}
        writen = offset - mByteOffsetStart;
        mByteOffsetStart = offset;
		mObjectsList.clear();
		return writen;
    }

	@Override
	public String toPDFString() {
		return null;
	}
	
	@Override
	public void clear() {
		super.clear();
		mByteOffsetStart = 0;
		mObjectNumberStart = 0;
		mGeneratedObjectsCount = 0;
		mObjectsList = new ArrayList<IndirectObject>();
	}
}
