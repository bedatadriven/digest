package org.renjin.cran.digest;


import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import org.renjin.eval.Context;
import org.renjin.eval.EvalException;
import org.renjin.invoke.annotations.Current;
import org.renjin.primitives.io.connections.Connection;
import org.renjin.primitives.io.serialization.RDataWriter;
import org.renjin.sexp.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Produces digests of R objects using builtin Java hashing algorithms
 */
public class Digest {

  private static final int SERIALIZATION_HEADER_SIZE = 14;

  public static SEXP hashSerializedSexp(@Current Context context, SEXP object, String algorithm,
                                        boolean leaveRaw, SEXP skip) throws IOException {


    Hasher hasher = forAlgorithm(algorithm).newHasher();
    
    int bytesToSkip = computeSkip(skip, SERIALIZATION_HEADER_SIZE);

    try(OutputStream digestStream = new SkippingHashingOutputStream(hasher, bytesToSkip)) {
      RDataWriter writer = new RDataWriter(context, digestStream);
      writer.serialize(object);
    }

    return buildSexpResult(hasher.hash(), leaveRaw);
  }

  /**
   * Hashes a character or raw vector
   */
  public static SEXP hashVector(SEXP object, String algorithm, boolean leaveRaw, SEXP skip) {
    HashFunction hashFunction = forAlgorithm(algorithm);
    
    int bytesToSkip = computeSkip(skip, 0);

    byte[] input = new byte[0];
    if(object instanceof StringVector) {
      if(object.length() > 0) {
        String string = ((StringVector) object).getElementAsString(0);
        if (StringVector.isNA(string)) {
          string = "NA";
        }
        input = string.getBytes(Charsets.UTF_8);
      }
    } else if(object instanceof RawVector) {
      if(object.length() > 0) {
        input = ((RawVector) object).toByteArray();
      }
    }

    HashCode hashCode = hashFunction.hashBytes(input, bytesToSkip, input.length - bytesToSkip);

    return buildSexpResult(hashCode, leaveRaw);
  }



  public static SEXP hashFile(@Current Context context, IntVector connectionHandle, String algorithm, boolean ascii,
                              boolean leaveRaw, int skip) throws IOException {

    if(ascii) {
      throw new EvalException("ASCII mode not supported");
    }
    Hasher hasher = forAlgorithm(algorithm).newHasher();

    Connection connection = context.getSession().getConnectionTable().getConnection(connectionHandle);
    InputStream inputStream = connection.getInputStream();
    
    // Skip initial bytes if asked
    long bytesSkipped = inputStream.skip(skip);
    if(bytesSkipped != skip) {
      throw new EvalException("Failed to skip first " + skip + " bytes");
    }

    // Hash the rest of the file
    byte[] buffer = new byte[1024];
    while(true) {
      int bytesRead = inputStream.read(buffer, 0, 1024);
      if (bytesRead == -1) {
        // EOF
        break;
      }
      hasher.putBytes(buffer, 0, bytesRead);
    }

    return buildSexpResult(hasher.hash(), leaveRaw);
  }


  private static HashFunction forAlgorithm(String algorithm) {
    switch (algorithm) {
      case "md5":
        return Hashing.md5();
      case "sha1":
        return Hashing.sha1();
      case "crc32":
        return Hashing.crc32();

      case "sha256":
        return Hashing.sha256();

      case "sha512":
        return Hashing.sha512();
      
      case "murmur32":
        return Hashing.murmur3_32();
      
      case "xxhash32":
      case "xxhash64":
      default:
        throw new EvalException(algorithm + " not yet implemented.");
    }
  }

  private static int computeSkip(SEXP skip, int defaultValue) {
    if(skip instanceof StringVector &&
        skip.length() >= 1 &&
        "auto".equals(((StringVector) skip).getElementAsString(0))) {

      return defaultValue;
    }

    if(skip instanceof AtomicVector && skip.length() >= 1) {
      int toSkip = ((AtomicVector) skip).getElementAsInt(0);
      if(toSkip < 0) {
        throw new EvalException("skip count cannot be negative: " + toSkip);
      }
      return toSkip;
    }
    throw new EvalException("invalid skip argument: " + skip);
  }

  private static SEXP buildSexpResult(HashCode hashCode, boolean leaveRaw) {
    if(leaveRaw) {
      return new RawVector(hashCode.asBytes());
    } else {
      return new StringArrayVector(hashCode.toString());
    }
  }
}
