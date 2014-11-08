/*
 * Copyright 2014 Miles Chaston
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.chaston.oakfunds.xsrf;

import org.apache.commons.codec.binary.Base64;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.Certificate;

/**
 * TODO(mchaston): write JavaDocs
 */
public class XsrfSigner {
  private static final Charset CHARSET = Charset.forName("UTF-8");
  private final SecureRandom secureRandom = new SecureRandom();
  private final Certificate certificate;
  private final PrivateKey privateKey;

  XsrfSigner() throws IOException, GeneralSecurityException {
    this.certificate = loadPublicKey();
    this.privateKey = loadPrivateKey();
  }

  public String sign(String material) throws GeneralSecurityException {
    Signature sig = Signature.getInstance("SHA256withRSA");
    sig.initSign(privateKey, secureRandom);
    sig.update(material.getBytes(CHARSET));
    return Base64.encodeBase64String(sig.sign());
  }

  public boolean verify(String material, String signature) throws GeneralSecurityException {
    Signature sig = Signature.getInstance("SHA256withRSA");
    sig.initVerify(certificate);
    sig.update(material.getBytes(CHARSET));
    return sig.verify(Base64.decodeBase64(signature));
  }

  /*
   * The following keys were created using:
   * keytool -genkeypair -alias xsrf -keyalg RSA -keypass ofxsrf -storetype pkcs12 \
   *     -keystore ~/Desktop/Projects/OakFunds/src/META-INF/secrets/xsrf.p12 \
   *     -storepass ofxsrf
   */

  private Certificate loadPublicKey() throws IOException, GeneralSecurityException {
    InputStream keyStream = getClass().getClassLoader().getResourceAsStream(
        "META-INF/secrets/xsrf.p12");
    KeyStore ks = KeyStore.getInstance("PKCS12");
    ks.load(keyStream, "ofxsrf".toCharArray());
    return ks.getCertificate("xsrf");
  }

  private PrivateKey loadPrivateKey() throws IOException, GeneralSecurityException {
    InputStream keyStream = getClass().getClassLoader().getResourceAsStream(
        "META-INF/secrets/xsrf.p12");
    KeyStore ks = KeyStore.getInstance("PKCS12");
    ks.load(keyStream, "ofxsrf".toCharArray());
    return (PrivateKey) ks.getKey("xsrf", "ofxsrf".toCharArray());
  }
}
