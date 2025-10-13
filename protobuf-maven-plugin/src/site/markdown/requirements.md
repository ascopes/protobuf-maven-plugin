# Requirements

<div id="pmp-toc"></div>

## Minimum requirements

This plugin requires that you meet the following requirements at a minimum:

- Apache Maven 3.9 or newer
- Java 17 or newer

While this plugin itself will work with many operating systems and architectures,
you will find that the official releases of `protoc` by Google only support mainstream
architectures and operating systems. These are listed below:

<table>
  <thead>
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
          <li>aarch64</li>
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

Refer to the `protocVersion` parameter documentation on the
[goals page](plugin-info.html) for how you can override the binary being used.

### Windows on ARM

As of right now, no official binaries for `aarch64` Windows systems are provided,
and there has been no sign of plans at the time of writing to support this from
Google.

Windows 11 ARM releases support x86 emulation via the Prism emulator.

To enable users to build on Windows ARM machines, this plugin will always download the
x86_64 release of `protoc` and any corresponding plugins, with the assumption that
emulation will work successfully.

### BSDs, Linux on unsupported CPUs, MINIX, Solaris, etc

Your best bet is to use a prebuilt version of `protoc` for your platform if provided
by your package vendor. Alternatively, you could try `binfmt` emulation or QEMU emulation
and pass the `-Dos.arch=aarch64 -Dos.name=Linux` flag.

Refer to the `protocVersion` parameter documentation on the
[goals page](plugin-info.html) for how you can override the binary being used.

You may also try something like `docker buildx` and use a cross-platform container to build
on the desired platform. Virtual machines are also an option.
