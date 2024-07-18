package net.timelegend.crl;
//
//  Android PDF Writer
//  http://coderesearchlabs.com/androidpdfwriter
//
//  by Javier Santo Domingo (j-a-s-d@coderesearchlabs.com)
//

public class Stream extends EnclosedContent {

    protected byte[] mStream;
    protected boolean hasStream;

	public Stream() {
		super();
		setBeginKeyword("stream",false,true);
		setEndKeyword("endstream",false,true);
	}

    public void setStream(byte[] value) {
        mStream = value;
        hasStream = true;
    }

    public byte[] getStream() {
        return mStream;
    }

    public boolean hasStream() {
        return hasStream;
    }

}
