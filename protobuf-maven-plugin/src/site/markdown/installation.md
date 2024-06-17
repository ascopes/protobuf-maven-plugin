# Minimum requirements

This plugin requires that you meet the following requirements at a minimum:

- Apache Maven 3.8.2 or newer
- Java 11 or newer

While this plugin itself will work with many operating systems and architectures,
you will find that the official releases of `protoc` by Google only support mainstream
architectures and operating systems. These are listed below:

<table>
  <thead>o
    <tr>
      <th>Operating system</th>
      <th>Supported CPU architectures</th>
      <th>Command to check</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>Linux</td>
      <td>
        <ul>
          <li>amd64 (x86_64)</li>
          <li>aarch64</li>
          <li>ppc64le (PowerPC)</li>
          <li>s390x (zarch_64)</li>
        </ul>
      </td>
      <td>
        <code>uname -m</code>
      </td>
    </tr>
    <tr>
      <td>Mac OS</td>
      <td>
        <ul>
          <li>amd64 (x86_64)</li>
          <li>aarch64</li>
        </ul>
      </td>
      <td>
        <code>uname -m</code>
      </td>
    </tr>
    <tr>
      <td>Windows</td>
      <td>
        <ul>
          <li>amd64 (x86_64)</li>
          <li>x86 (x86_32)</li>
        </ul>
      </td>
      <td>
        <code>echo %PROCESSOR_ARCHITECTURE%</code>
      </td>
    </tr>
  </tbody>
</table>

## Other platforms

### Android

While technically Linux, Android locks down a number of system calls that the
official build of `protoc` will rely on. For this reason, you will need to install
or build a custom version of `protoc`, such as the one provided on the Termux
repositories.

Refer to the `protobufCompiler` parameter documentation on the
[goals page](plugin-info.html) for how you can override the binary being used.

### Windows on ARM

As of right now, no official binaries for `aarch64` Windows systems are provided.
You may be able to use x86 emulation to get this to work. To do this, refer to
the Microsoft ARM emulation documentation.

Pass the `-Dos.arch=x86_64 -Dos.name=Windows` flag to Maven to force the use of the
amd64 binary for Windows.

Alternatively, you can build `protoc` yourself. Refer to the `protobufCompiler` 
parameter documentation on the [goals page](plugin-info.html) for how you can override
the binary being used.

### BSDs, Linux on unsupported CPUs, MINIX, Solaris, etc

Your best bet is to use a prebuilt version of `protoc` for your platform if provided
by your package vendor. Alternatively, you could try `binfmt` emulation or QEMU emulation
and pass the `-Dos.arch=aarch64 -Dos.name=Linux` flag.

Refer to the `protobufCompiler` parameter documentation on the
[goals page](plugin-info.html) for how you can override the binary being used.

You may also try something like `docker buildx` and use a cross-platform container to build
on the desired platform. Virtual machines are also an option.

### PowerPC Macs

Use a custom build of `protoc` if available.

Refer to the `protobufCompiler` parameter documentation on the
[goals page](plugin-info.html) for how you can override the binary being used.

### Mainframes from 1980 running in a dark room somewhere

If not on the supported list of operating systems and platforms, and not covered
by the alternatives above, then you may need to consider a different platform
to build your applications.

### Quantum computers

As far as I know, Java does not work on a quantum computer, so you are on your own.

### Toasters and Smart Fridges

If they run Linux, you should be covered by the above.

I haven't personally tried building with this plugin on a toaster running NetBSD.

Feel free to contribute documentation.

### Potatos and other vegetables

You should probably consider using a different platform as the acids from the organic
material will probably make your peripherals sticky. GitHub Codespaces are free to
use and run in a browser, so give those a try.

