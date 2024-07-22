//
//  Android PDF Writer
//  http://github.com/elementdavv/pdfwriter
//
//  by Element Davv (elementdavv@hotmail.com)
//

package crl.android.pdfwriter;

import android.util.Pair;

import java.util.HashMap;
import java.util.Map;

public class Gstate {
    private static Map<String, Pair<String, IndirectObject>> mGstatesList = new HashMap<>();

    /*
     * opaque: 0.0 invisible
     */
    public static Pair<String, IndirectObject> setOpaque(PDFDocument document, Double opaque) {
        opaque = Math.max(0.0, Math.min(1.0, opaque));
        String opaquestr = String.valueOf(opaque);
        String key = opaquestr + "_" + opaquestr;

        if (mGstatesList.containsKey(key)) {
            return mGstatesList.get(key);
        }

        int id = mGstatesList.size();
        String name = "Gs" + String.valueOf(++id);
		IndirectObject iobj = document.newIndirectObject();
		document.includeIndirectObject(iobj);
        iobj.setDictionaryContent("  /Type /ExtGState\n" + "  /ca " + opaquestr + "\n" + "  /CA " + opaquestr + "\n");
        Pair pair = Pair.create(name, iobj);
        mGstatesList.put(key, pair);

        return pair;
    }
}
