//
//  Android PDF Writer
//  http://coderesearchlabs.com/androidpdfwriter
//
//  by Javier Santo Domingo (j-a-s-d@coderesearchlabs.com)
//

package crl.android.pdfwriter;

import java.util.Collection;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

public class CrossReferenceTable extends Base {

	private int mObjectNumberStart;
    private SortedMap<Integer, String> crt;
	
	public CrossReferenceTable() {
        crt = new TreeMap<>();
		clear();
	}
	
	public void setObjectNumberStart(int Value) {
		mObjectNumberStart = Value;
	}
	
	public int getObjectNumberStart() {
		return mObjectNumberStart;
	}
	
	public void addObjectXRefInfo(IndirectObject iobj) {
        addObjectXRefInfo(iobj.getNumberID(), iobj.getByteOffset(), iobj.getGeneration(), iobj.getInUse());
    }

	public void addObjectXRefInfo(int numberID, int ByteOffset, int Generation, boolean InUse) {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("%010d", ByteOffset));
		sb.append(" ");
		sb.append(String.format("%05d", Generation));
		if (InUse) {
			sb.append(" n ");
		} else {
			sb.append(" f ");
		}
		sb.append("\n");
        crt.put(numberID, sb.toString());
	}

	private String getObjectsXRefInfo() {
        Collection<String> values = crt.values();
        Iterator<String> it = values.iterator();
		StringBuilder sb = new StringBuilder();

        while(it.hasNext()) {
            sb.append(it.next());
        }

        return sb.toString();
	}
	
	private String render() {
		StringBuilder sb = new StringBuilder();
		sb.append("xref");
		sb.append("\n");
		sb.append(mObjectNumberStart);
		sb.append(" ");
		sb.append(crt.size());
		sb.append("\n");
		sb.append(getObjectsXRefInfo());
		return sb.toString(); 
	}	
	
	@Override
	public String toPDFString() {
		return render();
	}

	@Override
	public void clear() {
		mObjectNumberStart = 0;
		crt.clear();
		addObjectXRefInfo(0, 0, 65536, false); // free objects linked list head
	}

}
