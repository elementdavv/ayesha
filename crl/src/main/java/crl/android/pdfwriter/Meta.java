//
//  Android PDF Writer
//  http://github.com/elementdavv/pdfwriter
//
//  by Element Davv (elementdavv@hotmail.com)
//

package crl.android.pdfwriter;

public class Meta extends EnclosedContent {
	
	public Meta() {
		super();
		setBeginKeyword("(",false,false);
		setEndKeyword(")",false,true);
	}
	
}
