//
//  Android PDF Writer
//  http://github.com/elementdavv/pdfwriter
//
//  by Element Davv (elementdavv@hotmail.com)
//

package crl.android.pdfwriter;

public class InvalidImageException extends Exception {
	public InvalidImageException(String errMessage) {
        super(errMessage);
	}

	public InvalidImageException(String errMessage, Throwable err) {
        super(errMessage, err);
	}
}
