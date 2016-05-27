package org.renjin.cran.digest;

import com.google.common.hash.Hasher;

import java.io.IOException;
import java.io.OutputStream;

public class SkippingHashingOutputStream extends OutputStream {

  private final Hasher hasher;
  private final int bytesToSkip;
  private int bytesWritten = 0;

  public SkippingHashingOutputStream(Hasher hasher, int bytesToSkip) {
    this.hasher = hasher;
    this.bytesToSkip = bytesToSkip;
  }

  public void write(int b) throws IOException {
    if(bytesWritten >= bytesToSkip) {
      hasher.putByte((byte) b);
    } else {
      bytesWritten++;
    }
  }

  public void write(byte[] b, int off, int len) throws IOException {
    if(bytesWritten >= bytesToSkip) {
      hasher.putBytes(b, off, len);
    
    } else if(bytesWritten+len >= bytesToSkip) {
      int bytesStillToSkip = (bytesToSkip-bytesWritten);
      hasher.putBytes(b, off+bytesStillToSkip, len - bytesStillToSkip);
      bytesWritten += bytesStillToSkip;
      
    } else {
      bytesWritten += len;
    }
  }
}
