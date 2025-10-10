## steps to reproduce

### clone
git clone https://github.com/buuuuuuug/pycrypto_graalpy.git

### versions
mvn --version
```
Apache Maven 3.9.11 (3e54c93a704957b63ee3494413a2b544fd3d825b)
Maven home: /root/.sdkman/candidates/maven/current
Java version: 25, vendor: Oracle Corporation, runtime: /root/.sdkman/candidates/java/25-graal
Default locale: en_US, platform encoding: UTF-8
OS name: "linux", version: "6.8.0-40-generic", arch: "amd64", family: "unix"
```

### test

```bash
mvn test
```

test cases:
```java
public class EmbeddingTest {
	
	@Test
	public void testSuccess() throws IOException {
		// success
		Main.main(new String[0]);
	}
}
```
