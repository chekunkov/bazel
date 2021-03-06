// Copyright 2017 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.build.lib.packages.util;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import com.google.devtools.build.lib.testutil.TestConstants;
import com.google.devtools.build.lib.view.config.crosstool.CrosstoolConfig;
import com.google.devtools.build.lib.view.config.crosstool.CrosstoolConfig.CToolchain;
import com.google.protobuf.TextFormat;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Creates mock BUILD files required for the objc rules.
 */
public final class MockObjcSupport {

  private static final ImmutableList<String> DEFAULT_OSX_CROSSTOOL_DEPS_DIRS =
      ImmutableList.of("third_party/bazel/tools/osx/crosstool");
  private static final String DEFAULT_OSX_CROSSTOOL_DIR = "tools/osx/crosstool";
  private static final String MOCK_OSX_CROSSTOOL_FILE =
      "com/google/devtools/build/lib/packages/util/MOCK_OSX_CROSSTOOL";
  /**
   * The build label for the mock OSX crosstool configuration.
   */
  public static final String DEFAULT_OSX_CROSSTOOL =
      "//" + DEFAULT_OSX_CROSSTOOL_DIR + ":crosstool";

  public static final String DEFAULT_XCODE_VERSION = "7.3.1";
  public static final String DEFAULT_IOS_SDK_VERSION = "8.4";

  /**
   * Sets up the support for building ObjC.
   * Any partial toolchain line will be merged into every toolchain stanza in the crosstool
   * loaded from file.
   */
  public static void setup(
      MockToolsConfig config, String... partialToolchainLines) throws IOException {
    for (String tool :
        ImmutableSet.of(
            "actoolwrapper",
            "bundlemerge",
            "objc_dummy.mm",
            "environment_plist.sh",
            "gcov",
            "ibtoolwrapper",
            "momcwrapper",
            "plmerge",
            "realpath",
            "swiftstdlibtoolwrapper",
            "testrunner",
            "xcrunwrapper",
            "mcov",
            "libtool")) {
      config.create("tools/objc/" + tool);
    }
    config.create(
        "tools/objc/BUILD",
        "package(default_visibility=['//visibility:public'])",
        "exports_files(glob(['**']))",
        "filegroup(name = 'default_provisioning_profile', srcs = ['foo.mobileprovision'])",
        "filegroup(name = 'compile_protos', srcs = ['compile_protos.py'])",
        "filegroup(name = 'protobuf_compiler_wrapper', srcs = ['protobuf_compiler_wrapper.sh'])",
        "filegroup(name = 'protobuf_compiler', srcs = ['protobuf_compiler_helper.py'])",
        "filegroup(",
        "  name = 'protobuf_compiler_support',",
        "  srcs = ['proto_support', 'protobuf_compiler_helper.py'],",
        ")",
        "filegroup(name = 'xctest_infoplist', srcs = ['xctest.plist'])",
        "filegroup(name = 'j2objc_dead_code_pruner', srcs = ['j2objc_dead_code_pruner.py'])",
        "ios_application(name = 'xctest_app', binary = ':xctest_appbin')",
        "objc_binary(name = 'xctest_appbin', srcs = ['objc_dummy.mm'])",
        "filegroup(",
        "  name = 'protobuf_well_known_types',",
        "  srcs = ['//objcproto:well_known_type.proto'],",
        ")",
        "xcode_config(name = 'host_xcodes', default = ':version7_3_1')",
        "xcode_version(",
        "  name = 'version7_3_1',",
        "  version = '" + DEFAULT_XCODE_VERSION + "',",
        "  default_ios_sdk_version = \"" + DEFAULT_IOS_SDK_VERSION + "\",",
        ")",
        "objc_library(name = 'dummy_lib', srcs = ['objc_dummy.mm'])");
    config.create("tools/objc/foo.mobileprovision", "No such luck");
    config.create("tools/objc/compile_protos.py");
    config.create("tools/objc/xctest.plist");
    config.create("tools/objc/proto_support");
    config.create("tools/objc/ios_runner.sh.mac_template");
    config.create("tools/objc/j2objc_dead_code_pruner.py");
    config.create("tools/objc/header_scanner");
    createCrosstoolPackage(config, partialToolchainLines);
  }

  /**
   * Sets up mock IOS test support.
   */
  public static void setupIosTest(MockToolsConfig config) throws IOException {
    config.create("tools/objc/precomp_testrunner_deploy.jar");
    config.create("tools/objc/StdRedirect.dylib");
    createMemleaksSim(config);
    config.create("tools/objc/ios_test.sh.bazel_template");
  }

  /**
   * Sets up mock IOS simulated device support.
   */
  public static void setupIosSimDevice(MockToolsConfig config) throws IOException {
    config.create(
        "tools/objc/sim_devices/BUILD",
        "ios_device(",
        "  name = 'default',",
        "  ios_version = '9.8',",
        "  type = 'iChimpanzee',",
        ")");
  }

  private static void createMemleaksSim(MockToolsConfig config) throws IOException {
    config.create("tools/objc/memleaks/BUILD",
       "package(default_visibility=['//visibility:public'])",
        "objc_library(",
        "  name = 'memleaks',",
        "  srcs = ['memleaks.m'],",
        ")");

    config.create("tools/objc/memleaks/libmemleaks.a");
    config.create("tools/objc/memleaks_plugin");
  }

  /**
   * Sets up the support for building protocol buffers for ObjC.
   */
  public static void setupObjcProto(MockToolsConfig config) throws IOException {
    config.overwrite(
        "WORKSPACE",
        TestConstants.WORKSPACE_CONTENT + "bind(",
        "  name = 'objc_proto_lib',",
        "  actual = '//objcproto:ProtocolBuffers_lib',",
        ")",
        "bind(",
        "  name = 'objc_protobuf_lib',",
        "  actual = '//objcproto:protobuf_lib',",
        ")");

    config.create(
        "objcproto/BUILD",
        "package(default_visibility=['//visibility:public'])",
        "objc_library(",
        "  name = 'ProtocolBuffers_lib',",
        "  srcs = ['empty.m'],",
        ")",
        "objc_library(",
        "  name = 'protobuf_lib',",
        "  srcs = ['empty.m'],",
        "  hdrs = ['include/header.h'],",
        "  includes = ['include'],",
        ")",
        "exports_files(['well_known_type.proto'])",
        "proto_library(",
        "  name = 'well_known_type_proto',",
        "  srcs = ['well_known_type.proto'],",
        ")");
    config.create("objcproto/empty.m");
    config.create("objcproto/empty.cc");
    config.create("objcproto/well_known_type.proto");
  }

  /**
   * Test setup method which creates a package containing the mock OSX crosstool. The crosstool
   * will be available at {@link #DEFAULT_OSX_CROSSTOOL}.
   */
  public static void createCrosstoolPackage(
      MockToolsConfig config, String... partialToolchainLines) throws IOException {
    if (config.isRealFileSystem()) {
      for (String depDir : DEFAULT_OSX_CROSSTOOL_DEPS_DIRS) {
        config.linkTools(depDir);
      }
      config.linkTools(DEFAULT_OSX_CROSSTOOL_DIR);
    } else {
      // Read the crosstool file into crosstoolString.
      InputStream crosstoolFileStream =
          MockObjcSupport.class.getClassLoader().getResourceAsStream(MOCK_OSX_CROSSTOOL_FILE);
      String crosstoolString =
          new String(ByteStreams.toByteArray(crosstoolFileStream), StandardCharsets.UTF_8);

      // Create a config builder and merge the crosstoolString into it.
      CrosstoolConfig.CrosstoolRelease.Builder configBuilder =
          CrosstoolConfig.CrosstoolRelease.newBuilder();
      TextFormat.merge(crosstoolString, configBuilder);

      // Merge partialToolchainLines into the builder.
      CToolchain.Builder toolchainBuilder = CToolchain.newBuilder();
      TextFormat.merge(Joiner.on("\n").join(partialToolchainLines), toolchainBuilder);
      CToolchain partialToolchain = toolchainBuilder.buildPartial();
      for (CToolchain.Builder partialToolchainBuilder :
          configBuilder.getToolchainBuilderList()) {
        partialToolchainBuilder.mergeFrom(partialToolchain);
      }

      // Extract the modified crosstoolString and set things up so
      // that tests can use a crosstool parsed from it.
      crosstoolString = TextFormat.printToString(configBuilder);
      config.overwrite(DEFAULT_OSX_CROSSTOOL_DIR + "/CROSSTOOL", crosstoolString);

      // Create special lines specifying the compiler map entry for
      // each toolchain.
      StringBuilder compilerMap = new StringBuilder();
      for (CToolchain toolchain : configBuilder.build().getToolchainList()) {
        compilerMap.append(String.format("'%s|%s': ':cc-compiler-%s',\n",
            toolchain.getTargetCpu(), toolchain.getCompiler(), toolchain.getTargetCpu()));
      }

      // Create the test BUILD file.
      Builder<String> crosstoolBuild =
          ImmutableList.<String>builder()
              .add(
                  "exports_files(glob(['**']))",
                  "cc_toolchain_suite(",
                  "    name = 'crosstool',",
                  "    toolchains = { " + compilerMap + " },",
                  ")",
                  "",
                  "cc_library(",
                  "    name = 'custom_malloc',",
                  ")",
                  "",
                  "filegroup(",
                  "    name = 'empty',",
                  "    srcs = [],",
                  ")",
                  "",
                  "filegroup(",
                  "    name = 'link',",
                  "    srcs = [",
                  "        'ar',",
                  "        'libempty.a',",
                  "        '//tools/objc:libtool'",
                  "    ],",
                  ")");

      for (String arch :
          ImmutableList.of(
              "ios_x86_64",
              "ios_i386",
              "ios_armv7",
              "ios_arm64",
              "darwin_x86_64",
              "watchos_i386",
              "watchos_armv7k",
              "tvos_x86_64",
              "tvos_arm64",
              // TODO(b/36471772): Remove 'k8' once unit tests do not require a host configuration
              // transition from the apple crosstool configuration.
              "k8")) {
        crosstoolBuild.add(
            "apple_cc_toolchain(",
            "    name = 'cc-compiler-" + arch + "',",
            "    all_files = ':empty',",
            "    compiler_files = ':empty',",
            "    cpu = 'ios',",
            "    dwp_files = ':empty',",
            "    dynamic_runtime_libs = [':empty'],",
            "    linker_files = ':link',",
            "    objcopy_files = ':empty',",
            "    static_runtime_libs = [':empty'],",
            "    strip_files = ':empty',",
            "    supports_param_files = 0,",
            ")");
      }

      config.create(DEFAULT_OSX_CROSSTOOL_DIR + "/BUILD",
          Joiner.on("\n").join(crosstoolBuild.build()));
    }
  }

  /** Test setup for the Apple SDK targets that are used in tests. */
  public static void setupAppleSdks(MockToolsConfig config) throws IOException {
    config.create(
        "third_party/apple_sdks/BUILD",
        "package(default_visibility=['//visibility:public'])\n"
            + "licenses([\"notice\"])\n"
            + "filegroup(name = \"apple_sdk_compile\")");
  }
}
