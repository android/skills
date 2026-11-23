<br />

**OWASP category:** [MASVS-CODE: Code Quality](https://mas.owasp.org/MASVS/10-MASVS-CODE)

## Overview

Android applications can take advantage of native code written in languages like
C and C++ for specific functionalities. However, when an application utilizes
the Java Native Interface (JNI) to interact with this native code, it
potentially exposes itself to vulnerabilities like buffer overflows and other
issues that may be present in the native code implementation.

## Impact

Despite very positive impacts such as performance optimization and obfuscation,
utilizing native code in Android applications can have negative security
impacts. Native code languages like C/C++ lack the memory safety features of
Java/Kotlin, making them susceptible to vulnerabilities like buffer overflows,
use-after-free errors, and other memory corruption issues -- leading to crashes
or arbitrary code execution. Additionally, if a vulnerability exists in the
native code component, it can potentially compromise the entire application,
even if the rest is written securely in Java.

## Mitigations

### Development and coding guidance

- **Secure Coding Guidelines**: For C/C++ projects, adhere to established secure coding standards (e.g., CERT, OWASP) to mitigate vulnerabilities like buffer overflows, integer overflows, and format string attacks. Prioritize libraries like Abseil known for quality and security. Whenever possible, consider adopting memory-safe languages like Rust, which offer performance comparable to C/C++.
- **Input Validation**: Rigorously validate all input data received from external sources, including user input, network data, and files, to prevent injection attacks and other vulnerabilities.

### Harden the compilation options

Native libraries utilizing the ELF format can be hardened against a range of
vulnerabilities by activating protective mechanisms like stack protection
(Canary), relocation read-only (RELRO), data execution prevention (NX), and
position-independent executables (PIE). Conveniently, the Android NDK
compilation options already enable all these protections by default.

To verify the implementation of these security mechanisms within a binary, you
can employ tools like `hardening-check` or `pwntools`.

### Bash

```bash
$ pwn checksec --file path/to/libnativecode.so
    Arch:     aarch64-64-little
    RELRO:    Full RELRO
    Stack:    Canary found
    NX:       NX enabled
    PIE:      PIE enabled
```

### Verify third-party libraries are not vulnerable

When choosing third-party libraries, prioritize using those with a solid
reputation in the development community. Resources like the [Google Play SDK
Index](https://play.google.com/sdks) can help you identify well-regarded and trustworthy libraries. Ensure
you keep the libraries updated to the latest versions and proactively search for
any known vulnerabilities related to them using resources like the databases
from [Exploit-DB](https://www.exploit-db.com/). A web search using keywords like
`[library_name] vulnerability` or `[library_name] CVE` can reveal critical
security information.

## Resources

- [CWE-111: Direct Use of Unsafe JNI](https://cwe.mitre.org/data/definitions/111.html)
- [Exploit database](https://www.exploit-db.com/)
- [Check binaries for security hardening features](https://www.systutorials.com/docs/linux/man/1-hardening-check/)
- [Check binary security settings with pwntools](https://docs.pwntools.com/en/stable/commandline.html#pwn-checksec)
- [Linux binary security hardening](https://medium.com/@n80fr1n60/linux-binary-security-hardening-1434e89a2525)
- [Hardening ELF binaries using Relocation Read-Only (RELRO)](https://www.redhat.com/fr/blog/hardening-elf-binaries-using-relocation-read-only-relro)
- [OWASP binary protection mechanisms](https://mas.owasp.org/MASTG/Android/0x05i-Testing-Code-Quality-and-Build-Settings/#binary-protection-mechanisms)
- [SEI CERT Coding Standards](https://wiki.sei.cmu.edu/confluence/display/seccode/SEI+CERT+Coding+Standards)
- [OWASP Developer Guide](https://owasp.org/www-project-developer-guide/release/)
- [Google Play SDK Index](https://play.google.com/sdks)
- [Android NDK](https://developer.android.com/ndk)
- [Android Rust introduction](https://source.android.com/docs/setup/build/rust/building-rust-modules/overview)
- [Abseil (C++ Common Libraries)](https://github.com/abseil/abseil-cpp)
- [PIE is enforced by the linker](https://cs.android.com/android/platform/superproject/main/+/main:bionic/linker/linker_main.cpp;l=425?q=linker_main&ss=android/platform/superproject/main)