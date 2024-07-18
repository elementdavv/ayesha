package net.timelegend.crl;
//
//  Android PDF Writer
//  http://coderesearchlabs.com/androidpdfwriter
//
//  by Javier Santo Domingo (j-a-s-d@coderesearchlabs.com)
//

import java.util.Collection;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

public class Pages {

	private PDFDocument mDocument;
	private IndirectObject mIndirectObject;
	private Array mMediaBox;
	private Array mKids;
    private SortedMap<Integer, String> kids;
	private Page mCurrentPage;
	
	public Pages(PDFDocument document, int pageWidth, int pageHeight) {
		mDocument = document;
		mIndirectObject = mDocument.newIndirectObject();
		mMediaBox = new Array();
		String content[] = {"0", "0", Integer.toString(pageWidth), Integer.toString(pageHeight)};
		mMediaBox.addItemsFromStringArray(content);
		mKids = new Array();
        kids = new TreeMap<>();
	}
	
	public IndirectObject getIndirectObject() {
		return mIndirectObject;
	}
	
	public Page newPage(int index) {
		Page lPage = new Page(mDocument);
        kids.put(index, lPage.getIndirectObject().getIndirectReference());
        mCurrentPage = lPage;
		return lPage;
	}
	
    public Page newPage(int index, int pageWidth, int pageHeight) {
		Page lPage = new Page(mDocument, pageWidth, pageHeight);
        kids.put(index, lPage.getIndirectObject().getIndirectReference());
        mCurrentPage = lPage;
		return lPage;
    }

	public int getCount() {
        return kids.size();
	}
	
    public void flush() {
        mCurrentPage.render(mIndirectObject.getIndirectReference());
        mCurrentPage = null;
    }

    private void renderKids() {
        Collection<String> values = kids.values();
        Iterator<String> it = values.iterator();
        while(it.hasNext()) {
            String value = it.next();
            mKids.addItem(value);
        }
    }

	public void render() {
        renderKids();
		mIndirectObject.setDictionaryContent(
				"  /Type /Pages\n" +
				"  /MediaBox " + mMediaBox.toPDFString() + "\n" +
				"  /Count " + kids.size() + "\n" +
				"  /Kids " + mKids.toPDFString() + "\n"
		);
	}
}
