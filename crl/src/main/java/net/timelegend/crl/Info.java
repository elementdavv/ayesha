package net.timelegend.crl;
//
//  Android PDF Writer
//  http://github.com/elementdavv/pdfwriter
//
//  by Element Davv (elementdavv@hotmail.com)
//

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Info {

    private IndirectObject mIndirectObject;

    public Info(PDFDocument mDocument, Map<String, String> info) {
		mIndirectObject = mDocument.newIndirectObject();
        
        if (info != null) {
            Set<String> keys = info.keySet();
            Iterator<String> it = keys.iterator();

            while (it.hasNext()) {
                String key = it.next();
                IndirectObject iobj = mDocument.newIndirectObject();
                iobj.setMeta(info.get(key));
		        mDocument.includeIndirectObject(iobj);
                mIndirectObject.addDictionaryContent("/" + key + " " + iobj.getIndirectReference() + "\n");
            }
        }

		mDocument.includeIndirectObject(mIndirectObject);
    }

    public IndirectObject getIndirectObject() {
        return mIndirectObject;
    }
}
