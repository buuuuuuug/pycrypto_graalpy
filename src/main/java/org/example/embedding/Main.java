/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.example.embedding;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.python.embedding.GraalPyResources;
import org.graalvm.python.embedding.VirtualFileSystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
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
                .allowAllAccess(true).build()) {
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
                            try:
                                from Crypto.Cipher import AES
                                from Crypto.Random import get_random_bytes
                                from Crypto.Protocol.KDF import PBKDF2
                                crypto_available = True
                            except ImportError:
                                crypto_available = False
                            
                            def start():
                                # Test PyCryptodome functionality
                                if not crypto_available:
                                    return "Error: PyCryptodome (Crypto) not available"
                            
                                try:
                                    # Test basic AES encryption
                                    key = get_random_bytes(16)
                                    cipher = AES.new(key, AES.MODE_EAX)
                                    data = "Hello, World!".encode('utf-8')
                                    ciphertext, tag = cipher.encrypt_and_digest(data)
                            
                                    # Test PBKDF2
                                    password = "my_password"
                                    salt = get_random_bytes(16)
                                    derived_key = PBKDF2(password, salt, 32)
                            
                                    result = {
                                        "aes_key_length": len(key),
                                        "ciphertext_length": len(ciphertext),
                                        "tag_length": len(tag),
                                        "derived_key_length": len(derived_key),
                                        "crypto_version": "Available"
                                    }
                            
                                    return result
                                except Exception as e:
                                    return f"Error: {str(e)}"
                            """);
                    Value executed = evalued.getMember("start").execute();
                    Map<String, Object> map = executed.as(Map.class);
                    System.out.println(map);
                }
            }
        }
    }
}
