package org.example.embedding;

import org.apache.commons.codec.binary.Hex;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.python.embedding.GraalPyResources;
import org.graalvm.python.embedding.VirtualFileSystem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/**
 * A basic polyglot application that tries to exercise a simple hello world style program in all installed languages.
 */
public class Main {

    public static void main(String[] args) throws IOException {
        Path externalResourceDirectoryPath = Paths.get("fs");
        GraalPyResources.extractVirtualFileSystemResources(VirtualFileSystem.create(), externalResourceDirectoryPath);
        try (Context context = GraalPyResources
                .contextBuilder(externalResourceDirectoryPath)
                .option("python.PosixModuleBackend", "native")
                .option("python.IsolateNativeModules", "true")
                .allowAllAccess(true)
                .allowExperimentalOptions(true)
                // 避免关于 native module 的告警信息
                .option("python.WarnExperimentalFeatures", "false").build()) {
            Set<String> languages = context.getEngine().getLanguages().keySet();
            for (String id : languages) {
                System.out.println("Initializing language " + id);
                context.initialize(id);
                if (id.equals("python")) {
                    context.eval("python", "print('#####################')");
                    context.eval("python", "import ijson");
                    if (args.length > 0) {
                        System.out.println("got here");
                        context.eval("python", """
                            import psutil
                            pids = psutil.pids()
                            print(len(pids))
                            """);
                    }
                    var evalued = context.eval("python", """
                            import binascii
                            def decryptAES_cryptography(data, key, salt):
                                data = binascii.unhexlify(data)
                                key = binascii.unhexlify(key)
                                iv = binascii.unhexlify(salt)  # 在CBC模式中，salt 用作 IV
                                from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
                                from cryptography.hazmat.backends import default_backend
                            
                                backend = default_backend()
                                cipher = Cipher(algorithms.AES(key), modes.CBC(iv), backend=backend)
                                decryptor = cipher.decryptor()
                                decrypted = decryptor.update(data) + decryptor.finalize()
                                return binascii.hexlify(decrypted).decode('utf-8')
                            
                            def decryptDES_cryptography(data, key):
                                data = binascii.unhexlify(data)
                                key = binascii.unhexlify(key)
                                from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
                                from cryptography.hazmat.backends import default_backend
                            
                                backend = default_backend()
                                cipher = Cipher(algorithms.TripleDES(key), modes.ECB(), backend=backend)
                                decryptor = cipher.decryptor()
                                decrypted = decryptor.update(data) + decryptor.finalize()
                                return binascii.hexlify(decrypted).decode('utf-8')
                            """);
                    String key = "2b7e151628aed2a6abf7158809cf4f3c";  // 128-bit key in hex
                    String iv = "6bc1bee22e409f96e93d7e117393172a";//  # IV in hex
                    String ciphertext = "679647F478987F56FFF83C7C934567A6625F4335D0A2D981B5C7E86C6124A628";
                    String expected_plaintext = new String(Hex.encodeHex("Hello World !!!!!!".getBytes(StandardCharsets.UTF_8)));// #"Hello World !!!!!!"
                    Value executed = evalued.getMember("decryptAES_cryptography").execute(ciphertext, key, iv);
                    String aesDecrypted = executed.asString();
                    System.out.println(aesDecrypted);
                    assert aesDecrypted.startsWith(expected_plaintext);

                    String key2 = "7364667364667373";  // 8-bytes key in hex
                    String ciphertext2 = "d39c2f8e91d3da40";//  # encrypted "hello" (padded)
                    String expected_plaintext2 = new String(Hex.encodeHex("hello".getBytes(StandardCharsets.UTF_8)));
                    String desDecrypted = evalued.getMember("decryptDES_cryptography").execute(ciphertext2, key2).asString();
                    System.out.println(desDecrypted);
                    assert desDecrypted.startsWith(expected_plaintext2);

                }
            }
        }
    }
}
