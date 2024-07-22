//
//  Android PDF Writer
//  http://coderesearchlabs.com/androidpdfwriter
//
//  by Javier Santo Domingo (j-a-s-d@coderesearchlabs.com)
//

package crl.android.pdfwriter;

public class Header extends Base {

	private String mVersion;
	private String mRenderedHeader;
	
	public Header() {
		clear();
	}
	
	public void setVersion(int Major, int Minor) {
		mVersion = Integer.toString(Major) + "." + Integer.toString(Minor);
		render();
	}
	
	private void render() {
        char[] c = {0x0a, 0x25, 0xff, 0xff, 0xff, 0xff, 0x0a};
        String bs = String.valueOf(c);
		mRenderedHeader = "%PDF-" + mVersion + bs;
	}
	
	public int getPDFStringSize() {
		return mRenderedHeader.length();
	}
	
	@Override
	public String toPDFString() {
		return mRenderedHeader;
	}

	@Override
	public void clear() {
		setVersion(1, 4);
	}

}
